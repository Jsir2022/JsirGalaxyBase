package com.jsirgalaxybase.terminal.client.viewmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TerminalLandSectionModel {

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
    private final List<MapCellModel> mapCells;
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

    public TerminalLandSectionModel(String serviceState, String serverId, String protectionMode,
        int dimensionId, int centerChunkX, int centerChunkZ, int selectedChunkX, int selectedChunkZ,
        int usedClaims, int maxClaims, String tab, List<String> gridStates, List<Long> ownedTitleIds,
        List<Integer> ownedChunkXs, List<Integer> ownedChunkZs, List<Long> ownedVersions,
        int pageIndex, int totalPages, int totalEntries, long selectedTitleId, long selectedVersion,
        String selectedState, boolean canClaim, boolean canUnclaim, String feedbackCode) {
        this(serviceState, serverId, protectionMode, dimensionId, centerChunkX, centerChunkZ,
            selectedChunkX, selectedChunkZ, usedClaims, maxClaims, tab, centerChunkX, centerChunkZ,
            "MEDIUM", Collections.<MapCellModel>emptyList(), ownedTitleIds, ownedChunkXs, ownedChunkZs,
            ownedVersions, pageIndex, totalPages, totalEntries, selectedTitleId, selectedVersion,
            selectedState, canClaim, canUnclaim, feedbackCode);
    }

    public TerminalLandSectionModel(String serviceState, String serverId, String protectionMode,
        int dimensionId, int centerChunkX, int centerChunkZ, int selectedChunkX, int selectedChunkZ,
        int usedClaims, int maxClaims, String tab, int viewportChunkX, int viewportChunkZ, String zoom,
        List<MapCellModel> mapCells, List<Long> ownedTitleIds, List<Integer> ownedChunkXs,
        List<Integer> ownedChunkZs, List<Long> ownedVersions, int pageIndex, int totalPages,
        int totalEntries, long selectedTitleId, long selectedVersion, String selectedState,
        boolean canClaim, boolean canUnclaim, String feedbackCode) {
        this.serviceState = safe(serviceState, "UNAVAILABLE");
        this.serverId = safe(serverId, "unknown");
        this.protectionMode = safe(protectionMode, "SHADOW");
        this.dimensionId = dimensionId;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.selectedChunkX = selectedChunkX;
        this.selectedChunkZ = selectedChunkZ;
        this.usedClaims = usedClaims;
        this.maxClaims = maxClaims;
        this.tab = safe(tab, "NEARBY");
        this.viewportChunkX = viewportChunkX;
        this.viewportChunkZ = viewportChunkZ;
        this.zoom = safe(zoom, "MEDIUM");
        this.mapCells = Collections.unmodifiableList(new ArrayList<MapCellModel>(mapCells == null
            ? Collections.<MapCellModel>emptyList() : mapCells));
        this.ownedTitleIds = freezeLongs(ownedTitleIds);
        this.ownedChunkXs = freezeInts(ownedChunkXs);
        this.ownedChunkZs = freezeInts(ownedChunkZs);
        this.ownedVersions = freezeLongs(ownedVersions);
        this.pageIndex = pageIndex;
        this.totalPages = totalPages;
        this.totalEntries = totalEntries;
        this.selectedTitleId = selectedTitleId;
        this.selectedVersion = selectedVersion;
        this.selectedState = safe(selectedState, "UNCLAIMED");
        this.canClaim = canClaim;
        this.canUnclaim = canUnclaim;
        this.feedbackCode = safe(feedbackCode, "NONE");
    }

    public static TerminalLandSectionModel unavailable() {
        return new TerminalLandSectionModel("UNAVAILABLE", "unknown", "SHADOW", 0, 0, 0, 0, 0, 0, 0,
            "NEARBY", Collections.<String>emptyList(), Collections.<Long>emptyList(),
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
    public List<MapCellModel> getMapCells() { return mapCells; }
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

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
    private static List<Long> freezeLongs(List<Long> value) { return Collections.unmodifiableList(new ArrayList<Long>(
        value == null ? Collections.<Long>emptyList() : value)); }
    private static List<Integer> freezeInts(List<Integer> value) { return Collections.unmodifiableList(new ArrayList<Integer>(
        value == null ? Collections.<Integer>emptyList() : value)); }

    public static final class MapCellModel {
        private final int chunkX;
        private final int chunkZ;
        private final String state;
        private final long titleId;
        private final long version;

        public MapCellModel(int chunkX, int chunkZ, String state, long titleId, long version) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.state = safe(state, "OTHER");
            this.titleId = Math.max(0L, titleId);
            this.version = Math.max(0L, version);
        }

        public int getChunkX() { return chunkX; }
        public int getChunkZ() { return chunkZ; }
        public String getState() { return state; }
        public long getTitleId() { return titleId; }
        public long getVersion() { return version; }
    }
}
