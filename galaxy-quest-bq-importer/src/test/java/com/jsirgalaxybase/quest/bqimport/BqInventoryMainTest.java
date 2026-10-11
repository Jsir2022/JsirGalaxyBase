package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class BqInventoryMainTest {
    @Test public void behaviorInventoryOutputContainsOnlyAggregateManifestDiagnostics() throws Exception {
        Path root = Files.createTempDirectory("bq-inventory-redaction");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        String privateQuestName = "Private Quest Name";
        String privateQuestId = "00000000-0000-0000-0000-00000000002a";
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":42,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"" + privateQuestName + "\"}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},"
            + "\"rewards:9\":{}}";
        Files.write(quests.resolve("quest.json"), json.getBytes(StandardCharsets.UTF_8));

        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(output, true, "UTF-8"));
            BqInventoryMain.main(new String[] { root.toString() });
        } finally {
            System.setOut(original);
        }

        String text = new String(output.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(text.contains("quests=1"));
        assertTrue(text.contains("WARNING/UNPLACED_QUEST=1"));
        assertFalse(text.contains(privateQuestName));
        assertFalse(text.contains(privateQuestId));
        assertFalse(text.contains("quest|"));
        assertTrue(text.contains("TASK|bq_standard:checkbox|1|SUPPORTED"));
    }
}
