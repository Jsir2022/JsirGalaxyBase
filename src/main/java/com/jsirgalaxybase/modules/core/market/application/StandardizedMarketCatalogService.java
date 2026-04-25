package com.jsirgalaxybase.modules.core.market.application;

import net.minecraft.item.ItemStack;

public class StandardizedMarketCatalogService implements StandardizedMarketProductCatalog {

    private final StandardizedMarketCatalogVersion catalogVersion;
    private final StandardizedMarketCatalogSource catalogSource;

    public StandardizedMarketCatalogService(StandardizedMarketCatalogVersion catalogVersion,
        StandardizedMarketCatalogSource catalogSource) {
        this.catalogVersion = requireNonNull(catalogVersion, "catalogVersion");
        this.catalogSource = requireNonNull(catalogSource, "catalogSource");
    }

    @Override
    public StandardizedMarketCatalogVersion getCatalogVersion() {
        return catalogVersion;
    }

    @Override
    public String getCatalogSourceKey() {
        return catalogSource.getSourceKey();
    }

    @Override
    public String getCatalogSourceDescription() {
        return catalogSource.getSourceDescription();
    }

    @Override
    public StandardizedMarketAdmissionDecision evaluateProduct(String productKey) {
        return catalogSource.findEntryByProductKey(productKey)
            .map(new java.util.function.Function<StandardizedMarketCatalogEntry, StandardizedMarketAdmissionDecision>() {

                @Override
                public StandardizedMarketAdmissionDecision apply(StandardizedMarketCatalogEntry entry) {
                    return admittedDecision(entry);
                }
            })
            .orElseGet(new java.util.function.Supplier<StandardizedMarketAdmissionDecision>() {

                @Override
                public StandardizedMarketAdmissionDecision get() {
                    return rejectedDecision("当前商品不在标准商品市场目录 " + catalogVersion.getVersionKey()
                        + " 的准入边界内；当前目录来源为 " + catalogSource.getSourceDescription() + "。",
                        StandardizedMarketAdmissionReason.CATALOG_BOUNDARY_REJECTED);
                }
            });
    }

    @Override
    public StandardizedMarketAdmissionDecision evaluateStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return rejectedDecision("当前手持物品不是可用于标准商品市场目录判定的真实物品堆。",
                StandardizedMarketAdmissionReason.INVALID_STACK);
        }
        return catalogSource.findEntryByStack(stack)
            .map(new java.util.function.Function<StandardizedMarketCatalogEntry, StandardizedMarketAdmissionDecision>() {

                @Override
                public StandardizedMarketAdmissionDecision apply(StandardizedMarketCatalogEntry entry) {
                    return admittedDecision(entry);
                }
            })
            .orElseGet(new java.util.function.Supplier<StandardizedMarketAdmissionDecision>() {

                @Override
                public StandardizedMarketAdmissionDecision get() {
                    return rejectedDecision("当前手持物品不在标准商品市场目录 " + catalogVersion.getVersionKey()
                        + " 的准入边界内；当前目录来源为 " + catalogSource.getSourceDescription() + "。",
                        StandardizedMarketAdmissionReason.CATALOG_BOUNDARY_REJECTED);
                }
            });
    }

    private StandardizedMarketAdmissionDecision admittedDecision(StandardizedMarketCatalogEntry entry) {
        return new StandardizedMarketAdmissionDecision(catalogVersion, entry, true,
            StandardizedMarketAdmissionReason.CATALOG_ADMITTED,
            "商品已按标准商品市场目录 " + catalogVersion.getVersionKey() + " 准入；目录来源为 "
                + catalogSource.getSourceDescription() + "。",
            catalogSource.getSourceKey(), catalogSource.getSourceDescription());
    }

    private StandardizedMarketAdmissionDecision rejectedDecision(String detailMessage,
        StandardizedMarketAdmissionReason reason) {
        return new StandardizedMarketAdmissionDecision(catalogVersion, null, false, reason, detailMessage,
            catalogSource.getSourceKey(), catalogSource.getSourceDescription());
    }

    private <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new MarketOperationException(fieldName + " must not be null");
        }
        return value;
    }
}