package com.jsirgalaxybase.terminal.client.viewmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public final class TerminalMarketSectionModel {

    private final String routePageId;
    private final String serviceState;
    private final String browserHint;
    private final List<String> productKeys;
    private final List<String> productLabels;
    private final String selectedProductKey;
    private final String selectedProductName;
    private final String selectedProductUnit;
    private final String latestTradePrice;
    private final String highestBid;
    private final String lowestAsk;
    private final String bestBidQuantity;
    private final String bestAskQuantity;
    private final String volume24h;
    private final String turnover24h;
    private final String sourceAvailable;
    private final String lockedEscrowQuantity;
    private final String claimableQuantity;
    private final String frozenFunds;
    private final String summaryNotice;
    private final String sourceMode;
    private final String warehouseNotice;
    private final String limitBuyPreview;
    private final String limitSellPreview;
    private final String instantBuyPreview;
    private final String instantSellPreview;
    private final List<String> askLines;
    private final List<String> bidLines;
    private final List<String> myOrderLines;
    private final List<String> myOrderIds;
    private final List<String> myOrderCancelableFlags;
    private final List<String> claimLines;
    private final List<String> claimIds;
    private final List<String> ruleLines;
    private final boolean depositEnabled;
    private final LimitBuyDraftModel limitBuyDraft;
    private final LimitSellDraftModel limitSellDraft;
    private final InstantDraftModel instantBuyDraft;
    private final InstantDraftModel instantSellDraft;
    private final ActionFeedbackModel actionFeedback;
    private List<CatalogProductModel> catalogProducts;
    private String catalogQuery;
    private int catalogPageIndex;
    private int catalogPageSize;
    private int catalogTotalEntries;
    private boolean catalogHasPreviousPage;
    private boolean catalogHasNextPage;
    private List<VaultAssetModel> vaultAssets;

    public TerminalMarketSectionModel(String routePageId, String serviceState, String browserHint,
        List<String> productKeys, List<String> productLabels, String selectedProductKey, String selectedProductName,
        String selectedProductUnit, String latestTradePrice, String highestBid, String lowestAsk,
        String bestBidQuantity, String bestAskQuantity, String volume24h, String turnover24h, String sourceAvailable,
        String lockedEscrowQuantity, String claimableQuantity, String frozenFunds, String summaryNotice,
        String sourceMode, String warehouseNotice, String limitBuyPreview, String limitSellPreview,
        String instantBuyPreview, String instantSellPreview, List<String> askLines, List<String> bidLines,
        List<String> myOrderLines, List<String> myOrderIds, List<String> myOrderCancelableFlags,
        List<String> claimLines, List<String> claimIds, List<String> ruleLines, boolean depositEnabled,
        LimitBuyDraftModel limitBuyDraft, LimitSellDraftModel limitSellDraft, InstantDraftModel instantBuyDraft,
        InstantDraftModel instantSellDraft, ActionFeedbackModel actionFeedback) {
        this.routePageId = TerminalPage.fromId(normalize(routePageId, TerminalPage.MARKET.getId())).getId();
        this.serviceState = normalize(serviceState, "市场服务状态未知");
        this.browserHint = normalize(browserHint, "当前没有市场浏览提示。");
        this.productKeys = freeze(productKeys, Collections.<String>emptyList());
        this.productLabels = freeze(productLabels, Collections.<String>emptyList());
        this.selectedProductKey = normalize(selectedProductKey, "");
        this.selectedProductName = normalize(selectedProductName, "未选中商品");
        this.selectedProductUnit = normalize(selectedProductUnit, "标准化单位");
        this.latestTradePrice = normalize(latestTradePrice, "--");
        this.highestBid = normalize(highestBid, "--");
        this.lowestAsk = normalize(lowestAsk, "--");
        this.bestBidQuantity = normalize(bestBidQuantity, "0");
        this.bestAskQuantity = normalize(bestAskQuantity, "0");
        this.volume24h = normalize(volume24h, "0");
        this.turnover24h = normalize(turnover24h, "0 STARCOIN");
        this.sourceAvailable = normalize(sourceAvailable, "0");
        this.lockedEscrowQuantity = normalize(lockedEscrowQuantity, "0");
        this.claimableQuantity = normalize(claimableQuantity, "0");
        this.frozenFunds = normalize(frozenFunds, "0 STARCOIN");
        this.summaryNotice = normalize(summaryNotice, "当前没有市场摘要说明。");
        this.sourceMode = normalize(sourceMode, "当前没有仓储来源说明。");
        this.warehouseNotice = normalize(warehouseNotice, "当前没有仓储提示。");
        this.limitBuyPreview = normalize(limitBuyPreview, "填写价格与数量后，将显示冻结资金摘要。");
        this.limitSellPreview = normalize(limitSellPreview, "填写价格与数量后，将显示 AVAILABLE 仓储卖出摘要。");
        this.instantBuyPreview = normalize(instantBuyPreview, "填写数量后，将按当前卖盘测深。");
        this.instantSellPreview = normalize(instantSellPreview, "填写数量后，将按当前买盘测深。");
        this.askLines = freeze(askLines, Collections.singletonList("当前没有卖盘深度。"));
        this.bidLines = freeze(bidLines, Collections.singletonList("当前没有买盘深度。"));
        this.myOrderLines = freeze(myOrderLines, Collections.singletonList("当前没有个人订单。"));
        this.myOrderIds = freeze(myOrderIds, Collections.singletonList(""));
        this.myOrderCancelableFlags = freeze(myOrderCancelableFlags, Collections.singletonList("0"));
        this.claimLines = freeze(claimLines, Collections.singletonList("当前没有待提取的 CLAIMABLE 资产。"));
        this.claimIds = freeze(claimIds, Collections.singletonList(""));
        this.ruleLines = freeze(ruleLines, Collections.singletonList("当前没有规则提示。"));
        this.depositEnabled = depositEnabled;
        this.limitBuyDraft = limitBuyDraft == null ? LimitBuyDraftModel.placeholder() : limitBuyDraft;
        this.limitSellDraft = limitSellDraft == null ? LimitSellDraftModel.placeholder() : limitSellDraft;
        this.instantBuyDraft = instantBuyDraft == null ? InstantDraftModel.placeholder() : instantBuyDraft;
        this.instantSellDraft = instantSellDraft == null ? InstantDraftModel.placeholder() : instantSellDraft;
        this.actionFeedback = actionFeedback == null ? ActionFeedbackModel.placeholder() : actionFeedback;
        this.catalogProducts = Collections.emptyList();
        this.catalogQuery = "";
        this.catalogPageIndex = 0;
        this.catalogPageSize = 0;
        this.catalogTotalEntries = 0;
        this.catalogHasPreviousPage = false;
        this.catalogHasNextPage = false;
        this.vaultAssets = Collections.emptyList();
    }

    public static TerminalMarketSectionModel placeholder(String routePageId) {
        return new TerminalMarketSectionModel(
            routePageId,
            "市场 section 宿主已接入",
            "当前没有市场运行态。",
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            "",
            "未选中商品",
            "标准化单位",
            "--",
            "--",
            "--",
            "0",
            "0",
            "0",
            "0 STARCOIN",
            "0",
            "0",
            "0",
            "0 STARCOIN",
            "phase 6 会在这里承接市场总入口和标准商品市场。",
            "当前没有仓储来源说明。",
            "当前没有仓储提示。",
            "填写价格与数量后，将显示冻结资金摘要。",
            "填写价格与数量后，将显示 AVAILABLE 仓储卖出摘要。",
            "填写数量后，将按当前卖盘测深。",
            "填写数量后，将按当前买盘测深。",
            Collections.singletonList("当前没有卖盘深度。"),
            Collections.singletonList("当前没有买盘深度。"),
            Collections.singletonList("当前没有个人订单。"),
            Collections.singletonList(""),
            Collections.singletonList("0"),
            Collections.singletonList("当前没有待提取的 CLAIMABLE 资产。"),
            Collections.singletonList(""),
            Collections.singletonList("当前没有规则提示。"),
            false,
            LimitBuyDraftModel.placeholder(),
            LimitSellDraftModel.placeholder(),
            InstantDraftModel.placeholder(),
            InstantDraftModel.placeholder(),
            ActionFeedbackModel.placeholder());
    }

    public String getRoutePageId() {
        return routePageId;
    }

    public String getServiceState() {
        return serviceState;
    }

    public String getBrowserHint() {
        return browserHint;
    }

    public List<String> getProductKeys() {
        return productKeys;
    }

    public List<String> getProductLabels() {
        return productLabels;
    }

    public String getSelectedProductKey() {
        return selectedProductKey;
    }

    public String getSelectedProductName() {
        return selectedProductName;
    }

    public String getSelectedProductUnit() {
        return selectedProductUnit;
    }

    public String getLatestTradePrice() {
        return latestTradePrice;
    }

    public String getHighestBid() {
        return highestBid;
    }

    public String getLowestAsk() {
        return lowestAsk;
    }

    public String getBestBidQuantity() {
        return bestBidQuantity;
    }

    public String getBestAskQuantity() {
        return bestAskQuantity;
    }

    public String getVolume24h() {
        return volume24h;
    }

    public String getTurnover24h() {
        return turnover24h;
    }

    public String getSourceAvailable() {
        return sourceAvailable;
    }

    public String getLockedEscrowQuantity() {
        return lockedEscrowQuantity;
    }

    public String getClaimableQuantity() {
        return claimableQuantity;
    }

    public String getFrozenFunds() {
        return frozenFunds;
    }

    public String getSummaryNotice() {
        return summaryNotice;
    }

    public String getSourceMode() {
        return sourceMode;
    }

    public String getWarehouseNotice() {
        return warehouseNotice;
    }

    public TerminalMarketSectionModel withVaultAssets(List<VaultAssetModel> assets) {
        this.vaultAssets = freeze(assets, Collections.<VaultAssetModel>emptyList());
        return this;
    }

    public List<VaultAssetModel> getVaultAssets() { return vaultAssets; }

    public String getLimitBuyPreview() {
        return limitBuyPreview;
    }

    public String getLimitSellPreview() {
        return limitSellPreview;
    }

    public String getInstantBuyPreview() {
        return instantBuyPreview;
    }

    public String getInstantSellPreview() {
        return instantSellPreview;
    }

    public List<String> getAskLines() {
        return askLines;
    }

    public List<String> getBidLines() {
        return bidLines;
    }

    public List<String> getMyOrderLines() {
        return myOrderLines;
    }

    public List<String> getMyOrderIds() {
        return myOrderIds;
    }

    public List<String> getMyOrderCancelableFlags() {
        return myOrderCancelableFlags;
    }

    public List<String> getClaimLines() {
        return claimLines;
    }

    public List<String> getClaimIds() {
        return claimIds;
    }

    public List<String> getRuleLines() {
        return ruleLines;
    }

    public boolean isDepositEnabled() {
        return depositEnabled;
    }

    public LimitBuyDraftModel getLimitBuyDraft() {
        return limitBuyDraft;
    }

    public LimitSellDraftModel getLimitSellDraft() {
        return limitSellDraft;
    }

    public InstantDraftModel getInstantBuyDraft() {
        return instantBuyDraft;
    }

    public InstantDraftModel getInstantSellDraft() {
        return instantSellDraft;
    }

    public ActionFeedbackModel getActionFeedback() {
        return actionFeedback;
    }

    public TerminalMarketSectionModel withCatalogPage(List<CatalogProductModel> products, String query, int pageIndex,
        int pageSize, int totalEntries, boolean hasPreviousPage, boolean hasNextPage) {
        this.catalogProducts = freeze(products, Collections.<CatalogProductModel>emptyList());
        this.catalogQuery = normalize(query, "");
        this.catalogPageIndex = Math.max(0, pageIndex);
        this.catalogPageSize = Math.max(0, pageSize);
        this.catalogTotalEntries = Math.max(0, totalEntries);
        this.catalogHasPreviousPage = hasPreviousPage;
        this.catalogHasNextPage = hasNextPage;
        return this;
    }

    public List<CatalogProductModel> getCatalogProducts() { return catalogProducts; }
    public String getCatalogQuery() { return catalogQuery; }
    public int getCatalogPageIndex() { return catalogPageIndex; }
    public int getCatalogPageSize() { return catalogPageSize; }
    public int getCatalogTotalEntries() { return catalogTotalEntries; }
    public int getCatalogTotalPages() {
        return catalogPageSize <= 0 || catalogTotalEntries <= 0 ? 0
            : (catalogTotalEntries + catalogPageSize - 1) / catalogPageSize;
    }
    public boolean hasCatalogPreviousPage() { return catalogHasPreviousPage; }
    public boolean hasCatalogNextPage() { return catalogHasNextPage; }

    public boolean isOverviewRoute() {
        return TerminalPage.MARKET.getId().equalsIgnoreCase(routePageId);
    }

    public boolean isStandardizedRoute() {
        return TerminalPage.MARKET_STANDARDIZED.getId().equalsIgnoreCase(routePageId);
    }

    private static <T> List<T> freeze(List<T> source, List<T> fallback) {
        List<T> resolved = source == null || source.isEmpty() ? fallback : source;
        return Collections.unmodifiableList(new ArrayList<T>(resolved));
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    public static final class CatalogProductModel {

        private final String productKey;
        private final String registryName;
        private final int meta;
        private final String displayName;
        private final String unitLabel;
        private final int sortOrder;
        private final boolean enabled;
        private final long referencePrice;
        private final String tradability;
        private final CatalogMarketSummaryModel marketSummary;

        public CatalogProductModel(String productKey, String registryName, int meta, String displayName,
            String unitLabel, int sortOrder, boolean enabled, long referencePrice, String tradability) {
            this(productKey, registryName, meta, displayName, unitLabel, sortOrder, enabled, referencePrice,
                tradability, CatalogMarketSummaryModel.empty());
        }

        public CatalogProductModel(String productKey, String registryName, int meta, String displayName,
            String unitLabel, int sortOrder, boolean enabled, long referencePrice, String tradability,
            CatalogMarketSummaryModel marketSummary) {
            this.productKey = normalize(productKey, "");
            this.registryName = normalize(registryName, "");
            this.meta = Math.max(0, meta);
            this.displayName = normalize(displayName, this.productKey);
            this.unitLabel = normalize(unitLabel, "标准单位");
            this.sortOrder = sortOrder;
            this.enabled = enabled;
            this.referencePrice = Math.max(0L, referencePrice);
            this.tradability = normalize(tradability, enabled ? "可交易" : "已停用");
            this.marketSummary = marketSummary == null ? CatalogMarketSummaryModel.empty() : marketSummary;
        }

        public String getProductKey() { return productKey; }
        public String getRegistryName() { return registryName; }
        public int getMeta() { return meta; }
        public String getDisplayName() { return displayName; }
        public String getUnitLabel() { return unitLabel; }
        public int getSortOrder() { return sortOrder; }
        public boolean isEnabled() { return enabled; }
        public long getReferencePrice() { return referencePrice; }
        public String getTradability() { return tradability; }
        public CatalogMarketSummaryModel getMarketSummary() { return marketSummary; }
    }

    public static final class CatalogMarketSummaryModel {
        private final String latestTrade;
        private final String bestBid;
        private final String bestAsk;
        private final String volume24h;
        private final String available;
        private final String escrow;
        private final String claimable;
        private final String dayChange;
        private final List<PricePointModel> pricePoints;
        public CatalogMarketSummaryModel(String latestTrade, String bestBid, String bestAsk, String volume24h,
            String available, String escrow, String claimable, List<PricePointModel> pricePoints) {
            this(latestTrade, bestBid, bestAsk, volume24h, available, escrow, claimable, "--", pricePoints);
        }
        public CatalogMarketSummaryModel(String latestTrade, String bestBid, String bestAsk, String volume24h,
            String available, String escrow, String claimable, String dayChange,
            List<PricePointModel> pricePoints) {
            this.latestTrade = normalize(latestTrade, "--"); this.bestBid = normalize(bestBid, "--");
            this.bestAsk = normalize(bestAsk, "--"); this.volume24h = normalize(volume24h, "0");
            this.available = normalize(available, "0"); this.escrow = normalize(escrow, "0");
            this.claimable = normalize(claimable, "0");
            this.dayChange = normalize(dayChange, "--");
            this.pricePoints = freeze(pricePoints, Collections.<PricePointModel>emptyList());
        }
        public static CatalogMarketSummaryModel empty() { return new CatalogMarketSummaryModel("--", "--", "--", "0", "0", "0", "0", Collections.<PricePointModel>emptyList()); }
        public String getLatestTrade() { return latestTrade; } public String getBestBid() { return bestBid; }
        public String getBestAsk() { return bestAsk; } public String getVolume24h() { return volume24h; }
        public String getAvailable() { return available; } public String getEscrow() { return escrow; }
        public String getClaimable() { return claimable; } public List<PricePointModel> getPricePoints() { return pricePoints; }
        public String getDayChange() { return dayChange; }
    }

    public static final class PricePointModel {
        private final long price; private final long quantity; private final long createdAtEpochSeconds;
        public PricePointModel(long price, long quantity, long createdAtEpochSeconds) { this.price = Math.max(0L, price); this.quantity = Math.max(0L, quantity); this.createdAtEpochSeconds = Math.max(0L, createdAtEpochSeconds); }
        public long getPrice() { return price; } public long getQuantity() { return quantity; }
        public long getCreatedAtEpochSeconds() { return createdAtEpochSeconds; }
    }

    public static final class LimitBuyDraftModel {

        private final String selectedProductKey;
        private final String priceText;
        private final String quantityText;
        private final boolean submitEnabled;

        public LimitBuyDraftModel(String selectedProductKey, String priceText, String quantityText,
            boolean submitEnabled) {
            this.selectedProductKey = normalize(selectedProductKey, "");
            this.priceText = normalize(priceText, "");
            this.quantityText = normalize(quantityText, "");
            this.submitEnabled = submitEnabled;
        }

        public static LimitBuyDraftModel placeholder() {
            return new LimitBuyDraftModel("", "", "", false);
        }

        public String getSelectedProductKey() {
            return selectedProductKey;
        }

        public String getPriceText() {
            return priceText;
        }

        public String getQuantityText() {
            return quantityText;
        }

        public boolean isSubmitEnabled() {
            return submitEnabled;
        }
    }

    public static final class ActionFeedbackModel {

        private final String title;
        private final String body;
        private final String severityName;

        public ActionFeedbackModel(String title, String body, String severityName) {
            this.title = normalize(title, "市场动作反馈");
            this.body = normalize(body, "当前没有市场动作反馈。");
            this.severityName = normalize(severityName, TerminalNotificationSeverity.INFO.name());
        }

        public static ActionFeedbackModel placeholder() {
            return new ActionFeedbackModel("市场动作反馈", "当前没有市场动作反馈。", TerminalNotificationSeverity.INFO.name());
        }

        public String getTitle() {
            return title;
        }

        public String getBody() {
            return body;
        }

        public String getSeverityName() {
            return severityName;
        }

        public TerminalNotificationSeverity getSeverity() {
            return TerminalNotificationSeverity.fromName(severityName);
        }
    }

    public static final class LimitSellDraftModel {

        private final String selectedProductKey;
        private final String priceText;
        private final String quantityText;
        private final boolean submitEnabled;

        public LimitSellDraftModel(String selectedProductKey, String priceText, String quantityText,
            boolean submitEnabled) {
            this.selectedProductKey = normalize(selectedProductKey, "");
            this.priceText = normalize(priceText, "");
            this.quantityText = normalize(quantityText, "");
            this.submitEnabled = submitEnabled;
        }

        public static LimitSellDraftModel placeholder() {
            return new LimitSellDraftModel("", "", "", false);
        }

        public String getSelectedProductKey() {
            return selectedProductKey;
        }

        public String getPriceText() {
            return priceText;
        }

        public String getQuantityText() {
            return quantityText;
        }

        public boolean isSubmitEnabled() {
            return submitEnabled;
        }
    }

    public static final class InstantDraftModel {

        private final String selectedProductKey;
        private final String quantityText;
        private final boolean submitEnabled;

        public InstantDraftModel(String selectedProductKey, String quantityText, boolean submitEnabled) {
            this.selectedProductKey = normalize(selectedProductKey, "");
            this.quantityText = normalize(quantityText, "");
            this.submitEnabled = submitEnabled;
        }

        public static InstantDraftModel placeholder() {
            return new InstantDraftModel("", "", false);
        }

        public String getSelectedProductKey() {
            return selectedProductKey;
        }

        public String getQuantityText() {
            return quantityText;
        }

        public boolean isSubmitEnabled() {
            return submitEnabled;
        }
    }

    public static final class VaultAssetModel {
        private final int slotIndex;
        private final String registryName;
        private final int meta;
        private final String displayName;
        private final int quantity;
        private final String standardizedProductKey;
        private final boolean standardizedEligible;
        private final String standardizedReason;

        public VaultAssetModel(int slotIndex, String registryName, int meta, String displayName, int quantity,
            String standardizedProductKey, boolean standardizedEligible, String standardizedReason) {
            this.slotIndex = Math.max(0, slotIndex);
            this.registryName = normalize(registryName, "");
            this.meta = Math.max(0, meta);
            this.displayName = normalize(displayName, this.registryName);
            this.quantity = Math.max(0, quantity);
            this.standardizedProductKey = normalize(standardizedProductKey, "");
            this.standardizedEligible = standardizedEligible;
            this.standardizedReason = normalize(standardizedReason,
                standardizedEligible ? "可存入标准市场" : "未准入标准市场目录");
        }
        public int getSlotIndex() { return slotIndex; }
        public String getRegistryName() { return registryName; }
        public int getMeta() { return meta; }
        public String getDisplayName() { return displayName; }
        public int getQuantity() { return quantity; }
        public String getStandardizedProductKey() { return standardizedProductKey; }
        public boolean isStandardizedEligible() { return standardizedEligible; }
        public String getStandardizedReason() { return standardizedReason; }
    }
}
