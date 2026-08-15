package com.jsirgalaxybase.terminal.client.viewmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;
import com.jsirgalaxybase.terminal.TerminalMarketBrowseEntry;

public final class TerminalCustomMarketSectionModel {

    private final String serviceState;
    private final String browserHint;
    private final String scopeLabel;
    private final List<String> activeListingLines;
    private final List<String> activeListingIds;
    private final List<String> activeListingIconRefs;
    private final List<String> sellingListingLines;
    private final List<String> sellingListingIds;
    private final List<String> sellingListingIconRefs;
    private final List<String> pendingListingLines;
    private final List<String> pendingListingIds;
    private final List<String> pendingListingIconRefs;
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
    private final ActionFeedbackModel actionFeedback;
    private List<TerminalMarketBrowseEntry> browseEntries;
    private String browseQuery;
    private int browsePageIndex;
    private int browsePageSize;
    private int browseTotalEntries;
    private boolean hasPreviousPage;
    private boolean hasNextPage;

    public TerminalCustomMarketSectionModel(String serviceState, String browserHint, String scopeLabel,
        List<String> activeListingLines, List<String> activeListingIds, List<String> sellingListingLines,
        List<String> sellingListingIds, List<String> pendingListingLines, List<String> pendingListingIds,
        String selectedListingId, String selectedTitle, String selectedPrice, String selectedStatus,
        String selectedCounterparty, String selectedItemIdentity, String selectedTradeSummary,
        String selectedActionHint, boolean canBuy, boolean canCancel, boolean canClaim,
        ActionFeedbackModel actionFeedback) {
        this(serviceState, browserHint, scopeLabel,
            activeListingLines, activeListingIds, Collections.<String>emptyList(),
            sellingListingLines, sellingListingIds, Collections.<String>emptyList(),
            pendingListingLines, pendingListingIds, Collections.<String>emptyList(),
            selectedListingId, selectedTitle, selectedPrice, selectedStatus,
            selectedCounterparty, selectedItemIdentity, selectedTradeSummary,
            selectedActionHint, canBuy, canCancel, canClaim, actionFeedback);
    }

    public TerminalCustomMarketSectionModel(String serviceState, String browserHint, String scopeLabel,
        List<String> activeListingLines, List<String> activeListingIds, List<String> activeListingIconRefs,
        List<String> sellingListingLines, List<String> sellingListingIds, List<String> sellingListingIconRefs,
        List<String> pendingListingLines, List<String> pendingListingIds, List<String> pendingListingIconRefs,
        String selectedListingId, String selectedTitle, String selectedPrice, String selectedStatus,
        String selectedCounterparty, String selectedItemIdentity, String selectedTradeSummary,
        String selectedActionHint, boolean canBuy, boolean canCancel, boolean canClaim,
        ActionFeedbackModel actionFeedback) {
        this.serviceState = normalize(serviceState, "定制商品市场状态未知");
        this.browserHint = normalize(browserHint, "当前没有定制商品浏览提示。");
        this.scopeLabel = normalize(scopeLabel, "全部挂牌");
        this.activeListingLines = freeze(activeListingLines, Collections.singletonList("当前没有可购买的定制挂牌。"));
        this.activeListingIds = freeze(activeListingIds, Collections.<String>emptyList());
        this.activeListingIconRefs = freeze(activeListingIconRefs, Collections.<String>emptyList());
        this.sellingListingLines = freeze(sellingListingLines, Collections.singletonList("你当前没有出售中的挂牌。"));
        this.sellingListingIds = freeze(sellingListingIds, Collections.<String>emptyList());
        this.sellingListingIconRefs = freeze(sellingListingIconRefs, Collections.<String>emptyList());
        this.pendingListingLines = freeze(pendingListingLines, Collections.singletonList("你当前没有待领取成交物。"));
        this.pendingListingIds = freeze(pendingListingIds, Collections.<String>emptyList());
        this.pendingListingIconRefs = freeze(pendingListingIconRefs, Collections.<String>emptyList());
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
        this.actionFeedback = actionFeedback == null ? ActionFeedbackModel.placeholder() : actionFeedback;
        this.browseEntries = Collections.emptyList();
        this.browseQuery = "";
        this.browsePageIndex = 0;
        this.browsePageSize = 12;
        this.browseTotalEntries = 0;
        this.hasPreviousPage = false;
        this.hasNextPage = false;
    }

    public TerminalCustomMarketSectionModel withBrowsePage(List<TerminalMarketBrowseEntry> entries, String query,
        int pageIndex, int pageSize, int totalEntries, boolean previous, boolean next) {
        TerminalCustomMarketSectionModel copy = new TerminalCustomMarketSectionModel(serviceState, browserHint, scopeLabel,
            activeListingLines, activeListingIds, activeListingIconRefs, sellingListingLines, sellingListingIds,
            sellingListingIconRefs, pendingListingLines, pendingListingIds, pendingListingIconRefs, selectedListingId,
            selectedTitle, selectedPrice, selectedStatus, selectedCounterparty, selectedItemIdentity, selectedTradeSummary,
            selectedActionHint, canBuy, canCancel, canClaim, actionFeedback);
        copy.browseEntries = freeze(entries, Collections.<TerminalMarketBrowseEntry>emptyList());
        copy.browseQuery = normalize(query, "");
        copy.browsePageIndex = Math.max(0, pageIndex);
        copy.browsePageSize = Math.max(1, pageSize);
        copy.browseTotalEntries = Math.max(0, totalEntries);
        copy.hasPreviousPage = previous;
        copy.hasNextPage = next;
        return copy;
    }

    public static TerminalCustomMarketSectionModel placeholder() {
        return new TerminalCustomMarketSectionModel(
            "定制商品市场已接入",
            "正在等待服务器返回定制商品市场数据。",
            "全部挂牌",
            Collections.singletonList("当前没有可购买的定制挂牌。"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            Collections.singletonList("你当前没有出售中的挂牌。"),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            Collections.singletonList("你当前没有待领取成交物。"),
            Collections.<String>emptyList(),
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
            ActionFeedbackModel.placeholder());
    }

    public String getServiceState() { return serviceState; }
    public String getBrowserHint() { return browserHint; }
    public String getScopeLabel() { return scopeLabel; }
    public List<String> getActiveListingLines() { return activeListingLines; }
    public List<String> getActiveListingIds() { return activeListingIds; }
    public List<String> getActiveListingIconRefs() { return activeListingIconRefs; }
    public List<String> getSellingListingLines() { return sellingListingLines; }
    public List<String> getSellingListingIds() { return sellingListingIds; }
    public List<String> getSellingListingIconRefs() { return sellingListingIconRefs; }
    public List<String> getPendingListingLines() { return pendingListingLines; }
    public List<String> getPendingListingIds() { return pendingListingIds; }
    public List<String> getPendingListingIconRefs() { return pendingListingIconRefs; }
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
    public ActionFeedbackModel getActionFeedback() { return actionFeedback; }
    public List<TerminalMarketBrowseEntry> getBrowseEntries() { return browseEntries; }
    public String getBrowseQuery() { return browseQuery; }
    public int getBrowsePageIndex() { return browsePageIndex; }
    public int getBrowsePageSize() { return browsePageSize; }
    public int getBrowseTotalEntries() { return browseTotalEntries; }
    public boolean hasPreviousPage() { return hasPreviousPage; }
    public boolean hasNextPage() { return hasNextPage; }
    public boolean hasSelectedListing() { return !selectedListingId.isEmpty(); }
    public boolean hasAnyListing() {
        return hasRealListing(activeListingIds) || hasRealListing(sellingListingIds) || hasRealListing(pendingListingIds);
    }
    public String getDisabledReason() {
        if (!hasAnyListing()) {
            return "当前范围没有挂牌数据。";
        }
        if (!hasSelectedListing()) {
            return "先从左侧列表选择一条挂牌。";
        }
        if (!canBuy && !canCancel && !canClaim) {
            return selectedActionHint;
        }
        return "";
    }

    private static <T> List<T> freeze(List<T> source, List<T> fallback) {
        List<T> compact = trimTrailingPadding(source);
        List<T> resolved = compact.isEmpty() ? fallback : compact;
        return Collections.unmodifiableList(new ArrayList<T>(resolved));
    }

    private static <T> List<T> trimTrailingPadding(List<T> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        int lastIncluded = source.size() - 1;
        while (lastIncluded >= 0) {
            T value = source.get(lastIncluded);
            if (value == null) {
                lastIncluded--;
                continue;
            }
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                lastIncluded--;
                continue;
            }
            break;
        }
        if (lastIncluded < 0) {
            return Collections.emptyList();
        }
        return new ArrayList<T>(source.subList(0, lastIncluded + 1));
    }

    private static boolean hasRealListing(List<String> ids) {
        if (ids == null) {
            return false;
        }
        for (String id : ids) {
            if (id != null && !id.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    public static final class ActionFeedbackModel {
        private final String title;
        private final String body;
        private final String severityName;

        public ActionFeedbackModel(String title, String body, String severityName) {
            this.title = normalize(title, "定制市场动作反馈");
            this.body = normalize(body, "当前没有定制市场动作反馈。");
            this.severityName = normalize(severityName, TerminalNotificationSeverity.INFO.name());
        }

        public static ActionFeedbackModel placeholder() {
            return new ActionFeedbackModel("定制市场动作反馈", "当前没有定制市场动作反馈。",
                TerminalNotificationSeverity.INFO.name());
        }

        public String getTitle() { return title; }
        public String getBody() { return body; }
        public String getSeverityName() { return severityName; }
    }
}
