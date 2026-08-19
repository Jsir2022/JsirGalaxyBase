package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MarketCompactTextTest {

    @Test
    public void keepsFourDigitOrderBookQuantityOnTheVisibleLine() {
        assertEquals("80x2.03K",
            MarketCompactText.compactOrderBookLine("买价 80 | 剩余 2,030 | orderId=228"));
    }

    @Test
    public void compactsGroupedPriceAndQuantityWithoutChangingTheirValues() {
        assertEquals("1,200x1.23M",
            MarketCompactText.compactOrderBookLine("卖价 1,200 | 剩余 1,234,567 | orderId=9"));
    }

    @Test
    public void keepsExactServerValuesForClickableBookRows() {
        long[] level = MarketCompactText.exactOrderBookLevel(
            "买价 1,200 | 剩余 1,234,567 | orderId=9");
        assertEquals(1200L, level[0]);
        assertEquals(1234567L, level[1]);
    }

    @Test
    public void preservesNonOrderBookFallbackText() {
        assertEquals("当前没有买盘。", MarketCompactText.compactOrderBookLine("当前没有买盘。"));
    }
}
