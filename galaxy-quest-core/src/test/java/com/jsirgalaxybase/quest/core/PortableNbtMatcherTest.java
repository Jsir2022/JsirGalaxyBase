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

    @Test public void compoundsRemainSubsetMatchesButListsUseBqPartialFlag() {
        Map<String, PortableNbt> requiredFields = new LinkedHashMap<String, PortableNbt>();
        requiredFields.put("grade", PortableNbt.number("2"));
        Map<String, PortableNbt> actualFields = new LinkedHashMap<String, PortableNbt>(requiredFields);
        actualFields.put("owner", PortableNbt.string("Alex"));
        assertTrue(PortableNbtMatcher.matches(PortableNbt.compound(requiredFields),
            PortableNbt.compound(actualFields), true));
        assertTrue(PortableNbtMatcher.matches(PortableNbt.compound(requiredFields),
            PortableNbt.compound(actualFields), false));
        assertFalse(PortableNbtMatcher.matches(PortableNbt.compound(actualFields),
            PortableNbt.compound(requiredFields), false));

        PortableNbt requiredList = PortableNbt.list(Arrays.asList(PortableNbt.string("required")));
        PortableNbt longerList = PortableNbt.list(Arrays.asList(PortableNbt.string("required"),
            PortableNbt.string("extra")));
        assertTrue(PortableNbtMatcher.matches(requiredList, longerList, true));
        assertFalse(PortableNbtMatcher.matches(requiredList, longerList, false));
        assertTrue(PortableNbtMatcher.matches(requiredList,
            PortableNbt.list(Arrays.asList(PortableNbt.string("different"))), false));

        Map<String, PortableNbt> repeatedRequirement = new LinkedHashMap<String, PortableNbt>();
        repeatedRequirement.put("id", PortableNbt.number("7"));
        Map<String, PortableNbt> unmatchedValue = new LinkedHashMap<String, PortableNbt>();
        unmatchedValue.put("id", PortableNbt.number("8"));
        PortableNbt required = PortableNbt.list(Arrays.asList(
            PortableNbt.compound(repeatedRequirement), PortableNbt.compound(repeatedRequirement)));
        PortableNbt actual = PortableNbt.list(Arrays.asList(
            PortableNbt.compound(repeatedRequirement), PortableNbt.compound(unmatchedValue)));
        // BQ's NBTTagList branch can reuse the same actual compound for repeated requirements.
        assertTrue(PortableNbtMatcher.matches(required, actual, true));
    }

    @Test public void arraysKeepTheirKindsAndNumbersCompareAcrossOriginalNbtWidths() {
        PortableNbt bytes = PortableNbt.byteArray(Arrays.asList(PortableNbt.number("1")));
        PortableNbt ints = PortableNbt.intArray(Arrays.asList(PortableNbt.number("1")));
        assertFalse(PortableNbtMatcher.matches(bytes, ints, false));
        assertTrue(PortableNbtMatcher.matches(PortableNbt.number("12.0"), PortableNbt.number("12"), false));

        PortableNbt duplicateBytes = PortableNbt.byteArray(Arrays.asList(
            PortableNbt.number("7"), PortableNbt.number("7")));
        PortableNbt oneMatchingByte = PortableNbt.byteArray(Arrays.asList(
            PortableNbt.number("7"), PortableNbt.number("8")));
        assertFalse(PortableNbtMatcher.matches(duplicateBytes, oneMatchingByte, true));
        PortableNbt duplicateInts = PortableNbt.intArray(Arrays.asList(
            PortableNbt.number("7"), PortableNbt.number("7")));
        PortableNbt oneMatchingInt = PortableNbt.intArray(Arrays.asList(
            PortableNbt.number("7"), PortableNbt.number("8")));
        assertFalse(PortableNbtMatcher.matches(duplicateInts, oneMatchingInt, true));
    }

    @Test public void malformedExternalEncodingFailsClosed() {
        assertFalse(PortableNbtMatcher.matches("C1:3:key", "C0:", true));
    }
}
