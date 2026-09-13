package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Server-owned safe starting points. Clients submit only the template id and a display name. */
public enum QuestDraftTemplate {
    EMPTY("空白任务"),
    MANUAL_CHECK("人工确认"),
    EXPERIENCE("经验目标");

    private final String displayName;
    QuestDraftTemplate(String displayName){this.displayName=displayName;}
    public String getDisplayName(){return displayName;}

    public QuestDefinition create(UUID id,String name){
        java.util.List<TaskDefinition> tasks=Collections.emptyList();
        if(this==MANUAL_CHECK)tasks=Collections.singletonList(new TaskDefinition("check","bq_standard:checkbox",false,Collections.<String,String>emptyMap()));
        else if(this==EXPERIENCE){java.util.Map<String,String> parameters=new LinkedHashMap<String,String>();parameters.put("amount","1");parameters.put("xpUnit","points");tasks=Collections.singletonList(new TaskDefinition("experience","bq_standard:xp",false,parameters));}
        return new QuestDefinition(id,1,name,"",QuestLogic.AND,QuestLogic.AND,Collections.<UUID>emptySet(),tasks,Collections.<RewardDefinition>emptyList());
    }
}
