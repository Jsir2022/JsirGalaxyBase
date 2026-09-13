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
public final class JdbcQuestCenterQuery implements QuestCenterQuery {
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
            return null;
        });
    }

    @Override public QuestCenterSnapshot load(final ParticipantId participantId){
        if(participantId==null)throw new IllegalArgumentException("participantId is required");
        return connections.withConnection(connection->load(connection,participantId));
    }

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
    private static QuestStatus resolved(QuestDefinition definition,Map<UUID,QuestStatus> statuses){
        if(definition.getPrerequisites().isEmpty())return QuestStatus.AVAILABLE;int complete=0;
        for(UUID prerequisite:definition.getPrerequisites())if(statuses.get(prerequisite)==QuestStatus.COMPLETED)complete++;
        return definition.getPrerequisiteLogic().satisfied(complete,definition.getPrerequisites().size())
            ?QuestStatus.AVAILABLE:QuestStatus.LOCKED;
    }
    private static List<Long> longs(Array array)throws SQLException{if(array==null)return Collections.emptyList();try{Object raw=array.getArray();List<Long> result=new ArrayList<Long>();if(raw instanceof Object[])for(Object value:(Object[])raw)result.add(((Number)value).longValue());return result;}finally{array.free();}}
    private static final class ProgressHead{private final QuestStatus status;private final long completedAt;private final int cycle;private ProgressHead(QuestStatus status,long completedAt,int cycle){this.status=status;this.completedAt=completedAt;this.cycle=cycle;}}
}
