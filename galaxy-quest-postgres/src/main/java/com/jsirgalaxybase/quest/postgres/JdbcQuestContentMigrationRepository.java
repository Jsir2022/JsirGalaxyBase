package com.jsirgalaxybase.quest.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.quest.core.QuestContentMigrationQuery;
import com.jsirgalaxybase.quest.core.QuestContentMigrationSummary;

/** Audit repository for offline BQ content migration previews and applied batches. */
public final class JdbcQuestContentMigrationRepository implements QuestContentMigrationQuery {
    private final JdbcQuestConnectionManager connections;

    public JdbcQuestContentMigrationRepository(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    public Optional<QuestContentMigrationBatch> find(final String batchId) {
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT source_hash,status,manifest_text,previous_chapter_order,error_count,warning_count "
                    + "FROM galaxy_quest_content_migration_batch WHERE batch_id=?")) {
                statement.setString(1, batchId);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) return Optional.empty();
                    return Optional.of(new QuestContentMigrationBatch(batchId, row.getString(1),
                        QuestContentMigrationBatch.Status.valueOf(row.getString(2)), row.getString(3),
                        row.getString(4), row.getInt(5), row.getInt(6), items(batchId)));
                }
            }
        });
    }

    public void lock() {
        requireTransaction();
        connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pg_advisory_xact_lock(hashtext('galaxy_quest_content_migration'))")) {
                statement.execute();
            }
            return null;
        });
    }

    @Override
    public List<QuestContentMigrationSummary> listRecent(final int limit) {
        return connections.withConnection(connection -> {
            List<QuestContentMigrationSummary> result = new ArrayList<QuestContentMigrationSummary>();
            String sql = "SELECT b.batch_id,b.source_hash,b.status,"
                + "EXTRACT(EPOCH FROM b.applied_at)*1000,"
                + "COALESCE(EXTRACT(EPOCH FROM b.rolled_back_at)*1000,0),"
                + "COUNT(*) FILTER (WHERE i.action='CREATED'),"
                + "COUNT(*) FILTER (WHERE i.action='VERSIONED'),"
                + "COUNT(*) FILTER (WHERE i.action='UNCHANGED'),"
                + "b.error_count,b.warning_count,LEFT(b.manifest_text,2048) "
                + "FROM galaxy_quest_content_migration_batch b "
                + "LEFT JOIN galaxy_quest_content_migration_item i ON i.batch_id=b.batch_id "
                + "GROUP BY b.batch_id ORDER BY b.applied_at DESC,b.batch_id LIMIT ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, Math.max(1, Math.min(20, limit)));
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) result.add(new QuestContentMigrationSummary(rows.getString(1),
                        rows.getString(2), rows.getString(3), rows.getLong(4), rows.getLong(5), rows.getInt(6),
                        rows.getInt(7), rows.getInt(8), rows.getInt(9), rows.getInt(10), rows.getString(11)));
                }
            }
            return result;
        });
    }

    public void insert(final QuestContentMigrationBatch batch) {
        requireTransaction();
        connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO galaxy_quest_content_migration_batch(batch_id,source_hash,status,manifest_text,"
                    + "previous_chapter_order,error_count,warning_count) VALUES (?,?,?,?,?,?,?)")) {
                bindBatch(statement, batch);
                statement.executeUpdate();
            }
            insertItems(batch);
            return null;
        });
    }

    /** Promotes an existing PREVIEW row to APPLIED while atomically replacing its now-current plan. */
    public void promoteApplied(final QuestContentMigrationBatch batch) {
        requireTransaction();
        connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE galaxy_quest_content_migration_batch SET status='APPLIED',manifest_text=?,"
                    + "previous_chapter_order=?,error_count=?,warning_count=?,applied_at=CURRENT_TIMESTAMP "
                    + "WHERE batch_id=? AND status='PREVIEW'")) {
                statement.setString(1, batch.getManifestText()); statement.setString(2, batch.getPreviousChapterOrder());
                statement.setInt(3, batch.getErrorCount()); statement.setInt(4, batch.getWarningCount());
                statement.setString(5, batch.getBatchId());
                if (statement.executeUpdate() != 1) throw new QuestPersistenceException(
                    "migration preview is not promotable");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM galaxy_quest_content_migration_item WHERE batch_id=?")) {
                statement.setString(1, batch.getBatchId()); statement.executeUpdate();
            }
            insertItems(batch);
            return null;
        });
    }

    public void markRolledBack(final String batchId) {
        requireTransaction();
        connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE galaxy_quest_content_migration_batch SET status='ROLLED_BACK',"
                    + "rolled_back_at=CURRENT_TIMESTAMP WHERE batch_id=? AND status='APPLIED'")) {
                statement.setString(1, batchId);
                if (statement.executeUpdate() != 1) throw new QuestPersistenceException(
                    "migration batch is not applied");
            }
            return null;
        });
    }

    public void restoreChapterCatalog(final String previousOrder, final List<QuestContentMigrationItem> items) {
        requireTransaction();
        connections.withConnection(connection -> {
            List<java.util.UUID> previous = new ArrayList<java.util.UUID>();
            if (previousOrder != null && !previousOrder.isEmpty()) for (String value : previousOrder.split(","))
                previous.add(java.util.UUID.fromString(value));
            try (PreparedStatement update = connection.prepareStatement(
                "UPDATE galaxy_quest_chapter_catalog SET sort_order=?,updated_at=CURRENT_TIMESTAMP WHERE chapter_id=?")) {
                for (int i = 0; i < previous.size(); i++) {
                    update.setLong(1, i); update.setObject(2, previous.get(i)); update.addBatch();
                }
                update.executeBatch();
            }
            try (PreparedStatement remove = connection.prepareStatement(
                "DELETE FROM galaxy_quest_chapter_catalog WHERE chapter_id=? AND NOT EXISTS "
                    + "(SELECT 1 FROM galaxy_quest_chapter_definition d WHERE d.chapter_id=? "
                    + "AND d.lifecycle='PUBLISHED')")) {
                for (QuestContentMigrationItem item : items) if (item.getKind() == QuestContentMigrationItem.Kind.CHAPTER
                    && item.getAction() == QuestContentMigrationItem.Action.CREATED && !previous.contains(item.getId())) {
                    remove.setObject(1, item.getId()); remove.setObject(2, item.getId()); remove.addBatch();
                }
                remove.executeBatch();
            }
            return null;
        });
    }

    private void insertItems(QuestContentMigrationBatch batch) throws java.sql.SQLException {
        try (PreparedStatement statement = connections.current().prepareStatement(
            "INSERT INTO galaxy_quest_content_migration_item(batch_id,entity_kind,entity_id,definition_version,"
                + "action,content_hash) VALUES (?,?,?,?,?,?)")) {
            for (QuestContentMigrationItem item : batch.getItems()) {
                statement.setString(1, batch.getBatchId()); statement.setString(2, item.getKind().name());
                statement.setObject(3, item.getId()); statement.setInt(4, item.getVersion());
                statement.setString(5, item.getAction().name()); statement.setString(6, item.getHash());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void bindBatch(PreparedStatement statement, QuestContentMigrationBatch batch)
        throws java.sql.SQLException {
        statement.setString(1, batch.getBatchId()); statement.setString(2, batch.getSourceHash());
        statement.setString(3, batch.getStatus().name()); statement.setString(4, batch.getManifestText());
        statement.setString(5, batch.getPreviousChapterOrder()); statement.setInt(6, batch.getErrorCount());
        statement.setInt(7, batch.getWarningCount());
    }

    private List<QuestContentMigrationItem> items(String batchId) throws java.sql.SQLException {
        return connections.withConnection(connection -> {
            List<QuestContentMigrationItem> result = new ArrayList<QuestContentMigrationItem>();
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT entity_kind,entity_id,definition_version,action,content_hash "
                    + "FROM galaxy_quest_content_migration_item WHERE batch_id=? ORDER BY entity_kind,entity_id")) {
                statement.setString(1, batchId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) result.add(new QuestContentMigrationItem(
                        QuestContentMigrationItem.Kind.valueOf(rows.getString(1)),
                        (java.util.UUID) rows.getObject(2), rows.getInt(3),
                        QuestContentMigrationItem.Action.valueOf(rows.getString(4)), rows.getString(5)));
                }
            }
            return result;
        });
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "migration mutations require an active quest transaction");
    }
}
