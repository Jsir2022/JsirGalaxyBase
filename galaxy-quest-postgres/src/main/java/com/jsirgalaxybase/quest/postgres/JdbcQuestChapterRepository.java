package com.jsirgalaxybase.quest.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestChapterDefinition;
import com.jsirgalaxybase.quest.core.QuestChapterRepository;
import com.jsirgalaxybase.quest.core.QuestChapterManagementQuery;
import com.jsirgalaxybase.quest.core.QuestChapterManagementPage;
import com.jsirgalaxybase.quest.core.QuestChapterCatalogEntry;
import com.jsirgalaxybase.quest.core.QuestChapterCatalogRepository;
import com.jsirgalaxybase.quest.core.QuestDefinitionManagementRequest;
import com.jsirgalaxybase.quest.core.QuestDefinitionConflictException;
import com.jsirgalaxybase.quest.core.QuestDefinitionLifecycle;
import com.jsirgalaxybase.quest.core.StoredQuestChapter;

public final class JdbcQuestChapterRepository implements QuestChapterRepository, QuestChapterManagementQuery, QuestChapterCatalogRepository {
    private final JdbcQuestConnectionManager connections;
    private final QuestChapterJsonCodec codec = new QuestChapterJsonCodec();

    public JdbcQuestChapterRepository(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public StoredQuestChapter createDraft(final QuestChapterDefinition definition) {
        requireTransaction();
        final QuestChapterJsonCodec.Encoded encoded = codec.encode(definition);
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO galaxy_quest_chapter_definition(chapter_id,definition_version,lifecycle,content_json,content_hash) "
                    + "VALUES (?,?,'DRAFT',?::jsonb,?)")) {
                statement.setObject(1, definition.getId());
                statement.setInt(2, definition.getVersion());
                statement.setString(3, encoded.json);
                statement.setString(4, encoded.hash);
                try {
                    statement.executeUpdate();
                } catch (SQLException conflict) {
                    if ("23505".equals(conflict.getSQLState())) throw new QuestDefinitionConflictException(
                        "chapter version already exists");
                    throw conflict;
                }
            }
            ensureCatalogEntry(definition.getId());
            return stored(definition, QuestDefinitionLifecycle.DRAFT, encoded.hash, 0L);
        });
    }

    @Override
    public StoredQuestChapter updateDraft(final QuestChapterDefinition definition, final String expectedContentHash) {
        requireTransaction();
        final QuestChapterJsonCodec.Encoded encoded = codec.encode(definition);
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE galaxy_quest_chapter_definition SET content_json=?::jsonb,content_hash=? "
                    + "WHERE chapter_id=? AND definition_version=? AND lifecycle='DRAFT' AND content_hash=?")) {
                statement.setString(1, encoded.json);
                statement.setString(2, encoded.hash);
                statement.setObject(3, definition.getId());
                statement.setInt(4, definition.getVersion());
                statement.setString(5, expectedContentHash);
                if (statement.executeUpdate() != 1) throw new QuestDefinitionConflictException(
                    "chapter draft was changed, published, retired, or removed");
            }
            return stored(definition, QuestDefinitionLifecycle.DRAFT, encoded.hash, 0L);
        });
    }

    @Override
    public boolean publish(final UUID chapterId, final int version, final String expectedContentHash,
        final long publishedAt) {
        requireTransaction();
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE galaxy_quest_chapter_definition SET lifecycle='PUBLISHED',published_at=to_timestamp(? / 1000.0) "
                    + "WHERE chapter_id=? AND definition_version=? AND lifecycle='DRAFT' AND content_hash=?")) {
                statement.setLong(1, publishedAt);
                statement.setObject(2, chapterId);
                statement.setInt(3, version);
                statement.setString(4, expectedContentHash);
                return statement.executeUpdate() == 1;
            }
        });
    }

    @Override
    public boolean retire(final UUID chapterId, final int version) {
        requireTransaction();
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE galaxy_quest_chapter_definition SET lifecycle='RETIRED' "
                    + "WHERE chapter_id=? AND definition_version=? AND lifecycle='PUBLISHED'")) {
                statement.setObject(1, chapterId);
                statement.setInt(2, version);
                return statement.executeUpdate() == 1;
            }
        });
    }

    @Override
    public Optional<StoredQuestChapter> find(final UUID chapterId, final int version) {
        return connections.withConnection(connection -> select(connection,
            "SELECT lifecycle,content_json::text,content_hash,COALESCE(EXTRACT(EPOCH FROM published_at)*1000,0)::bigint "
                + "FROM galaxy_quest_chapter_definition WHERE chapter_id=? AND definition_version=?", chapterId, version));
    }

    @Override
    public Optional<StoredQuestChapter> findPublished(final UUID chapterId) {
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT lifecycle,content_json::text,content_hash,COALESCE(EXTRACT(EPOCH FROM published_at)*1000,0)::bigint "
                    + "FROM galaxy_quest_chapter_definition WHERE chapter_id=? AND lifecycle='PUBLISHED' "
                    + "ORDER BY definition_version DESC LIMIT 1")) {
                statement.setObject(1, chapterId);
                return read(statement);
            }
        });
    }

    @Override
    public List<StoredQuestChapter> findAllPublished() {
        return connections.withConnection(connection -> {
            String sql = "SELECT lifecycle,content_json::text,content_hash,"
                + "COALESCE(EXTRACT(EPOCH FROM published_at)*1000,0)::bigint FROM ("
                + "SELECT DISTINCT ON (chapter_id) lifecycle,content_json,content_hash,published_at,chapter_id,definition_version "
                + "FROM galaxy_quest_chapter_definition WHERE lifecycle='PUBLISHED' "
                + "ORDER BY chapter_id,definition_version DESC) latest ORDER BY chapter_id";
            List<StoredQuestChapter> result = new ArrayList<StoredQuestChapter>();
            try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(stored(codec.decode(rows.getString(2)),
                    QuestDefinitionLifecycle.valueOf(rows.getString(1)), rows.getString(3), rows.getLong(4)));
            }
            return result;
        });
    }

    @Override
    public QuestChapterManagementPage load(final QuestDefinitionManagementRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        return connections.withConnection(connection -> {
            String where = where(request);
            long total;
            try (PreparedStatement count = connection.prepareStatement(
                "SELECT COUNT(*) FROM galaxy_quest_chapter_definition" + where)) {
                bind(count, request);
                try (ResultSet row = count.executeQuery()) {
                    row.next();
                    total = row.getLong(1);
                }
            }
            List<StoredQuestChapter> result = new ArrayList<StoredQuestChapter>();
            String sql = "SELECT lifecycle,content_json::text,content_hash,"
                + "COALESCE(EXTRACT(EPOCH FROM published_at)*1000,0)::bigint "
                + "FROM galaxy_quest_chapter_definition"
                + " LEFT JOIN galaxy_quest_chapter_catalog catalog ON catalog.chapter_id=galaxy_quest_chapter_definition.chapter_id" + where
                + " ORDER BY COALESCE(catalog.sort_order,9223372036854775807),galaxy_quest_chapter_definition.created_at DESC,galaxy_quest_chapter_definition.chapter_id,definition_version DESC LIMIT ? OFFSET ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = bind(statement, request);
                statement.setInt(index++, request.getPageSize());
                statement.setInt(index, request.getOffset());
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) result.add(stored(codec.decode(rows.getString(2)),
                        QuestDefinitionLifecycle.valueOf(rows.getString(1)), rows.getString(3), rows.getLong(4)));
                }
            }
            return new QuestChapterManagementPage(result, request.getPage(), request.getPageSize(), total);
        });
    }

    private static String where(QuestDefinitionManagementRequest request) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1");
        if (request.getLifecycle() != null) sql.append(" AND lifecycle=?");
        if (!request.getQuery().isEmpty()) {
            sql.append(" AND (LOWER(content_json->>'name') LIKE ? OR CAST(galaxy_quest_chapter_definition.chapter_id AS text)=?)");
        }
        return sql.toString();
    }

    private static int bind(PreparedStatement statement, QuestDefinitionManagementRequest request)
        throws SQLException {
        int index = 1;
        if (request.getLifecycle() != null) statement.setString(index++, request.getLifecycle().name());
        if (!request.getQuery().isEmpty()) {
            statement.setString(index++, "%" + request.getQuery().toLowerCase(java.util.Locale.ROOT) + "%");
            statement.setString(index++, request.getQuery());
        }
        return index;
    }

    private Optional<StoredQuestChapter> select(Connection connection, String sql, UUID id, int version)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.setInt(2, version);
            return read(statement);
        }
    }

    private Optional<StoredQuestChapter> read(PreparedStatement statement) throws SQLException {
        try (ResultSet rows = statement.executeQuery()) {
            if (!rows.next()) return Optional.empty();
            return Optional.of(stored(codec.decode(rows.getString(2)),
                QuestDefinitionLifecycle.valueOf(rows.getString(1)), rows.getString(3), rows.getLong(4)));
        }
    }

    private static StoredQuestChapter stored(QuestChapterDefinition definition, QuestDefinitionLifecycle lifecycle,
        String hash, long publishedAt) {
        return new StoredQuestChapter(definition, lifecycle, hash, publishedAt);
    }

    @Override public void ensureCatalogEntry(final UUID chapterId) {
        requireTransaction(); if (chapterId == null) throw new IllegalArgumentException("chapter id is required");
        connections.withConnection(connection -> {
            try (PreparedStatement lock = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext('galaxy_quest_chapter_catalog'))")) { lock.execute(); }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO galaxy_quest_chapter_catalog(chapter_id,sort_order) "
                + "SELECT ?,COALESCE(MAX(sort_order)+1,0) FROM galaxy_quest_chapter_catalog ON CONFLICT (chapter_id) DO NOTHING")) {
                statement.setObject(1, chapterId); statement.executeUpdate();
            }
            return null;
        });
    }

    @Override public List<QuestChapterCatalogEntry> listOrdered() {
        return connections.withConnection(connection -> {
            List<QuestChapterCatalogEntry> result = new ArrayList<QuestChapterCatalogEntry>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT chapter_id,sort_order FROM galaxy_quest_chapter_catalog ORDER BY sort_order,chapter_id"); ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new QuestChapterCatalogEntry((UUID) rows.getObject(1), rows.getLong(2)));
            }
            return result;
        });
    }

    @Override public void replaceOrder(final List<UUID> orderedChapterIds) {
        requireTransaction(); if (orderedChapterIds == null) throw new IllegalArgumentException("order is required");
        connections.withConnection(connection -> {
            try (PreparedStatement lock = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext('galaxy_quest_chapter_catalog'))")) { lock.execute(); }
            List<UUID> known = new ArrayList<UUID>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT chapter_id FROM galaxy_quest_chapter_catalog FOR UPDATE"); ResultSet rows = statement.executeQuery()) { while (rows.next()) known.add((UUID) rows.getObject(1)); }
            if (known.size() != orderedChapterIds.size() || !new java.util.HashSet<UUID>(known).equals(new java.util.HashSet<UUID>(orderedChapterIds))) throw new QuestDefinitionConflictException("chapter catalog changed");
            try (PreparedStatement update = connection.prepareStatement("UPDATE galaxy_quest_chapter_catalog SET sort_order=? WHERE chapter_id=?")) {
                for (int i = 0; i < orderedChapterIds.size(); i++) { update.setLong(1, i); update.setObject(2, orderedChapterIds.get(i)); update.addBatch(); }
                int[] changed = update.executeBatch(); if (changed.length != orderedChapterIds.size()) throw new QuestDefinitionConflictException("chapter catalog changed");
            }
            return null;
        });
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "chapter mutations require an active quest transaction");
    }
}
