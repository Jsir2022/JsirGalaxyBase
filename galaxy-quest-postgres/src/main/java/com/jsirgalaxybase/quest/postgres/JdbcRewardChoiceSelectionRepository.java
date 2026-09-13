package com.jsirgalaxybase.quest.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.RewardChoiceSelectionRepository;
import com.jsirgalaxybase.quest.core.RewardChoiceSelectionStatus;

/** PostgreSQL authority for immutable player choice selection. */
public final class JdbcRewardChoiceSelectionRepository implements RewardChoiceSelectionRepository {
    private final JdbcQuestConnectionManager connections;

    public JdbcRewardChoiceSelectionRepository(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public RewardChoiceSelectionStatus select(final String entitlementKey, final UUID playerId,
        final int choiceIndex, final long selectedAt) {
        requireTransaction();
        if (entitlementKey == null || entitlementKey.trim().isEmpty() || playerId == null) {
            throw new IllegalArgumentException("entitlementKey and playerId are required");
        }
        return connections.withConnection(connection -> {
            String insert = "INSERT INTO galaxy_quest_reward_choice_selection(entitlement_key,choice_index,selected_at) "
                + "SELECT entitlement_key,?,to_timestamp(?/1000.0) FROM galaxy_quest_reward_entitlement "
                + "WHERE entitlement_key=? AND participant_type='PLAYER' AND participant_id=? "
                + "AND reward_type='bq_standard:choice' AND delivery_status IN ('CLAIMABLE','PENDING','FAILED') "
                + "AND ?>=0 AND ?<COALESCE((reward_parameters_json->>'item.count')::integer,0) "
                + "ON CONFLICT (entitlement_key) DO NOTHING";
            try (PreparedStatement statement = connection.prepareStatement(insert)) {
                statement.setInt(1, choiceIndex);
                statement.setLong(2, selectedAt);
                statement.setString(3, entitlementKey);
                statement.setObject(4, playerId);
                statement.setInt(5, choiceIndex);
                statement.setInt(6, choiceIndex);
                if (statement.executeUpdate() == 1) return RewardChoiceSelectionStatus.SELECTED;
            }
            String inspect = "SELECT participant_type,participant_id,reward_type,"
                + "COALESCE((reward_parameters_json->>'item.count')::integer,0),s.choice_index "
                + "FROM galaxy_quest_reward_entitlement e LEFT JOIN galaxy_quest_reward_choice_selection s "
                + "ON s.entitlement_key=e.entitlement_key WHERE e.entitlement_key=? FOR UPDATE OF e";
            try (PreparedStatement statement = connection.prepareStatement(inspect)) {
                statement.setString(1, entitlementKey);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) return RewardChoiceSelectionStatus.NOT_FOUND;
                    if (!"PLAYER".equals(row.getString(1)) || !playerId.equals(row.getObject(2))
                        || !"bq_standard:choice".equals(row.getString(3))
                        || choiceIndex < 0 || choiceIndex >= row.getInt(4)) return RewardChoiceSelectionStatus.INVALID;
                    int existing = row.getInt(5);
                    if (!row.wasNull()) return existing == choiceIndex ? RewardChoiceSelectionStatus.ALREADY_SELECTED
                        : RewardChoiceSelectionStatus.CONFLICT;
                    return RewardChoiceSelectionStatus.INVALID;
                }
            }
        });
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "reward choice selection requires an active quest transaction");
    }
}
