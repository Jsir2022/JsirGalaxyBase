package com.jsirgalaxybase.modules.land;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class LandModuleTest {

    @Test
    public void moduleDoesNotCreateClientOrInMemoryRuntimeInConstructor() {
        LandModule module = new LandModule();

        assertEquals("land", module.getId());
        assertEquals("core", module.getCategory());
        assertNull(module.getPersonalLandService());
        assertFalse(module.isRuntimeAvailable());
    }
}
