package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class BqQuestChapterImporterTest {
    @Test
    public void importsSplitQuestLinesInDeclaredOrder() throws Exception {
        Path root = Files.createTempDirectory("bq-lines");
        UUID firstId = new UUID(0L, 5L);
        UUID secondId = new UUID(0L, 1L);
        Files.write(root.resolve("QuestLinesOrder.txt"), (
            "AAAAAAAAAAAAAAAAAAAABQ==: Tier 3 - HV\nAAAAAAAAAAAAAAAAAAAAAQ==: Stone Age\n")
                .getBytes(StandardCharsets.UTF_8));
        Path first = Files.createDirectories(root.resolve("QuestLines/Tier3HV-AAAAAAAAAAAAAAAAAAAABQ=="));
        Path second = Files.createDirectories(root.resolve("QuestLines/StoneAge-AAAAAAAAAAAAAAAAAAAAAQ=="));
        Files.write(first.resolve("QuestLine.json"), ("{\"questLineIDHigh:4\":0,\"questLineIDLow:4\":5,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"HV metadata\","
            + "\"desc:8\":\"Power\",\"bg_image:8\":\"mod:bg.png\","
            + "\"bg_size:3\":512,\"visibility:8\":\"UNLOCKED\","
            + "\"icon:10\":{\"id:8\":\"minecraft:book\"}}}}").getBytes(StandardCharsets.UTF_8));
        Files.write(first.resolve("q.json"), ("{\"questIDHigh:4\":0,\"questIDLow:4\":1906,"
            + "\"sizeX:3\":48,\"sizeY:3\":24,\"x:3\":60,\"y:3\":156}").getBytes(StandardCharsets.UTF_8));
        Files.write(second.resolve("q.json"), ("{\"questIDHigh:4\":0,\"questIDLow:4\":2,"
            + "\"sizeX:3\":24,\"sizeY:3\":24,\"x:3\":0,\"y:3\":0}").getBytes(StandardCharsets.UTF_8));

        List<BqImportedChapter> chapters = new BqQuestChapterImporter().readDirectory(root);

        assertEquals(2, chapters.size());
        assertEquals(firstId, chapters.get(0).getDefinition().getId());
        assertEquals("HV metadata", chapters.get(0).getDefinition().getName());
        assertEquals("Power", chapters.get(0).getDefinition().getDescription());
        assertEquals("mod:bg.png", chapters.get(0).getDefinition().getBackgroundReference());
        assertEquals(512, chapters.get(0).getDefinition().getBackgroundSize());
        assertEquals(com.jsirgalaxybase.quest.core.QuestVisibility.UNLOCKED,
            chapters.get(0).getDefinition().getVisibility());
        assertEquals("{\"id:8\":\"minecraft:book\"}", chapters.get(0).getDefinition().getIconReference());
        assertEquals(1906L, chapters.get(0).getDefinition().getEntries().get(0).getQuestId().getLeastSignificantBits());
        assertEquals(48, chapters.get(0).getDefinition().getEntries().get(0).getWidth());
        assertEquals(secondId, chapters.get(1).getDefinition().getId());
    }

    @Test
    public void rejectsMalformedDirectoryIdentityAndDimensions() throws Exception {
        assertThrows(java.io.IOException.class, () -> BqQuestChapterImporter.idFromFileName("missing"));
        Path root = Files.createTempDirectory("bq-lines-invalid");
        Files.write(root.resolve("QuestLinesOrder.txt"), "AAAAAAAAAAAAAAAAAAAAAQ==: Stone\n"
            .getBytes(StandardCharsets.UTF_8));
        Path line = Files.createDirectories(root.resolve("QuestLines/Stone-AAAAAAAAAAAAAAAAAAAAAQ=="));
        Files.write(line.resolve("q.json"), ("{\"questIDHigh:4\":0,\"questIDLow:4\":2,"
            + "\"sizeX:3\":0,\"sizeY:3\":24}").getBytes(StandardCharsets.UTF_8));
        assertThrows(java.io.IOException.class, () -> new BqQuestChapterImporter().readDirectory(root));
    }

    @Test
    public void directoryParserDoesNotConfuseUrlSafeDashInsideUuidForSeparator() throws Exception {
        UUID expected = BqQuestChapterImporter.decodeUuid("D_aFb1hFQH2PDkN0_KvAEQ==");
        assertEquals(expected, BqQuestChapterImporter.idFromFileName(
            "Tier-with-dashes-D_aFb1hFQH2PDkN0_KvAEQ=="));
    }
}
