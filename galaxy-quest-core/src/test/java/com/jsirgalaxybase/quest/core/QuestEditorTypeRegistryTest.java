package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class QuestEditorTypeRegistryTest {
    @Test public void validatesScalarEnumUuidAndStructuredListFields() {
        QuestElementTypeDescriptor descriptor = new QuestElementTypeDescriptor("test:task", QuestElementKind.TASK,
            "Task", Arrays.asList(
                QuestEditorFieldDescriptor.builder("amount", "Amount", QuestEditorFieldType.LONG).required().minimum(1L).build(),
                QuestEditorFieldDescriptor.builder("mode", "Mode", QuestEditorFieldType.ENUM).required().options("one", "two").build(),
                QuestEditorFieldDescriptor.builder("quest", "Quest", QuestEditorFieldType.QUEST_REFERENCE).required().build(),
                QuestEditorFieldDescriptor.builder("item", "Items", QuestEditorFieldType.ITEM_LIST).required().build()), null);
        QuestEditorTypeRegistry registry = new QuestEditorTypeRegistry(Collections.singleton(descriptor));
        Map<String, String> invalid = new LinkedHashMap<String, String>();
        invalid.put("amount", "0");
        invalid.put("mode", "three");
        invalid.put("quest", "not-a-uuid");
        invalid.put("item.count", "1");
        invalid.put("item.0.amount", "bad");

        assertEquals(5, registry.validate(new TaskDefinition("task", "test:task", false, invalid)).size());
    }

    @Test public void rejectsDuplicateAndWrongKindLookups() {
        QuestElementTypeDescriptor descriptor = new QuestElementTypeDescriptor("test:item", QuestElementKind.REWARD,
            "Reward", Collections.<QuestEditorFieldDescriptor>emptyList(), null);
        QuestEditorTypeRegistry registry = new QuestEditorTypeRegistry(Collections.singleton(descriptor));
        assertEquals(1, registry.list(QuestElementKind.REWARD).size());
        try {
            registry.require("test:item", QuestElementKind.TASK);
            fail("expected wrong kind rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("unknown task type"));
        }
    }

    @Test public void locationAcceptsDimensionWideNegativeRangeButMeetingRequiresPositiveRange() {
        QuestEditorTypeRegistry registry = StandardQuestEditorTypeRegistry.create();
        Map<String, String> location = new LinkedHashMap<String, String>();
        location.put("dimension", "227");
        location.put("range", "-1");
        assertTrue(registry.validate(new TaskDefinition("location", "bq_standard:location", false, location)).isEmpty());

        Map<String, String> meeting = new LinkedHashMap<String, String>();
        meeting.put("subject", "minecraft:villager");
        meeting.put("amount", "1");
        meeting.put("range", "-1");
        assertFalse(registry.validate(new TaskDefinition("meeting", "bq_standard:meeting", false, meeting)).isEmpty());
    }

    @Test public void emptyItemRewardIsNoOpButChoiceStillRequiresOptions() {
        QuestEditorTypeRegistry registry = StandardQuestEditorTypeRegistry.create();
        Map<String, String> empty = Collections.singletonMap("item.count", "0");
        assertTrue(registry.validate(new RewardDefinition("item", "bq_standard:item", empty)).isEmpty());
        assertFalse(registry.validate(new RewardDefinition("choice", "bq_standard:choice", empty)).isEmpty());
    }

    @Test public void zeroBlockRequirementIsSatisfiedButZeroItemRequirementIsInvalid() {
        QuestEditorTypeRegistry registry = StandardQuestEditorTypeRegistry.create();
        Map<String, String> block = new LinkedHashMap<String, String>();
        block.put("block.count", "1");
        block.put("block.0.registryName", "minecraft:air");
        block.put("block.0.amount", "0");
        assertTrue(registry.validate(new TaskDefinition("block", "bq_standard:block_break", false, block)).isEmpty());
        block.put("block.0.amount", "-1");
        assertFalse(registry.validate(new TaskDefinition("block", "bq_standard:block_break", false, block)).isEmpty());

        Map<String, String> item = new LinkedHashMap<String, String>();
        item.put("item.count", "1");
        item.put("item.0.registryName", "minecraft:iron_ingot");
        item.put("item.0.amount", "0");
        assertFalse(registry.validate(new TaskDefinition("item", "bq_standard:retrieval", false, item)).isEmpty());
    }
}
