package com.jsirgalaxybase.modules.land.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class LandProtectionModeTest {

    @Test
    public void parserAcceptsOnlyExplicitModes() {
        assertEquals(LandProtectionMode.SHADOW, LandProtectionMode.parseStrict("shadow"));
        assertEquals(LandProtectionMode.ENFORCE, LandProtectionMode.parseStrict(" ENFORCE "));
        try {
            LandProtectionMode.parseStrict("disabled");
            fail("Expected invalid mode to fail");
        } catch (IllegalArgumentException expected) {
            assertEquals("landProtectionMode must be SHADOW or ENFORCE: disabled", expected.getMessage());
        }
    }
}
