package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.QuestEditorTypeRegistry;
import com.jsirgalaxybase.quest.core.QuestElementKind;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;

public class BqStandardEditorTypeRegistryTest {
    @Test public void catalogsEverySupportedStandardTaskAndReward() {
        QuestEditorTypeRegistry registry = BqStandardEditorTypeRegistry.create();
        assertEquals(BqStandardCodec.supportedTaskTypes(), types(registry, QuestElementKind.TASK));
        assertEquals(BqStandardCodec.supportedRewardTypes(), types(registry, QuestElementKind.REWARD));
    }

    @Test public void validatesImportedStructuredSemanticsInsteadOfRawJson() {
        QuestEditorTypeRegistry registry = BqStandardEditorTypeRegistry.create();
        Map<String, String> valid = new LinkedHashMap<String, String>();
        valid.put("item.count", "1");
        valid.put("item.0.registryName", "minecraft:iron_ingot");
        valid.put("item.0.amount", "4");
        assertTrue(registry.validate(new TaskDefinition("retrieve", "bq_standard:retrieval", false, valid)).isEmpty());
        valid.put("item.0.amount", "0");
        assertFalse(registry.validate(new TaskDefinition("retrieve", "bq_standard:retrieval", false, valid)).isEmpty());
        assertFalse(registry.validate(new RewardDefinition("complete", "bq_standard:questcompletion",
            Collections.singletonMap("questUuid", "legacy-numeric-id"))).isEmpty());
    }

    private static java.util.Set<String> types(QuestEditorTypeRegistry registry, QuestElementKind kind) {
        java.util.Set<String> result = new java.util.LinkedHashSet<String>();
        for (com.jsirgalaxybase.quest.core.QuestElementTypeDescriptor descriptor : registry.list(kind)) {
            result.add(descriptor.getTypeId());
        }
        return result;
    }
}
