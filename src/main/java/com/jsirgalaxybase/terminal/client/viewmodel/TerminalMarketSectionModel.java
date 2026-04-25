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
    private final String volume24h;
    private final String turnover24h;
    private final String sourceAvailable;
    private final String lockedEscrowQuantity;
    private final String claimableQuantity;
    private final String frozenFunds;
    private final String summaryNotice;
    private final List<String> askLines;
    private final List<String> bidLines;
    private final List<String> myOrderLines;
    private final List<String> claimLines;
    private final List<String> claimIds;
    private final List<String> ruleLines;
    private final LimitBuyDraftModel limitBuyDraft;
    private final ActionFeedbackModel actionFeedback;

    public TerminalMarketSectionModel(String routePageId, String serviceState, String browserHint,
        List<String> productKeys, List<String> productLabels, String selectedProductKey, String selectedProductName,
        String selectedProductUnit, String latestTradePrice, String highestBid, String lowestAsk, String volume24h,
        String turnover24h, String sourceAvailable, String lockedEscrowQuantity, String claimableQuantity,
        String frozenFunds, String summaryNotice, List<String> askLines, List<String> bidLines,
        List<String> myOrderLines, List<String> claimLines, List<String> claimIds, List<String> ruleLines,
        LimitBuyDraftModel limitBuyDraft, ActionFeedbackModel actionFeedback) {
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
        this.volume24h = normalize(volume24h, "0");
        this.turnover24h = normalize(turnover24h, "0 STARCOIN");
        this.sourceAvailable = normalize(sourceAvailable, "0");
        this.lockedEscrowQuantity = normalize(lockedEscrowQuantity, "0");
        this.claimableQuantity = normalize(claimableQuantity, "0");
        this.frozenFunds = normalize(frozenFunds, "0 STARCOIN");
        this.summaryNotice = normalize(summaryNotice, "当前没有市场摘要说明。");
        this.askLines = freeze(askLines, Collections.singletonList("当前没有卖盘深度。"));
        this.bidLines = freeze(bidLines, Collections.singletonList("当前没有买盘深度。"));
        this.myOrderLines = freeze(myOrderLines, Collections.singletonList("当前没有个人订单。"));
        this.claimLines = freeze(claimLines, Collections.singletonList("当前没有待提取的 CLAIMABLE 资产。"));
        this.claimIds = freeze(claimIds, Collections.singletonList(""));
        this.ruleLines = freeze(ruleLines, Collections.singletonList("当前没有规则提示。"));
        this.limitBuyDraft = limitBuyDraft == null ? LimitBuyDraftModel.placeholder() : limitBuyDraft;
        this.actionFeedback = actionFeedback == null ? ActionFeedbackModel.placeholder() : actionFeedback;
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
            "0 STARCOIN",
            "0",
            "0",
            "0",
            "0 STARCOIN",
            "phase 6 会在这里承接市场总入口和标准商品市场。",
            Collections.singletonList("当前没有卖盘深度。"),
            Collections.singletonList("当前没有买盘深度。"),
            Collections.singletonList("当前没有个人订单。"),
            Collections.singletonList("当前没有待提取的 CLAIMABLE 资产。"),
            Collections.singletonList(""),
            Collections.singletonList("当前没有规则提示。"),
            LimitBuyDraftModel.placeholder(),
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

    public List<String> getAskLines() {
        return askLines;
    }

    public List<String> getBidLines() {
        return bidLines;
    }

    public List<String> getMyOrderLines() {
        return myOrderLines;
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

    public LimitBuyDraftModel getLimitBuyDraft() {
        return limitBuyDraft;
    }

    public ActionFeedbackModel getActionFeedback() {
        return actionFeedback;
    }

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
}