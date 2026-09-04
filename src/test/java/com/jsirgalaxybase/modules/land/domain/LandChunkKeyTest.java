package com.jsirgalaxybase.modules.land.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class LandChunkKeyTest {

    @Test
    public void blockCoordinatesUseMinecraftChunkFlooringForNegativeValues() {
        assertEquals(new LandChunkKey("s2", 0, 0, 0), LandChunkKey.fromBlockCoordinates("s2", 0, 15, 15));
        assertEquals(new LandChunkKey("s2", 0, 1, 1), LandChunkKey.fromBlockCoordinates("s2", 0, 16, 16));
        assertEquals(new LandChunkKey("s2", 0, -1, -1), LandChunkKey.fromBlockCoordinates("s2", 0, -1, -1));
        assertEquals(new LandChunkKey("s2", 0, -2, -2), LandChunkKey.fromBlockCoordinates("s2", 0, -17, -17));
    }

    @Test
    public void serverIdentityIsPartOfThePhysicalLandKey() {
        LandChunkKey s1 = new LandChunkKey("s1", 0, 12, -5);
        LandChunkKey s2 = new LandChunkKey("s2", 0, 12, -5);

        assertNotEquals(s1, s2);
        assertEquals("s1:[0@12,-5]", s1.toString());
        assertEquals(200, s1.getCenterBlockX());
        assertEquals(-72, s1.getCenterBlockZ());
    }
}
