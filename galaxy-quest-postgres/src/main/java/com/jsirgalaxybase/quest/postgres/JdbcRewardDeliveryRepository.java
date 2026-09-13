package com.jsirgalaxybase.quest.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantType;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;
import com.jsirgalaxybase.quest.core.RewardDeliveryRepository;

public final class JdbcRewardDeliveryRepository implements RewardDeliveryRepository {
    private final JdbcQuestConnectionManager connections;
    private final Gson gson = new Gson();

    public JdbcRewardDeliveryRepository(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public List<RewardDeliveryLease> leaseAvailable(final String workerId, final long now, final long leaseUntil,
        final int limit) {
        requireTransaction();
        if (workerId == null || workerId.trim().isEmpty()) throw new IllegalArgumentException("workerId must not be blank");
        if (leaseUntil <= now) throw new IllegalArgumentException("leaseUntil must be after now");
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        return connections.withConnection(connection -> {
            String sql = "WITH candidates AS (SELECT e.entitlement_key,s.choice_index FROM galaxy_quest_reward_entitlement e "
                + "LEFT JOIN galaxy_quest_reward_choice_selection s ON s.entitlement_key=e.entitlement_key "
                + "WHERE (((e.delivery_status IN ('PENDING','FAILED') AND (e.next_attempt_at IS NULL OR e.next_attempt_at<=to_timestamp(?/1000.0))) "
                + "OR (e.delivery_status='DELIVERING' AND e.lease_until<to_timestamp(?/1000.0)))) "
                + "AND (e.reward_type<>'bq_standard:choice' OR s.entitlement_key IS NOT NULL) "
                + "ORDER BY e.created_at,e.entitlement_key FOR UPDATE OF e SKIP LOCKED LIMIT ?) "
                + "UPDATE galaxy_quest_reward_entitlement e SET delivery_status='DELIVERING',attempt_count=e.attempt_count+1,"
                + "lease_owner=?,lease_until=to_timestamp(?/1000.0),next_attempt_at=NULL,last_error=NULL FROM candidates c "
                + "WHERE e.entitlement_key=c.entitlement_key RETURNING e.entitlement_key,e.participant_type,e.participant_id,"
                + "e.reward_key,e.reward_type,e.reward_parameters_json::text,e.attempt_count,c.choice_index";
            List<RewardDeliveryLease> result = new ArrayList<RewardDeliveryLease>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, now);
                statement.setLong(2, now);
                statement.setInt(3, limit);
                statement.setString(4, workerId);
                statement.setLong(5, leaseUntil);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        Map<String, String> rewardParameters = parameters(rows.getString(6));
                        int selectedChoice = rows.getInt(8);
                        if (!rows.wasNull()) rewardParameters.put("choice.index", Integer.toString(selectedChoice));
                        RewardDeliveryLease lease = new RewardDeliveryLease(rows.getString(1),
                            new ParticipantId(ParticipantType.valueOf(rows.getString(2)), (java.util.UUID) rows.getObject(3)),
                            new RewardDefinition(rows.getString(4), rows.getString(5), rewardParameters),
                            rows.getInt(7), workerId, leaseUntil);
                        expirePreviousAttempt(connection, lease, now);
                        insertAttempt(connection, lease, now);
                        result.add(lease);
                    }
                }
            }
            return result;
        });
    }

    @Override
    public boolean markDelivered(final String entitlementKey, final String workerId, final int attempt,
        final long deliveredAt) {
        requireTransaction();
        return complete(entitlementKey, workerId, attempt, deliveredAt, "DELIVERED", null, 0L);
    }

    @Override
    public boolean markFailed(final String entitlementKey, final String workerId, final int attempt,
        final String error, final long failedAt, final long retryAt, final boolean retryable) {
        requireTransaction();
        return complete(entitlementKey, workerId, attempt, failedAt,
            retryable ? "FAILED" : "ABANDONED", error, retryAt);
    }

    private boolean complete(final String key, final String worker, final int attempt, final long completedAt,
        final String status, final String error, final long retryAt) {
        return connections.withConnection(connection -> {
            String sql = "UPDATE galaxy_quest_reward_entitlement SET delivery_status=?,lease_owner=NULL,lease_until=NULL,"
                + "next_attempt_at=" + ("FAILED".equals(status) ? "to_timestamp(?/1000.0)" : "NULL")
                + ",last_error=?,delivered_at=" + ("DELIVERED".equals(status) ? "to_timestamp(?/1000.0)" : "NULL")
                + " WHERE entitlement_key=? AND delivery_status='DELIVERING' AND lease_owner=? AND attempt_count=?";
            int updated;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                statement.setString(index++, status);
                if ("FAILED".equals(status)) statement.setLong(index++, retryAt);
                statement.setString(index++, error);
                if ("DELIVERED".equals(status)) statement.setLong(index++, completedAt);
                statement.setString(index++, key);
                statement.setString(index++, worker);
                statement.setInt(index, attempt);
                updated = statement.executeUpdate();
            }
            if (updated == 1) updateAttempt(connection, key, attempt, status, completedAt, error);
            return updated == 1;
        });
    }

    private void insertAttempt(Connection connection, RewardDeliveryLease lease, long startedAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO galaxy_quest_reward_delivery_attempt(entitlement_key,attempt_no,worker_id,status,started_at) "
                + "VALUES (?,?,?,'STARTED',to_timestamp(?/1000.0))")) {
            statement.setString(1, lease.getEntitlementKey());
            statement.setInt(2, lease.getAttempt());
            statement.setString(3, lease.getWorkerId());
            statement.setLong(4, startedAt);
            statement.executeUpdate();
        }
    }

    private void expirePreviousAttempt(Connection connection, RewardDeliveryLease lease, long now) throws SQLException {
        if (lease.getAttempt() <= 1) return;
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE galaxy_quest_reward_delivery_attempt SET status='FAILED',completed_at=to_timestamp(?/1000.0),"
                + "error_text='lease expired before confirmation' WHERE entitlement_key=? AND attempt_no=? AND status='STARTED'")) {
            statement.setLong(1, now);
            statement.setString(2, lease.getEntitlementKey());
            statement.setInt(3, lease.getAttempt() - 1);
            statement.executeUpdate();
        }
    }

    private void updateAttempt(Connection connection, String key, int attempt, String status, long completedAt,
        String error) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE galaxy_quest_reward_delivery_attempt SET status=?,completed_at=to_timestamp(?/1000.0),error_text=? "
                + "WHERE entitlement_key=? AND attempt_no=? AND status='STARTED'")) {
            statement.setString(1, status);
            statement.setLong(2, completedAt);
            statement.setString(3, error);
            statement.setString(4, key);
            statement.setInt(5, attempt);
            if (statement.executeUpdate() != 1) throw new QuestPersistenceException("delivery attempt no longer active");
        }
    }

    private Map<String, String> parameters(String json) {
        JsonObject object = gson.fromJson(json, JsonObject.class);
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) result.put(entry.getKey(), entry.getValue().getAsString());
        return result;
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "reward delivery mutations require an active quest transaction");
    }
}
