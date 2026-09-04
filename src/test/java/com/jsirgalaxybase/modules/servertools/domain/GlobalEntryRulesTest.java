package com.jsirgalaxybase.modules.servertools.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GlobalEntryRulesTest {

    @Test
    public void parsesHubAndTargetRtpProfiles() {
        GlobalEntryRules rules = GlobalEntryRules.parse("lobby|0|10.5|72|20.5|90|0",
            new String[] { "s2|7|100|80|-50|256|2048" });
        assertTrue(rules.getHubTarget().isPresent());
        assertEquals("lobby", rules.getHubTarget().get().getServerId());
        TargetServerRtpProfile profile = rules.findRtpProfile("s2").get();
        assertEquals(7, profile.getDimensionId());
        assertEquals(2048.0D, profile.getMaxDistance(), 0.0001D);
        assertEquals("s2", profile.getFallbackTarget().getServerId());
    }

    @Test
    public void emptyConfigurationKeepsLocalSpawnAndDisablesTargetRtp() {
        GlobalEntryRules rules = GlobalEntryRules.parse("", new String[0]);
        assertFalse(rules.getHubTarget().isPresent());
        assertFalse(rules.findRtpProfile("lobby").isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDuplicateTargetProfiles() {
        GlobalEntryRules.parse("", new String[] { "s2|0|0|80|0|1|2", "s2|0|0|80|0|1|2" });
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidHubDefinition() {
        GlobalEntryRules.parse("lobby|0|1", new String[0]);
    }
}
