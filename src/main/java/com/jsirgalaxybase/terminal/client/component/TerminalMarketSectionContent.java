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
        lines.add("服务状态: " + model.getServiceState());
        lines.add("浏览提示: " + model.getBrowserHint());
        lines.add("活跃商品预览: " + summarizeProducts(model));
        lines.add("CLAIMABLE / 冻结资金: " + model.getClaimableQuantity() + " / " + model.getFrozenFunds());
        return lines;
    }

    static List<String> buildOverviewEntryLines(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.singletonList("当前没有标准商品入口摘要。");
        }
        List<String> lines = new ArrayList<String>();
        lines.add("最新成交价: " + model.getLatestTradePrice());
        lines.add("盘口: 买一 " + model.getHighestBid() + " / 卖一 " + model.getLowestAsk());
        lines.add("24h: 成交量 " + model.getVolume24h() + " / 成交额 " + model.getTurnover24h());
        lines.add("摘要: " + model.getSummaryNotice());
        return lines;
    }

    static List<String> buildOverviewBoundaryLines() {
        List<String> lines = new ArrayList<String>();
        lines.add("MARKET 根页继续只做总入口与共享摘要。");
        lines.add("MARKET_STANDARDIZED 继续承接真实标准商品市场动作。");
        lines.add("MARKET_CUSTOM / MARKET_EXCHANGE 继续明确留给 phase 7。");
        lines.add("旧 ModularUI 市场实现本轮只参考，不回接、不 cutover、不删除。");
        return lines;
    }

    static List<ProductEntry> buildProductEntries(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.emptyList();
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

    static List<String> buildMetricsLines(TerminalMarketSectionModel model) {
        if (model == null) {
            return Collections.singletonList("当前没有交易焦点摘要。");
        }
        List<String> lines = new ArrayList<String>();
        lines.add("商品: " + model.getSelectedProductName());
        lines.add("单位: " + model.getSelectedProductUnit());
        lines.add("最新成交价: " + model.getLatestTradePrice());
        lines.add("买一 / 卖一: " + model.getHighestBid() + " / " + model.getLowestAsk());
        lines.add("24h 成交量 / 成交额: " + model.getVolume24h() + " / " + model.getTurnover24h());
        lines.add("AVAILABLE / ESCROW / CLAIMABLE: " + model.getSourceAvailable() + " / "
            + model.getLockedEscrowQuantity() + " / " + model.getClaimableQuantity());
        lines.add("冻结资金: " + model.getFrozenFunds());
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

    private static String summarizeProducts(TerminalMarketSectionModel model) {
        StringBuilder builder = new StringBuilder();
        for (ProductEntry entry : buildProductEntries(model)) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(entry.getLabel());
            if (builder.length() > 96) {
                break;
            }
        }
        return builder.length() == 0 ? "当前没有活跃标准商品。" : builder.toString();
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
        private final boolean selected;

        ProductEntry(String key, String label, boolean selected) {
            this.key = normalize(key);
            this.label = normalize(label);
            this.selected = selected;
        }

        String getKey() {
            return key;
        }

        String getLabel() {
            return label;
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
}