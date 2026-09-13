package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.terminal.client.settings.TerminalWindowSize;

public class TerminalWindowMetricsTest {
    @Test public void centersAWindowInsteadOfUsingTheWholeRegularViewport() {
        TerminalWindowMetrics metrics = TerminalWindowMetrics.compute(new UiSize(464,253));
        UiRect bounds = metrics.getBounds();
        assertEquals(418, bounds.getWidth());
        assertEquals(223, bounds.getHeight());
        assertEquals((464-418)/2, bounds.getX());
        assertEquals((253-223)/2, bounds.getY());
        assertTrue(metrics.getMainWidth() > metrics.getNavigationWidth());
    }

    @Test public void keepsAcceptanceMinimumAndCapsLargeWindows() {
        UiRect minimum = TerminalWindowMetrics.compute(new UiSize(350,193)).getBounds();
        assertEquals(350, minimum.getWidth());
        assertEquals(193, minimum.getHeight());
        UiRect large = TerminalWindowMetrics.compute(new UiSize(1200,800)).getBounds();
        assertEquals(620, large.getWidth());
        assertEquals(340, large.getHeight());
        assertEquals(290, large.getX());
        assertEquals(230, large.getY());
    }

    @Test public void remainsSafeBelowTheNormalMinimum() {
        UiRect tiny = TerminalWindowMetrics.compute(new UiSize(240,140)).getBounds();
        assertEquals(new UiRect(0,0,240,140), tiny);
    }

    @Test public void appliesDiscreteWindowProfilesWithoutBecomingFullscreen() {
        UiSize viewport=new UiSize(464,253);
        UiRect compact=TerminalWindowMetrics.compute(viewport,TerminalWindowSize.COMPACT).getBounds();
        UiRect standard=TerminalWindowMetrics.compute(viewport,TerminalWindowSize.STANDARD).getBounds();
        UiRect large=TerminalWindowMetrics.compute(viewport,TerminalWindowSize.LARGE).getBounds();
        assertTrue(compact.getWidth()<standard.getWidth());
        assertTrue(compact.getHeight()<standard.getHeight());
        assertTrue(standard.getWidth()<large.getWidth());
        assertTrue(standard.getHeight()<large.getHeight());
        assertTrue(large.getWidth()<viewport.getWidth());
        assertTrue(large.getHeight()<viewport.getHeight());
    }
}
