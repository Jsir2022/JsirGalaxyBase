package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class InventoryObservationCanonicalizerTest {
    @Test
    public void mergesOnlyEquivalentStacksAndPreservesFirstSeenOrder() {
        List<InventoryObservationEntry> result = new InventoryObservationCanonicalizer().canonicalize(Arrays.asList(
            entry("minecraft:iron_ingot", 0, 3L, "ingotIron", "{}"),
            entry("minecraft:gold_ingot", 0, 2L, "ingotGold", "{}"),
            entry("minecraft:iron_ingot", 0, 4L, "ingotIron", "{}"),
            entry("minecraft:iron_ingot", 0, 8L, "ingotIron", "{\"tag\":1}")));
        assertEquals(3, result.size());
        assertEquals("minecraft:iron_ingot", result.get(0).getRegistryName());
        assertEquals(7L, result.get(0).getAmount());
        assertEquals("minecraft:gold_ingot", result.get(1).getRegistryName());
        assertEquals("{\"tag\":1}", result.get(2).getNbtPortable());
    }

    @Test
    public void saturatesEquivalentStackAmounts() {
        List<InventoryObservationEntry> result = new InventoryObservationCanonicalizer().canonicalize(Arrays.asList(
            entry("minecraft:stone", 0, Long.MAX_VALUE, "", "{}"),
            entry("minecraft:stone", 0, 1L, "", "{}")));
        assertEquals(1, result.size());
        assertEquals(Long.MAX_VALUE, result.get(0).getAmount());
    }

    private static InventoryObservationEntry entry(String name, int meta, long amount, String ore, String nbt) {
        return new InventoryObservationEntry(name, meta, amount, ore, nbt, nbt);
    }
}
