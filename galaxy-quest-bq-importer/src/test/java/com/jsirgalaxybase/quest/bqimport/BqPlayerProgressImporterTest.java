package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.Test;

public class BqPlayerProgressImporterTest {
    @Test
    public void importsCompletionClaimAndPreservesOpaqueTaskProgress() throws Exception {
        UUID player = UUID.randomUUID();
        Path source = Files.createTempDirectory("bq-progress").resolve(player + ".json");
        String json = "{\"questProgress:9\":{\"0:10\":{\"questIDHigh:4\":0,\"questIDLow:4\":9,"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\","
            + "\"completeUsers:9\":{\"0:8\":\"" + player + "\"},"
            + "\"userProgress:9\":{\"0:10\":{\"uuid:8\":\"" + player + "\",\"data:11\":[0,1]}}},"
            + "\"1:10\":{\"index:3\":1,\"taskID:8\":\"bq_standard:hunt\","
            + "\"userProgress:9\":{\"0:10\":{\"uuid:8\":\"" + player + "\",\"value:3\":10}}}},"
            + "\"completed:9\":{\"0:10\":{\"uuid:8\":\"" + player
            + "\",\"claimed:1\":1,\"timestamp:4\":42}}}}}";
        Files.write(source, json.getBytes(StandardCharsets.UTF_8));
        BqImportedPlayerProgress imported = new BqPlayerProgressImporter().read(source);
        assertEquals(player, imported.getPlayerId());
        assertTrue(imported.getQuests().get(0).isCompleted());
        assertTrue(imported.getQuests().get(0).isClaimed());
        assertEquals(42L, imported.getQuests().get(0).getTimestamp());
        assertTrue(imported.getQuests().get(0).getRawTasksJson().contains("data:11"));
        assertEquals(2, imported.getQuests().get(0).getTasks().size());
        BqImportedTaskProgress retrieval = imported.getQuests().get(0).getTasks().get(0);
        assertEquals("task-0", retrieval.getKey());
        assertEquals("bq_standard:retrieval", retrieval.getTypeId());
        assertTrue(retrieval.isComplete());
        assertEquals(java.util.Arrays.asList(0L, 1L), retrieval.getValues());
        BqImportedTaskProgress hunt = imported.getQuests().get(0).getTasks().get(1);
        assertFalse(hunt.isComplete());
        assertEquals(java.util.Arrays.asList(10L), hunt.getValues());
    }
}
