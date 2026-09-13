package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.jsirgalaxybase.terminal.TerminalLandActionPayload;
import com.jsirgalaxybase.ui2.geometry.UiRect;

public class LandMapViewportTest {

    @Test
    public void mapsNegativeChunkCoordinatesAtEdgesAndCenter() {
        UiRect map = new UiRect(10, 20, 300, 300);
        assertArrayEquals(new int[] { -17, -27 }, LandMapViewport.chunkAt(map, -10, -20,
            TerminalLandActionPayload.Zoom.MEDIUM, 10, 20));
        assertArrayEquals(new int[] { -10, -20 }, LandMapViewport.chunkAt(map, -10, -20,
            TerminalLandActionPayload.Zoom.MEDIUM, 160, 170));
    }

    @Test
    public void rectangularViewportKeepsSquareCellsAndUsesLongestSideDensity() {
        UiRect wide = new UiRect(10, 20, 450, 270);
        assertEquals(30D, LandMapViewport.cellSize(wide, TerminalLandActionPayload.Zoom.MEDIUM), 0.001D);
        assertEquals(15, LandMapViewport.visibleColumns(wide, TerminalLandActionPayload.Zoom.MEDIUM));
        assertEquals(9, LandMapViewport.visibleRows(wide, TerminalLandActionPayload.Zoom.MEDIUM));
        assertArrayEquals(new int[] { 10, 20 }, LandMapViewport.chunkAt(wide, 10, 20,
            TerminalLandActionPayload.Zoom.MEDIUM, 235, 155));
        assertArrayEquals(new int[] { 3, 16 }, LandMapViewport.chunkAt(wide, 10, 20,
            TerminalLandActionPayload.Zoom.MEDIUM, 10, 20));

        UiRect tall = new UiRect(0, 0, 270, 450);
        assertEquals(30D, LandMapViewport.cellSize(tall, TerminalLandActionPayload.Zoom.MEDIUM), 0.001D);
        assertEquals(9, LandMapViewport.visibleColumns(tall, TerminalLandActionPayload.Zoom.MEDIUM));
        assertEquals(15, LandMapViewport.visibleRows(tall, TerminalLandActionPayload.Zoom.MEDIUM));
    }

    @Test
    public void allZoomLevelsUseNineFifteenAndThirtyOneCellsOnLongestSide() {
        UiRect map = new UiRect(0, 0, 620, 340);
        assertEquals(9, LandMapViewport.visibleColumns(map, TerminalLandActionPayload.Zoom.NEAR));
        assertEquals(15, LandMapViewport.visibleColumns(map, TerminalLandActionPayload.Zoom.MEDIUM));
        assertEquals(31, LandMapViewport.visibleColumns(map, TerminalLandActionPayload.Zoom.FAR));
    }

    @Test
    public void dragAndZoomKeepBoundedStableCoordinates() {
        UiRect map = new UiRect(0, 0, 310, 310);
        assertArrayEquals(new int[] { 9, 12 }, LandMapViewport.panCenter(10, 10, map,
            TerminalLandActionPayload.Zoom.FAR, 10, -20));
        int[] centered = LandMapViewport.zoomCenter(-40, 70, map, TerminalLandActionPayload.Zoom.MEDIUM,
            TerminalLandActionPayload.Zoom.FAR, 155, 155);
        assertArrayEquals(new int[] { -40, 70 }, centered);
        assertEquals(Integer.MAX_VALUE - 16,
            LandMapViewport.clampAround(Integer.MAX_VALUE, Integer.MIN_VALUE, 16));
        assertEquals(Integer.MIN_VALUE + 16,
            LandMapViewport.clampAround(Integer.MIN_VALUE, Integer.MAX_VALUE, 16));
    }

    @Test
    public void zoomStopsAtNearAndFar() {
        assertEquals(TerminalLandActionPayload.Zoom.NEAR,
            LandMapViewport.stepZoom(TerminalLandActionPayload.Zoom.NEAR, 1));
        assertEquals(TerminalLandActionPayload.Zoom.FAR,
            LandMapViewport.stepZoom(TerminalLandActionPayload.Zoom.FAR, -1));
    }
}
