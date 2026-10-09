package com.jsirgalaxybase.quest.core;

/** Applies BetterQuesting's server-wide noRewards gate without conflating it with successful delivery. */
public final class RewardSuppressionPolicy {
    private final boolean noRewards;

    public RewardSuppressionPolicy(boolean noRewards) {
        this.noRewards = noRewards;
    }

    public boolean isNoRewardsEnabled() {
        return noRewards;
    }

    public boolean suppresses(RewardDefinition reward) {
        if (!noRewards || reward == null) return false;
        boolean ignoreDisabled = QuestBooleanParameter.parse(reward.getParameters().get("ignoreDisabled"),
            defaultIgnoreDisabled(reward.getTypeId()));
        return !ignoreDisabled;
    }

    public static boolean defaultIgnoreDisabled(String rewardType) {
        return "bq_standard:command".equals(rewardType)
            || "bq_standard:questcompletion".equals(rewardType)
            || "bq_standard:scoreboard".equals(rewardType);
    }
}
