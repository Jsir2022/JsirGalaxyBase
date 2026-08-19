package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public final class TerminalMarketSectionSnapshot {

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
    private List<String> myOrderLines;
    private List<String> myOrderIds;
    private List<String> myOrderCancelableFlags;
    private final List<String> claimLines;
    private final List<String> claimIds;
    private final List<String> ruleLines;
    private final boolean depositEnabled;
    private final LimitBuyDraft limitBuyDraft;
    private final LimitSellDraft limitSellDraft;
    private final InstantDraft instantBuyDraft;
    private final InstantDraft instantSellDraft;
    private final ActionFeedback actionFeedback;
    private List<CatalogProduct> catalogProducts;
    private String catalogQuery;
    private int catalogPageIndex;
    private int catalogPageSize;
    private int catalogTotalEntries;
    private boolean catalogHasPreviousPage;
    private boolean catalogHasNextPage;
    private List<VaultAsset> vaultAssets;
    private int historyTotalEntries;
    private int historyPageIndex;
    private int historyPageSize;
    private String accountCenterTab;
    private String centerBankAvailable;
    private String centerFrozenFunds;
    private int centerVaultUsedSlots;
    private int centerVaultTotalSlots;
    private int centerActiveOrders;
    private int centerPendingDeliveries;
    private int centerRecoveryItems;
    private List<String> centerRowKinds;
    private List<String> centerRowIconRefs;
    private List<TerminalMarketAccountCenterRow> accountCenterRows;

    public TerminalMarketSectionSnapshot(String routePageId, String serviceState, String browserHint,
        List<String> productKeys, List<String> productLabels, String selectedProductKey, String selectedProductName,
        String selectedProductUnit, String latestTradePrice, String highestBid, String lowestAsk,
        String bestBidQuantity, String bestAskQuantity, String volume24h, String turnover24h, String sourceAvailable,
        String lockedEscrowQuantity, String claimableQuantity, String frozenFunds, String summaryNotice,
        String sourceMode, String warehouseNotice, String limitBuyPreview, String limitSellPreview,
        String instantBuyPreview, String instantSellPreview, List<String> askLines, List<String> bidLines,
        List<String> myOrderLines, List<String> myOrderIds, List<String> myOrderCancelableFlags,
        List<String> claimLines, List<String> claimIds, List<String> ruleLines, boolean depositEnabled,
        LimitBuyDraft limitBuyDraft, LimitSellDraft limitSellDraft, InstantDraft instantBuyDraft,
        InstantDraft instantSellDraft, ActionFeedback actionFeedback) {
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
        this.limitSellPreview = normalize(limitSellPreview, "填写价格与数量后，将显示账户仓卖出摘要。");
        this.instantBuyPreview = normalize(instantBuyPreview, "填写数量后，将按当前卖盘测深。");
        this.instantSellPreview = normalize(instantSellPreview, "填写数量后，将按当前买盘测深。");
        this.askLines = freeze(askLines, Collections.singletonList("当前没有卖盘深度。"));
        this.bidLines = freeze(bidLines, Collections.singletonList("当前没有买盘深度。"));
        this.myOrderLines = freeze(myOrderLines, Collections.singletonList("当前没有个人订单。"));
        this.myOrderIds = freeze(myOrderIds, Collections.singletonList(""));
        this.myOrderCancelableFlags = freeze(myOrderCancelableFlags, Collections.singletonList("0"));
        this.claimLines = freeze(claimLines, Collections.singletonList("当前没有待收货资产。"));
        this.claimIds = freeze(claimIds, Collections.singletonList(""));
        this.ruleLines = freeze(ruleLines, Collections.singletonList("当前没有规则提示。"));
        this.depositEnabled = depositEnabled;
        this.limitBuyDraft = limitBuyDraft == null ? LimitBuyDraft.placeholder() : limitBuyDraft;
        this.limitSellDraft = limitSellDraft == null ? LimitSellDraft.placeholder() : limitSellDraft;
        this.instantBuyDraft = instantBuyDraft == null ? InstantDraft.placeholder() : instantBuyDraft;
        this.instantSellDraft = instantSellDraft == null ? InstantDraft.placeholder() : instantSellDraft;
        this.actionFeedback = actionFeedback == null ? ActionFeedback.placeholder() : actionFeedback;
        this.catalogProducts = Collections.emptyList();
        this.catalogQuery = "";
        this.catalogPageIndex = 0;
        this.catalogPageSize = 0;
        this.catalogTotalEntries = 0;
        this.catalogHasPreviousPage = false;
        this.catalogHasNextPage = false;
        this.vaultAssets = Collections.emptyList();
        this.historyTotalEntries = 0;
        this.historyPageIndex = 0;
        this.historyPageSize = TerminalMarketActionPayload.DEFAULT_HISTORY_PAGE_SIZE;
        this.accountCenterTab = "OPEN_ORDERS";
        this.centerBankAvailable = "0";
        this.centerFrozenFunds = "0";
        this.centerRowKinds = Collections.emptyList();
        this.centerRowIconRefs = Collections.emptyList();
        this.accountCenterRows = Collections.emptyList();
    }

    public static TerminalMarketSectionSnapshot placeholder(String routePageId) {
        return new TerminalMarketSectionSnapshot(
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
            "填写价格与数量后，将显示账户仓卖出摘要。",
            "填写数量后，将按当前卖盘测深。",
            "填写数量后，将按当前买盘测深。",
            Collections.singletonList("当前没有卖盘深度。"),
            Collections.singletonList("当前没有买盘深度。"),
            Collections.singletonList("当前没有个人订单。"),
            Collections.singletonList(""),
            Collections.singletonList("0"),
            Collections.singletonList("当前没有待收货资产。"),
            Collections.singletonList(""),
            Collections.singletonList("当前没有规则提示。"),
            false,
            LimitBuyDraft.placeholder(),
            LimitSellDraft.placeholder(),
            InstantDraft.placeholder(),
            InstantDraft.placeholder(),
            ActionFeedback.placeholder());
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

    public String getVolume24h() {
        return volume24h;
    }

    public String getBestBidQuantity() {
        return bestBidQuantity;
    }

    public String getBestAskQuantity() {
        return bestAskQuantity;
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

    public LimitBuyDraft getLimitBuyDraft() {
        return limitBuyDraft;
    }

    public LimitSellDraft getLimitSellDraft() {
        return limitSellDraft;
    }

    public InstantDraft getInstantBuyDraft() {
        return instantBuyDraft;
    }

    public InstantDraft getInstantSellDraft() {
        return instantSellDraft;
    }

    public ActionFeedback getActionFeedback() {
        return actionFeedback;
    }

    /**
     * Appends the formal catalogue page without disturbing the legacy string-list contract.
     * The list is immutable once attached and is the canonical input for the workstation browser.
     */
    public TerminalMarketSectionSnapshot withCatalogPage(List<CatalogProduct> products, String query, int pageIndex,
        int pageSize, int totalEntries, boolean hasPreviousPage, boolean hasNextPage) {
        this.catalogProducts = freeze(products, Collections.<CatalogProduct>emptyList());
        this.catalogQuery = normalize(query, "");
        this.catalogPageIndex = Math.max(0, pageIndex);
        this.catalogPageSize = Math.max(0, pageSize);
        this.catalogTotalEntries = Math.max(0, totalEntries);
        this.catalogHasPreviousPage = hasPreviousPage;
        this.catalogHasNextPage = hasNextPage;
        return this;
    }

    public List<CatalogProduct> getCatalogProducts() { return catalogProducts; }
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

    public TerminalMarketSectionSnapshot withVaultAssets(List<VaultAsset> assets) {
        this.vaultAssets = freeze(assets, Collections.<VaultAsset>emptyList());
        return this;
    }

    public List<VaultAsset> getVaultAssets() { return vaultAssets; }

    public TerminalMarketSectionSnapshot withHistoryPage(List<String> lines, List<String> ids,
        List<String> cancelableFlags, int totalEntries, int pageIndex, int pageSize) {
        this.myOrderLines = freezeAllowEmpty(lines);
        this.myOrderIds = freezeAllowEmpty(ids);
        this.myOrderCancelableFlags = freezeAllowEmpty(cancelableFlags);
        this.historyTotalEntries = Math.max(0, totalEntries);
        this.historyPageIndex = Math.max(0, pageIndex);
        this.historyPageSize = Math.max(1, pageSize);
        return this;
    }

    public int getHistoryTotalEntries() { return historyTotalEntries; }
    public int getHistoryPageIndex() { return historyPageIndex; }
    public int getHistoryPageSize() { return historyPageSize; }
    public int getHistoryTotalPages() {
        return Math.max(1, (historyTotalEntries + historyPageSize - 1) / historyPageSize);
    }
    public boolean hasHistoryPreviousPage() { return historyPageIndex > 0; }
    public boolean hasHistoryNextPage() { return historyPageIndex + 1 < getHistoryTotalPages(); }

    public TerminalMarketSectionSnapshot withAccountCenter(String tab, String bankAvailable, String frozenFunds,
        int vaultUsedSlots, int vaultTotalSlots, int activeOrders, int pendingDeliveries, int recoveryItems,
        List<String> rowKinds, List<String> rowIconRefs) {
        accountCenterTab = normalize(tab, "OPEN_ORDERS");
        centerBankAvailable = normalize(bankAvailable, "0");
        centerFrozenFunds = normalize(frozenFunds, "0");
        centerVaultUsedSlots = Math.max(0, vaultUsedSlots); centerVaultTotalSlots = Math.max(0, vaultTotalSlots);
        centerActiveOrders = Math.max(0, activeOrders); centerPendingDeliveries = Math.max(0, pendingDeliveries);
        centerRecoveryItems = Math.max(0, recoveryItems);
        centerRowKinds = freezeAllowEmpty(rowKinds); centerRowIconRefs = freezeAllowEmpty(rowIconRefs);
        return this;
    }
    public String getAccountCenterTab() { return accountCenterTab; }
    public String getCenterBankAvailable() { return centerBankAvailable; }
    public String getCenterFrozenFunds() { return centerFrozenFunds; }
    public int getCenterVaultUsedSlots() { return centerVaultUsedSlots; }
    public int getCenterVaultTotalSlots() { return centerVaultTotalSlots; }
    public int getCenterActiveOrders() { return centerActiveOrders; }
    public int getCenterPendingDeliveries() { return centerPendingDeliveries; }
    public int getCenterRecoveryItems() { return centerRecoveryItems; }
    public List<String> getCenterRowKinds() { return centerRowKinds; }
    public List<String> getCenterRowIconRefs() { return centerRowIconRefs; }
    public TerminalMarketSectionSnapshot withAccountCenterRows(List<TerminalMarketAccountCenterRow> rows) {
        accountCenterRows = Collections.unmodifiableList(new ArrayList<TerminalMarketAccountCenterRow>(
            rows == null ? Collections.<TerminalMarketAccountCenterRow>emptyList() : rows));
        return this;
    }
    public List<TerminalMarketAccountCenterRow> getAccountCenterRows() { return accountCenterRows; }

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

    private static <T> List<T> freezeAllowEmpty(List<T> source) {
        return Collections.unmodifiableList(new ArrayList<T>(
            source == null ? Collections.<T>emptyList() : source));
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    public static final class CatalogProduct {

        private final String productKey;
        private final String registryName;
        private final int meta;
        private final String displayName;
        private final String unitLabel;
        private final int sortOrder;
        private final boolean enabled;
        private final long referencePrice;
        private final String tradability;
        private final CatalogMarketSummary marketSummary;

        public CatalogProduct(String productKey, String registryName, int meta, String displayName, String unitLabel,
            int sortOrder, boolean enabled, long referencePrice, String tradability) {
            this(productKey, registryName, meta, displayName, unitLabel, sortOrder, enabled, referencePrice,
                tradability, CatalogMarketSummary.empty());
        }

        public CatalogProduct(String productKey, String registryName, int meta, String displayName, String unitLabel,
            int sortOrder, boolean enabled, long referencePrice, String tradability, CatalogMarketSummary marketSummary) {
            this.productKey = normalize(productKey, "");
            this.registryName = normalize(registryName, "");
            this.meta = Math.max(0, meta);
            this.displayName = normalize(displayName, this.productKey);
            this.unitLabel = normalize(unitLabel, "标准单位");
            this.sortOrder = sortOrder;
            this.enabled = enabled;
            this.referencePrice = Math.max(0L, referencePrice);
            this.tradability = normalize(tradability, enabled ? "可交易" : "已停用");
            this.marketSummary = marketSummary == null ? CatalogMarketSummary.empty() : marketSummary;
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
        public CatalogMarketSummary getMarketSummary() { return marketSummary; }
    }

    /** A read-only personal Vault slot advertised to market pickers. */
    public static final class VaultAsset {
        private final int slotIndex;
        private final String registryName;
        private final int meta;
        private final String displayName;
        private final int quantity;
        private final String standardizedProductKey;
        private final boolean standardizedEligible;
        private final String standardizedReason;

        public VaultAsset(int slotIndex, String registryName, int meta, String displayName, int quantity,
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

    public static final class CatalogMarketSummary {
        private final String latestTrade;
        private final String bestBid;
        private final String bestAsk;
        private final String volume24h;
        private final String available;
        private final String escrow;
        private final String claimable;
        private final String dayChange;
        private final List<PricePoint> pricePoints;

        public CatalogMarketSummary(String latestTrade, String bestBid, String bestAsk, String volume24h,
            String available, String escrow, String claimable, List<PricePoint> pricePoints) {
            this(latestTrade, bestBid, bestAsk, volume24h, available, escrow, claimable, "--", pricePoints);
        }

        public CatalogMarketSummary(String latestTrade, String bestBid, String bestAsk, String volume24h,
            String available, String escrow, String claimable, String dayChange, List<PricePoint> pricePoints) {
            this.latestTrade = normalize(latestTrade, "--");
            this.bestBid = normalize(bestBid, "--");
            this.bestAsk = normalize(bestAsk, "--");
            this.volume24h = normalize(volume24h, "0");
            this.available = normalize(available, "0");
            this.escrow = normalize(escrow, "0");
            this.claimable = normalize(claimable, "0");
            this.dayChange = normalize(dayChange, "--");
            this.pricePoints = freeze(pricePoints, Collections.<PricePoint>emptyList());
        }

        public static CatalogMarketSummary empty() {
            return new CatalogMarketSummary("--", "--", "--", "0", "0", "0", "0",
                Collections.<PricePoint>emptyList());
        }
        public String getLatestTrade() { return latestTrade; }
        public String getBestBid() { return bestBid; }
        public String getBestAsk() { return bestAsk; }
        public String getVolume24h() { return volume24h; }
        public String getAvailable() { return available; }
        public String getEscrow() { return escrow; }
        public String getClaimable() { return claimable; }
        public String getDayChange() { return dayChange; }
        public List<PricePoint> getPricePoints() { return pricePoints; }
    }

    public static final class PricePoint {
        private final long open;
        private final long high;
        private final long low;
        private final long price;
        private final long quantity;
        private final long turnover;
        private final long createdAtEpochSeconds;
        private final String source;
        public PricePoint(long price, long quantity, long createdAtEpochSeconds) {
            this(price, price, price, price, quantity, price * quantity, createdAtEpochSeconds, "TRADE");
        }
        public PricePoint(long open, long high, long low, long close, long quantity, long turnover,
            long createdAtEpochSeconds) {
            this(open, high, low, close, quantity, turnover, createdAtEpochSeconds, "TRADE");
        }
        public PricePoint(long open, long high, long low, long close, long quantity, long turnover,
            long createdAtEpochSeconds, String source) {
            this.open = Math.max(0L, open);
            this.high = Math.max(this.open, Math.max(high, close));
            this.low = Math.max(0L, Math.min(this.open, Math.min(low, close)));
            this.price = Math.max(0L, close);
            this.quantity = Math.max(0L, quantity);
            this.turnover = Math.max(0L, turnover);
            this.createdAtEpochSeconds = Math.max(0L, createdAtEpochSeconds);
            this.source = normalizeSource(source);
        }
        public long getOpen() { return open; }
        public long getHigh() { return high; }
        public long getLow() { return low; }
        public long getPrice() { return price; }
        public long getQuantity() { return quantity; }
        public long getTurnover() { return turnover; }
        public long getCreatedAtEpochSeconds() { return createdAtEpochSeconds; }
        public String getSource() { return source; }

        private static String normalizeSource(String source) {
            if ("CARRY_FORWARD".equals(source) || "REFERENCE".equals(source) || "EMPTY".equals(source)) {
                return source;
            }
            return "TRADE";
        }
    }

    public static final class LimitBuyDraft {

        private final String selectedProductKey;
        private final String priceText;
        private final String quantityText;
        private final boolean submitEnabled;

        public LimitBuyDraft(String selectedProductKey, String priceText, String quantityText, boolean submitEnabled) {
            this.selectedProductKey = normalize(selectedProductKey, "");
            this.priceText = normalize(priceText, "");
            this.quantityText = normalize(quantityText, "");
            this.submitEnabled = submitEnabled;
        }

        public static LimitBuyDraft placeholder() {
            return new LimitBuyDraft("", "", "", false);
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

    public static final class LimitSellDraft {

        private final String selectedProductKey;
        private final String priceText;
        private final String quantityText;
        private final boolean submitEnabled;

        public LimitSellDraft(String selectedProductKey, String priceText, String quantityText, boolean submitEnabled) {
            this.selectedProductKey = normalize(selectedProductKey, "");
            this.priceText = normalize(priceText, "");
            this.quantityText = normalize(quantityText, "");
            this.submitEnabled = submitEnabled;
        }

        public static LimitSellDraft placeholder() {
            return new LimitSellDraft("", "", "", false);
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

    public static final class InstantDraft {

        private final String selectedProductKey;
        private final String quantityText;
        private final boolean submitEnabled;

        public InstantDraft(String selectedProductKey, String quantityText, boolean submitEnabled) {
            this.selectedProductKey = normalize(selectedProductKey, "");
            this.quantityText = normalize(quantityText, "");
            this.submitEnabled = submitEnabled;
        }

        public static InstantDraft placeholder() {
            return new InstantDraft("", "", false);
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

    public static final class ActionFeedback {

        private final String title;
        private final String body;
        private final String severityName;

        public ActionFeedback(String title, String body, String severityName) {
            this.title = normalize(title, "市场动作反馈");
            this.body = normalize(body, "当前没有市场动作反馈。");
            this.severityName = normalize(severityName, TerminalNotificationSeverity.INFO.name());
        }

        public static ActionFeedback placeholder() {
            return new ActionFeedback("市场动作反馈", "当前没有市场动作反馈。", TerminalNotificationSeverity.INFO.name());
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
    }
}
