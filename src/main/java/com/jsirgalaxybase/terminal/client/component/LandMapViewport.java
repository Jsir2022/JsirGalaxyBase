package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.terminal.TerminalLandActionPayload;
import com.jsirgalaxybase.ui2.geometry.UiRect;

public final class LandMapViewport {

    public static final int DRAG_THRESHOLD = 4;

    private LandMapViewport() {}

    public static int[] chunkAt(UiRect map, int centerX, int centerZ,
        TerminalLandActionPayload.Zoom zoom, int mouseX, int mouseY) {
        if (map == null || !map.contains(mouseX, mouseY)) return null;
        double cell = cellSize(map, zoom);
        int offsetX = (int) Math.floor((mouseX - centerPixelX(map)) / cell + 0.5D);
        int offsetZ = (int) Math.floor((mouseY - centerPixelY(map)) / cell + 0.5D);
        int radius = safeZoom(zoom).getRadius();
        if (Math.abs((long) offsetX) > radius || Math.abs((long) offsetZ) > radius) return null;
        return new int[] { saturatingAdd(centerX, offsetX), saturatingAdd(centerZ, offsetZ) };
    }

    public static int[] panCenter(int centerX, int centerZ, UiRect map,
        TerminalLandActionPayload.Zoom zoom, int deltaX, int deltaY) {
        double cell = cellSize(map, zoom);
        return new int[] { saturatingAdd(centerX, -(int) Math.round(deltaX / cell)),
            saturatingAdd(centerZ, -(int) Math.round(deltaY / cell)) };
    }

    public static int[] zoomCenter(int centerX, int centerZ, UiRect map,
        TerminalLandActionPayload.Zoom oldZoom, TerminalLandActionPayload.Zoom newZoom,
        int mouseX, int mouseY) {
        if (map == null || !map.contains(mouseX, mouseY)) return new int[] { centerX, centerZ };
        double oldCell = cellSize(map, oldZoom);
        double newCell = cellSize(map, newZoom);
        double offsetX = mouseX - centerPixelX(map);
        double offsetZ = mouseY - centerPixelY(map);
        double anchorX = centerX + offsetX / oldCell;
        double anchorZ = centerZ + offsetZ / oldCell;
        return new int[] { clampLong(Math.round(anchorX - offsetX / newCell)),
            clampLong(Math.round(anchorZ - offsetZ / newCell)) };
    }

    public static TerminalLandActionPayload.Zoom stepZoom(TerminalLandActionPayload.Zoom zoom, int direction) {
        TerminalLandActionPayload.Zoom value = safeZoom(zoom);
        if (direction > 0) {
            return value == TerminalLandActionPayload.Zoom.FAR ? TerminalLandActionPayload.Zoom.MEDIUM
                : TerminalLandActionPayload.Zoom.NEAR;
        }
        return value == TerminalLandActionPayload.Zoom.NEAR ? TerminalLandActionPayload.Zoom.MEDIUM
            : TerminalLandActionPayload.Zoom.FAR;
    }

    public static int clampAround(int player, int requested, int radius) {
        long min = Math.max(Integer.MIN_VALUE, (long) player - radius);
        long max = Math.min(Integer.MAX_VALUE, (long) player + radius);
        return clampLong(Math.max(min, Math.min(max, (long) requested)));
    }

    public static double cellSize(UiRect map, TerminalLandActionPayload.Zoom zoom) {
        if (map == null) return 1D;
        int side = safeZoom(zoom).getRadius() * 2 + 1;
        return Math.max(1D, Math.max(map.getWidth(), map.getHeight()) / (double) side);
    }

    public static int visibleColumns(UiRect map, TerminalLandActionPayload.Zoom zoom) {
        return visibleCells(map == null ? 0 : map.getWidth(), cellSize(map, zoom));
    }

    public static int visibleRows(UiRect map, TerminalLandActionPayload.Zoom zoom) {
        return visibleCells(map == null ? 0 : map.getHeight(), cellSize(map, zoom));
    }

    public static int cellLeft(UiRect map, TerminalLandActionPayload.Zoom zoom, int offsetX) {
        return (int) Math.floor(centerPixelX(map) + (offsetX - 0.5D) * cellSize(map, zoom));
    }

    public static int cellTop(UiRect map, TerminalLandActionPayload.Zoom zoom, int offsetZ) {
        return (int) Math.floor(centerPixelY(map) + (offsetZ - 0.5D) * cellSize(map, zoom));
    }

    public static int cellRight(UiRect map, TerminalLandActionPayload.Zoom zoom, int offsetX) {
        return (int) Math.ceil(centerPixelX(map) + (offsetX + 0.5D) * cellSize(map, zoom));
    }

    public static int cellBottom(UiRect map, TerminalLandActionPayload.Zoom zoom, int offsetZ) {
        return (int) Math.ceil(centerPixelY(map) + (offsetZ + 0.5D) * cellSize(map, zoom));
    }

    private static int visibleCells(int pixels, double cell) {
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(1, pixels) / cell)));
    }

    private static double centerPixelX(UiRect map) {
        return map.getX() + map.getWidth() / 2D;
    }

    private static double centerPixelY(UiRect map) {
        return map.getY() + map.getHeight() / 2D;
    }

    private static int saturatingAdd(int value, int delta) {
        return clampLong((long) value + delta);
    }

    private static int clampLong(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    private static TerminalLandActionPayload.Zoom safeZoom(TerminalLandActionPayload.Zoom zoom) {
        return zoom == null ? TerminalLandActionPayload.Zoom.MEDIUM : zoom;
    }
}
