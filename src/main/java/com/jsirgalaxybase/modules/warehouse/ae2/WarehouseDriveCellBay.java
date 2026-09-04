package com.jsirgalaxybase.modules.warehouse.ae2;

/**
 * Pure one-bay state machine. The AE2 adapter supplies real Cell capacity and
 * channel/power state; this class makes the safe install/remove decision testable
 * without creating a world or an AE grid.
 */
public final class WarehouseDriveCellBay {

    private boolean installed;
    private long storedItems;

    public WarehouseDriveProbeResult install(boolean cellHandled) {
        if (!cellHandled) return WarehouseDriveProbeResult.of(WarehouseDriveProbeResult.Code.INVALID_CELL);
        if (installed) return WarehouseDriveProbeResult.of(WarehouseDriveProbeResult.Code.BAY_OCCUPIED);
        installed = true;
        storedItems = 0L;
        return WarehouseDriveProbeResult.of(WarehouseDriveProbeResult.Code.SUCCESS);
    }

    public WarehouseDriveProbeResult remove() {
        if (!installed) return WarehouseDriveProbeResult.of(WarehouseDriveProbeResult.Code.NO_CELL);
        if (storedItems > 0L) return WarehouseDriveProbeResult.of(WarehouseDriveProbeResult.Code.CELL_NOT_EMPTY);
        installed = false;
        return WarehouseDriveProbeResult.of(WarehouseDriveProbeResult.Code.SUCCESS);
    }

    public WarehouseDriveProbeResult evaluateTransfer(boolean active, long requested, long accepted) {
        if (!installed) return WarehouseDriveProbeResult.of(WarehouseDriveProbeResult.Code.NO_CELL);
        if (!active) return WarehouseDriveProbeResult.of(WarehouseDriveProbeResult.Code.NO_CHANNEL_OR_POWER);
        long normalizedRequested = Math.max(0L, requested);
        long normalizedAccepted = Math.max(0L, Math.min(normalizedRequested, accepted));
        return WarehouseDriveProbeResult.transfer(normalizedAccepted == normalizedRequested
            ? WarehouseDriveProbeResult.Code.SUCCESS : WarehouseDriveProbeResult.Code.PARTIAL,
            normalizedRequested, normalizedAccepted);
    }

    public void updateStoredItems(long count) { storedItems = Math.max(0L, count); }
    public boolean isInstalled() { return installed; }
    public long getStoredItems() { return storedItems; }
}
