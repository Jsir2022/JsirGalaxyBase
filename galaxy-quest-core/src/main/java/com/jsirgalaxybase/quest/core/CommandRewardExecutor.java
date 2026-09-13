package com.jsirgalaxybase.quest.core;

/** Executor must durably deduplicate by invocation entitlement key before reporting success. */
public interface CommandRewardExecutor {
    RewardDeliveryOutcome execute(CommandRewardInvocation invocation);
}
