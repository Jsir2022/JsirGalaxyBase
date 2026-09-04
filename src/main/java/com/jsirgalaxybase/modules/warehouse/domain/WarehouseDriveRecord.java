package com.jsirgalaxybase.modules.warehouse.domain;

import java.util.Objects;

/** PostgreSQL truth for a physical Drive's ownership, not its AE2 contents. */
public final class WarehouseDriveRecord {

    private final long driveId;
    private final WarehouseDriveKey key;
    private final String ownerPlayerRef;
    private final WarehouseDriveStatus status;
    private final long version;

    public WarehouseDriveRecord(long driveId, WarehouseDriveKey key, String ownerPlayerRef,
        WarehouseDriveStatus status, long version) {
        if (driveId <= 0L || version <= 0L) throw new IllegalArgumentException("drive id and version must be positive");
        if (ownerPlayerRef == null || ownerPlayerRef.trim().isEmpty()) throw new IllegalArgumentException("owner is required");
        this.driveId = driveId;
        this.key = Objects.requireNonNull(key, "key");
        this.ownerPlayerRef = ownerPlayerRef.trim();
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
    }

    public long getDriveId() { return driveId; }
    public WarehouseDriveKey getKey() { return key; }
    public String getOwnerPlayerRef() { return ownerPlayerRef; }
    public WarehouseDriveStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public WarehouseDriveRecord remove() {
        return new WarehouseDriveRecord(driveId, key, ownerPlayerRef, WarehouseDriveStatus.REMOVED, version + 1L);
    }
}
