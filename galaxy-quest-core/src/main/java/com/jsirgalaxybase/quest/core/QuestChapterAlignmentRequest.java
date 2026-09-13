package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Bounded optimistic request; the server derives all new coordinates from its stored chapter. */
public final class QuestChapterAlignmentRequest {
    private final UUID chapterId; private final int version; private final String expectedHash;
    private final List<UUID> questIds; private final QuestChapterAlignment alignment;
    public QuestChapterAlignmentRequest(UUID chapterId,int version,String expectedHash,List<UUID> questIds,QuestChapterAlignment alignment){
        this.chapterId=Objects.requireNonNull(chapterId,"chapterId"); if(version<1)throw new IllegalArgumentException("version must be positive"); this.version=version;
        this.expectedHash=expectedHash==null?"":expectedHash.trim(); if(this.expectedHash.isEmpty())throw new IllegalArgumentException("expected hash is required");
        if(questIds==null||questIds.size()<2||questIds.size()>128)throw new IllegalArgumentException("alignment requires 2 to 128 quest ids");
        ArrayList<UUID> copy=new ArrayList<UUID>(questIds); if(copy.contains(null)||new HashSet<UUID>(copy).size()!=copy.size())throw new IllegalArgumentException("alignment ids must be distinct");
        this.questIds=Collections.unmodifiableList(copy); this.alignment=Objects.requireNonNull(alignment,"alignment");
    }
    public UUID getChapterId(){return chapterId;} public int getVersion(){return version;} public String getExpectedHash(){return expectedHash;}
    public List<UUID> getQuestIds(){return questIds;} public QuestChapterAlignment getAlignment(){return alignment;}
}
