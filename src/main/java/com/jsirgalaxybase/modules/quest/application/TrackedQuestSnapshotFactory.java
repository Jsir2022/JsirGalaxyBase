package com.jsirgalaxybase.modules.quest.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestCenterEntry;
import com.jsirgalaxybase.quest.core.QuestCenterQuery;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskProgress;
import com.jsirgalaxybase.terminal.TerminalTrackedQuestSnapshot;

/** Projects the authoritative quest center into a deliberately small HUD snapshot. */
public final class TrackedQuestSnapshotFactory {
    private final QuestCenterQuery query;
    public TrackedQuestSnapshotFactory(QuestCenterQuery query){if(query==null)throw new IllegalArgumentException("query is required");this.query=query;}
    public TerminalTrackedQuestSnapshot create(ParticipantId participant,long generatedAt){List<TerminalTrackedQuestSnapshot.Quest> quests=new ArrayList<TerminalTrackedQuestSnapshot.Quest>();com.jsirgalaxybase.quest.core.QuestCenterSnapshot snapshot=query instanceof com.jsirgalaxybase.quest.core.AuthenticatedQuestCenterQuery&&participant.getType()==com.jsirgalaxybase.quest.core.ParticipantType.PLAYER?((com.jsirgalaxybase.quest.core.AuthenticatedQuestCenterQuery)query).loadForPlayer(participant.getId()):query.load(participant);for(QuestCenterEntry entry:snapshot.getQuests()){if(!entry.isTracked())continue;quests.add(quest(entry));if(quests.size()==5)break;}return new TerminalTrackedQuestSnapshot(quests,generatedAt);}
    private static TerminalTrackedQuestSnapshot.Quest quest(QuestCenterEntry entry){QuestDefinition definition=entry.getDefinition();List<TerminalTrackedQuestSnapshot.Task> tasks=new ArrayList<TerminalTrackedQuestSnapshot.Task>();for(TaskDefinition task:definition.getTasks()){TaskProgress progress=entry.getProgress()==null?null:entry.getProgress().getTasks().get(task.getKey());long value=progress==null?0L:progress.getValue(),target=progress==null?target(task):progress.getTarget();tasks.add(new TerminalTrackedQuestSnapshot.Task(label(task),value,target));if(tasks.size()==3)break;}return new TerminalTrackedQuestSnapshot.Quest(definition.getId().toString(),definition.getName(),entry.getStatus().name(),tasks);}
    private static long target(TaskDefinition task){String value=task.getParameters().get("target");try{return Math.max(1L,Long.parseLong(value));}catch(RuntimeException invalid){return 1L;}}
    private static String label(TaskDefinition task){String type=task.getTypeId();int split=type.indexOf(':');return (split>=0?type.substring(split+1):type).replace('_',' ');}
}
