package com.jsirgalaxybase.modules.quest.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jsirgalaxybase.quest.core.QuestCenterChapterSummary;
import com.jsirgalaxybase.quest.core.QuestCenterEntry;
import com.jsirgalaxybase.quest.core.QuestCenterPage;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskProgress;
import com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot;

/** Converts authoritative quest-domain rows to the bounded terminal wire contract. */
public final class TerminalQuestCenterSnapshotMapper {
    public TerminalQuestCenterSectionSnapshot map(QuestCenterPage page,String filter,String query,String selectedQuestId,
        boolean detailView,String message){
        List<TerminalQuestCenterSectionSnapshot.Chapter> chapters=new ArrayList<TerminalQuestCenterSectionSnapshot.Chapter>();
        for(QuestCenterChapterSummary value:page.getChapters())chapters.add(new TerminalQuestCenterSectionSnapshot.Chapter(value.getId(),value.getName(),value.getCompleted(),value.getTotal()));
        List<TerminalQuestCenterSectionSnapshot.Quest> quests=new ArrayList<TerminalQuestCenterSectionSnapshot.Quest>();TerminalQuestCenterSectionSnapshot.Detail detail=null;
        for(QuestCenterEntry entry:page.getQuests()){TerminalQuestCenterSectionSnapshot.Quest summary=summary(entry,page.getSelectedChapterId());quests.add(summary);if(detailView&&entry.getDefinition().getId().toString().equals(selectedQuestId))detail=detail(entry,summary);}
        return new TerminalQuestCenterSectionSnapshot(quests.isEmpty()?"EMPTY":"READY",message,
            detail==null?TerminalQuestCenterSectionSnapshot.View.BROWSE:TerminalQuestCenterSectionSnapshot.View.DETAIL,
            page.getSelectedChapterId(),detail==null?"":selectedQuestId,filter,query,chapters,page.getChapterPageIndex(),
            page.getChapterPageSize(),page.getChapterTotalEntries(),quests,page.getQuestPageIndex(),page.getQuestPageSize(),
            page.getQuestTotalEntries(),detail);
    }
    private static TerminalQuestCenterSectionSnapshot.Quest summary(QuestCenterEntry entry,String chapterId){QuestDefinition q=entry.getDefinition();int done=0;if(entry.getProgress()!=null)for(TaskProgress p:entry.getProgress().getTasks().values())if(p.isComplete())done++;String registry=icon(q);return new TerminalQuestCenterSectionSnapshot.Quest(q.getId().toString(),chapterId,q.getName(),subtitle(entry),state(entry),done,q.getTasks().size(),registry,0,q.getName(),entry.isTracked());}
    private static TerminalQuestCenterSectionSnapshot.Detail detail(QuestCenterEntry entry,TerminalQuestCenterSectionSnapshot.Quest summary){QuestDefinition q=entry.getDefinition();QuestProgressSnapshot progress=entry.getProgress();List<TerminalQuestCenterSectionSnapshot.Task> tasks=new ArrayList<TerminalQuestCenterSectionSnapshot.Task>();for(TaskDefinition task:q.getTasks()){TaskProgress value=progress==null?null:progress.getTasks().get(task.getKey());if(value==null)value=TaskProgress.empty(task);tasks.add(new TerminalQuestCenterSectionSnapshot.Task(task.getKey(),typeName(task.getTypeId(),"任务"),task.isOptional()?"可选目标":"必需目标",value.getValue(),value.getTarget()));}List<TerminalQuestCenterSectionSnapshot.Reward> rewards=new ArrayList<TerminalQuestCenterSectionSnapshot.Reward>();for(RewardDefinition reward:q.getRewards()){Map<String,String> p=reward.getParameters();String registry=text(p.get("item.0.registryName"));List<TerminalQuestCenterSectionSnapshot.Choice> choices=choices(reward);rewards.add(new TerminalQuestCenterSectionSnapshot.Reward(reward.getKey(),typeName(reward.getTypeId(),"奖励"),reward.getTypeId(),registry,integer(p.get("item.0.meta")),typeName(reward.getTypeId(),"奖励"),number(p.get("item.0.amount"),registry.isEmpty()?0:1),entry.getRewards().hasClaimable(),entry.getRewards().isFullyDelivered(),choices,entry.getRewards().getSelectedChoice(reward.getKey())));}return new TerminalQuestCenterSectionSnapshot.Detail(summary,q.getDescription(),q.getPrerequisites().isEmpty()?"无前置":"前置任务 "+q.getPrerequisites().size()+" 项",q.getRepeatPolicy().isRepeatable()?"可重复":"不可重复",tasks,rewards);}
    private static List<TerminalQuestCenterSectionSnapshot.Choice> choices(RewardDefinition reward){List<TerminalQuestCenterSectionSnapshot.Choice> result=new ArrayList<TerminalQuestCenterSectionSnapshot.Choice>();if(!"bq_standard:choice".equals(reward.getTypeId()))return result;Map<String,String> p=reward.getParameters();int count=Math.min(32,Math.max(0,integer(p.get("item.count"))));for(int i=0;i<count;i++){String prefix="item."+i+".";String registry=text(p.get(prefix+"registryName"));if(registry.isEmpty())continue;result.add(new TerminalQuestCenterSectionSnapshot.Choice(registry,integer(p.get(prefix+"meta")),text(p.get(prefix+"name")),number(p.get(prefix+"amount"),1)));}return result;}
    private static String state(QuestCenterEntry entry){if(entry.getRewards().hasClaimable())return "CLAIMABLE";if(entry.getStatus()==QuestStatus.LOCKED)return "LOCKED";if(entry.getStatus()==QuestStatus.IN_PROGRESS)return "IN_PROGRESS";if(entry.getStatus()==QuestStatus.COMPLETED)return "COMPLETED";return "AVAILABLE";}
    private static String subtitle(QuestCenterEntry entry){if(entry.getRewards().hasClaimable())return "任务完成，奖励等待领取";if(entry.getStatus()==QuestStatus.LOCKED)return "完成前置任务后解锁";if(entry.getStatus()==QuestStatus.COMPLETED)return entry.getRewards().isFullyDelivered()?"已完成并领取":"已完成";return entry.getDefinition().getTasks().size()+" 个目标";}
    private static String icon(QuestDefinition q){String value=q.getBehavior().getIconReference();return value==null||value.startsWith("{")?"":value;}
    private static String typeName(String type,String suffix){String value=text(type);int split=value.indexOf(':');if(split>=0)value=value.substring(split+1);return value.replace('_',' ')+" "+suffix;}
    private static int integer(String value){try{return Integer.parseInt(value);}catch(RuntimeException invalid){return 0;}}
    private static long number(String value,long fallback){try{return Long.parseLong(value);}catch(RuntimeException invalid){return fallback;}}
    private static String text(String value){return value==null?"":value;}
}
