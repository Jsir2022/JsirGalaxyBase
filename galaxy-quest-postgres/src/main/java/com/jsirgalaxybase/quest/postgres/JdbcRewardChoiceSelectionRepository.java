package com.jsirgalaxybase.quest.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.RewardChoiceSelectionRepository;
import com.jsirgalaxybase.quest.core.RewardChoiceSelectionStatus;

/** PostgreSQL authority for player choice selection before reward claiming. */
public final class JdbcRewardChoiceSelectionRepository implements RewardChoiceSelectionRepository {
    private final JdbcQuestConnectionManager connections;

    public JdbcRewardChoiceSelectionRepository(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public RewardChoiceSelectionStatus select(final String entitlementKey, final UUID playerId,
        final int choiceIndex, final long selectedAt) {
        return selectInternal(entitlementKey,playerId,choiceIndex,selectedAt,false);
    }

    @Override
    public RewardChoiceSelectionStatus selectForForcedClaim(final String entitlementKey, final UUID playerId,
        final int choiceIndex, final long selectedAt) {
        return selectInternal(entitlementKey,playerId,choiceIndex,selectedAt,true);
    }

    private RewardChoiceSelectionStatus selectInternal(final String entitlementKey, final UUID playerId,
        final int choiceIndex, final long selectedAt, final boolean allowQueuedFirstSelection) {
        requireTransaction();
        if (entitlementKey == null || entitlementKey.trim().isEmpty() || playerId == null) {
            throw new IllegalArgumentException("entitlementKey and playerId are required");
        }
        return connections.withConnection(connection -> {
            String inspect = "SELECT participant_type,recipient_player_id,reward_type,"
                + "COALESCE((reward_parameters_json->>'item.count')::integer,0),e.delivery_status,s.choice_index "
                + "FROM galaxy_quest_reward_entitlement e LEFT JOIN galaxy_quest_reward_choice_selection s "
                + "ON s.entitlement_key=e.entitlement_key WHERE e.entitlement_key=? FOR UPDATE OF e";
            try (PreparedStatement statement = connection.prepareStatement(inspect)) {
                statement.setString(1, entitlementKey);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) return RewardChoiceSelectionStatus.NOT_FOUND;
                    if (!playerId.equals(row.getObject(2))
                        || !"bq_standard:choice".equals(row.getString(3))
                        || choiceIndex < 0 || choiceIndex >= row.getInt(4)) return RewardChoiceSelectionStatus.INVALID;
                    String deliveryStatus = row.getString(5);
                    int existing = row.getInt(6);
                    boolean hasSelection = !row.wasNull();
                    if (hasSelection && existing == choiceIndex) return RewardChoiceSelectionStatus.ALREADY_SELECTED;
                    if (hasSelection) {
                        if (!"CLAIMABLE".equals(deliveryStatus)) return RewardChoiceSelectionStatus.CONFLICT;
                        try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE galaxy_quest_reward_choice_selection SET choice_index=?,selected_at=to_timestamp(?/1000.0) "
                                + "WHERE entitlement_key=?")) {
                            update.setInt(1, choiceIndex);
                            update.setLong(2, selectedAt);
                            update.setString(3, entitlementKey);
                            return update.executeUpdate() == 1 ? RewardChoiceSelectionStatus.RESELECTED
                                : RewardChoiceSelectionStatus.CONFLICT;
                        }
                    }
                    boolean claimable="CLAIMABLE".equals(deliveryStatus);
                    boolean queuedFirstSelectionAllowed=allowQueuedFirstSelection
                        &&("PENDING".equals(deliveryStatus)||"FAILED".equals(deliveryStatus));
                    if (!claimable&&!queuedFirstSelectionAllowed)return RewardChoiceSelectionStatus.INVALID;
                    try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO galaxy_quest_reward_choice_selection(entitlement_key,choice_index,selected_at) "
                            + "VALUES(?,?,to_timestamp(?/1000.0)) ON CONFLICT (entitlement_key) DO NOTHING")) {
                        insert.setString(1, entitlementKey);
                        insert.setInt(2, choiceIndex);
                        insert.setLong(3, selectedAt);
                        return insert.executeUpdate() == 1 ? RewardChoiceSelectionStatus.SELECTED
                            : RewardChoiceSelectionStatus.CONFLICT;
                    }
                }
            }
        });
    }

    @Override
    public RewardChoiceSelectionStatus selectLatest(final UUID questId, final int version, final String rewardKey,
        final UUID playerId, final int choiceIndex, final long selectedAt) {
        return connections.withConnection(connection -> {
            String key = null;
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT entitlement_key FROM galaxy_quest_reward_entitlement WHERE quest_id=? "
                    + "AND definition_version=? AND reward_key=? AND recipient_player_id=? "
                    + "AND delivery_status='CLAIMABLE' "
                    + "ORDER BY cycle,participant_type,participant_id LIMIT 1")) {
                statement.setObject(1, questId);
                statement.setInt(2, version);
                statement.setString(3, rewardKey);
                statement.setObject(4, playerId);
                try (ResultSet row = statement.executeQuery()) { if (row.next()) key = row.getString(1); }
            }
            return key == null ? RewardChoiceSelectionStatus.NOT_FOUND
                : select(key, playerId, choiceIndex, selectedAt);
        });
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "reward choice selection requires an active quest transaction");
    }
}
