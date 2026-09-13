package com.jsirgalaxybase.quest.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Builds the administration canvas dependency projection from authoritative published definitions.
 * The client supplies only a chapter identity; edges are never accepted from the client.
 */
public final class AuthenticatedQuestChapterDependencyQuery {
    public enum Status { SUCCESS, FORBIDDEN, NOT_FOUND }

    public static final class Result {
        private final Status status;
        private final QuestChapterDependencyGraph graph;

        private Result(Status status, QuestChapterDependencyGraph graph) {
            this.status = status;
            this.graph = graph;
        }

        public Status getStatus() { return status; }
        public QuestChapterDependencyGraph getGraph() { return graph; }
    }

    private final QuestChapterRepository chapters;
    private final QuestDefinitionRepository definitions;
    private final QuestEditorAuthorization authorization;

    public AuthenticatedQuestChapterDependencyQuery(QuestChapterRepository chapters,
        QuestDefinitionRepository definitions, QuestEditorAuthorization authorization) {
        if (chapters == null || definitions == null || authorization == null) {
            throw new IllegalArgumentException("dependencies are required");
        }
        this.chapters = chapters;
        this.definitions = definitions;
        this.authorization = authorization;
    }

    public Result load(QuestEditorActor actor, UUID chapterId, int version) {
        if (actor == null || !authorization.canManageDefinitions(actor)) {
            return new Result(Status.FORBIDDEN, null);
        }
        if (chapterId == null || version < 1) {
            throw new IllegalArgumentException("valid chapter identity is required");
        }
        Optional<StoredQuestChapter> stored = chapters.find(chapterId, version);
        if (!stored.isPresent()) return new Result(Status.NOT_FOUND, null);

        Map<UUID, QuestDefinition> loaded = new LinkedHashMap<UUID, QuestDefinition>();
        for (QuestChapterEntry entry : stored.get().getDefinition().getEntries()) {
            loadPublished(loaded, entry.getQuestId());
        }
        // Load direct prerequisites as well so an existing off-canvas quest is EXTERNAL, not MISSING.
        for (QuestDefinition definition : new java.util.ArrayList<QuestDefinition>(loaded.values())) {
            for (UUID prerequisite : definition.getPrerequisites()) loadPublished(loaded, prerequisite);
        }
        return new Result(Status.SUCCESS, QuestChapterDependencyGraph.build(stored.get().getDefinition(), loaded));
    }

    private void loadPublished(Map<UUID, QuestDefinition> loaded, UUID id) {
        if (loaded.containsKey(id)) return;
        Optional<StoredQuestDefinition> stored = definitions.findPublished(id);
        if (stored.isPresent()) loaded.put(id, stored.get().getDefinition());
    }
}
