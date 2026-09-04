package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TerminalLandActionPayload {

    public enum Tab { NEARBY, MINE }

    public enum Zoom {
        NEAR(4), MEDIUM(7), FAR(15);

        private final int radius;

        Zoom(int radius) { this.radius = radius; }

        public int getRadius() { return radius; }
    }

    private static final int MAX_PAGE = 1000;

    private final Tab tab;
    private final int selectedChunkX;
    private final int selectedChunkZ;
    private final int pageIndex;
    private final String requestId;
    private final long expectedVersion;
    private final int viewportChunkX;
    private final int viewportChunkZ;
    private final Zoom zoom;

    public TerminalLandActionPayload(Tab tab, int selectedChunkX, int selectedChunkZ, int pageIndex,
        String requestId, long expectedVersion) {
        this(tab, selectedChunkX, selectedChunkZ, pageIndex, requestId, expectedVersion,
            selectedChunkX, selectedChunkZ, Zoom.MEDIUM);
    }

    public TerminalLandActionPayload(Tab tab, int selectedChunkX, int selectedChunkZ, int pageIndex,
        String requestId, long expectedVersion, int viewportChunkX, int viewportChunkZ, Zoom zoom) {
        this.tab = tab == null ? Tab.NEARBY : tab;
        this.selectedChunkX = selectedChunkX;
        this.selectedChunkZ = selectedChunkZ;
        this.pageIndex = Math.max(0, Math.min(MAX_PAGE, pageIndex));
        this.requestId = normalize(requestId, 96);
        this.expectedVersion = Math.max(0L, expectedVersion);
        this.viewportChunkX = viewportChunkX;
        this.viewportChunkZ = viewportChunkZ;
        this.zoom = zoom == null ? Zoom.MEDIUM : zoom;
    }

    public static TerminalLandActionPayload empty() {
        return new TerminalLandActionPayload(Tab.NEARBY, 0, 0, 0, "", 0L);
    }

    public static TerminalLandActionPayload decode(String payload) {
        if (payload == null || payload.trim().isEmpty()) return empty();
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 6 && parts.length != 9) return empty();
        try {
            int selectedX = Integer.parseInt(parts[1]);
            int selectedZ = Integer.parseInt(parts[2]);
            return new TerminalLandActionPayload(Tab.valueOf(parts[0]), selectedX, selectedZ,
                Integer.parseInt(parts[3]), decodePart(parts[4]), Long.parseLong(parts[5]),
                parts.length == 9 ? Integer.parseInt(parts[6]) : selectedX,
                parts.length == 9 ? Integer.parseInt(parts[7]) : selectedZ,
                parts.length == 9 ? Zoom.valueOf(parts[8]) : Zoom.MEDIUM);
        } catch (RuntimeException ignored) {
            return empty();
        }
    }

    public String encode() {
        return tab.name() + "|" + selectedChunkX + "|" + selectedChunkZ + "|" + pageIndex + "|"
            + encodePart(requestId) + "|" + expectedVersion + "|" + viewportChunkX + "|"
            + viewportChunkZ + "|" + zoom.name();
    }

    public Tab getTab() { return tab; }
    public int getSelectedChunkX() { return selectedChunkX; }
    public int getSelectedChunkZ() { return selectedChunkZ; }
    public int getPageIndex() { return pageIndex; }
    public String getRequestId() { return requestId; }
    public long getExpectedVersion() { return expectedVersion; }
    public boolean hasRequestId() { return !requestId.isEmpty(); }
    public int getViewportChunkX() { return viewportChunkX; }
    public int getViewportChunkZ() { return viewportChunkZ; }
    public Zoom getZoom() { return zoom; }

    private static String encodePart(String value) {
        return Base64.getUrlEncoder().encodeToString(normalize(value, 96).getBytes(StandardCharsets.UTF_8));
    }

    private static String decodePart(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(normalize(value, 160)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static String normalize(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
