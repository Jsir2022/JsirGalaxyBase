package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One immutable composition of runtime evaluators and editor schemas.
 *
 * <p>BetterQuesting accepts custom task factories through a registry.  Base
 * mirrors that useful property with explicit, testable registration while
 * retaining ownership of the lifecycle and without loading BQ classes.</p>
 */
public final class QuestRuntimeTypeRegistry {
    private final TaskEvaluatorRegistry evaluators;
    private final QuestEditorTypeRegistry editorTypes;
    private final List<String> extensionIds;

    private QuestRuntimeTypeRegistry(TaskEvaluatorRegistry evaluators, QuestEditorTypeRegistry editorTypes,
        List<String> extensionIds) {
        this.evaluators = evaluators;
        this.editorTypes = editorTypes;
        this.extensionIds = Collections.unmodifiableList(new ArrayList<String>(extensionIds));
    }

    public static QuestRuntimeTypeRegistry standard() {
        return withExtensions(Collections.<QuestRuntimeTypeExtension>emptyList());
    }

    public static QuestRuntimeTypeRegistry withExtensions(Collection<QuestRuntimeTypeExtension> extensions) {
        TaskEvaluatorRegistry evaluators = BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch();
        List<QuestElementTypeDescriptor> descriptors = new ArrayList<QuestElementTypeDescriptor>();
        QuestEditorTypeRegistry standardEditors = StandardQuestEditorTypeRegistry.create();
        descriptors.addAll(standardEditors.list(QuestElementKind.TASK));
        descriptors.addAll(standardEditors.list(QuestElementKind.REWARD));
        Set<String> ids = new LinkedHashSet<String>();
        if (extensions != null) for (QuestRuntimeTypeExtension extension : extensions) {
            if (extension == null) throw new IllegalArgumentException("quest type extension must not be null");
            String id = extension.getId();
            if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException(
                "quest type extension id must not be blank");
            if (!ids.add(id.trim())) throw new IllegalArgumentException("duplicate quest type extension: " + id);
            Collection<TaskEvaluator> contributedEvaluators = extension.taskEvaluators();
            if (contributedEvaluators != null) for (TaskEvaluator evaluator : contributedEvaluators) {
                evaluators.register(evaluator);
            }
            Collection<QuestElementTypeDescriptor> contributedEditors = extension.editorTypes();
            if (contributedEditors != null) for (QuestElementTypeDescriptor descriptor : contributedEditors) {
                if (descriptor == null) throw new IllegalArgumentException("extension editor type must not be null");
                descriptors.add(descriptor);
            }
        }
        return new QuestRuntimeTypeRegistry(evaluators, new QuestEditorTypeRegistry(descriptors),
            new ArrayList<String>(ids));
    }

    public TaskEvaluatorRegistry getEvaluators() { return evaluators; }
    public QuestEditorTypeRegistry getEditorTypes() { return editorTypes; }
    public List<String> getExtensionIds() { return extensionIds; }
}
