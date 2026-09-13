package com.jsirgalaxybase.quest.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestTrackingRepository;

/** PostgreSQL authority for a player's cross-server tracked-quest preference. */
public final class JdbcQuestTrackingRepository implements QuestTrackingRepository {
    private final JdbcQuestConnectionManager connections;
    public JdbcQuestTrackingRepository(JdbcQuestConnectionManager connections){if(connections==null)throw new IllegalArgumentException("connections must not be null");this.connections=connections;}
    @Override public boolean toggle(final UUID playerId,final UUID questId,final long updatedAt){
        if(connections.current()==null)throw new QuestPersistenceException("quest tracking requires an active quest transaction");
        if(playerId==null||questId==null||updatedAt<0L)throw new IllegalArgumentException("player, quest, and time are required");
        return connections.withConnection(connection->{String sql="INSERT INTO galaxy_quest_player_preference(player_id,quest_id,tracked,updated_at) VALUES(?,?,TRUE,to_timestamp(?/1000.0)) ON CONFLICT(player_id,quest_id) DO UPDATE SET tracked=NOT galaxy_quest_player_preference.tracked,updated_at=EXCLUDED.updated_at RETURNING tracked";
            try(PreparedStatement statement=connection.prepareStatement(sql)){statement.setObject(1,playerId);statement.setObject(2,questId);statement.setLong(3,updatedAt);try(ResultSet row=statement.executeQuery()){if(!row.next())throw new QuestPersistenceException("quest tracking toggle returned no row");return row.getBoolean(1);}}});
    }
}
