package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConsumptionSubmission {
    private final ConsumptionRequest request;
    private final ConsumptionSubmissionStatus status;
    private final String evidence;
    private final List<Long> appliedAmounts;

    public ConsumptionSubmission(ConsumptionRequest request, ConsumptionSubmissionStatus status, String evidence) {
        this(request, status, evidence, status == ConsumptionSubmissionStatus.APPLIED
            || status == ConsumptionSubmissionStatus.CONFIRMED ? request.getAmounts() : Collections.<Long>emptyList());
    }

    public ConsumptionSubmission(ConsumptionRequest request, ConsumptionSubmissionStatus status, String evidence,
        List<Long> appliedAmounts) {
        this.request = Objects.requireNonNull(request, "request");
        this.status = Objects.requireNonNull(status, "status");
        this.evidence = evidence == null ? "" : evidence;
        this.appliedAmounts = Collections.unmodifiableList(new ArrayList<Long>(appliedAmounts == null
            ? Collections.<Long>emptyList() : appliedAmounts));
    }

    public ConsumptionRequest getRequest() { return request; }
    public ConsumptionSubmissionStatus getStatus() { return status; }
    public String getEvidence() { return evidence; }
    public List<Long> getAppliedAmounts() { return appliedAmounts; }
}
