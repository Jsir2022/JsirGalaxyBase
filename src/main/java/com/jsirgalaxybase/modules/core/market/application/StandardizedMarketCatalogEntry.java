package com.jsirgalaxybase.modules.core.market.application;

import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

public class StandardizedMarketCatalogEntry {

    private final StandardizedMarketProduct product;
    private final String categoryCode;
    private final String admissionBasis;
    private final String sourceEntryLabel;

    public StandardizedMarketCatalogEntry(StandardizedMarketProduct product, String categoryCode,
        String admissionBasis, String sourceEntryLabel) {
        this.product = product;
        this.categoryCode = categoryCode;
        this.admissionBasis = admissionBasis;
        this.sourceEntryLabel = sourceEntryLabel;
    }

    public StandardizedMarketProduct getProduct() {
        return product;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getAdmissionBasis() {
        return admissionBasis;
    }

    public String getSourceEntryLabel() {
        return sourceEntryLabel;
    }
}