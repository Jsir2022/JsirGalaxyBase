package com.jsirgalaxybase.modules.warehouse.domain;

/** Idempotent, server-audited result for one terminal Bay mutation. */
public final class TerminalWarehouseBayReceipt {
    private final String requestId;
    private final String semanticsKey;
    private final TerminalWarehouseBayResult result;
    private final TerminalWarehouseBay before;
    private final TerminalWarehouseBay after;
    private final String detail;

    public TerminalWarehouseBayReceipt(String requestId, String semanticsKey, TerminalWarehouseBayResult result,
        TerminalWarehouseBay before, TerminalWarehouseBay after, String detail) {
        this.requestId = required(requestId, "requestId");
        this.semanticsKey = required(semanticsKey, "semanticsKey");
        this.result = result == null ? TerminalWarehouseBayResult.RUNTIME_UNAVAILABLE : result;
        this.before = before;
        this.after = after;
        this.detail = detail == null ? "" : detail.trim();
    }

    public String getRequestId() { return requestId; }
    public String getSemanticsKey() { return semanticsKey; }
    public TerminalWarehouseBayResult getResult() { return result; }
    public TerminalWarehouseBay getBefore() { return before; }
    public TerminalWarehouseBay getAfter() { return after; }
    public String getDetail() { return detail; }
    public boolean isSuccess() { return result == TerminalWarehouseBayResult.SUCCESS; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
