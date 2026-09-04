package com.jsirgalaxybase.modules.warehouse.domain;

/** Auditable result returned only after server-side identity and ownership checks. */
public final class WarehouseDriveReceipt {

    private final String requestId;
    private final String semanticsKey;
    private final WarehouseDriveOperation operation;
    private final WarehouseDriveResult result;
    private final WarehouseDriveKey key;
    private final WarehouseDriveRecord before;
    private final WarehouseDriveRecord after;
    private final String detail;

    public WarehouseDriveReceipt(String requestId, String semanticsKey, WarehouseDriveOperation operation,
        WarehouseDriveResult result, WarehouseDriveRecord before, WarehouseDriveRecord after, String detail) {
        this(requestId, semanticsKey, operation, result,
            before == null ? (after == null ? null : after.getKey()) : before.getKey(), before, after, detail);
    }

    public WarehouseDriveReceipt(String requestId, String semanticsKey, WarehouseDriveOperation operation,
        WarehouseDriveResult result, WarehouseDriveKey key, WarehouseDriveRecord before, WarehouseDriveRecord after,
        String detail) {
        this.requestId = require(requestId, "requestId");
        this.semanticsKey = require(semanticsKey, "semanticsKey");
        this.operation = operation == null ? WarehouseDriveOperation.DENIED : operation;
        this.result = result == null ? WarehouseDriveResult.RUNTIME_UNAVAILABLE : result;
        if (key == null) throw new IllegalArgumentException("key is required");
        this.key = key;
        this.before = before;
        this.after = after;
        this.detail = detail == null ? "" : detail.trim();
    }

    public String getRequestId() { return requestId; }
    public String getSemanticsKey() { return semanticsKey; }
    public WarehouseDriveOperation getOperation() { return operation; }
    public WarehouseDriveResult getResult() { return result; }
    public WarehouseDriveKey getKey() { return key; }
    public WarehouseDriveRecord getBefore() { return before; }
    public WarehouseDriveRecord getAfter() { return after; }
    public String getDetail() { return detail; }
    public boolean isSuccess() { return result == WarehouseDriveResult.SUCCESS; }

    private static String require(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
