package com.jsirgalaxybase.ui2.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Platform-neutral chapter administration projection. */
public final class QuestChapterManagementVisualModel {
    private final TerminalVisualModel shell;
    private final String query;
    private final String lifecycle;
    private final String message;
    private final int page;
    private final int pageSize;
    private final long total;
    private final List<Chapter> chapters;
    private final Detail detail;

    public QuestChapterManagementVisualModel(TerminalVisualModel shell, String query, String lifecycle, String message,
        int page, int pageSize, long total, List<Chapter> chapters, Detail detail) {
        if (shell == null) throw new IllegalArgumentException("shell is required");
        this.shell = shell;
        this.query = text(query);
        this.lifecycle = text(lifecycle);
        this.message = text(message);
        this.page = Math.max(0, page);
        this.pageSize = Math.max(1, pageSize);
        this.total = Math.max(0L, total);
        this.chapters = copy(chapters);
        this.detail = detail;
    }

    public TerminalVisualModel getShell() { return shell; }
    public String getQuery() { return query; }
    public String getLifecycle() { return lifecycle; }
    public String getMessage() { return message; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotal() { return total; }
    public List<Chapter> getChapters() { return chapters; }
    public Detail getDetail() { return detail; }
    public int getPages() { return Math.max(1, (int) Math.min(Integer.MAX_VALUE, (total + pageSize - 1L) / pageSize)); }

    public static final class Chapter {
        private final String id, name, lifecycle, hash;
        private final int version, entryCount;
        public Chapter(String id, int version, String name, String lifecycle, String hash, int entryCount) {
            this.id = text(id); this.version = Math.max(1, version); this.name = text(name);
            this.lifecycle = text(lifecycle); this.hash = text(hash); this.entryCount = Math.max(0, entryCount);
        }
        public String getId() { return id; }
        public int getVersion() { return version; }
        public String getName() { return name; }
        public String getLifecycle() { return lifecycle; }
        public String getHash() { return hash; }
        public int getEntryCount() { return entryCount; }
    }

    public static final class Detail {
        private final Chapter summary;
        private final String description, icon, background, visibility;
        private final int backgroundSize;
        private final List<Entry> entries;
        private final List<QuestCandidate> candidates;
        private final List<Dependency> dependencies;
        public Detail(Chapter summary, String description, String icon, String background, int backgroundSize,
            String visibility, List<Entry> entries, List<QuestCandidate> candidates) {
            this(summary, description, icon, background, backgroundSize, visibility, entries, candidates,
                Collections.<Dependency>emptyList());
        }
        public Detail(Chapter summary, String description, String icon, String background, int backgroundSize,
            String visibility, List<Entry> entries, List<QuestCandidate> candidates, List<Dependency> dependencies) {
            if (summary == null) throw new IllegalArgumentException("summary is required");
            this.summary = summary; this.description = text(description); this.icon = text(icon);
            this.background = text(background); this.backgroundSize = Math.max(1, backgroundSize);
            this.visibility = text(visibility); this.entries = copy(entries); this.candidates = copy(candidates); this.dependencies = copy(dependencies);
        }
        public Chapter getSummary() { return summary; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
        public String getBackground() { return background; }
        public int getBackgroundSize() { return backgroundSize; }
        public String getVisibility() { return visibility; }
        public List<Entry> getEntries() { return entries; }
        public List<QuestCandidate> getCandidates() { return candidates; }
        public List<Dependency> getDependencies() { return dependencies; }
    }

    public static final class Entry {
        private final String questId, name;
        private final int x, y, width, height;
        public Entry(String questId, String name, int x, int y, int width, int height) {
            this.questId = text(questId); this.name = text(name); this.x = x; this.y = y;
            this.width = Math.max(1, width); this.height = Math.max(1, height);
        }
        public String getQuestId() { return questId; }
        public String getName() { return name; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    public static final class QuestCandidate {
        private final String id, name;
        public QuestCandidate(String id, String name) { this.id = text(id); this.name = text(name); }
        public String getId() { return id; }
        public String getName() { return name; }
    }
    /** An immutable server-projected prerequisite edge. */
    public static final class Dependency {
        private final String prerequisiteQuestId, questId, kind;
        public Dependency(String prerequisiteQuestId, String questId, String kind) {
            this.prerequisiteQuestId = text(prerequisiteQuestId); this.questId = text(questId); this.kind = text(kind);
        }
        public String getPrerequisiteQuestId() { return prerequisiteQuestId; }
        public String getQuestId() { return questId; }
        public String getKind() { return kind; }
    }

    private static String text(String value) { return value == null ? "" : value; }
    private static <T> List<T> copy(List<T> value) { return value == null ? Collections.<T>emptyList()
        : Collections.unmodifiableList(new ArrayList<T>(value)); }
}
