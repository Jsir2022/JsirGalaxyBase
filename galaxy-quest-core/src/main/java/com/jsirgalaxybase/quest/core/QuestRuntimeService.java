package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class QuestRuntimeService {
    private final QuestRuntimeRepository repository;
    private final QuestTransaction transaction;
    private final QuestFactProjector projector;
    private final QuestEngine engine;

    public QuestRuntimeService(QuestRuntimeRepository repository, QuestTransaction transaction,
        QuestFactProjector projector, QuestEngine engine) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.projector = Objects.requireNonNull(projector, "projector");
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    public FactProcessingResult apply(final GameplayFact fact, final List<QuestEvaluationRequest> requests,
        final long now) {
        Objects.requireNonNull(fact, "fact");
        if (requests == null || requests.isEmpty()) throw new IllegalArgumentException("requests must not be empty");
        return transaction.inTransaction(new QuestTransaction.Work<FactProcessingResult>() {
            @Override
            public FactProcessingResult execute() {
                return QuestRuntimeService.this.execute(fact, requests, now);
            }
        });
    }

    public FactProcessingResult apply(final GameplayFact fact, final QuestEvaluationRequestProvider provider,
        final long now) {
        Objects.requireNonNull(fact, "fact");
        Objects.requireNonNull(provider, "provider");
        return transaction.inTransaction(() -> {
            List<QuestEvaluationRequest> requests = provider.requestsFor(fact);
            if (requests == null || requests.isEmpty()) return FactProcessingResult.ignored();
            return execute(fact, requests, now);
        });
    }

    private FactProcessingResult execute(GameplayFact fact, List<QuestEvaluationRequest> requests, long now) {
        if (!repository.recordFactIfAbsent(fact)) return FactProcessingResult.duplicate();
        List<QuestEvaluation> evaluations = new ArrayList<QuestEvaluation>();
        Set<RewardEntitlement> entitlements = new LinkedHashSet<RewardEntitlement>();
        for (QuestEvaluationRequest request : requests) {
            ParticipantId participant = request.getMembership().getParticipantId();
            QuestDefinition definition = request.getDefinition();
            QuestProgressSnapshot previous = repository
                .findProgress(participant, definition.getId(), definition.getVersion()).orElse(null);
            Map<String, TaskProgress> observations = projector
                .project(definition, previousOrEmpty(participant, definition, previous), request.getMembership(), fact);
            QuestEvaluation evaluation = engine.evaluate(participant, definition, previous, observations,
                request.getCompletedPrerequisites(), now);
            repository.saveProgress(evaluation.getProgress());
            entitlements.addAll(evaluation.getEntitlements());
            evaluations.add(evaluation);
        }
        repository.insertEntitlementsIfAbsent(entitlements);
        return FactProcessingResult.accepted(evaluations);
    }

    private static QuestProgressSnapshot previousOrEmpty(ParticipantId participant, QuestDefinition definition,
        QuestProgressSnapshot previous) {
        if (previous != null) return previous;
        return new QuestProgressSnapshot(participant, definition.getId(), definition.getVersion(),
            QuestStatus.AVAILABLE, java.util.Collections.<String, TaskProgress>emptyMap(), 0L, 0);
    }
}
