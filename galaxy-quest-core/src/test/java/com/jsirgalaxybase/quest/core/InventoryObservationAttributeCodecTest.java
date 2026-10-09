package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class InventoryObservationAttributeCodecTest {
    private final InventoryObservationAttributeCodec codec = new InventoryObservationAttributeCodec();

    @Test
    public void roundTripsCanonicalInventoryEntriesAndOptionalNbt() {
        List<InventoryObservationEntry> entries = Arrays.asList(
            new InventoryObservationEntry("minecraft:iron_ingot", 0, 2L, "ingotIron", "", ""),
            new InventoryObservationEntry("minecraft:iron_ingot", 0, 3L, "ingotIron", "", ""),
            new InventoryObservationEntry("example:tool", 7, 1L, "", "{damage:2}", "{damage:2}", true));

        Map<String, String> attributes = codec.encode(entries);
        List<InventoryObservationEntry> decoded = codec.decode(attributes);

        assertEquals("2", attributes.get(InventoryObservationAttributeCodec.COUNT));
        assertEquals(5L, decoded.get(0).getAmount());
        assertEquals("example:tool", decoded.get(1).getRegistryName());
        assertTrue(decoded.get(1).isDamageable());
        assertFalse(attributes.containsKey("inventory.1.nbt"));
        assertTrue(decoded.get(1).getNbt().isEmpty());
        assertEquals("{damage:2}", decoded.get(1).getNbtPortable());
    }

    @Test
    public void rejectsMalformedAndUnboundedSnapshots() {
        try {
            codec.decode(Collections.<String, String>singletonMap(InventoryObservationAttributeCodec.COUNT, "4097"));
            fail("expected entry limit");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("out of range"));
        }

        try {
            codec.decode(Collections.<String, String>singletonMap(InventoryObservationAttributeCodec.COUNT, "1"));
            fail("expected missing entry fields");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("missing inventory attribute"));
        }
    }
}
