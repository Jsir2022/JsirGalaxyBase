package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded server-authored HUD projection. It contains no player identity or mutable authority fields. */
public final class TerminalTrackedQuestSnapshot {
    public static final TerminalTrackedQuestSnapshot EMPTY=new TerminalTrackedQuestSnapshot(Collections.<Quest>emptyList(),0L);
    private final List<Quest> quests;private final long generatedAt;
    public TerminalTrackedQuestSnapshot(List<Quest> quests,long generatedAt){List<Quest> safe=quests==null?Collections.<Quest>emptyList():quests;this.quests=Collections.unmodifiableList(new ArrayList<Quest>(safe.subList(0,Math.min(5,safe.size()))));this.generatedAt=Math.max(0L,generatedAt);}
    public List<Quest> getQuests(){return quests;}public long getGeneratedAt(){return generatedAt;}
    public static final class Quest {
        private final String id,name,state;private final List<Task> tasks;
        public Quest(String id,String name,String state,List<Task> tasks){this.id=text(id,64);this.name=text(name,96);this.state=text(state,24);List<Task> safe=tasks==null?Collections.<Task>emptyList():tasks;this.tasks=Collections.unmodifiableList(new ArrayList<Task>(safe.subList(0,Math.min(3,safe.size()))));}
        public String getId(){return id;}public String getName(){return name;}public String getState(){return state;}public List<Task> getTasks(){return tasks;}
    }
    public static final class Task {
        private final String label;private final long value,target;
        public Task(String label,long value,long target){this.label=text(label,80);this.value=Math.max(0L,value);this.target=Math.max(1L,target);}
        public String getLabel(){return label;}public long getValue(){return value;}public long getTarget(){return target;}
    }
    private static String text(String value,int max){String safe=value==null?"":value;return safe.length()<=max?safe:safe.substring(0,max);}
}
