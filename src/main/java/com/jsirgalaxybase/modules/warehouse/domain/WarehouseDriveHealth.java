package com.jsirgalaxybase.modules.warehouse.domain;

/** Read-only runtime summary. No ME item list is included. */
public final class WarehouseDriveHealth {

    private final WarehouseDriveRecord record;
    private final boolean chunkLoaded;
    private final boolean blockPresent;
    private final boolean cellInstalled;
    private final boolean powered;
    private final long storedItems;
    private final long storedTypes;

    public WarehouseDriveHealth(WarehouseDriveRecord record, boolean chunkLoaded, boolean blockPresent,
        boolean cellInstalled, boolean powered, long storedItems, long storedTypes) {
        this.record = record;
        this.chunkLoaded = chunkLoaded;
        this.blockPresent = blockPresent;
        this.cellInstalled = cellInstalled;
        this.powered = powered;
        this.storedItems = Math.max(0L, storedItems);
        this.storedTypes = Math.max(0L, storedTypes);
    }

    public WarehouseDriveRecord getRecord() { return record; }
    public boolean isChunkLoaded() { return chunkLoaded; }
    public boolean isBlockPresent() { return blockPresent; }
    public boolean isCellInstalled() { return cellInstalled; }
    public boolean isPowered() { return powered; }
    public long getStoredItems() { return storedItems; }
    public long getStoredTypes() { return storedTypes; }
}
