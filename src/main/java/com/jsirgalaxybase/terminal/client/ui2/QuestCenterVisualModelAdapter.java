package com.jsirgalaxybase.terminal.client.ui2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestCenterEntry;
import com.jsirgalaxybase.quest.core.QuestCenterChapterSummary;
import com.jsirgalaxybase.quest.core.QuestCenterPage;
import com.jsirgalaxybase.quest.core.QuestCenterSnapshot;
import com.jsirgalaxybase.quest.core.QuestChapterDefinition;
import com.jsirgalaxybase.quest.core.QuestChapterEntry;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskProgress;
import com.jsirgalaxybase.ui2.terminal.QuestCenterVisualModel;
import com.jsirgalaxybase.ui2.terminal.TerminalVisualModel;
import com.jsirgalaxybase.ui2.terminal.VisualItem;
import com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot;

/** Converts the server-authenticated quest read model into a platform-neutral UI document model. */
public final class QuestCenterVisualModelAdapter {
    private static final String UNPLACED="unplaced";
    private static final int DEFAULT_CHAPTER_PAGE_SIZE=6;
    private static final int DEFAULT_QUEST_PAGE_SIZE=7;
    private QuestCenterVisualModelAdapter() {}

    public static QuestCenterVisualModel browse(TerminalVisualModel shell,QuestCenterSnapshot snapshot,
        String selectedChapterId,String filter,String query){
        return browse(shell,snapshot,selectedChapterId,filter,query,0,DEFAULT_CHAPTER_PAGE_SIZE,0,DEFAULT_QUEST_PAGE_SIZE);
    }

    /** Maps an already bounded server page without re-filtering or silently truncating it. */
    public static QuestCenterVisualModel browse(TerminalVisualModel shell,QuestCenterPage page,String filter,String query){
        if(shell==null||page==null)throw new IllegalArgumentException("shell and page are required");
        List<QuestCenterVisualModel.Chapter> chapters=new ArrayList<QuestCenterVisualModel.Chapter>();
        for(QuestCenterChapterSummary chapter:page.getChapters())chapters.add(new QuestCenterVisualModel.Chapter(
            chapter.getId(),chapter.getName(),chapter.getCompleted(),chapter.getTotal()));
        List<QuestCenterVisualModel.QuestSummary> quests=new ArrayList<QuestCenterVisualModel.QuestSummary>();
        for(QuestCenterEntry entry:page.getQuests())quests.add(summary(entry,page.getSelectedChapterId()));
        return new QuestCenterVisualModel(shell,QuestCenterVisualModel.View.BROWSE,
            quests.isEmpty()?QuestCenterVisualModel.LoadState.EMPTY:QuestCenterVisualModel.LoadState.READY,
            page.getSelectedChapterId(),"",safe(filter).isEmpty()?"all":filter,safe(query),"",chapters,
            page.getChapterPageIndex(),page.getChapterPageSize(),page.getChapterTotalEntries(),quests,
            page.getQuestPageIndex(),page.getQuestPageSize(),page.getQuestTotalEntries(),null);
    }

    public static QuestCenterVisualModel fromWire(TerminalVisualModel shell,TerminalQuestCenterSectionSnapshot wire){
        if(shell==null)throw new IllegalArgumentException("shell is required");
        if(wire==null)return new QuestCenterVisualModel(shell,QuestCenterVisualModel.View.BROWSE,
            QuestCenterVisualModel.LoadState.ERROR,"","","all","任务快照缺失",
            Collections.<QuestCenterVisualModel.Chapter>emptyList(),Collections.<QuestCenterVisualModel.QuestSummary>emptyList(),null);
        List<QuestCenterVisualModel.Chapter> chapters=new ArrayList<QuestCenterVisualModel.Chapter>();
        for(TerminalQuestCenterSectionSnapshot.Chapter value:wire.getChapters())chapters.add(new QuestCenterVisualModel.Chapter(value.getId(),value.getName(),value.getCompleted(),value.getTotal()));
        List<QuestCenterVisualModel.QuestSummary> quests=new ArrayList<QuestCenterVisualModel.QuestSummary>();
        for(TerminalQuestCenterSectionSnapshot.Quest value:wire.getQuests())quests.add(wireSummary(value));
        QuestCenterVisualModel.QuestDetail detail=null;if(wire.getDetail()!=null){TerminalQuestCenterSectionSnapshot.Detail source=wire.getDetail();
            List<QuestCenterVisualModel.TaskRow> tasks=new ArrayList<QuestCenterVisualModel.TaskRow>();for(TerminalQuestCenterSectionSnapshot.Task value:source.getTasks())tasks.add(new QuestCenterVisualModel.TaskRow(value.getKey(),value.getLabel(),value.getDetail(),value.getValue(),value.getTarget()));
            List<QuestCenterVisualModel.RewardRow> rewards=new ArrayList<QuestCenterVisualModel.RewardRow>();for(TerminalQuestCenterSectionSnapshot.Reward value:source.getRewards()){VisualItem item=value.getItemRegistry().isEmpty()?null:new VisualItem("quest-reward-"+value.getKey(),value.getItemRegistry(),value.getItemMeta(),value.getItemName(),value.getQuantity());List<QuestCenterVisualModel.ChoiceOption> choices=new ArrayList<QuestCenterVisualModel.ChoiceOption>();int choiceIndex=0;for(TerminalQuestCenterSectionSnapshot.Choice choice:value.getChoices()){VisualItem choiceItem=new VisualItem("quest-choice-"+value.getKey()+"-"+choiceIndex,choice.getItemRegistry(),choice.getItemMeta(),choice.getItemName(),choice.getQuantity());choices.add(new QuestCenterVisualModel.ChoiceOption(choiceItem,choice.getItemName()));choiceIndex++;}rewards.add(new QuestCenterVisualModel.RewardRow(value.getKey(),value.getLabel(),value.getDetail(),item,value.isClaimable(),value.isClaimed(),choices,value.getSelectedChoiceIndex()));}
            detail=new QuestCenterVisualModel.QuestDetail(wireSummary(source.getSummary()),source.getDescription(),source.getPrerequisiteText(),source.getRepeatText(),tasks,rewards);}
        QuestCenterVisualModel.LoadState load="UNAVAILABLE".equalsIgnoreCase(wire.getServiceState())||"ERROR".equalsIgnoreCase(wire.getServiceState())?QuestCenterVisualModel.LoadState.ERROR:quests.isEmpty()&&detail==null?QuestCenterVisualModel.LoadState.EMPTY:QuestCenterVisualModel.LoadState.READY;
        QuestCenterVisualModel result=new QuestCenterVisualModel(shell,wire.getView()==TerminalQuestCenterSectionSnapshot.View.DETAIL?QuestCenterVisualModel.View.DETAIL:QuestCenterVisualModel.View.BROWSE,load,
            wire.getSelectedChapterId(),wire.getSelectedQuestId(),wire.getFilter(),wire.getQuery(),wire.getMessage(),chapters,wire.getChapterPageIndex(),wire.getChapterPageSize(),wire.getChapterTotalEntries(),quests,wire.getQuestPageIndex(),wire.getQuestPageSize(),wire.getQuestTotalEntries(),detail);
        List<QuestCenterVisualModel.Participant> participants=new ArrayList<QuestCenterVisualModel.Participant>();for(TerminalQuestCenterSectionSnapshot.Participant value:wire.getParticipants()){List<QuestCenterVisualModel.Member> members=new ArrayList<QuestCenterVisualModel.Member>();for(TerminalQuestCenterSectionSnapshot.Member member:value.getMembers())members.add(new QuestCenterVisualModel.Member(member.getPlayerId(),member.getRole(),member.getJoinedAt(),member.getMembershipVersion()));participants.add(new QuestCenterVisualModel.Participant(value.getType(),value.getId(),value.getName(),value.getRole(),value.getRevision(),value.getMemberCount(),members));}return result.withParticipants(participants);
    }

    private static QuestCenterVisualModel.QuestSummary wireSummary(TerminalQuestCenterSectionSnapshot.Quest value){VisualItem icon=value.getIconRegistry().isEmpty()?null:new VisualItem("quest-icon-"+value.getId(),value.getIconRegistry(),value.getIconMeta(),value.getIconName(),1);QuestCenterVisualModel.QuestState state;try{state=QuestCenterVisualModel.QuestState.valueOf(value.getState());}catch(RuntimeException invalid){state=QuestCenterVisualModel.QuestState.AVAILABLE;}return new QuestCenterVisualModel.QuestSummary(value.getId(),value.getChapterId(),value.getName(),value.getSubtitle(),state,value.getCompletedTasks(),value.getTotalTasks(),icon,value.isTracked());}

    public static QuestCenterVisualModel browse(TerminalVisualModel shell,QuestCenterSnapshot snapshot,
        String selectedChapterId,String filter,String query,int chapterPageIndex,int chapterPageSize,
        int questPageIndex,int questPageSize){
        if(shell==null||snapshot==null)throw new IllegalArgumentException("shell and snapshot are required");
        Map<UUID,QuestCenterEntry> byId=index(snapshot.getQuests());Set<UUID> placed=placed(snapshot.getChapters());
        String chapter=chooseChapter(snapshot,selectedChapterId,placed);List<QuestCenterVisualModel.Chapter> chapters=new ArrayList<QuestCenterVisualModel.Chapter>();
        for(QuestChapterDefinition value:snapshot.getChapters())chapters.add(chapter(value,byId));
        int unplaced=0,unplacedDone=0;for(QuestCenterEntry entry:snapshot.getQuests())if(!placed.contains(entry.getDefinition().getId())){unplaced++;if(done(entry))unplacedDone++;}
        if(unplaced>0)chapters.add(new QuestCenterVisualModel.Chapter(UNPLACED,"其他任务",unplacedDone,unplaced));
        List<QuestCenterVisualModel.QuestSummary> quests=new ArrayList<QuestCenterVisualModel.QuestSummary>();String needle=safe(query).toLowerCase(Locale.ROOT);
        Set<UUID> chapterIds=chapterQuestIds(snapshot.getChapters(),chapter);
        for(QuestCenterEntry entry:snapshot.getQuests()){
            boolean inChapter=UNPLACED.equals(chapter)?!placed.contains(entry.getDefinition().getId()):chapterIds.contains(entry.getDefinition().getId());
            if(!inChapter||!matches(entry,filter)||(!needle.isEmpty()&&!searchText(entry).contains(needle)))continue;
            quests.add(summary(entry,chapter));
        }
        int chapterPage=clampPage(chapterPageIndex,chapters.size(),chapterPageSize);
        int questPage=clampPage(questPageIndex,quests.size(),questPageSize);
        return new QuestCenterVisualModel(shell,QuestCenterVisualModel.View.BROWSE,
            quests.isEmpty()?QuestCenterVisualModel.LoadState.EMPTY:QuestCenterVisualModel.LoadState.READY,
            chapter,"",safe(filter).isEmpty()?"all":filter,safe(query),"",
            page(chapters,chapterPage,chapterPageSize),chapterPage,chapterPageSize,chapters.size(),
            page(quests,questPage,questPageSize),questPage,questPageSize,quests.size(),null);
    }

    public static QuestCenterVisualModel detail(TerminalVisualModel shell,QuestCenterSnapshot snapshot,String questId,
        String selectedChapterId,String filter){
        UUID id=parse(questId);QuestCenterEntry selected=null;for(QuestCenterEntry entry:snapshot.getQuests())if(entry.getDefinition().getId().equals(id)){selected=entry;break;}
        if(selected==null)return browse(shell,snapshot,selectedChapterId,filter,"");
        QuestCenterVisualModel browse=browse(shell,snapshot,selectedChapterId,filter,"");
        return new QuestCenterVisualModel(shell,QuestCenterVisualModel.View.DETAIL,QuestCenterVisualModel.LoadState.READY,
            browse.getSelectedChapterId(),questId,safe(filter),browse.getQuery(),"",
            browse.getChapters(),browse.getChapterPageIndex(),browse.getChapterPageSize(),browse.getChapterTotalEntries(),
            browse.getQuests(),browse.getQuestPageIndex(),browse.getQuestPageSize(),browse.getQuestTotalEntries(),detail(selected));
    }

    private static QuestCenterVisualModel.Chapter chapter(QuestChapterDefinition chapter,Map<UUID,QuestCenterEntry> byId){int total=0,done=0;for(QuestChapterEntry placement:chapter.getEntries()){QuestCenterEntry entry=byId.get(placement.getQuestId());if(entry==null)continue;total++;if(done(entry))done++;}return new QuestCenterVisualModel.Chapter(chapter.getId().toString(),chapter.getName(),done,total);}
    private static QuestCenterVisualModel.QuestSummary summary(QuestCenterEntry entry,String chapter){QuestDefinition q=entry.getDefinition();int total=q.getTasks().size(),done=0;if(entry.getProgress()!=null)for(TaskProgress p:entry.getProgress().getTasks().values())if(p.isComplete())done++;return new QuestCenterVisualModel.QuestSummary(q.getId().toString(),chapter,q.getName(),subtitle(entry),state(entry),done,total,icon(q),entry.isTracked());}
    private static QuestCenterVisualModel.QuestDetail detail(QuestCenterEntry entry){QuestDefinition q=entry.getDefinition();List<QuestCenterVisualModel.TaskRow> tasks=new ArrayList<QuestCenterVisualModel.TaskRow>();QuestProgressSnapshot progress=entry.getProgress();for(TaskDefinition task:q.getTasks()){TaskProgress value=progress==null?TaskProgress.empty(task):progress.getTasks().get(task.getKey());if(value==null)value=TaskProgress.empty(task);tasks.add(new QuestCenterVisualModel.TaskRow(task.getKey(),taskName(task),task.isOptional()?"可选目标":"必需目标",value.getValue(),value.getTarget()));}List<QuestCenterVisualModel.RewardRow> rewards=new ArrayList<QuestCenterVisualModel.RewardRow>();for(RewardDefinition reward:q.getRewards())rewards.add(new QuestCenterVisualModel.RewardRow(reward.getKey(),rewardName(reward),rewardDetail(reward),rewardItem(reward),entry.getRewards().hasClaimable(),entry.getRewards().isFullyDelivered()));String prerequisites=q.getPrerequisites().isEmpty()?"无前置":"前置任务 "+q.getPrerequisites().size()+" 项";String repeat=q.getRepeatPolicy().isRepeatable()?"可重复":"不可重复";return new QuestCenterVisualModel.QuestDetail(summary(entry,""),q.getDescription(),prerequisites,repeat,tasks,rewards);}
    private static QuestCenterVisualModel.QuestState state(QuestCenterEntry entry){if(entry.getRewards().hasClaimable())return QuestCenterVisualModel.QuestState.CLAIMABLE;QuestStatus status=entry.getStatus();if(status==QuestStatus.LOCKED)return QuestCenterVisualModel.QuestState.LOCKED;if(status==QuestStatus.IN_PROGRESS)return QuestCenterVisualModel.QuestState.IN_PROGRESS;if(status==QuestStatus.COMPLETED)return QuestCenterVisualModel.QuestState.COMPLETED;return QuestCenterVisualModel.QuestState.AVAILABLE;}
    private static String subtitle(QuestCenterEntry entry){QuestDefinition q=entry.getDefinition();if(entry.getRewards().hasClaimable())return "任务完成，奖励等待领取";if(entry.getStatus()==QuestStatus.LOCKED)return "完成前置任务后解锁";if(entry.getStatus()==QuestStatus.COMPLETED)return entry.getRewards().isFullyDelivered()?"已完成并领取":"已完成";return q.getTasks().size()+" 个目标 · "+(q.getRepeatPolicy().isRepeatable()?"可重复":"单次任务");}
    private static boolean matches(QuestCenterEntry entry,String filter){String value=safe(filter);if(value.isEmpty()||"all".equals(value))return true;if("claimable".equals(value))return entry.getRewards().hasClaimable();if("active".equals(value))return entry.getStatus()==QuestStatus.AVAILABLE||entry.getStatus()==QuestStatus.IN_PROGRESS;return true;}
    private static String searchText(QuestCenterEntry entry){return (entry.getDefinition().getName()+" "+entry.getDefinition().getDescription()).toLowerCase(Locale.ROOT);}
    private static boolean done(QuestCenterEntry entry){return entry.getStatus()==QuestStatus.COMPLETED;}
    private static Map<UUID,QuestCenterEntry> index(List<QuestCenterEntry> entries){Map<UUID,QuestCenterEntry> result=new LinkedHashMap<UUID,QuestCenterEntry>();for(QuestCenterEntry entry:entries)result.put(entry.getDefinition().getId(),entry);return result;}
    private static Set<UUID> placed(List<QuestChapterDefinition> chapters){Set<UUID> result=new HashSet<UUID>();for(QuestChapterDefinition chapter:chapters)for(QuestChapterEntry entry:chapter.getEntries())result.add(entry.getQuestId());return result;}
    private static Set<UUID> chapterQuestIds(List<QuestChapterDefinition> chapters,String selected){for(QuestChapterDefinition chapter:chapters)if(chapter.getId().toString().equals(selected)){Set<UUID> result=new HashSet<UUID>();for(QuestChapterEntry entry:chapter.getEntries())result.add(entry.getQuestId());return result;}return Collections.emptySet();}
    private static String chooseChapter(QuestCenterSnapshot snapshot,String requested,Set<UUID> placed){for(QuestChapterDefinition chapter:snapshot.getChapters())if(chapter.getId().toString().equals(requested))return requested;if(!snapshot.getChapters().isEmpty())return snapshot.getChapters().get(0).getId().toString();for(QuestCenterEntry entry:snapshot.getQuests())if(!placed.contains(entry.getDefinition().getId()))return UNPLACED;return "";}
    private static VisualItem icon(QuestDefinition q){String registry=q.getBehavior().getIconReference();if(registry==null||registry.isEmpty()||registry.startsWith("{"))return null;return new VisualItem("quest-icon-"+q.getId(),registry,0,q.getName(),1);}
    private static VisualItem rewardItem(RewardDefinition reward){Map<String,String> p=reward.getParameters();String registry=p.get("item.0.registryName");if(registry==null||registry.isEmpty())return null;return new VisualItem("reward-"+reward.getKey(),registry,integer(p.get("item.0.meta")),rewardName(reward),number(p.get("item.0.amount"),1));}
    private static String taskName(TaskDefinition task){return typeName(task.getTypeId(),"任务");}
    private static String rewardName(RewardDefinition reward){return typeName(reward.getTypeId(),"奖励");}
    private static String typeName(String type,String suffix){int split=type.indexOf(':');String value=split>=0?type.substring(split+1):type;return value.replace('_',' ')+" "+suffix;}
    private static String rewardDetail(RewardDefinition reward){String amount=reward.getParameters().get("amount");return amount==null?reward.getTypeId():"数量 "+amount;}
    private static int integer(String value){try{return Integer.parseInt(value);}catch(RuntimeException invalid){return 0;}}
    private static long number(String value,long fallback){try{return Long.parseLong(value);}catch(RuntimeException invalid){return fallback;}}
    private static UUID parse(String value){try{return UUID.fromString(value);}catch(RuntimeException invalid){return null;}}
    private static String safe(String value){return value==null?"":value;}
    private static int clampPage(int requested,int total,int size){int safeSize=Math.max(1,size);int last=total<=0?0:(total-1)/safeSize;return Math.max(0,Math.min(requested,last));}
    private static <T> List<T> page(List<T> values,int page,int size){int safeSize=Math.max(1,size);int from=Math.min(values.size(),Math.max(0,page)*safeSize);int to=Math.min(values.size(),from+safeSize);return new ArrayList<T>(values.subList(from,to));}
}
