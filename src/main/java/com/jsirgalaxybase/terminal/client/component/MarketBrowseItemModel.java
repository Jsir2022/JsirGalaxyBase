package com.jsirgalaxybase.terminal.client.component;

import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

/** Shared browse vocabulary for the standard, custom and exchange market adapters. */
public final class MarketBrowseItemModel {

    public enum Kind { STANDARDIZED, CUSTOM_LISTING, EXCHANGE_COIN }

    private final String key;
    private final String iconRef;
    private final String title;
    private final String referencePrice;
    private final String status;
    private final String latestTrade;
    private final String bestBid;
    private final String bestAsk;
    private final String volume24h;
    private final String available;
    private final String escrow;
    private final String claimable;
    private final String dayChange;
    private final List<TerminalMarketSectionModel.PricePointModel> pricePoints;
    private final Kind kind;
    private final String tooltipPrimary;
    private final String tooltipSecondary;

    public MarketBrowseItemModel(String key, String iconRef, String title, String referencePrice, String status,
        String latestTrade, String bestBid, String bestAsk, String volume24h,
        List<TerminalMarketSectionModel.PricePointModel> pricePoints) {
        this(key, iconRef, title, referencePrice, status, latestTrade, bestBid, bestAsk, volume24h, "0", "0", "0",
            "--", pricePoints);
    }

    public MarketBrowseItemModel(String key, String iconRef, String title, String referencePrice, String status,
        String latestTrade, String bestBid, String bestAsk, String volume24h, String available, String escrow,
        String claimable, List<TerminalMarketSectionModel.PricePointModel> pricePoints) {
        this(key, iconRef, title, referencePrice, status, latestTrade, bestBid, bestAsk, volume24h, available, escrow,
            claimable, "--", pricePoints);
    }

    public MarketBrowseItemModel(String key, String iconRef, String title, String referencePrice, String status,
        String latestTrade, String bestBid, String bestAsk, String volume24h, String available, String escrow,
        String claimable, String dayChange, List<TerminalMarketSectionModel.PricePointModel> pricePoints) {
        this(key, iconRef, title, referencePrice, status, latestTrade, bestBid, bestAsk, volume24h, available,
            escrow, claimable, dayChange, pricePoints, Kind.STANDARDIZED, "", "");
    }

    private MarketBrowseItemModel(String key, String iconRef, String title, String referencePrice, String status,
        String latestTrade, String bestBid, String bestAsk, String volume24h, String available, String escrow,
        String claimable, String dayChange, List<TerminalMarketSectionModel.PricePointModel> pricePoints, Kind kind,
        String tooltipPrimary, String tooltipSecondary) {
        this.key = safe(key); this.iconRef = safe(iconRef); this.title = safe(title);
        this.referencePrice = safe(referencePrice); this.status = safe(status); this.latestTrade = safe(latestTrade);
        this.bestBid = safe(bestBid); this.bestAsk = safe(bestAsk); this.volume24h = safe(volume24h);
        this.available = safeCount(available); this.escrow = safeCount(escrow); this.claimable = safeCount(claimable);
        this.dayChange = safe(dayChange);
        this.pricePoints = pricePoints == null ? Collections.<TerminalMarketSectionModel.PricePointModel>emptyList()
            : Collections.unmodifiableList(pricePoints);
        this.kind = kind == null ? Kind.STANDARDIZED : kind;
        this.tooltipPrimary = safe(tooltipPrimary);
        this.tooltipSecondary = safe(tooltipSecondary);
    }

    public static MarketBrowseItemModel customListing(String key, String iconRef, String title, String price,
        String ownerOrDelivery, String status) {
        return new MarketBrowseItemModel(key, iconRef, title, price, status, ownerOrDelivery, "--", "--", "--",
            "0", "0", "0", "--", Collections.<TerminalMarketSectionModel.PricePointModel>emptyList(),
            Kind.CUSTOM_LISTING, ownerOrDelivery, status);
    }

    public static MarketBrowseItemModel exchangeCoin(String key, String iconRef, String title, String faceValue,
        String familyTier, String availability) {
        return new MarketBrowseItemModel(key, iconRef, title, faceValue, availability, familyTier, "--", "--", "--",
            "0", "0", "0", "--", Collections.<TerminalMarketSectionModel.PricePointModel>emptyList(),
            Kind.EXCHANGE_COIN, familyTier, availability);
    }

    public String getKey() { return key; }
    public String getIconRef() { return iconRef; }
    public String getTitle() { return title; }
    public String getReferencePrice() { return referencePrice; }
    public String getStatus() { return status; }
    public String getLatestTrade() { return latestTrade; }
    public String getBestBid() { return bestBid; }
    public String getBestAsk() { return bestAsk; }
    public String getVolume24h() { return volume24h; }
    public String getAvailable() { return available; }
    public String getEscrow() { return escrow; }
    public String getClaimable() { return claimable; }
    public String getDayChange() { return dayChange; }
    public List<TerminalMarketSectionModel.PricePointModel> getPricePoints() { return pricePoints; }
    public Kind getKind() { return kind; }
    public String getTooltipPrimary() { return tooltipPrimary; }
    public String getTooltipSecondary() { return tooltipSecondary; }
    public boolean isStandardized() { return kind == Kind.STANDARDIZED; }
    public String getCardStatus() { return isStandardized() ? (hasDayChange() ? dayChange : status) : status; }

    public double getChangePercent() {
        if (pricePoints.isEmpty()) { return 0.0D; }
        long first = pricePoints.get(0).getPrice();
        long last = pricePoints.get(pricePoints.size() - 1).getPrice();
        return first <= 0L ? 0.0D : (last - first) * 100.0D / first;
    }

    public boolean hasIntradayTrade() { return !pricePoints.isEmpty(); }

    public boolean hasDayChange() { return !"--".equals(dayChange); }

    public long getDayOpenPrice() { return pricePoints.isEmpty() ? 0L : pricePoints.get(0).getPrice(); }

    public String getCompactLatestPrice() {
        return compactPrice("--".equals(latestTrade) ? referencePrice : latestTrade);
    }

    public String getCompactReferencePrice() { return compactPrice(referencePrice); }

    public String getCompactBestBid() { return compactPrice(bestBid); }

    public String getCompactBestAsk() { return compactPrice(bestAsk); }

    public String getLiquidityLabel() {
        boolean bid = !"--".equals(bestBid);
        boolean ask = !"--".equals(bestAsk);
        return bid && ask ? "双边" : bid || ask ? "单边" : "无盘口";
    }

    private static String safe(String value) { return value == null || value.trim().isEmpty() ? "--" : value.trim(); }
    private static String safeCount(String value) { return value == null || value.trim().isEmpty() ? "0" : value.trim(); }
    private static String compactPrice(String value) {
        if (value == null) { return "--"; }
        return value.replace(" / STARCOIN", "").replace(" STARCOIN", "").trim();
    }
}
