package com.jsirgalaxybase.modules.core.market.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One authenticated player's structured order and asset center snapshot. */
public final class MarketAccountCenterSnapshot {
    private final MarketAccountCenterQuery.Tab tab;
    private final AccountSummary summary;
    private final PageMetadata page;
    private final List<OrderRow> openOrders;
    private final List<FillRow> fills;
    private final List<DeliveryRow> deliveries;
    private final List<HistoryRow> history;

    public MarketAccountCenterSnapshot(MarketAccountCenterQuery.Tab tab, AccountSummary summary, PageMetadata page,
        List<OrderRow> openOrders, List<FillRow> fills, List<DeliveryRow> deliveries, List<HistoryRow> history) {
        this.tab = tab == null ? MarketAccountCenterQuery.Tab.OPEN_ORDERS : tab;
        this.summary = summary == null ? AccountSummary.empty() : summary;
        this.page = page == null ? new PageMetadata(0, 0, 4) : page;
        this.openOrders = freeze(openOrders); this.fills = freeze(fills);
        this.deliveries = freeze(deliveries); this.history = freeze(history);
    }
    public MarketAccountCenterQuery.Tab getTab() { return tab; }
    public AccountSummary getSummary() { return summary; }
    public PageMetadata getPage() { return page; }
    public List<OrderRow> getOpenOrders() { return openOrders; }
    public List<FillRow> getFills() { return fills; }
    public List<DeliveryRow> getDeliveries() { return deliveries; }
    public List<HistoryRow> getHistory() { return history; }

    private static <T> List<T> freeze(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<T>(values == null ? Collections.<T>emptyList() : values));
    }

    public static final class PageMetadata {
        private final int totalEntries, pageIndex, pageSize;
        public PageMetadata(int totalEntries, int pageIndex, int pageSize) {
            this.totalEntries = Math.max(0, totalEntries); this.pageIndex = Math.max(0, pageIndex);
            this.pageSize = Math.max(1, pageSize);
        }
        public int getTotalEntries() { return totalEntries; } public int getPageIndex() { return pageIndex; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return Math.max(1, (totalEntries + pageSize - 1) / pageSize); }
    }

    public static final class AccountSummary {
        private final long availableFunds, frozenFunds;
        private final int vaultUsedSlots, vaultTotalSlots, activeOrders, pendingDeliveries, recoveryItems;
        public AccountSummary(long availableFunds, long frozenFunds, int vaultUsedSlots, int vaultTotalSlots,
            int activeOrders, int pendingDeliveries, int recoveryItems) {
            this.availableFunds = Math.max(0L, availableFunds); this.frozenFunds = Math.max(0L, frozenFunds);
            this.vaultUsedSlots = Math.max(0, vaultUsedSlots); this.vaultTotalSlots = Math.max(0, vaultTotalSlots);
            this.activeOrders = Math.max(0, activeOrders); this.pendingDeliveries = Math.max(0, pendingDeliveries);
            this.recoveryItems = Math.max(0, recoveryItems);
        }
        static AccountSummary empty() { return new AccountSummary(0L, 0L, 0, 0, 0, 0, 0); }
        public long getAvailableFunds() { return availableFunds; } public long getFrozenFunds() { return frozenFunds; }
        public int getVaultUsedSlots() { return vaultUsedSlots; } public int getVaultTotalSlots() { return vaultTotalSlots; }
        public int getActiveOrders() { return activeOrders; } public int getPendingDeliveries() { return pendingDeliveries; }
        public int getRecoveryItems() { return recoveryItems; }
    }

    public static final class OrderRow {
        private final MarketOrder order;
        public OrderRow(MarketOrder order) { this.order = order; }
        public MarketOrder getOrder() { return order; }
    }
    public static final class FillRow {
        private final MarketTradeRecord trade; private final MarketOrderSide side; private final long orderId;
        public FillRow(MarketTradeRecord trade, MarketOrderSide side, long orderId) {
            this.trade = trade; this.side = side; this.orderId = Math.max(0L, orderId);
        }
        public MarketTradeRecord getTrade() { return trade; } public MarketOrderSide getSide() { return side; }
        public long getOrderId() { return orderId; }
    }
    public static final class DeliveryRow {
        private final String recordId, kind, status, message; private final StandardizedMarketProduct product;
        private final long quantity, relatedOrderId; private final Instant updatedAt;
        public DeliveryRow(String recordId, String kind, String status, String message,
            StandardizedMarketProduct product, long quantity, long relatedOrderId, Instant updatedAt) {
            this.recordId = safe(recordId); this.kind = safe(kind); this.status = safe(status);
            this.message = safe(message); this.product = product; this.quantity = Math.max(0L, quantity);
            this.relatedOrderId = Math.max(0L, relatedOrderId); this.updatedAt = updatedAt;
        }
        public String getRecordId() { return recordId; } public String getKind() { return kind; }
        public String getStatus() { return status; } public String getMessage() { return message; }
        public StandardizedMarketProduct getProduct() { return product; } public long getQuantity() { return quantity; }
        public long getRelatedOrderId() { return relatedOrderId; } public Instant getUpdatedAt() { return updatedAt; }
    }
    public static final class HistoryRow {
        private final MarketOrder order;
        public HistoryRow(MarketOrder order) { this.order = order; }
        public MarketOrder getOrder() { return order; }
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
