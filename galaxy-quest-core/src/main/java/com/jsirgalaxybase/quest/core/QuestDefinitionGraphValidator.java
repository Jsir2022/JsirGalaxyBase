package com.jsirgalaxybase.quest.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Validates the effective prerequisite graph before a draft becomes the latest published version. */
public final class QuestDefinitionGraphValidator {
    private static final int MAX_VISITED = 4096;

    public List<QuestEditorValidationIssue> validateForPublish(QuestDefinition candidate,
        QuestDefinitionRepository definitions) {
        if (candidate == null || definitions == null) throw new IllegalArgumentException("dependencies are required");
        List<QuestEditorValidationIssue> issues = new ArrayList<QuestEditorValidationIssue>();
        for (UUID prerequisite : candidate.getPrerequisites()) {
            Optional<StoredQuestDefinition> root = definitions.findPublished(prerequisite);
            if (!root.isPresent()) {
                issue(issues, prerequisite, "missing_prerequisite", "Prerequisite has no published definition");
                continue;
            }
            SearchResult result = reaches(root.get().getDefinition(), candidate.getId(), definitions);
            if (result == SearchResult.CYCLE) {
                issue(issues, prerequisite, "dependency_cycle", "Publishing would create a prerequisite cycle");
            } else if (result == SearchResult.INCOMPLETE) {
                issue(issues, prerequisite, "incomplete_graph", "Prerequisite graph references an unpublished definition");
            } else if (result == SearchResult.TOO_LARGE) {
                issue(issues, prerequisite, "graph_limit", "Prerequisite graph exceeds the validation limit");
            }
        }
        return issues;
    }

    private static SearchResult reaches(QuestDefinition root, UUID target, QuestDefinitionRepository definitions) {
        ArrayDeque<UUID> pending = new ArrayDeque<UUID>();
        pending.add(root.getId());
        Set<UUID> visited = new HashSet<UUID>();
        boolean incomplete = false;
        while (!pending.isEmpty()) {
            UUID id = pending.removeFirst();
            if (target.equals(id)) return SearchResult.CYCLE;
            if (!visited.add(id)) continue;
            if (visited.size() > MAX_VISITED) return SearchResult.TOO_LARGE;
            Optional<StoredQuestDefinition> stored = definitions.findPublished(id);
            if (!stored.isPresent()) { incomplete = true; continue; }
            for (UUID prerequisite : stored.get().getDefinition().getPrerequisites()) pending.addLast(prerequisite);
        }
        return incomplete ? SearchResult.INCOMPLETE : SearchResult.OK;
    }

    private static void issue(List<QuestEditorValidationIssue> issues, UUID prerequisite, String code, String message) {
        issues.add(new QuestEditorValidationIssue("prerequisites." + prerequisite, code, message));
    }

    private enum SearchResult { OK, CYCLE, INCOMPLETE, TOO_LARGE }
}
