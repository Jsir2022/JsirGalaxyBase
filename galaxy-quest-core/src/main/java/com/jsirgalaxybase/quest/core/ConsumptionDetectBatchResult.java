package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of one bounded Quest-level consuming Detect attempt. */
public final class ConsumptionDetectBatchResult {
    public enum Status { COMPLETED, RESUMABLE, WAITING_FOR_SOURCE_SERVER, NO_CONSUMING_TASKS }

    private final String batchId;
    private final String sourceServer;
    private final Status status;
    private final List<ConsumptionSubmission> submissions;
    private final int nextTaskIndex;
    private final int taskCount;

    public ConsumptionDetectBatchResult(String batchId, Status status, List<ConsumptionSubmission> submissions,
        int nextTaskIndex, int taskCount) {
        this(batchId, "", status, submissions, nextTaskIndex, taskCount);
    }

    public ConsumptionDetectBatchResult(String batchId, String sourceServer, Status status,
        List<ConsumptionSubmission> submissions, int nextTaskIndex, int taskCount) {
        this.batchId = batchId == null ? "" : batchId;
        this.sourceServer = sourceServer == null ? "" : sourceServer;
        this.status = status;
        this.submissions = Collections.unmodifiableList(new ArrayList<ConsumptionSubmission>(
            submissions == null ? Collections.<ConsumptionSubmission>emptyList() : submissions));
        this.nextTaskIndex = Math.max(0, nextTaskIndex);
        this.taskCount = Math.max(0, taskCount);
    }

    public String getBatchId() { return batchId; }
    public String getSourceServer() { return sourceServer; }
    public Status getStatus() { return status; }
    public List<ConsumptionSubmission> getSubmissions() { return submissions; }
    public int getNextTaskIndex() { return nextTaskIndex; }
    public int getTaskCount() { return taskCount; }
}
