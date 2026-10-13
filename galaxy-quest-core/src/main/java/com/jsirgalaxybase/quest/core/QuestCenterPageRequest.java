package com.jsirgalaxybase.quest.core;

/** Bounded, platform-neutral request for the player task center. */
public final class QuestCenterPageRequest {
    public static final int MAX_CHAPTER_PAGE_SIZE = 12;
    public static final int MAX_QUEST_PAGE_SIZE = 24;

    private final String selectedChapterId;
    private final String selectedQuestId;
    private final String filter;
    private final String query;
    private final int chapterPageIndex;
    private final int chapterPageSize;
    private final int questPageIndex;
    private final int questPageSize;

    public QuestCenterPageRequest(String selectedChapterId, String filter, String query,
        int chapterPageIndex, int chapterPageSize, int questPageIndex, int questPageSize) {
        this(selectedChapterId, "", filter, query, chapterPageIndex, chapterPageSize, questPageIndex, questPageSize);
    }

    public QuestCenterPageRequest(String selectedChapterId, String selectedQuestId, String filter, String query,
        int chapterPageIndex, int chapterPageSize, int questPageIndex, int questPageSize) {
        this.selectedChapterId = text(selectedChapterId);
        this.selectedQuestId = bounded(selectedQuestId, 64);
        this.filter = normalizeFilter(filter);
        this.query = text(query).trim();
        this.chapterPageIndex = Math.max(0, chapterPageIndex);
        this.chapterPageSize = bound(chapterPageSize, 4, MAX_CHAPTER_PAGE_SIZE);
        this.questPageIndex = Math.max(0, questPageIndex);
        this.questPageSize = bound(questPageSize, 4, MAX_QUEST_PAGE_SIZE);
    }

    public static QuestCenterPageRequest initial() { return new QuestCenterPageRequest("", "all", "", 0, 4, 0, 4); }
    private static String text(String value) { return value == null ? "" : value; }
    private static String bounded(String value, int maximum) {
        String result = text(value).trim();
        if (result.length() > maximum) throw new IllegalArgumentException("quest id exceeds " + maximum + " characters");
        return result;
    }
    private static int bound(int value, int fallback, int max) { int resolved=value<=0?fallback:value;return Math.min(max,resolved); }
    private static String normalizeFilter(String value) {
        String filter=text(value).trim().toLowerCase(java.util.Locale.ROOT);
        return "active".equals(filter)||"claimable".equals(filter)||"completed".equals(filter)?filter:"all";
    }
    public String getSelectedChapterId(){return selectedChapterId;} public String getSelectedQuestId(){return selectedQuestId;}
    public String getFilter(){return filter;}
    public String getQuery(){return query;} public int getChapterPageIndex(){return chapterPageIndex;}
    public int getChapterPageSize(){return chapterPageSize;} public int getQuestPageIndex(){return questPageIndex;}
    public int getQuestPageSize(){return questPageSize;}
}
