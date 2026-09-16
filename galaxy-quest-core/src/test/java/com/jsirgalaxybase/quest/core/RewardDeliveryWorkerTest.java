package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class RewardDeliveryWorkerTest {
    @Test
    public void deliversWithStableEntitlementKeyAndConfirmsLease() {
        FakeRepository repository = new FakeRepository(lease(1));
        final List<String> externalKeys = new ArrayList<String>();
        RewardDeliveryWorker worker = worker(repository, externalKeys, RewardDeliveryOutcome.delivered(), 3);

        RewardDeliveryBatchResult result = worker.runOnce("worker-a", 10);

        assertEquals(Collections.singletonList("entitlement-1"), externalKeys);
        assertEquals(1, result.getLeased());
        assertEquals(1, result.getDelivered());
        assertEquals(0, result.getStaleConfirmations());
        assertEquals("entitlement-1", repository.completedKey);
        assertEquals(1, repository.completedAttempt);
    }

    @Test
    public void retryableFailureStopsAtMaximumAttempt() {
        FakeRepository retryRepository = new FakeRepository(lease(2));
        RewardDeliveryBatchResult retry = worker(retryRepository, new ArrayList<String>(),
            RewardDeliveryOutcome.failed("full", true), 3).runOnce("worker-a", 1);
        assertEquals(1, retry.getRetryScheduled());
        assertTrue(retryRepository.retryable);
        assertEquals(1200L, retryRepository.retryAt);

        FakeRepository abandonedRepository = new FakeRepository(lease(3));
        RewardDeliveryBatchResult abandoned = worker(abandonedRepository, new ArrayList<String>(),
            RewardDeliveryOutcome.failed("full", true), 3).runOnce("worker-a", 1);
        assertEquals(1, abandoned.getAbandoned());
        assertFalse(abandonedRepository.retryable);
    }

    @Test
    public void deferredExternalConditionNeverExhaustsAttempts() {
        FakeRepository repository = new FakeRepository(lease(99,0));
        RewardDeliveryBatchResult result = worker(repository, new ArrayList<String>(),
            RewardDeliveryOutcome.deferred("player-offline"), 3).runOnce("worker-a", 1);
        assertEquals(1, result.getRetryScheduled());
        assertEquals(0, result.getAbandoned());
        assertTrue(repository.deferred);
        assertTrue(repository.retryable);
    }

    @Test
    public void staleConfirmationIsReportedWithoutCountingSuccess() {
        FakeRepository repository = new FakeRepository(lease(1));
        repository.acceptConfirmation = false;
        RewardDeliveryBatchResult result = worker(repository, new ArrayList<String>(),
            RewardDeliveryOutcome.delivered(), 3).runOnce("worker-a", 1);
        assertEquals(0, result.getDelivered());
        assertEquals(1, result.getStaleConfirmations());
    }

    private static RewardDeliveryWorker worker(FakeRepository repository, final List<String> keys,
        final RewardDeliveryOutcome outcome, int maxAttempts) {
        QuestTransaction transaction = new QuestTransaction() {
            @Override
            public <T> T inTransaction(Work<T> work) { return work.execute(); }
        };
        RewardDeliveryPort delivery = lease -> {
            keys.add(lease.getEntitlementKey());
            return outcome;
        };
        return new RewardDeliveryWorker(repository, transaction, delivery, () -> 1000L, 100L, 200L, maxAttempts);
    }

    private static RewardDeliveryLease lease(int attempt) {
        return lease(attempt,Math.max(0,attempt-1));
    }

    private static RewardDeliveryLease lease(int attempt,int failureCount) {
        UUID player=UUID.randomUUID();return new RewardDeliveryLease("entitlement-1", ParticipantId.player(player),player,
            new RewardDefinition("reward", "item", Collections.<String, String>emptyMap()), attempt,
            failureCount,"worker-a", 1100L);
    }

    private static final class FakeRepository implements RewardDeliveryRepository {
        private final List<RewardDeliveryLease> leases;
        private boolean acceptConfirmation = true;
        private String completedKey;
        private int completedAttempt;
        private boolean retryable;
        private boolean deferred;
        private long retryAt;

        private FakeRepository(RewardDeliveryLease lease) {
            this.leases = Collections.singletonList(lease);
        }

        @Override
        public List<RewardDeliveryLease> leaseAvailable(String workerId, long now, long leaseUntil, int limit) {
            return leases;
        }

        @Override
        public boolean markDelivered(String entitlementKey, String workerId, int attempt, long deliveredAt) {
            completedKey = entitlementKey;
            completedAttempt = attempt;
            return acceptConfirmation;
        }

        @Override public boolean markDeferred(String entitlementKey,String workerId,int attempt,String error,
            long deferredAt,long retryAt){completedKey=entitlementKey;completedAttempt=attempt;this.retryAt=retryAt;
            this.retryable=true;this.deferred=true;return acceptConfirmation;}

        @Override
        public boolean markFailed(String entitlementKey, String workerId, int attempt, String error,
            long failedAt, long retryAt, boolean retryable) {
            completedKey = entitlementKey;
            completedAttempt = attempt;
            this.retryAt = retryAt;
            this.retryable = retryable;
            return acceptConfirmation;
        }
    }
}
