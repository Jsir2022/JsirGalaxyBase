package com.jsirgalaxybase.quest.bqimport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Produces a deterministic, read-only preview. It never writes either BetterQuesting directory. */
public final class BqProgressConflictPreviewMain {
    private BqProgressConflictPreviewMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException(
            "Usage: BqProgressConflictPreviewMain <left QuestProgress directory> <right QuestProgress directory>");
        BqProgressConflictReport report = new BqProgressConflictAnalyzer().compare(
            read(Paths.get(args[0])), read(Paths.get(args[1])));
        for (BqProgressRelation relation : BqProgressRelation.values()) {
            System.out.println(relation.name().toLowerCase() + "=" + report.count(relation));
        }
        for (BqProgressConflict entry : report.getEntries()) {
            if (entry.getRelation() == BqProgressRelation.DIVERGENT) System.out.println("divergent\t"
                + entry.getPlayerId() + "\t" + entry.getQuestId());
        }
    }

    private static List<BqImportedPlayerProgress> read(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) throw new IllegalArgumentException("not a directory: " + directory);
        List<Path> files = new ArrayList<Path>();
        try (java.util.stream.Stream<Path> stream = Files.list(directory)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(files::add);
        }
        files.sort(Comparator.comparing(path -> path.getFileName().toString()));
        BqPlayerProgressImporter importer = new BqPlayerProgressImporter();
        List<BqImportedPlayerProgress> result = new ArrayList<BqImportedPlayerProgress>();
        for (Path file : files) result.add(importer.read(file));
        return result;
    }
}
