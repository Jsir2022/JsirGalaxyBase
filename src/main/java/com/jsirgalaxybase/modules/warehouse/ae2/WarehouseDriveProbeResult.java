package com.jsirgalaxybase.modules.warehouse.ae2;

/** Immutable result used by the Phase 0 probe and deterministic tests. */
public final class WarehouseDriveProbeResult {

    public enum Code {
        SUCCESS,
        NO_CELL,
        INVALID_CELL,
        BAY_OCCUPIED,
        CELL_NOT_EMPTY,
        NO_CHANNEL_OR_POWER,
        PARTIAL
    }

    private final Code code;
    private final long requested;
    private final long accepted;

    private WarehouseDriveProbeResult(Code code, long requested, long accepted) {
        this.code = code;
        this.requested = requested;
        this.accepted = accepted;
    }

    public static WarehouseDriveProbeResult of(Code code) { return new WarehouseDriveProbeResult(code, 0L, 0L); }
    public static WarehouseDriveProbeResult transfer(Code code, long requested, long accepted) {
        return new WarehouseDriveProbeResult(code, requested, accepted);
    }

    public Code getCode() { return code; }
    public long getRequested() { return requested; }
    public long getAccepted() { return accepted; }
    public boolean isSuccess() { return code == Code.SUCCESS; }
}
