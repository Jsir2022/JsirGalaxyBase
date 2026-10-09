package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class BqPartyInventorySnapshotAggregatorTest {
    @Test
    public void combinesOnlyPresentMembersInAuthoritativeMembershipOrder() {
        UUID first = UUID.randomUUID();
        UUID offline = UUID.randomUUID();
        UUID last = UUID.randomUUID();
        Map<UUID, List<InventoryObservationEntry>> active = new LinkedHashMap<UUID, List<InventoryObservationEntry>>();
        active.put(last, Collections.singletonList(item("minecraft:iron_ingot", 3L)));
        active.put(first, Collections.singletonList(item("minecraft:iron_ingot", 2L)));

        List<InventoryObservationEntry> aggregate = new BqPartyInventorySnapshotAggregator().aggregate(
            Arrays.asList(first, offline, last), active);

        assertEquals(1, aggregate.size());
        assertEquals(5L, aggregate.get(0).getAmount());
        assertEquals("minecraft:iron_ingot", aggregate.get(0).getRegistryName());
    }

    @Test
    public void emptyActiveSetProducesAnEmptySnapshot() {
        assertTrue(new BqPartyInventorySnapshotAggregator().aggregate(Collections.<UUID>emptyList(),
            Collections.<UUID, List<InventoryObservationEntry>>emptyMap()).isEmpty());
    }

    private static InventoryObservationEntry item(String name, long amount) {
        return new InventoryObservationEntry(name, 0, amount, "", "", "");
    }
}
