package com.jsirgalaxybase.terminal.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TerminalNumberFormatTest {

    @Test
    public void keepsSubThousandQuantitiesExact() {
        assertEquals("0", TerminalNumberFormat.compactQuantity(0L));
        assertEquals("999", TerminalNumberFormat.compactQuantity(999L));
        assertEquals("-999", TerminalNumberFormat.compactQuantity(-999L));
    }

    @Test
    public void usesThreeSignificantDigitsAcrossSiUnits() {
        assertEquals("1K", TerminalNumberFormat.compactQuantity(1000L));
        assertEquals("2.03K", TerminalNumberFormat.compactQuantity(2030L));
        assertEquals("12.3K", TerminalNumberFormat.compactQuantity(12399L));
        assertEquals("123K", TerminalNumberFormat.compactQuantity(123999L));
        assertEquals("1M", TerminalNumberFormat.compactQuantity(1000000L));
        assertEquals("1.23M", TerminalNumberFormat.compactQuantity(1234567L));
        assertEquals("1.23G", TerminalNumberFormat.compactQuantity(1234567890L));
        assertEquals("1.23T", TerminalNumberFormat.compactQuantity(1234567890123L));
        assertEquals("1.23P", TerminalNumberFormat.compactQuantity(1234567890123456L));
        assertEquals("1.23E", TerminalNumberFormat.compactQuantity(1234567890123456789L));
    }

    @Test
    public void truncatesMarketQuantitiesInsteadOfOverstatingThem() {
        assertEquals("999K", TerminalNumberFormat.compactQuantity(999999L));
        assertEquals("-2.03K", TerminalNumberFormat.compactQuantity(-2039L));
        assertEquals("-9.22E", TerminalNumberFormat.compactQuantity(Long.MIN_VALUE));
    }

    @Test
    public void keepsExactGroupedFormForActionableValues() {
        assertEquals("1,840,230", TerminalNumberFormat.exact(1840230L));
        assertEquals("-9,223,372,036,854,775,808", TerminalNumberFormat.exact(Long.MIN_VALUE));
    }
}
