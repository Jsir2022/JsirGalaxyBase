package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

final class TerminalMarketSectionContent {

    private TerminalMarketSectionContent() {}

    static List<String> buildOverviewSummaryLines(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.singletonList("当前没有共享运行态。");
        }
        List<String> lines = new ArrayList<String>();
        lines.add("服务: " + model.getServiceState() + " | 活跃商品: " + countActiveProducts(model));
        lines.add("CLAIMABLE: " + model.getClaimableQuantity() + " | 冻结资金: " + model.getFrozenFunds());
        return lines;
    }

    static OverviewEntrySummary buildStandardizedOverviewEntry(TerminalMarketSectionModel model) {
        String secondary = model == null
            ? "当前没有标准市场状态。"
            : "活跃 " + countActiveProducts(model) + " | claim " + model.getClaimableQuantity();
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
            return "市场快照未加载";
        }
        if (normalize(model.getServiceState()).contains("不可用")) {
            return "市场服务不可用";
        }
        return "暂无可浏览标准商品";
    }

    static String buildProductCatalogEmptyReason(TerminalMarketSectionModel model) {
        if (model == null) {
            return "等待服务端回写标准商品市场 snapshot。";
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
        lines.add("AVAILABLE / ESCROW / CLAIMABLE: " + model.getSourceAvailable() + " / "
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
            return "从个人仓存入 AVAILABLE 后，才能继续卖出。";
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
            this.label = product == null ? "" : normalize(product.getDisplayName());
            this.title = this.label.isEmpty() ? this.key : this.label;
            String unit = product == null ? "" : normalize(product.getUnitLabel());
            this.referencePrice = product == null ? 0L : product.getReferencePrice();
            this.subtitle = unit.isEmpty() ? "标准单位" : unit;
            this.stateLabel = selected ? "已选中" : product == null ? "不可用" : product.getTradability();
            this.iconRef = product == null ? "" : normalize(product.getRegistryName()) + "@" + product.getMeta();
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

        OrderEntry(String orderId, String detail, boolean cancelable) {
            this.orderId = normalize(orderId);
            this.detail = normalize(detail);
            this.cancelable = cancelable;
        }

        String getOrderId() {
            return orderId;
        }

        String getDetail() {
            return detail;
        }

        boolean isCancelable() {
            return cancelable && !orderId.isEmpty();
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
