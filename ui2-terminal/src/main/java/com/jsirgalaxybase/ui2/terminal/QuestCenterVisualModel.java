package com.jsirgalaxybase.ui2.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Platform-neutral snapshot for the player quest center. */
public final class QuestCenterVisualModel {
    public enum View { BROWSE, DETAIL }
    public enum LoadState { READY, LOADING, EMPTY, ERROR }
    public enum QuestState { LOCKED, AVAILABLE, IN_PROGRESS, CLAIMABLE, COMPLETED }

    private final TerminalVisualModel shell;
    private final View view;
    private final LoadState loadState;
    private final String selectedChapterId;
    private final String selectedQuestId;
    private final String filter;
    private final String query;
    private final String message;
    private final int chapterPageIndex, chapterPageSize, chapterTotalEntries;
    private final int questPageIndex, questPageSize, questTotalEntries;
    private final List<Chapter> chapters;
    private final List<QuestSummary> quests;
    private final QuestDetail detail;
    private final List<Participant> participants;

    public QuestCenterVisualModel(TerminalVisualModel shell, View view, LoadState loadState,
        String selectedChapterId, String selectedQuestId, String filter, String message,
        List<Chapter> chapters, List<QuestSummary> quests, QuestDetail detail) {
        this(shell, view, loadState, selectedChapterId, selectedQuestId, filter, "", message,
            chapters, 0, Math.max(1, chapters == null ? 1 : chapters.size()), chapters == null ? 0 : chapters.size(),
            quests, 0, Math.max(1, quests == null ? 1 : quests.size()), quests == null ? 0 : quests.size(), detail);
    }

    public QuestCenterVisualModel(TerminalVisualModel shell, View view, LoadState loadState,
        String selectedChapterId, String selectedQuestId, String filter, String query, String message,
        List<Chapter> chapters, int chapterPageIndex, int chapterPageSize, int chapterTotalEntries,
        List<QuestSummary> quests, int questPageIndex, int questPageSize, int questTotalEntries,
        QuestDetail detail) {
        if (shell == null) throw new IllegalArgumentException("shell is required");
        this.shell = shell;
        this.view = view == null ? View.BROWSE : view;
        this.loadState = loadState == null ? LoadState.READY : loadState;
        this.selectedChapterId = text(selectedChapterId);
        this.selectedQuestId = text(selectedQuestId);
        this.filter = text(filter);
        this.query = text(query);
        this.message = text(message);
        this.chapters = immutable(chapters);
        this.quests = immutable(quests);
        this.chapterPageSize = Math.max(1, chapterPageSize);
        this.chapterTotalEntries = Math.max(this.chapters.size(), chapterTotalEntries);
        this.chapterPageIndex = clampPage(chapterPageIndex, this.chapterTotalEntries, this.chapterPageSize);
        this.questPageSize = Math.max(1, questPageSize);
        this.questTotalEntries = Math.max(this.quests.size(), questTotalEntries);
        this.questPageIndex = clampPage(questPageIndex, this.questTotalEntries, this.questPageSize);
        this.detail = detail;
        this.participants = Collections.emptyList();
    }

    private QuestCenterVisualModel(QuestCenterVisualModel source,List<Participant> participants){
        this.shell=source.shell;this.view=source.view;this.loadState=source.loadState;
        this.selectedChapterId=source.selectedChapterId;this.selectedQuestId=source.selectedQuestId;
        this.filter=source.filter;this.query=source.query;this.message=source.message;
        this.chapterPageIndex=source.chapterPageIndex;this.chapterPageSize=source.chapterPageSize;
        this.chapterTotalEntries=source.chapterTotalEntries;this.questPageIndex=source.questPageIndex;
        this.questPageSize=source.questPageSize;this.questTotalEntries=source.questTotalEntries;
        this.chapters=source.chapters;this.quests=source.quests;this.detail=source.detail;
        this.participants=immutable(participants);
    }
    public QuestCenterVisualModel withParticipants(List<Participant> values){return new QuestCenterVisualModel(this,values);}

    private static int clampPage(int page, int total, int size) {
        int last = total <= 0 ? 0 : (total - 1) / size;
        return Math.max(0, Math.min(page, last));
    }

    private static String text(String value) { return value == null ? "" : value; }
    private static <T> List<T> immutable(List<T> value) { return value == null ? Collections.<T>emptyList()
        : Collections.unmodifiableList(new ArrayList<T>(value)); }

    public TerminalVisualModel getShell() { return shell; }
    public View getView() { return view; }
    public LoadState getLoadState() { return loadState; }
    public String getSelectedChapterId() { return selectedChapterId; }
    public String getSelectedQuestId() { return selectedQuestId; }
    public String getFilter() { return filter; }
    public String getQuery() { return query; }
    public String getMessage() { return message; }
    public List<Chapter> getChapters() { return chapters; }
    public List<QuestSummary> getQuests() { return quests; }
    public QuestDetail getDetail() { return detail; }
    public List<Participant> getParticipants(){return participants;}

    public static final class Participant {
        private final String type,id,name,role;private final long revision;private final int memberCount;private final List<Member> members;
        public Participant(String type,String id,String name,String role,long revision,int memberCount){this(type,id,name,role,revision,memberCount,Collections.<Member>emptyList());}
        public Participant(String type,String id,String name,String role,long revision,int memberCount,List<Member> members){this.type=text(type);this.id=text(id);this.name=text(name);this.role=text(role);this.revision=Math.max(0L,revision);this.memberCount=Math.max(1,memberCount);this.members=immutable(members);}
        public String getType(){return type;}public String getId(){return id;}public String getName(){return name;}
        public String getRole(){return role;}public long getRevision(){return revision;}public int getMemberCount(){return memberCount;}public List<Member> getMembers(){return members;}
    }
    public static final class Member {
        private final String playerId,role;private final long joinedAt,membershipVersion;
        public Member(String playerId,String role,long joinedAt,long membershipVersion){this.playerId=text(playerId);this.role=text(role);this.joinedAt=Math.max(0L,joinedAt);this.membershipVersion=Math.max(0L,membershipVersion);}
        public String getPlayerId(){return playerId;}public String getRole(){return role;}public long getJoinedAt(){return joinedAt;}public long getMembershipVersion(){return membershipVersion;}
    }
    public int getChapterPageIndex() { return chapterPageIndex; }
    public int getChapterPageSize() { return chapterPageSize; }
    public int getChapterTotalEntries() { return chapterTotalEntries; }
    public int getChapterTotalPages() { return pages(chapterTotalEntries, chapterPageSize); }
    public boolean hasPreviousChapterPage() { return chapterPageIndex > 0; }
    public boolean hasNextChapterPage() { return chapterPageIndex + 1 < getChapterTotalPages(); }
    public int getQuestPageIndex() { return questPageIndex; }
    public int getQuestPageSize() { return questPageSize; }
    public int getQuestTotalEntries() { return questTotalEntries; }
    public int getQuestTotalPages() { return pages(questTotalEntries, questPageSize); }
    public boolean hasPreviousQuestPage() { return questPageIndex > 0; }
    public boolean hasNextQuestPage() { return questPageIndex + 1 < getQuestTotalPages(); }
    private static int pages(int total, int size) { return Math.max(1, (total + size - 1) / size); }

    public static final class Chapter {
        private final String id, name;
        private final int completed, total;
        public Chapter(String id, String name, int completed, int total) {
            this.id = text(id); this.name = text(name); this.completed = Math.max(0, completed);
            this.total = Math.max(this.completed, total);
        }
        public String getId() { return id; } public String getName() { return name; }
        public int getCompleted() { return completed; } public int getTotal() { return total; }
    }

    public static final class QuestSummary {
        private final String id, chapterId, name, subtitle;
        private final QuestState state;
        private final int completedTasks, totalTasks;
        private final VisualItem icon;
        private final boolean tracked;
        public QuestSummary(String id, String chapterId, String name, String subtitle, QuestState state,
            int completedTasks, int totalTasks, VisualItem icon) {this(id,chapterId,name,subtitle,state,completedTasks,totalTasks,icon,false);}
        public QuestSummary(String id, String chapterId, String name, String subtitle, QuestState state,
            int completedTasks, int totalTasks, VisualItem icon,boolean tracked) {
            this.id=text(id);this.chapterId=text(chapterId);this.name=text(name);this.subtitle=text(subtitle);
            this.state=state==null?QuestState.AVAILABLE:state;this.completedTasks=Math.max(0,completedTasks);
            this.totalTasks=Math.max(this.completedTasks,totalTasks);this.icon=icon;this.tracked=tracked;
        }
        public String getId(){return id;} public String getChapterId(){return chapterId;}
        public String getName(){return name;} public String getSubtitle(){return subtitle;}
        public QuestState getState(){return state;} public int getCompletedTasks(){return completedTasks;}
        public int getTotalTasks(){return totalTasks;} public VisualItem getIcon(){return icon;}
        public boolean isTracked(){return tracked;}
    }

    public static final class QuestDetail {
        private final QuestSummary summary;
        private final String description, prerequisiteText, repeatText;
        private final List<TaskRow> tasks;
        private final List<RewardRow> rewards;
        private final int definitionVersion,cycle;
        private final List<PrerequisiteRow> prerequisites;
        public QuestDetail(QuestSummary summary, String description, String prerequisiteText, String repeatText,
            List<TaskRow> tasks, List<RewardRow> rewards) {
            this(summary,description,prerequisiteText,repeatText,tasks,rewards,1,0,Collections.<PrerequisiteRow>emptyList());
        }
        public QuestDetail(QuestSummary summary, String description, String prerequisiteText, String repeatText,
            List<TaskRow> tasks, List<RewardRow> rewards,int definitionVersion,int cycle,
            List<PrerequisiteRow> prerequisites) {
            if (summary == null) throw new IllegalArgumentException("summary is required");
            this.summary=summary;this.description=text(description);this.prerequisiteText=text(prerequisiteText);
            this.repeatText=text(repeatText);this.tasks=immutable(tasks);this.rewards=immutable(rewards);
            this.definitionVersion=Math.max(1,definitionVersion);this.cycle=Math.max(0,cycle);
            this.prerequisites=immutable(prerequisites);
        }
        public QuestSummary getSummary(){return summary;} public String getDescription(){return description;}
        public String getPrerequisiteText(){return prerequisiteText;} public String getRepeatText(){return repeatText;}
        public List<TaskRow> getTasks(){return tasks;} public List<RewardRow> getRewards(){return rewards;}
        public int getDefinitionVersion(){return definitionVersion;} public int getCycle(){return cycle;}
        public List<PrerequisiteRow> getPrerequisites(){return prerequisites;}
    }

    public static final class PrerequisiteRow {
        private final String id,name,state;private final boolean navigable;
        public PrerequisiteRow(String id,String name,String state,boolean navigable){this.id=text(id);this.name=text(name);
            this.state=text(state);this.navigable=navigable;}
        public String getId(){return id;}public String getName(){return name;}public String getState(){return state;}
        public boolean isNavigable(){return navigable;}
    }

    public static final class TaskRow {
        private final String key, label, detail,typeId;
        private final long value, target;
        private final boolean confirmable,submittable,detectable;
        private final List<ComponentRow> components;
        public TaskRow(String key,String label,String detail,long value,long target){this(key,label,detail,value,target,false,false,false,"",Collections.<ComponentRow>emptyList());}
        public TaskRow(String key,String label,String detail,long value,long target,boolean confirmable){this(key,label,detail,value,target,confirmable,false,false,"",Collections.<ComponentRow>emptyList());}
        public TaskRow(String key,String label,String detail,long value,long target,boolean confirmable,boolean submittable){this(key,label,detail,value,target,confirmable,submittable,false,"",Collections.<ComponentRow>emptyList());}
        public TaskRow(String key,String label,String detail,long value,long target,boolean confirmable,boolean submittable,boolean detectable){this(key,label,detail,value,target,confirmable,submittable,detectable,"",Collections.<ComponentRow>emptyList());}
        public TaskRow(String key,String label,String detail,long value,long target,boolean confirmable,boolean submittable,
            boolean detectable,String typeId,List<ComponentRow> components){this.key=text(key);this.label=text(label);
            this.detail=text(detail);this.value=Math.max(0,value);this.target=Math.max(1,target);this.confirmable=confirmable;
            this.submittable=submittable;this.detectable=detectable;this.typeId=text(typeId);this.components=immutable(components);}
        public String getKey(){return key;} public String getLabel(){return label;} public String getDetail(){return detail;}
        public long getValue(){return value;} public long getTarget(){return target;} public boolean isComplete(){return value>=target;} public boolean isConfirmable(){return confirmable;} public boolean isSubmittable(){return submittable;} public boolean isDetectable(){return detectable;}
        public String getTypeId(){return typeId;}public List<ComponentRow> getComponents(){return components;}
    }

    /** A typed task/reward vector row; NBT remains serialized data until the Minecraft bridge resolves it. */
    public static final class ComponentRow {
        private final int sourceIndex,metadata;private final String kind,key,identity,displayName,oreDictionary,nbt;
        private final long quantity,progress,target;private final VisualItem item;
        public ComponentRow(int sourceIndex,String kind,String key,String identity,String displayName,int metadata,
            long quantity,long progress,long target,String oreDictionary,String nbt,VisualItem item){
            this.sourceIndex=sourceIndex;this.kind=text(kind);this.key=text(key);this.identity=text(identity);
            this.displayName=text(displayName);this.metadata=metadata;this.quantity=Math.max(0,quantity);
            this.progress=Math.max(0,progress);this.target=Math.max(0,target);this.oreDictionary=text(oreDictionary);
            this.nbt=text(nbt);this.item=item;
        }
        public int getSourceIndex(){return sourceIndex;}public String getKind(){return kind;}public String getKey(){return key;}
        public String getIdentity(){return identity;}public String getDisplayName(){return displayName;}
        public int getMetadata(){return metadata;}public long getQuantity(){return quantity;}public long getProgress(){return progress;}
        public long getTarget(){return target;}public String getOreDictionary(){return oreDictionary;}public String getNbt(){return nbt;}
        public VisualItem getItem(){return item;}
    }

    public static final class RewardRow {
        private final String key, label, detail;
        private final VisualItem item;
        private final boolean claimable, claimed;
        private final List<ChoiceOption> choices;private final List<ComponentRow> components;private final int selectedChoiceIndex;
        private final String typeId,itemNbt;
        public RewardRow(String key,String label,String detail,VisualItem item,boolean claimable,boolean claimed){this(key,label,detail,item,claimable,claimed,Collections.<ChoiceOption>emptyList(),-1,"",Collections.<ComponentRow>emptyList(),"");}
        public RewardRow(String key,String label,String detail,VisualItem item,boolean claimable,boolean claimed,List<ChoiceOption> choices,int selectedChoiceIndex){this(key,label,detail,item,claimable,claimed,choices,selectedChoiceIndex,"",Collections.<ComponentRow>emptyList(),"");}
        public RewardRow(String key,String label,String detail,VisualItem item,boolean claimable,boolean claimed,
            List<ChoiceOption> choices,int selectedChoiceIndex,String typeId,List<ComponentRow> components,String itemNbt){this.key=text(key);
            this.label=text(label);this.detail=text(detail);this.item=item;this.claimable=claimable;this.claimed=claimed;
            this.choices=normalizedChoices(choices);this.selectedChoiceIndex=selectedChoiceIndex>=0&&selectedChoiceIndex<32?selectedChoiceIndex:-1;
            this.typeId=text(typeId);this.components=immutable(components);this.itemNbt=text(itemNbt);}
        public String getKey(){return key;} public String getLabel(){return label;} public String getDetail(){return detail;}
        public VisualItem getItem(){return item;} public boolean isClaimable(){return claimable;} public boolean isClaimed(){return claimed;}
        public List<ChoiceOption> getChoices(){return choices;}public int getSelectedChoiceIndex(){return selectedChoiceIndex;}public boolean requiresChoice(){return !choices.isEmpty();}
        public String getTypeId(){return typeId;}public List<ComponentRow> getComponents(){return components;}public String getItemNbt(){return itemNbt;}
        private static List<ChoiceOption> normalizedChoices(List<ChoiceOption> values){
            if(values==null||values.isEmpty())return Collections.emptyList();
            List<ChoiceOption> result=new ArrayList<ChoiceOption>();
            for(int i=0;i<values.size();i++){ChoiceOption value=values.get(i);result.add(value.getSourceIndex()<0
                ?new ChoiceOption(i,value.getItem(),value.getLabel(),value.getNbt(),value.getQuantity()):value);}
            return Collections.unmodifiableList(result);
        }
    }
    public static final class ChoiceOption {
        private final int sourceIndex;private final VisualItem item;private final String label,nbt;private final long quantity;
        public ChoiceOption(VisualItem item,String label){this(-1,item,label,"",item==null?0:item.getQuantity());}
        public ChoiceOption(int sourceIndex,VisualItem item,String label,String nbt){this(sourceIndex,item,label,nbt,item==null?0:item.getQuantity());}
        public ChoiceOption(int sourceIndex,VisualItem item,String label,String nbt,long quantity){if(item==null&&(label==null||label.trim().isEmpty()))throw new IllegalArgumentException("choice item or label is required");this.sourceIndex=sourceIndex;this.item=item;this.label=text(label);this.nbt=text(nbt);this.quantity=Math.max(0,quantity);}
        public int getSourceIndex(){return sourceIndex;}public VisualItem getItem(){return item;}
        public String getLabel(){return label.isEmpty()&&item!=null?item.getDisplayName():label;}public String getNbt(){return nbt;}public long getQuantity(){return quantity;}
    }
}
