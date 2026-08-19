package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.client.TerminalNumberFormat;
import com.jsirgalaxybase.terminal.TerminalMarketAccountCenterRow;

final class TerminalMarketSectionContent {

    private TerminalMarketSectionContent() {}

    static List<String> buildOverviewSummaryLines(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.singletonList("当前没有共享运行态。");
        }
        List<String> lines = new ArrayList<String>();
        lines.add("服务: " + model.getServiceState() + " | 活跃商品: " + countActiveProducts(model));
        lines.add("待收货: " + model.getClaimableQuantity() + " | 买单冻结资金: " + model.getFrozenFunds());
        return lines;
    }

    static OverviewEntrySummary buildStandardizedOverviewEntry(TerminalMarketSectionModel model) {
        String secondary = model == null
            ? "当前没有标准市场状态。"
            : "活跃 " + countActiveProducts(model) + " | 待收货 " + model.getClaimableQuantity();
        return new OverviewEntrySummary(
            "标准商品市场",
            "目录商品、订单簿、仓储与即时成交",
            secondary,
            "进入标准市场");
    }

    static OverviewEntrySummary buildCustomOverviewEntry() {
        return new OverviewEntrySummary(
            "定制商品市场",
            "单件挂牌、浏览、购买与待交付处理",
            "适合非标准化物品与玩家间撮合",
            "进入定制市场");
    }

    static OverviewEntrySummary buildExchangeOverviewEntry() {
        return new OverviewEntrySummary(
            "汇率市场",
            "正式报价、规则校验与兑换确认",
            "适合规则化兑换与目标报价刷新",
            "进入汇率市场");
    }

    static OverviewEntrySummary buildOverviewHelpEntry() {
        return new OverviewEntrySummary(
            "交易规则",
            "标准商品先入仓，定制商品按玩家交付。",
            "汇率市场按规则报价并二次确认。",
            "查看说明");
    }

    static List<ProductEntry> buildProductEntries(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.emptyList();
        }
        if (!model.getCatalogProducts().isEmpty()) {
            List<ProductEntry> results = new ArrayList<ProductEntry>(model.getCatalogProducts().size());
            for (TerminalMarketSectionModel.CatalogProductModel product : model.getCatalogProducts()) {
                if (product == null || !product.isEnabled()) {
                    continue;
                }
                results.add(new ProductEntry(product, product.getProductKey().equals(model.getSelectedProductKey())));
            }
            return results;
        }
        int count = Math.max(model.getProductKeys().size(), model.getProductLabels().size());
        List<ProductEntry> results = new ArrayList<ProductEntry>(count);
        for (int index = 0; index < count; index++) {
            String key = resolveListValue(model.getProductKeys(), index);
            String label = resolveListValue(model.getProductLabels(), index);
            if (key.isEmpty() && label.isEmpty()) {
                continue;
            }
            results.add(new ProductEntry(key, label.isEmpty() ? key : label, key.equals(model.getSelectedProductKey())));
        }
        return results;
    }

    static boolean hasProductCatalog(TerminalMarketSectionModel model) {
        return !buildProductEntries(model).isEmpty();
    }

    static boolean hasSelectedProduct(TerminalMarketSectionModel model) {
        return model != null && !normalize(model.getSelectedProductKey()).isEmpty();
    }

    static String buildProductCatalogEmptyTitle(TerminalMarketSectionModel model) {
        if (model == null) {
            return "市场数据尚未加载";
        }
        if (normalize(model.getServiceState()).contains("不可用")) {
            return "市场服务不可用";
        }
        return "暂无可浏览标准商品";
    }

    static String buildProductCatalogEmptyReason(TerminalMarketSectionModel model) {
        if (model == null) {
            return "正在读取标准商品目录，请稍候。";
        }
        String hint = normalize(model.getBrowserHint());
        if (!hint.isEmpty()) {
            return hint;
        }
        return "当前查询下没有正式准入的标准商品。";
    }

    static String buildProductSelectionReason(TerminalMarketSectionModel model) {
        if (!hasProductCatalog(model)) {
            return buildProductCatalogEmptyReason(model);
        }
        if (!hasSelectedProduct(model)) {
            return "先从左侧商品浏览器选择一个商品。";
        }
        return "";
    }

    static List<String> buildMetricsLines(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.singletonList("当前没有交易焦点摘要。");
        }
        List<String> lines = new ArrayList<String>();
        lines.add("商品: " + model.getSelectedProductName());
        lines.add("单位: " + model.getSelectedProductUnit());
        return lines;
    }

    static List<String> buildMarketSnapshotLines(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.singletonList("当前没有交易焦点摘要。");
        }
        List<String> lines = new ArrayList<String>();
        lines.add("最新成交价: " + model.getLatestTradePrice());
        lines.add("买一 / 卖一: " + model.getHighestBid() + " x " + model.getBestBidQuantity() + " / "
            + model.getLowestAsk() + " x " + model.getBestAskQuantity());
        lines.add("24h 成交量 / 成交额: " + model.getVolume24h() + " / " + model.getTurnover24h());
        return lines;
    }

    static List<String> buildInventoryStatusLines(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.singletonList("当前没有仓储状态。");
        }
        List<String> lines = new ArrayList<String>();
        lines.add("可售 / 卖单锁定 / 待收货: " + model.getSourceAvailable() + " / "
            + model.getLockedEscrowQuantity() + " / " + model.getClaimableQuantity());
        lines.add("冻结资金: " + model.getFrozenFunds());
        lines.add("来源目录: " + model.getSourceMode());
        lines.add("仓储提示: " + model.getWarehouseNotice());
        return lines;
    }

    static List<String> buildBrowserStatusLines(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.singletonList("当前没有目录状态。");
        }
        List<String> lines = new ArrayList<String>();
        lines.add("服务: " + compactServiceState(model.getServiceState()));
        lines.add("目录: " + compactSourceMode(model.getSourceMode()));
        lines.add("个人仓: " + compactWarehouseNotice(model.getWarehouseNotice()));
        return lines;
    }

    static List<String> buildBookLines(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.singletonList("当前没有盘口与个人订单信息。");
        }
        List<String> lines = new ArrayList<String>();
        int depth = Math.max(model.getAskLines().size(), model.getBidLines().size());
        for (int index = 0; index < depth; index++) {
            String askLine = resolveListValue(model.getAskLines(), index);
            String bidLine = resolveListValue(model.getBidLines(), index);
            lines.add("卖" + (index + 1) + ": " + (askLine.isEmpty() ? "--" : askLine)
                + " | 买" + (index + 1) + ": " + (bidLine.isEmpty() ? "--" : bidLine));
        }
        for (String myOrderLine : model.getMyOrderLines()) {
            String normalized = normalize(myOrderLine);
            if (!normalized.isEmpty()) {
                lines.add("我的订单: " + normalized);
            }
        }
        if (lines.isEmpty()) {
            lines.add("当前没有盘口与个人订单信息。");
        }
        return lines;
    }

    static List<ClaimEntry> buildClaimEntries(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.emptyList();
        }
        int count = Math.max(model.getClaimIds().size(), model.getClaimLines().size());
        List<ClaimEntry> entries = new ArrayList<ClaimEntry>(count);
        for (int index = 0; index < count; index++) {
            String custodyId = resolveListValue(model.getClaimIds(), index);
            String detail = resolveListValue(model.getClaimLines(), index);
            if (custodyId.isEmpty() && detail.isEmpty()) {
                continue;
            }
            entries.add(new ClaimEntry(custodyId, detail.isEmpty() ? "待提取资产" : detail));
        }
        return entries;
    }

    static List<OrderEntry> buildOrderEntries(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.emptyList();
        }
        int count = Math.max(Math.max(model.getMyOrderIds().size(), model.getMyOrderLines().size()),
            model.getMyOrderCancelableFlags().size());
        List<OrderEntry> entries = new ArrayList<OrderEntry>(count);
        for (int index = 0; index < count; index++) {
            String orderId = resolveListValue(model.getMyOrderIds(), index);
            String detail = resolveListValue(model.getMyOrderLines(), index);
            String cancelableFlag = resolveListValue(model.getMyOrderCancelableFlags(), index);
            if (orderId.isEmpty() && detail.isEmpty()) {
                continue;
            }
            entries.add(new OrderEntry(orderId, detail.isEmpty() ? "订单详情" : detail,
                "1".equals(cancelableFlag) || "true".equalsIgnoreCase(cancelableFlag)));
        }
        return entries;
    }

    static List<OrderEntry> buildAccountCenterEntries(TerminalMarketSectionModel model) {
        if (model == null) return Collections.emptyList();
        List<OrderEntry> entries = new ArrayList<OrderEntry>(model.getAccountCenterRows().size());
        for (TerminalMarketAccountCenterRow row : model.getAccountCenterRows()) entries.add(new OrderEntry(row));
        return entries;
    }

    static List<String> buildRuleLines(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.singletonList("当前没有规则提示。");
        }
        List<String> lines = new ArrayList<String>();
        for (String ruleLine : model.getRuleLines()) {
            String normalized = normalize(ruleLine);
            if (!normalized.isEmpty()) {
                lines.add(normalized);
            }
        }
        if (lines.isEmpty()) {
            lines.add("当前没有规则提示。");
        }
        return lines;
    }

    static String depositActionHint(TerminalMarketSectionModel model) {
        if (model == null) {
            return "市场快照未加载，不能存入。";
        }
        if (model.isDepositEnabled()) {
            return "个人账户仓库存不足，无法继续卖出。";
        }
        if (!hasProductCatalog(model)) {
            return "当前没有可存入的标准化物品或标准商品目录。";
        }
        return "个人仓中没有可存入的对应商品。";
    }

    static String limitBuyActionHint(TerminalMarketSectionModel model, TerminalMarketSectionState state) {
        if (model == null || state == null) {
            return "先选择商品，再填写价格与数量。";
        }
        if (!hasProductCatalog(model)) {
            return "当前没有可交易标准商品。";
        }
        if (state.getSelectedProductKey().isEmpty()) {
            return "先在左侧选择商品。";
        }
        if (state.getLimitBuyPriceText().isEmpty() || state.getLimitBuyQuantityText().isEmpty()) {
            return "填写价格与数量后可提交限价买单。";
        }
        return model.getLimitBuyPreview();
    }

    static String limitSellActionHint(TerminalMarketSectionModel model, TerminalMarketSectionState state) {
        if (model == null || state == null) {
            return "先选择商品，再填写价格与数量。";
        }
        if (!hasProductCatalog(model)) {
            return "当前没有可交易标准商品。";
        }
        if (state.getSelectedProductKey().isEmpty()) {
            return "先在左侧选择商品。";
        }
        if (state.getLimitSellPriceText().isEmpty() || state.getLimitSellQuantityText().isEmpty()) {
            return "填写价格与数量后可提交限价卖单。";
        }
        return model.getLimitSellPreview();
    }

    static String instantBuyActionHint(TerminalMarketSectionModel model, TerminalMarketSectionState state) {
        if (model == null || state == null) {
            return "先选择商品，再填写数量。";
        }
        if (!hasProductCatalog(model)) {
            return "当前没有可交易标准商品。";
        }
        if (state.getSelectedProductKey().isEmpty()) {
            return "先在左侧选择商品。";
        }
        if (state.getInstantBuyQuantityText().isEmpty()) {
            return "填写数量后可按当前卖盘测深。";
        }
        return model.getInstantBuyPreview();
    }

    static String instantSellActionHint(TerminalMarketSectionModel model, TerminalMarketSectionState state) {
        if (model == null || state == null) {
            return "先选择商品，再填写数量。";
        }
        if (!hasProductCatalog(model)) {
            return "当前没有可交易标准商品。";
        }
        if (state.getSelectedProductKey().isEmpty()) {
            return "先在左侧选择商品。";
        }
        if (state.getInstantSellQuantityText().isEmpty()) {
            return "填写数量后可按当前买盘测深。";
        }
        return model.getInstantSellPreview();
    }

    static String latestFeedbackLine(TerminalMarketSectionModel model) {
        if (model == null) {
            return "当前没有动作反馈。";
        }
        return model.getActionFeedback().getSeverityName() + " / " + model.getActionFeedback().getTitle();
    }

    static String countActiveProducts(TerminalMarketSectionModel model) {
        int count = buildProductEntries(model).size();
        return count <= 0 ? "0" : String.valueOf(count);
    }

    private static String compactServiceState(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return "--";
        }
        if (normalized.contains("在线")) {
            return "在线";
        }
        if (normalized.contains("离线")) {
            return "离线";
        }
        return normalized;
    }

    private static String compactSourceMode(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return "--";
        }
        if (normalized.contains("统一仓储")) {
            return "统一仓储";
        }
        if (normalized.contains("runtime")) {
            return "runtime";
        }
        if (normalized.length() > 18) {
            return normalized.substring(0, 18);
        }
        return normalized;
    }

    private static String compactWarehouseNotice(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return "--";
        }
        if (normalized.contains("未检测")) {
            return "未检测到可存入物品";
        }
        if (normalized.contains("可直接卖出")) {
            return "可直接卖出";
        }
        if (normalized.length() > 20) {
            return normalized.substring(0, 20);
        }
        return normalized;
    }

    private static String resolveListValue(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return "";
        }
        return normalize(values.get(index));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    static final class ProductEntry {

        private final String key;
        private final String label;
        private final String title;
        private final String subtitle;
        private final String stateLabel;
        private final String iconRef;
        private final long referencePrice;
        private final boolean enabled;
        private final boolean selected;

        ProductEntry(String key, String label, boolean selected) {
            this.key = normalize(key);
            this.label = normalize(label);
            int split = this.label.indexOf('|');
            String parsedTitle = split >= 0 ? this.label.substring(0, split).trim() : this.label;
            String parsedSubtitle = split >= 0 ? this.label.substring(split + 1).trim() : this.key;
            this.title = parsedTitle.isEmpty() ? this.key : parsedTitle;
            this.subtitle = parsedSubtitle.isEmpty() ? "runtime 商品目录" : parsedSubtitle;
            this.stateLabel = selected ? "已选中" : "可交易";
            this.iconRef = this.key;
            this.referencePrice = 0L;
            this.enabled = true;
            this.selected = selected;
        }

        ProductEntry(TerminalMarketSectionModel.CatalogProductModel product, boolean selected) {
            this.key = product == null ? "" : normalize(product.getProductKey());
            String fallback = product == null ? "" : normalize(product.getDisplayName());
            this.label = product == null ? "" : TerminalMarketVisuals.resolveLocalizedItemName(
                TerminalMarketVisuals.itemRef(product.getRegistryName(), product.getMeta()), fallback);
            this.title = this.label.isEmpty() ? this.key : this.label;
            String unit = product == null ? "" : normalize(product.getUnitLabel());
            this.referencePrice = product == null ? 0L : product.getReferencePrice();
            this.subtitle = unit.isEmpty() ? "标准单位" : unit;
            this.stateLabel = selected ? "已选中" : product == null ? "不可用" : product.getTradability();
            this.iconRef = product == null ? "" : TerminalMarketVisuals.itemRef(product.getRegistryName(), product.getMeta());
            this.enabled = product != null && product.isEnabled();
            this.selected = selected;
        }

        String getKey() {
            return key;
        }

        String getLabel() {
            return label;
        }

        String getTitle() {
            return title;
        }

        String getSubtitle() {
            return subtitle;
        }

        String getStateLabel() {
            return stateLabel;
        }

        String getIconRef() {
            return iconRef;
        }

        long getReferencePrice() {
            return referencePrice;
        }

        boolean isEnabled() {
            return enabled;
        }

        boolean isSelected() {
            return selected;
        }
    }

    static final class ClaimEntry {

        private final String custodyId;
        private final String detail;

        ClaimEntry(String custodyId, String detail) {
            this.custodyId = normalize(custodyId);
            this.detail = normalize(detail);
        }

        String getCustodyId() {
            return custodyId;
        }

        String getDetail() {
            return detail;
        }
    }

    static final class OrderEntry {

        private final String orderId;
        private final String detail;
        private final boolean cancelable;
        private final String productKey;
        private final String side;
        private final String status;
        private final String createdAt;
        private final String displayName;
        private final String unitPrice;
        private final String originalQuantity;
        private final String filledQuantity;
        private final String remainingQuantity;
        private final long updatedAtEpochSeconds;
        private final String registryName;
        private final int meta;

        OrderEntry(String orderId, String detail, boolean cancelable) {
            this.orderId = normalize(orderId);
            this.detail = normalize(detail);
            this.cancelable = cancelable;
            String[] parts = this.detail.split("\\|");
            this.productKey = part(parts, 1);
            this.side = part(parts, 2).toUpperCase(java.util.Locale.ROOT);
            this.status = part(parts, 7).toUpperCase(java.util.Locale.ROOT);
            this.createdAt = part(parts, 8);
            this.displayName = part(parts, 9).isEmpty() ? productKey : part(parts, 9);
            this.unitPrice = stripLabel(part(parts, 3), "价");
            this.originalQuantity = stripLabel(part(parts, 4), "总");
            this.filledQuantity = stripLabel(part(parts, 5), "成");
            this.remainingQuantity = stripLabel(part(parts, 6), "剩");
            this.updatedAtEpochSeconds = parseLong(part(parts, 10));
            this.registryName = part(parts, 11);
            this.meta = (int) Math.max(0L, parseLong(part(parts, 12)));
        }

        OrderEntry(TerminalMarketAccountCenterRow row) {
            this.orderId = normalize(row.getRecordId()); this.detail = ""; this.cancelable = row.isCancelable();
            this.registryName = normalize(row.getRegistryName()); this.meta = row.getMeta();
            this.productKey = registryName.isEmpty() ? "" : registryName + ":" + meta;
            this.side = normalize(row.getSide()).toUpperCase(java.util.Locale.ROOT);
            this.status = normalize(row.getStatus()).toUpperCase(java.util.Locale.ROOT);
            this.createdAt = normalize(row.getCreatedAt()); this.displayName = productKey;
            this.unitPrice = TerminalNumberFormat.exact(row.getUnitPrice());
            this.originalQuantity = TerminalNumberFormat.exact(row.getOriginalQuantity());
            this.filledQuantity = TerminalNumberFormat.exact(row.getFilledQuantity());
            this.remainingQuantity = TerminalNumberFormat.exact(row.getRemainingQuantity());
            this.updatedAtEpochSeconds = row.getUpdatedAtEpochSeconds();
        }

        String getOrderId() {
            return orderId;
        }

        String getDetail() {
            return detail;
        }

        boolean isCancelable() {
            return cancelable && !orderId.isEmpty() && parseLong(remainingQuantity) > 0L
                && ("OPEN".equals(status) || "PARTIALLY_FILLED".equals(status));
        }

        String getProductKey() { return productKey; }
        String getSide() { return side; }
        String getStatus() { return status; }
        String getCreatedAt() { return createdAt; }
        String getDisplayName() { return displayName; }
        String getUnitPrice() { return unitPrice; }
        String getOriginalQuantity() { return originalQuantity; }
        String getFilledQuantity() { return filledQuantity; }
        String getRemainingQuantity() { return remainingQuantity; }
        long getUpdatedAtEpochSeconds() { return updatedAtEpochSeconds; }
        String getRegistryName() { return registryName; }
        int getMeta() { return meta; }

        String getSideLabel() {
            return "BUY".equals(side) ? "买" : "SELL".equals(side) ? "卖" : "--";
        }

        String getStatusLabel() {
            if ("OPEN".equals(status)) return "未成交";
            if ("PARTIALLY_FILLED".equals(status)) return "部分成交";
            if ("FILLED".equals(status) || "COMPLETED".equals(status)) return "已成交";
            if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return "已撤销";
            if ("REJECTED".equals(status)) return "已拒绝";
            if ("EXPIRED".equals(status)) return "已过期";
            return status.isEmpty() ? "--" : status;
        }

        String getStatusMarkerLabel() {
            if ("OPEN".equals(status)) return "进行中·未成交";
            if ("PARTIALLY_FILLED".equals(status)) return "进行中·部分成交";
            if ("FILLED".equals(status) || "COMPLETED".equals(status)) return "完成·已成交";
            if ("CANCELLED".equals(status) || "CANCELED".equals(status)) return "结束·已撤销";
            if ("REJECTED".equals(status)) return "结束·已拒绝";
            if ("EXPIRED".equals(status)) return "结束·已过期";
            return getStatusLabel();
        }

        String getFillProgressLabel() {
            long original = parseLong(originalQuantity);
            long filled = parseLong(filledQuantity);
            if (original <= 0L) return filledQuantity + "/" + originalQuantity;
            long percent = java.math.BigInteger.valueOf(filled)
                .multiply(java.math.BigInteger.valueOf(100L))
                .divide(java.math.BigInteger.valueOf(original))
                .longValue();
            percent = Math.max(0L, Math.min(100L, percent));
            return filledQuantity + "/" + originalQuantity + " " + percent + "%";
        }

        private static long parseLong(String value) {
            if (value == null) return 0L;
            try {
                return Long.parseLong(value.replace(",", "").trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }

        boolean matches(TerminalMarketSectionState state, String selectedProductKey) {
            if (state == null) return true;
            if (state.isHistoryCurrentProductOnly() && !normalize(selectedProductKey).equals(productKey)) return false;
            if (state.getHistorySide() != TerminalMarketSectionState.HistorySide.ALL
                && !state.getHistorySide().name().equals(side)) return false;
            if (!matchesStatus(state.getHistoryStatus())) return false;
            return matchesTime(state.getHistoryTime());
        }

        String getCompactSummary() {
            String[] parts = detail.split("\\|");
            String price = part(parts, 3);
            String filled = part(parts, 5);
            String remaining = part(parts, 6);
            return price + " / " + filled + " / " + remaining;
        }

        private boolean matchesStatus(TerminalMarketSectionState.HistoryStatus filter) {
            if (filter == TerminalMarketSectionState.HistoryStatus.ALL) return true;
            if (filter == TerminalMarketSectionState.HistoryStatus.OPEN) {
                return "OPEN".equals(status) || "PARTIALLY_FILLED".equals(status);
            }
            if (filter == TerminalMarketSectionState.HistoryStatus.FILLED) {
                return "FILLED".equals(status) || "COMPLETED".equals(status);
            }
            return "CANCELLED".equals(status) || "CANCELED".equals(status) || "REJECTED".equals(status)
                || "EXPIRED".equals(status);
        }

        private boolean matchesTime(TerminalMarketSectionState.HistoryTime filter) {
            if (filter == TerminalMarketSectionState.HistoryTime.ALL || createdAt.isEmpty()) return true;
            try {
                long ageMillis = System.currentTimeMillis() - java.time.Instant.parse(createdAt).toEpochMilli();
                long days = filter == TerminalMarketSectionState.HistoryTime.DAY ? 1L
                    : filter == TerminalMarketSectionState.HistoryTime.WEEK ? 7L : 30L;
                return ageMillis >= 0L && ageMillis <= days * 24L * 60L * 60L * 1000L;
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        private static String part(String[] parts, int index) {
            return parts != null && index >= 0 && index < parts.length ? normalize(parts[index]) : "";
        }

        private static String stripLabel(String value, String label) {
            String normalized = normalize(value);
            String prefix = normalize(label);
            return normalized.startsWith(prefix) ? normalize(normalized.substring(prefix.length())) : normalized;
        }
    }

    static final class OverviewEntrySummary {

        private final String title;
        private final String summary;
        private final String status;
        private final String actionLabel;

        OverviewEntrySummary(String title, String summary, String status, String actionLabel) {
            this.title = normalize(title);
            this.summary = normalize(summary);
            this.status = normalize(status);
            this.actionLabel = normalize(actionLabel);
        }

        String getTitle() {
            return title;
        }

        String getSummary() {
            return summary;
        }

        String getStatus() {
            return status;
        }

        String getActionLabel() {
            return actionLabel;
        }
    }
}
