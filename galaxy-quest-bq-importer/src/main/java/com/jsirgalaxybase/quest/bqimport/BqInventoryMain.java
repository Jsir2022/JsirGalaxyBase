package com.jsirgalaxybase.quest.bqimport;

import java.nio.file.Paths;
import java.util.List;

public final class BqInventoryMain {
    private BqInventoryMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: BqInventoryMain <DefaultQuests directory>");
        java.nio.file.Path source = Paths.get(args[0]);
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(source);
        System.out.print(manifest.toText());
        List<BqImportedQuest> quests = new BqQuestDefinitionImporter().readDirectory(source);
        System.out.print(BqCompatibilityReport.create(quests, BqStandardCodec.supportedTaskTypes(),
            BqStandardCodec.supportedRewardTypes()).toText());
        List<BqImportedChapter> chapters = new BqQuestChapterImporter().readDirectory(source);
        int placements = 0;
        for (BqImportedChapter chapter : chapters) placements += chapter.getDefinition().getEntries().size();
        System.out.println("chapters=" + chapters.size());
        System.out.println("chapterPlacements=" + placements);
    }
}
