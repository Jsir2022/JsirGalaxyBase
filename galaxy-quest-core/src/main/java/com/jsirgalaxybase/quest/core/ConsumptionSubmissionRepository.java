package com.jsirgalaxybase.quest.core;

import java.util.Optional;

public interface ConsumptionSubmissionRepository {
    ConsumptionSubmission prepareIfAbsent(ConsumptionRequest request);
    Optional<ConsumptionSubmission> find(String submissionKey);
    ConsumptionSubmission markApplied(String submissionKey, String evidence, java.util.List<Long> appliedAmounts, long now);
    ConsumptionSubmission markConfirmed(String submissionKey, long now);
    ConsumptionSubmission markRejected(String submissionKey, String reason, long now);
}
