package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.UUID;

/** One allow-listed semantic edit. Transport adapters must not expose arbitrary object mutation. */
public final class QuestDraftEditOperation {
    public enum Kind {
        SET_NAME, SET_DESCRIPTION, SET_PREREQUISITE_LOGIC, SET_TASK_LOGIC,
        ADD_PREREQUISITE, REMOVE_PREREQUISITE,
        ADD_TASK, REPLACE_TASK, REMOVE_TASK, MOVE_TASK,
        ADD_REWARD, REPLACE_REWARD, REMOVE_REWARD, MOVE_REWARD,
        SET_OPTION
    }

    private final Kind kind;
    private final String key;
    private final String text;
    private final UUID questId;
    private final TaskDefinition task;
    private final RewardDefinition reward;
    private final int index;

    private QuestDraftEditOperation(Kind kind,String key,String text,UUID questId,TaskDefinition task,
        RewardDefinition reward,int index){
        this.kind=Objects.requireNonNull(kind,"kind");this.key=bounded(key,64);this.text=bounded(text,4096);
        this.questId=questId;this.task=task;this.reward=reward;this.index=index;
    }

    public static QuestDraftEditOperation name(String value){return value(Kind.SET_NAME,value);}
    public static QuestDraftEditOperation description(String value){return value(Kind.SET_DESCRIPTION,value);}
    public static QuestDraftEditOperation prerequisiteLogic(QuestLogic value){return value(Kind.SET_PREREQUISITE_LOGIC,Objects.requireNonNull(value,"logic").name());}
    public static QuestDraftEditOperation taskLogic(QuestLogic value){return value(Kind.SET_TASK_LOGIC,Objects.requireNonNull(value,"logic").name());}
    public static QuestDraftEditOperation option(String key,String value){return new QuestDraftEditOperation(Kind.SET_OPTION,required(key),value,null,null,null,0);}
    public static QuestDraftEditOperation addPrerequisite(UUID value){return reference(Kind.ADD_PREREQUISITE,value);}
    public static QuestDraftEditOperation removePrerequisite(UUID value){return reference(Kind.REMOVE_PREREQUISITE,value);}
    public static QuestDraftEditOperation addTask(TaskDefinition value){return task(Kind.ADD_TASK,"",value,0);}
    public static QuestDraftEditOperation replaceTask(String key,TaskDefinition value){return task(Kind.REPLACE_TASK,key,value,0);}
    public static QuestDraftEditOperation removeTask(String key){return keyed(Kind.REMOVE_TASK,key,0);}
    public static QuestDraftEditOperation moveTask(String key,int index){return keyed(Kind.MOVE_TASK,key,index);}
    public static QuestDraftEditOperation addReward(RewardDefinition value){return reward(Kind.ADD_REWARD,"",value,0);}
    public static QuestDraftEditOperation replaceReward(String key,RewardDefinition value){return reward(Kind.REPLACE_REWARD,key,value,0);}
    public static QuestDraftEditOperation removeReward(String key){return keyed(Kind.REMOVE_REWARD,key,0);}
    public static QuestDraftEditOperation moveReward(String key,int index){return keyed(Kind.MOVE_REWARD,key,index);}

    private static QuestDraftEditOperation value(Kind kind,String value){return new QuestDraftEditOperation(kind,"",value,null,null,null,0);}
    private static QuestDraftEditOperation reference(Kind kind,UUID value){return new QuestDraftEditOperation(kind,"","",Objects.requireNonNull(value,"questId"),null,null,0);}
    private static QuestDraftEditOperation keyed(Kind kind,String key,int index){return new QuestDraftEditOperation(kind,required(key),"",null,null,null,index);}
    private static QuestDraftEditOperation task(Kind kind,String key,TaskDefinition value,int index){return new QuestDraftEditOperation(kind,key,"",null,Objects.requireNonNull(value,"task"),null,index);}
    private static QuestDraftEditOperation reward(Kind kind,String key,RewardDefinition value,int index){return new QuestDraftEditOperation(kind,key,"",null,null,Objects.requireNonNull(value,"reward"),index);}
    public Kind getKind(){return kind;}public String getKey(){return key;}public String getText(){return text;}
    public UUID getQuestId(){return questId;}public TaskDefinition getTask(){return task;}public RewardDefinition getReward(){return reward;}public int getIndex(){return index;}
    private static String required(String value){String result=bounded(value,64);if(result.isEmpty())throw new IllegalArgumentException("key is required");return result;}
    private static String bounded(String value,int max){String result=value==null?"":value.trim();if(result.length()>max)throw new IllegalArgumentException("edit value exceeds "+max+" characters");return result;}
}
