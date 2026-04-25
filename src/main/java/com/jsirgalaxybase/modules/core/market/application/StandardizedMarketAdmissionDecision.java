package com.jsirgalaxybase.modules.core.market.application;

import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

public class StandardizedMarketAdmissionDecision {

    private final StandardizedMarketCatalogVersion catalogVersion;
    private final StandardizedMarketCatalogEntry catalogEntry;
    private final boolean admitted;
    private final StandardizedMarketAdmissionReason reason;
    private final String detailMessage;
    private final String sourceKey;
    private final String sourceDescription;

    public StandardizedMarketAdmissionDecision(StandardizedMarketCatalogVersion catalogVersion,
        StandardizedMarketCatalogEntry catalogEntry, boolean admitted, StandardizedMarketAdmissionReason reason,
        String detailMessage, String sourceKey, String sourceDescription) {
        this.catalogVersion = catalogVersion;
        this.catalogEntry = catalogEntry;
        this.admitted = admitted;
        this.reason = reason;
        this.detailMessage = detailMessage;
        this.sourceKey = sourceKey;
        this.sourceDescription = sourceDescription;
    }

    public StandardizedMarketCatalogVersion getCatalogVersion() {
        return catalogVersion;
    }

    public StandardizedMarketCatalogEntry getCatalogEntry() {
        return catalogEntry;
    }

    public boolean isAdmitted() {
        return admitted;
    }

    public StandardizedMarketAdmissionReason getReason() {
        return reason;
    }

    public String getReasonCode() {
        return reason.getCode();
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public StandardizedMarketProduct requireProduct() {
        if (!admitted || catalogEntry == null || catalogEntry.getProduct() == null) {
            throw new MarketOperationException(detailMessage);
        }
        return catalogEntry.getProduct();
    }

    public String toSummaryLine() {
        return "catalogVersion=" + catalogVersion.getVersionKey() + ", admission=" + getReasonCode() + ", source="
            + sourceKey;
    }
}