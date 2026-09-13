package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Deterministic server-side filtering and bounding for large imported quest catalogs. */
public final class QuestCenterPaginator {
    public static final String UNPLACED_CHAPTER_ID="unplaced";
    private QuestCenterPaginator() {}

    public static QuestCenterPage page(QuestCenterSnapshot snapshot,QuestCenterPageRequest request){
        if(snapshot==null)throw new IllegalArgumentException("snapshot is required");
        QuestCenterPageRequest r=request==null?QuestCenterPageRequest.initial():request;
        List<QuestChapterDefinition> chapters=snapshot.getChapters();Set<UUID> placed=placed(chapters);
        List<QuestCenterChapterSummary> chapterSummaries=summaries(snapshot,chapters,placed);
        String selected=selectChapter(snapshot,r.getSelectedChapterId(),placed);
        Set<UUID> selectedIds=chapterIds(chapters,selected);String needle=r.getQuery().toLowerCase(Locale.ROOT);
        List<QuestCenterEntry> matches=new ArrayList<QuestCenterEntry>();
        for(QuestCenterEntry entry:snapshot.getQuests()){
            boolean member=UNPLACED_CHAPTER_ID.equals(selected)?!placed.contains(entry.getDefinition().getId()):selectedIds.contains(entry.getDefinition().getId());
            if(member&&matches(entry,r.getFilter())&&search(entry,needle))matches.add(entry);
        }
        int chapterPage=clamp(r.getChapterPageIndex(),chapterSummaries.size(),r.getChapterPageSize());
        int questPage=clamp(r.getQuestPageIndex(),matches.size(),r.getQuestPageSize());
        return new QuestCenterPage(snapshot.getParticipantId(),selected,
            slice(chapterSummaries,chapterPage,r.getChapterPageSize()),chapterPage,r.getChapterPageSize(),chapterSummaries.size(),
            slice(matches,questPage,r.getQuestPageSize()),questPage,r.getQuestPageSize(),matches.size());
    }

    private static boolean matches(QuestCenterEntry entry,String filter){
        if("claimable".equals(filter))return entry.getRewards().hasClaimable();
        if("active".equals(filter))return entry.getStatus()==QuestStatus.AVAILABLE||entry.getStatus()==QuestStatus.IN_PROGRESS;
        if("completed".equals(filter))return entry.getStatus()==QuestStatus.COMPLETED;
        return true;
    }
    private static boolean search(QuestCenterEntry entry,String needle){return needle.isEmpty()||(entry.getDefinition().getName()+" "+entry.getDefinition().getDescription()).toLowerCase(Locale.ROOT).contains(needle);}
    private static String selectChapter(QuestCenterSnapshot snapshot,String requested,Set<UUID> placed){
        for(QuestChapterDefinition chapter:snapshot.getChapters())if(chapter.getId().toString().equals(requested))return requested;
        if(UNPLACED_CHAPTER_ID.equals(requested)&&hasUnplaced(snapshot,placed))return requested;
        if(!snapshot.getChapters().isEmpty())return snapshot.getChapters().get(0).getId().toString();
        return hasUnplaced(snapshot,placed)?UNPLACED_CHAPTER_ID:"";
    }
    private static boolean hasUnplaced(QuestCenterSnapshot snapshot,Set<UUID> placed){for(QuestCenterEntry entry:snapshot.getQuests())if(!placed.contains(entry.getDefinition().getId()))return true;return false;}
    private static List<QuestCenterChapterSummary> summaries(QuestCenterSnapshot snapshot,List<QuestChapterDefinition> chapters,Set<UUID> placed){
        java.util.Map<UUID,QuestCenterEntry> byId=new java.util.HashMap<UUID,QuestCenterEntry>();for(QuestCenterEntry entry:snapshot.getQuests())byId.put(entry.getDefinition().getId(),entry);
        List<QuestCenterChapterSummary> result=new ArrayList<QuestCenterChapterSummary>();
        for(QuestChapterDefinition chapter:chapters){int total=0,done=0;for(QuestChapterEntry placement:chapter.getEntries()){QuestCenterEntry entry=byId.get(placement.getQuestId());if(entry==null)continue;total++;if(entry.getStatus()==QuestStatus.COMPLETED)done++;}result.add(new QuestCenterChapterSummary(chapter.getId().toString(),chapter.getName(),done,total));}
        int total=0,done=0;for(QuestCenterEntry entry:snapshot.getQuests())if(!placed.contains(entry.getDefinition().getId())){total++;if(entry.getStatus()==QuestStatus.COMPLETED)done++;}
        if(total>0)result.add(new QuestCenterChapterSummary(UNPLACED_CHAPTER_ID,"Other quests",done,total));return result;
    }
    private static Set<UUID> placed(List<QuestChapterDefinition> chapters){Set<UUID> ids=new HashSet<UUID>();for(QuestChapterDefinition chapter:chapters)for(QuestChapterEntry entry:chapter.getEntries())ids.add(entry.getQuestId());return ids;}
    private static Set<UUID> chapterIds(List<QuestChapterDefinition> chapters,String selected){Set<UUID> ids=new HashSet<UUID>();for(QuestChapterDefinition chapter:chapters)if(chapter.getId().toString().equals(selected))for(QuestChapterEntry entry:chapter.getEntries())ids.add(entry.getQuestId());return ids;}
    private static int clamp(int page,int total,int size){int last=total<=0?0:(total-1)/size;return Math.max(0,Math.min(page,last));}
    private static <T> List<T> slice(List<T> values,int page,int size){int from=Math.min(values.size(),page*size);return new ArrayList<T>(values.subList(from,Math.min(values.size(),from+size)));}
}
