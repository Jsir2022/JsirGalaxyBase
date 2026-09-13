package com.jsirgalaxybase.quest.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestDefinitionConflictException;
import com.jsirgalaxybase.quest.core.QuestDefinitionLifecycle;
import com.jsirgalaxybase.quest.core.QuestDefinitionRepository;
import com.jsirgalaxybase.quest.core.QuestDefinitionManagementPage;
import com.jsirgalaxybase.quest.core.QuestDefinitionManagementQuery;
import com.jsirgalaxybase.quest.core.QuestDefinitionManagementRequest;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;

public final class JdbcQuestDefinitionRepository implements QuestDefinitionRepository, QuestDefinitionManagementQuery {
    private final JdbcQuestConnectionManager connections;
    private final QuestDefinitionJsonCodec codec = new QuestDefinitionJsonCodec();

    public JdbcQuestDefinitionRepository(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public StoredQuestDefinition createDraft(final QuestDefinition definition) {
        requireTransaction();
        final QuestDefinitionJsonCodec.Encoded encoded = codec.encode(definition);
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO galaxy_quest_definition(quest_id,definition_version,lifecycle,content_json,content_hash) "
                    + "VALUES (?,?,'DRAFT',?::jsonb,?)")) {
                statement.setObject(1, definition.getId());
                statement.setInt(2, definition.getVersion());
                statement.setString(3, encoded.json);
                statement.setString(4, encoded.hash);
                try {
                    statement.executeUpdate();
                } catch (SQLException conflict) {
                    if ("23505".equals(conflict.getSQLState())) {
                        throw new QuestDefinitionConflictException("definition version already exists");
                    }
                    throw conflict;
                }
            }
            return new StoredQuestDefinition(definition, QuestDefinitionLifecycle.DRAFT, encoded.hash, 0L);
        });
    }

    @Override
    public StoredQuestDefinition updateDraft(final QuestDefinition definition, final String expectedContentHash) {
        requireTransaction();
        final QuestDefinitionJsonCodec.Encoded encoded = codec.encode(definition);
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE galaxy_quest_definition SET content_json=?::jsonb,content_hash=? "
                    + "WHERE quest_id=? AND definition_version=? AND lifecycle='DRAFT' AND content_hash=?")) {
                statement.setString(1, encoded.json);
                statement.setString(2, encoded.hash);
                statement.setObject(3, definition.getId());
                statement.setInt(4, definition.getVersion());
                statement.setString(5, expectedContentHash);
                if (statement.executeUpdate() != 1) throw new QuestDefinitionConflictException(
                    "draft was changed, published, retired, or removed");
            }
            return new StoredQuestDefinition(definition, QuestDefinitionLifecycle.DRAFT, encoded.hash, 0L);
        });
    }

    @Override
    public boolean publish(final UUID questId, final int version, final String expectedContentHash,
        final long publishedAt) {
        requireTransaction();
        return connections.withConnection(connection -> updateLifecycle(connection,
            "UPDATE galaxy_quest_definition SET lifecycle='PUBLISHED',published_at=to_timestamp(? / 1000.0) "
                + "WHERE quest_id=? AND definition_version=? AND lifecycle='DRAFT' AND content_hash=?",
            questId, version, expectedContentHash, publishedAt));
    }

    @Override
    public boolean retire(final UUID questId, final int version) {
        requireTransaction();
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE galaxy_quest_definition SET lifecycle='RETIRED' "
                    + "WHERE quest_id=? AND definition_version=? AND lifecycle='PUBLISHED'")) {
                statement.setObject(1, questId);
                statement.setInt(2, version);
                return statement.executeUpdate() == 1;
            }
        });
    }

    @Override
    public Optional<StoredQuestDefinition> find(final UUID questId, final int version) {
        return connections.withConnection(connection -> select(connection,
            "SELECT lifecycle,content_json::text,content_hash,COALESCE(EXTRACT(EPOCH FROM published_at)*1000,0)::bigint "
                + "FROM galaxy_quest_definition WHERE quest_id=? AND definition_version=?", questId, version));
    }

    @Override
    public Optional<StoredQuestDefinition> findPublished(final UUID questId) {
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT lifecycle,content_json::text,content_hash,COALESCE(EXTRACT(EPOCH FROM published_at)*1000,0)::bigint "
                    + "FROM galaxy_quest_definition WHERE quest_id=? AND lifecycle='PUBLISHED' "
                    + "ORDER BY definition_version DESC LIMIT 1")) {
                statement.setObject(1, questId);
                return read(statement);
            }
        });
    }

    @Override
    public List<StoredQuestDefinition> findAllPublished() {
        return connections.withConnection(connection -> {
            String sql = "SELECT lifecycle,content_json::text,content_hash,"
                + "COALESCE(EXTRACT(EPOCH FROM published_at)*1000,0)::bigint FROM ("
                + "SELECT DISTINCT ON (quest_id) lifecycle,content_json,content_hash,published_at,quest_id,definition_version "
                + "FROM galaxy_quest_definition WHERE lifecycle='PUBLISHED' "
                + "ORDER BY quest_id,definition_version DESC) latest ORDER BY quest_id";
            List<StoredQuestDefinition> result = new ArrayList<StoredQuestDefinition>();
            try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new StoredQuestDefinition(codec.decode(rows.getString(2)),
                    QuestDefinitionLifecycle.valueOf(rows.getString(1)), rows.getString(3), rows.getLong(4)));
            }
            return result;
        });
    }

    @Override
    public QuestDefinitionManagementPage load(final QuestDefinitionManagementRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return connections.withConnection(connection -> {
            String where = where(request);
            long total;
            try (PreparedStatement count = connection.prepareStatement(
                "SELECT COUNT(*) FROM galaxy_quest_definition" + where)) {
                bindFilters(count, request);
                try (ResultSet row = count.executeQuery()) { row.next(); total = row.getLong(1); }
            }
            List<StoredQuestDefinition> result = new ArrayList<StoredQuestDefinition>();
            String sql = "SELECT lifecycle,content_json::text,content_hash,"
                + "COALESCE(EXTRACT(EPOCH FROM published_at)*1000,0)::bigint FROM galaxy_quest_definition"
                + where + " ORDER BY created_at DESC,quest_id,definition_version DESC LIMIT ? OFFSET ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = bindFilters(statement, request);
                statement.setInt(index++, request.getPageSize());
                statement.setInt(index, request.getOffset());
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) result.add(new StoredQuestDefinition(codec.decode(rows.getString(2)),
                        QuestDefinitionLifecycle.valueOf(rows.getString(1)), rows.getString(3), rows.getLong(4)));
                }
            }
            return new QuestDefinitionManagementPage(result, request.getPage(), request.getPageSize(), total);
        });
    }

    private static String where(QuestDefinitionManagementRequest request) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1");
        if (request.getLifecycle() != null) sql.append(" AND lifecycle=?");
        if (!request.getQuery().isEmpty()) {
            sql.append(" AND (LOWER(content_json->>'name') LIKE ? OR CAST(quest_id AS text)=?)");
        }
        return sql.toString();
    }

    private static int bindFilters(PreparedStatement statement, QuestDefinitionManagementRequest request)
        throws SQLException {
        int index = 1;
        if (request.getLifecycle() != null) statement.setString(index++, request.getLifecycle().name());
        if (!request.getQuery().isEmpty()) {
            statement.setString(index++, "%" + request.getQuery().toLowerCase(java.util.Locale.ROOT) + "%");
            statement.setString(index++, request.getQuery());
        }
        return index;
    }

    private boolean updateLifecycle(Connection connection, String sql, UUID questId, int version,
        String expectedHash, long publishedAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, publishedAt);
            statement.setObject(2, questId);
            statement.setInt(3, version);
            statement.setString(4, expectedHash);
            return statement.executeUpdate() == 1;
        }
    }

    private Optional<StoredQuestDefinition> select(Connection connection, String sql, UUID questId, int version)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, questId);
            statement.setInt(2, version);
            return read(statement);
        }
    }

    private Optional<StoredQuestDefinition> read(PreparedStatement statement) throws SQLException {
        try (ResultSet rows = statement.executeQuery()) {
            if (!rows.next()) return Optional.empty();
            return Optional.of(new StoredQuestDefinition(codec.decode(rows.getString(2)),
                QuestDefinitionLifecycle.valueOf(rows.getString(1)), rows.getString(3), rows.getLong(4)));
        }
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "definition mutations require an active quest transaction");
    }
}
