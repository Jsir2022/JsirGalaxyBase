package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BqProgressMigrationPreviewMainTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final UUID player = new UUID(0, 2);
    private Path definitions() throws Exception {
        Path root = temporary.newFolder().toPath();
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        Files.createDirectories(root.resolve("Quests/line"));
        Files.write(root.resolve("Quests/line/q.json"), ("{\"questIDHigh:4\":0,\"questIDLow:4\":1,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Q\"}},\"tasks:9\":{"
            + "\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},\"rewards:9\":{}}")
            .getBytes(StandardCharsets.UTF_8));
        return root;
    }
    private byte[] progress() {
        return ("{\"questProgress:9\":{\"0:10\":{\"questIDHigh:4\":0,\"questIDLow:4\":1,"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\","
            + "\"completeUsers:9\":{\"0:8\":\"" + player + "\"}}},\"completed:9\":{\"0:10\":{"
            + "\"uuid:8\":\"" + player + "\",\"claimed:1\":1,\"timestamp:4\":42}}}}}")
            .getBytes(StandardCharsets.UTF_8);
    }
    private String run(String... args) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, "UTF-8")) {
            System.setOut(capture);
            try { BqProgressMigrationPreviewMain.main(args); } finally { System.setOut(original); }
        }
        return bytes.toString("UTF-8");
    }
    @Test public void actualCliBindsHashedBytesAndNeverChangesInputs() throws Exception {
        Path definitions = definitions();
        Path a = temporary.newFolder().toPath(), b = temporary.newFolder().toPath();
        byte[] input = progress();
        Files.write(a.resolve(player + ".json"), input); Files.write(b.resolve(player + ".json"), input);
        String first = run(definitions.toString(), "s2=" + b, "lobby=" + a);
        assertEquals(first, run(definitions.toString(), "lobby=" + a, "s2=" + b));
        JsonObject report = JsonParser.parseString(first).getAsJsonObject();
        StringBuilder expected = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(input))
            expected.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        assertEquals(expected.toString(), report.getAsJsonObject("sourceFileHashes")
            .getAsJsonObject("lobby").get(player + ".json").getAsString());
        assertFalse(report.get("applyAllowed").getAsBoolean());
        assertTrue(first.contains("IDENTICAL")); assertTrue(first.contains("\"claimed\": true"));
        assertArrayEquals(input, Files.readAllBytes(a.resolve(player + ".json")));
        assertArrayEquals(input, Files.readAllBytes(b.resolve(player + ".json")));
        try (java.util.stream.Stream<Path> files = Files.list(a)) { assertEquals(1, files.count()); }
    }
    @Test(expected = IllegalArgumentException.class) public void duplicateSourceIsRejected() throws Exception {
        Path root = definitions(), source = temporary.newFolder().toPath();
        run(root.toString(), "s2=" + source, "s2=" + source);
    }
    @Test(expected = IllegalArgumentException.class) public void invalidSourceIdIsRejected() throws Exception {
        run(definitions().toString(), "../bad=" + temporary.newFolder().toPath());
    }
    @Test public void malformedInputPublishesNoPartialReport() throws Exception {
        Path root = definitions(), source = temporary.newFolder().toPath();
        Files.write(source.resolve(player + ".json"), "null".getBytes(StandardCharsets.UTF_8));
        PrintStream original = System.out; ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, "UTF-8")) {
            System.setOut(capture);
            try { BqProgressMigrationPreviewMain.main(new String[]{root.toString(), "s2=" + source}); fail(); }
            catch (com.google.gson.JsonSyntaxException expected) { assertEquals(0, bytes.size()); }
            finally { System.setOut(original); }
        }
    }
    @Test public void parserUsesCapturedBytesEvenWhenBackingFileChanges() throws Exception {
        Path file = temporary.newFolder().toPath().resolve(player + ".json");
        byte[] snapshot = progress(); Files.write(file, "null".getBytes(StandardCharsets.UTF_8));
        BqImportedPlayerProgress parsed = new BqPlayerProgressImporter().readSnapshot(file, snapshot);
        assertTrue(parsed.getQuests().get(0).isClaimed()); assertEquals(42, parsed.getQuests().get(0).getTimestamp());
    }
}
