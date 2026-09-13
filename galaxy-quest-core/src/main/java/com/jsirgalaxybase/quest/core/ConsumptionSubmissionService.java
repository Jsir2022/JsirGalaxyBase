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
        if (intent == null || authorizer == null) throw new IllegalArgumentException("intent and authorizer are required");
        Authorized prepared = transaction.inTransaction(() -> {
            ConsumptionRequest request = authorizer.authorize(intent, now);
            return new Authorized(request, submissions.prepareIfAbsent(request));
        });
        return process(prepared.request, prepared.submission, now);
    }

    public ConsumptionSubmission resume(final String submissionKey, final long now) {
        ConsumptionSubmission submission = transaction.inTransaction(() -> submissions.find(submissionKey)
            .orElseThrow(() -> new IllegalArgumentException("unknown consumption submission")));
        return process(submission.getRequest(), submission, now);
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
