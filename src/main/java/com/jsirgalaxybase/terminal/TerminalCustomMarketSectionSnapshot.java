package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

public final class TerminalCustomMarketSectionSnapshot {

    private final String serviceState;
    private final String browserHint;
    private final String scopeLabel;
    private final List<String> activeListingLines;
    private final List<String> activeListingIds;
    private final List<String> sellingListingLines;
    private final List<String> sellingListingIds;
    private final List<String> pendingListingLines;
    private final List<String> pendingListingIds;
    private final String selectedListingId;
    private final String selectedTitle;
    private final String selectedPrice;
    private final String selectedStatus;
    private final String selectedCounterparty;
    private final String selectedItemIdentity;
    private final String selectedTradeSummary;
    private final String selectedActionHint;
    private final boolean canBuy;
    private final boolean canCancel;
    private final boolean canClaim;
    private final ActionFeedback actionFeedback;

    public TerminalCustomMarketSectionSnapshot(String serviceState, String browserHint, String scopeLabel,
        List<String> activeListingLines, List<String> activeListingIds, List<String> sellingListingLines,
        List<String> sellingListingIds, List<String> pendingListingLines, List<String> pendingListingIds,
        String selectedListingId, String selectedTitle, String selectedPrice, String selectedStatus,
        String selectedCounterparty, String selectedItemIdentity, String selectedTradeSummary,
        String selectedActionHint, boolean canBuy, boolean canCancel, boolean canClaim,
        ActionFeedback actionFeedback) {
        this.serviceState = normalize(serviceState, "定制商品市场状态未知");
        this.browserHint = normalize(browserHint, "当前没有定制商品浏览提示。");
        this.scopeLabel = normalize(scopeLabel, "全部挂牌");
        this.activeListingLines = freeze(activeListingLines, Collections.singletonList("当前没有 active custom listings。"));
        this.activeListingIds = freeze(activeListingIds, Collections.<String>emptyList());
        this.sellingListingLines = freeze(sellingListingLines, Collections.singletonList("你当前没有出售中的挂牌。"));
        this.sellingListingIds = freeze(sellingListingIds, Collections.<String>emptyList());
        this.pendingListingLines = freeze(pendingListingLines, Collections.singletonList("你当前没有待领取成交物。"));
        this.pendingListingIds = freeze(pendingListingIds, Collections.<String>emptyList());
        this.selectedListingId = normalize(selectedListingId, "");
        this.selectedTitle = normalize(selectedTitle, "未选中挂牌");
        this.selectedPrice = normalize(selectedPrice, "--");
        this.selectedStatus = normalize(selectedStatus, "--");
        this.selectedCounterparty = normalize(selectedCounterparty, "请先选择挂牌");
        this.selectedItemIdentity = normalize(selectedItemIdentity, "--");
        this.selectedTradeSummary = normalize(selectedTradeSummary, "--");
        this.selectedActionHint = normalize(selectedActionHint, "先从列表选择一条挂牌。");
        this.canBuy = canBuy;
        this.canCancel = canCancel;
        this.canClaim = canClaim;
        this.actionFeedback = actionFeedback == null ? ActionFeedback.placeholder() : actionFeedback;
    }

    public static TerminalCustomMarketSectionSnapshot placeholder() {
        return new TerminalCustomMarketSectionSnapshot(
            "定制商品市场 section 已接入",
            "listing-first 页面等待服务端 snapshot。",
            "全部挂牌",
            Collections.singletonList("当前没有 active custom listings。"),
            Collections.<String>emptyList(),
            Collections.singletonList("你当前没有出售中的挂牌。"),
            Collections.<String>emptyList(),
            Collections.singletonList("你当前没有待领取成交物。"),
            Collections.<String>emptyList(),
            "",
            "未选中挂牌",
            "--",
            "--",
            "请先选择挂牌",
            "--",
            "--",
            "先从列表选择一条挂牌。",
            false,
            false,
            false,
            ActionFeedback.placeholder());
    }

    public String getServiceState() { return serviceState; }
    public String getBrowserHint() { return browserHint; }
    public String getScopeLabel() { return scopeLabel; }
    public List<String> getActiveListingLines() { return activeListingLines; }
    public List<String> getActiveListingIds() { return activeListingIds; }
    public List<String> getSellingListingLines() { return sellingListingLines; }
    public List<String> getSellingListingIds() { return sellingListingIds; }
    public List<String> getPendingListingLines() { return pendingListingLines; }
    public List<String> getPendingListingIds() { return pendingListingIds; }
    public String getSelectedListingId() { return selectedListingId; }
    public String getSelectedTitle() { return selectedTitle; }
    public String getSelectedPrice() { return selectedPrice; }
    public String getSelectedStatus() { return selectedStatus; }
    public String getSelectedCounterparty() { return selectedCounterparty; }
    public String getSelectedItemIdentity() { return selectedItemIdentity; }
    public String getSelectedTradeSummary() { return selectedTradeSummary; }
    public String getSelectedActionHint() { return selectedActionHint; }
    public boolean isCanBuy() { return canBuy; }
    public boolean isCanCancel() { return canCancel; }
    public boolean isCanClaim() { return canClaim; }
    public ActionFeedback getActionFeedback() { return actionFeedback; }

    private static <T> List<T> freeze(List<T> source, List<T> fallback) {
        List<T> resolved = source == null || source.isEmpty() ? fallback : source;
        return Collections.unmodifiableList(new ArrayList<T>(resolved));
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    public static final class ActionFeedback {
        private final String title;
        private final String body;
        private final String severityName;

        public ActionFeedback(String title, String body, String severityName) {
            this.title = normalize(title, "定制市场动作反馈");
            this.body = normalize(body, "当前没有定制市场动作反馈。");
            this.severityName = normalize(severityName, TerminalNotificationSeverity.INFO.name());
        }

        public static ActionFeedback placeholder() {
            return new ActionFeedback("定制市场动作反馈", "当前没有定制市场动作反馈。",
                TerminalNotificationSeverity.INFO.name());
        }

        public String getTitle() { return title; }
        public String getBody() { return body; }
        public String getSeverityName() { return severityName; }
    }
}
