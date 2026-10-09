package com.jsirgalaxybase.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Constructor;

import org.junit.Test;

public class ModConfigurationQuestRewardTest {
    @Test
    public void legacyConfigurationDefaultsOffAndExtendedConfigurationRetainsValue() throws Exception {
        assertFalse(configuration(false).isQuestNoRewardsEnabled());
        assertTrue(configuration(true).isQuestNoRewardsEnabled());
    }

    private static ModConfiguration configuration(boolean noRewards) throws Exception {
        Constructor<ModConfiguration> constructor = ModConfiguration.class.getDeclaredConstructor(File.class,
            boolean.class, String.class, int.class, float.class, float.class, float.class, boolean.class,
            String.class, String.class, String.class, String.class, boolean.class, String.class, int.class,
            int[].class, String[].class, boolean.class, boolean.class, boolean.class, String[].class,
            String.class, String[].class, boolean.class, String[].class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(new File("."), false, "items", 0x529BED, 0.72f, 0.44f, 0.07f,
            false, "", "", "", "", false, "SHADOW", 4, new int[0], new String[0], false, false,
            false, new String[0], "", new String[0], false, new String[0], noRewards);
    }
}
