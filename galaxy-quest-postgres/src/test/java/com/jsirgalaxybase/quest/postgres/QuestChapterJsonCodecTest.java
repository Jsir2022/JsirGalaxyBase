package com.jsirgalaxybase.quest.postgres;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.QuestChapterDefinition;
import com.jsirgalaxybase.quest.core.QuestChapterEntry;

public class QuestChapterJsonCodecTest {
    @Test
    public void roundTripPreservesOrderedPlacementsAndProducesStableHash() {
        QuestChapterDefinition source = new QuestChapterDefinition(UUID.randomUUID(), 3, "Industry", "Progression",
            "minecraft:book@0", "jsirgalaxybase:textures/quest/industry.png", Arrays.asList(
                new QuestChapterEntry(UUID.randomUUID(), -24, 8, 24, 24),
                new QuestChapterEntry(UUID.randomUUID(), 40, 16, 32, 20)));
        QuestChapterJsonCodec codec = new QuestChapterJsonCodec();
        QuestChapterJsonCodec.Encoded first = codec.encode(source);
        QuestChapterDefinition restored = codec.decode(first.json);

        assertEquals(first.hash, codec.encode(source).hash);
        assertEquals(source.getId(), restored.getId());
        assertEquals(source.getName(), restored.getName());
        assertEquals(source.getBackgroundReference(), restored.getBackgroundReference());
        assertEquals(source.getEntries().get(1).getQuestId(), restored.getEntries().get(1).getQuestId());
        assertEquals(32, restored.getEntries().get(1).getWidth());
    }
}
