package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.junit.Test;

public class BqRuntimeCapabilityRegistryTest {
    @Test public void publishesOnlyTheNineTaskAndFourRewardTypesUsedByThePinnedBook() {
        assertEquals(new LinkedHashSet<String>(Arrays.asList(
            "bq_standard:block_break", "bq_standard:checkbox", "bq_standard:crafting", "bq_standard:fluid",
            "bq_standard:hunt", "bq_standard:location", "bq_standard:meeting",
            "bq_standard:optional_retrieval", "bq_standard:retrieval")),
            BqRuntimeCapabilityRegistry.taskEvidence().keySet());
        assertEquals(new LinkedHashSet<String>(Arrays.asList(
            "bq_standard:choice", "bq_standard:item", "bq_standard:questcompletion", "bq_standard:xp")),
            BqRuntimeCapabilityRegistry.rewardEvidence().keySet());
        for (String evidence : BqRuntimeCapabilityRegistry.taskEvidence().values()) assertFalse(evidence.isEmpty());
        for (String evidence : BqRuntimeCapabilityRegistry.rewardEvidence().values()) assertFalse(evidence.isEmpty());
    }

    @Test public void decodableFutureTypesRemainNonPublishable() {
        assertEquals(BqCompatibilityStatus.ADAPTED,
            BqRuntimeCapabilityRegistry.taskStatus("bq_standard:interact_entity"));
        assertEquals(BqCompatibilityStatus.ADAPTED,
            BqRuntimeCapabilityRegistry.taskStatus("bq_standard:scoreboard"));
        assertEquals(BqCompatibilityStatus.ADAPTED,
            BqRuntimeCapabilityRegistry.rewardStatus("bq_standard:command"));
        assertEquals(BqCompatibilityStatus.UNKNOWN,
            BqRuntimeCapabilityRegistry.taskStatus("addon:unknown"));
    }
}
