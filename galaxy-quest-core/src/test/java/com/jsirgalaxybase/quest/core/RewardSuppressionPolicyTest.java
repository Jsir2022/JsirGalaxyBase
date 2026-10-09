package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;

public class RewardSuppressionPolicyTest {
    @Test
    public void noRewardsSuppressesDefaultRewardsButHonorsBqIgnoreDisabledDefaults() {
        RewardSuppressionPolicy policy = new RewardSuppressionPolicy(true);

        assertTrue(policy.suppresses(reward("bq_standard:item", Collections.<String, String>emptyMap())));
        assertTrue(policy.suppresses(reward("bq_standard:choice", Collections.<String, String>emptyMap())));
        assertTrue(policy.suppresses(reward("bq_standard:xp", Collections.<String, String>emptyMap())));
        assertFalse(policy.suppresses(reward("bq_standard:command", Collections.<String, String>emptyMap())));
        assertFalse(policy.suppresses(reward("bq_standard:questcompletion", Collections.<String, String>emptyMap())));
        assertFalse(policy.suppresses(reward("bq_standard:scoreboard", Collections.<String, String>emptyMap())));
    }

    @Test
    public void explicitIgnoreDisabledOverridesDefaultsAndAcceptsBqNumericBooleans() {
        RewardSuppressionPolicy policy = new RewardSuppressionPolicy(true);

        assertFalse(policy.suppresses(reward("bq_standard:item", parameter("ignoreDisabled", "1"))));
        assertTrue(policy.suppresses(reward("bq_standard:command", parameter("ignoreDisabled", "0"))));
    }

    @Test
    public void disabledGlobalPolicyNeverSuppressesAndUnknownRewardsUseDefaultFalse() {
        assertFalse(new RewardSuppressionPolicy(false)
            .suppresses(reward("bq_standard:item", Collections.<String, String>emptyMap())));
        assertTrue(new RewardSuppressionPolicy(true)
            .suppresses(reward("example:unknown", Collections.<String, String>emptyMap())));
    }

    private static RewardDefinition reward(String type, Map<String, String> parameters) {
        return new RewardDefinition("reward-1", type, parameters);
    }

    private static Map<String, String> parameter(String key, String value) {
        return Collections.singletonMap(key, value);
    }
}
