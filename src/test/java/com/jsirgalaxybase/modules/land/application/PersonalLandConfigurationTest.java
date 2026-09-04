package com.jsirgalaxybase.modules.land.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Constructor;

import org.junit.Test;

import com.jsirgalaxybase.config.ModConfiguration;
import com.jsirgalaxybase.modules.land.domain.LandActionResult;
import com.jsirgalaxybase.modules.land.domain.LandChunkKey;

public class PersonalLandConfigurationTest {

    @Test
    public void serverOnlyLandConfigurationBuildsStrictRules() throws Exception {
        ModConfiguration configuration = configuration("ENFORCE", 2, new int[] { -1 },
            new String[] { "0:3:-4" });
        PersonalLandRules rules = PersonalLandRulesFactory.fromConfiguration(configuration, "lobby");

        assertTrue(configuration.isLandEnabled());
        assertEquals("ENFORCE", configuration.getLandProtectionMode());
        assertEquals(2, rules.getMaxClaimsPerPlayer());
        assertFalse(configuration.isLandAllowFakePlayers());
        assertEquals(LandActionResult.DIMENSION_BLOCKED,
            rules.evaluateClaim(new LandChunkKey("lobby", -1, 0, 0), 0));
        assertEquals(LandActionResult.RESERVED,
            rules.evaluateClaim(new LandChunkKey("lobby", 0, 3, -4), 0));
    }

    @Test
    public void malformedReservedChunkFailsInsteadOfBeingIgnored() throws Exception {
        ModConfiguration configuration = configuration("SHADOW", 4, new int[0],
            new String[] { "bad-coordinate" });
        try {
            PersonalLandRulesFactory.fromConfiguration(configuration, "lobby");
            fail("Expected malformed reserved chunk to fail");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("dimension:chunkX:chunkZ"));
        }
    }

    private static ModConfiguration configuration(String mode, int maxClaims, int[] blocked, String[] reserved)
        throws Exception {
        Constructor<ModConfiguration> constructor = ModConfiguration.class.getDeclaredConstructor(File.class,
            boolean.class, String.class, int.class, float.class, float.class, float.class, boolean.class,
            String.class, String.class, String.class, String.class, boolean.class, String.class, int.class,
            int[].class, String[].class, boolean.class, boolean.class, String[].class, String.class, String[].class);
        constructor.setAccessible(true);
        return constructor.newInstance(new File("."), false, "items", 0x529BED, 0.72f, 0.44f, 0.07f, true,
            "jdbc:postgresql://example.invalid/db", "test", "test", "lobby", true, mode, maxClaims, blocked,
            reserved, false, false, new String[0], "", new String[0]);
    }
}
