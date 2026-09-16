package com.jsirgalaxybase.quest.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ParticipantDirectoryEntry;
import com.jsirgalaxybase.quest.core.ParticipantDirectoryQuery;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantMemberRole;
import com.jsirgalaxybase.quest.core.ParticipantMemberEntry;
import com.jsirgalaxybase.quest.core.ParticipantType;

public final class JdbcParticipantDirectoryQuery implements ParticipantDirectoryQuery {
    private final JdbcQuestConnectionManager connections;
    public JdbcParticipantDirectoryQuery(JdbcQuestConnectionManager connections){if(connections==null)throw new IllegalArgumentException("connections are required");this.connections=connections;}
    @Override public List<ParticipantDirectoryEntry> findActiveForPlayer(final UUID playerId){if(playerId==null)throw new IllegalArgumentException("playerId is required");return connections.withConnection(connection->{
        String sql="SELECT p.participant_type,p.participant_id,p.display_name,m.member_role,p.revision,"
            +"(SELECT COUNT(*) FROM galaxy_quest_participant_membership all_members WHERE all_members.participant_type=p.participant_type AND all_members.participant_id=p.participant_id AND all_members.left_at IS NULL) "
            +"FROM galaxy_quest_participant p JOIN galaxy_quest_participant_membership m ON m.participant_type=p.participant_type AND m.participant_id=p.participant_id "
            +"WHERE m.player_id=? AND m.left_at IS NULL AND p.lifecycle='ACTIVE' ORDER BY p.participant_type,p.display_name,p.participant_id";
        List<ParticipantDirectoryEntry> result=new ArrayList<ParticipantDirectoryEntry>();try(PreparedStatement statement=connection.prepareStatement(sql)){statement.setObject(1,playerId);try(ResultSet rows=statement.executeQuery()){while(rows.next())result.add(new ParticipantDirectoryEntry(new ParticipantId(ParticipantType.valueOf(rows.getString(1)),(UUID)rows.getObject(2)),rows.getString(3),ParticipantMemberRole.valueOf(rows.getString(4)),rows.getLong(5),rows.getInt(6)));}}return result;});}

    @Override public List<ParticipantMemberEntry> findMembers(final UUID authenticatedPlayerId,
        final ParticipantId participantId){if(authenticatedPlayerId==null||participantId==null)throw new IllegalArgumentException("identity and participant are required");return connections.withConnection(connection->{
        String sql="SELECT target.player_id,target.member_role,(EXTRACT(EPOCH FROM target.joined_at)*1000)::bigint,target.membership_version "
            +"FROM galaxy_quest_participant_membership actor JOIN galaxy_quest_participant_membership target "
            +"ON target.participant_type=actor.participant_type AND target.participant_id=actor.participant_id "
            +"WHERE actor.participant_type=? AND actor.participant_id=? AND actor.player_id=? AND actor.left_at IS NULL "
            +"AND target.left_at IS NULL ORDER BY CASE target.member_role WHEN 'OWNER' THEN 0 WHEN 'ADMIN' THEN 1 ELSE 2 END,target.joined_at,target.player_id LIMIT 64";
        List<ParticipantMemberEntry> result=new ArrayList<ParticipantMemberEntry>();try(PreparedStatement statement=connection.prepareStatement(sql)){statement.setString(1,participantId.getType().name());statement.setObject(2,participantId.getId());statement.setObject(3,authenticatedPlayerId);try(ResultSet rows=statement.executeQuery()){while(rows.next())result.add(new ParticipantMemberEntry((UUID)rows.getObject(1),ParticipantMemberRole.valueOf(rows.getString(2)),rows.getLong(3),rows.getLong(4)));}}return result;});}
}
