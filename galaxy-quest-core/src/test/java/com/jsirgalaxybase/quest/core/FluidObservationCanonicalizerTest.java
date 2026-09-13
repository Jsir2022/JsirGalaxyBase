package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class FluidObservationCanonicalizerTest {
    @Test
    public void mergesSameFluidAndPortableNbtInFirstSeenOrder() {
        List<FluidObservationEntry> result = new FluidObservationCanonicalizer().canonicalize(Arrays.asList(
            new FluidObservationEntry("water", 1000L, "{A:1}", "{A:1}"),
            new FluidObservationEntry("lava", 500L, "", ""),
            new FluidObservationEntry("water", 2000L, "{A:1,extra:ignored-by-portable}", "{A:1}")));
        assertEquals(2, result.size());
        assertEquals("water", result.get(0).getName());
        assertEquals(3000L, result.get(0).getAmount());
        assertEquals("{A:1}", result.get(0).getNbt());
        assertEquals("lava", result.get(1).getName());
    }

    @Test
    public void keepsDifferentFluidNamesOrNbtSeparateAndSaturates() {
        List<FluidObservationEntry> result = new FluidObservationCanonicalizer().canonicalize(Arrays.asList(
            new FluidObservationEntry("water", Long.MAX_VALUE, "", ""),
            new FluidObservationEntry("water", 1L, "", ""),
            new FluidObservationEntry("water", 1L, "{A:2}", "{A:2}"),
            new FluidObservationEntry("Water", 1L, "", "")));
        assertEquals(3, result.size());
        assertEquals(Long.MAX_VALUE, result.get(0).getAmount());
        assertEquals("{A:2}", result.get(1).getNbtPortable());
        assertEquals("Water", result.get(2).getName());
    }
}
