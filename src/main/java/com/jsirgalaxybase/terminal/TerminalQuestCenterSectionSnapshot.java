package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Bounded, client-safe quest-center payload. It deliberately contains no player identity. */
public final class TerminalQuestCenterSectionSnapshot {
    public enum View { BROWSE, DETAIL, ADMIN }

    private final String serviceState, message, selectedChapterId, selectedQuestId, filter, query;
    private final View view;
    private final List<Chapter> chapters;
    private final int chapterPageIndex, chapterPageSize, chapterTotalEntries;
    private final List<Quest> quests;
    private final int questPageIndex, questPageSize, questTotalEntries;
    private final Detail detail;
    private final Management management;
    private final ChapterManagement chapterManagement;
    private final List<Participant> participants;

    public TerminalQuestCenterSectionSnapshot(String serviceState,String message,View view,
        String selectedChapterId,String selectedQuestId,String filter,String query,List<Chapter> chapters,
        int chapterPageIndex,int chapterPageSize,int chapterTotalEntries,List<Quest> quests,
        int questPageIndex,int questPageSize,int questTotalEntries,Detail detail){this(serviceState,message,view,selectedChapterId,selectedQuestId,filter,query,chapters,chapterPageIndex,chapterPageSize,chapterTotalEntries,quests,questPageIndex,questPageSize,questTotalEntries,detail,null);}
    public TerminalQuestCenterSectionSnapshot(String serviceState,String message,View view,
        String selectedChapterId,String selectedQuestId,String filter,String query,List<Chapter> chapters,
        int chapterPageIndex,int chapterPageSize,int chapterTotalEntries,List<Quest> quests,
        int questPageIndex,int questPageSize,int questTotalEntries,Detail detail,Management management){
        this(serviceState,message,view,selectedChapterId,selectedQuestId,filter,query,chapters,chapterPageIndex,
            chapterPageSize,chapterTotalEntries,quests,questPageIndex,questPageSize,questTotalEntries,detail,management,null);
    }
    public TerminalQuestCenterSectionSnapshot(String serviceState,String message,View view,
        String selectedChapterId,String selectedQuestId,String filter,String query,List<Chapter> chapters,
        int chapterPageIndex,int chapterPageSize,int chapterTotalEntries,List<Quest> quests,
        int questPageIndex,int questPageSize,int questTotalEntries,Detail detail,Management management,
        ChapterManagement chapterManagement){
        this.serviceState=text(serviceState);this.message=text(message);this.view=view==null?View.BROWSE:view;
        this.selectedChapterId=text(selectedChapterId);this.selectedQuestId=text(selectedQuestId);
        this.filter=text(filter);this.query=text(query);this.chapters=immutable(chapters);
        this.chapterPageIndex=Math.max(0,chapterPageIndex);this.chapterPageSize=Math.max(1,chapterPageSize);
        this.chapterTotalEntries=Math.max(this.chapters.size(),chapterTotalEntries);this.quests=immutable(quests);
        this.questPageIndex=Math.max(0,questPageIndex);this.questPageSize=Math.max(1,questPageSize);
        this.questTotalEntries=Math.max(this.quests.size(),questTotalEntries);this.detail=detail;this.management=management;
        this.chapterManagement=chapterManagement;
        this.participants=Collections.emptyList();
    }
    private TerminalQuestCenterSectionSnapshot(TerminalQuestCenterSectionSnapshot source,List<Participant> participants,
        String message){
        this.serviceState=source.serviceState;this.message=message==null?source.message:text(message);this.view=source.view;
        this.selectedChapterId=source.selectedChapterId;this.selectedQuestId=source.selectedQuestId;
        this.filter=source.filter;this.query=source.query;this.chapters=source.chapters;
        this.chapterPageIndex=source.chapterPageIndex;this.chapterPageSize=source.chapterPageSize;
        this.chapterTotalEntries=source.chapterTotalEntries;this.quests=source.quests;
        this.questPageIndex=source.questPageIndex;this.questPageSize=source.questPageSize;
        this.questTotalEntries=source.questTotalEntries;this.detail=source.detail;this.management=source.management;
        this.chapterManagement=source.chapterManagement;this.participants=immutable(participants);
    }
    public TerminalQuestCenterSectionSnapshot withParticipants(List<Participant> values){return new TerminalQuestCenterSectionSnapshot(this,values,null);}
    public TerminalQuestCenterSectionSnapshot withMessage(String value){return new TerminalQuestCenterSectionSnapshot(this,participants,value);}
    public static TerminalQuestCenterSectionSnapshot unavailable(String message){return new TerminalQuestCenterSectionSnapshot(
        "UNAVAILABLE",message,View.BROWSE,"","","all","",Collections.<Chapter>emptyList(),0,6,0,
        Collections.<Quest>emptyList(),0,7,0,null);}
    private static String text(String value){return value==null?"":value;}
    private static <T> List<T> immutable(List<T> value){return value==null?Collections.<T>emptyList():Collections.unmodifiableList(new ArrayList<T>(value));}
    public String getServiceState(){return serviceState;} public String getMessage(){return message;} public View getView(){return view;}
    public String getSelectedChapterId(){return selectedChapterId;} public String getSelectedQuestId(){return selectedQuestId;}
    public String getFilter(){return filter;} public String getQuery(){return query;} public List<Chapter> getChapters(){return chapters;}
    public int getChapterPageIndex(){return chapterPageIndex;} public int getChapterPageSize(){return chapterPageSize;}
    public int getChapterTotalEntries(){return chapterTotalEntries;} public List<Quest> getQuests(){return quests;}
    public int getQuestPageIndex(){return questPageIndex;} public int getQuestPageSize(){return questPageSize;}
    public int getQuestTotalEntries(){return questTotalEntries;} public Detail getDetail(){return detail;}
    public Management getManagement(){return management;}
    public ChapterManagement getChapterManagement(){return chapterManagement;}
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

    public static final class Chapter {
        private final String id,name;private final int completed,total;
        public Chapter(String id,String name,int completed,int total){this.id=text(id);this.name=text(name);this.completed=Math.max(0,completed);this.total=Math.max(this.completed,total);}
        public String getId(){return id;}public String getName(){return name;}public int getCompleted(){return completed;}public int getTotal(){return total;}
    }
    public static class Quest {
        private final String id,chapterId,name,subtitle,state,iconRegistry,iconName;private final int iconMeta,completedTasks,totalTasks;private final boolean tracked;
        public Quest(String id,String chapterId,String name,String subtitle,String state,int completedTasks,int totalTasks,
            String iconRegistry,int iconMeta,String iconName){this(id,chapterId,name,subtitle,state,completedTasks,totalTasks,iconRegistry,iconMeta,iconName,false);}
        public Quest(String id,String chapterId,String name,String subtitle,String state,int completedTasks,int totalTasks,
            String iconRegistry,int iconMeta,String iconName,boolean tracked){this.id=text(id);this.chapterId=text(chapterId);this.name=text(name);this.subtitle=text(subtitle);this.state=text(state);this.completedTasks=Math.max(0,completedTasks);this.totalTasks=Math.max(this.completedTasks,totalTasks);this.iconRegistry=text(iconRegistry);this.iconMeta=iconMeta;this.iconName=text(iconName);this.tracked=tracked;}
        public String getId(){return id;}public String getChapterId(){return chapterId;}public String getName(){return name;}public String getSubtitle(){return subtitle;}public String getState(){return state;}public int getCompletedTasks(){return completedTasks;}public int getTotalTasks(){return totalTasks;}public String getIconRegistry(){return iconRegistry;}public int getIconMeta(){return iconMeta;}public String getIconName(){return iconName;}
        public boolean isTracked(){return tracked;}
    }
    public static final class Detail {
        private final Quest summary;private final String description,prerequisiteText,repeatText;private final List<Task> tasks;private final List<Reward> rewards;
        public Detail(Quest summary,String description,String prerequisiteText,String repeatText,List<Task> tasks,List<Reward> rewards){if(summary==null)throw new IllegalArgumentException("summary is required");this.summary=summary;this.description=text(description);this.prerequisiteText=text(prerequisiteText);this.repeatText=text(repeatText);this.tasks=immutable(tasks);this.rewards=immutable(rewards);}
        public Quest getSummary(){return summary;}public String getDescription(){return description;}public String getPrerequisiteText(){return prerequisiteText;}public String getRepeatText(){return repeatText;}public List<Task> getTasks(){return tasks;}public List<Reward> getRewards(){return rewards;}
    }
    public static final class Task {
        private final String key,label,detail;private final long value,target;
        public Task(String key,String label,String detail,long value,long target){this.key=text(key);this.label=text(label);this.detail=text(detail);this.value=Math.max(0,value);this.target=Math.max(1,target);}
        public String getKey(){return key;}public String getLabel(){return label;}public String getDetail(){return detail;}public long getValue(){return value;}public long getTarget(){return target;}
    }
    public static final class Reward {
        private final String key,label,detail,itemRegistry,itemName;private final int itemMeta;private final long quantity;private final boolean claimable,claimed;
        private final List<Choice> choices;private final int selectedChoiceIndex;
        public Reward(String key,String label,String detail,String itemRegistry,int itemMeta,String itemName,long quantity,boolean claimable,boolean claimed){this(key,label,detail,itemRegistry,itemMeta,itemName,quantity,claimable,claimed,Collections.<Choice>emptyList(),-1);}
        public Reward(String key,String label,String detail,String itemRegistry,int itemMeta,String itemName,long quantity,boolean claimable,boolean claimed,List<Choice> choices,int selectedChoiceIndex){this.key=text(key);this.label=text(label);this.detail=text(detail);this.itemRegistry=text(itemRegistry);this.itemMeta=itemMeta;this.itemName=text(itemName);this.quantity=Math.max(0,quantity);this.claimable=claimable;this.claimed=claimed;this.choices=immutable(choices);this.selectedChoiceIndex=selectedChoiceIndex>=0&&selectedChoiceIndex<this.choices.size()?selectedChoiceIndex:-1;}
        public String getKey(){return key;}public String getLabel(){return label;}public String getDetail(){return detail;}public String getItemRegistry(){return itemRegistry;}public int getItemMeta(){return itemMeta;}public String getItemName(){return itemName;}public long getQuantity(){return quantity;}public boolean isClaimable(){return claimable;}public boolean isClaimed(){return claimed;}
        public List<Choice> getChoices(){return choices;}public int getSelectedChoiceIndex(){return selectedChoiceIndex;}public boolean requiresChoice(){return !choices.isEmpty();}
    }
    public static final class Choice {
        private final String itemRegistry,itemName;private final int itemMeta;private final long quantity;
        public Choice(String itemRegistry,int itemMeta,String itemName,long quantity){this.itemRegistry=text(itemRegistry);this.itemMeta=itemMeta;this.itemName=text(itemName);this.quantity=Math.max(1,quantity);}
        public String getItemRegistry(){return itemRegistry;}public int getItemMeta(){return itemMeta;}public String getItemName(){return itemName;}public long getQuantity(){return quantity;}
    }
    public static final class Management {
        private final String query,lifecycle;private final int page,pageSize;private final long total;private final List<Definition> definitions;private final DefinitionDetail detail;private final List<ElementType> taskTypes,rewardTypes;
        public Management(String query,String lifecycle,int page,int pageSize,long total,List<Definition> definitions){this(query,lifecycle,page,pageSize,total,definitions,null);}
        public Management(String query,String lifecycle,int page,int pageSize,long total,List<Definition> definitions,DefinitionDetail detail){this(query,lifecycle,page,pageSize,total,definitions,detail,Collections.<ElementType>emptyList(),Collections.<ElementType>emptyList());}
        public Management(String query,String lifecycle,int page,int pageSize,long total,List<Definition> definitions,DefinitionDetail detail,List<ElementType> taskTypes,List<ElementType> rewardTypes){this.query=text(query);this.lifecycle=text(lifecycle);this.page=Math.max(0,page);this.pageSize=Math.max(1,pageSize);this.total=Math.max(0L,total);this.definitions=immutable(definitions);this.detail=detail;this.taskTypes=immutable(taskTypes);this.rewardTypes=immutable(rewardTypes);}
        public String getQuery(){return query;}public String getLifecycle(){return lifecycle;}public int getPage(){return page;}public int getPageSize(){return pageSize;}public long getTotal(){return total;}public List<Definition> getDefinitions(){return definitions;}public DefinitionDetail getDetail(){return detail;}public List<ElementType> getTaskTypes(){return taskTypes;}public List<ElementType> getRewardTypes(){return rewardTypes;}
    }
    public static final class Definition {
        private final String id,name,lifecycle,contentHash;private final int version,taskCount,rewardCount;private final long publishedAt;
        public Definition(String id,int version,String name,String lifecycle,String contentHash,long publishedAt,int taskCount,int rewardCount){this.id=text(id);this.version=Math.max(1,version);this.name=text(name);this.lifecycle=text(lifecycle);this.contentHash=text(contentHash);this.publishedAt=Math.max(0L,publishedAt);this.taskCount=Math.max(0,taskCount);this.rewardCount=Math.max(0,rewardCount);}
        public String getId(){return id;}public int getVersion(){return version;}public String getName(){return name;}public String getLifecycle(){return lifecycle;}public String getContentHash(){return contentHash;}public long getPublishedAt(){return publishedAt;}public int getTaskCount(){return taskCount;}public int getRewardCount(){return rewardCount;}
    }
    public static final class DefinitionDetail {
        private final Definition summary;private final String description,prerequisiteLogic,taskLogic;private final List<String> prerequisites;private final List<Element> tasks,rewards;private final Map<String,String> options;
        public DefinitionDetail(Definition summary,String description,String prerequisiteLogic,String taskLogic,List<String> prerequisites,List<Element> tasks,List<Element> rewards,Map<String,String> options){if(summary==null)throw new IllegalArgumentException("summary is required");this.summary=summary;this.description=text(description);this.prerequisiteLogic=text(prerequisiteLogic);this.taskLogic=text(taskLogic);this.prerequisites=immutable(prerequisites);this.tasks=immutable(tasks);this.rewards=immutable(rewards);this.options=options==null?Collections.<String,String>emptyMap():Collections.unmodifiableMap(new LinkedHashMap<String,String>(options));}
        public Definition getSummary(){return summary;}public String getDescription(){return description;}public String getPrerequisiteLogic(){return prerequisiteLogic;}public String getTaskLogic(){return taskLogic;}public List<String> getPrerequisites(){return prerequisites;}public List<Element> getTasks(){return tasks;}public List<Element> getRewards(){return rewards;}public Map<String,String> getOptions(){return options;}
    }
    public static final class Element {
        private final String key,typeId;private final boolean optional;private final Map<String,String> parameters;
        public Element(String key,String typeId,boolean optional,Map<String,String> parameters){this.key=text(key);this.typeId=text(typeId);this.optional=optional;this.parameters=parameters==null?Collections.<String,String>emptyMap():Collections.unmodifiableMap(new LinkedHashMap<String,String>(parameters));}
        public String getKey(){return key;}public String getTypeId(){return typeId;}public boolean isOptional(){return optional;}public Map<String,String> getParameters(){return parameters;}
    }
    public static final class ElementType{private final String id,name,kind;private final List<Field> fields;public ElementType(String id,String name,String kind,List<Field> fields){this.id=text(id);this.name=text(name);this.kind=text(kind);this.fields=immutable(fields);}public String getId(){return id;}public String getName(){return name;}public String getKind(){return kind;}public List<Field> getFields(){return fields;}}
    public static final class Field{private final String key,label,type;private final boolean required;private final Long minimum,maximum;private final List<String> options;public Field(String key,String label,String type,boolean required,Long minimum,Long maximum,List<String> options){this.key=text(key);this.label=text(label);this.type=text(type);this.required=required;this.minimum=minimum;this.maximum=maximum;this.options=immutable(options);}public String getKey(){return key;}public String getLabel(){return label;}public String getType(){return type;}public boolean isRequired(){return required;}public Long getMinimum(){return minimum;}public Long getMaximum(){return maximum;}public List<String> getOptions(){return options;}}

    public static final class ChapterManagement {
        private final String query,lifecycle;private final int page,pageSize;private final long total;
        private final List<ManagedChapter> chapters;private final ChapterDetail detail;
        public ChapterManagement(String query,String lifecycle,int page,int pageSize,long total,
            List<ManagedChapter> chapters,ChapterDetail detail){this.query=text(query);this.lifecycle=text(lifecycle);
            this.page=Math.max(0,page);this.pageSize=Math.max(1,pageSize);this.total=Math.max(0L,total);
            this.chapters=immutable(chapters);this.detail=detail;}
        public String getQuery(){return query;}public String getLifecycle(){return lifecycle;}public int getPage(){return page;}
        public int getPageSize(){return pageSize;}public long getTotal(){return total;}
        public List<ManagedChapter> getChapters(){return chapters;}public ChapterDetail getDetail(){return detail;}
    }
    public static final class ManagedChapter {
        private final String id,name,lifecycle,contentHash;private final int version,entryCount;private final long publishedAt;
        public ManagedChapter(String id,int version,String name,String lifecycle,String contentHash,long publishedAt,
            int entryCount){this.id=text(id);this.version=Math.max(1,version);this.name=text(name);this.lifecycle=text(lifecycle);
            this.contentHash=text(contentHash);this.publishedAt=Math.max(0L,publishedAt);this.entryCount=Math.max(0,entryCount);}
        public String getId(){return id;}public int getVersion(){return version;}public String getName(){return name;}
        public String getLifecycle(){return lifecycle;}public String getContentHash(){return contentHash;}
        public long getPublishedAt(){return publishedAt;}public int getEntryCount(){return entryCount;}
    }
    public static final class ChapterDetail {
        private final ManagedChapter summary;private final String description,icon,background,visibility;
        private final int backgroundSize;private final List<ChapterPlacement> placements;private final List<ChapterCandidate> candidates;private final List<ChapterDependency> dependencies;
        public ChapterDetail(ManagedChapter summary,String description,String icon,String background,int backgroundSize,
            String visibility,List<ChapterPlacement> placements,List<ChapterCandidate> candidates){if(summary==null)
            throw new IllegalArgumentException("summary is required");this.summary=summary;this.description=text(description);
            this.icon=text(icon);this.background=text(background);this.backgroundSize=Math.max(1,backgroundSize);
            this.visibility=text(visibility);this.placements=immutable(placements);this.candidates=immutable(candidates);this.dependencies=java.util.Collections.<ChapterDependency>emptyList();}
        public ChapterDetail(ManagedChapter summary,String description,String icon,String background,int backgroundSize,
            String visibility,List<ChapterPlacement> placements,List<ChapterCandidate> candidates,List<ChapterDependency> dependencies){if(summary==null)
            throw new IllegalArgumentException("summary is required");this.summary=summary;this.description=text(description);
            this.icon=text(icon);this.background=text(background);this.backgroundSize=Math.max(1,backgroundSize);
            this.visibility=text(visibility);this.placements=immutable(placements);this.candidates=immutable(candidates);this.dependencies=immutable(dependencies);}
        public ManagedChapter getSummary(){return summary;}public String getDescription(){return description;}
        public String getIcon(){return icon;}public String getBackground(){return background;}
        public int getBackgroundSize(){return backgroundSize;}public String getVisibility(){return visibility;}
        public List<ChapterPlacement> getPlacements(){return placements;}public List<ChapterCandidate> getCandidates(){return candidates;}public List<ChapterDependency> getDependencies(){return dependencies;}
    }
    public static final class ChapterPlacement {
        private final String questId,name;private final int x,y,width,height;
        public ChapterPlacement(String questId,String name,int x,int y,int width,int height){this.questId=text(questId);
            this.name=text(name);this.x=x;this.y=y;this.width=Math.max(1,width);this.height=Math.max(1,height);}
        public String getQuestId(){return questId;}public String getName(){return name;}public int getX(){return x;}
        public int getY(){return y;}public int getWidth(){return width;}public int getHeight(){return height;}
    }
    public static final class ChapterCandidate {
        private final String questId,name;
        public ChapterCandidate(String questId,String name){this.questId=text(questId);this.name=text(name);}
        public String getQuestId(){return questId;}public String getName(){return name;}
    }
    public static final class ChapterDependency {
        private final String prerequisiteQuestId,questId,kind;
        public ChapterDependency(String prerequisiteQuestId,String questId,String kind){this.prerequisiteQuestId=text(prerequisiteQuestId);this.questId=text(questId);this.kind=text(kind);}
        public String getPrerequisiteQuestId(){return prerequisiteQuestId;}public String getQuestId(){return questId;}public String getKind(){return kind;}
    }
}
