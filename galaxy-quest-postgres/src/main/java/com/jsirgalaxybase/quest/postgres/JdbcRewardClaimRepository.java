package com.jsirgalaxybase.quest.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.RewardClaimRepository;
import com.jsirgalaxybase.quest.core.RewardClaimStatus;

public final class JdbcRewardClaimRepository implements RewardClaimRepository {
    private final JdbcQuestConnectionManager connections;

    public JdbcRewardClaimRepository(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public RewardClaimStatus claim(final UUID questId, final int version, final int cycle,
        final UUID currentPlayerId, final long claimedAt) {
        requireTransaction();
        if (questId == null || currentPlayerId == null || version < 1 || cycle < 0 || claimedAt < 0L) {
            throw new IllegalArgumentException("valid quest identity, cycle, and current player are required");
        }
        return connections.withConnection(connection -> {
            String inspect = "SELECT delivery_status,reward_type,s.choice_index "
                + "FROM galaxy_quest_reward_entitlement e LEFT JOIN galaxy_quest_reward_choice_selection s "
                + "ON s.entitlement_key=e.entitlement_key WHERE e.quest_id=? AND e.definition_version=? AND e.cycle=? "
                + "AND e.participant_type='PLAYER' AND e.participant_id=? ORDER BY e.entitlement_key FOR UPDATE OF e";
            int rows = 0;
            boolean claimable = false;
            boolean alreadyClaimed = true;
            try (PreparedStatement statement = connection.prepareStatement(inspect)) {
                statement.setObject(1, questId);
                statement.setInt(2, version);
                statement.setInt(3, cycle);
                statement.setObject(4, currentPlayerId);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        rows++;
                        String status = result.getString(1);
                        claimable |= "CLAIMABLE".equals(status);
                        alreadyClaimed &= !"CLAIMABLE".equals(status);
                        if ("CLAIMABLE".equals(status) && "bq_standard:choice".equals(result.getString(2))) {
                            result.getInt(3);
                            if (result.wasNull()) return RewardClaimStatus.NEEDS_CHOICE;
                        }
                        if (!"CLAIMABLE".equals(status) && !"PENDING".equals(status)
                            && !"DELIVERING".equals(status) && !"DELIVERED".equals(status)
                            && !"FAILED".equals(status) && !"ABANDONED".equals(status)) {
                            return RewardClaimStatus.INVALID_STATE;
                        }
                    }
                }
            }
            if (rows == 0) return existsForOtherPlayer(connection, questId, version, cycle)
                ? RewardClaimStatus.NOT_OWNED : RewardClaimStatus.NOT_FOUND;
            if (!claimable && alreadyClaimed) return RewardClaimStatus.ALREADY_CLAIMED;
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE galaxy_quest_reward_entitlement SET delivery_status='PENDING',next_attempt_at=NULL,"
                    + "claimed_at=to_timestamp(?/1000.0) "
                    + "WHERE quest_id=? AND definition_version=? AND cycle=? AND participant_type='PLAYER' "
                    + "AND participant_id=? AND delivery_status='CLAIMABLE'")) {
                statement.setLong(1, claimedAt);
                statement.setObject(2, questId);
                statement.setInt(3, version);
                statement.setInt(4, cycle);
                statement.setObject(5, currentPlayerId);
                if (statement.executeUpdate() < 1) return RewardClaimStatus.INVALID_STATE;
            }
            return RewardClaimStatus.CLAIMED;
        });
    }

    private boolean existsForOtherPlayer(java.sql.Connection connection, UUID questId, int version, int cycle)
        throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT 1 FROM galaxy_quest_reward_entitlement WHERE quest_id=? AND definition_version=? AND cycle=? "
                + "AND participant_type='PLAYER' LIMIT 1")) {
            statement.setObject(1, questId);
            statement.setInt(2, version);
            statement.setInt(3, cycle);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "reward claim requires an active quest transaction");
    }
}
