package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class TerminalLandActionPayloadTest {

    @Test
    public void roundTripsNegativeCoordinatesAndVersion() {
        TerminalLandActionPayload payload = new TerminalLandActionPayload(TerminalLandActionPayload.Tab.MINE,
            -17, 23, 4, "request-中文", 9L);
        TerminalLandActionPayload decoded = TerminalLandActionPayload.decode(payload.encode());

        assertEquals(TerminalLandActionPayload.Tab.MINE, decoded.getTab());
        assertEquals(-17, decoded.getSelectedChunkX());
        assertEquals(23, decoded.getSelectedChunkZ());
        assertEquals(4, decoded.getPageIndex());
        assertEquals("request-中文", decoded.getRequestId());
        assertEquals(9L, decoded.getExpectedVersion());
        assertEquals(-17, decoded.getViewportChunkX());
        assertEquals(23, decoded.getViewportChunkZ());
        assertEquals(TerminalLandActionPayload.Zoom.MEDIUM, decoded.getZoom());
    }

    @Test
    public void malformedAndOversizedValuesAreBounded() {
        assertFalse(TerminalLandActionPayload.decode("broken").hasRequestId());
        TerminalLandActionPayload payload = new TerminalLandActionPayload(null, 1, 2, Integer.MAX_VALUE,
            new String(new char[200]).replace('\0', 'x'), -4L);
        assertEquals(1000, payload.getPageIndex());
        assertEquals(96, payload.getRequestId().length());
        assertEquals(0L, payload.getExpectedVersion());
    }

    @Test
    public void extendedViewportRoundTripsAndLegacyPayloadDefaultsToMedium() {
        TerminalLandActionPayload payload = new TerminalLandActionPayload(TerminalLandActionPayload.Tab.NEARBY,
            -2, 4, 0, "", 0L, 7, -9, TerminalLandActionPayload.Zoom.FAR);
        TerminalLandActionPayload decoded = TerminalLandActionPayload.decode(payload.encode());
        assertEquals(7, decoded.getViewportChunkX());
        assertEquals(-9, decoded.getViewportChunkZ());
        assertEquals(TerminalLandActionPayload.Zoom.FAR, decoded.getZoom());

        TerminalLandActionPayload legacy = TerminalLandActionPayload.decode("NEARBY|-3|5|0||0");
        assertEquals(-3, legacy.getViewportChunkX());
        assertEquals(5, legacy.getViewportChunkZ());
        assertEquals(TerminalLandActionPayload.Zoom.MEDIUM, legacy.getZoom());
    }
}
