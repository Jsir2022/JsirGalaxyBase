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
}
