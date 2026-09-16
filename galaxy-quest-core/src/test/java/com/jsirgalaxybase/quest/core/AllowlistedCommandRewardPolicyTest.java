package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class AllowlistedCommandRewardPolicyTest {
    @Test public void emptyAllowlistDeniesEveryCommand() {
        AllowlistedCommandRewardPolicy policy = new AllowlistedCommandRewardPolicy(Collections.<String>emptySet());
        assertFalse(policy.allows("give Alex stone", false));
        assertFalse(policy.allows("galaxyreward grant Alex", true));
    }

    @Test public void matchesOnlyTheExactNormalizedRoot() {
        AllowlistedCommandRewardPolicy policy = new AllowlistedCommandRewardPolicy(
            new java.util.HashSet<String>(Arrays.asList(" GalaxyReward ", "scoreboard")));
        assertTrue(policy.allows("galaxyreward grant Alex", false));
        assertTrue(policy.allows("SCOREBOARD players add Alex quest 1", false));
        assertFalse(policy.allows("galaxyreward-admin grant Alex", false));
        assertFalse(policy.allows("/galaxyreward grant Alex", false));
    }

    @Test public void rejectsEmptyOversizedAndControlCharacterInput() {
        AllowlistedCommandRewardPolicy policy = new AllowlistedCommandRewardPolicy(
            Collections.singleton("galaxyreward"));
        assertFalse(policy.allows("", false));
        assertFalse(policy.allows("galaxyreward grant\nstop", false));
        StringBuilder oversized = new StringBuilder("galaxyreward ");
        while (oversized.length() <= 2048) oversized.append('x');
        assertFalse(policy.allows(oversized.toString(), false));
    }
}
