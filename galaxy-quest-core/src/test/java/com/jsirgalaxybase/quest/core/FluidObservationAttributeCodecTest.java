package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class FluidObservationAttributeCodecTest {
    @Test
    public void roundTripsAbsentAndExplicitlyEmptyNbt() {
        FluidObservationAttributeCodec codec = new FluidObservationAttributeCodec();
        Map<String, String> encoded = codec.encode(Arrays.asList(
            new FluidObservationEntry("water", 1000L, "", "", false),
            new FluidObservationEntry("water", 250L, "{}", "{\"type\":\"compound\",\"value\":{}}", true)));

        assertEquals("2", encoded.get(FluidObservationAttributeCodec.COUNT));
        assertEquals(Arrays.asList(
            new FluidObservationEntry("water", 1000L, "", "", false),
            new FluidObservationEntry("water", 250L, "{}", "{\"type\":\"compound\",\"value\":{}}", true)),
            codec.decode(encoded));
    }

    @Test
    public void aggregatesOnlySnapshotsPresentInAuthoritativeMemberOrder() {
        UUID first = UUID.randomUUID();
        UUID offline = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        Map<UUID, java.util.List<FluidObservationEntry>> active = new LinkedHashMap<UUID, java.util.List<FluidObservationEntry>>();
        active.put(first, Collections.singletonList(new FluidObservationEntry("water", 400L, "", "", false)));
        active.put(third, Collections.singletonList(new FluidObservationEntry("water", 600L, "", "", false)));

        java.util.List<FluidObservationEntry> result = new BqPartyFluidInventorySnapshotAggregator()
            .aggregate(Arrays.asList(first, offline, third), active);

        assertEquals(1, result.size());
        assertEquals("water", result.get(0).getName());
        assertEquals(1000L, result.get(0).getAmount());
        assertTrue("canonicalization should retain the first inventory's NBT shape", !result.get(0).isNbtPresent());
    }
}
