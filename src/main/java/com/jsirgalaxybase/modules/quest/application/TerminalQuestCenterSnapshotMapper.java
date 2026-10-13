package com.jsirgalaxybase.modules.quest.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.jsirgalaxybase.quest.core.QuestCenterChapterSummary;
import com.jsirgalaxybase.quest.core.QuestCenterEntry;
import com.jsirgalaxybase.quest.core.QuestCenterPage;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.ConsumptionRequestAuthorizer;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskProgress;
import com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot;

/** Converts authoritative quest-domain rows to the bounded terminal wire contract. */
public final class TerminalQuestCenterSnapshotMapper {
    public TerminalQuestCenterSectionSnapshot map(QuestCenterPage page,String filter,String query,String selectedQuestId,
        boolean detailView,String message){
        return map(page,filter,query,selectedQuestId,detailView,message,false);
    }
    public TerminalQuestCenterSectionSnapshot map(QuestCenterPage page,String filter,String query,String selectedQuestId,
        boolean detailView,String message,boolean consumptionAvailable){
        List<TerminalQuestCenterSectionSnapshot.Chapter> chapters=new ArrayList<TerminalQuestCenterSectionSnapshot.Chapter>();
        for(QuestCenterChapterSummary value:page.getChapters())chapters.add(new TerminalQuestCenterSectionSnapshot.Chapter(value.getId(),value.getName(),value.getCompleted(),value.getTotal()));
        List<TerminalQuestCenterSectionSnapshot.Quest> quests=new ArrayList<TerminalQuestCenterSectionSnapshot.Quest>();TerminalQuestCenterSectionSnapshot.Detail detail=null;
        for(QuestCenterEntry entry:page.getQuests()){
            TerminalQuestCenterSectionSnapshot.Quest summary=summary(entry,page.getSelectedChapterId());quests.add(summary);
        }
        QuestCenterEntry selected=page.getSelectedQuest();
        if(selected==null&&detailView)for(QuestCenterEntry entry:page.getQuests())
            if(entry.getDefinition().getId().toString().equals(selectedQuestId)){selected=entry;break;}
        if(detailView&&selected!=null&&selected.getDefinition().getId().toString().equals(selectedQuestId)){
            detail=detail(selected,summary(selected,page.getSelectedChapterId()),consumptionAvailable,
                page.getVisiblePrerequisites());
        }
        return new TerminalQuestCenterSectionSnapshot(quests.isEmpty()?"EMPTY":"READY",message,
            detail==null?TerminalQuestCenterSectionSnapshot.View.BROWSE:TerminalQuestCenterSectionSnapshot.View.DETAIL,
            page.getSelectedChapterId(),detail==null?"":selectedQuestId,filter,query,chapters,page.getChapterPageIndex(),
            page.getChapterPageSize(),page.getChapterTotalEntries(),quests,page.getQuestPageIndex(),page.getQuestPageSize(),
            page.getQuestTotalEntries(),detail);
    }
    private static TerminalQuestCenterSectionSnapshot.Quest summary(QuestCenterEntry entry,String chapterId){
        QuestDefinition q=entry.getDefinition();int done=0;
        if(entry.getProgress()!=null)for(TaskProgress p:entry.getProgress().getTasks().values())if(p.isComplete())done++;
        IconData icon=icon(q);
        return new TerminalQuestCenterSectionSnapshot.Quest(q.getId().toString(),chapterId,q.getName(),subtitle(entry),
            state(entry),done,q.getTasks().size(),icon.registry,icon.meta,q.getName(),entry.isTracked(),icon.nbt);
    }
    private static TerminalQuestCenterSectionSnapshot.Detail detail(QuestCenterEntry entry,
        TerminalQuestCenterSectionSnapshot.Quest summary,boolean consumptionAvailable,
        List<QuestCenterEntry> visiblePrerequisiteEntries){
        QuestDefinition q=entry.getDefinition();
        QuestProgressSnapshot progress=entry.getProgress();
        List<TerminalQuestCenterSectionSnapshot.Task> tasks=new ArrayList<TerminalQuestCenterSectionSnapshot.Task>();
        for(TaskDefinition task:q.getTasks()){
            TaskProgress value=progress==null?null:progress.getTasks().get(task.getKey());
            if(value==null)value=TaskProgress.empty(task);
            boolean confirmable="bq_standard:checkbox".equals(task.getTypeId())
                &&(entry.getStatus()!=QuestStatus.LOCKED||q.getBehavior().isProgressWhileLocked());
            boolean submittable=consumptionAvailable&&entry.getStatus()!=QuestStatus.LOCKED
                &&entry.getStatus()!=QuestStatus.COMPLETED&&!entry.getRewards().hasClaimable()&&!value.isComplete()
                &&ConsumptionRequestAuthorizer.supports(q,task);
            List<TerminalQuestCenterSectionSnapshot.Component> components=taskComponents(task.getTypeId(),
                task.getParameters(),value);
            tasks.add(new TerminalQuestCenterSectionSnapshot.Task(task.getKey(),typeName(task.getTypeId(),"任务"),
                taskDetail(task.getTypeId(),task.getParameters(),components),value.getValue(),value.getTarget(),
                confirmable,submittable,false,task.getTypeId(),components));
        }
        List<TerminalQuestCenterSectionSnapshot.Reward> rewards=new ArrayList<TerminalQuestCenterSectionSnapshot.Reward>();
        for(RewardDefinition reward:q.getRewards()){
            Map<String,String> p=reward.getParameters();List<TerminalQuestCenterSectionSnapshot.Component> components=
                itemComponents(p,"item",null);
            TerminalQuestCenterSectionSnapshot.Component first=components.isEmpty()?null:components.get(0);
            List<TerminalQuestCenterSectionSnapshot.Choice> choices=choices(reward);
            String rewardDetail=rewardDetail(reward.getTypeId(),p,components);
            rewards.add(new TerminalQuestCenterSectionSnapshot.Reward(reward.getKey(),typeName(reward.getTypeId(),"奖励"),
                rewardDetail,first==null?"":first.getIdentity(),first==null?0:first.getMetadata(),
                first==null?typeName(reward.getTypeId(),"奖励"):display(first),first==null?0:first.getQuantity(),
                entry.getRewards().hasClaimable(),entry.getRewards().isFullyDelivered(),choices,
                selectedChoice(entry,reward.getKey()),reward.getTypeId(),components,first==null?"":first.getNbt()));
        }
        String repeat=q.getRepeatPolicy().isRepeatable()?"可重复":"不可重复";
        if(entry.getRewards().getSourceParticipant()!=null)repeat+=" · 奖励来源 "+entry.getRewards().getSourceParticipant().getType().name()+" · 周期 "+entry.getRewards().getCycle();
        List<TerminalQuestCenterSectionSnapshot.Prerequisite> prerequisites=new ArrayList<TerminalQuestCenterSectionSnapshot.Prerequisite>();
        for(QuestCenterEntry prerequisite:visiblePrerequisiteEntries){
            QuestDefinition definition=prerequisite.getDefinition();
            if(!q.getPrerequisites().contains(definition.getId()))continue;
            prerequisites.add(new TerminalQuestCenterSectionSnapshot.Prerequisite(definition.getId().toString(),
                definition.getName(),prerequisite.getStatus().name(),true));
        }
        String prerequisiteText=q.getPrerequisites().isEmpty()?"无前置":prerequisites.size()==q.getPrerequisites().size()
            ?"前置任务 "+prerequisites.size()+" 项":"完成可见前置任务后解锁";
        return new TerminalQuestCenterSectionSnapshot.Detail(summary,q.getDescription(),prerequisiteText,repeat,tasks,rewards,
            q.getVersion(),entry.getRewards().getCycle(),prerequisites);
    }
    private static int selectedChoice(QuestCenterEntry entry,String rewardKey){Integer selected=entry.getRewards().getSelectedChoices().get(rewardKey);return selected==null?-1:selected.intValue();}
    private static List<TerminalQuestCenterSectionSnapshot.Choice> choices(RewardDefinition reward){
        List<TerminalQuestCenterSectionSnapshot.Choice> result=new ArrayList<TerminalQuestCenterSectionSnapshot.Choice>();
        if(!"bq_standard:choice".equals(reward.getTypeId()))return result;
        Map<String,String> p=reward.getParameters();int count=count(p,"item.count");
        for(int i=0;i<count;i++){String prefix="item."+i+".";String nbt=text(p.get(prefix+"nbt"));
            result.add(new TerminalQuestCenterSectionSnapshot.Choice(i,text(p.get(prefix+"registryName")),
                integer(p.get(prefix+"meta")),text(p.get(prefix+"name")),number(p.get(prefix+"amount"),1),nbt));}
        return result;
    }
    private static List<TerminalQuestCenterSectionSnapshot.Component> taskComponents(String type,
        Map<String,String> parameters,TaskProgress progress){
        String family="";
        if(type.endsWith(":retrieval")||type.endsWith(":optional_retrieval")||type.endsWith(":crafting"))family="item";
        else if(type.endsWith(":fluid"))family="fluid";
        else if(type.endsWith(":block_break"))family="block";
        if(!family.isEmpty())return itemComponents(parameters,family,progress);
        return scalarComponents(type,parameters);
    }
    private static List<TerminalQuestCenterSectionSnapshot.Component> itemComponents(Map<String,String> parameters,
        String family,TaskProgress progress){
        List<TerminalQuestCenterSectionSnapshot.Component> result=new ArrayList<TerminalQuestCenterSectionSnapshot.Component>();
        int count=count(parameters,family+".count");List<Long> values=progress==null?Collections.<Long>emptyList():progress.getValues();
        List<Long> targets=progress==null?Collections.<Long>emptyList():progress.getTargets();
        for(int i=0;i<count;i++){String prefix=family+"."+i+".";String identity=text(parameters.get(prefix+("fluid".equals(family)?"name":"registryName")));
            long amount=number(parameters.get(prefix+"amount"),identity.isEmpty()?0:1);
            long current=i<values.size()?values.get(i).longValue():0L;
            long target=i<targets.size()?targets.get(i).longValue():amount;
            if(target<=0)target=amount;
            result.add(new TerminalQuestCenterSectionSnapshot.Component(i,family,"",identity,
                text(parameters.get(prefix+"name")),integer(parameters.get(prefix+"meta")),amount,current,target,
                text(parameters.get(prefix+"oreDictionary")),text(parameters.get(prefix+"nbt"))));}
        return result;
    }
    private static List<TerminalQuestCenterSectionSnapshot.Component> scalarComponents(String type,
        Map<String,String> parameters){
        String[] keys;
        if(type.endsWith(":hunt"))keys=new String[]{"subject","target","required","targetNBT.portable"};
        else if(type.endsWith(":location"))keys=new String[]{"x","y","z","dimension","range","target"};
        else if(type.endsWith(":meeting"))keys=new String[]{"subject","range","target","targetNBT.portable"};
        else if(type.endsWith(":checkbox"))keys=new String[]{"name","title","description","desc"};
        else if(type.endsWith(":scoreboard"))keys=new String[]{"scoreName","scoreTarget","operator"};
        else if(type.endsWith(":xp"))keys=new String[]{"amount","xpUnit"};
        else if(type.endsWith(":interact_entity"))keys=new String[]{"subject","target","requiredUses","item.registryName","item.meta","item.amount","targetNBT.portable"};
        else if(type.endsWith(":interact_item"))keys=new String[]{"requiredUses","item.registryName","item.meta","item.amount","block.registryName","block.meta"};
        else keys=new String[]{"target"};
        List<TerminalQuestCenterSectionSnapshot.Component> result=new ArrayList<TerminalQuestCenterSectionSnapshot.Component>();
        for(String key:keys){String value=text(parameters.get(key));if(value.isEmpty())continue;
            result.add(new TerminalQuestCenterSectionSnapshot.Component(result.size(),"DETAIL",key,key,value,0,0,0,0,"",""));}
        return result;
    }
    private static String taskDetail(String type,Map<String,String> parameters,
        List<TerminalQuestCenterSectionSnapshot.Component> components){
        if(!components.isEmpty()&&components.get(0).getKind().equals("DETAIL"))return details(components);
        if(!components.isEmpty())return components(components,true);
        if(type.endsWith(":checkbox"))return "手动确认目标";
        return "任务目标";
    }
    private static String rewardDetail(String type,Map<String,String> parameters,
        List<TerminalQuestCenterSectionSnapshot.Component> components){
        if("bq_standard:item".equals(type))return components(components,false);
        if("bq_standard:choice".equals(type))return "请选择一项奖励";
        if("bq_standard:xp".equals(type)){String amount=text(parameters.get("amount"));String unit=text(parameters.get("xpUnit"));
            return amount.isEmpty()?"经验奖励":"经验 "+amount+("levels".equals(unit)?" 级":" 点");}
        if("bq_standard:questcompletion".equals(type))return "完成目标任务 "+first(parameters,"questId","target","quest");
        return details(scalarComponents(type,parameters));
    }
    private static String components(List<TerminalQuestCenterSectionSnapshot.Component> values,boolean progress){
        List<String> labels=new ArrayList<String>();for(TerminalQuestCenterSectionSnapshot.Component value:values){
            String identity=value.getIdentity().isEmpty()?"[缺失目标]":value.getIdentity();
            labels.add(identity+" ×"+value.getQuantity()+(progress?" ("+value.getProgress()+"/"+value.getTarget()+")":""));}
        return join(labels,"；");
    }
    private static String details(List<TerminalQuestCenterSectionSnapshot.Component> values){
        List<String> labels=new ArrayList<String>();for(TerminalQuestCenterSectionSnapshot.Component value:values)
            labels.add(value.getKey()+"："+value.getDisplayName());return join(labels," · ");
    }
    private static String display(TerminalQuestCenterSectionSnapshot.Component value){
        return value.getDisplayName().isEmpty()?value.getIdentity():value.getDisplayName();}
    private static String join(List<String> values,String separator){StringBuilder result=new StringBuilder();
        for(String value:values){if(result.length()>0)result.append(separator);result.append(value);}return result.toString();}
    private static String first(Map<String,String> values,String... keys){for(String key:keys){String value=text(values.get(key));if(!value.isEmpty())return value;}return "[未配置]";}
    private static int count(Map<String,String> parameters,String key){int value=Math.max(0,integer(parameters.get(key)));
        if(value>64)throw new IllegalArgumentException("quest detail component count exceeds 64: "+key);return value;}
    private static IconData icon(QuestDefinition q){String raw=text(q.getBehavior().getIconReference());
        if(raw.isEmpty())return new IconData("",0,"");
        if(!raw.startsWith("{"))return new IconData(raw,0,"");
        try{JsonObject root=new JsonParser().parse(raw).getAsJsonObject();
            String registry=primitive(root.get("id:8"));int meta=integer(primitive(root.get("Damage:2")));
            JsonElement tag=root.get("tag:10");return new IconData(registry,meta,tag==null?"":tag.toString());
        }catch(RuntimeException malformed){return new IconData("",0,"");}}
    private static String primitive(JsonElement value){return value==null||value.isJsonNull()?"":value.getAsString();}
    private static final class IconData{private final String registry,nbt;private final int meta;
        private IconData(String registry,int meta,String nbt){this.registry=registry;this.meta=meta;this.nbt=nbt;}}
    private static String state(QuestCenterEntry entry){if(entry.getRewards().hasClaimable())return "CLAIMABLE";if(entry.getStatus()==QuestStatus.LOCKED)return "LOCKED";if(entry.getStatus()==QuestStatus.IN_PROGRESS)return "IN_PROGRESS";if(entry.getStatus()==QuestStatus.COMPLETED)return "COMPLETED";return "AVAILABLE";}
    private static String subtitle(QuestCenterEntry entry){if(entry.getRewards().hasClaimable())return "任务完成，奖励等待领取";if(entry.getStatus()==QuestStatus.LOCKED)return "完成前置任务后解锁";if(entry.getRewards().isFullySuppressed())return "服务器奖励策略已停发奖励";if(entry.getRewards().hasSuppressed())return "部分奖励按服务器策略停发";if(entry.getStatus()==QuestStatus.COMPLETED)return entry.getRewards().isFullyDelivered()?"已完成并领取":"已完成";return entry.getDefinition().getTasks().size()+" 个目标";}
    private static String typeName(String type,String suffix){String value=text(type);int split=value.indexOf(':');if(split>=0)value=value.substring(split+1);return value.replace('_',' ')+" "+suffix;}
    private static int integer(String value){try{return Integer.parseInt(value);}catch(RuntimeException invalid){return 0;}}
    private static long number(String value,long fallback){try{return Long.parseLong(value);}catch(RuntimeException invalid){return fallback;}}
    private static String text(String value){return value==null?"":value;}
}
