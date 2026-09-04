package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TerminalLandSectionSnapshot {

    private final String serviceState;
    private final String serverId;
    private final String protectionMode;
    private final int dimensionId;
    private final int centerChunkX;
    private final int centerChunkZ;
    private final int selectedChunkX;
    private final int selectedChunkZ;
    private final int usedClaims;
    private final int maxClaims;
    private final String tab;
    private final int viewportChunkX;
    private final int viewportChunkZ;
    private final String zoom;
    private final List<TerminalLandMapCellSnapshot> mapCells;
    private final List<Long> ownedTitleIds;
    private final List<Integer> ownedChunkXs;
    private final List<Integer> ownedChunkZs;
    private final List<Long> ownedVersions;
    private final int pageIndex;
    private final int totalPages;
    private final int totalEntries;
    private final long selectedTitleId;
    private final long selectedVersion;
    private final String selectedState;
    private final boolean canClaim;
    private final boolean canUnclaim;
    private final String feedbackCode;

    public TerminalLandSectionSnapshot(String serviceState, String serverId, String protectionMode,
        int dimensionId, int centerChunkX, int centerChunkZ, int selectedChunkX, int selectedChunkZ,
        int usedClaims, int maxClaims, String tab, List<String> gridStates, List<Long> ownedTitleIds,
        List<Integer> ownedChunkXs, List<Integer> ownedChunkZs, List<Long> ownedVersions,
        int pageIndex, int totalPages, int totalEntries, long selectedTitleId, long selectedVersion,
        String selectedState, boolean canClaim, boolean canUnclaim, String feedbackCode) {
        this(serviceState, serverId, protectionMode, dimensionId, centerChunkX, centerChunkZ,
            selectedChunkX, selectedChunkZ, usedClaims, maxClaims, tab, centerChunkX, centerChunkZ,
            TerminalLandActionPayload.Zoom.MEDIUM.name(), mapCellsFromGrid(centerChunkX, centerChunkZ, gridStates),
            ownedTitleIds, ownedChunkXs, ownedChunkZs, ownedVersions, pageIndex, totalPages, totalEntries,
            selectedTitleId, selectedVersion, selectedState, canClaim, canUnclaim, feedbackCode);
    }

    public TerminalLandSectionSnapshot(String serviceState, String serverId, String protectionMode,
        int dimensionId, int centerChunkX, int centerChunkZ, int selectedChunkX, int selectedChunkZ,
        int usedClaims, int maxClaims, String tab, int viewportChunkX, int viewportChunkZ, String zoom,
        List<TerminalLandMapCellSnapshot> mapCells, List<Long> ownedTitleIds,
        List<Integer> ownedChunkXs, List<Integer> ownedChunkZs, List<Long> ownedVersions,
        int pageIndex, int totalPages, int totalEntries, long selectedTitleId, long selectedVersion,
        String selectedState, boolean canClaim, boolean canUnclaim, String feedbackCode) {
        this.serviceState = normalize(serviceState, "UNAVAILABLE");
        this.serverId = normalize(serverId, "unknown");
        this.protectionMode = normalize(protectionMode, "SHADOW");
        this.dimensionId = dimensionId;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.selectedChunkX = selectedChunkX;
        this.selectedChunkZ = selectedChunkZ;
        this.usedClaims = Math.max(0, usedClaims);
        this.maxClaims = Math.max(0, maxClaims);
        this.tab = normalize(tab, "NEARBY");
        this.viewportChunkX = viewportChunkX;
        this.viewportChunkZ = viewportChunkZ;
        this.zoom = normalize(zoom, TerminalLandActionPayload.Zoom.MEDIUM.name());
        this.mapCells = freezeCells(mapCells);
        this.ownedTitleIds = freezeLongs(ownedTitleIds);
        this.ownedChunkXs = freezeInts(ownedChunkXs);
        this.ownedChunkZs = freezeInts(ownedChunkZs);
        this.ownedVersions = freezeLongs(ownedVersions);
        this.pageIndex = Math.max(0, pageIndex);
        this.totalPages = Math.max(0, totalPages);
        this.totalEntries = Math.max(0, totalEntries);
        this.selectedTitleId = Math.max(0L, selectedTitleId);
        this.selectedVersion = Math.max(0L, selectedVersion);
        this.selectedState = normalize(selectedState, "UNCLAIMED");
        this.canClaim = canClaim;
        this.canUnclaim = canUnclaim;
        this.feedbackCode = normalize(feedbackCode, "NONE");
    }

    public static TerminalLandSectionSnapshot unavailable() {
        return new TerminalLandSectionSnapshot("UNAVAILABLE", "unknown", "SHADOW", 0, 0, 0, 0, 0,
            0, 0, "NEARBY", Collections.<String>emptyList(), Collections.<Long>emptyList(),
            Collections.<Integer>emptyList(), Collections.<Integer>emptyList(), Collections.<Long>emptyList(),
            0, 0, 0, 0L, 0L, "UNCLAIMED", false, false, "NONE");
    }

    public String getServiceState() { return serviceState; }
    public String getServerId() { return serverId; }
    public String getProtectionMode() { return protectionMode; }
    public int getDimensionId() { return dimensionId; }
    public int getCenterChunkX() { return centerChunkX; }
    public int getCenterChunkZ() { return centerChunkZ; }
    public int getSelectedChunkX() { return selectedChunkX; }
    public int getSelectedChunkZ() { return selectedChunkZ; }
    public int getUsedClaims() { return usedClaims; }
    public int getMaxClaims() { return maxClaims; }
    public String getTab() { return tab; }
    public int getViewportChunkX() { return viewportChunkX; }
    public int getViewportChunkZ() { return viewportChunkZ; }
    public String getZoom() { return zoom; }
    public List<TerminalLandMapCellSnapshot> getMapCells() { return mapCells; }
    public List<Long> getOwnedTitleIds() { return ownedTitleIds; }
    public List<Integer> getOwnedChunkXs() { return ownedChunkXs; }
    public List<Integer> getOwnedChunkZs() { return ownedChunkZs; }
    public List<Long> getOwnedVersions() { return ownedVersions; }
    public int getPageIndex() { return pageIndex; }
    public int getTotalPages() { return totalPages; }
    public int getTotalEntries() { return totalEntries; }
    public long getSelectedTitleId() { return selectedTitleId; }
    public long getSelectedVersion() { return selectedVersion; }
    public String getSelectedState() { return selectedState; }
    public boolean isCanClaim() { return canClaim; }
    public boolean isCanUnclaim() { return canUnclaim; }
    public String getFeedbackCode() { return feedbackCode; }

    private static String normalize(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static List<Long> freezeLongs(List<Long> value) {
        return Collections.unmodifiableList(new ArrayList<Long>(value == null ? Collections.<Long>emptyList() : value));
    }

    private static List<Integer> freezeInts(List<Integer> value) {
        return Collections.unmodifiableList(new ArrayList<Integer>(value == null ? Collections.<Integer>emptyList() : value));
    }

    private static List<TerminalLandMapCellSnapshot> freezeCells(List<TerminalLandMapCellSnapshot> value) {
        return Collections.unmodifiableList(new ArrayList<TerminalLandMapCellSnapshot>(value == null
            ? Collections.<TerminalLandMapCellSnapshot>emptyList() : value));
    }

    private static List<TerminalLandMapCellSnapshot> mapCellsFromGrid(int centerX, int centerZ,
        List<String> states) {
        List<TerminalLandMapCellSnapshot> cells = new ArrayList<TerminalLandMapCellSnapshot>();
        if (states == null) return cells;
        int size = (int) Math.round(Math.sqrt(states.size()));
        if (size <= 0 || size * size != states.size()) return cells;
        int radius = size / 2;
        for (int index = 0; index < states.size(); index++) {
            String state = states.get(index);
            if (state != null && !"UNCLAIMED".equals(state)) {
                cells.add(new TerminalLandMapCellSnapshot(centerX + index % size - radius,
                    centerZ + index / size - radius, state, 0L, 0L));
            }
        }
        return cells;
    }
}
