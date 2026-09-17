package com.jsirgalaxybase.quest.postgres;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantMembership;
import com.jsirgalaxybase.quest.core.ParticipantType;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestCenterQuery;
import com.jsirgalaxybase.quest.core.QuestCenterEntry;
import com.jsirgalaxybase.quest.core.QuestCenterQuery;
import com.jsirgalaxybase.quest.core.QuestCenterSnapshot;
import com.jsirgalaxybase.quest.core.QuestChapterDefinition;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestRewardSummary;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.QuestVisibilityEvaluator;
import com.jsirgalaxybase.quest.core.TaskProgress;

/** Terminal read model. Callers must derive the participant from the authenticated server session. */
public final class JdbcQuestCenterQuery implements AuthenticatedQuestCenterQuery {
    private final JdbcQuestConnectionManager connections;
    private final QuestDefinitionJsonCodec questCodec=new QuestDefinitionJsonCodec();
    private final QuestChapterJsonCodec chapterCodec=new QuestChapterJsonCodec();

    public JdbcQuestCenterQuery(JdbcQuestConnectionManager connections){
        if(connections==null)throw new IllegalArgumentException("connections must not be null");
        this.connections=connections;
    }

    /** Read-only startup check. It never creates or mutates schema objects. */
    public void validateSchema() {
        connections.withConnection(connection -> {
            requireTable(connection,"galaxy_quest_definition");
            requireTable(connection,"galaxy_quest_chapter_definition");
            requireTable(connection,"galaxy_quest_chapter_catalog");
            requireTable(connection,"galaxy_quest_progress");
            requireTable(connection,"galaxy_quest_task_progress");
            requireTable(connection,"galaxy_quest_reward_entitlement");
            requireTable(connection,"galaxy_quest_reward_choice_selection");
            requireTable(connection,"galaxy_quest_player_preference");
            requireTable(connection,"galaxy_quest_participant");
            requireTable(connection,"galaxy_quest_participant_membership");
            requireTable(connection,"galaxy_quest_content_migration_batch");
            requireTable(connection,"galaxy_quest_content_migration_item");
            return null;
        });
    }

    @Override public QuestCenterSnapshot load(final ParticipantId participantId){
        if(participantId==null)throw new IllegalArgumentException("participantId is required");
        return connections.withConnection(connection->load(connection,participantId));
    }

    @Override public QuestCenterSnapshot loadForPlayer(final UUID playerId) {
        if(playerId==null)throw new IllegalArgumentException("playerId is required");
        return connections.withConnection(connection->loadForPlayer(connection,playerId));
    }

    private QuestCenterSnapshot loadForPlayer(Connection connection,UUID playerId)throws SQLException{
        List<QuestDefinition> definitions=definitions(connection);List<QuestChapterDefinition> chapters=chapters(connection);
        List<ParticipantMembership> memberships=memberships(connection,playerId);
        Map<ParticipantId,Map<String,ProgressHead>> progressBySubject=new LinkedHashMap<ParticipantId,Map<String,ProgressHead>>();
        Map<ParticipantId,Map<String,Map<String,TaskProgress>>> tasksBySubject=new LinkedHashMap<ParticipantId,Map<String,Map<String,TaskProgress>>>();
        for(ParticipantMembership membership:memberships){ParticipantId subject=membership.getParticipantId();
            progressBySubject.put(subject,progress(connection,subject));tasksBySubject.put(subject,tasks(connection,subject));}
        Map<String,QuestRewardSummary> rewards=rewardsForRecipient(connection,playerId);
        Map<UUID,QuestDefinition> definitionMap=new LinkedHashMap<UUID,QuestDefinition>();Map<UUID,QuestStatus> statuses=new LinkedHashMap<UUID,QuestStatus>();
        for(QuestDefinition definition:definitions){definitionMap.put(definition.getId(),definition);ParticipantMembership membership=membershipFor(definition,memberships);
            if(membership==null)continue;ProgressHead head=progressBySubject.get(membership.getParticipantId()).get(versionKey(definition.getId(),definition.getVersion()));
            if(head!=null)statuses.put(definition.getId(),head.status);}
        for(QuestDefinition definition:definitions)if(!statuses.containsKey(definition.getId()))statuses.put(definition.getId(),resolved(definition,statuses));
        java.util.Set<UUID> tracked=tracked(connection,ParticipantId.player(playerId));QuestVisibilityEvaluator visibility=new QuestVisibilityEvaluator();
        List<QuestCenterEntry> entries=new ArrayList<QuestCenterEntry>();
        for(QuestDefinition definition:definitions){if(!visibility.isVisible(definition,definitionMap,statuses,false,false))continue;
            ParticipantMembership membership=membershipFor(definition,memberships);QuestProgressSnapshot snapshot=null;int cycle=0;String key=versionKey(definition.getId(),definition.getVersion());
            if(membership!=null){ParticipantId subject=membership.getParticipantId();ProgressHead head=progressBySubject.get(subject).get(key);
                if(head!=null){cycle=head.cycle;Map<String,TaskProgress> values=tasksBySubject.get(subject).get(key);snapshot=new QuestProgressSnapshot(subject,definition.getId(),definition.getVersion(),head.status,values==null?Collections.<String,TaskProgress>emptyMap():values,head.completedAt,head.cycle);}}
            entries.add(new QuestCenterEntry(definition,snapshot,rewards.get(key),statuses.get(definition.getId()),tracked.contains(definition.getId())));}
        return new QuestCenterSnapshot(ParticipantId.player(playerId),chapters,entries);
    }

    private List<ParticipantMembership> memberships(Connection connection,UUID playerId)throws SQLException{
        List<ParticipantMembership> result=new ArrayList<ParticipantMembership>();result.add(ParticipantMembership.player(playerId));
        try(PreparedStatement statement=connection.prepareStatement("SELECT participant_type,participant_id FROM galaxy_quest_participant_membership WHERE player_id=? AND left_at IS NULL ORDER BY participant_type,participant_id")){
            statement.setObject(1,playerId);try(ResultSet rows=statement.executeQuery()){while(rows.next()){ParticipantType type=ParticipantType.valueOf(rows.getString(1));
                if(type==ParticipantType.PLAYER)throw new QuestPersistenceException("PLAYER membership rows are not allowed");UUID id=(UUID)rows.getObject(2);
                java.util.Set<UUID> players=new java.util.LinkedHashSet<UUID>();try(PreparedStatement members=connection.prepareStatement("SELECT player_id FROM galaxy_quest_participant_membership WHERE participant_type=? AND participant_id=? AND left_at IS NULL ORDER BY joined_at,player_id")){members.setString(1,type.name());members.setObject(2,id);try(ResultSet memberRows=members.executeQuery()){while(memberRows.next())players.add((UUID)memberRows.getObject(1));}}
                result.add(new ParticipantMembership(new ParticipantId(type,id),players));}}
        }return result;
    }

    private static ParticipantMembership membershipFor(QuestDefinition definition,List<ParticipantMembership> memberships){ParticipantMembership found=null;
        ParticipantType required=ParticipantType.valueOf(definition.getBehavior().getParticipantScope().name());for(ParticipantMembership membership:memberships)if(membership.getParticipantId().getType()==required){if(found!=null)throw new QuestPersistenceException("multiple active "+required+" memberships");found=membership;}return found;}

    private QuestCenterSnapshot load(Connection connection,ParticipantId participant) throws SQLException{
        List<QuestDefinition> definitions=definitions(connection);
        List<QuestChapterDefinition> chapters=chapters(connection);
        Map<String,ProgressHead> progress=progress(connection,participant);
        Map<String,Map<String,TaskProgress>> tasks=tasks(connection,participant);
        Map<String,QuestRewardSummary> rewards=rewards(connection,participant);
        java.util.Set<UUID> tracked=tracked(connection,participant);
        Map<UUID,QuestDefinition> definitionMap=new LinkedHashMap<UUID,QuestDefinition>();
        Map<UUID,QuestStatus> statuses=new LinkedHashMap<UUID,QuestStatus>();
        for(QuestDefinition definition:definitions)definitionMap.put(definition.getId(),definition);
        for(QuestDefinition definition:definitions){ProgressHead head=progress.get(versionKey(definition.getId(),definition.getVersion()));
            if(head!=null)statuses.put(definition.getId(),head.status);}
        for(QuestDefinition definition:definitions)if(!statuses.containsKey(definition.getId()))statuses.put(definition.getId(),resolved(definition,statuses));
        QuestVisibilityEvaluator visibility=new QuestVisibilityEvaluator();
        List<QuestCenterEntry> entries=new ArrayList<QuestCenterEntry>(definitions.size());
        for(QuestDefinition definition:definitions){
            if(!visibility.isVisible(definition,definitionMap,statuses,false,false))continue;
            String versionKey=versionKey(definition.getId(),definition.getVersion());
            ProgressHead head=progress.get(versionKey);QuestProgressSnapshot snapshot=null;int cycle=0;
            if(head!=null){cycle=head.cycle;snapshot=new QuestProgressSnapshot(participant,definition.getId(),definition.getVersion(),
                head.status,tasks.containsKey(versionKey)?tasks.get(versionKey):Collections.<String,TaskProgress>emptyMap(),head.completedAt,head.cycle);}
            QuestRewardSummary reward=rewards.get(rewardKey(definition.getId(),definition.getVersion(),cycle));
            entries.add(new QuestCenterEntry(definition,snapshot,reward,statuses.get(definition.getId()),tracked.contains(definition.getId())));
        }
        return new QuestCenterSnapshot(participant,chapters,entries);
    }

    private java.util.Set<UUID> tracked(Connection connection,ParticipantId participant)throws SQLException{
        if(participant.getType()!=com.jsirgalaxybase.quest.core.ParticipantType.PLAYER)return Collections.emptySet();
        java.util.Set<UUID> result=new java.util.LinkedHashSet<UUID>();
        try(PreparedStatement statement=connection.prepareStatement("SELECT quest_id FROM galaxy_quest_player_preference WHERE player_id=? AND tracked ORDER BY updated_at DESC LIMIT 32")){
            statement.setObject(1,participant.getId());try(ResultSet rows=statement.executeQuery()){while(rows.next())result.add((UUID)rows.getObject(1));}}
        return result;
    }

    private List<QuestDefinition> definitions(Connection connection)throws SQLException{
        String sql="SELECT content_json::text FROM (SELECT DISTINCT ON (quest_id) content_json,quest_id,definition_version "
            +"FROM galaxy_quest_definition WHERE lifecycle='PUBLISHED' ORDER BY quest_id,definition_version DESC) latest ORDER BY quest_id";
        List<QuestDefinition> result=new ArrayList<QuestDefinition>();
        try(PreparedStatement statement=connection.prepareStatement(sql);ResultSet rows=statement.executeQuery()){
            while(rows.next())result.add(questCodec.decode(rows.getString(1)));
        }
        return result;
    }

    private List<QuestChapterDefinition> chapters(Connection connection)throws SQLException{
        String sql="SELECT latest.content_json::text FROM (SELECT DISTINCT ON (chapter_id) content_json,chapter_id,definition_version "
            +"FROM galaxy_quest_chapter_definition WHERE lifecycle='PUBLISHED' ORDER BY chapter_id,definition_version DESC) latest "
            +"LEFT JOIN galaxy_quest_chapter_catalog catalog ON catalog.chapter_id=latest.chapter_id "
            +"ORDER BY COALESCE(catalog.sort_order,9223372036854775807),latest.chapter_id";
        List<QuestChapterDefinition> result=new ArrayList<QuestChapterDefinition>();
        try(PreparedStatement statement=connection.prepareStatement(sql);ResultSet rows=statement.executeQuery()){
            while(rows.next())result.add(chapterCodec.decode(rows.getString(1)));
        }
        return result;
    }

    private Map<String,ProgressHead> progress(Connection connection,ParticipantId participant)throws SQLException{
        Map<String,ProgressHead> result=new LinkedHashMap<String,ProgressHead>();
        try(PreparedStatement statement=connection.prepareStatement("SELECT quest_id,definition_version,status,completed_at,cycle "
            +"FROM galaxy_quest_progress WHERE participant_type=? AND participant_id=?")){
            bindParticipant(statement,participant);try(ResultSet rows=statement.executeQuery()){
                while(rows.next())result.put(versionKey((UUID)rows.getObject(1),rows.getInt(2)),
                    new ProgressHead(QuestStatus.valueOf(rows.getString(3)),rows.getLong(4),rows.getInt(5)));
            }
        }
        return result;
    }

    private Map<String,Map<String,TaskProgress>> tasks(Connection connection,ParticipantId participant)throws SQLException{
        Map<String,Map<String,TaskProgress>> result=new LinkedHashMap<String,Map<String,TaskProgress>>();
        try(PreparedStatement statement=connection.prepareStatement("SELECT quest_id,definition_version,task_key,progress_values,target_values,complete "
            +"FROM galaxy_quest_task_progress WHERE participant_type=? AND participant_id=? ORDER BY quest_id,definition_version,task_key")){
            bindParticipant(statement,participant);try(ResultSet rows=statement.executeQuery()){
                while(rows.next()){
                    String key=versionKey((UUID)rows.getObject(1),rows.getInt(2));Map<String,TaskProgress> values=result.get(key);
                    if(values==null){values=new LinkedHashMap<String,TaskProgress>();result.put(key,values);}
                    String taskKey=rows.getString(3);values.put(taskKey,new TaskProgress(taskKey,longs(rows.getArray(4)),longs(rows.getArray(5)),rows.getBoolean(6)));
                }
            }
        }
        return result;
    }

    private Map<String,QuestRewardSummary> rewards(Connection connection,ParticipantId participant)throws SQLException{
        Map<String,int[]> counts=new LinkedHashMap<String,int[]>();
        Map<String,Map<String,Integer>> choices=new LinkedHashMap<String,Map<String,Integer>>();
        try(PreparedStatement statement=connection.prepareStatement("SELECT e.quest_id,e.definition_version,e.cycle,e.delivery_status,COUNT(*),"
            +"e.reward_key,s.choice_index FROM galaxy_quest_reward_entitlement e LEFT JOIN galaxy_quest_reward_choice_selection s "
            +"ON s.entitlement_key=e.entitlement_key WHERE e.participant_type=? AND e.participant_id=? "
            +"GROUP BY e.quest_id,e.definition_version,e.cycle,e.delivery_status,e.reward_key,s.choice_index")){
            bindParticipant(statement,participant);try(ResultSet rows=statement.executeQuery()){
                while(rows.next()){
                    String key=rewardKey((UUID)rows.getObject(1),rows.getInt(2),rows.getInt(3));int[] value=counts.get(key);
                    if(value==null){value=new int[6];counts.put(key,value);}int count=rows.getInt(5);value[0]+=count;
                    String status=rows.getString(4);if("CLAIMABLE".equals(status))value[1]+=count;else if("PENDING".equals(status))value[2]+=count;
                    else if("DELIVERING".equals(status))value[3]+=count;else if("DELIVERED".equals(status))value[4]+=count;
                    else if("FAILED".equals(status)||"ABANDONED".equals(status))value[5]+=count;
                    int selected=rows.getInt(7);if(!rows.wasNull()){Map<String,Integer> selectedByReward=choices.get(key);
                        if(selectedByReward==null){selectedByReward=new LinkedHashMap<String,Integer>();choices.put(key,selectedByReward);}
                        selectedByReward.put(rows.getString(6),Integer.valueOf(selected));}
                }
            }
        }
        Map<String,QuestRewardSummary> result=new LinkedHashMap<String,QuestRewardSummary>();
        for(Map.Entry<String,int[]> entry:counts.entrySet()){int[] c=entry.getValue();result.put(entry.getKey(),new QuestRewardSummary(c[0],c[1],c[2],c[3],c[4],c[5],choices.get(entry.getKey())));}
        return result;
    }

    private Map<String,QuestRewardSummary> rewardsForRecipient(Connection connection,UUID playerId)throws SQLException{
        Map<String,int[]> counts=new LinkedHashMap<String,int[]>();Map<String,Map<String,Integer>> choices=new LinkedHashMap<String,Map<String,Integer>>();
        Map<String,RewardBatch> batches=new LinkedHashMap<String,RewardBatch>();
        String sql="SELECT e.participant_type,e.participant_id,e.quest_id,e.definition_version,e.cycle,e.delivery_status,COUNT(*),e.reward_key,s.choice_index "
            +"FROM galaxy_quest_reward_entitlement e LEFT JOIN galaxy_quest_reward_choice_selection s ON s.entitlement_key=e.entitlement_key "
            +"WHERE e.recipient_player_id=? GROUP BY e.participant_type,e.participant_id,e.quest_id,e.definition_version,e.cycle,e.delivery_status,e.reward_key,s.choice_index";
        try(PreparedStatement statement=connection.prepareStatement(sql)){statement.setObject(1,playerId);try(ResultSet rows=statement.executeQuery()){while(rows.next()){
            ParticipantId subject=new ParticipantId(ParticipantType.valueOf(rows.getString(1)),(UUID)rows.getObject(2));UUID quest=(UUID)rows.getObject(3);int version=rows.getInt(4),cycle=rows.getInt(5);String key=subjectRewardKey(subject,quest,version,cycle);
            batches.put(key,new RewardBatch(subject,quest,version,cycle));
            int[] value=counts.get(key);if(value==null){value=new int[6];counts.put(key,value);}int count=rows.getInt(7);value[0]+=count;String status=rows.getString(6);
            if("CLAIMABLE".equals(status))value[1]+=count;else if("PENDING".equals(status))value[2]+=count;else if("DELIVERING".equals(status))value[3]+=count;else if("DELIVERED".equals(status))value[4]+=count;else if("FAILED".equals(status)||"ABANDONED".equals(status))value[5]+=count;
            int selected=rows.getInt(9);if(!rows.wasNull()){Map<String,Integer> selectedByReward=choices.get(key);if(selectedByReward==null){selectedByReward=new LinkedHashMap<String,Integer>();choices.put(key,selectedByReward);}selectedByReward.put(rows.getString(8),Integer.valueOf(selected));}
        }}}
        Map<String,RewardBatch> selected=new LinkedHashMap<String,RewardBatch>();
        for(Map.Entry<String,RewardBatch> entry:batches.entrySet()){RewardBatch candidate=entry.getValue();candidate.key=entry.getKey();candidate.counts=counts.get(entry.getKey());
            String questKey=versionKey(candidate.quest,candidate.version);RewardBatch current=selected.get(questKey);
            if(current==null||candidate.precedes(current))selected.put(questKey,candidate);}
        Map<String,QuestRewardSummary> result=new LinkedHashMap<String,QuestRewardSummary>();for(Map.Entry<String,RewardBatch> entry:selected.entrySet()){RewardBatch batch=entry.getValue();int[] c=batch.counts;
            result.put(entry.getKey(),new QuestRewardSummary(c[0],c[1],c[2],c[3],c[4],c[5],choices.get(batch.key),batch.subject,batch.cycle));}return result;
    }

    private static void bindParticipant(PreparedStatement statement,ParticipantId participant)throws SQLException{
        statement.setString(1,participant.getType().name());statement.setObject(2,participant.getId());
    }
    private static void requireTable(Connection connection,String table)throws SQLException{
        try(PreparedStatement statement=connection.prepareStatement("SELECT to_regclass(?)")){
            statement.setString(1,table);try(ResultSet rows=statement.executeQuery()){
                if(!rows.next()||rows.getString(1)==null)throw new SQLException("required quest table is missing: "+table);
            }
        }
    }
    private static String versionKey(UUID quest,int version){return quest+":"+version;}
    private static String rewardKey(UUID quest,int version,int cycle){return versionKey(quest,version)+":"+cycle;}
    private static String subjectRewardKey(ParticipantId participant,UUID quest,int version,int cycle){return participant.getType()+":"+participant.getId()+":"+rewardKey(quest,version,cycle);}
    private static QuestStatus resolved(QuestDefinition definition,Map<UUID,QuestStatus> statuses){
        if(definition.getPrerequisites().isEmpty())return QuestStatus.AVAILABLE;int complete=0;
        for(UUID prerequisite:definition.getPrerequisites())if(statuses.get(prerequisite)==QuestStatus.COMPLETED)complete++;
        return definition.getPrerequisiteLogic().satisfied(complete,definition.getPrerequisites().size())
            ?QuestStatus.AVAILABLE:QuestStatus.LOCKED;
    }
    private static List<Long> longs(Array array)throws SQLException{if(array==null)return Collections.emptyList();try{Object raw=array.getArray();List<Long> result=new ArrayList<Long>();if(raw instanceof Object[])for(Object value:(Object[])raw)result.add(((Number)value).longValue());return result;}finally{array.free();}}
    private static final class ProgressHead{private final QuestStatus status;private final long completedAt;private final int cycle;private ProgressHead(QuestStatus status,long completedAt,int cycle){this.status=status;this.completedAt=completedAt;this.cycle=cycle;}}
    private static final class RewardBatch{private final ParticipantId subject;private final UUID quest;private final int version,cycle;private String key;private int[] counts;
        private RewardBatch(ParticipantId subject,UUID quest,int version,int cycle){this.subject=subject;this.quest=quest;this.version=version;this.cycle=cycle;}
        private boolean precedes(RewardBatch other){boolean active=counts!=null&&counts[1]>0,otherActive=other.counts!=null&&other.counts[1]>0;
            if(active!=otherActive)return active;if(active)return cycle<other.cycle||cycle==other.cycle&&subject.toString().compareTo(other.subject.toString())<0;
            return cycle>other.cycle||cycle==other.cycle&&subject.toString().compareTo(other.subject.toString())<0;}}
}
