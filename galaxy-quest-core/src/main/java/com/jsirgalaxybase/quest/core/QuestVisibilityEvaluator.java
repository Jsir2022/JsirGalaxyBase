package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Pure visibility evaluation derived from BetterQuesting's QuestCache, without client or singleton state. */
public final class QuestVisibilityEvaluator {
    public boolean isVisible(QuestDefinition quest, Map<UUID, QuestDefinition> definitions,
        Map<UUID, QuestStatus> statuses, boolean editorMode, boolean viewMode) {
        if (quest == null) return false;
        return visible(quest, safe(definitions), safe(statuses), editorMode, viewMode, new HashSet<UUID>());
    }

    private boolean visible(QuestDefinition quest, Map<UUID, QuestDefinition> definitions,
        Map<UUID, QuestStatus> statuses, boolean editorMode, boolean viewMode, Set<UUID> visiting) {
        QuestVisibility visibility = quest.getBehavior().getVisibility();
        if (editorMode || visibility == QuestVisibility.ALWAYS) return true;
        if (visibility == QuestVisibility.HIDDEN) return false;
        QuestStatus state = state(quest, statuses);
        boolean complete = state == QuestStatus.COMPLETED;
        boolean unlocked = state != QuestStatus.LOCKED;
        if (visibility == QuestVisibility.SECRET || visibility == QuestVisibility.UNLOCKED) {
            return complete || unlocked;
        }
        if (viewMode) return true;
        if (visibility == QuestVisibility.NORMAL) {
            if (complete || unlocked) return true;
            for (UUID prerequisite : quest.getPrerequisites()) {
                QuestDefinition parent = definitions.get(prerequisite);
                if (parent == null || state(parent, statuses) == QuestStatus.LOCKED) return false;
            }
            return true;
        }
        if (visibility == QuestVisibility.COMPLETED) return complete;
        if (visibility == QuestVisibility.CHAIN) {
            if (quest.getPrerequisites().isEmpty()) return true;
            if (!visiting.add(quest.getId())) return false;
            try {
                for (UUID prerequisite : quest.getPrerequisites()) {
                    QuestDefinition parent = definitions.get(prerequisite);
                    if (parent != null && visible(parent, definitions, statuses, false, false, visiting)) return true;
                }
                return false;
            } finally {
                visiting.remove(quest.getId());
            }
        }
        return true;
    }

    private static QuestStatus state(QuestDefinition quest, Map<UUID, QuestStatus> statuses) {
        QuestStatus found = statuses.get(quest.getId());
        return found == null ? (quest.getPrerequisites().isEmpty() ? QuestStatus.AVAILABLE : QuestStatus.LOCKED) : found;
    }

    private static <K, V> Map<K, V> safe(Map<K, V> value) {
        return value == null ? Collections.<K, V>emptyMap() : value;
    }
}
