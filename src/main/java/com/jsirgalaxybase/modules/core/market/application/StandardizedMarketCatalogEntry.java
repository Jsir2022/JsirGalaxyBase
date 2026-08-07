package com.jsirgalaxybase.modules.core.market.application;

import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

public class StandardizedMarketCatalogEntry {

    private final StandardizedMarketProduct product;
    private final String categoryCode;
    private final String admissionBasis;
    private final String sourceEntryLabel;
    private final String displayName;
    private final String unitLabel;
    private final int sortOrder;
    private final String catalogVersion;
    private final boolean enabled;
    private final long referencePrice;

    public StandardizedMarketCatalogEntry(StandardizedMarketProduct product, String categoryCode,
        String admissionBasis, String sourceEntryLabel) {
        this(product, categoryCode, admissionBasis, sourceEntryLabel, product == null ? "" : product.getProductKey(),
            "标准单位", 0, "compatibility", true, 0L);
    }

    public StandardizedMarketCatalogEntry(StandardizedMarketProduct product, String categoryCode,
        String admissionBasis, String sourceEntryLabel, String displayName, String unitLabel, int sortOrder,
        String catalogVersion) {
        this(product, categoryCode, admissionBasis, sourceEntryLabel, displayName, unitLabel, sortOrder,
            catalogVersion, true, 0L);
    }

    public StandardizedMarketCatalogEntry(StandardizedMarketProduct product, String categoryCode,
        String admissionBasis, String sourceEntryLabel, String displayName, String unitLabel, int sortOrder,
        String catalogVersion, boolean enabled, long referencePrice) {
        this.product = product;
        this.categoryCode = categoryCode;
        this.admissionBasis = admissionBasis;
        this.sourceEntryLabel = sourceEntryLabel;
        this.displayName = displayName;
        this.unitLabel = unitLabel;
        this.sortOrder = sortOrder;
        this.catalogVersion = catalogVersion;
        this.enabled = enabled;
        this.referencePrice = Math.max(0L, referencePrice);
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

    public String getDisplayName() { return displayName; }
    public String getUnitLabel() { return unitLabel; }
    public int getSortOrder() { return sortOrder; }
    public String getCatalogVersion() { return catalogVersion; }
    public boolean isEnabled() { return enabled; }
    public long getReferencePrice() { return referencePrice; }
}
