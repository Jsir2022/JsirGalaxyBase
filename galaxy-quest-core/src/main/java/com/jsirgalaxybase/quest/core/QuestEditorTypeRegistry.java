package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuestEditorTypeRegistry {
    private final Map<String, QuestElementTypeDescriptor> types;

    public QuestEditorTypeRegistry(Collection<QuestElementTypeDescriptor> descriptors) {
        Map<String, QuestElementTypeDescriptor> result = new LinkedHashMap<String, QuestElementTypeDescriptor>();
        if (descriptors != null) for (QuestElementTypeDescriptor descriptor : descriptors) {
            String key = key(descriptor.getTypeId(), descriptor.getKind());
            if (result.put(key, descriptor) != null) {
                throw new IllegalArgumentException("duplicate " + descriptor.getKind().name().toLowerCase()
                    + " type: " + descriptor.getTypeId());
            }
        }
        this.types = Collections.unmodifiableMap(result);
    }

    public QuestElementTypeDescriptor require(String typeId, QuestElementKind kind) {
        QuestElementTypeDescriptor result = types.get(key(typeId, kind));
        if (result == null) throw new IllegalArgumentException(
            "unknown " + kind.name().toLowerCase() + " type: " + typeId);
        return result;
    }

    public List<QuestElementTypeDescriptor> list(QuestElementKind kind) {
        List<QuestElementTypeDescriptor> result = new ArrayList<QuestElementTypeDescriptor>();
        for (QuestElementTypeDescriptor descriptor : types.values()) if (descriptor.getKind() == kind) result.add(descriptor);
        return Collections.unmodifiableList(result);
    }

    public List<QuestEditorValidationIssue> validate(TaskDefinition task) {
        return require(task.getTypeId(), QuestElementKind.TASK).validate(task.getParameters());
    }

    public List<QuestEditorValidationIssue> validate(RewardDefinition reward) {
        return require(reward.getTypeId(), QuestElementKind.REWARD).validate(reward.getParameters());
    }

    private static String key(String typeId, QuestElementKind kind) { return kind.name() + "\u0000" + typeId; }
}
