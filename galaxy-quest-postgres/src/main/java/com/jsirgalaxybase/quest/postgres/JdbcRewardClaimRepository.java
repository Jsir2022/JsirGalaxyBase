package com.jsirgalaxybase.quest.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.OptionalInt;
import java.util.Optional;

import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantType;
import com.jsirgalaxybase.quest.core.RewardClaimRepository;
import com.jsirgalaxybase.quest.core.RewardClaimStatus;
import com.jsirgalaxybase.quest.core.RewardClaimTarget;

public final class JdbcRewardClaimRepository implements RewardClaimRepository {
    private final JdbcQuestConnectionManager connections;

    public JdbcRewardClaimRepository(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public RewardClaimStatus claim(final UUID questId, final int version, final int cycle,
        final UUID currentPlayerId, final long claimedAt) {
        return claimInternal(null, questId, version, cycle, currentPlayerId, claimedAt);
    }

    @Override
    public RewardClaimStatus claim(final RewardClaimTarget target, final UUID questId, final int version,
        final UUID currentPlayerId, final long claimedAt) {
        if (target == null) throw new IllegalArgumentException("target is required");
        return claimInternal(target.getParticipantId(), questId, version, target.getCycle(), currentPlayerId, claimedAt);
    }

    private RewardClaimStatus claimInternal(final ParticipantId subject, final UUID questId, final int version,
        final int cycle, final UUID currentPlayerId, final long claimedAt) {
        requireTransaction();
        if (questId == null || currentPlayerId == null || version < 1 || cycle < 0 || claimedAt < 0L) {
            throw new IllegalArgumentException("valid quest identity, cycle, and current player are required");
        }
        return connections.withConnection(connection -> {
            String subjectFilter = subject == null ? "" : "AND e.participant_type=? AND e.participant_id=? ";
            String inspect = "SELECT delivery_status,reward_type,s.choice_index "
                + "FROM galaxy_quest_reward_entitlement e LEFT JOIN galaxy_quest_reward_choice_selection s "
                + "ON s.entitlement_key=e.entitlement_key WHERE e.quest_id=? AND e.definition_version=? AND e.cycle=? "
                + "AND e.recipient_player_id=? " + subjectFilter + "ORDER BY e.entitlement_key FOR UPDATE OF e";
            int rows = 0;
            boolean claimable = false;
            boolean alreadyClaimed = true;
            try (PreparedStatement statement = connection.prepareStatement(inspect)) {
                statement.setObject(1, questId);
                statement.setInt(2, version);
                statement.setInt(3, cycle);
                statement.setObject(4, currentPlayerId);
                if (subject != null) { statement.setString(5, subject.getType().name()); statement.setObject(6, subject.getId()); }
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
            if (rows == 0) return existsForOtherPlayer(connection, subject, questId, version, cycle, currentPlayerId)
                ? RewardClaimStatus.NOT_OWNED : RewardClaimStatus.NOT_FOUND;
            if (!claimable && alreadyClaimed) return RewardClaimStatus.ALREADY_CLAIMED;
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE galaxy_quest_reward_entitlement SET delivery_status='PENDING',next_attempt_at=NULL,"
                    + "claimed_at=to_timestamp(?/1000.0) "
                    + "WHERE quest_id=? AND definition_version=? AND cycle=? AND recipient_player_id=? "
                    + (subject == null ? "" : "AND participant_type=? AND participant_id=? ")
                    + "AND delivery_status='CLAIMABLE'")) {
                statement.setLong(1, claimedAt);
                statement.setObject(2, questId);
                statement.setInt(3, version);
                statement.setInt(4, cycle);
                statement.setObject(5, currentPlayerId);
                if (subject != null) { statement.setString(6, subject.getType().name()); statement.setObject(7, subject.getId()); }
                if (statement.executeUpdate() < 1) return RewardClaimStatus.INVALID_STATE;
            }
            return RewardClaimStatus.CLAIMED;
        });
    }

    private boolean existsForOtherPlayer(java.sql.Connection connection, ParticipantId subject, UUID questId, int version, int cycle,
        UUID currentPlayerId)
        throws java.sql.SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT 1 FROM galaxy_quest_reward_entitlement WHERE quest_id=? AND definition_version=? AND cycle=? "
                + "AND recipient_player_id<>? " + (subject == null ? "" : "AND participant_type=? AND participant_id=? ") + "LIMIT 1")) {
            statement.setObject(1, questId);
            statement.setInt(2, version);
            statement.setInt(3, cycle);
            statement.setObject(4, currentPlayerId);
            if (subject != null) { statement.setString(5, subject.getType().name()); statement.setObject(6, subject.getId()); }
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    @Override
    public Optional<RewardClaimTarget> findNextRecipientTarget(final UUID questId, final int version,
        final UUID playerId) {
        return connections.withConnection(connection -> {
            String sql = "SELECT participant_type,participant_id,cycle FROM galaxy_quest_reward_entitlement "
                + "WHERE quest_id=? AND definition_version=? AND recipient_player_id=? AND delivery_status='CLAIMABLE' "
                + "GROUP BY participant_type,participant_id,cycle ORDER BY cycle,participant_type,participant_id LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, questId); statement.setInt(2, version); statement.setObject(3, playerId);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) return Optional.empty();
                    return Optional.of(new RewardClaimTarget(new ParticipantId(
                        ParticipantType.valueOf(row.getString(1)), (UUID) row.getObject(2)), row.getInt(3)));
                }
            }
        });
    }

    @Override
    public OptionalInt findLatestRecipientCycle(final UUID questId, final int version, final UUID playerId) {
        return connections.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT MAX(cycle) FROM galaxy_quest_reward_entitlement WHERE quest_id=? AND definition_version=? "
                    + "AND recipient_player_id=?")) {
                statement.setObject(1, questId);
                statement.setInt(2, version);
                statement.setObject(3, playerId);
                try (ResultSet row = statement.executeQuery()) {
                    if (!row.next()) return OptionalInt.empty();
                    int cycle = row.getInt(1);
                    return row.wasNull() ? OptionalInt.empty() : OptionalInt.of(cycle);
                }
            }
        });
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "reward claim requires an active quest transaction");
    }
}
