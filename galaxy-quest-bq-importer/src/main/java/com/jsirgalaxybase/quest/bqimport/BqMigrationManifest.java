package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BqMigrationManifest {
    private final String batchId;
    private final String sourceHash;
    private final List<BqImportedQuest> quests;
    private final List<BqImportedChapter> chapters;
    private final List<BqMigrationIssue> issues;

    BqMigrationManifest(String batchId, String sourceHash, List<BqImportedQuest> quests,
        List<BqImportedChapter> chapters, List<BqMigrationIssue> issues) {
        this.batchId = batchId; this.sourceHash = sourceHash;
        this.quests = Collections.unmodifiableList(new ArrayList<BqImportedQuest>(quests));
        this.chapters = Collections.unmodifiableList(new ArrayList<BqImportedChapter>(chapters));
        List<BqMigrationIssue> ordered = new ArrayList<BqMigrationIssue>(issues);
        Collections.sort(ordered); this.issues = Collections.unmodifiableList(ordered);
    }
    public String getBatchId() { return batchId; }
    public String getSourceHash() { return sourceHash; }
    public List<BqImportedQuest> getQuests() { return quests; }
    public List<BqImportedChapter> getChapters() { return chapters; }
    public List<BqMigrationIssue> getIssues() { return issues; }
    public boolean isApplicable() {
        for (BqMigrationIssue issue : issues) if (issue.getSeverity() == BqMigrationIssue.Severity.ERROR) return false;
        return true;
    }
    public String toText() {
        StringBuilder out = new StringBuilder().append("batchId=").append(batchId).append('\n')
            .append("sourceHash=").append(sourceHash).append('\n').append("quests=").append(quests.size())
            .append('\n').append("chapters=").append(chapters.size()).append('\n')
            .append("applicable=").append(isApplicable()).append('\n');
        for (BqMigrationIssue issue : issues) out.append(issue.getSeverity()).append('|').append(issue.getCode())
            .append('|').append(issue.getSubject()).append('|').append(issue.getMessage()).append('\n');
        for(BqImportedQuest quest:quests)out.append("quest|").append(quest.getDefinition().getId()).append('|').append(clean(quest.getDefinition().getName())).append('|').append(quest.getDefinition().getTasks().size()).append('|').append(quest.getDefinition().getRewards().size()).append('\n');
        for(BqImportedChapter chapter:chapters)out.append("chapter|").append(chapter.getDefinition().getId()).append('|').append(clean(chapter.getDefinition().getName())).append('|').append(chapter.getDisplayOrder()).append('|').append(chapter.getDefinition().getEntries().size()).append('\n');
        return out.toString();
    }
    private static String clean(String value){return value==null?"":value.replace('\n',' ').replace('\r',' ').replace('|','/');}
}
