package com.jsirgalaxybase.quest.bqimport;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

public final class BqProgressInventoryMain {
    private BqProgressInventoryMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: BqProgressInventoryMain <QuestProgress directory>");
        Path directory = Paths.get(args[0]);
        BqPlayerProgressImporter importer = new BqPlayerProgressImporter();
        int players = 0;
        int quests = 0;
        int completed = 0;
        int claimed = 0;
        Map<String, Integer> taskTypes = new TreeMap<String, Integer>();
        Map<String, Integer> progressedTypes = new TreeMap<String, Integer>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : files) {
                BqImportedPlayerProgress player = importer.read(file);
                players++;
                for (BqImportedQuestProgress quest : player.getQuests()) {
                    quests++;
                    if (quest.isCompleted()) completed++;
                    if (quest.isClaimed()) claimed++;
                    for (BqImportedTaskProgress task : quest.getTasks()) {
                        increment(taskTypes, task.getTypeId());
                        if (task.isComplete() || !task.getValues().isEmpty()) increment(progressedTypes, task.getTypeId());
                    }
                }
            }
        }
        System.out.println("players=" + players);
        System.out.println("questProgress=" + quests);
        System.out.println("completed=" + completed);
        System.out.println("claimed=" + claimed);
        System.out.println("taskTypes=" + taskTypes);
        System.out.println("progressedTaskTypes=" + progressedTypes);
    }

    private static void increment(Map<String, Integer> counts, String key) {
        Integer old = counts.get(key);
        counts.put(key, old == null ? 1 : old + 1);
    }
}
