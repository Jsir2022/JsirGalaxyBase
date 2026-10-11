package com.jsirgalaxybase.quest.core;

import java.util.Objects;

/** Crash-recoverable saga around a platform-idempotent inventory deduction and transactional quest progress. */
public final class ConsumptionSubmissionService {
    private final ConsumptionSubmissionRepository submissions;
    private final ConsumptionPort port;
    private final QuestRuntimeService runtime;
    private final QuestEvaluationRequestProvider requests;
    private final QuestTransaction transaction;

    public ConsumptionSubmissionService(ConsumptionSubmissionRepository submissions, ConsumptionPort port,
        QuestRuntimeService runtime, QuestEvaluationRequestProvider requests, QuestTransaction transaction) {
        this.submissions = Objects.requireNonNull(submissions, "submissions");
        this.port = Objects.requireNonNull(port, "port");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.requests = Objects.requireNonNull(requests, "requests");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
    }

    public ConsumptionSubmission submit(final ConsumptionIntent intent, final ConsumptionRequestAuthorizer authorizer,
        final long now) {
        return submit(intent, authorizer, now, null, null);
    }

    /** Submits one batch Task while pinning its participant/cycle before touching player inventory. */
    public ConsumptionSubmission submit(final ConsumptionIntent intent, final ConsumptionRequestAuthorizer authorizer,
        final long now, final ParticipantId expectedParticipant, final Integer expectedCycle) {
        return submit(intent, authorizer, now, expectedParticipant, expectedCycle, null);
    }

    /** Submits a Task against the definition version pinned by its durable Detect batch. */
    public ConsumptionSubmission submit(final ConsumptionIntent intent, final ConsumptionRequestAuthorizer authorizer,
        final long now, final ParticipantId expectedParticipant, final Integer expectedCycle,
        final Integer expectedDefinitionVersion) {
        if (intent == null || authorizer == null) throw new IllegalArgumentException("intent and authorizer are required");
        Authorized prepared = transaction.inTransaction(() -> {
            java.util.Optional<ConsumptionSubmission> existing = submissions.find(intent.getSubmissionKey());
            if (existing.isPresent()) {
                ConsumptionRequest request = existing.get().getRequest();
                if (!request.getSourceServer().equals(intent.getSourceServer())
                    || !request.getPlayerId().equals(intent.getPlayerId())
                    || !request.getQuestId().equals(intent.getQuestId())
                    || !request.getTaskKey().equals(intent.getTaskKey())) {
                    throw new IllegalArgumentException("consumption submission key belongs to a different request");
                }
                requireExpectedContext(request, expectedParticipant, expectedCycle, expectedDefinitionVersion);
                return new Authorized(request, existing.get());
            }
            ConsumptionRequest request = authorizer.authorize(intent, now, expectedParticipant, expectedCycle,
                expectedDefinitionVersion);
            return new Authorized(request, submissions.prepareIfAbsent(request));
        });
        return process(prepared.request, prepared.submission, now);
    }

    private void requireExpectedContext(ConsumptionRequest request, ParticipantId expectedParticipant,
        Integer expectedCycle, Integer expectedDefinitionVersion) {
        if (expectedParticipant != null && !expectedParticipant.equals(request.getParticipantId())) throw new IllegalArgumentException(
            "persisted consumption operation belongs to a different participant than its Detect batch");
        if (expectedCycle != null && expectedCycle.intValue() != request.getQuestCycle()) throw new IllegalArgumentException(
            "persisted consumption operation belongs to a different cycle than its Detect batch");
        if (expectedDefinitionVersion != null
            && expectedDefinitionVersion.intValue() != request.getDefinitionVersion()) throw new IllegalArgumentException(
                "persisted consumption operation belongs to a different definition than its Detect batch");
    }

    public ConsumptionSubmission resume(final String submissionKey, final long now) {
        ConsumptionSubmission submission = transaction.inTransaction(() -> submissions.find(submissionKey)
            .orElseThrow(() -> new IllegalArgumentException("unknown consumption submission")));
        return process(submission.getRequest(), submission, now);
    }

    /** Resumes at most a bounded batch after the submitting player is online on the originating server. */
    public java.util.List<ConsumptionSubmission> resumePendingForPlayer(final java.util.UUID playerId,
        final String sourceServer, final long now) {
        if (playerId == null) throw new IllegalArgumentException("playerId is required");
        if (sourceServer == null || sourceServer.trim().isEmpty()) throw new IllegalArgumentException(
            "sourceServer is required");
        java.util.List<ConsumptionSubmission> pending = transaction.inTransaction(
            () -> submissions.findPendingForPlayer(playerId, sourceServer.trim(), 64));
        java.util.List<ConsumptionSubmission> resumed = new java.util.ArrayList<ConsumptionSubmission>(pending.size());
        for (ConsumptionSubmission submission : pending) {
            ConsumptionRequest request = submission.getRequest();
            if (!playerId.equals(request.getPlayerId()) || !sourceServer.trim().equals(request.getSourceServer())) {
                throw new IllegalStateException("pending consumption query crossed its player or source-server boundary");
            }
            resumed.add(process(submission.getRequest(), submission, now));
        }
        return java.util.Collections.unmodifiableList(resumed);
    }

    private ConsumptionSubmission process(final ConsumptionRequest request, ConsumptionSubmission submission,
        final long now) {
        if (submission.getStatus() == ConsumptionSubmissionStatus.CONFIRMED
            || submission.getStatus() == ConsumptionSubmissionStatus.REJECTED) return submission;
        if (submission.getStatus() == ConsumptionSubmissionStatus.PREPARED) {
            ConsumptionApplyResult applied = port.apply(request);
            if (!applied.isApplied()) return transaction.inTransaction(
                () -> submissions.markRejected(request.getSubmissionKey(), applied.getEvidence(), now));
            final String evidence = applied.getEvidence();
            final java.util.List<Long> appliedAmounts = applied.getAppliedAmounts().isEmpty()
                ? request.getAmounts() : applied.getAppliedAmounts();
            submission = transaction.inTransaction(
                () -> submissions.markApplied(request.getSubmissionKey(), evidence, appliedAmounts, now));
        }
        if (submission.getStatus() != ConsumptionSubmissionStatus.APPLIED) return submission;
        final java.util.List<Long> confirmedAmounts = submission.getAppliedAmounts();
        return transaction.inTransaction(() -> {
            FactProcessingResult result = runtime.apply(request.confirmedFact(confirmedAmounts), requests, now);
            if (result.getStatus() == FactProcessingStatus.IGNORED) {
                throw new IllegalStateException("applied consumption has no matching published task");
            }
            return submissions.markConfirmed(request.getSubmissionKey(), now);
        });
    }

    private static final class Authorized {
        private final ConsumptionRequest request;
        private final ConsumptionSubmission submission;
        private Authorized(ConsumptionRequest request, ConsumptionSubmission submission) {
            this.request = request;
            this.submission = submission;
        }
    }
}
