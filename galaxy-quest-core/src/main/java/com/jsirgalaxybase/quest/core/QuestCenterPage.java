package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Server-produced bounded task-center page suitable for a network snapshot. */
public final class QuestCenterPage {
    private final ParticipantId participantId;
    private final String selectedChapterId;
    private final List<QuestCenterChapterSummary> chapters;
    private final int chapterPageIndex, chapterPageSize, chapterTotalEntries;
    private final List<QuestCenterEntry> quests;
    private final int questPageIndex, questPageSize, questTotalEntries;

    public QuestCenterPage(ParticipantId participantId,String selectedChapterId,
        List<QuestCenterChapterSummary> chapters,int chapterPageIndex,int chapterPageSize,int chapterTotalEntries,
        List<QuestCenterEntry> quests,int questPageIndex,int questPageSize,int questTotalEntries) {
        if(participantId==null)throw new IllegalArgumentException("participantId is required");
        this.participantId=participantId;this.selectedChapterId=selectedChapterId==null?"":selectedChapterId;
        this.chapters=immutable(chapters);this.chapterPageIndex=Math.max(0,chapterPageIndex);
        this.chapterPageSize=Math.max(1,chapterPageSize);this.chapterTotalEntries=Math.max(this.chapters.size(),chapterTotalEntries);
        this.quests=immutable(quests);this.questPageIndex=Math.max(0,questPageIndex);
        this.questPageSize=Math.max(1,questPageSize);this.questTotalEntries=Math.max(this.quests.size(),questTotalEntries);
    }
    private static <T> List<T> immutable(List<T> values){return Collections.unmodifiableList(new ArrayList<T>(values));}
    public ParticipantId getParticipantId(){return participantId;} public String getSelectedChapterId(){return selectedChapterId;}
    public List<QuestCenterChapterSummary> getChapters(){return chapters;} public int getChapterPageIndex(){return chapterPageIndex;}
    public int getChapterPageSize(){return chapterPageSize;} public int getChapterTotalEntries(){return chapterTotalEntries;}
    public List<QuestCenterEntry> getQuests(){return quests;} public int getQuestPageIndex(){return questPageIndex;}
    public int getQuestPageSize(){return questPageSize;} public int getQuestTotalEntries(){return questTotalEntries;}
}
