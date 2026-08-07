package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TerminalMarketActionPayload {

    private final String selectedProductKey;
    private final String limitBuyPriceText;
    private final String limitBuyQuantityText;
    private final String custodyIdText;
    private final String orderIdText;
    private final String limitSellPriceText;
    private final String limitSellQuantityText;
    private final String instantBuyQuantityText;
    private final String instantSellQuantityText;
    private final String browserQuery;
    private final String browserPageText;
    private final String browserFilter;
    private final String vaultDepositQuantityText;
    private final String orderSide;
    private final String orderType;
    private final String orderQuantityText;
    private final String orderLimitPriceText;
    private final String chartRange;
    private final String browserSort;

    public TerminalMarketActionPayload(String selectedProductKey, String limitBuyPriceText, String limitBuyQuantityText,
        String custodyIdText, String orderIdText, String limitSellPriceText, String limitSellQuantityText,
        String instantBuyQuantityText, String instantSellQuantityText) {
        this.selectedProductKey = normalize(selectedProductKey);
        this.limitBuyPriceText = normalize(limitBuyPriceText);
        this.limitBuyQuantityText = normalize(limitBuyQuantityText);
        this.custodyIdText = normalize(custodyIdText);
        this.orderIdText = normalize(orderIdText);
        this.limitSellPriceText = normalize(limitSellPriceText);
        this.limitSellQuantityText = normalize(limitSellQuantityText);
        this.instantBuyQuantityText = normalize(instantBuyQuantityText);
        this.instantSellQuantityText = normalize(instantSellQuantityText);
        this.browserQuery = "";
        this.browserPageText = "0";
        this.browserFilter = "";
        this.vaultDepositQuantityText = "";
        this.orderSide = "";
        this.orderType = "";
        this.orderQuantityText = "";
        this.orderLimitPriceText = "";
        this.chartRange = "24h";
        this.browserSort = "";
    }

    public TerminalMarketActionPayload(String selectedProductKey, String limitBuyPriceText, String limitBuyQuantityText,
        String custodyIdText, String orderIdText, String limitSellPriceText, String limitSellQuantityText,
        String instantBuyQuantityText, String instantSellQuantityText, String browserQuery, String browserPageText,
        String browserFilter) {
        this.selectedProductKey = normalize(selectedProductKey);
        this.limitBuyPriceText = normalize(limitBuyPriceText);
        this.limitBuyQuantityText = normalize(limitBuyQuantityText);
        this.custodyIdText = normalize(custodyIdText);
        this.orderIdText = normalize(orderIdText);
        this.limitSellPriceText = normalize(limitSellPriceText);
        this.limitSellQuantityText = normalize(limitSellQuantityText);
        this.instantBuyQuantityText = normalize(instantBuyQuantityText);
        this.instantSellQuantityText = normalize(instantSellQuantityText);
        this.browserQuery = normalize(browserQuery);
        this.browserPageText = normalize(browserPageText);
        this.browserFilter = normalize(browserFilter);
        this.vaultDepositQuantityText = "";
        this.orderSide = "";
        this.orderType = "";
        this.orderQuantityText = "";
        this.orderLimitPriceText = "";
        this.chartRange = "24h";
        this.browserSort = "";
    }

    public TerminalMarketActionPayload(String selectedProductKey, String limitBuyPriceText, String limitBuyQuantityText,
        String custodyIdText, String orderIdText, String limitSellPriceText, String limitSellQuantityText,
        String instantBuyQuantityText, String instantSellQuantityText, String browserQuery, String browserPageText,
        String browserFilter, String vaultDepositQuantityText) {
        this.selectedProductKey = normalize(selectedProductKey);
        this.limitBuyPriceText = normalize(limitBuyPriceText);
        this.limitBuyQuantityText = normalize(limitBuyQuantityText);
        this.custodyIdText = normalize(custodyIdText);
        this.orderIdText = normalize(orderIdText);
        this.limitSellPriceText = normalize(limitSellPriceText);
        this.limitSellQuantityText = normalize(limitSellQuantityText);
        this.instantBuyQuantityText = normalize(instantBuyQuantityText);
        this.instantSellQuantityText = normalize(instantSellQuantityText);
        this.browserQuery = normalize(browserQuery);
        this.browserPageText = normalize(browserPageText);
        this.browserFilter = normalize(browserFilter);
        this.vaultDepositQuantityText = normalize(vaultDepositQuantityText);
        this.orderSide = "";
        this.orderType = "";
        this.orderQuantityText = "";
        this.orderLimitPriceText = "";
        this.chartRange = "24h";
        this.browserSort = "";
    }

    private TerminalMarketActionPayload(TerminalMarketActionPayload base, String orderSide, String orderType,
        String orderQuantityText, String orderLimitPriceText, String chartRange, String browserSort) {
        this.selectedProductKey = base.selectedProductKey;
        this.limitBuyPriceText = base.limitBuyPriceText;
        this.limitBuyQuantityText = base.limitBuyQuantityText;
        this.custodyIdText = base.custodyIdText;
        this.orderIdText = base.orderIdText;
        this.limitSellPriceText = base.limitSellPriceText;
        this.limitSellQuantityText = base.limitSellQuantityText;
        this.instantBuyQuantityText = base.instantBuyQuantityText;
        this.instantSellQuantityText = base.instantSellQuantityText;
        this.browserQuery = base.browserQuery;
        this.browserPageText = base.browserPageText;
        this.browserFilter = base.browserFilter;
        this.vaultDepositQuantityText = base.vaultDepositQuantityText;
        this.orderSide = normalize(orderSide);
        this.orderType = normalize(orderType);
        this.orderQuantityText = normalize(orderQuantityText);
        this.orderLimitPriceText = normalize(orderLimitPriceText);
        this.chartRange = normalize(chartRange).isEmpty() ? "24h" : normalize(chartRange);
        this.browserSort = normalize(browserSort);
    }

    public static TerminalMarketActionPayload empty() {
        return new TerminalMarketActionPayload("", "", "", "", "", "", "", "", "");
    }

    public static TerminalMarketActionPayload decode(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return empty();
        }
        String[] parts = payload.split("\\|", -1);
        if (parts.length == 4) {
            return new TerminalMarketActionPayload(
                decodePart(parts[0]),
                decodePart(parts[1]),
                decodePart(parts[2]),
                decodePart(parts[3]),
                "",
                "",
                "",
                "",
                "");
        }
        if (parts.length != 9 && parts.length != 12 && parts.length != 13 && parts.length != 19) {
            return empty();
        }
        TerminalMarketActionPayload decoded = new TerminalMarketActionPayload(
            decodePart(parts[0]),
            decodePart(parts[1]),
            decodePart(parts[2]),
            decodePart(parts[3]),
            decodePart(parts[4]),
            decodePart(parts[5]),
            decodePart(parts[6]),
            decodePart(parts[7]),
            decodePart(parts[8]),
            parts.length >= 12 ? decodePart(parts[9]) : "",
            parts.length >= 12 ? decodePart(parts[10]) : "0",
            parts.length >= 12 ? decodePart(parts[11]) : "",
            parts.length >= 13 ? decodePart(parts[12]) : "");
        return parts.length == 19 ? new TerminalMarketActionPayload(decoded,
            decodePart(parts[13]), decodePart(parts[14]), decodePart(parts[15]), decodePart(parts[16]),
            decodePart(parts[17]), decodePart(parts[18])) : decoded;
    }

    public String encode() {
        return encodePart(selectedProductKey) + "|"
            + encodePart(limitBuyPriceText) + "|"
            + encodePart(limitBuyQuantityText) + "|"
            + encodePart(custodyIdText) + "|"
            + encodePart(orderIdText) + "|"
            + encodePart(limitSellPriceText) + "|"
            + encodePart(limitSellQuantityText) + "|"
            + encodePart(instantBuyQuantityText) + "|"
            + encodePart(instantSellQuantityText) + "|"
            + encodePart(browserQuery) + "|"
            + encodePart(browserPageText) + "|"
            + encodePart(browserFilter) + "|"
            + encodePart(vaultDepositQuantityText);
    }

    /** Extended encoding used only by the unified order ticket. Legacy actions retain the 13-field form. */
    public String encodeUnifiedOrder() {
        return encode() + "|" + encodePart(orderSide) + "|" + encodePart(orderType) + "|"
            + encodePart(orderQuantityText) + "|" + encodePart(orderLimitPriceText) + "|"
            + encodePart(chartRange) + "|" + encodePart(browserSort);
    }

    public TerminalMarketActionPayload withOrderTicket(String side, String type, String quantityText,
        String limitPriceText, String selectedChartRange, String selectedBrowserSort) {
        return new TerminalMarketActionPayload(this, side, type, quantityText, limitPriceText, selectedChartRange,
            selectedBrowserSort);
    }

    public String getSelectedProductKey() {
        return selectedProductKey;
    }

    public String getPriceText() {
        return limitBuyPriceText;
    }

    public String getQuantityText() {
        return limitBuyQuantityText;
    }

    public String getCustodyIdText() {
        return custodyIdText;
    }

    public String getOrderIdText() {
        return orderIdText;
    }

    public String getLimitBuyPriceText() {
        return limitBuyPriceText;
    }

    public String getLimitBuyQuantityText() {
        return limitBuyQuantityText;
    }

    public String getLimitSellPriceText() {
        return limitSellPriceText;
    }

    public String getLimitSellQuantityText() {
        return limitSellQuantityText;
    }

    public String getInstantBuyQuantityText() {
        return instantBuyQuantityText;
    }

    public String getInstantSellQuantityText() {
        return instantSellQuantityText;
    }

    public String getBrowserQuery() { return browserQuery; }
    public String getBrowserFilter() { return browserFilter; }
    public int getBrowserPage() { return (int) Math.max(0L, parseLong(browserPageText)); }
    public String getVaultDepositQuantityText() { return vaultDepositQuantityText; }
    public long parseVaultDepositQuantity() { return parseLong(vaultDepositQuantityText); }
    public String getOrderSide() { return orderSide; }
    public String getOrderType() { return orderType; }
    public long parseOrderQuantity() { return parseLong(orderQuantityText); }
    public long parseOrderLimitPrice() { return parseLong(orderLimitPriceText); }
    public String getChartRange() { return chartRange; }
    public String getBrowserSort() { return browserSort; }
    public boolean hasUnifiedOrderTicket() {
        return ("BUY".equals(orderSide) || "SELL".equals(orderSide))
            && ("MARKET".equals(orderType) || "LIMIT".equals(orderType))
            && parseOrderQuantity() > 0L
            && (!"LIMIT".equals(orderType) || parseOrderLimitPrice() > 0L);
    }

    public long parsePrice() {
        return parseLong(limitBuyPriceText);
    }

    public long parseQuantity() {
        return parseLong(limitBuyQuantityText);
    }

    public long parseCustodyId() {
        return parseLong(custodyIdText);
    }

    public long parseOrderId() {
        return parseLong(orderIdText);
    }

    public long parseLimitSellPrice() {
        return parseLong(limitSellPriceText);
    }

    public long parseLimitSellQuantity() {
        return parseLong(limitSellQuantityText);
    }

    public long parseInstantBuyQuantity() {
        return parseLong(instantBuyQuantityText);
    }

    public long parseInstantSellQuantity() {
        return parseLong(instantSellQuantityText);
    }

    public TerminalMarketActionPayload clearedAfterLimitBuySuccess() {
        return copy(limitBuyPriceText, "", custodyIdText, orderIdText, limitSellPriceText, limitSellQuantityText,
            instantBuyQuantityText, instantSellQuantityText);
    }

    public TerminalMarketActionPayload clearedAfterLimitSellSuccess() {
        return copy(limitBuyPriceText, limitBuyQuantityText, custodyIdText, orderIdText, limitSellPriceText, "",
            instantBuyQuantityText, instantSellQuantityText);
    }

    public TerminalMarketActionPayload clearedAfterInstantBuySuccess() {
        return copy(limitBuyPriceText, limitBuyQuantityText, custodyIdText, orderIdText, limitSellPriceText,
            limitSellQuantityText, "", instantSellQuantityText);
    }

    public TerminalMarketActionPayload clearedAfterInstantSellSuccess() {
        return copy(limitBuyPriceText, limitBuyQuantityText, custodyIdText, orderIdText, limitSellPriceText,
            limitSellQuantityText, instantBuyQuantityText, "");
    }

    public TerminalMarketActionPayload clearedAfterClaimSuccess() {
        return copy(limitBuyPriceText, limitBuyQuantityText, "", orderIdText, limitSellPriceText, limitSellQuantityText,
            instantBuyQuantityText, instantSellQuantityText);
    }

    public TerminalMarketActionPayload clearedAfterCancelSuccess() {
        return copy(limitBuyPriceText, limitBuyQuantityText, custodyIdText, "", limitSellPriceText, limitSellQuantityText,
            instantBuyQuantityText, instantSellQuantityText);
    }

    private TerminalMarketActionPayload copy(String limitBuyPrice, String limitBuyQuantity, String custodyId,
        String orderId, String limitSellPrice, String limitSellQuantity, String instantBuyQuantity,
        String instantSellQuantity) {
        TerminalMarketActionPayload copied = new TerminalMarketActionPayload(selectedProductKey, limitBuyPrice, limitBuyQuantity, custodyId, orderId,
            limitSellPrice, limitSellQuantity, instantBuyQuantity, instantSellQuantity, browserQuery, browserPageText,
            browserFilter, vaultDepositQuantityText);
        return new TerminalMarketActionPayload(copied, orderSide, orderType, orderQuantityText, orderLimitPriceText,
            chartRange, browserSort);
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

    private static String encodePart(String value) {
        return Base64.getUrlEncoder().encodeToString(normalize(value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decodePart(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(normalize(value)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
