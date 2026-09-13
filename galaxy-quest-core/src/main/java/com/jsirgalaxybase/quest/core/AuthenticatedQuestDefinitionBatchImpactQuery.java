package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.UUID;

/** Authorization boundary for a server-authoritative batch retirement preview. */
public final class AuthenticatedQuestDefinitionBatchImpactQuery {
    public enum Status { SUCCESS, FORBIDDEN }
    public static final class Result {
        private final Status status;
        private final QuestDefinitionBatchImpactReport report;
        private Result(Status status, QuestDefinitionBatchImpactReport report) {
            this.status = status; this.report = report;
        }
        public Status getStatus() { return status; }
        public QuestDefinitionBatchImpactReport getReport() { return report; }
    }

    private final QuestDefinitionRepository definitions;
    private final QuestChapterRepository chapters;
    private final QuestEditorAuthorization authorization;
    private final QuestDefinitionImpactAnalyzer analyzer = new QuestDefinitionImpactAnalyzer();

    public AuthenticatedQuestDefinitionBatchImpactQuery(QuestDefinitionRepository definitions,
        QuestChapterRepository chapters, QuestEditorAuthorization authorization) {
        if (definitions == null || chapters == null || authorization == null)
            throw new IllegalArgumentException("dependencies are required");
        this.definitions = definitions; this.chapters = chapters; this.authorization = authorization;
    }

    public Result load(QuestEditorActor actor, List<UUID> targetIds) {
        if (actor == null || !authorization.canManageDefinitions(actor)) return new Result(Status.FORBIDDEN, null);
        return new Result(Status.SUCCESS,
            analyzer.analyzeBatch(targetIds, definitions.findAllPublished(), chapters.findAllPublished()));
    }
}
