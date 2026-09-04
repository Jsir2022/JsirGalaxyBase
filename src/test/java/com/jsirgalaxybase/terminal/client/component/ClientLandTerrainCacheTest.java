package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ClientLandTerrainCacheTest {

    @Test
    public void heightShadingPreservesAlphaAndClampsChannels() {
        assertEquals(0xFF223344, ClientLandTerrainCache.shade(0xFF112233, 17));
        assertEquals(0xFFFFFFFF, ClientLandTerrainCache.shade(0xFFF8FAFC, 32));
        assertEquals(0xFF000000, ClientLandTerrainCache.shade(0xFF050607, -16));
    }
}
