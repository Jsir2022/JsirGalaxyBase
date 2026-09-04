package com.jsirgalaxybase.terminal;

public final class TerminalLandMapCellSnapshot {

    private final int chunkX;
    private final int chunkZ;
    private final String state;
    private final long titleId;
    private final long version;

    public TerminalLandMapCellSnapshot(int chunkX, int chunkZ, String state, long titleId, long version) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.state = state == null || state.trim().isEmpty() ? "OTHER" : state.trim();
        this.titleId = Math.max(0L, titleId);
        this.version = Math.max(0L, version);
    }

    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public String getState() { return state; }
    public long getTitleId() { return titleId; }
    public long getVersion() { return version; }
}
