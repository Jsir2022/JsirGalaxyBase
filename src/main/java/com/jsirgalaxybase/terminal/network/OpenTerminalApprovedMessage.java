package com.jsirgalaxybase.terminal.network;

import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.terminal.TerminalBankSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalCustomMarketSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalExchangeMarketSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalMarketSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalMarketBrowseEntry;
import com.jsirgalaxybase.terminal.TerminalMarketAccountCenterRow;
import com.jsirgalaxybase.terminal.TerminalOpenApproval;
import com.jsirgalaxybase.terminal.TerminalNotificationFeed;
import com.jsirgalaxybase.terminal.TerminalServerToolsSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalLandSectionSnapshot;
import com.jsirgalaxybase.terminal.client.TerminalClientScreenController;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalServerToolsSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalLandSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalNotificationCenterModel;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class OpenTerminalApprovedMessage implements IMessage {

    private String selectedPageId;
    private String terminalTitle;
    private String terminalSubtitle;
    private String statusEyebrow;
    private String statusHeadline;
    private String statusDetail;
    private String statusBadgeLabel;
    private String statusBadgeValue;
    private List<TerminalHomeScreenModel.NavItemModel> navItems = new ArrayList<TerminalHomeScreenModel.NavItemModel>();
    private List<TerminalHomeScreenModel.PageSnapshotModel> pageSnapshots = new ArrayList<TerminalHomeScreenModel.PageSnapshotModel>();
    private List<TerminalHomeScreenModel.NotificationModel> notifications = new ArrayList<TerminalHomeScreenModel.NotificationModel>();
    private String sessionToken;

    public OpenTerminalApprovedMessage() {}

    public OpenTerminalApprovedMessage(TerminalOpenApproval approval) {
        this(
            approval.getSelectedPageId(),
            approval.getTerminalTitle(),
            approval.getTerminalSubtitle(),
            approval.getStatusBand().getEyebrow(),
            approval.getStatusBand().getHeadline(),
            approval.getStatusBand().getDetail(),
            approval.getStatusBand().getBadgeLabel(),
            approval.getStatusBand().getBadgeValue(),
            toNavItemModels(approval.getNavItems()),
            toPageSnapshotModels(approval.getPageSnapshots()),
            toNotificationModels(approval.getNotifications()),
            approval.getSessionToken());
    }

    public OpenTerminalApprovedMessage(String selectedPageId, String terminalTitle, String terminalSubtitle,
        String statusEyebrow, String statusHeadline, String statusDetail, String statusBadgeLabel,
        String statusBadgeValue, List<TerminalHomeScreenModel.NavItemModel> navItems,
        List<TerminalHomeScreenModel.PageSnapshotModel> pageSnapshots,
        List<TerminalHomeScreenModel.NotificationModel> notifications, String sessionToken) {
        this.selectedPageId = selectedPageId;
        this.terminalTitle = terminalTitle;
        this.terminalSubtitle = terminalSubtitle;
        this.statusEyebrow = statusEyebrow;
        this.statusHeadline = statusHeadline;
        this.statusDetail = statusDetail;
        this.statusBadgeLabel = statusBadgeLabel;
        this.statusBadgeValue = statusBadgeValue;
        this.navItems = navItems == null ? new ArrayList<TerminalHomeScreenModel.NavItemModel>() : navItems;
        this.pageSnapshots = pageSnapshots == null ? new ArrayList<TerminalHomeScreenModel.PageSnapshotModel>() : pageSnapshots;
        this.notifications = notifications == null ? new ArrayList<TerminalHomeScreenModel.NotificationModel>() : notifications;
        this.sessionToken = sessionToken;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        selectedPageId = ByteBufUtils.readUTF8String(buf);
        terminalTitle = ByteBufUtils.readUTF8String(buf);
        terminalSubtitle = ByteBufUtils.readUTF8String(buf);
        statusEyebrow = ByteBufUtils.readUTF8String(buf);
        statusHeadline = ByteBufUtils.readUTF8String(buf);
        statusDetail = ByteBufUtils.readUTF8String(buf);
        statusBadgeLabel = ByteBufUtils.readUTF8String(buf);
        statusBadgeValue = ByteBufUtils.readUTF8String(buf);
        navItems = readNavItems(buf);
        pageSnapshots = readPageSnapshots(buf);
        notifications = readNotifications(buf);
        sessionToken = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, safe(selectedPageId));
        ByteBufUtils.writeUTF8String(buf, safe(terminalTitle));
        ByteBufUtils.writeUTF8String(buf, safe(terminalSubtitle));
        ByteBufUtils.writeUTF8String(buf, safe(statusEyebrow));
        ByteBufUtils.writeUTF8String(buf, safe(statusHeadline));
        ByteBufUtils.writeUTF8String(buf, safe(statusDetail));
        ByteBufUtils.writeUTF8String(buf, safe(statusBadgeLabel));
        ByteBufUtils.writeUTF8String(buf, safe(statusBadgeValue));
        writeNavItems(buf, navItems);
        writePageSnapshots(buf, pageSnapshots);
        writeNotifications(buf, notifications);
        ByteBufUtils.writeUTF8String(buf, safe(sessionToken));
    }

    public TerminalHomeScreenModel toScreenModel() {
        return new TerminalHomeScreenModel(
            selectedPageId,
            terminalTitle,
            terminalSubtitle,
            new TerminalHomeScreenModel.StatusBandModel(
                statusEyebrow,
                statusHeadline,
                statusDetail,
                statusBadgeLabel,
                statusBadgeValue),
            navItems,
            pageSnapshots,
            notifications,
            sessionToken);
    }

    static void writePageSnapshots(ByteBuf buf, List<TerminalHomeScreenModel.PageSnapshotModel> snapshots) {
        List<TerminalHomeScreenModel.PageSnapshotModel> safeItems = snapshots == null
            ? new ArrayList<TerminalHomeScreenModel.PageSnapshotModel>() : snapshots;
        buf.writeInt(safeItems.size());
        for (TerminalHomeScreenModel.PageSnapshotModel snapshot : safeItems) {
            ByteBufUtils.writeUTF8String(buf, safe(snapshot.getPageId()));
            ByteBufUtils.writeUTF8String(buf, safe(snapshot.getTitle()));
            ByteBufUtils.writeUTF8String(buf, safe(snapshot.getLead()));
            writeSections(buf, snapshot.getSections());
            writeBankSection(buf, snapshot.getBankSectionModel());
            writeMarketSection(buf, snapshot.getMarketSectionModel());
            writeCustomMarketSection(buf, snapshot.getCustomMarketSectionModel());
            writeExchangeMarketSection(buf, snapshot.getExchangeMarketSectionModel());
            writeServerToolsSection(buf, snapshot.getServerToolsSectionModel());
            writeLandSection(buf, snapshot.getLandSectionModel());
            writeNotificationCenter(buf, snapshot.getNotificationCenterModel());
            writeQuestCenter(buf, snapshot.getQuestCenterModel());
        }
    }

    static List<TerminalHomeScreenModel.PageSnapshotModel> readPageSnapshots(ByteBuf buf) {
        int size = buf.readInt();
        List<TerminalHomeScreenModel.PageSnapshotModel> items = new ArrayList<TerminalHomeScreenModel.PageSnapshotModel>(size);
        for (int i = 0; i < size; i++) {
            items.add(new TerminalHomeScreenModel.PageSnapshotModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                readSections(buf),
                readBankSection(buf),
                readMarketSection(buf),
                readCustomMarketSection(buf),
                readExchangeMarketSection(buf),
                readServerToolsSection(buf),
                readLandSection(buf),
                readNotificationCenter(buf),
                readQuestCenter(buf)));
        }
        return items;
    }

    static void writeQuestCenter(ByteBuf buf, com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot model) {
        buf.writeBoolean(model != null);if(model==null)return;
        ByteBufUtils.writeUTF8String(buf,safe(model.getServiceState()));ByteBufUtils.writeUTF8String(buf,safe(model.getMessage()));
        ByteBufUtils.writeUTF8String(buf,model.getView().name());ByteBufUtils.writeUTF8String(buf,safe(model.getSelectedChapterId()));
        ByteBufUtils.writeUTF8String(buf,safe(model.getSelectedQuestId()));ByteBufUtils.writeUTF8String(buf,safe(model.getFilter()));
        ByteBufUtils.writeUTF8String(buf,safe(model.getQuery()));buf.writeInt(model.getChapterPageIndex());buf.writeInt(model.getChapterPageSize());buf.writeInt(model.getChapterTotalEntries());
        int chapterCount=Math.min(12,model.getChapters().size());buf.writeInt(chapterCount);for(int chapterIndex=0;chapterIndex<chapterCount;chapterIndex++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Chapter chapter=model.getChapters().get(chapterIndex);
            ByteBufUtils.writeUTF8String(buf,safe(chapter.getId()));ByteBufUtils.writeUTF8String(buf,safe(chapter.getName()));buf.writeInt(chapter.getCompleted());buf.writeInt(chapter.getTotal());}
        buf.writeInt(model.getQuestPageIndex());buf.writeInt(model.getQuestPageSize());buf.writeInt(model.getQuestTotalEntries());
        int questCount=Math.min(24,model.getQuests().size());buf.writeInt(questCount);for(int questIndex=0;questIndex<questCount;questIndex++)writeQuest(buf,model.getQuests().get(questIndex));
        buf.writeBoolean(model.getDetail()!=null);if(model.getDetail()!=null){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Detail detail=model.getDetail();writeQuest(buf,detail.getSummary());
            ByteBufUtils.writeUTF8String(buf,safe(detail.getDescription()));ByteBufUtils.writeUTF8String(buf,safe(detail.getPrerequisiteText()));ByteBufUtils.writeUTF8String(buf,safe(detail.getRepeatText()));
            int taskCount=Math.min(64,detail.getTasks().size());buf.writeInt(taskCount);for(int taskIndex=0;taskIndex<taskCount;taskIndex++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Task task=detail.getTasks().get(taskIndex);ByteBufUtils.writeUTF8String(buf,safe(task.getKey()));ByteBufUtils.writeUTF8String(buf,safe(task.getLabel()));ByteBufUtils.writeUTF8String(buf,safe(task.getDetail()));buf.writeLong(task.getValue());buf.writeLong(task.getTarget());}
            int rewardCount=Math.min(64,detail.getRewards().size());buf.writeInt(rewardCount);for(int rewardIndex=0;rewardIndex<rewardCount;rewardIndex++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Reward reward=model.getDetail().getRewards().get(rewardIndex);ByteBufUtils.writeUTF8String(buf,safe(reward.getKey()));ByteBufUtils.writeUTF8String(buf,safe(reward.getLabel()));ByteBufUtils.writeUTF8String(buf,safe(reward.getDetail()));ByteBufUtils.writeUTF8String(buf,safe(reward.getItemRegistry()));buf.writeInt(reward.getItemMeta());ByteBufUtils.writeUTF8String(buf,safe(reward.getItemName()));buf.writeLong(reward.getQuantity());buf.writeBoolean(reward.isClaimable());buf.writeBoolean(reward.isClaimed());int choiceCount=Math.min(32,reward.getChoices().size());buf.writeInt(choiceCount);for(int choiceIndex=0;choiceIndex<choiceCount;choiceIndex++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Choice choice=reward.getChoices().get(choiceIndex);ByteBufUtils.writeUTF8String(buf,safe(choice.getItemRegistry()));buf.writeInt(choice.getItemMeta());ByteBufUtils.writeUTF8String(buf,safe(choice.getItemName()));buf.writeLong(choice.getQuantity());}buf.writeInt(reward.getSelectedChoiceIndex());}}
        com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Management management=model.getManagement();buf.writeBoolean(management!=null);if(management!=null){ByteBufUtils.writeUTF8String(buf,safe(management.getQuery()));ByteBufUtils.writeUTF8String(buf,safe(management.getLifecycle()));buf.writeInt(management.getPage());buf.writeInt(management.getPageSize());buf.writeLong(management.getTotal());int definitionCount=Math.min(50,management.getDefinitions().size());buf.writeInt(definitionCount);for(int i=0;i<definitionCount;i++)writeManagedDefinition(buf,management.getDefinitions().get(i));com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.DefinitionDetail managed=management.getDetail();buf.writeBoolean(managed!=null);if(managed!=null){writeManagedDefinition(buf,managed.getSummary());ByteBufUtils.writeUTF8String(buf,safe(managed.getDescription()));ByteBufUtils.writeUTF8String(buf,safe(managed.getPrerequisiteLogic()));ByteBufUtils.writeUTF8String(buf,safe(managed.getTaskLogic()));int prerequisiteCount=Math.min(64,managed.getPrerequisites().size());buf.writeInt(prerequisiteCount);for(int i=0;i<prerequisiteCount;i++)ByteBufUtils.writeUTF8String(buf,safe(managed.getPrerequisites().get(i)));writeManagedElements(buf,managed.getTasks());writeManagedElements(buf,managed.getRewards());writeStringMap(buf,managed.getOptions(),32);}writeElementTypes(buf,management.getTaskTypes());writeElementTypes(buf,management.getRewardTypes());int migrationCount=Math.min(10,management.getMigrations().size());buf.writeInt(migrationCount);for(int i=0;i<migrationCount;i++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Migration migration=management.getMigrations().get(i);ByteBufUtils.writeUTF8String(buf,safe(migration.getBatchId()));ByteBufUtils.writeUTF8String(buf,safe(migration.getSourceHash()));ByteBufUtils.writeUTF8String(buf,safe(migration.getStatus()));buf.writeLong(migration.getAppliedAt());buf.writeLong(migration.getRolledBackAt());buf.writeInt(migration.getCreated());buf.writeInt(migration.getVersioned());buf.writeInt(migration.getUnchanged());buf.writeInt(migration.getErrorCount());buf.writeInt(migration.getWarningCount());ByteBufUtils.writeUTF8String(buf,safe(migration.getDiagnostic()));}}
        writeChapterManagement(buf,model.getChapterManagement());
        int participantCount=Math.min(3,model.getParticipants().size());buf.writeInt(participantCount);
        for(int i=0;i<participantCount;i++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Participant value=model.getParticipants().get(i);ByteBufUtils.writeUTF8String(buf,safe(value.getType()));ByteBufUtils.writeUTF8String(buf,safe(value.getId()));ByteBufUtils.writeUTF8String(buf,safe(value.getName()));ByteBufUtils.writeUTF8String(buf,safe(value.getRole()));buf.writeLong(value.getRevision());buf.writeInt(value.getMemberCount());int memberCount=Math.min(64,value.getMembers().size());buf.writeInt(memberCount);for(int memberIndex=0;memberIndex<memberCount;memberIndex++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Member member=value.getMembers().get(memberIndex);ByteBufUtils.writeUTF8String(buf,safe(member.getPlayerId()));ByteBufUtils.writeUTF8String(buf,safe(member.getRole()));buf.writeLong(member.getJoinedAt());buf.writeLong(member.getMembershipVersion());}}
    }

    static com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot readQuestCenter(ByteBuf buf) {
        if(!buf.readBoolean())return null;String service=ByteBufUtils.readUTF8String(buf),message=ByteBufUtils.readUTF8String(buf);
        com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.View view;
        try{view=com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.View.valueOf(ByteBufUtils.readUTF8String(buf));}catch(RuntimeException invalid){view=com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.View.BROWSE;}
        String chapterId=ByteBufUtils.readUTF8String(buf),questId=ByteBufUtils.readUTF8String(buf),filter=ByteBufUtils.readUTF8String(buf),query=ByteBufUtils.readUTF8String(buf);
        int chapterPage=buf.readInt(),chapterSize=buf.readInt(),chapterTotal=buf.readInt(),chapterCount=count(buf.readInt(),12,"quest chapters");
        List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Chapter> chapters=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Chapter>(chapterCount);
        for(int i=0;i<chapterCount;i++)chapters.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Chapter(ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),buf.readInt(),buf.readInt()));
        int questPage=buf.readInt(),questSize=buf.readInt(),questTotal=buf.readInt(),questCount=count(buf.readInt(),24,"quest rows");
        List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Quest> quests=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Quest>(questCount);for(int i=0;i<questCount;i++)quests.add(readQuest(buf));
        com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Detail detail=null;if(buf.readBoolean()){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Quest summary=readQuest(buf);String description=ByteBufUtils.readUTF8String(buf),prerequisite=ByteBufUtils.readUTF8String(buf),repeat=ByteBufUtils.readUTF8String(buf);
            int taskCount=count(buf.readInt(),64,"quest tasks");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Task> tasks=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Task>(taskCount);for(int i=0;i<taskCount;i++)tasks.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Task(ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),buf.readLong(),buf.readLong()));
            int rewardCount=count(buf.readInt(),64,"quest rewards");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Reward> rewards=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Reward>(rewardCount);for(int i=0;i<rewardCount;i++){String rewardKey=ByteBufUtils.readUTF8String(buf),label=ByteBufUtils.readUTF8String(buf),rewardDetail=ByteBufUtils.readUTF8String(buf),registry=ByteBufUtils.readUTF8String(buf);int meta=buf.readInt();String name=ByteBufUtils.readUTF8String(buf);long quantity=buf.readLong();boolean claimable=buf.readBoolean(),claimed=buf.readBoolean();int choiceCount=count(buf.readInt(),32,"quest reward choices");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Choice> choices=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Choice>(choiceCount);for(int choiceIndex=0;choiceIndex<choiceCount;choiceIndex++)choices.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Choice(ByteBufUtils.readUTF8String(buf),buf.readInt(),ByteBufUtils.readUTF8String(buf),buf.readLong()));rewards.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Reward(rewardKey,label,rewardDetail,registry,meta,name,quantity,claimable,claimed,choices,buf.readInt()));}detail=new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Detail(summary,description,prerequisite,repeat,tasks,rewards);}
        com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Management management=null;if(buf.readBoolean()){String managementQuery=ByteBufUtils.readUTF8String(buf),lifecycle=ByteBufUtils.readUTF8String(buf);int managementPage=buf.readInt(),managementPageSize=buf.readInt();long managementTotal=buf.readLong();int definitionCount=count(buf.readInt(),50,"quest management definitions");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Definition> definitions=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Definition>(definitionCount);for(int i=0;i<definitionCount;i++)definitions.add(readManagedDefinition(buf));com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.DefinitionDetail managed=null;if(buf.readBoolean()){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Definition summary=readManagedDefinition(buf);String managedDescription=ByteBufUtils.readUTF8String(buf),prerequisiteLogic=ByteBufUtils.readUTF8String(buf),taskLogic=ByteBufUtils.readUTF8String(buf);int prerequisiteCount=count(buf.readInt(),64,"quest prerequisites");List<String> prerequisites=new ArrayList<String>(prerequisiteCount);for(int i=0;i<prerequisiteCount;i++)prerequisites.add(ByteBufUtils.readUTF8String(buf));managed=new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.DefinitionDetail(summary,managedDescription,prerequisiteLogic,taskLogic,prerequisites,readManagedElements(buf),readManagedElements(buf),readStringMap(buf,32));}List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ElementType> taskTypes=readElementTypes(buf),rewardTypes=readElementTypes(buf);int migrationCount=count(buf.readInt(),10,"quest migration batches");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Migration> migrations=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Migration>(migrationCount);for(int i=0;i<migrationCount;i++)migrations.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Migration(ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),buf.readLong(),buf.readLong(),buf.readInt(),buf.readInt(),buf.readInt(),buf.readInt(),buf.readInt(),ByteBufUtils.readUTF8String(buf)));management=new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Management(managementQuery,lifecycle,managementPage,managementPageSize,managementTotal,definitions,managed,taskTypes,rewardTypes,migrations);}
        com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterManagement chapterManagement=readChapterManagement(buf);
        int participantCount=count(buf.readInt(),3,"quest participants");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Participant> participants=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Participant>(participantCount);for(int i=0;i<participantCount;i++){String type=ByteBufUtils.readUTF8String(buf),id=ByteBufUtils.readUTF8String(buf),name=ByteBufUtils.readUTF8String(buf),role=ByteBufUtils.readUTF8String(buf);long revision=buf.readLong();int totalMembers=buf.readInt();int memberCount=count(buf.readInt(),64,"quest participant members");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Member> members=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Member>(memberCount);for(int memberIndex=0;memberIndex<memberCount;memberIndex++)members.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Member(ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),buf.readLong(),buf.readLong()));participants.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Participant(type,id,name,role,revision,totalMembers,members));}
        return new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot(service,message,view,chapterId,questId,filter,query,chapters,chapterPage,chapterSize,chapterTotal,quests,questPage,questSize,questTotal,detail,management,chapterManagement).withParticipants(participants);
    }

    private static void writeQuest(ByteBuf buf,com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Quest quest){ByteBufUtils.writeUTF8String(buf,safe(quest.getId()));ByteBufUtils.writeUTF8String(buf,safe(quest.getChapterId()));ByteBufUtils.writeUTF8String(buf,safe(quest.getName()));ByteBufUtils.writeUTF8String(buf,safe(quest.getSubtitle()));ByteBufUtils.writeUTF8String(buf,safe(quest.getState()));buf.writeInt(quest.getCompletedTasks());buf.writeInt(quest.getTotalTasks());ByteBufUtils.writeUTF8String(buf,safe(quest.getIconRegistry()));buf.writeInt(quest.getIconMeta());ByteBufUtils.writeUTF8String(buf,safe(quest.getIconName()));buf.writeBoolean(quest.isTracked());}
    private static com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Quest readQuest(ByteBuf buf){return new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Quest(ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),buf.readInt(),buf.readInt(),ByteBufUtils.readUTF8String(buf),buf.readInt(),ByteBufUtils.readUTF8String(buf),buf.readBoolean());}
    private static void writeManagedDefinition(ByteBuf buf,com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Definition definition){ByteBufUtils.writeUTF8String(buf,safe(definition.getId()));buf.writeInt(definition.getVersion());ByteBufUtils.writeUTF8String(buf,safe(definition.getName()));ByteBufUtils.writeUTF8String(buf,safe(definition.getLifecycle()));ByteBufUtils.writeUTF8String(buf,safe(definition.getContentHash()));buf.writeLong(definition.getPublishedAt());buf.writeInt(definition.getTaskCount());buf.writeInt(definition.getRewardCount());}
    private static com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Definition readManagedDefinition(ByteBuf buf){return new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Definition(ByteBufUtils.readUTF8String(buf),buf.readInt(),ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),buf.readLong(),buf.readInt(),buf.readInt());}
    private static void writeChapterManagement(ByteBuf buf,com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterManagement value){buf.writeBoolean(value!=null);if(value==null)return;ByteBufUtils.writeUTF8String(buf,safe(value.getQuery()));ByteBufUtils.writeUTF8String(buf,safe(value.getLifecycle()));buf.writeInt(value.getPage());buf.writeInt(value.getPageSize());buf.writeLong(value.getTotal());int size=Math.min(50,value.getChapters().size());buf.writeInt(size);for(int i=0;i<size;i++)writeManagedChapter(buf,value.getChapters().get(i));buf.writeBoolean(value.getDetail()!=null);if(value.getDetail()!=null){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterDetail detail=value.getDetail();writeManagedChapter(buf,detail.getSummary());ByteBufUtils.writeUTF8String(buf,safe(detail.getDescription()));ByteBufUtils.writeUTF8String(buf,safe(detail.getIcon()));ByteBufUtils.writeUTF8String(buf,safe(detail.getBackground()));buf.writeInt(detail.getBackgroundSize());ByteBufUtils.writeUTF8String(buf,safe(detail.getVisibility()));int placements=Math.min(4096,detail.getPlacements().size());buf.writeInt(placements);for(int i=0;i<placements;i++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterPlacement row=detail.getPlacements().get(i);ByteBufUtils.writeUTF8String(buf,safe(row.getQuestId()));ByteBufUtils.writeUTF8String(buf,safe(row.getName()));buf.writeInt(row.getX());buf.writeInt(row.getY());buf.writeInt(row.getWidth());buf.writeInt(row.getHeight());}int dependencies=Math.min(8192,detail.getDependencies().size());buf.writeInt(dependencies);for(int i=0;i<dependencies;i++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterDependency row=detail.getDependencies().get(i);ByteBufUtils.writeUTF8String(buf,safe(row.getPrerequisiteQuestId()));ByteBufUtils.writeUTF8String(buf,safe(row.getQuestId()));ByteBufUtils.writeUTF8String(buf,safe(row.getKind()));}int candidates=Math.min(50,detail.getCandidates().size());buf.writeInt(candidates);for(int i=0;i<candidates;i++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterCandidate row=detail.getCandidates().get(i);ByteBufUtils.writeUTF8String(buf,safe(row.getQuestId()));ByteBufUtils.writeUTF8String(buf,safe(row.getName()));}}}
    private static com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterManagement readChapterManagement(ByteBuf buf){if(!buf.readBoolean())return null;String query=ByteBufUtils.readUTF8String(buf),lifecycle=ByteBufUtils.readUTF8String(buf);int page=buf.readInt(),pageSize=buf.readInt();long total=buf.readLong();int count=count(buf.readInt(),50,"managed chapters");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ManagedChapter> rows=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ManagedChapter>(count);for(int i=0;i<count;i++)rows.add(readManagedChapter(buf));com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterDetail detail=null;if(buf.readBoolean()){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ManagedChapter summary=readManagedChapter(buf);String description=ByteBufUtils.readUTF8String(buf),icon=ByteBufUtils.readUTF8String(buf),background=ByteBufUtils.readUTF8String(buf);int backgroundSize=buf.readInt();String visibility=ByteBufUtils.readUTF8String(buf);int placements=count(buf.readInt(),4096,"chapter placements");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterPlacement> placed=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterPlacement>(placements);for(int i=0;i<placements;i++)placed.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterPlacement(ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),buf.readInt(),buf.readInt(),buf.readInt(),buf.readInt()));int dependencies=count(buf.readInt(),8192,"chapter dependencies");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterDependency> edges=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterDependency>(dependencies);for(int i=0;i<dependencies;i++)edges.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterDependency(ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf)));int candidates=count(buf.readInt(),50,"chapter candidates");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterCandidate> available=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterCandidate>(candidates);for(int i=0;i<candidates;i++)available.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterCandidate(ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf)));detail=new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterDetail(summary,description,icon,background,backgroundSize,visibility,placed,available,edges);}return new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ChapterManagement(query,lifecycle,page,pageSize,total,rows,detail);}
    private static void writeManagedChapter(ByteBuf buf,com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ManagedChapter value){ByteBufUtils.writeUTF8String(buf,safe(value.getId()));buf.writeInt(value.getVersion());ByteBufUtils.writeUTF8String(buf,safe(value.getName()));ByteBufUtils.writeUTF8String(buf,safe(value.getLifecycle()));ByteBufUtils.writeUTF8String(buf,safe(value.getContentHash()));buf.writeLong(value.getPublishedAt());buf.writeInt(value.getEntryCount());}
    private static com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ManagedChapter readManagedChapter(ByteBuf buf){return new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ManagedChapter(ByteBufUtils.readUTF8String(buf),buf.readInt(),ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),buf.readLong(),buf.readInt());}
    private static void writeManagedElements(ByteBuf buf,List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Element> values){int size=Math.min(64,values.size());buf.writeInt(size);for(int i=0;i<size;i++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Element value=values.get(i);ByteBufUtils.writeUTF8String(buf,safe(value.getKey()));ByteBufUtils.writeUTF8String(buf,safe(value.getTypeId()));buf.writeBoolean(value.isOptional());writeStringMap(buf,value.getParameters(),64);}}
    private static List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Element> readManagedElements(ByteBuf buf){int size=count(buf.readInt(),64,"quest managed elements");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Element> result=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Element>(size);for(int i=0;i<size;i++)result.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Element(ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf),buf.readBoolean(),readStringMap(buf,64)));return result;}
    private static void writeElementTypes(ByteBuf buf,List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ElementType> values){int size=Math.min(32,values.size());buf.writeInt(size);for(int i=0;i<size;i++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ElementType type=values.get(i);ByteBufUtils.writeUTF8String(buf,safe(type.getId()));ByteBufUtils.writeUTF8String(buf,safe(type.getName()));ByteBufUtils.writeUTF8String(buf,safe(type.getKind()));int fields=Math.min(32,type.getFields().size());buf.writeInt(fields);for(int j=0;j<fields;j++){com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Field field=type.getFields().get(j);ByteBufUtils.writeUTF8String(buf,safe(field.getKey()));ByteBufUtils.writeUTF8String(buf,safe(field.getLabel()));ByteBufUtils.writeUTF8String(buf,safe(field.getType()));buf.writeBoolean(field.isRequired());buf.writeBoolean(field.getMinimum()!=null);if(field.getMinimum()!=null)buf.writeLong(field.getMinimum());buf.writeBoolean(field.getMaximum()!=null);if(field.getMaximum()!=null)buf.writeLong(field.getMaximum());int options=Math.min(32,field.getOptions().size());buf.writeInt(options);for(int k=0;k<options;k++)ByteBufUtils.writeUTF8String(buf,safe(field.getOptions().get(k)));}}}
    private static List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ElementType> readElementTypes(ByteBuf buf){int size=count(buf.readInt(),32,"quest element types");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ElementType> result=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ElementType>(size);for(int i=0;i<size;i++){String id=ByteBufUtils.readUTF8String(buf),name=ByteBufUtils.readUTF8String(buf),kind=ByteBufUtils.readUTF8String(buf);int fields=count(buf.readInt(),32,"quest editor fields");List<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Field> fieldValues=new ArrayList<com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Field>(fields);for(int j=0;j<fields;j++){String key=ByteBufUtils.readUTF8String(buf),label=ByteBufUtils.readUTF8String(buf),type=ByteBufUtils.readUTF8String(buf);boolean required=buf.readBoolean();Long minimum=buf.readBoolean()?Long.valueOf(buf.readLong()):null;Long maximum=buf.readBoolean()?Long.valueOf(buf.readLong()):null;int options=count(buf.readInt(),32,"quest editor options");List<String> optionValues=new ArrayList<String>(options);for(int k=0;k<options;k++)optionValues.add(ByteBufUtils.readUTF8String(buf));fieldValues.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.Field(key,label,type,required,minimum,maximum,optionValues));}result.add(new com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot.ElementType(id,name,kind,fieldValues));}return result;}
    private static void writeStringMap(ByteBuf buf,java.util.Map<String,String> values,int maximum){int size=Math.min(maximum,values.size());buf.writeInt(size);int index=0;for(java.util.Map.Entry<String,String> entry:values.entrySet()){if(index++>=size)break;ByteBufUtils.writeUTF8String(buf,safe(entry.getKey()));ByteBufUtils.writeUTF8String(buf,safe(entry.getValue()));}}
    private static java.util.Map<String,String> readStringMap(ByteBuf buf,int maximum){int size=count(buf.readInt(),maximum,"quest string map");java.util.Map<String,String> result=new java.util.LinkedHashMap<String,String>();for(int i=0;i<size;i++)result.put(ByteBufUtils.readUTF8String(buf),ByteBufUtils.readUTF8String(buf));return result;}
    private static int count(int value,int maximum,String label){if(value<0||value>maximum)throw new IllegalArgumentException(label+" count out of bounds: "+value);return value;}

    static void writeServerToolsSection(ByteBuf buf, TerminalServerToolsSectionModel model) {
        buf.writeBoolean(model != null);
        if (model == null) {
            return;
        }
        ByteBufUtils.writeUTF8String(buf, safe(model.getServiceState()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getCurrentServerId()));
        writeStringList(buf, model.getServerLines());
        writeStringList(buf, model.getServerIds());
        writeStringList(buf, model.getWarpLines());
        writeStringList(buf, model.getWarpNames());
        writeStringList(buf, model.getWarpSubtitles());
        writeStringList(buf, model.getWarpStateLabels());
        writeStringList(buf, model.getRecentTransferLines());
        writeStringList(buf, model.getHomeLines());
        writeStringList(buf, model.getHomeNames());
        writeStringList(buf, model.getHomeSubtitles());
        writeStringList(buf, model.getTpaDirections());
        writeStringList(buf, model.getTpaCounterpartyNames());
        writeStringList(buf, model.getTpaTargetServerIds());
        writeStringList(buf, model.getTpaStatusLabels());
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedWarpName()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedWarpTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedWarpDetail()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTargetServerId()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTargetLocation()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedWarpDescription()));
        buf.writeBoolean(model.isSelectedWarpEnabled());
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedHomeName()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedHomeTargetServerId()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedHomeTargetLocation()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedHomeDescription()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getRecentSourceServerId()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getRecentTargetServerId()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getRecentTransferStatus()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getRecentTransferTime()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getRecentTransferSummary()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getBody()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getSeverityName()));
    }

    static void writeLandSection(ByteBuf buf, TerminalLandSectionModel model) {
        buf.writeBoolean(model != null);
        if (model == null) return;
        ByteBufUtils.writeUTF8String(buf, safe(model.getServiceState()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getServerId()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getProtectionMode()));
        buf.writeInt(model.getDimensionId());
        buf.writeInt(model.getCenterChunkX());
        buf.writeInt(model.getCenterChunkZ());
        buf.writeInt(model.getSelectedChunkX());
        buf.writeInt(model.getSelectedChunkZ());
        buf.writeInt(model.getUsedClaims());
        buf.writeInt(model.getMaxClaims());
        ByteBufUtils.writeUTF8String(buf, safe(model.getTab()));
        buf.writeInt(model.getViewportChunkX());
        buf.writeInt(model.getViewportChunkZ());
        ByteBufUtils.writeUTF8String(buf, safe(model.getZoom()));
        int mapCellCount = Math.min(961, model.getMapCells().size());
        buf.writeInt(mapCellCount);
        for (int index = 0; index < mapCellCount; index++) {
            TerminalLandSectionModel.MapCellModel cell = model.getMapCells().get(index);
            buf.writeInt(cell.getChunkX());
            buf.writeInt(cell.getChunkZ());
            ByteBufUtils.writeUTF8String(buf, safe(cell.getState()));
            buf.writeLong(cell.getTitleId());
            buf.writeLong(cell.getVersion());
        }
        writeLongList(buf, model.getOwnedTitleIds());
        writeIntList(buf, model.getOwnedChunkXs());
        writeIntList(buf, model.getOwnedChunkZs());
        writeLongList(buf, model.getOwnedVersions());
        buf.writeInt(model.getPageIndex());
        buf.writeInt(model.getTotalPages());
        buf.writeInt(model.getTotalEntries());
        buf.writeLong(model.getSelectedTitleId());
        buf.writeLong(model.getSelectedVersion());
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedState()));
        buf.writeBoolean(model.isCanClaim());
        buf.writeBoolean(model.isCanUnclaim());
        ByteBufUtils.writeUTF8String(buf, safe(model.getFeedbackCode()));
    }

    static TerminalLandSectionModel readLandSection(ByteBuf buf) {
        if (!buf.readBoolean()) return null;
        String serviceState = ByteBufUtils.readUTF8String(buf);
        String serverId = ByteBufUtils.readUTF8String(buf);
        String protectionMode = ByteBufUtils.readUTF8String(buf);
        int dimensionId = buf.readInt();
        int centerX = buf.readInt();
        int centerZ = buf.readInt();
        int selectedX = buf.readInt();
        int selectedZ = buf.readInt();
        int usedClaims = buf.readInt();
        int maxClaims = buf.readInt();
        String tab = ByteBufUtils.readUTF8String(buf);
        int viewportX = buf.readInt();
        int viewportZ = buf.readInt();
        String zoom = ByteBufUtils.readUTF8String(buf);
        int cellCount = Math.max(0, Math.min(961, buf.readInt()));
        List<TerminalLandSectionModel.MapCellModel> cells =
            new ArrayList<TerminalLandSectionModel.MapCellModel>(cellCount);
        for (int index = 0; index < cellCount; index++) {
            cells.add(new TerminalLandSectionModel.MapCellModel(buf.readInt(), buf.readInt(),
                ByteBufUtils.readUTF8String(buf), buf.readLong(), buf.readLong()));
        }
        return new TerminalLandSectionModel(serviceState, serverId, protectionMode, dimensionId, centerX, centerZ,
            selectedX, selectedZ, usedClaims, maxClaims, tab, viewportX, viewportZ, zoom, cells,
            readLongList(buf), readIntList(buf), readIntList(buf), readLongList(buf), buf.readInt(), buf.readInt(),
            buf.readInt(), buf.readLong(), buf.readLong(), ByteBufUtils.readUTF8String(buf), buf.readBoolean(),
            buf.readBoolean(), ByteBufUtils.readUTF8String(buf));
    }

    static void writeNotificationCenter(ByteBuf buf, TerminalNotificationCenterModel model) {
        buf.writeBoolean(model != null);
        if (model == null) return;
        ByteBufUtils.writeUTF8String(buf, safe(model.getServiceState()));
        buf.writeInt(Math.max(0, model.getRetainedEntries()));
        int count = Math.min(TerminalNotificationFeed.MAX_ENTRIES, model.getEntries().size());
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            TerminalNotificationCenterModel.EntryModel entry = model.getEntries().get(i);
            ByteBufUtils.writeUTF8String(buf, safe(entry.getSourceId()));
            ByteBufUtils.writeUTF8String(buf, safe(entry.getTargetPageId()));
            ByteBufUtils.writeUTF8String(buf, safe(entry.getTargetRecordId()));
            ByteBufUtils.writeUTF8String(buf, safe(entry.getTitle()));
            ByteBufUtils.writeUTF8String(buf, safe(entry.getBody()));
            ByteBufUtils.writeUTF8String(buf, safe(entry.getSeverityName()));
            buf.writeInt(Math.max(1, entry.getOccurrences()));
        }
    }

    static TerminalNotificationCenterModel readNotificationCenter(ByteBuf buf) {
        if (!buf.readBoolean()) return null;
        String state = ByteBufUtils.readUTF8String(buf);
        int retained = Math.max(0, buf.readInt());
        int count = Math.max(0, Math.min(TerminalNotificationFeed.MAX_ENTRIES, buf.readInt()));
        List<TerminalNotificationCenterModel.EntryModel> entries =
            new ArrayList<TerminalNotificationCenterModel.EntryModel>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new TerminalNotificationCenterModel.EntryModel(ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf), Math.max(1, buf.readInt())));
        }
        return new TerminalNotificationCenterModel(state, entries, retained);
    }

    static TerminalServerToolsSectionModel readServerToolsSection(ByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        return new TerminalServerToolsSectionModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readBoolean(),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            new TerminalServerToolsSectionModel.ActionFeedbackModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf)));
    }

    static void writeCustomMarketSection(ByteBuf buf, TerminalCustomMarketSectionModel model) {
        buf.writeBoolean(model != null);
        if (model == null) {
            return;
        }
        ByteBufUtils.writeUTF8String(buf, safe(model.getServiceState()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getBrowserHint()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getScopeLabel()));
        writeStringList(buf, model.getActiveListingLines());
        writeStringList(buf, model.getActiveListingIds());
        writeStringList(buf, model.getActiveListingIconRefs());
        writeStringList(buf, model.getSellingListingLines());
        writeStringList(buf, model.getSellingListingIds());
        writeStringList(buf, model.getSellingListingIconRefs());
        writeStringList(buf, model.getPendingListingLines());
        writeStringList(buf, model.getPendingListingIds());
        writeStringList(buf, model.getPendingListingIconRefs());
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedListingId()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedPrice()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedStatus()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedCounterparty()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedItemIdentity()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTradeSummary()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedActionHint()));
        buf.writeBoolean(model.isCanBuy());
        buf.writeBoolean(model.isCanCancel());
        buf.writeBoolean(model.isCanClaim());
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getBody()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getSeverityName()));
        writeBrowsePage(buf, model.getBrowseEntries(), model.getBrowseQuery(), model.getBrowsePageIndex(),
            model.getBrowsePageSize(), model.getBrowseTotalEntries(), model.hasPreviousPage(), model.hasNextPage());
    }

    static TerminalCustomMarketSectionModel readCustomMarketSection(ByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        TerminalCustomMarketSectionModel model = new TerminalCustomMarketSectionModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            new TerminalCustomMarketSectionModel.ActionFeedbackModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf)));
        return readBrowsePage(buf, model);
    }

    static void writeExchangeMarketSection(ByteBuf buf, TerminalExchangeMarketSectionModel model) {
        buf.writeBoolean(model != null);
        if (model == null) {
            return;
        }
        ByteBufUtils.writeUTF8String(buf, safe(model.getServiceState()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getBrowserHint()));
        writeStringList(buf, model.getTargetCodes());
        writeStringList(buf, model.getTargetLabels());
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTargetCode()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedCoinCode()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTargetTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getSelectedTargetSummary()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getHeldSummary()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getInputRegistryName()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getPairCode()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getInputAssetCode()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getOutputAssetCode()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getRuleVersion()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getLimitStatus()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getReasonCode()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getNotes()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getInputQuantity()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getNominalFaceValue()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getEffectiveExchangeValue()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getContributionValue()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getDiscountStatus()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getRateDisplay()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getExecutionHint()));
        buf.writeBoolean(model.isExecutable());
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getBody()));
        ByteBufUtils.writeUTF8String(buf, safe(model.getActionFeedback().getSeverityName()));
        writeBrowsePage(buf, model.getBrowseEntries(), model.getBrowseQuery(), model.getBrowsePageIndex(),
            model.getBrowsePageSize(), model.getBrowseTotalEntries(), model.hasPreviousPage(), model.hasNextPage());
    }

    static TerminalExchangeMarketSectionModel readExchangeMarketSection(ByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        TerminalExchangeMarketSectionModel model = new TerminalExchangeMarketSectionModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            readStringList(buf),
            readStringList(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readBoolean(),
            new TerminalExchangeMarketSectionModel.ActionFeedbackModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf)));
        return readBrowsePage(buf, model);
    }

    static void writeMarketSection(ByteBuf buf, TerminalMarketSectionModel marketSectionModel) {
        buf.writeBoolean(marketSectionModel != null);
        if (marketSectionModel == null) {
            return;
        }

        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getRoutePageId()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getServiceState()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getBrowserHint()));
        writeStringList(buf, marketSectionModel.getProductKeys());
        writeStringList(buf, marketSectionModel.getProductLabels());
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getSelectedProductKey()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getSelectedProductName()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getSelectedProductUnit()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLatestTradePrice()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getHighestBid()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLowestAsk()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getBestBidQuantity()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getBestAskQuantity()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getVolume24h()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getTurnover24h()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getSourceAvailable()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLockedEscrowQuantity()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getClaimableQuantity()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getFrozenFunds()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getSummaryNotice()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getSourceMode()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getWarehouseNotice()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLimitBuyPreview()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLimitSellPreview()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getInstantBuyPreview()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getInstantSellPreview()));
        writeStringList(buf, marketSectionModel.getAskLines());
        writeStringList(buf, marketSectionModel.getBidLines());
        writeStringList(buf, marketSectionModel.getMyOrderLines());
        writeStringList(buf, marketSectionModel.getMyOrderIds());
        writeStringList(buf, marketSectionModel.getMyOrderCancelableFlags());
        writeStringList(buf, marketSectionModel.getClaimLines());
        writeStringList(buf, marketSectionModel.getClaimIds());
        writeStringList(buf, marketSectionModel.getRuleLines());
        buf.writeBoolean(marketSectionModel.isDepositEnabled());
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLimitBuyDraft().getSelectedProductKey()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLimitBuyDraft().getPriceText()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLimitBuyDraft().getQuantityText()));
        buf.writeBoolean(marketSectionModel.getLimitBuyDraft().isSubmitEnabled());
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLimitSellDraft().getSelectedProductKey()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLimitSellDraft().getPriceText()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getLimitSellDraft().getQuantityText()));
        buf.writeBoolean(marketSectionModel.getLimitSellDraft().isSubmitEnabled());
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getInstantBuyDraft().getSelectedProductKey()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getInstantBuyDraft().getQuantityText()));
        buf.writeBoolean(marketSectionModel.getInstantBuyDraft().isSubmitEnabled());
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getInstantSellDraft().getSelectedProductKey()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getInstantSellDraft().getQuantityText()));
        buf.writeBoolean(marketSectionModel.getInstantSellDraft().isSubmitEnabled());
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getActionFeedback().getTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getActionFeedback().getBody()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getActionFeedback().getSeverityName()));
        writeCatalogProducts(buf, marketSectionModel.getCatalogProducts());
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getCatalogQuery()));
        buf.writeInt(marketSectionModel.getCatalogPageIndex());
        buf.writeInt(marketSectionModel.getCatalogPageSize());
        buf.writeInt(marketSectionModel.getCatalogTotalEntries());
        buf.writeBoolean(marketSectionModel.hasCatalogPreviousPage());
        buf.writeBoolean(marketSectionModel.hasCatalogNextPage());
        writeVaultAssets(buf, marketSectionModel.getVaultAssets());
        buf.writeInt(marketSectionModel.getHistoryTotalEntries());
        buf.writeInt(marketSectionModel.getHistoryPageIndex());
        buf.writeInt(marketSectionModel.getHistoryPageSize());
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getAccountCenterTab()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getCenterBankAvailable()));
        ByteBufUtils.writeUTF8String(buf, safe(marketSectionModel.getCenterFrozenFunds()));
        buf.writeInt(marketSectionModel.getCenterVaultUsedSlots());
        buf.writeInt(marketSectionModel.getCenterVaultTotalSlots());
        buf.writeInt(marketSectionModel.getCenterActiveOrders());
        buf.writeInt(marketSectionModel.getCenterPendingDeliveries());
        buf.writeInt(marketSectionModel.getCenterRecoveryItems());
        writeStringList(buf, marketSectionModel.getCenterRowKinds());
        writeStringList(buf, marketSectionModel.getCenterRowIconRefs());
        writeAccountCenterRows(buf, marketSectionModel.getAccountCenterRows());
    }

    static TerminalMarketSectionModel readMarketSection(ByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        TerminalMarketSectionModel model = new TerminalMarketSectionModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            readStringList(buf),
            readStringList(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            readStringList(buf),
            buf.readBoolean(),
            new TerminalMarketSectionModel.LimitBuyDraftModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                buf.readBoolean()),
            new TerminalMarketSectionModel.LimitSellDraftModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                buf.readBoolean()),
            new TerminalMarketSectionModel.InstantDraftModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                buf.readBoolean()),
            new TerminalMarketSectionModel.InstantDraftModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                buf.readBoolean()),
            new TerminalMarketSectionModel.ActionFeedbackModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf))).withCatalogPage(
                    readCatalogProducts(buf),
                    ByteBufUtils.readUTF8String(buf),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean())
                .withVaultAssets(readVaultAssets(buf));
        model.withHistoryPage(
            model.getMyOrderLines(),
            model.getMyOrderIds(),
            model.getMyOrderCancelableFlags(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt());
        model.withAccountCenter(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
            buf.readInt(), readStringList(buf), readStringList(buf));
        return model.withAccountCenterRows(readAccountCenterRows(buf));
    }

    private static void writeAccountCenterRows(ByteBuf buf, List<TerminalMarketAccountCenterRow> rows) {
        int count = rows == null ? 0 : Math.min(50, rows.size());
        buf.writeInt(count);
        for (int index = 0; index < count; index++) {
            TerminalMarketAccountCenterRow row = rows.get(index);
            ByteBufUtils.writeUTF8String(buf, safe(row.getRecordId()));
            ByteBufUtils.writeUTF8String(buf, safe(row.getKind()));
            ByteBufUtils.writeUTF8String(buf, safe(row.getRegistryName())); buf.writeInt(row.getMeta());
            ByteBufUtils.writeUTF8String(buf, safe(row.getSide()));
            ByteBufUtils.writeUTF8String(buf, safe(row.getOrderType()));
            buf.writeLong(row.getUnitPrice()); buf.writeLong(row.getOriginalQuantity());
            buf.writeLong(row.getFilledQuantity()); buf.writeLong(row.getRemainingQuantity());
            ByteBufUtils.writeUTF8String(buf, safe(row.getStatus()));
            ByteBufUtils.writeUTF8String(buf, safe(row.getCreatedAt()));
            buf.writeLong(row.getRelatedOrderId()); buf.writeLong(row.getGrossAmount()); buf.writeLong(row.getFeeAmount());
            ByteBufUtils.writeUTF8String(buf, safe(row.getMessage())); buf.writeBoolean(row.isCancelable());
            buf.writeLong(row.getUpdatedAtEpochSeconds()); buf.writeLong(row.getReservedFunds());
        }
    }

    private static List<TerminalMarketAccountCenterRow> readAccountCenterRows(ByteBuf buf) {
        int count = Math.max(0, Math.min(50, buf.readInt()));
        List<TerminalMarketAccountCenterRow> rows = new ArrayList<TerminalMarketAccountCenterRow>(count);
        for (int index = 0; index < count; index++) {
            rows.add(new TerminalMarketAccountCenterRow(ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf), buf.readInt(),
                ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf), buf.readLong(), buf.readLong(),
                buf.readLong(), buf.readLong(), ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf),
                buf.readLong(), buf.readLong(), buf.readLong(), ByteBufUtils.readUTF8String(buf), buf.readBoolean(),
                buf.readLong(), buf.readLong()));
        }
        return rows;
    }

    private static void writeCatalogProducts(ByteBuf buf,
        List<TerminalMarketSectionModel.CatalogProductModel> products) {
        int count = products == null ? 0 : Math.min(64, products.size());
        buf.writeInt(count);
        if (products == null) {
            return;
        }
        for (int index = 0; index < count; index++) {
            TerminalMarketSectionModel.CatalogProductModel product = products.get(index);
            ByteBufUtils.writeUTF8String(buf, safe(product.getProductKey()));
            ByteBufUtils.writeUTF8String(buf, safe(product.getRegistryName()));
            buf.writeInt(product.getMeta());
            ByteBufUtils.writeUTF8String(buf, safe(product.getDisplayName()));
            ByteBufUtils.writeUTF8String(buf, safe(product.getUnitLabel()));
            buf.writeInt(product.getSortOrder());
            buf.writeBoolean(product.isEnabled());
            buf.writeLong(product.getReferencePrice());
            ByteBufUtils.writeUTF8String(buf, safe(product.getTradability()));
            writeCatalogMarketSummary(buf, product.getMarketSummary());
        }
    }

    private static List<TerminalMarketSectionModel.CatalogProductModel> readCatalogProducts(ByteBuf buf) {
        int count = Math.max(0, Math.min(64, buf.readInt()));
        List<TerminalMarketSectionModel.CatalogProductModel> products =
            new ArrayList<TerminalMarketSectionModel.CatalogProductModel>(count);
        for (int index = 0; index < count; index++) {
            products.add(new TerminalMarketSectionModel.CatalogProductModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                buf.readInt(),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                buf.readInt(),
                buf.readBoolean(),
                buf.readLong(),
                ByteBufUtils.readUTF8String(buf),
                readCatalogMarketSummary(buf)));
        }
        return products;
    }

    private static void writeVaultAssets(ByteBuf buf, List<TerminalMarketSectionModel.VaultAssetModel> assets) {
        int count = assets == null ? 0 : Math.min(54, assets.size());
        buf.writeInt(count);
        for (int index = 0; index < count; index++) {
            TerminalMarketSectionModel.VaultAssetModel asset = assets.get(index);
            buf.writeInt(asset.getSlotIndex());
            ByteBufUtils.writeUTF8String(buf, safe(asset.getRegistryName()));
            buf.writeInt(asset.getMeta());
            ByteBufUtils.writeUTF8String(buf, safe(asset.getDisplayName()));
            buf.writeInt(asset.getQuantity());
            ByteBufUtils.writeUTF8String(buf, safe(asset.getStandardizedProductKey()));
            buf.writeBoolean(asset.isStandardizedEligible());
            ByteBufUtils.writeUTF8String(buf, safe(asset.getStandardizedReason()));
        }
    }

    private static List<TerminalMarketSectionModel.VaultAssetModel> readVaultAssets(ByteBuf buf) {
        int count = Math.max(0, Math.min(54, buf.readInt()));
        List<TerminalMarketSectionModel.VaultAssetModel> assets =
            new ArrayList<TerminalMarketSectionModel.VaultAssetModel>(count);
        for (int index = 0; index < count; index++) {
            assets.add(new TerminalMarketSectionModel.VaultAssetModel(buf.readInt(), ByteBufUtils.readUTF8String(buf),
                buf.readInt(), ByteBufUtils.readUTF8String(buf), buf.readInt(), ByteBufUtils.readUTF8String(buf),
                buf.readBoolean(), ByteBufUtils.readUTF8String(buf)));
        }
        return assets;
    }

    private static void writeCatalogMarketSummary(ByteBuf buf,
        TerminalMarketSectionModel.CatalogMarketSummaryModel summary) {
        TerminalMarketSectionModel.CatalogMarketSummaryModel value = summary == null
            ? TerminalMarketSectionModel.CatalogMarketSummaryModel.empty() : summary;
        ByteBufUtils.writeUTF8String(buf, safe(value.getLatestTrade()));
        ByteBufUtils.writeUTF8String(buf, safe(value.getBestBid()));
        ByteBufUtils.writeUTF8String(buf, safe(value.getBestAsk()));
        ByteBufUtils.writeUTF8String(buf, safe(value.getVolume24h()));
        ByteBufUtils.writeUTF8String(buf, safe(value.getAvailable()));
        ByteBufUtils.writeUTF8String(buf, safe(value.getEscrow()));
        ByteBufUtils.writeUTF8String(buf, safe(value.getClaimable()));
        ByteBufUtils.writeUTF8String(buf, safe(value.getDayChange()));
        int count = Math.min(32, value.getPricePoints().size());
        buf.writeInt(count);
        for (int index = 0; index < count; index++) {
            TerminalMarketSectionModel.PricePointModel point = value.getPricePoints().get(index);
            buf.writeLong(point.getOpen());
            buf.writeLong(point.getHigh());
            buf.writeLong(point.getLow());
            buf.writeLong(point.getPrice());
            buf.writeLong(point.getQuantity());
            buf.writeLong(point.getTurnover());
            buf.writeLong(point.getCreatedAtEpochSeconds());
            ByteBufUtils.writeUTF8String(buf, safe(point.getSource()));
        }
    }

    private static TerminalMarketSectionModel.CatalogMarketSummaryModel readCatalogMarketSummary(ByteBuf buf) {
        String latest = ByteBufUtils.readUTF8String(buf);
        String bid = ByteBufUtils.readUTF8String(buf);
        String ask = ByteBufUtils.readUTF8String(buf);
        String volume = ByteBufUtils.readUTF8String(buf);
        String available = ByteBufUtils.readUTF8String(buf);
        String escrow = ByteBufUtils.readUTF8String(buf);
        String claimable = ByteBufUtils.readUTF8String(buf);
        String dayChange = ByteBufUtils.readUTF8String(buf);
        int count = Math.max(0, Math.min(32, buf.readInt()));
        List<TerminalMarketSectionModel.PricePointModel> points =
            new ArrayList<TerminalMarketSectionModel.PricePointModel>(count);
        for (int index = 0; index < count; index++) {
            points.add(new TerminalMarketSectionModel.PricePointModel(buf.readLong(), buf.readLong(), buf.readLong(),
                buf.readLong(), buf.readLong(), buf.readLong(), buf.readLong(), ByteBufUtils.readUTF8String(buf)));
        }
        return new TerminalMarketSectionModel.CatalogMarketSummaryModel(latest, bid, ask, volume, available,
            escrow, claimable, dayChange, points);
    }

    static void writeBankSection(ByteBuf buf, TerminalBankSectionModel bankSectionModel) {
        buf.writeBoolean(bankSectionModel != null);
        if (bankSectionModel == null) {
            return;
        }

        TerminalBankSectionModel.AccountStatusModel accountStatus = bankSectionModel.getAccountStatus();
        buf.writeBoolean(accountStatus.isOpened());
        ByteBufUtils.writeUTF8String(buf, safe(accountStatus.getServiceState()));
        ByteBufUtils.writeUTF8String(buf, safe(accountStatus.getAccountLabel()));
        ByteBufUtils.writeUTF8String(buf, safe(accountStatus.getPlayerStatus()));
        ByteBufUtils.writeUTF8String(buf, safe(accountStatus.getAccountNo()));
        ByteBufUtils.writeUTF8String(buf, safe(accountStatus.getUpdatedAt()));
        buf.writeBoolean(accountStatus.isOpenAllowed());

        TerminalBankSectionModel.BalanceSummaryModel balanceSummary = bankSectionModel.getBalanceSummary();
        ByteBufUtils.writeUTF8String(buf, safe(balanceSummary.getPlayerBalance()));
        ByteBufUtils.writeUTF8String(buf, safe(balanceSummary.getExchangeBalance()));
        ByteBufUtils.writeUTF8String(buf, safe(balanceSummary.getExchangeStatus()));
        ByteBufUtils.writeUTF8String(buf, safe(balanceSummary.getTransferHint()));
        buf.writeBoolean(balanceSummary.isTransferAllowed());

        TerminalBankSectionModel.TransferFormModel transferForm = bankSectionModel.getTransferForm();
        ByteBufUtils.writeUTF8String(buf, safe(transferForm.getTargetPlayerName()));
        ByteBufUtils.writeUTF8String(buf, safe(transferForm.getAmountText()));
        ByteBufUtils.writeUTF8String(buf, safe(transferForm.getComment()));
        buf.writeBoolean(transferForm.isTransferEnabled());

        TerminalBankSectionModel.ActionFeedbackModel actionFeedback = bankSectionModel.getActionFeedback();
        ByteBufUtils.writeUTF8String(buf, safe(actionFeedback.getTitle()));
        ByteBufUtils.writeUTF8String(buf, safe(actionFeedback.getBody()));
        ByteBufUtils.writeUTF8String(buf, safe(actionFeedback.getSeverityName()));

        List<String> ledgerLines = bankSectionModel.getPlayerLedgerLines();
        buf.writeInt(ledgerLines.size());
        for (String ledgerLine : ledgerLines) {
            ByteBufUtils.writeUTF8String(buf, safe(ledgerLine));
        }
    }

    static void writeStringList(ByteBuf buf, List<String> values) {
        List<String> safeValues = values == null ? new ArrayList<String>() : values;
        buf.writeInt(safeValues.size());
        for (String value : safeValues) {
            ByteBufUtils.writeUTF8String(buf, safe(value));
        }
    }

    static List<String> readStringList(ByteBuf buf) {
        int size = buf.readInt();
        List<String> values = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            values.add(ByteBufUtils.readUTF8String(buf));
        }
        return values;
    }

    static void writeLongList(ByteBuf buf, List<Long> values) {
        List<Long> safeValues = values == null ? new ArrayList<Long>() : values;
        buf.writeInt(safeValues.size());
        for (Long value : safeValues) buf.writeLong(value == null ? 0L : value.longValue());
    }

    static List<Long> readLongList(ByteBuf buf) {
        int size = buf.readInt();
        List<Long> values = new ArrayList<Long>(size);
        for (int i = 0; i < size; i++) values.add(Long.valueOf(buf.readLong()));
        return values;
    }

    static void writeIntList(ByteBuf buf, List<Integer> values) {
        List<Integer> safeValues = values == null ? new ArrayList<Integer>() : values;
        buf.writeInt(safeValues.size());
        for (Integer value : safeValues) buf.writeInt(value == null ? 0 : value.intValue());
    }

    static List<Integer> readIntList(ByteBuf buf) {
        int size = buf.readInt();
        List<Integer> values = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++) values.add(Integer.valueOf(buf.readInt()));
        return values;
    }

    private static void writeBrowsePage(ByteBuf buf, List<TerminalMarketBrowseEntry> entries, String query,
        int pageIndex, int pageSize, int totalEntries, boolean previous, boolean next) {
        List<TerminalMarketBrowseEntry> safeEntries = entries == null
            ? java.util.Collections.<TerminalMarketBrowseEntry>emptyList() : entries;
        buf.writeInt(Math.min(64, safeEntries.size()));
        for (int index = 0; index < safeEntries.size() && index < 64; index++) {
            TerminalMarketBrowseEntry entry = safeEntries.get(index);
            ByteBufUtils.writeUTF8String(buf, safe(entry.getKey()));
            ByteBufUtils.writeUTF8String(buf, safe(entry.getItemIdentity()));
            ByteBufUtils.writeUTF8String(buf, safe(entry.getTitle()));
            ByteBufUtils.writeUTF8String(buf, safe(entry.getSubtitle()));
            ByteBufUtils.writeUTF8String(buf, safe(entry.getPrimaryValue()));
            ByteBufUtils.writeUTF8String(buf, safe(entry.getStatus()));
        }
        ByteBufUtils.writeUTF8String(buf, safe(query));
        buf.writeInt(Math.max(0, pageIndex));
        buf.writeInt(Math.max(1, pageSize));
        buf.writeInt(Math.max(0, totalEntries));
        buf.writeBoolean(previous);
        buf.writeBoolean(next);
    }

    private static List<TerminalMarketBrowseEntry> readBrowseEntries(ByteBuf buf) {
        int size = Math.max(0, Math.min(64, buf.readInt()));
        List<TerminalMarketBrowseEntry> entries = new ArrayList<TerminalMarketBrowseEntry>(size);
        for (int index = 0; index < size; index++) {
            entries.add(new TerminalMarketBrowseEntry(
                ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf)));
        }
        return entries;
    }

    private static TerminalCustomMarketSectionModel readBrowsePage(ByteBuf buf,
        TerminalCustomMarketSectionModel model) {
        List<TerminalMarketBrowseEntry> entries = readBrowseEntries(buf);
        return model.withBrowsePage(entries, ByteBufUtils.readUTF8String(buf), buf.readInt(), buf.readInt(),
            buf.readInt(), buf.readBoolean(), buf.readBoolean());
    }

    private static TerminalExchangeMarketSectionModel readBrowsePage(ByteBuf buf,
        TerminalExchangeMarketSectionModel model) {
        List<TerminalMarketBrowseEntry> entries = readBrowseEntries(buf);
        return model.withBrowsePage(entries, ByteBufUtils.readUTF8String(buf), buf.readInt(), buf.readInt(),
            buf.readInt(), buf.readBoolean(), buf.readBoolean());
    }

    static TerminalBankSectionModel readBankSection(ByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }

        TerminalBankSectionModel.AccountStatusModel accountStatus = new TerminalBankSectionModel.AccountStatusModel(
            buf.readBoolean(),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readBoolean());
        TerminalBankSectionModel.BalanceSummaryModel balanceSummary = new TerminalBankSectionModel.BalanceSummaryModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readBoolean());
        TerminalBankSectionModel.TransferFormModel transferForm = new TerminalBankSectionModel.TransferFormModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            buf.readBoolean());
        TerminalBankSectionModel.ActionFeedbackModel actionFeedback = new TerminalBankSectionModel.ActionFeedbackModel(
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf),
            ByteBufUtils.readUTF8String(buf));
        int ledgerSize = buf.readInt();
        List<String> ledgerLines = new ArrayList<String>(ledgerSize);
        for (int i = 0; i < ledgerSize; i++) {
            ledgerLines.add(ByteBufUtils.readUTF8String(buf));
        }
        return new TerminalBankSectionModel(accountStatus, balanceSummary, transferForm, actionFeedback, ledgerLines);
    }

    static void writeNavItems(ByteBuf buf, List<TerminalHomeScreenModel.NavItemModel> items) {
        List<TerminalHomeScreenModel.NavItemModel> safeItems = items == null ? new ArrayList<TerminalHomeScreenModel.NavItemModel>() : items;
        buf.writeInt(safeItems.size());
        for (TerminalHomeScreenModel.NavItemModel item : safeItems) {
            ByteBufUtils.writeUTF8String(buf, safe(item.getPageId()));
            ByteBufUtils.writeUTF8String(buf, safe(item.getLabel()));
            ByteBufUtils.writeUTF8String(buf, safe(item.getSubtitle()));
            buf.writeBoolean(item.isEnabled());
            buf.writeBoolean(item.isSelected());
        }
    }

    static List<TerminalHomeScreenModel.NavItemModel> readNavItems(ByteBuf buf) {
        int size = buf.readInt();
        List<TerminalHomeScreenModel.NavItemModel> items = new ArrayList<TerminalHomeScreenModel.NavItemModel>(size);
        for (int i = 0; i < size; i++) {
            items.add(new TerminalHomeScreenModel.NavItemModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                buf.readBoolean(),
                buf.readBoolean()));
        }
        return items;
    }

    static void writeSections(ByteBuf buf, List<TerminalHomeScreenModel.SectionModel> sections) {
        List<TerminalHomeScreenModel.SectionModel> safeSections = sections == null
            ? new ArrayList<TerminalHomeScreenModel.SectionModel>() : sections;
        buf.writeInt(safeSections.size());
        for (TerminalHomeScreenModel.SectionModel section : safeSections) {
            ByteBufUtils.writeUTF8String(buf, safe(section.getSectionId()));
            ByteBufUtils.writeUTF8String(buf, safe(section.getTitle()));
            ByteBufUtils.writeUTF8String(buf, safe(section.getSummary()));
            ByteBufUtils.writeUTF8String(buf, safe(section.getDetail()));
        }
    }

    static List<TerminalHomeScreenModel.SectionModel> readSections(ByteBuf buf) {
        int size = buf.readInt();
        List<TerminalHomeScreenModel.SectionModel> sections = new ArrayList<TerminalHomeScreenModel.SectionModel>(size);
        for (int i = 0; i < size; i++) {
            sections.add(new TerminalHomeScreenModel.SectionModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf)));
        }
        return sections;
    }

    static void writeNotifications(ByteBuf buf, List<TerminalHomeScreenModel.NotificationModel> items) {
        List<TerminalHomeScreenModel.NotificationModel> safeItems = items == null
            ? new ArrayList<TerminalHomeScreenModel.NotificationModel>() : items;
        buf.writeInt(safeItems.size());
        for (TerminalHomeScreenModel.NotificationModel item : safeItems) {
            ByteBufUtils.writeUTF8String(buf, safe(item.getTitle()));
            ByteBufUtils.writeUTF8String(buf, safe(item.getBody()));
            ByteBufUtils.writeUTF8String(buf, safe(item.getSeverityName()));
            ByteBufUtils.writeUTF8String(buf, safe(item.getSourceId()));
            ByteBufUtils.writeUTF8String(buf, safe(item.getTargetPageId()));
            ByteBufUtils.writeUTF8String(buf, safe(item.getTargetRecordId()));
        }
    }

    static List<TerminalHomeScreenModel.NotificationModel> readNotifications(ByteBuf buf) {
        int size = buf.readInt();
        List<TerminalHomeScreenModel.NotificationModel> items = new ArrayList<TerminalHomeScreenModel.NotificationModel>(size);
        for (int i = 0; i < size; i++) {
            items.add(new TerminalHomeScreenModel.NotificationModel(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf)));
        }
        return items;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static List<TerminalHomeScreenModel.NavItemModel> toNavItemModels(List<TerminalOpenApproval.NavItem> items) {
        List<TerminalHomeScreenModel.NavItemModel> models = new ArrayList<TerminalHomeScreenModel.NavItemModel>();
        if (items == null) {
            return models;
        }
        for (TerminalOpenApproval.NavItem item : items) {
            models.add(new TerminalHomeScreenModel.NavItemModel(
                item.getPageId(),
                item.getLabel(),
                item.getSubtitle(),
                item.isEnabled(),
                item.isSelected()));
        }
        return models;
    }

    private static List<TerminalHomeScreenModel.PageSnapshotModel> toPageSnapshotModels(
        List<TerminalOpenApproval.PageSnapshot> snapshots) {
        List<TerminalHomeScreenModel.PageSnapshotModel> models = new ArrayList<TerminalHomeScreenModel.PageSnapshotModel>();
        if (snapshots == null) {
            return models;
        }
        for (TerminalOpenApproval.PageSnapshot snapshot : snapshots) {
            models.add(new TerminalHomeScreenModel.PageSnapshotModel(
                snapshot.getPageId(),
                snapshot.getTitle(),
                snapshot.getLead(),
                toSectionModels(snapshot.getSections()),
                toBankSectionModel(snapshot.getBankSectionSnapshot()),
                toMarketSectionModel(snapshot.getMarketSectionSnapshot()),
                toCustomMarketSectionModel(snapshot.getCustomMarketSectionSnapshot()),
                toExchangeMarketSectionModel(snapshot.getExchangeMarketSectionSnapshot()),
                toServerToolsSectionModel(snapshot.getServerToolsSectionSnapshot()),
                toLandSectionModel(snapshot.getLandSectionSnapshot()),
                toNotificationCenterModel(snapshot.getNotificationCenterSnapshot()),
                snapshot.getQuestCenterSnapshot()));
        }
        return models;
    }

    private static TerminalServerToolsSectionModel toServerToolsSectionModel(TerminalServerToolsSectionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new TerminalServerToolsSectionModel(
            snapshot.getServiceState(),
            snapshot.getCurrentServerId(),
            snapshot.getServerLines(),
            snapshot.getServerIds(),
            snapshot.getWarpLines(),
            snapshot.getWarpNames(),
            snapshot.getWarpSubtitles(),
            snapshot.getWarpStateLabels(),
            snapshot.getRecentTransferLines(),
            snapshot.getHomeLines(),
            snapshot.getHomeNames(),
            snapshot.getHomeSubtitles(),
            snapshot.getTpaDirections(),
            snapshot.getTpaCounterpartyNames(),
            snapshot.getTpaTargetServerIds(),
            snapshot.getTpaStatusLabels(),
            snapshot.getSelectedWarpName(),
            snapshot.getSelectedWarpTitle(),
            snapshot.getSelectedWarpDetail(),
            snapshot.getSelectedTargetServerId(),
            snapshot.getSelectedTargetLocation(),
            snapshot.getSelectedWarpDescription(),
            snapshot.isSelectedWarpEnabled(),
            snapshot.getSelectedHomeName(),
            snapshot.getSelectedHomeTargetServerId(),
            snapshot.getSelectedHomeTargetLocation(),
            snapshot.getSelectedHomeDescription(),
            snapshot.getRecentSourceServerId(),
            snapshot.getRecentTargetServerId(),
            snapshot.getRecentTransferStatus(),
            snapshot.getRecentTransferTime(),
            snapshot.getRecentTransferSummary(),
            new TerminalServerToolsSectionModel.ActionFeedbackModel(
                snapshot.getActionFeedback().getTitle(),
                snapshot.getActionFeedback().getBody(),
                snapshot.getActionFeedback().getSeverityName()));
    }

    private static TerminalLandSectionModel toLandSectionModel(TerminalLandSectionSnapshot snapshot) {
        if (snapshot == null) return null;
        List<TerminalLandSectionModel.MapCellModel> mapCells =
            new ArrayList<TerminalLandSectionModel.MapCellModel>();
        for (com.jsirgalaxybase.terminal.TerminalLandMapCellSnapshot cell : snapshot.getMapCells()) {
            mapCells.add(new TerminalLandSectionModel.MapCellModel(cell.getChunkX(), cell.getChunkZ(),
                cell.getState(), cell.getTitleId(), cell.getVersion()));
        }
        return new TerminalLandSectionModel(snapshot.getServiceState(), snapshot.getServerId(),
            snapshot.getProtectionMode(), snapshot.getDimensionId(), snapshot.getCenterChunkX(),
            snapshot.getCenterChunkZ(), snapshot.getSelectedChunkX(), snapshot.getSelectedChunkZ(),
            snapshot.getUsedClaims(), snapshot.getMaxClaims(), snapshot.getTab(), snapshot.getViewportChunkX(),
            snapshot.getViewportChunkZ(), snapshot.getZoom(), mapCells,
            snapshot.getOwnedTitleIds(), snapshot.getOwnedChunkXs(), snapshot.getOwnedChunkZs(),
            snapshot.getOwnedVersions(), snapshot.getPageIndex(), snapshot.getTotalPages(), snapshot.getTotalEntries(),
            snapshot.getSelectedTitleId(), snapshot.getSelectedVersion(), snapshot.getSelectedState(),
            snapshot.isCanClaim(), snapshot.isCanUnclaim(), snapshot.getFeedbackCode());
    }

    private static TerminalNotificationCenterModel toNotificationCenterModel(
        com.jsirgalaxybase.terminal.TerminalNotificationCenterSnapshot snapshot) {
        if (snapshot == null) return null;
        List<TerminalNotificationCenterModel.EntryModel> entries =
            new ArrayList<TerminalNotificationCenterModel.EntryModel>();
        for (com.jsirgalaxybase.terminal.TerminalNotificationCenterSnapshot.Entry entry : snapshot.getEntries()) {
            entries.add(new TerminalNotificationCenterModel.EntryModel(entry.getSourceId(), entry.getTargetPageId(),
                entry.getTargetRecordId(), entry.getTitle(), entry.getBody(), entry.getSeverityName(), entry.getOccurrences()));
        }
        return new TerminalNotificationCenterModel(snapshot.getServiceState(), entries, snapshot.getRetainedEntries());
    }

    private static TerminalCustomMarketSectionModel toCustomMarketSectionModel(
        TerminalCustomMarketSectionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new TerminalCustomMarketSectionModel(
            snapshot.getServiceState(),
            snapshot.getBrowserHint(),
            snapshot.getScopeLabel(),
            snapshot.getActiveListingLines(),
            snapshot.getActiveListingIds(),
            snapshot.getActiveListingIconRefs(),
            snapshot.getSellingListingLines(),
            snapshot.getSellingListingIds(),
            snapshot.getSellingListingIconRefs(),
            snapshot.getPendingListingLines(),
            snapshot.getPendingListingIds(),
            snapshot.getPendingListingIconRefs(),
            snapshot.getSelectedListingId(),
            snapshot.getSelectedTitle(),
            snapshot.getSelectedPrice(),
            snapshot.getSelectedStatus(),
            snapshot.getSelectedCounterparty(),
            snapshot.getSelectedItemIdentity(),
            snapshot.getSelectedTradeSummary(),
            snapshot.getSelectedActionHint(),
            snapshot.isCanBuy(),
            snapshot.isCanCancel(),
            snapshot.isCanClaim(),
            new TerminalCustomMarketSectionModel.ActionFeedbackModel(
                snapshot.getActionFeedback().getTitle(),
                snapshot.getActionFeedback().getBody(),
                snapshot.getActionFeedback().getSeverityName())).withBrowsePage(
                    snapshot.getBrowseEntries(), snapshot.getBrowseQuery(), snapshot.getBrowsePageIndex(),
                    snapshot.getBrowsePageSize(), snapshot.getBrowseTotalEntries(), snapshot.hasPreviousPage(),
                    snapshot.hasNextPage());
    }

    private static TerminalExchangeMarketSectionModel toExchangeMarketSectionModel(
        TerminalExchangeMarketSectionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new TerminalExchangeMarketSectionModel(
            snapshot.getServiceState(),
            snapshot.getBrowserHint(),
            snapshot.getTargetCodes(),
            snapshot.getTargetLabels(),
            snapshot.getSelectedTargetCode(),
            snapshot.getSelectedCoinCode(),
            snapshot.getSelectedTargetTitle(),
            snapshot.getSelectedTargetSummary(),
            snapshot.getHeldSummary(),
            snapshot.getInputRegistryName(),
            snapshot.getPairCode(),
            snapshot.getInputAssetCode(),
            snapshot.getOutputAssetCode(),
            snapshot.getRuleVersion(),
            snapshot.getLimitStatus(),
            snapshot.getReasonCode(),
            snapshot.getNotes(),
            snapshot.getInputQuantity(),
            snapshot.getNominalFaceValue(),
            snapshot.getEffectiveExchangeValue(),
            snapshot.getContributionValue(),
            snapshot.getDiscountStatus(),
            snapshot.getRateDisplay(),
            snapshot.getExecutionHint(),
            snapshot.isExecutable(),
            new TerminalExchangeMarketSectionModel.ActionFeedbackModel(
                snapshot.getActionFeedback().getTitle(),
                snapshot.getActionFeedback().getBody(),
                snapshot.getActionFeedback().getSeverityName())).withBrowsePage(
                    snapshot.getBrowseEntries(), snapshot.getBrowseQuery(), snapshot.getBrowsePageIndex(),
                    snapshot.getBrowsePageSize(), snapshot.getBrowseTotalEntries(), snapshot.hasPreviousPage(),
                    snapshot.hasNextPage());
    }

    private static TerminalBankSectionModel toBankSectionModel(TerminalBankSectionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new TerminalBankSectionModel(
            new TerminalBankSectionModel.AccountStatusModel(
                snapshot.getAccountStatus().isOpened(),
                snapshot.getAccountStatus().getServiceState(),
                snapshot.getAccountStatus().getAccountLabel(),
                snapshot.getAccountStatus().getPlayerStatus(),
                snapshot.getAccountStatus().getAccountNo(),
                snapshot.getAccountStatus().getUpdatedAt(),
                snapshot.getAccountStatus().isOpenAllowed()),
            new TerminalBankSectionModel.BalanceSummaryModel(
                snapshot.getBalanceSummary().getPlayerBalance(),
                snapshot.getBalanceSummary().getExchangeBalance(),
                snapshot.getBalanceSummary().getExchangeStatus(),
                snapshot.getBalanceSummary().getTransferHint(),
                snapshot.getBalanceSummary().isTransferAllowed()),
            new TerminalBankSectionModel.TransferFormModel(
                snapshot.getTransferForm().getTargetPlayerName(),
                snapshot.getTransferForm().getAmountText(),
                snapshot.getTransferForm().getComment(),
                snapshot.getTransferForm().isTransferEnabled()),
            new TerminalBankSectionModel.ActionFeedbackModel(
                snapshot.getActionFeedback().getTitle(),
                snapshot.getActionFeedback().getBody(),
                snapshot.getActionFeedback().getSeverityName()),
            snapshot.getPlayerLedgerLines());
    }

    private static TerminalMarketSectionModel toMarketSectionModel(TerminalMarketSectionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new TerminalMarketSectionModel(
            snapshot.getRoutePageId(),
            snapshot.getServiceState(),
            snapshot.getBrowserHint(),
            snapshot.getProductKeys(),
            snapshot.getProductLabels(),
            snapshot.getSelectedProductKey(),
            snapshot.getSelectedProductName(),
            snapshot.getSelectedProductUnit(),
            snapshot.getLatestTradePrice(),
            snapshot.getHighestBid(),
            snapshot.getLowestAsk(),
            snapshot.getBestBidQuantity(),
            snapshot.getBestAskQuantity(),
            snapshot.getVolume24h(),
            snapshot.getTurnover24h(),
            snapshot.getSourceAvailable(),
            snapshot.getLockedEscrowQuantity(),
            snapshot.getClaimableQuantity(),
            snapshot.getFrozenFunds(),
            snapshot.getSummaryNotice(),
            snapshot.getSourceMode(),
            snapshot.getWarehouseNotice(),
            snapshot.getLimitBuyPreview(),
            snapshot.getLimitSellPreview(),
            snapshot.getInstantBuyPreview(),
            snapshot.getInstantSellPreview(),
            snapshot.getAskLines(),
            snapshot.getBidLines(),
            snapshot.getMyOrderLines(),
            snapshot.getMyOrderIds(),
            snapshot.getMyOrderCancelableFlags(),
            snapshot.getClaimLines(),
            snapshot.getClaimIds(),
            snapshot.getRuleLines(),
            snapshot.isDepositEnabled(),
            new TerminalMarketSectionModel.LimitBuyDraftModel(
                snapshot.getLimitBuyDraft().getSelectedProductKey(),
                snapshot.getLimitBuyDraft().getPriceText(),
                snapshot.getLimitBuyDraft().getQuantityText(),
                snapshot.getLimitBuyDraft().isSubmitEnabled()),
            new TerminalMarketSectionModel.LimitSellDraftModel(
                snapshot.getLimitSellDraft().getSelectedProductKey(),
                snapshot.getLimitSellDraft().getPriceText(),
                snapshot.getLimitSellDraft().getQuantityText(),
                snapshot.getLimitSellDraft().isSubmitEnabled()),
            new TerminalMarketSectionModel.InstantDraftModel(
                snapshot.getInstantBuyDraft().getSelectedProductKey(),
                snapshot.getInstantBuyDraft().getQuantityText(),
                snapshot.getInstantBuyDraft().isSubmitEnabled()),
            new TerminalMarketSectionModel.InstantDraftModel(
                snapshot.getInstantSellDraft().getSelectedProductKey(),
                snapshot.getInstantSellDraft().getQuantityText(),
                snapshot.getInstantSellDraft().isSubmitEnabled()),
            new TerminalMarketSectionModel.ActionFeedbackModel(
                snapshot.getActionFeedback().getTitle(),
                snapshot.getActionFeedback().getBody(),
                snapshot.getActionFeedback().getSeverityName())).withCatalogPage(
                    toCatalogProducts(snapshot),
                    snapshot.getCatalogQuery(),
                    snapshot.getCatalogPageIndex(),
                    snapshot.getCatalogPageSize(),
                    snapshot.getCatalogTotalEntries(),
                    snapshot.hasCatalogPreviousPage(),
                    snapshot.hasCatalogNextPage())
                .withVaultAssets(toVaultAssets(snapshot))
                .withHistoryPage(
                    snapshot.getMyOrderLines(),
                    snapshot.getMyOrderIds(),
                    snapshot.getMyOrderCancelableFlags(),
                    snapshot.getHistoryTotalEntries(),
                    snapshot.getHistoryPageIndex(),
                    snapshot.getHistoryPageSize())
                .withAccountCenter(snapshot.getAccountCenterTab(), snapshot.getCenterBankAvailable(),
                    snapshot.getCenterFrozenFunds(), snapshot.getCenterVaultUsedSlots(),
                    snapshot.getCenterVaultTotalSlots(), snapshot.getCenterActiveOrders(),
                    snapshot.getCenterPendingDeliveries(), snapshot.getCenterRecoveryItems(),
                    snapshot.getCenterRowKinds(), snapshot.getCenterRowIconRefs())
                .withAccountCenterRows(snapshot.getAccountCenterRows());
    }

    private static List<TerminalMarketSectionModel.CatalogProductModel> toCatalogProducts(
        TerminalMarketSectionSnapshot snapshot) {
        List<TerminalMarketSectionModel.CatalogProductModel> models =
            new ArrayList<TerminalMarketSectionModel.CatalogProductModel>();
        for (TerminalMarketSectionSnapshot.CatalogProduct product : snapshot.getCatalogProducts()) {
            models.add(new TerminalMarketSectionModel.CatalogProductModel(
                product.getProductKey(), product.getRegistryName(), product.getMeta(), product.getDisplayName(),
                product.getUnitLabel(), product.getSortOrder(), product.isEnabled(), product.getReferencePrice(),
                product.getTradability(), toCatalogSummary(product.getMarketSummary())));
        }
        return models;
    }

    private static TerminalMarketSectionModel.CatalogMarketSummaryModel toCatalogSummary(
        TerminalMarketSectionSnapshot.CatalogMarketSummary summary) {
        List<TerminalMarketSectionModel.PricePointModel> points = new ArrayList<TerminalMarketSectionModel.PricePointModel>();
        if (summary != null) {
            for (TerminalMarketSectionSnapshot.PricePoint point : summary.getPricePoints()) {
                points.add(new TerminalMarketSectionModel.PricePointModel(point.getOpen(), point.getHigh(),
                    point.getLow(), point.getPrice(), point.getQuantity(), point.getTurnover(),
                    point.getCreatedAtEpochSeconds(), point.getSource()));
            }
            return new TerminalMarketSectionModel.CatalogMarketSummaryModel(summary.getLatestTrade(), summary.getBestBid(),
                summary.getBestAsk(), summary.getVolume24h(), summary.getAvailable(), summary.getEscrow(),
                summary.getClaimable(), summary.getDayChange(), points);
        }
        return TerminalMarketSectionModel.CatalogMarketSummaryModel.empty();
    }

    private static List<TerminalMarketSectionModel.VaultAssetModel> toVaultAssets(
        TerminalMarketSectionSnapshot snapshot) {
        List<TerminalMarketSectionModel.VaultAssetModel> models =
            new ArrayList<TerminalMarketSectionModel.VaultAssetModel>();
        for (TerminalMarketSectionSnapshot.VaultAsset asset : snapshot.getVaultAssets()) {
            models.add(new TerminalMarketSectionModel.VaultAssetModel(asset.getSlotIndex(), asset.getRegistryName(),
                asset.getMeta(), asset.getDisplayName(), asset.getQuantity(), asset.getStandardizedProductKey(),
                asset.isStandardizedEligible(), asset.getStandardizedReason()));
        }
        return models;
    }

    private static List<TerminalHomeScreenModel.SectionModel> toSectionModels(List<TerminalOpenApproval.Section> sections) {
        List<TerminalHomeScreenModel.SectionModel> models = new ArrayList<TerminalHomeScreenModel.SectionModel>();
        if (sections == null) {
            return models;
        }
        for (TerminalOpenApproval.Section section : sections) {
            models.add(new TerminalHomeScreenModel.SectionModel(
                section.getSectionId(),
                section.getTitle(),
                section.getSummary(),
                section.getDetail()));
        }
        return models;
    }

    private static List<TerminalHomeScreenModel.NotificationModel> toNotificationModels(
        List<TerminalOpenApproval.NotificationEntry> items) {
        List<TerminalHomeScreenModel.NotificationModel> models = new ArrayList<TerminalHomeScreenModel.NotificationModel>();
        if (items == null) {
            return models;
        }
        for (TerminalOpenApproval.NotificationEntry item : items) {
            models.add(new TerminalHomeScreenModel.NotificationModel(
                item.getTitle(),
                item.getBody(),
                item.getSeverityName(),
                item.getSourceId(),
                item.getTargetPageId(),
                item.getTargetRecordId()));
        }
        return models;
    }

    public static class Handler implements IMessageHandler<OpenTerminalApprovedMessage, IMessage> {

        @Override
        public IMessage onMessage(OpenTerminalApprovedMessage message, MessageContext ctx) {
            TerminalClientScreenController.INSTANCE.queueHomeScreen(message.toScreenModel());
            return null;
        }
    }
}
