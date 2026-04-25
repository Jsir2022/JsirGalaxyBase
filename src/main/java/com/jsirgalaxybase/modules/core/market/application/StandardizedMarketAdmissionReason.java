package com.jsirgalaxybase.modules.core.market.application;

public enum StandardizedMarketAdmissionReason {

    CATALOG_ADMITTED("CATALOG_ADMITTED"),
    CATALOG_BOUNDARY_REJECTED("CATALOG_BOUNDARY_REJECTED"),
    INVALID_STACK("INVALID_STACK");

    private final String code;

    StandardizedMarketAdmissionReason(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}