package com.jsirgalaxybase.quest.postgres;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ConsumptionDetectBatch;
import com.jsirgalaxybase.quest.core.ConsumptionDetectBatchRepository;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantType;

/** PostgreSQL active-batch cursor; individual inventory operations remain in the submission saga table. */
public final class JdbcConsumptionDetectBatchRepository implements ConsumptionDetectBatchRepository {
    private final JdbcQuestConnectionManager connections;

    public JdbcConsumptionDetectBatchRepository(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections are required");
        this.connections = connections;
    }

    @Override
    public Optional<ConsumptionDetectBatch> findActive(final UUID playerId, final UUID questId) {
        requireTransaction();
        if (playerId == null || questId == null) throw new IllegalArgumentException("valid Detect identity is required");
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT batch_id,source_server,player_id,participant_type,participant_id,quest_id,"
                    + "definition_version,quest_cycle,task_keys,next_task_index,created_at "
                    + "FROM galaxy_quest_consumption_detect_batch WHERE player_id=? "
                    + "AND quest_id=? AND batch_status='ACTIVE' ORDER BY created_at,batch_id FOR UPDATE")) {
                statement.setObject(1, playerId);
                statement.setObject(2, questId);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) return Optional.<ConsumptionDetectBatch>empty();
                    ConsumptionDetectBatch found = read(row);
                    if (row.next()) throw new QuestPersistenceException(
                        "multiple active Detect batches exist for one player and Quest; refusing ambiguous recovery");
                    return Optional.of(found);
                }
            }
        });
    }

    @Override
    public ConsumptionDetectBatch createIfAbsent(final ConsumptionDetectBatch batch) {
        requireTransaction();
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO galaxy_quest_consumption_detect_batch(batch_id,source_server,player_id,"
                    + "participant_type,participant_id,quest_id,definition_version,quest_cycle,task_keys,"
                    + "next_task_index,batch_status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,"
                    + "'ACTIVE',?,?) ON CONFLICT (participant_type,participant_id,quest_id) "
                    + "WHERE batch_status='ACTIVE' DO NOTHING")) {
                statement.setObject(1, UUID.fromString(batch.getBatchId()));
                statement.setString(2, batch.getSourceServer());
                statement.setObject(3, batch.getPlayerId());
                statement.setString(4, batch.getParticipantId().getType().name());
                statement.setObject(5, batch.getParticipantId().getId());
                statement.setObject(6, batch.getQuestId());
                statement.setInt(7, batch.getDefinitionVersion());
                statement.setInt(8, batch.getCycle());
                statement.setArray(9, connection.createArrayOf("TEXT", batch.getTaskKeys().toArray(
                    new String[batch.getTaskKeys().size()])));
                statement.setInt(10, batch.getNextTaskIndex());
                statement.setLong(11, batch.getCreatedAt());
                statement.setLong(12, batch.getCreatedAt());
                statement.executeUpdate();
            }
            return selectActive(connection, batch.getPlayerId(), batch.getQuestId());
        });
    }

    @Override
    public ConsumptionDetectBatch advance(final String batchId, final int expectedTaskIndex, final long updatedAt) {
        requireTransaction();
        final UUID id = parseId(batchId);
        if (expectedTaskIndex < 0) throw new IllegalArgumentException("expectedTaskIndex must not be negative");
        return connections.withConnection(connection -> {
            ConsumptionDetectBatch before = selectById(connection, id);
            if (before.getNextTaskIndex() > expectedTaskIndex) return before;
            if (before.getNextTaskIndex() != expectedTaskIndex || before.isComplete()) throw new QuestPersistenceException(
                "Detect batch cursor changed unexpectedly");
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE galaxy_quest_consumption_detect_batch SET next_task_index=next_task_index+1,updated_at=? "
                    + "WHERE batch_id=? AND batch_status='ACTIVE' AND next_task_index=?")) {
                statement.setLong(1, updatedAt);
                statement.setObject(2, id);
                statement.setInt(3, expectedTaskIndex);
                if (statement.executeUpdate() != 1) throw new QuestPersistenceException(
                    "Detect batch cursor was concurrently changed");
            }
            return selectById(connection, id);
        });
    }

    @Override
    public void deleteCompleted(final String batchId, final int expectedTaskCount, final long updatedAt) {
        requireTransaction();
        if (expectedTaskCount < 1) throw new IllegalArgumentException("expectedTaskCount must be positive");
        final UUID id = parseId(batchId);
        connections.withConnection(connection -> {
            ConsumptionDetectBatch batch = selectById(connection, id);
            if (!batch.isComplete() || batch.getTaskKeys().size() != expectedTaskCount) throw new QuestPersistenceException(
                "cannot remove an incomplete or changed Detect batch");
            try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM galaxy_quest_consumption_detect_batch WHERE batch_id=? AND batch_status='ACTIVE' "
                    + "AND next_task_index=?")) {
                statement.setObject(1, id);
                statement.setInt(2, expectedTaskCount);
                if (statement.executeUpdate() != 1) throw new QuestPersistenceException(
                    "Detect batch disappeared before settlement");
            }
            return null;
        });
    }

    private ConsumptionDetectBatch selectActive(Connection connection, UUID playerId, UUID questId)
        throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT batch_id,source_server,player_id,participant_type,participant_id,quest_id,"
                + "definition_version,quest_cycle,task_keys,next_task_index,created_at "
                + "FROM galaxy_quest_consumption_detect_batch WHERE player_id=? "
                + "AND quest_id=? AND batch_status='ACTIVE' ORDER BY created_at,batch_id FOR UPDATE")) {
            statement.setObject(1, playerId);
            statement.setObject(2, questId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new QuestPersistenceException("active Detect batch insert was not visible");
                ConsumptionDetectBatch found = read(row);
                if (row.next()) throw new QuestPersistenceException(
                    "multiple active Detect batches exist for one player and Quest; refusing ambiguous recovery");
                return found;
            }
        }
    }

    private ConsumptionDetectBatch selectById(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT batch_id,source_server,player_id,participant_type,participant_id,quest_id,"
                + "definition_version,quest_cycle,task_keys,next_task_index,created_at "
                + "FROM galaxy_quest_consumption_detect_batch WHERE batch_id=? AND batch_status='ACTIVE' FOR UPDATE")) {
            statement.setObject(1, id);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new QuestPersistenceException("unknown active Detect batch: " + id);
                return read(row);
            }
        }
    }

    private ConsumptionDetectBatch read(ResultSet row) throws SQLException {
        Array sqlArray = row.getArray(9);
        Object[] values = (Object[]) sqlArray.getArray();
        List<String> keys = new ArrayList<String>(values.length);
        for (Object value : values) keys.add(String.valueOf(value));
        return new ConsumptionDetectBatch(row.getString(1), row.getString(2), (UUID) row.getObject(3),
            new ParticipantId(ParticipantType.valueOf(row.getString(4)), (UUID) row.getObject(5)),
            (UUID) row.getObject(6), row.getInt(7), row.getInt(8), keys, row.getInt(10), row.getLong(11));
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "consumption Detect batch mutations require an active quest transaction");
    }

    private UUID parseId(String value) {
        try { return UUID.fromString(value); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("invalid Detect batch id", invalid); }
    }
}
