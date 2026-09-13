package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class PortableNbtMatcherTest {
    @Test public void encodingIsStableAcrossCompoundInsertionOrderAndEscapedText() {
        Map<String, PortableNbt> left = new LinkedHashMap<String, PortableNbt>();
        left.put("z", PortableNbt.string("a:b\u0000中文"));
        left.put("a", PortableNbt.number("2.00"));
        Map<String, PortableNbt> right = new LinkedHashMap<String, PortableNbt>();
        right.put("a", PortableNbt.number("2"));
        right.put("z", PortableNbt.string("a:b\u0000中文"));
        assertEquals(PortableNbt.compound(left).encode(), PortableNbt.compound(right).encode());
        assertEquals(PortableNbt.compound(left).encode(), PortableNbt.decode(PortableNbt.compound(left).encode()).encode());
    }

    @Test public void partialCompoundAndUnorderedListFollowBqSemanticsWithoutReusingEntries() {
        Map<String, PortableNbt> requiredFields = new LinkedHashMap<String, PortableNbt>();
        requiredFields.put("grade", PortableNbt.number("2"));
        Map<String, PortableNbt> actualFields = new LinkedHashMap<String, PortableNbt>(requiredFields);
        actualFields.put("owner", PortableNbt.string("Alex"));
        assertTrue(PortableNbtMatcher.matches(PortableNbt.compound(requiredFields),
            PortableNbt.compound(actualFields), true));
        assertFalse(PortableNbtMatcher.matches(PortableNbt.compound(requiredFields),
            PortableNbt.compound(actualFields), false));

        PortableNbt required = PortableNbt.list(Arrays.asList(PortableNbt.number("7"), PortableNbt.number("7")));
        PortableNbt actual = PortableNbt.list(Arrays.asList(PortableNbt.number("7"), PortableNbt.number("8")));
        assertFalse(PortableNbtMatcher.matches(required, actual, true));
    }

    @Test public void arraysKeepTheirKindsAndNumbersCompareAcrossOriginalNbtWidths() {
        PortableNbt bytes = PortableNbt.byteArray(Arrays.asList(PortableNbt.number("1")));
        PortableNbt ints = PortableNbt.intArray(Arrays.asList(PortableNbt.number("1")));
        assertFalse(PortableNbtMatcher.matches(bytes, ints, false));
        assertTrue(PortableNbtMatcher.matches(PortableNbt.number("12.0"), PortableNbt.number("12"), false));
    }

    @Test public void malformedExternalEncodingFailsClosed() {
        assertFalse(PortableNbtMatcher.matches("C1:3:key", "C0:", true));
    }
}
