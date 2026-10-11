package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Coordinates the BQ-compatible Quest-level Detect pass for supported consuming inventory Tasks. */
public final class AuthenticatedQuestConsumptionDetectService {
    private final String sourceServer;
    private final ConsumptionDetectBatchRepository batches;
    private final ConsumptionRequestAuthorizer authorizer;
    private final ConsumptionSubmissionService submissions;
    private final QuestRuntimeService runtime;
    private final QuestEvaluationRequestProvider requests;
    private final QuestTransaction transaction;

    public AuthenticatedQuestConsumptionDetectService(String sourceServer,
        ConsumptionDetectBatchRepository batches, ConsumptionRequestAuthorizer authorizer,
        ConsumptionSubmissionService submissions, QuestRuntimeService runtime,
        QuestEvaluationRequestProvider requests, QuestTransaction transaction) {
        if (sourceServer == null || sourceServer.trim().isEmpty() || batches == null || authorizer == null
            || submissions == null || runtime == null || requests == null || transaction == null) {
            throw new IllegalArgumentException("Quest Detect dependencies are required");
        }
        this.sourceServer = sourceServer.trim();
        this.batches = batches;
        this.authorizer = authorizer;
        this.submissions = submissions;
        this.runtime = runtime;
        this.requests = requests;
        this.transaction = transaction;
    }

    /** The only identity accepted here is the player authenticated by the server action handler. */
    public ConsumptionDetectBatchResult detect(UUID authenticatedPlayerId, UUID questId, long now) {
        if (authenticatedPlayerId == null || questId == null) throw new IllegalArgumentException(
            "authenticated player and quest are required");
        Optional<ConsumptionDetectBatch> found = transaction.inTransaction(
            () -> batches.findActive(authenticatedPlayerId, questId));
        ConsumptionDetectBatch batch;
        if (found.isPresent()) {
            batch = found.get();
            if (!sourceServer.equals(batch.getSourceServer())) return new ConsumptionDetectBatchResult(
                batch.getBatchId(), batch.getSourceServer(),
                ConsumptionDetectBatchResult.Status.WAITING_FOR_SOURCE_SERVER, null,
                batch.getNextTaskIndex(), batch.getTaskKeys().size());
        } else {
            final int version = transaction.inTransaction(() -> Integer.valueOf(authorizer.publishedVersion(questId)))
                .intValue();
            QuestConsumptionDetectPlan plan = transaction.inTransaction(
                () -> authorizer.planQuestDetect(authenticatedPlayerId, questId, now));
            if (plan.getTaskKeys().isEmpty()) return new ConsumptionDetectBatchResult("",
                ConsumptionDetectBatchResult.Status.NO_CONSUMING_TASKS, null, 0, 0);
            if (plan.getEvaluationRequest().getDefinition().getVersion() != version) throw new IllegalStateException(
                "published quest changed while creating Detect batch");
            ConsumptionDetectBatch candidate = new ConsumptionDetectBatch(UUID.randomUUID().toString(), sourceServer,
                authenticatedPlayerId, plan.getEvaluationRequest().getMembership().getParticipantId(), questId,
                version, plan.getCycle(), plan.getTaskKeys(), 0, now);
            batch = transaction.inTransaction(() -> batches.createIfAbsent(candidate));
            if (!sourceServer.equals(batch.getSourceServer())) return new ConsumptionDetectBatchResult(
                batch.getBatchId(), batch.getSourceServer(),
                ConsumptionDetectBatchResult.Status.WAITING_FOR_SOURCE_SERVER, null,
                batch.getNextTaskIndex(), batch.getTaskKeys().size());
            if (!sameBatchSubject(batch, candidate)) throw new IllegalStateException(
                "an incompatible Quest Detect operation is already active for this player and Quest");
        }
        return continueBatch(batch, now);
    }

    private ConsumptionDetectBatchResult continueBatch(ConsumptionDetectBatch initial, long now) {
        ConsumptionDetectBatch batch = initial;
        final String activeBatchId = initial.getBatchId();
        List<ConsumptionSubmission> results = new ArrayList<ConsumptionSubmission>();
        while (!batch.isComplete()) {
            int index = batch.getNextTaskIndex();
            String taskKey = batch.getCurrentTaskKey();
            String submissionKey = batch.getBatchId() + ":" + index;
            ConsumptionIntent intent = new ConsumptionIntent(submissionKey, sourceServer, batch.getPlayerId(),
                batch.getQuestId(), taskKey, batch.getCreatedAt(), null);
            ConsumptionSubmission submitted = submissions.submit(intent, authorizer, now,
                batch.getParticipantId(), Integer.valueOf(batch.getCycle()),
                Integer.valueOf(batch.getDefinitionVersion()));
            results.add(submitted);
            if (submitted.getStatus() != ConsumptionSubmissionStatus.CONFIRMED
                && submitted.getStatus() != ConsumptionSubmissionStatus.REJECTED) {
                return new ConsumptionDetectBatchResult(batch.getBatchId(),
                    ConsumptionDetectBatchResult.Status.RESUMABLE, results, index, batch.getTaskKeys().size());
            }
            batch = transaction.inTransaction(() -> batches.advance(activeBatchId, index, now));
        }

        final ConsumptionDetectBatch completedBatch = batch;
        transaction.inTransaction(() -> {
            batches.deleteCompleted(completedBatch.getBatchId(), completedBatch.getTaskKeys().size(), now);
            QuestEvaluationRequest settlement = authorizer.evaluationRequest(completedBatch.getPlayerId(),
                completedBatch.getQuestId(), completedBatch.getDefinitionVersion(), completedBatch.getParticipantId());
            if (!settlement.getMembership().getParticipantId().equals(completedBatch.getParticipantId())
                || settlement.getDefinition().getVersion() != completedBatch.getDefinitionVersion()) {
                throw new IllegalStateException("Quest membership or definition changed before Detect settlement");
            }
            runtime.reconcileAfterConsumptionDetectBatch(settlement, requests, now, completedBatch.getCycle(),
                completedBatch.getTaskKeys());
            return null;
        });
        return new ConsumptionDetectBatchResult(completedBatch.getBatchId(),
            ConsumptionDetectBatchResult.Status.COMPLETED, results, completedBatch.getTaskKeys().size(),
            completedBatch.getTaskKeys().size());
    }

    private static boolean sameBatchSubject(ConsumptionDetectBatch left, ConsumptionDetectBatch right) {
        return left.getSourceServer().equals(right.getSourceServer()) && left.getPlayerId().equals(right.getPlayerId())
            && left.getParticipantId().equals(right.getParticipantId()) && left.getQuestId().equals(right.getQuestId());
    }
}
