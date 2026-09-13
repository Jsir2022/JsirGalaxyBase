package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable server-authoritative read model used by terminal adapters. */
public final class QuestCenterSnapshot {
    private final ParticipantId participantId;
    private final List<QuestChapterDefinition> chapters;
    private final List<QuestCenterEntry> quests;

    public QuestCenterSnapshot(ParticipantId participantId,List<QuestChapterDefinition> chapters,List<QuestCenterEntry> quests){
        if(participantId==null)throw new IllegalArgumentException("participantId is required");
        this.participantId=participantId;this.chapters=immutable(chapters);this.quests=immutable(quests);
    }
    private static <T> List<T> immutable(List<T> value){return value==null?Collections.<T>emptyList():Collections.unmodifiableList(new ArrayList<T>(value));}
    public ParticipantId getParticipantId(){return participantId;} public List<QuestChapterDefinition> getChapters(){return chapters;}
    public List<QuestCenterEntry> getQuests(){return quests;}
}
