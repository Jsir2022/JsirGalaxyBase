package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestEvaluationRequest;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/** Main-thread-only, bounded retry buffer for immutable facts not yet accepted by PostgreSQL. */
final class OrderedQuestFactRetryQueue {
    static final int CAPACITY = 1024;
    static final long MAX_RETAINED_BYTES = 8L * 1024L * 1024L;
    static final int RETRY_INTERVAL_TICKS = 20;
    static final int MAX_FACTS_PER_RETRY = 32;

    private final Queue<PendingFact> pending = new ArrayDeque<PendingFact>();
    private long retainedBytes;
    private long rejectedCount;

    SubmissionResult submit(GameplayFact fact, QuestFactSink sink) {
        return submit(fact, sink, Operation.ACCEPT, null);
    }

    SubmissionResult submit(GameplayFact fact, QuestFactSink sink,
        List<QuestEvaluationRequest> eventTimeRequests) {
        return submit(fact, sink, Operation.ACCEPT, immutableRequests(eventTimeRequests));
    }

    SubmissionResult submitInventorySession(GameplayFact fact, QuestFactSink sink) {
        return submit(fact, sink, Operation.BEGIN_INVENTORY_SESSION, null);
    }

    private SubmissionResult submit(GameplayFact fact, QuestFactSink sink, Operation operation,
        List<QuestEvaluationRequest> eventTimeRequests) {
        if (fact == null || sink == null) throw new IllegalArgumentException("fact and sink are required");
        if (!pending.isEmpty()) return enqueue(fact, operation, eventTimeRequests, null);
        try {
            deliver(sink, fact, operation, eventTimeRequests);
            return new SubmissionResult(false, null);
        } catch (RuntimeException failure) {
            return enqueue(fact, operation, eventTimeRequests, failure);
        }
    }

    FlushResult retry(QuestFactSink sink, int budget) {
        if (sink == null || budget < 0) throw new IllegalArgumentException("sink and non-negative budget are required");
        int accepted = 0;
        while (accepted < budget && !pending.isEmpty()) {
            PendingFact next = pending.peek();
            try {
                deliver(sink, next.fact, next.operation, next.eventTimeRequests);
                pending.remove();
                retainedBytes -= next.estimatedBytes;
                accepted++;
            } catch (RuntimeException failure) {
                return new FlushResult(accepted, failure);
            }
        }
        return new FlushResult(accepted, null);
    }

    int pendingCount() { return pending.size(); }
    long retainedBytes() { return retainedBytes; }
    boolean isEmpty() { return pending.isEmpty(); }
    long drainRejectedCount() { long result = rejectedCount; rejectedCount = 0; return result; }
    void clear() { pending.clear(); retainedBytes = 0L; rejectedCount = 0; }

    private SubmissionResult enqueue(GameplayFact fact, Operation operation,
        List<QuestEvaluationRequest> eventTimeRequests, RuntimeException failure) {
        long estimatedBytes = estimateRetainedBytes(fact, eventTimeRequests);
        if (pending.size() >= CAPACITY || estimatedBytes > MAX_RETAINED_BYTES
            || retainedBytes > MAX_RETAINED_BYTES - estimatedBytes) {
            rejectedCount++;
            return new SubmissionResult(true, failure);
        }
        pending.add(new PendingFact(fact, operation, eventTimeRequests, estimatedBytes));
        retainedBytes += estimatedBytes;
        return new SubmissionResult(false, failure);
    }

    private static void deliver(QuestFactSink sink, GameplayFact fact, Operation operation,
        List<QuestEvaluationRequest> eventTimeRequests) {
        if (operation == Operation.BEGIN_INVENTORY_SESSION) sink.beginInventorySession(fact);
        else if (eventTimeRequests == null) sink.accept(fact);
        else sink.accept(fact, eventTimeRequests);
    }

    private static long estimateRetainedBytes(GameplayFact fact,
        List<QuestEvaluationRequest> eventTimeRequests) {
        long chars = fact.getSourceServer().length() + fact.getEventId().length() + fact.getTypeId().length() + 36L;
        long mapEntries = fact.getAttributes().size();
        for (Map.Entry<String, String> entry : fact.getAttributes().entrySet()) {
            chars += entry.getKey().length();
            if (entry.getValue() != null) chars += entry.getValue().length();
        }
        // Conservative accounting for UTF-16 strings, map entries, and object references.
        long bytes = 256L + mapEntries * 128L + chars * 2L;
        if (eventTimeRequests != null) for (QuestEvaluationRequest request : eventTimeRequests) {
            bytes = add(bytes, estimateRequestBytes(request));
        }
        return bytes;
    }

    private static long estimateRequestBytes(QuestEvaluationRequest request) {
        QuestDefinition definition = request.getDefinition();
        long bytes = 512L + 64L * request.getMembership().getPlayerIds().size()
            + 64L * request.getFactMembership().getPlayerIds().size()
            + 48L * request.getCompletedPrerequisites().size()
            + 48L * definition.getPrerequisites().size();
        bytes = add(bytes, stringBytes(definition.getName()));
        bytes = add(bytes, stringBytes(definition.getDescription()));
        for (TaskDefinition task : definition.getTasks()) {
            bytes = add(bytes, 128L + stringBytes(task.getKey()) + stringBytes(task.getTypeId())
                + estimateStringMap(task.getParameters()));
        }
        for (RewardDefinition reward : definition.getRewards()) {
            bytes = add(bytes, 128L + stringBytes(reward.getKey()) + stringBytes(reward.getTypeId())
                + estimateStringMap(reward.getParameters()));
        }
        return bytes;
    }

    private static long estimateStringMap(Map<String, String> values) {
        long bytes = 48L * values.size();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            bytes = add(bytes, stringBytes(entry.getKey()));
            bytes = add(bytes, stringBytes(entry.getValue()));
        }
        return bytes;
    }

    private static long stringBytes(String value) {
        return value == null ? 0L : 40L + 2L * value.length();
    }

    private static long add(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static List<QuestEvaluationRequest> immutableRequests(List<QuestEvaluationRequest> requests) {
        if (requests == null) return null;
        List<QuestEvaluationRequest> copy = new ArrayList<QuestEvaluationRequest>(requests.size());
        for (QuestEvaluationRequest request : requests) {
            if (request == null) throw new IllegalArgumentException("event-time request must not be null");
            copy.add(request);
        }
        return Collections.unmodifiableList(copy);
    }

    private enum Operation { ACCEPT, BEGIN_INVENTORY_SESSION }

    private static final class PendingFact {
        private final GameplayFact fact;
        private final Operation operation;
        private final List<QuestEvaluationRequest> eventTimeRequests;
        private final long estimatedBytes;
        private PendingFact(GameplayFact fact, Operation operation,
            List<QuestEvaluationRequest> eventTimeRequests, long estimatedBytes) {
            this.fact = fact;
            this.operation = operation;
            this.eventTimeRequests = eventTimeRequests;
            this.estimatedBytes = estimatedBytes;
        }
    }

    static final class SubmissionResult {
        private final boolean rejected;
        private final RuntimeException failure;
        private SubmissionResult(boolean rejected, RuntimeException failure) {
            this.rejected = rejected;
            this.failure = failure;
        }
        boolean isRejected() { return rejected; }
        RuntimeException getFailure() { return failure; }
    }

    static final class FlushResult {
        private final int accepted;
        private final RuntimeException failure;
        private FlushResult(int accepted, RuntimeException failure) {
            this.accepted = accepted;
            this.failure = failure;
        }
        int getAccepted() { return accepted; }
        RuntimeException getFailure() { return failure; }
    }
}
