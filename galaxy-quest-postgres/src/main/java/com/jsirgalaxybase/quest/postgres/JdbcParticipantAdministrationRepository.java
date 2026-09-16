package com.jsirgalaxybase.quest.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ParticipantAdministrationRepository;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantMemberRole;
import com.jsirgalaxybase.quest.core.ParticipantMutationStatus;

public final class JdbcParticipantAdministrationRepository implements ParticipantAdministrationRepository {
    private final JdbcQuestConnectionManager connections;

    public JdbcParticipantAdministrationRepository(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public ParticipantMutationStatus create(final ParticipantId participant, final String displayName,
        final UUID owner, final long changedAt) {
        requireTransaction();
        return connections.withConnection(connection -> {
            if (hasActiveMembership(connection, participant.getType().name(), owner)) {
                return ParticipantMutationStatus.ALREADY_ACTIVE;
            }
            try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO galaxy_quest_participant(participant_type,participant_id,display_name) "
                    + "VALUES (?,?,?) ON CONFLICT DO NOTHING")) {
                insert.setString(1, participant.getType().name());
                insert.setObject(2, participant.getId());
                insert.setString(3, displayName);
                if (insert.executeUpdate() != 1) return ParticipantMutationStatus.ALREADY_ACTIVE;
            }
            try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO galaxy_quest_participant_membership(participant_type,participant_id,player_id,"
                    + "member_role,joined_at) VALUES (?,?,?,'OWNER',to_timestamp(?/1000.0))")) {
                bindParticipant(insert, participant);
                insert.setObject(3, owner);
                insert.setLong(4, changedAt);
                insert.executeUpdate();
            }
            return ParticipantMutationStatus.APPLIED;
        });
    }

    @Override
    public ParticipantMutationStatus addMember(final ParticipantId participant, final UUID actor,
        final UUID playerId, final ParticipantMemberRole role, final long expectedRevision, final long changedAt) {
        requireTransaction();
        return connections.withConnection(connection -> {
            ParticipantMutationStatus state = lockAndAuthorize(connection, participant, actor, expectedRevision, false);
            if (state != ParticipantMutationStatus.APPLIED) return state;
            try (PreparedStatement check = connection.prepareStatement(
                "SELECT 1 FROM galaxy_quest_participant_membership WHERE participant_type=? AND player_id=? "
                    + "AND left_at IS NULL")) {
                check.setString(1, participant.getType().name());
                check.setObject(2, playerId);
                try (ResultSet row = check.executeQuery()) {
                    if (row.next()) return ParticipantMutationStatus.ALREADY_ACTIVE;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO galaxy_quest_participant_membership(participant_type,participant_id,player_id,"
                    + "member_role,joined_at,membership_version) VALUES (?,?,?,?,to_timestamp(?/1000.0),?)")) {
                bindParticipant(insert, participant);
                insert.setObject(3, playerId);
                insert.setString(4, role.name());
                insert.setLong(5, changedAt);
                insert.setLong(6, expectedRevision + 1L);
                insert.executeUpdate();
            }
            bumpRevision(connection, participant, expectedRevision, changedAt, false);
            return ParticipantMutationStatus.APPLIED;
        });
    }

    @Override
    public ParticipantMutationStatus removeMember(final ParticipantId participant, final UUID actor,
        final UUID playerId, final long expectedRevision, final long changedAt) {
        requireTransaction();
        return connections.withConnection(connection -> {
            ParticipantMutationStatus state = lockAndAuthorize(connection, participant, actor, expectedRevision,
                actor.equals(playerId));
            if (state != ParticipantMutationStatus.APPLIED) return state;
            String targetRole = activeRole(connection, participant, playerId);
            if (targetRole == null) return ParticipantMutationStatus.NOT_FOUND;
            if ("OWNER".equals(targetRole) && activeMemberCount(connection, participant) > 1) {
                return ParticipantMutationStatus.OWNER_TRANSFER_REQUIRED;
            }
            try (PreparedStatement update = connection.prepareStatement(
                "UPDATE galaxy_quest_participant_membership SET left_at=to_timestamp(?/1000.0),"
                    + "membership_version=membership_version+1 WHERE participant_type=? AND participant_id=? "
                    + "AND player_id=? AND left_at IS NULL")) {
                update.setLong(1, changedAt);
                update.setString(2, participant.getType().name());
                update.setObject(3, participant.getId());
                update.setObject(4, playerId);
                if (update.executeUpdate() != 1) return ParticipantMutationStatus.VERSION_CONFLICT;
            }
            bumpRevision(connection, participant, expectedRevision, changedAt, "OWNER".equals(targetRole));
            return ParticipantMutationStatus.APPLIED;
        });
    }

    @Override
    public ParticipantMutationStatus transferOwnership(final ParticipantId participant, final UUID owner,
        final UUID newOwner, final long expectedRevision, final long changedAt) {
        requireTransaction();
        return connections.withConnection(connection -> {
            ParticipantMutationStatus state = lockAndAuthorizeOwner(connection, participant, owner, expectedRevision);
            if (state != ParticipantMutationStatus.APPLIED) return state;
            String targetRole = activeRole(connection, participant, newOwner);
            if (targetRole == null) return ParticipantMutationStatus.NOT_FOUND;
            try (PreparedStatement update = connection.prepareStatement(
                "UPDATE galaxy_quest_participant_membership SET member_role=CASE WHEN player_id=? THEN 'OWNER' "
                    + "WHEN player_id=? THEN 'ADMIN' ELSE member_role END,membership_version=membership_version+1 "
                    + "WHERE participant_type=? AND participant_id=? AND player_id IN (?,?) AND left_at IS NULL")) {
                update.setObject(1, newOwner);
                update.setObject(2, owner);
                update.setString(3, participant.getType().name());
                update.setObject(4, participant.getId());
                update.setObject(5, owner);
                update.setObject(6, newOwner);
                if (update.executeUpdate() != 2) throw new QuestPersistenceException(
                    "ownership members changed while participant was locked");
            }
            bumpRevision(connection, participant, expectedRevision, changedAt, false);
            return ParticipantMutationStatus.APPLIED;
        });
    }

    private ParticipantMutationStatus lockAndAuthorize(Connection connection, ParticipantId participant, UUID actor,
        long expectedRevision, boolean selfLeave) throws SQLException {
        try (PreparedStatement lock = connection.prepareStatement(
            "SELECT revision,lifecycle FROM galaxy_quest_participant WHERE participant_type=? AND participant_id=? "
                + "FOR UPDATE")) {
            bindParticipant(lock, participant);
            try (ResultSet row = lock.executeQuery()) {
                if (!row.next() || !"ACTIVE".equals(row.getString(2))) return ParticipantMutationStatus.NOT_FOUND;
                if (row.getLong(1) != expectedRevision) return ParticipantMutationStatus.VERSION_CONFLICT;
            }
        }
        if (selfLeave && activeRole(connection, participant, actor) != null) return ParticipantMutationStatus.APPLIED;
        String role = activeRole(connection, participant, actor);
        return "OWNER".equals(role) || "ADMIN".equals(role) ? ParticipantMutationStatus.APPLIED
            : ParticipantMutationStatus.NOT_AUTHORIZED;
    }

    private ParticipantMutationStatus lockAndAuthorizeOwner(Connection connection, ParticipantId participant,
        UUID owner, long expectedRevision) throws SQLException {
        ParticipantMutationStatus state = lockAndAuthorize(connection, participant, owner, expectedRevision, false);
        if (state != ParticipantMutationStatus.APPLIED) return state;
        return "OWNER".equals(activeRole(connection, participant, owner)) ? ParticipantMutationStatus.APPLIED
            : ParticipantMutationStatus.NOT_AUTHORIZED;
    }

    private String activeRole(Connection connection, ParticipantId participant, UUID player) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT member_role FROM galaxy_quest_participant_membership WHERE participant_type=? "
                + "AND participant_id=? AND player_id=? AND left_at IS NULL FOR UPDATE")) {
            bindParticipant(statement, participant);
            statement.setObject(3, player);
            try (ResultSet row = statement.executeQuery()) { return row.next() ? row.getString(1) : null; }
        }
    }

    private int activeMemberCount(Connection connection, ParticipantId participant) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT COUNT(*) FROM galaxy_quest_participant_membership WHERE participant_type=? "
                + "AND participant_id=? AND left_at IS NULL")) {
            bindParticipant(statement, participant);
            try (ResultSet row = statement.executeQuery()) { row.next(); return row.getInt(1); }
        }
    }

    private boolean hasActiveMembership(Connection connection,String participantType,UUID player)throws SQLException{
        try(PreparedStatement statement=connection.prepareStatement(
            "SELECT 1 FROM galaxy_quest_participant_membership WHERE participant_type=? AND player_id=? AND left_at IS NULL")){
            statement.setString(1,participantType);statement.setObject(2,player);
            try(ResultSet row=statement.executeQuery()){return row.next();}
        }
    }

    private void bumpRevision(Connection connection, ParticipantId participant, long expectedRevision, long changedAt,
        boolean archive) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
            "UPDATE galaxy_quest_participant SET revision=revision+1,updated_at=to_timestamp(?/1000.0),lifecycle=? "
                + "WHERE participant_type=? AND participant_id=? AND revision=?")) {
            update.setLong(1, changedAt);
            update.setString(2, archive ? "ARCHIVED" : "ACTIVE");
            update.setString(3, participant.getType().name());
            update.setObject(4, participant.getId());
            update.setLong(5, expectedRevision);
            if (update.executeUpdate() != 1) throw new QuestPersistenceException("participant revision changed");
        }
    }

    private static void bindParticipant(PreparedStatement statement, ParticipantId participant) throws SQLException {
        statement.setString(1, participant.getType().name());
        statement.setObject(2, participant.getId());
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "participant membership changes require an active quest transaction");
    }
}
