package com.jsirgalaxybase.quest.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantMembership;
import com.jsirgalaxybase.quest.core.ParticipantMembershipResolver;
import com.jsirgalaxybase.quest.core.ParticipantType;

/** Reads the current cross-server membership snapshot exclusively from PostgreSQL. */
public final class JdbcParticipantMembershipResolver implements ParticipantMembershipResolver {
    private final JdbcQuestConnectionManager connections;

    public JdbcParticipantMembershipResolver(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public List<ParticipantMembership> resolve(final UUID playerId) {
        if (playerId == null) throw new IllegalArgumentException("playerId must not be null");
        return connections.withConnection(connection -> resolve(connection, playerId));
    }

    @Override
    public ParticipantMembership resolveParticipant(final ParticipantId participantId) {
        if (participantId == null) throw new IllegalArgumentException("participantId must not be null");
        if (participantId.getType() == ParticipantType.PLAYER) return ParticipantMembership.player(participantId.getId());
        return connections.withConnection(connection -> new ParticipantMembership(participantId,
            activeMembers(connection, participantId.getType(), participantId.getId())));
    }

    private List<ParticipantMembership> resolve(Connection connection, UUID playerId) throws SQLException {
        List<ParticipantMembership> result = new ArrayList<ParticipantMembership>();
        result.add(ParticipantMembership.player(playerId));
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT participant_type,participant_id FROM galaxy_quest_participant_membership "
                + "WHERE player_id=? AND left_at IS NULL ORDER BY participant_type,participant_id")) {
            statement.setObject(1, playerId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    ParticipantType type = ParticipantType.valueOf(rows.getString(1));
                    if (type == ParticipantType.PLAYER) throw new QuestPersistenceException(
                        "PLAYER membership rows are not allowed");
                    UUID participantId = (UUID) rows.getObject(2);
                    result.add(new ParticipantMembership(new ParticipantId(type, participantId),
                        activeMembers(connection, type, participantId)));
                }
            }
        }
        return result;
    }

    private Set<UUID> activeMembers(Connection connection, ParticipantType type, UUID participantId)
        throws SQLException {
        Set<UUID> members = new LinkedHashSet<UUID>();
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT player_id FROM galaxy_quest_participant_membership WHERE participant_type=? "
                + "AND participant_id=? AND left_at IS NULL ORDER BY joined_at,player_id")) {
            statement.setString(1, type.name());
            statement.setObject(2, participantId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) members.add((UUID) rows.getObject(1));
            }
        }
        if (members.isEmpty()) throw new QuestPersistenceException("active participant has no active members");
        return members;
    }
}
