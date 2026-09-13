package com.jsirgalaxybase.quest.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestCompletionQuery;

public final class JdbcQuestCompletionQuery implements QuestCompletionQuery {
    private final JdbcQuestConnectionManager connections;

    public JdbcQuestCompletionQuery(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public Set<UUID> findCompletedQuestIds(final ParticipantId participantId) {
        if (participantId == null) throw new IllegalArgumentException("participantId must not be null");
        return connections.withConnection(connection -> {
            Set<UUID> result = new LinkedHashSet<UUID>();
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT DISTINCT quest_id FROM galaxy_quest_progress WHERE participant_type=? AND participant_id=? "
                    + "AND status='COMPLETED' ORDER BY quest_id")) {
                statement.setString(1, participantId.getType().name());
                statement.setObject(2, participantId.getId());
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) result.add((UUID) rows.getObject(1));
                }
            }
            return result;
        });
    }
}
