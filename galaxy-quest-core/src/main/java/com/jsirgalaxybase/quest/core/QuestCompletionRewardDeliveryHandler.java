package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** Delivery adapter for BetterQuesting's reward that completes another quest. */
public final class QuestCompletionRewardDeliveryHandler implements RewardDeliveryHandler {
    private final QuestCompletionRewardPort completion;
    private final QuestClock clock;

    public QuestCompletionRewardDeliveryHandler(QuestCompletionRewardPort completion, QuestClock clock) {
        if (completion == null || clock == null) throw new IllegalArgumentException("completion and clock are required");
        this.completion = completion;
        this.clock = clock;
    }

    @Override public String getTypeId() { return "bq_standard:questcompletion"; }

    @Override public RewardDeliveryOutcome deliver(RewardDeliveryLease lease) {
        String raw = lease.getReward().getParameters().get("questUuid");
        final UUID target;
        try { target = UUID.fromString(raw); }
        catch (RuntimeException invalid) {
            return RewardDeliveryOutcome.failed("invalid-reward:questUuid", false);
        }
        QuestCompletionRewardPort.Result result = completion.complete(lease.getEntitlementKey(),
            lease.getParticipantId(), target, clock.currentTimeMillis());
        if (result == QuestCompletionRewardPort.Result.TARGET_NOT_FOUND) {
            return RewardDeliveryOutcome.failed("target-quest-not-published:" + target, false);
        }
        return RewardDeliveryOutcome.delivered();
    }
}
