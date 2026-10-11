package com.jsirgalaxybase.quest.bqimport;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BqInventoryMain {
    private BqInventoryMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: BqInventoryMain <DefaultQuests directory>");
        java.nio.file.Path source = Paths.get(args[0]);
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(source);
        System.out.print(inventorySummary(manifest));
        System.out.println("behaviorInventory (unverified types remain BLOCKED):");
        System.out.print(BqBehaviorInventory.from(manifest).toText());
        List<BqImportedQuest> quests = new BqQuestDefinitionImporter().readDirectory(source);
        System.out.println("contentCodecCompatibility (ADAPTED means parsed, not runtime verified):");
        System.out.print(BqCompatibilityReport.create(quests, BqStandardCodec.supportedTaskTypes(),
            BqStandardCodec.supportedRewardTypes()).toText());
        List<BqImportedChapter> chapters = new BqQuestChapterImporter().readDirectory(source);
        int placements = 0;
        for (BqImportedChapter chapter : chapters) placements += chapter.getDefinition().getEntries().size();
        System.out.println("chapters=" + chapters.size());
        System.out.println("chapterPlacements=" + placements);
    }

    static String inventorySummary(BqMigrationManifest manifest) {
        Map<String, Integer> issueCounts = new TreeMap<String, Integer>();
        for (BqMigrationIssue issue : manifest.getIssues()) {
            String key = issue.getSeverity() + "/" + issue.getCode();
            Integer old = issueCounts.get(key);
            issueCounts.put(key, old == null ? 1 : old + 1);
        }
        return "batchId=" + manifest.getBatchId() + '\n'
            + "sourceHash=" + manifest.getSourceHash() + '\n'
            + "quests=" + manifest.getQuests().size() + '\n'
            + "chapters=" + manifest.getChapters().size() + '\n'
            + "applicable=" + manifest.isApplicable() + '\n'
            + "issuesByCode=" + issueCounts + '\n';
    }
}
