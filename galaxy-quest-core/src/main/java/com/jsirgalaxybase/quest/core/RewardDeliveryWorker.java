package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.Objects;

public final class RewardDeliveryWorker {
    private final RewardDeliveryRepository repository;
    private final QuestTransaction transaction;
    private final RewardDeliveryPort delivery;
    private final QuestClock clock;
    private final long leaseMillis;
    private final long retryDelayMillis;
    private final int maxAttempts;

    public RewardDeliveryWorker(RewardDeliveryRepository repository, QuestTransaction transaction,
        RewardDeliveryPort delivery, QuestClock clock, long leaseMillis, long retryDelayMillis, int maxAttempts) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (leaseMillis < 1 || retryDelayMillis < 0 || maxAttempts < 1) throw new IllegalArgumentException(
            "invalid delivery policy");
        this.leaseMillis = leaseMillis;
        this.retryDelayMillis = retryDelayMillis;
        this.maxAttempts = maxAttempts;
    }

    public RewardDeliveryBatchResult runOnce(final String workerId, final int limit) {
        final long leaseStart = clock.currentTimeMillis();
        List<RewardDeliveryLease> leases = transaction.inTransaction(() -> repository
            .leaseAvailable(workerId, leaseStart, saturatingAdd(leaseStart, leaseMillis), limit));
        int deliveredCount = 0;
        int retryCount = 0;
        int abandonedCount = 0;
        int stale = 0;
        for (final RewardDeliveryLease lease : leases) {
            RewardDeliveryOutcome outcome;
            try {
                outcome = delivery.deliver(lease);
                if (outcome == null) outcome = RewardDeliveryOutcome.failed("delivery returned no outcome", true);
            } catch (RuntimeException failure) {
                outcome = RewardDeliveryOutcome.failed(failure.getClass().getSimpleName() + ": " + failure.getMessage(), true);
            }
            final long finishedAt = clock.currentTimeMillis();
            final RewardDeliveryOutcome finalOutcome = outcome;
            boolean confirmed;
            if (outcome.isDelivered()) {
                confirmed = transaction.inTransaction(() -> repository.markDelivered(lease.getEntitlementKey(),
                    workerId, lease.getAttempt(), finishedAt));
                if (confirmed) deliveredCount++; else stale++;
            } else {
                final boolean retryable = outcome.isRetryable() && lease.getAttempt() < maxAttempts;
                final long retryAt = saturatingAdd(finishedAt, retryDelayMillis);
                confirmed = transaction.inTransaction(() -> repository.markFailed(lease.getEntitlementKey(), workerId,
                    lease.getAttempt(), finalOutcome.getError(), finishedAt, retryAt, retryable));
                if (!confirmed) stale++;
                else if (retryable) retryCount++;
                else abandonedCount++;
            }
        }
        return new RewardDeliveryBatchResult(leases.size(), deliveredCount, retryCount, abandonedCount, stale);
    }

    private static long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
