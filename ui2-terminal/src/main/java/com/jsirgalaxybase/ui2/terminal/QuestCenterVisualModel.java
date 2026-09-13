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
    }

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
        public QuestDetail(QuestSummary summary, String description, String prerequisiteText, String repeatText,
            List<TaskRow> tasks, List<RewardRow> rewards) {
            if (summary == null) throw new IllegalArgumentException("summary is required");
            this.summary=summary;this.description=text(description);this.prerequisiteText=text(prerequisiteText);
            this.repeatText=text(repeatText);this.tasks=immutable(tasks);this.rewards=immutable(rewards);
        }
        public QuestSummary getSummary(){return summary;} public String getDescription(){return description;}
        public String getPrerequisiteText(){return prerequisiteText;} public String getRepeatText(){return repeatText;}
        public List<TaskRow> getTasks(){return tasks;} public List<RewardRow> getRewards(){return rewards;}
    }

    public static final class TaskRow {
        private final String key, label, detail;
        private final long value, target;
        public TaskRow(String key,String label,String detail,long value,long target){this.key=text(key);this.label=text(label);
            this.detail=text(detail);this.value=Math.max(0,value);this.target=Math.max(1,target);}
        public String getKey(){return key;} public String getLabel(){return label;} public String getDetail(){return detail;}
        public long getValue(){return value;} public long getTarget(){return target;} public boolean isComplete(){return value>=target;}
    }

    public static final class RewardRow {
        private final String key, label, detail;
        private final VisualItem item;
        private final boolean claimable, claimed;
        private final List<ChoiceOption> choices;private final int selectedChoiceIndex;
        public RewardRow(String key,String label,String detail,VisualItem item,boolean claimable,boolean claimed){this(key,label,detail,item,claimable,claimed,Collections.<ChoiceOption>emptyList(),-1);}
        public RewardRow(String key,String label,String detail,VisualItem item,boolean claimable,boolean claimed,List<ChoiceOption> choices,int selectedChoiceIndex){this.key=text(key);
            this.label=text(label);this.detail=text(detail);this.item=item;this.claimable=claimable;this.claimed=claimed;this.choices=immutable(choices);this.selectedChoiceIndex=selectedChoiceIndex>=0&&selectedChoiceIndex<this.choices.size()?selectedChoiceIndex:-1;}
        public String getKey(){return key;} public String getLabel(){return label;} public String getDetail(){return detail;}
        public VisualItem getItem(){return item;} public boolean isClaimable(){return claimable;} public boolean isClaimed(){return claimed;}
        public List<ChoiceOption> getChoices(){return choices;}public int getSelectedChoiceIndex(){return selectedChoiceIndex;}public boolean requiresChoice(){return !choices.isEmpty();}
    }
    public static final class ChoiceOption {
        private final VisualItem item;private final String label;
        public ChoiceOption(VisualItem item,String label){if(item==null)throw new IllegalArgumentException("item is required");this.item=item;this.label=text(label);}
        public VisualItem getItem(){return item;}public String getLabel(){return label.isEmpty()?item.getDisplayName():label;}
    }
}
