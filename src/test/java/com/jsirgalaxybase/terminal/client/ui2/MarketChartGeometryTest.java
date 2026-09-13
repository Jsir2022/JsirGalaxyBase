package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel.PricePointModel;
import com.jsirgalaxybase.ui2.geometry.UiRect;

public class MarketChartGeometryTest {
    @Test public void projectsOhlcvInsideBothChartAreas() {
        MarketChartGeometry.Projection p = MarketChartGeometry.project(Arrays.asList(
            new PricePointModel(100, 105, 98, 102, 40, 4080, 1),
            new PricePointModel(102, 108, 101, 107, 80, 8560, 2)), new UiRect(4, 6, 240, 120));
        assertEquals(2, p.points.size());
        for (MarketChartGeometry.Point q : p.points) {
            assertTrue(p.priceArea.contains(q.x, q.openY));
            assertTrue(p.priceArea.contains(q.x, q.closeY));
            assertTrue(q.highY <= q.lowY);
            assertTrue(q.volumeTop >= p.volumeArea.getY());
            assertTrue(q.volumeTop <= p.volumeArea.getBottom());
        }
    }

    @Test public void emptyAndSingleExtremePointStayFinite() {
        MarketChartGeometry.Projection empty = MarketChartGeometry.project(
            Collections.<PricePointModel>emptyList(), new UiRect(0, 0, 30, 20));
        assertTrue(empty.points.isEmpty());
        MarketChartGeometry.Projection one = MarketChartGeometry.project(Collections.singletonList(
            new PricePointModel(Long.MAX_VALUE - 2, Long.MAX_VALUE - 1, Long.MAX_VALUE - 3,
                Long.MAX_VALUE - 2, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)),
            new UiRect(-20, -10, 80, 50));
        assertEquals(1, one.points.size());
        assertTrue(one.priceArea.contains(one.points.get(0).x, one.points.get(0).closeY));
    }
}
