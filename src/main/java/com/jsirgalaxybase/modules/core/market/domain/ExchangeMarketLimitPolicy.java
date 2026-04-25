package com.jsirgalaxybase.modules.core.market.domain;

public class ExchangeMarketLimitPolicy {

    private final ExchangeMarketLimitStatus status;
    private final String reasonCode;
    private final String note;

    public ExchangeMarketLimitPolicy(ExchangeMarketLimitStatus status, String reasonCode, String note) {
        this.status = status;
        this.reasonCode = reasonCode;
        this.note = note;
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

    public boolean isExecutable() {
        return status != ExchangeMarketLimitStatus.DISALLOWED;
    }
}