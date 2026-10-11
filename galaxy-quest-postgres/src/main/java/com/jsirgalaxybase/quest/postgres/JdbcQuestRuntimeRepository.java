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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
    public List<QuestProgressSnapshot> findLockedProgress(final ParticipantId participantId) {
        if (participantId == null) throw new IllegalArgumentException("participant is required");
        requireTransaction();
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<List<QuestProgressSnapshot>>() {
            @Override
            public List<QuestProgressSnapshot> execute(Connection connection) throws SQLException {
                Map<String, LockedProgressAccumulator> locked = new LinkedHashMap<String, LockedProgressAccumulator>();
                String sql = "SELECT p.quest_id,p.definition_version,p.completed_at,p.cycle,"
                    + "t.task_key,t.progress_values,t.target_values,t.complete,t.source_progress_json "
                    + "FROM galaxy_quest_progress p LEFT JOIN galaxy_quest_task_progress t "
                    + "ON t.participant_type=p.participant_type AND t.participant_id=p.participant_id "
                    + "AND t.quest_id=p.quest_id AND t.definition_version=p.definition_version "
                    + "WHERE p.participant_type=? AND p.participant_id=? AND p.status='LOCKED' "
                    + "ORDER BY p.quest_id,p.definition_version,t.task_key FOR UPDATE OF p";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, participantId.getType().name());
                    statement.setObject(2, participantId.getId());
                    try (ResultSet rows = statement.executeQuery()) {
                        while (rows.next()) {
                            UUID questId = UUID.fromString(rows.getString(1));
                            int version = rows.getInt(2);
                            String identity = questId + ":" + version;
                            LockedProgressAccumulator value = locked.get(identity);
                            if (value == null) {
                                value = new LockedProgressAccumulator(questId, version, rows.getLong(3), rows.getInt(4));
                                locked.put(identity, value);
                            }
                            String taskKey = rows.getString(5);
                            if (taskKey != null) {
                                value.tasks.put(taskKey, readTaskProgress(taskKey, rows.getArray(6),
                                    rows.getArray(7), rows.getBoolean(8), rows.getString(9)));
                            }
                        }
                    }
                }
                List<QuestProgressSnapshot> result = new ArrayList<QuestProgressSnapshot>(locked.size());
                for (LockedProgressAccumulator value : locked.values()) {
                    result.add(new QuestProgressSnapshot(participantId, value.questId, value.version,
                        QuestStatus.LOCKED, value.tasks, value.completedAt, value.cycle));
                }
                return Collections.unmodifiableList(result);
            }
        });
    }

    @Override
    public boolean wasEverCompleted(final ParticipantId participantId, final UUID questId,
        final int definitionVersion) {
        if (participantId == null || questId == null || definitionVersion < 1) {
            throw new IllegalArgumentException("valid progress identity is required");
        }
        requireTransaction();
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Boolean>() {
            @Override
            public Boolean execute(Connection connection) throws SQLException {
                lockProgressIdentity(connection, participantId, questId, definitionVersion);
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT ever_completed FROM galaxy_quest_progress "
                        + "WHERE participant_type=? AND participant_id=? AND quest_id=? AND definition_version=?")) {
                    bindIdentity(statement, participantId, questId, definitionVersion);
                    try (ResultSet rows = statement.executeQuery()) {
                        return Boolean.valueOf(rows.next() && rows.getBoolean(1));
                    }
                }
            }
        }).booleanValue();
    }

    @Override
    public boolean hasPendingConsumptionSubmission(final ParticipantId participantId, final UUID questId,
        final int definitionVersion, final int cycle) {
        requireTransaction();
        if (participantId == null || questId == null || definitionVersion < 1 || cycle < 0) {
            throw new IllegalArgumentException("valid progress identity and cycle are required");
        }
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Boolean>() {
            @Override
            public Boolean execute(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM galaxy_quest_consumption_submission "
                        + "WHERE participant_type=? AND participant_id=? AND quest_id=? AND definition_version=? "
                        + "AND submission_status IN ('PREPARED','APPLIED') "
                        + "AND COALESCE(resource_parameters_json->>'galaxy.quest.cycle','0')=? LIMIT 1")) {
                    statement.setString(1, participantId.getType().name());
                    statement.setObject(2, participantId.getId());
                    statement.setObject(3, questId);
                    statement.setInt(4, definitionVersion);
                    statement.setString(5, Integer.toString(cycle));
                    try (ResultSet rows = statement.executeQuery()) {
                        return Boolean.valueOf(rows.next());
                    }
                }
            }
        }).booleanValue();
    }

    @Override
    public boolean hasActiveConsumptionDetectBatch(final ParticipantId participantId, final UUID questId,
        final int definitionVersion) {
        requireTransaction();
        if (participantId == null || questId == null || definitionVersion < 1) throw new IllegalArgumentException(
            "valid progress identity is required");
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM galaxy_quest_consumption_detect_batch WHERE participant_type=? AND participant_id=? "
                    + "AND quest_id=? AND definition_version=? AND batch_status='ACTIVE' LIMIT 1")) {
                statement.setString(1, participantId.getType().name());
                statement.setObject(2, participantId.getId());
                statement.setObject(3, questId);
                statement.setInt(4, definitionVersion);
                try (ResultSet rows = statement.executeQuery()) { return Boolean.valueOf(rows.next()); }
            }
        }).booleanValue();
    }

    @Override
    public boolean hasClaimableRewardEntitlement(final ParticipantId participantId, final UUID questId,
        final int definitionVersion, final int cycle) {
        requireTransaction();
        if (participantId == null || questId == null || definitionVersion < 1 || cycle < 0) {
            throw new IllegalArgumentException("valid progress identity and cycle are required");
        }
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Boolean>() {
            @Override
            public Boolean execute(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM galaxy_quest_reward_entitlement WHERE participant_type=? AND participant_id=? "
                        + "AND quest_id=? AND definition_version=? AND cycle=? AND delivery_status='CLAIMABLE' LIMIT 1")) {
                    statement.setString(1, participantId.getType().name());
                    statement.setObject(2, participantId.getId());
                    statement.setObject(3, questId);
                    statement.setInt(4, definitionVersion);
                    statement.setInt(5, cycle);
                    try (ResultSet rows = statement.executeQuery()) { return Boolean.valueOf(rows.next()); }
                }
            }
        }).booleanValue();
    }

    @Override
    public long findRepeatCycleAnchorMillis(final ParticipantId participantId, final UUID questId,
        final int definitionVersion, final int cycle, final long fallbackCompletedAt) {
        requireTransaction();
        if (participantId == null || questId == null || definitionVersion < 1 || cycle < 0
            || fallbackCompletedAt < 0L) {
            throw new IllegalArgumentException("valid progress identity, cycle, and fallback time are required");
        }
        Long claimedAt = connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Long>() {
            @Override
            public Long execute(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT floor(extract(epoch FROM max(claimed_at))*1000)::bigint "
                        + "FROM galaxy_quest_reward_entitlement WHERE participant_type=? AND participant_id=? "
                        + "AND quest_id=? AND definition_version=? AND cycle=?")) {
                    statement.setString(1, participantId.getType().name());
                    statement.setObject(2, participantId.getId());
                    statement.setObject(3, questId);
                    statement.setInt(4, definitionVersion);
                    statement.setInt(5, cycle);
                    try (ResultSet rows = statement.executeQuery()) {
                        if (!rows.next()) return null;
                        long value = rows.getLong(1);
                        return rows.wasNull() ? null : Long.valueOf(value);
                    }
                }
            }
        });
        return claimedAt == null ? fallbackCompletedAt : claimedAt.longValue();
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
                    statement.setString(6, gson.toJson(persistedFactAttributes(fact)));
                    return statement.executeUpdate() == 1;
                }
            }
        });
    }

    /** Inventory attributes are evaluation input, not durable history; keep only non-sensitive audit metadata. */
    private static Map<String, String> persistedFactAttributes(GameplayFact fact) {
        if (!"galaxy:inventory_observed".equals(fact.getTypeId())) return fact.getAttributes();
        Map<String, String> persisted = new LinkedHashMap<String, String>();
        String count = fact.getAttributes().get("inventory.count");
        if (count != null) persisted.put("inventory.count", count);
        persisted.put("inventory.snapshotRedacted", "true");
        String partySnapshot = fact.getAttributes().get("inventory.bqPartySnapshot");
        String partyId = fact.getAttributes().get("inventory.bqPartyId");
        if (partySnapshot != null) persisted.put("inventory.bqPartySnapshot", partySnapshot);
        if (partyId != null) persisted.put("inventory.bqPartyId", partyId);
        return persisted;
    }

    @Override
    public void saveProgress(final QuestProgressSnapshot progress) {
        requireTransaction();
        connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Void>() {
            @Override
            public Void execute(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO galaxy_quest_progress(participant_type,participant_id,quest_id,definition_version,cycle,status,completed_at,ever_completed) "
                        + "VALUES (?,?,?,?,?,?,?,?) ON CONFLICT (participant_type,participant_id,quest_id,definition_version) "
                        + "DO UPDATE SET cycle=EXCLUDED.cycle,status=EXCLUDED.status,completed_at=EXCLUDED.completed_at,"
                        + "ever_completed=galaxy_quest_progress.ever_completed OR EXCLUDED.ever_completed,"
                        + "revision=galaxy_quest_progress.revision+1,updated_at=CURRENT_TIMESTAMP")) {
                    bindIdentity(statement, progress.getParticipantId(), progress.getQuestId(), progress.getDefinitionVersion());
                    statement.setInt(5, progress.getCycle());
                    statement.setString(6, progress.getStatus().name());
                    statement.setLong(7, progress.getCompletedAt());
                    statement.setBoolean(8, progress.getStatus() == QuestStatus.COMPLETED);
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
                    "INSERT INTO galaxy_quest_reward_entitlement(entitlement_key,participant_type,participant_id,"
                        + "recipient_player_id,quest_id,definition_version,cycle,reward_key,reward_type,"
                        + "reward_parameters_json,delivery_status) VALUES (?,?,?,?,?,?,?,?,?,?::jsonb,?) "
                        + "ON CONFLICT DO NOTHING")) {
                    for (RewardEntitlement entitlement : entitlements) {
                        statement.setString(1, entitlement.getEntitlementKey());
                        statement.setString(2, entitlement.getParticipantId().getType().name());
                        statement.setObject(3, entitlement.getParticipantId().getId());
                        statement.setObject(4, entitlement.getRecipientPlayerId());
                        statement.setObject(5, entitlement.getQuestId());
                        statement.setInt(6, entitlement.getQuestVersion());
                        statement.setInt(7, entitlement.getCycle());
                        statement.setString(8, entitlement.getReward().getKey());
                        statement.setString(9, entitlement.getReward().getTypeId());
                        statement.setString(10, gson.toJson(entitlement.getReward().getParameters()));
                        statement.setString(11, entitlement.getInitialDeliveryStatus().name());
                        boolean inserted = statement.executeUpdate() == 1;
                        if (inserted && "bq_standard:choice".equals(entitlement.getReward().getTypeId())
                            && entitlement.getCycle() > 0) {
                            inheritChoiceFromPreviousCycle(connection, entitlement.getEntitlementKey());
                        }
                    }
                }
                return null;
            }
        });
    }

    private static void inheritChoiceFromPreviousCycle(Connection connection, String entitlementKey)
        throws SQLException {
        String sql = "INSERT INTO galaxy_quest_reward_choice_selection(entitlement_key,choice_index,selected_at) "
            + "SELECT fresh.entitlement_key,previous.choice_index,previous.selected_at "
            + "FROM galaxy_quest_reward_entitlement fresh "
            + "JOIN LATERAL (SELECT selection.choice_index,selection.selected_at "
            + "FROM galaxy_quest_reward_entitlement prior "
            + "JOIN galaxy_quest_reward_choice_selection selection ON selection.entitlement_key=prior.entitlement_key "
            + "WHERE prior.quest_id=fresh.quest_id AND prior.definition_version=fresh.definition_version "
            + "AND prior.reward_key=fresh.reward_key AND prior.reward_type='bq_standard:choice' "
            + "AND prior.recipient_player_id=fresh.recipient_player_id AND prior.cycle<fresh.cycle "
            + "ORDER BY prior.cycle DESC LIMIT 1) previous "
            + "ON previous.choice_index<COALESCE((fresh.reward_parameters_json->>'item.count')::integer,0) "
            + "WHERE fresh.entitlement_key=? AND fresh.reward_type='bq_standard:choice' "
            + "ON CONFLICT (entitlement_key) DO NOTHING";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entitlementKey);
            statement.executeUpdate();
        }
    }

    private Map<String, TaskProgress> readTasks(Connection connection, ParticipantId participantId, UUID questId,
        int version) throws SQLException {
        Map<String, TaskProgress> tasks = new LinkedHashMap<String, TaskProgress>();
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT task_key,progress_values,target_values,complete,source_progress_json "
                + "FROM galaxy_quest_task_progress "
                + "WHERE participant_type=? AND participant_id=? AND quest_id=? AND definition_version=? ORDER BY task_key")) {
            bindIdentity(statement, participantId, questId, version);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String key = rows.getString(1);
                    tasks.put(key, readTaskProgress(key, rows.getArray(2), rows.getArray(3), rows.getBoolean(4),
                        rows.getString(5)));
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
                + "progress_values,target_values,complete,source_progress_json) VALUES (?,?,?,?,?,?,?,?,?::jsonb)")) {
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
                    insert.setString(9, encodeSourceProgress(task));
                    insert.addBatch();
                }
                insert.executeBatch();
            } finally {
                for (Array array : arrays) array.free();
            }
        }
    }

    private TaskProgress readTaskProgress(String taskKey, Array valuesArray, Array targetsArray, boolean complete,
        String sourceProgressJson) throws SQLException {
        List<Long> values = longs(valuesArray);
        List<Long> targets = longs(targetsArray);
        if (sourceProgressJson == null || sourceProgressJson.trim().isEmpty()) {
            return new TaskProgress(taskKey, values, targets, complete);
        }
        try {
            JsonObject state = new JsonParser().parse(sourceProgressJson).getAsJsonObject();
            if (!state.has("legacyBaseline") || !state.has("sources")) {
                return new TaskProgress(taskKey, values, targets, complete);
            }
            List<Long> baseline = jsonLongs(state.getAsJsonArray("legacyBaseline"));
            JsonObject sources = state.getAsJsonObject("sources");
            Map<String, TaskProgress.SourceContribution> contributions = new LinkedHashMap<String,
                TaskProgress.SourceContribution>();
            for (Map.Entry<String, JsonElement> entry : sources.entrySet()) {
                JsonObject source = entry.getValue().getAsJsonObject();
                contributions.put(entry.getKey(), new TaskProgress.SourceContribution(
                    jsonLongs(source.getAsJsonArray("events")), jsonLongs(source.getAsJsonArray("observed"))));
            }
            TaskProgress restored = TaskProgress.restoreSourceScoped(taskKey, targets, complete, baseline,
                contributions);
            if (!values.equals(restored.getValues())) {
                throw new SQLException("persisted aggregate does not match source-scoped progress for task "
                    + taskKey);
            }
            return restored;
        } catch (RuntimeException invalid) {
            throw new SQLException("invalid source-scoped progress JSON for task " + taskKey, invalid);
        }
    }

    private String encodeSourceProgress(TaskProgress task) {
        if (!task.isSourceScoped()) return "{}";
        JsonObject state = new JsonObject();
        state.add("legacyBaseline", jsonLongs(task.getSourceBaselineValues()));
        JsonObject sources = new JsonObject();
        for (Map.Entry<String, TaskProgress.SourceContribution> entry : task.getSourceContributions().entrySet()) {
            JsonObject source = new JsonObject();
            source.add("events", jsonLongs(entry.getValue().getEventValues()));
            source.add("observed", jsonLongs(entry.getValue().getObservedValues()));
            sources.add(entry.getKey(), source);
        }
        state.add("sources", sources);
        return gson.toJson(state);
    }

    private static JsonArray jsonLongs(List<Long> values) {
        JsonArray result = new JsonArray();
        for (Long value : values) result.add(value);
        return result;
    }

    private static List<Long> jsonLongs(JsonArray values) {
        if (values == null) throw new IllegalArgumentException("progress vector is missing");
        List<Long> result = new ArrayList<Long>(values.size());
        for (JsonElement value : values) result.add(Long.valueOf(value.getAsLong()));
        return result;
    }

    private static final class LockedProgressAccumulator {
        private final UUID questId;
        private final int version;
        private final long completedAt;
        private final int cycle;
        private final Map<String, TaskProgress> tasks = new LinkedHashMap<String, TaskProgress>();

        private LockedProgressAccumulator(UUID questId, int version, long completedAt, int cycle) {
            this.questId = questId;
            this.version = version;
            this.completedAt = completedAt;
            this.cycle = cycle;
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
