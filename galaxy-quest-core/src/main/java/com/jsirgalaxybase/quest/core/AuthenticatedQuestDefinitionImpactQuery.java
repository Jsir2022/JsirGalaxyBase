package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** Authorization boundary for server-authoritative quest change previews. */
public final class AuthenticatedQuestDefinitionImpactQuery {
    public enum Status { SUCCESS, FORBIDDEN }
    public static final class Result {
        private final Status status;
        private final QuestDefinitionImpactReport report;
        private Result(Status status, QuestDefinitionImpactReport report) { this.status = status; this.report = report; }
        public Status getStatus() { return status; }
        public QuestDefinitionImpactReport getReport() { return report; }
    }

    private final QuestDefinitionRepository definitions;
    private final QuestChapterRepository chapters;
    private final QuestEditorAuthorization authorization;
    private final QuestDefinitionImpactAnalyzer analyzer;

    public AuthenticatedQuestDefinitionImpactQuery(QuestDefinitionRepository definitions,
        QuestChapterRepository chapters, QuestEditorAuthorization authorization) {
        if (definitions == null || chapters == null || authorization == null) {
            throw new IllegalArgumentException("dependencies are required");
        }
        this.definitions = definitions;
        this.chapters = chapters;
        this.authorization = authorization;
        this.analyzer = new QuestDefinitionImpactAnalyzer();
    }

    public Result load(QuestEditorActor actor, UUID targetId) {
        if (actor == null || !authorization.canManageDefinitions(actor)) return new Result(Status.FORBIDDEN, null);
        if (targetId == null) throw new IllegalArgumentException("targetId is required");
        return new Result(Status.SUCCESS,
            analyzer.analyze(targetId, definitions.findAllPublished(), chapters.findAllPublished()));
    }
}
