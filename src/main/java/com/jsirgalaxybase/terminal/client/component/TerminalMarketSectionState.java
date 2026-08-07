package com.jsirgalaxybase.terminal.client.component;

import java.util.List;

import com.jsirgalaxybase.terminal.TerminalMarketActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

public final class TerminalMarketSectionState {

    public enum FocusField {
        NONE,
        BROWSER_QUERY,
        LIMIT_BUY_PRICE,
        LIMIT_BUY_QUANTITY,
        LIMIT_SELL_PRICE,
        LIMIT_SELL_QUANTITY,
        INSTANT_BUY_QUANTITY,
        INSTANT_SELL_QUANTITY
    }

    public enum StandardizedViewMode { BROWSE, DETAIL }
    public enum OrderSide { BUY, SELL }
    public enum OrderType { MARKET, LIMIT }
    public enum BrowserFilter { ALL, TRADED, BOOK }
    public enum BrowserSort { DIRECTORY, PRICE, GAIN, LOSS, VOLUME }

    private String selectedProductKey = "";
    private String browserQuery = "";
    private int browserPage;
    private int browserGridScrollOffset;
    private String limitBuyPriceText = "";
    private String limitBuyQuantityText = "";
    private String limitSellPriceText = "";
    private String limitSellQuantityText = "";
    private String instantBuyQuantityText = "";
    private String instantSellQuantityText = "";
    private String vaultDepositQuantityText = "";
    private String pendingClaimCustodyId = "";
    private String pendingCancelOrderId = "";
    private FocusField focusedField = FocusField.NONE;
    private StandardizedViewMode standardizedViewMode = StandardizedViewMode.BROWSE;
    private String pendingDetailProductKey = "";
    private OrderSide orderSide = OrderSide.BUY;
    private OrderType orderType = OrderType.MARKET;
    private BrowserFilter browserFilter = BrowserFilter.ALL;
    private BrowserSort browserSort = BrowserSort.DIRECTORY;
    private String selectedChartRange = "24h";
    private final TerminalCustomMarketSectionState customState = new TerminalCustomMarketSectionState();
    private final TerminalExchangeMarketSectionState exchangeState = new TerminalExchangeMarketSectionState();

    public void applyModel(TerminalMarketSectionModel model) {
        if (model == null) {
            selectedProductKey = "";
            browserQuery = "";
            browserPage = 0;
            browserGridScrollOffset = 0;
            limitBuyPriceText = "";
            limitBuyQuantityText = "";
            limitSellPriceText = "";
            limitSellQuantityText = "";
            instantBuyQuantityText = "";
            instantSellQuantityText = "";
            vaultDepositQuantityText = "";
            pendingClaimCustodyId = "";
            pendingCancelOrderId = "";
            focusedField = FocusField.NONE;
            standardizedViewMode = StandardizedViewMode.BROWSE;
            pendingDetailProductKey = "";
            return;
        }
        selectedProductKey = normalize(model.getSelectedProductKey());
        if (!pendingDetailProductKey.isEmpty() && pendingDetailProductKey.equals(selectedProductKey)) {
            standardizedViewMode = StandardizedViewMode.DETAIL;
            pendingDetailProductKey = "";
        }
        browserQuery = normalize(model.getCatalogQuery());
        browserPage = Math.max(0, model.getCatalogPageIndex());
        limitBuyPriceText = sanitizeNumber(model.getLimitBuyDraft().getPriceText());
        limitBuyQuantityText = sanitizeNumber(model.getLimitBuyDraft().getQuantityText());
        limitSellPriceText = sanitizeNumber(model.getLimitSellDraft().getPriceText());
        limitSellQuantityText = sanitizeNumber(model.getLimitSellDraft().getQuantityText());
        instantBuyQuantityText = sanitizeNumber(model.getInstantBuyDraft().getQuantityText());
        instantSellQuantityText = sanitizeNumber(model.getInstantSellDraft().getQuantityText());
        pendingClaimCustodyId = resolvePendingClaimId(model.getClaimIds(), pendingClaimCustodyId);
        pendingCancelOrderId = resolvePendingClaimId(model.getMyOrderIds(), pendingCancelOrderId);
    }

    public TerminalCustomMarketSectionState getCustomState() {
        return customState;
    }

    public TerminalExchangeMarketSectionState getExchangeState() {
        return exchangeState;
    }

    public TerminalMarketActionPayload toPayload() {
        return new TerminalMarketActionPayload(
            selectedProductKey,
            limitBuyPriceText,
            limitBuyQuantityText,
            pendingClaimCustodyId,
            pendingCancelOrderId,
            limitSellPriceText,
            limitSellQuantityText,
            instantBuyQuantityText,
            instantSellQuantityText,
            browserQuery,
            String.valueOf(browserPage),
            browserFilter.name(),
            vaultDepositQuantityText);
    }

    public TerminalMarketActionPayload toUnifiedOrderPayload() {
        String quantity = orderSide == OrderSide.BUY
            ? (orderType == OrderType.MARKET ? instantBuyQuantityText : limitBuyQuantityText)
            : (orderType == OrderType.MARKET ? instantSellQuantityText : limitSellQuantityText);
        String price = orderType == OrderType.LIMIT
            ? (orderSide == OrderSide.BUY ? limitBuyPriceText : limitSellPriceText) : "";
        return toPayload().withOrderTicket(orderSide.name(), orderType.name(), quantity, price, selectedChartRange,
            browserSort.name());
    }

    public String getSelectedChartRange() { return selectedChartRange; }

    public void setSelectedChartRange(String value) {
        selectedChartRange = "1h".equalsIgnoreCase(value) ? "1h" : "7d".equalsIgnoreCase(value) ? "7d" : "24h";
    }

    public TerminalMarketActionPayload toBrowsePayload() {
        return toPayload().withOrderTicket(orderSide.name(), orderType.name(), "", "", selectedChartRange,
            browserSort.name());
    }

    public String getSelectedProductKey() {
        return selectedProductKey;
    }

    public void setSelectedProductKey(String selectedProductKey) {
        this.selectedProductKey = normalize(selectedProductKey);
    }

    public void requestDetailProduct(String productKey) {
        this.pendingDetailProductKey = normalize(productKey);
    }

    public boolean isStandardizedDetailView() { return standardizedViewMode == StandardizedViewMode.DETAIL; }

    public OrderSide getOrderSide() { return orderSide; }
    public void setOrderSide(OrderSide value) { orderSide = value == null ? OrderSide.BUY : value; }
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType value) { orderType = value == null ? OrderType.MARKET : value; }
    public BrowserFilter getBrowserFilter() { return browserFilter; }
    public BrowserSort getBrowserSort() { return browserSort; }
    public void cycleBrowserFilter() {
        BrowserFilter[] values = BrowserFilter.values();
        browserFilter = values[(browserFilter.ordinal() + 1) % values.length];
        browserPage = 0;
        browserGridScrollOffset = 0;
    }
    public void cycleBrowserSort() {
        BrowserSort[] values = BrowserSort.values();
        browserSort = values[(browserSort.ordinal() + 1) % values.length];
        browserPage = 0;
        browserGridScrollOffset = 0;
    }
    public String getBrowserFilterLabel() {
        return browserFilter == BrowserFilter.TRADED ? "有成交" : browserFilter == BrowserFilter.BOOK ? "有盘口" : "全部";
    }
    public String getBrowserSortLabel() {
        switch (browserSort) {
            case PRICE: return "价格";
            case GAIN: return "涨幅";
            case LOSS: return "跌幅";
            case VOLUME: return "成交量";
            default: return "目录";
        }
    }

    public boolean hasCompleteOrderTicket() {
        if (orderSide == OrderSide.BUY) {
            return orderType == OrderType.MARKET ? hasCompleteInstantBuyDraft() : hasCompleteLimitBuyDraft();
        }
        return orderType == OrderType.MARKET ? hasCompleteInstantSellDraft() : hasCompleteLimitSellDraft();
    }

    public void returnToStandardizedBrowse() {
        standardizedViewMode = StandardizedViewMode.BROWSE;
        pendingDetailProductKey = "";
        focusedField = FocusField.NONE;
    }

    public String getBrowserQuery() {
        return browserQuery;
    }

    public void setBrowserQuery(String browserQuery) {
        this.browserQuery = normalize(browserQuery);
        this.browserPage = 0;
    }

    public int getBrowserPage() {
        return browserPage;
    }

    public void setBrowserPage(int browserPage) {
        this.browserPage = Math.max(0, browserPage);
        this.browserGridScrollOffset = 0;
    }

    public int getBrowserGridScrollOffset() { return browserGridScrollOffset; }
    public void setBrowserGridScrollOffset(int offset) { this.browserGridScrollOffset = Math.max(0, offset); }

    public String getLimitBuyPriceText() {
        return limitBuyPriceText;
    }

    public void setLimitBuyPriceText(String limitBuyPriceText) {
        this.limitBuyPriceText = sanitizeNumber(limitBuyPriceText);
    }

    public String getLimitBuyQuantityText() {
        return limitBuyQuantityText;
    }

    public void setLimitBuyQuantityText(String limitBuyQuantityText) {
        this.limitBuyQuantityText = sanitizeNumber(limitBuyQuantityText);
    }

    public String getPendingClaimCustodyId() {
        return pendingClaimCustodyId;
    }

    public void setPendingClaimCustodyId(String pendingClaimCustodyId) {
        this.pendingClaimCustodyId = sanitizeNumber(pendingClaimCustodyId);
    }

    public boolean hasCompleteLimitBuyDraft() {
        return !selectedProductKey.isEmpty() && parseLong(limitBuyPriceText) > 0L && parseLong(limitBuyQuantityText) > 0L;
    }

    public String getLimitSellPriceText() {
        return limitSellPriceText;
    }

    public void setLimitSellPriceText(String limitSellPriceText) {
        this.limitSellPriceText = sanitizeNumber(limitSellPriceText);
    }

    public String getLimitSellQuantityText() {
        return limitSellQuantityText;
    }

    public void setLimitSellQuantityText(String limitSellQuantityText) {
        this.limitSellQuantityText = sanitizeNumber(limitSellQuantityText);
    }

    public String getInstantBuyQuantityText() {
        return instantBuyQuantityText;
    }

    public void setInstantBuyQuantityText(String instantBuyQuantityText) {
        this.instantBuyQuantityText = sanitizeNumber(instantBuyQuantityText);
    }

    public String getInstantSellQuantityText() {
        return instantSellQuantityText;
    }

    public void setInstantSellQuantityText(String instantSellQuantityText) {
        this.instantSellQuantityText = sanitizeNumber(instantSellQuantityText);
    }

    public String getVaultDepositQuantityText() { return vaultDepositQuantityText; }
    public void setVaultDepositQuantityText(String value) { vaultDepositQuantityText = sanitizeNumber(value); }

    public boolean hasCompleteLimitSellDraft() {
        return !selectedProductKey.isEmpty() && parseLong(limitSellPriceText) > 0L && parseLong(limitSellQuantityText) > 0L;
    }

    public boolean hasCompleteInstantBuyDraft() {
        return !selectedProductKey.isEmpty() && parseLong(instantBuyQuantityText) > 0L;
    }

    public boolean hasCompleteInstantSellDraft() {
        return !selectedProductKey.isEmpty() && parseLong(instantSellQuantityText) > 0L;
    }

    public boolean hasPendingClaimSelection() {
        return parseLong(pendingClaimCustodyId) > 0L;
    }

    public String getPendingCancelOrderId() {
        return pendingCancelOrderId;
    }

    public void setPendingCancelOrderId(String pendingCancelOrderId) {
        this.pendingCancelOrderId = sanitizeNumber(pendingCancelOrderId);
    }

    public boolean hasPendingCancelOrderSelection() {
        return parseLong(pendingCancelOrderId) > 0L;
    }

    public long parsePendingClaimCustodyId() {
        return parseLong(pendingClaimCustodyId);
    }

    public void focus(FocusField focusField) {
        this.focusedField = focusField == null ? FocusField.NONE : focusField;
    }

    public boolean isFocused(FocusField focusField) {
        return focusedField == focusField;
    }

    private String resolvePendingClaimId(List<String> claimIds, String currentValue) {
        String current = sanitizeNumber(currentValue);
        if (!current.isEmpty()) {
            for (String claimId : claimIds) {
                if (current.equals(sanitizeNumber(claimId))) {
                    return current;
                }
            }
        }
        if (claimIds != null) {
            for (String claimId : claimIds) {
                String sanitized = sanitizeNumber(claimId);
                if (!sanitized.isEmpty()) {
                    return sanitized;
                }
            }
        }
        return "";
    }

    private static String sanitizeNumber(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (current >= '0' && current <= '9') {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private static long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
