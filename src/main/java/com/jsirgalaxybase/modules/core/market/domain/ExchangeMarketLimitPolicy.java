package com.jsirgalaxybase.modules.core.market.domain;

public class ExchangeMarketLimitPolicy {

    private final ExchangeMarketLimitStatus status;
    private final String reasonCode;
    private final String note;
    private final long maximumInputQuantity;

    public ExchangeMarketLimitPolicy(ExchangeMarketLimitStatus status, String reasonCode, String note) {
        this(status, reasonCode, note, 0L);
    }

    public ExchangeMarketLimitPolicy(ExchangeMarketLimitStatus status, String reasonCode, String note,
        long maximumInputQuantity) {
        this.status = status;
        this.reasonCode = reasonCode;
        this.note = note;
        this.maximumInputQuantity = maximumInputQuantity;
    }

    public ExchangeMarketLimitStatus getStatus() {
        return status;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getNote() {
        return note;
    }

    /** A positive value is a server-enforced per-request input cap. */
    public long getMaximumInputQuantity() {
        return maximumInputQuantity;
    }

    public boolean isExecutable() {
        return status != ExchangeMarketLimitStatus.DISALLOWED;
    }
}
