package com.jsirgalaxybase.quest.bqimport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Prints a private dry-run JSON to stdout; never opens a database or writes inputs. */
public final class BqProgressMigrationPreviewMain {
    private BqProgressMigrationPreviewMain() {}
    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException(
            "Usage: <DefaultQuests> <sourceId=QuestProgressDirectory> [...]");
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(Paths.get(args[0]));
        Map<String, List<BqImportedPlayerProgress>> sources = new TreeMap<String, List<BqImportedPlayerProgress>>();
        Map<String, JsonObject> sourceEvidence = new TreeMap<String, JsonObject>();
        for (int i = 1; i < args.length; i++) {
            int delimiter = args[i].indexOf('=');
            if (delimiter < 1) throw new IllegalArgumentException("source must be ID=directory");
            String id = args[i].substring(0, delimiter);
            if (!id.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("invalid source ID");
            if (sources.containsKey(id)) throw new IllegalArgumentException("duplicate source ID");
            Path directory = Paths.get(args[i].substring(delimiter + 1));
            if (!Files.isDirectory(directory)) throw new IllegalArgumentException("source is not a directory");
            List<Path> files = new ArrayList<Path>();
            try (java.util.stream.Stream<Path> stream = Files.list(directory)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(files::add);
            }
            files.sort(java.util.Comparator.comparing(p -> p.getFileName().toString()));
            JsonObject hashes = new JsonObject();
            List<BqImportedPlayerProgress> players = new ArrayList<BqImportedPlayerProgress>();
            for (Path file : files) {
                byte[] before = Files.readAllBytes(file);
                players.add(new BqPlayerProgressImporter().readSnapshot(file, before));
                if (!Arrays.equals(before, Files.readAllBytes(file)))
                    throw new IllegalStateException("source changed during read; use an immutable offline copy");
                byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(before);
                StringBuilder hash = new StringBuilder();
                for (byte value : digest) hash.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
                hashes.addProperty(file.getFileName().toString(), hash.toString());
            }
            sources.put(id, players); sourceEvidence.put(id, hashes);
        }
        JsonObject report = JsonParser.parseString(new BqProgressMigrationPreview().report(manifest, sources)).getAsJsonObject();
        JsonObject evidence = new JsonObject();
        for (Map.Entry<String, JsonObject> source : sourceEvidence.entrySet()) evidence.add(source.getKey(), source.getValue());
        report.add("sourceFileHashes", evidence);
        System.out.println(new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
    }
}
