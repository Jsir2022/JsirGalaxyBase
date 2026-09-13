package com.jsirgalaxybase.quest.postgres;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestRuntimeRepository;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.RewardEntitlement;
import com.jsirgalaxybase.quest.core.TaskProgress;

public final class JdbcQuestRuntimeRepository implements QuestRuntimeRepository {
    private final JdbcQuestConnectionManager connections;
    private final Gson gson;

    public JdbcQuestRuntimeRepository(JdbcQuestConnectionManager connections) {
        this(connections, new Gson());
    }

    JdbcQuestRuntimeRepository(JdbcQuestConnectionManager connections, Gson gson) {
        if (connections == null || gson == null) throw new IllegalArgumentException("dependencies must not be null");
        this.connections = connections;
        this.gson = gson;
    }

    @Override
    public Optional<QuestProgressSnapshot> findProgress(final ParticipantId participantId, final UUID questId,
        final int definitionVersion) {
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Optional<QuestProgressSnapshot>>() {
            @Override
            public Optional<QuestProgressSnapshot> execute(Connection connection) throws SQLException {
                if (connections.current() == connection) lockProgressIdentity(connection, participantId, questId,
                    definitionVersion);
                String lock = connections.current() == connection ? " FOR UPDATE" : "";
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT status, completed_at, cycle FROM galaxy_quest_progress "
                        + "WHERE participant_type=? AND participant_id=? AND quest_id=? AND definition_version=?" + lock)) {
                    bindIdentity(statement, participantId, questId, definitionVersion);
                    try (ResultSet rows = statement.executeQuery()) {
                        if (!rows.next()) return Optional.empty();
                        QuestStatus status = QuestStatus.valueOf(rows.getString(1));
                        long completedAt = rows.getLong(2);
                        int cycle = rows.getInt(3);
                        Map<String, TaskProgress> tasks = readTasks(connection, participantId, questId, definitionVersion);
                        return Optional.of(new QuestProgressSnapshot(participantId, questId, definitionVersion,
                            status, tasks, completedAt, cycle));
                    }
                }
            }
        });
    }

    @Override
    public boolean recordFactIfAbsent(final GameplayFact fact) {
        requireTransaction();
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Boolean>() {
            @Override
            public Boolean execute(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO galaxy_quest_fact(source_server,event_id,player_id,fact_type,occurred_at,attributes_json) "
                        + "VALUES (?,?,?,?,?,?::jsonb) ON CONFLICT (source_server,event_id) DO NOTHING")) {
                    statement.setString(1, fact.getSourceServer());
                    statement.setString(2, fact.getEventId());
                    statement.setObject(3, fact.getPlayerId());
                    statement.setString(4, fact.getTypeId());
                    statement.setLong(5, fact.getOccurredAt());
                    statement.setString(6, gson.toJson(fact.getAttributes()));
                    return statement.executeUpdate() == 1;
                }
            }
        });
    }

    @Override
    public void saveProgress(final QuestProgressSnapshot progress) {
        requireTransaction();
        connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Void>() {
            @Override
            public Void execute(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO galaxy_quest_progress(participant_type,participant_id,quest_id,definition_version,cycle,status,completed_at) "
                        + "VALUES (?,?,?,?,?,?,?) ON CONFLICT (participant_type,participant_id,quest_id,definition_version) "
                        + "DO UPDATE SET cycle=EXCLUDED.cycle,status=EXCLUDED.status,completed_at=EXCLUDED.completed_at,"
                        + "revision=galaxy_quest_progress.revision+1,updated_at=CURRENT_TIMESTAMP")) {
                    bindIdentity(statement, progress.getParticipantId(), progress.getQuestId(), progress.getDefinitionVersion());
                    statement.setInt(5, progress.getCycle());
                    statement.setString(6, progress.getStatus().name());
                    statement.setLong(7, progress.getCompletedAt());
                    statement.executeUpdate();
                }
                replaceTasks(connection, progress);
                return null;
            }
        });
    }

    @Override
    public void insertEntitlementsIfAbsent(final Set<RewardEntitlement> entitlements) {
        requireTransaction();
        connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Void>() {
            @Override
            public Void execute(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO galaxy_quest_reward_entitlement(entitlement_key,participant_type,participant_id,quest_id,"
                        + "definition_version,cycle,reward_key,reward_type,reward_parameters_json,delivery_status) "
                        + "VALUES (?,?,?,?,?,?,?,?,?::jsonb,?) ON CONFLICT DO NOTHING")) {
                    for (RewardEntitlement entitlement : entitlements) {
                        statement.setString(1, entitlement.getEntitlementKey());
                        statement.setString(2, entitlement.getParticipantId().getType().name());
                        statement.setObject(3, entitlement.getParticipantId().getId());
                        statement.setObject(4, entitlement.getQuestId());
                        statement.setInt(5, entitlement.getQuestVersion());
                        statement.setInt(6, entitlement.getCycle());
                        statement.setString(7, entitlement.getReward().getKey());
                        statement.setString(8, entitlement.getReward().getTypeId());
                        statement.setString(9, gson.toJson(entitlement.getReward().getParameters()));
                        statement.setString(10, entitlement.getInitialDeliveryStatus().name());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                return null;
            }
        });
    }

    private Map<String, TaskProgress> readTasks(Connection connection, ParticipantId participantId, UUID questId,
        int version) throws SQLException {
        Map<String, TaskProgress> tasks = new LinkedHashMap<String, TaskProgress>();
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT task_key,progress_values,target_values,complete FROM galaxy_quest_task_progress "
                + "WHERE participant_type=? AND participant_id=? AND quest_id=? AND definition_version=? ORDER BY task_key")) {
            bindIdentity(statement, participantId, questId, version);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String key = rows.getString(1);
                    tasks.put(key, new TaskProgress(key, longs(rows.getArray(2)), longs(rows.getArray(3)), rows.getBoolean(4)));
                }
            }
        }
        return tasks;
    }

    private void replaceTasks(Connection connection, QuestProgressSnapshot progress) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
            "DELETE FROM galaxy_quest_task_progress WHERE participant_type=? AND participant_id=? AND quest_id=? AND definition_version=?")) {
            bindIdentity(delete, progress.getParticipantId(), progress.getQuestId(), progress.getDefinitionVersion());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
            "INSERT INTO galaxy_quest_task_progress(participant_type,participant_id,quest_id,definition_version,task_key,"
                + "progress_values,target_values,complete) VALUES (?,?,?,?,?,?,?,?)")) {
            List<Array> arrays = new ArrayList<Array>();
            try {
                for (TaskProgress task : progress.getTasks().values()) {
                    bindIdentity(insert, progress.getParticipantId(), progress.getQuestId(), progress.getDefinitionVersion());
                    insert.setString(5, task.getTaskKey());
                    Array values = connection.createArrayOf("BIGINT", task.getValues().toArray(new Long[task.getValues().size()]));
                    Array targets = connection.createArrayOf("BIGINT", task.getTargets().toArray(new Long[task.getTargets().size()]));
                    arrays.add(values);
                    arrays.add(targets);
                    insert.setArray(6, values);
                    insert.setArray(7, targets);
                    insert.setBoolean(8, task.isComplete());
                    insert.addBatch();
                }
                insert.executeBatch();
            } finally {
                for (Array array : arrays) array.free();
            }
        }
    }

    private static void bindIdentity(PreparedStatement statement, ParticipantId participantId, UUID questId,
        int version) throws SQLException {
        statement.setString(1, participantId.getType().name());
        statement.setObject(2, participantId.getId());
        statement.setObject(3, questId);
        statement.setInt(4, version);
    }

    private static void lockProgressIdentity(Connection connection, ParticipantId participantId, UUID questId,
        int version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
            statement.setString(1, participantId.asStableKey() + ":" + questId + ":" + version);
            statement.executeQuery().close();
        }
    }

    private static List<Long> longs(Array array) throws SQLException {
        if (array == null) return Collections.emptyList();
        try {
            Object raw = array.getArray();
            List<Long> result = new ArrayList<Long>();
            if (raw instanceof Object[]) {
                for (Object value : (Object[]) raw) result.add(((Number) value).longValue());
            }
            return result;
        } finally {
            array.free();
        }
    }

    private void requireTransaction() {
        if (connections.current() == null) {
            throw new QuestPersistenceException("progress and entitlement writes require an active quest transaction");
        }
    }
}
