package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public class QuestRuntimeTypeRegistryTest {
    @Test
    public void composesStandardAndExplicitExtensionIntoOneRuntimeDirectory() {
        final String type = "galaxy:test_extension_task";
        QuestElementTypeDescriptor descriptor = new QuestElementTypeDescriptor(type, QuestElementKind.TASK,
            "Extension task", Collections.<QuestEditorFieldDescriptor>emptyList(), null);
        QuestRuntimeTypeRegistry registry = QuestRuntimeTypeRegistry.withExtensions(
            Collections.singletonList(new QuestRuntimeTypeExtension() {
                @Override public String getId() { return "test-extension"; }
                @Override public java.util.Collection<TaskEvaluator> taskEvaluators() {
                    return Collections.<TaskEvaluator>singletonList(new FactCountTaskEvaluator(type));
                }
                @Override public java.util.Collection<QuestElementTypeDescriptor> editorTypes() {
                    return Collections.singletonList(descriptor);
                }
            }));

        assertTrue(registry.getEvaluators().snapshot().containsKey("bq_standard:crafting"));
        assertTrue(registry.getEvaluators().snapshot().containsKey(type));
        assertEquals("test-extension", registry.getExtensionIds().get(0));
        assertEquals("Extension task", registry.getEditorTypes().require(type, QuestElementKind.TASK).getDisplayName());
    }

    @Test
    public void rejectsDuplicateExtensionAndDuplicateStandardType() {
        QuestRuntimeTypeExtension duplicateId = new QuestRuntimeTypeExtension() {
            @Override public String getId() { return "same"; }
            @Override public java.util.Collection<TaskEvaluator> taskEvaluators() { return Collections.emptyList(); }
            @Override public java.util.Collection<QuestElementTypeDescriptor> editorTypes() { return Collections.emptyList(); }
        };
        try {
            QuestRuntimeTypeRegistry.withExtensions(Arrays.asList(duplicateId, duplicateId));
            throw new AssertionError("expected duplicate extension rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("duplicate quest type extension"));
        }

        try {
            QuestRuntimeTypeRegistry.withExtensions(Collections.singletonList(new QuestRuntimeTypeExtension() {
                @Override public String getId() { return "duplicate-type"; }
                @Override public java.util.Collection<TaskEvaluator> taskEvaluators() {
                    return Collections.<TaskEvaluator>singletonList(new FactCountTaskEvaluator("bq_standard:crafting"));
                }
                @Override public java.util.Collection<QuestElementTypeDescriptor> editorTypes() {
                    return Collections.emptyList();
                }
            }));
            throw new AssertionError("expected duplicate evaluator rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("duplicate task evaluator"));
        }
    }
}
