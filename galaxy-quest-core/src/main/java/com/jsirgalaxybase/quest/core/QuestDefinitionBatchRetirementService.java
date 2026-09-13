package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Performs impact validation and all lifecycle writes inside one server transaction. */
public final class QuestDefinitionBatchRetirementService {
    private static final class Conflict extends RuntimeException { private static final long serialVersionUID = 1L; }
    private final QuestDefinitionRepository definitions;
    private final QuestChapterRepository chapters;
    private final QuestTransaction transaction;
    private final QuestEditorAuthorization authorization;
    private final QuestDefinitionImpactAnalyzer analyzer = new QuestDefinitionImpactAnalyzer();

    public QuestDefinitionBatchRetirementService(QuestDefinitionRepository definitions,
        QuestChapterRepository chapters, QuestTransaction transaction, QuestEditorAuthorization authorization) {
        if (definitions == null || chapters == null || transaction == null || authorization == null)
            throw new IllegalArgumentException("dependencies are required");
        this.definitions = definitions; this.chapters = chapters; this.transaction = transaction;
        this.authorization = authorization;
    }

    public QuestDefinitionBatchRetirementResult retire(QuestEditorActor actor,
        final QuestDefinitionBatchRetirementRequest request) {
        if (actor == null || !authorization.canManageDefinitions(actor))
            return QuestDefinitionBatchRetirementResult.status(QuestDefinitionBatchRetirementResult.Status.FORBIDDEN);
        if (request == null)
            return QuestDefinitionBatchRetirementResult.status(QuestDefinitionBatchRetirementResult.Status.INVALID);
        try {
            return transaction.inTransaction(new QuestTransaction.Work<QuestDefinitionBatchRetirementResult>() {
                @Override public QuestDefinitionBatchRetirementResult execute() {
                    List<UUID> ids = new ArrayList<UUID>();
                    for (QuestDefinitionBatchRetirementRequest.Target target : request.getTargets())
                        ids.add(target.getQuestId());
                    QuestDefinitionBatchImpactReport impact = analyzer.analyzeBatch(ids,
                        definitions.findAllPublished(), chapters.findAllPublished());
                    if (!impact.getUnpublishedTargetIds().isEmpty())
                        return QuestDefinitionBatchRetirementResult.status(
                            QuestDefinitionBatchRetirementResult.Status.NOT_FOUND);
                    if (!impact.isSafeToRetire()) return QuestDefinitionBatchRetirementResult.unsafe(impact);
                    for (QuestDefinitionBatchRetirementRequest.Target target : request.getTargets()) {
                        Optional<StoredQuestDefinition> stored = definitions.find(target.getQuestId(), target.getVersion());
                        if (!stored.isPresent() || stored.get().getLifecycle() != QuestDefinitionLifecycle.PUBLISHED)
                            return QuestDefinitionBatchRetirementResult.status(
                                QuestDefinitionBatchRetirementResult.Status.NOT_FOUND);
                    }
                    for (QuestDefinitionBatchRetirementRequest.Target target : request.getTargets())
                        if (!definitions.retire(target.getQuestId(), target.getVersion())) throw new Conflict();
                    List<StoredQuestDefinition> retired = new ArrayList<StoredQuestDefinition>();
                    for (QuestDefinitionBatchRetirementRequest.Target target : request.getTargets()) {
                        Optional<StoredQuestDefinition> stored = definitions.find(target.getQuestId(), target.getVersion());
                        if (!stored.isPresent() || stored.get().getLifecycle() != QuestDefinitionLifecycle.RETIRED)
                            throw new Conflict();
                        retired.add(stored.get());
                    }
                    return QuestDefinitionBatchRetirementResult.success(retired);
                }
            });
        } catch (Conflict conflict) {
            return QuestDefinitionBatchRetirementResult.status(QuestDefinitionBatchRetirementResult.Status.CONFLICT);
        }
    }
}
