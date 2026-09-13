package com.jsirgalaxybase.quest.postgres;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jsirgalaxybase.quest.core.ConsumptionRequest;
import com.jsirgalaxybase.quest.core.ConsumptionSubmission;
import com.jsirgalaxybase.quest.core.ConsumptionSubmissionRepository;
import com.jsirgalaxybase.quest.core.ConsumptionSubmissionStatus;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantType;

public final class JdbcConsumptionSubmissionRepository implements ConsumptionSubmissionRepository {
    private final JdbcQuestConnectionManager connections;
    private final Gson gson = new Gson();

    public JdbcConsumptionSubmissionRepository(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public Optional<ConsumptionSubmission> find(final String submissionKey) {
        requireTransaction();
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM galaxy_quest_consumption_submission WHERE submission_key=? FOR UPDATE")) {
                statement.setString(1, submissionKey);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) return Optional.empty();
                }
            }
            return Optional.of(selectForUpdate(connection, submissionKey));
        });
    }

    @Override
    public ConsumptionSubmission prepareIfAbsent(final ConsumptionRequest request) {
        requireTransaction();
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO galaxy_quest_consumption_submission(submission_key,source_server,player_id,"
                    + "participant_type,participant_id,quest_id,definition_version,task_key,resource_kind,amount_values,"
                    + "resource_parameters_json,submission_status,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?::jsonb,"
                    + "'PREPARED',?) ON CONFLICT (submission_key) DO NOTHING")) {
                statement.setString(1, request.getSubmissionKey());
                statement.setString(2, request.getSourceServer());
                statement.setObject(3, request.getPlayerId());
                statement.setString(4, request.getParticipantId().getType().name());
                statement.setObject(5, request.getParticipantId().getId());
                statement.setObject(6, request.getQuestId());
                statement.setInt(7, request.getDefinitionVersion());
                statement.setString(8, request.getTaskKey());
                statement.setString(9, request.getKind());
                statement.setArray(10, longArray(connection, request.getAmounts()));
                statement.setString(11, gson.toJson(request.getResourceParameters()));
                statement.setLong(12, request.getCreatedAt());
                statement.executeUpdate();
            }
            ConsumptionSubmission stored = selectForUpdate(connection, request.getSubmissionKey());
            if (!same(stored.getRequest(), request)) throw new QuestPersistenceException(
                "consumption submission key reused with different content");
            return stored;
        });
    }

    @Override public ConsumptionSubmission markApplied(String key, String evidence, List<Long> appliedAmounts, long now) {
        requireTransaction();
        return connections.withConnection(connection -> {
            ConsumptionSubmission current = selectForUpdate(connection, key);
            if (appliedAmounts == null || appliedAmounts.size() != current.getRequest().getAmounts().size()) {
                throw new QuestPersistenceException("applied consumption vector width mismatch");
            }
            for (int i = 0; i < appliedAmounts.size(); i++) {
                long value = appliedAmounts.get(i) == null ? -1L : appliedAmounts.get(i);
                if (value < 0 || value > current.getRequest().getAmounts().get(i)) throw new QuestPersistenceException(
                    "invalid applied consumption amount");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE galaxy_quest_consumption_submission SET submission_status='APPLIED',evidence=?,"
                    + "applied_values=?,applied_at=? WHERE submission_key=? AND submission_status='PREPARED'")) {
                statement.setString(1, evidence);
                statement.setArray(2, longArray(connection, appliedAmounts));
                statement.setLong(3, now);
                statement.setString(4, key);
                statement.executeUpdate();
            }
            ConsumptionSubmission result = selectForUpdate(connection, key);
            if (result.getStatus() != ConsumptionSubmissionStatus.APPLIED
                && result.getStatus() != ConsumptionSubmissionStatus.CONFIRMED) throw new QuestPersistenceException(
                    "illegal consumption transition " + result.getStatus() + " -> APPLIED");
            return result;
        });
    }

    @Override public ConsumptionSubmission markConfirmed(String key, long now) {
        return transition(key, "APPLIED", "CONFIRMED", null, null, "confirmed_at", now);
    }

    @Override public ConsumptionSubmission markRejected(String key, String reason, long now) {
        return transition(key, "PREPARED", "REJECTED", "rejection_reason", reason, "rejected_at", now);
    }

    private ConsumptionSubmission transition(final String key, final String expected, final String next,
        final String textColumn, final String text, final String timeColumn, final long now) {
        requireTransaction();
        return connections.withConnection(connection -> {
            String sql = "UPDATE galaxy_quest_consumption_submission SET submission_status='" + next + "',"
                + timeColumn + "=?" + (textColumn == null ? "" : "," + textColumn + "=?")
                + " WHERE submission_key=? AND submission_status='" + expected + "'";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, now);
                int index = 2;
                if (textColumn != null) statement.setString(index++, text);
                statement.setString(index, key);
                statement.executeUpdate();
            }
            ConsumptionSubmission result = selectForUpdate(connection, key);
            if (!next.equals(result.getStatus().name())) {
                if (("APPLIED".equals(next) && result.getStatus() == ConsumptionSubmissionStatus.CONFIRMED)
                    || next.equals(result.getStatus().name())) return result;
                throw new QuestPersistenceException("illegal consumption transition " + result.getStatus() + " -> " + next);
            }
            return result;
        });
    }

    private ConsumptionSubmission selectForUpdate(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT source_server,player_id,participant_type,participant_id,quest_id,definition_version,task_key,"
                + "resource_kind,amount_values,resource_parameters_json::text,created_at,submission_status,"
                + "COALESCE(evidence,rejection_reason,''),applied_values FROM galaxy_quest_consumption_submission "
                + "WHERE submission_key=? FOR UPDATE")) {
            statement.setString(1, key);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new QuestPersistenceException("unknown consumption submission: " + key);
                ConsumptionRequest request = new ConsumptionRequest(key, row.getString(1), (UUID) row.getObject(2),
                    new ParticipantId(ParticipantType.valueOf(row.getString(3)), (UUID) row.getObject(4)),
                    (UUID) row.getObject(5), row.getInt(6), row.getString(7), row.getString(8),
                    longs(row.getArray(9)), parameters(row.getString(10)), row.getLong(11));
                Array applied = row.getArray(14);
                return new ConsumptionSubmission(request, ConsumptionSubmissionStatus.valueOf(row.getString(12)),
                    row.getString(13), applied == null ? java.util.Collections.<Long>emptyList() : longs(applied));
            }
        }
    }

    private Array longArray(Connection connection, List<Long> values) throws SQLException {
        return connection.createArrayOf("BIGINT", values.toArray(new Long[values.size()]));
    }

    private List<Long> longs(Array sqlArray) throws SQLException {
        Object[] values = (Object[]) sqlArray.getArray();
        List<Long> result = new ArrayList<Long>(values.length);
        for (Object value : values) result.add(((Number) value).longValue());
        return result;
    }

    private Map<String, String> parameters(String json) {
        JsonObject object = gson.fromJson(json, JsonObject.class);
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) result.put(entry.getKey(), entry.getValue().getAsString());
        return result;
    }

    private boolean same(ConsumptionRequest left, ConsumptionRequest right) {
        return left.getSubmissionKey().equals(right.getSubmissionKey())
            && left.getSourceServer().equals(right.getSourceServer()) && left.getPlayerId().equals(right.getPlayerId())
            && left.getParticipantId().equals(right.getParticipantId()) && left.getQuestId().equals(right.getQuestId())
            && left.getDefinitionVersion() == right.getDefinitionVersion() && left.getTaskKey().equals(right.getTaskKey())
            && left.getKind().equals(right.getKind()) && left.getAmounts().equals(right.getAmounts())
            && left.getResourceParameters().equals(right.getResourceParameters()) && left.getCreatedAt() == right.getCreatedAt();
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "consumption mutations require an active quest transaction");
    }
}
