package com.jsirgalaxybase.terminal.client.viewmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;
import com.jsirgalaxybase.terminal.TerminalMarketBrowseEntry;

public final class TerminalExchangeMarketSectionModel {

    private final String serviceState;
    private final String browserHint;
    private final List<String> targetCodes;
    private final List<String> targetLabels;
    private final String selectedTargetCode;
    private final String selectedCoinCode;
    private final String selectedTargetTitle;
    private final String selectedTargetSummary;
    private final String heldSummary;
    private final String inputRegistryName;
    private final String pairCode;
    private final String inputAssetCode;
    private final String outputAssetCode;
    private final String ruleVersion;
    private final String limitStatus;
    private final String reasonCode;
    private final String notes;
    private final String inputQuantity;
    private final String nominalFaceValue;
    private final String effectiveExchangeValue;
    private final String contributionValue;
    private final String discountStatus;
    private final String rateDisplay;
    private final String executionHint;
    private final boolean executable;
    private final ActionFeedbackModel actionFeedback;
    private List<TerminalMarketBrowseEntry> browseEntries;
    private String browseQuery;
    private int browsePageIndex;
    private int browsePageSize;
    private int browseTotalEntries;
    private boolean hasPreviousPage;
    private boolean hasNextPage;

    public TerminalExchangeMarketSectionModel(String serviceState, String browserHint, List<String> targetCodes,
        List<String> targetLabels, String selectedTargetCode, String selectedTargetTitle,
        String selectedTargetSummary, String heldSummary, String inputRegistryName, String pairCode,
        String inputAssetCode, String outputAssetCode, String ruleVersion, String limitStatus, String reasonCode,
        String notes, String inputQuantity, String nominalFaceValue, String effectiveExchangeValue,
        String contributionValue, String discountStatus, String rateDisplay, String executionHint,
        boolean executable, ActionFeedbackModel actionFeedback) {
        this(serviceState, browserHint, targetCodes, targetLabels, selectedTargetCode, "", selectedTargetTitle,
            selectedTargetSummary, heldSummary, inputRegistryName, pairCode, inputAssetCode, outputAssetCode,
            ruleVersion, limitStatus, reasonCode, notes, inputQuantity, nominalFaceValue, effectiveExchangeValue,
            contributionValue, discountStatus, rateDisplay, executionHint, executable, actionFeedback);
    }

    public TerminalExchangeMarketSectionModel(String serviceState, String browserHint, List<String> targetCodes,
        List<String> targetLabels, String selectedTargetCode, String selectedCoinCode, String selectedTargetTitle,
        String selectedTargetSummary, String heldSummary, String inputRegistryName, String pairCode,
        String inputAssetCode, String outputAssetCode, String ruleVersion, String limitStatus, String reasonCode,
        String notes, String inputQuantity, String nominalFaceValue, String effectiveExchangeValue,
        String contributionValue, String discountStatus, String rateDisplay, String executionHint,
        boolean executable, ActionFeedbackModel actionFeedback) {
        this.serviceState = normalize(serviceState, "汇率市场状态未知");
        this.browserHint = normalize(browserHint, "当前没有汇率市场浏览提示。");
        this.targetCodes = freeze(targetCodes, Collections.<String>emptyList());
        this.targetLabels = freeze(targetLabels, Collections.<String>emptyList());
        this.selectedTargetCode = normalize(selectedTargetCode, "");
        this.selectedCoinCode = normalize(selectedCoinCode, "");
        this.selectedTargetTitle = normalize(selectedTargetTitle, "未选择兑换标的");
        this.selectedTargetSummary = normalize(selectedTargetSummary, "请选择标的后查看报价。");
        this.heldSummary = normalize(heldSummary, "当前未选择 Base Vault 资产");
        this.inputRegistryName = normalize(inputRegistryName, "--");
        this.pairCode = normalize(pairCode, "--");
        this.inputAssetCode = normalize(inputAssetCode, "--");
        this.outputAssetCode = normalize(outputAssetCode, "--");
        this.ruleVersion = normalize(ruleVersion, "--");
        this.limitStatus = normalize(limitStatus, "UNAVAILABLE");
        this.reasonCode = normalize(reasonCode, "--");
        this.notes = normalize(notes, "--");
        this.inputQuantity = normalize(inputQuantity, "0");
        this.nominalFaceValue = normalize(nominalFaceValue, "0");
        this.effectiveExchangeValue = normalize(effectiveExchangeValue, "0");
        this.contributionValue = normalize(contributionValue, "0");
        this.discountStatus = normalize(discountStatus, "当前暂无可执行报价");
        this.rateDisplay = normalize(rateDisplay, "--");
        this.executionHint = normalize(executionHint, "当前不能继续执行兑换。");
        this.executable = executable;
        this.actionFeedback = actionFeedback == null ? ActionFeedbackModel.placeholder() : actionFeedback;
        this.browseEntries = Collections.emptyList();
        this.browseQuery = "";
        this.browsePageIndex = 0;
        this.browsePageSize = 12;
        this.browseTotalEntries = 0;
        this.hasPreviousPage = false;
        this.hasNextPage = false;
    }

    public TerminalExchangeMarketSectionModel withBrowsePage(List<TerminalMarketBrowseEntry> entries, String query,
        int pageIndex, int pageSize, int totalEntries, boolean previous, boolean next) {
        TerminalExchangeMarketSectionModel copy = new TerminalExchangeMarketSectionModel(serviceState, browserHint,
            targetCodes, targetLabels, selectedTargetCode, selectedCoinCode, selectedTargetTitle, selectedTargetSummary, heldSummary,
            inputRegistryName, pairCode, inputAssetCode, outputAssetCode, ruleVersion, limitStatus, reasonCode, notes,
            inputQuantity, nominalFaceValue, effectiveExchangeValue, contributionValue, discountStatus, rateDisplay,
            executionHint, executable, actionFeedback);
        copy.browseEntries = freeze(entries, Collections.<TerminalMarketBrowseEntry>emptyList());
        copy.browseQuery = normalize(query, "");
        copy.browsePageIndex = Math.max(0, pageIndex);
        copy.browsePageSize = Math.max(1, pageSize);
        copy.browseTotalEntries = Math.max(0, totalEntries);
        copy.hasPreviousPage = previous;
        copy.hasNextPage = next;
        return copy;
    }

    public static TerminalExchangeMarketSectionModel placeholder() {
        return new TerminalExchangeMarketSectionModel(
            "汇率市场已接入",
            "正在等待服务器返回汇率市场数据。",
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            "",
            "未选择兑换标的",
            "请选择标的后查看报价。",
            "当前未选择 Base Vault 资产",
            "--",
            "--",
            "--",
            "--",
            "--",
            "UNAVAILABLE",
            "--",
            "--",
            "0",
            "0",
            "0",
            "0",
            "当前暂无可执行报价",
            "--",
            "当前不能继续执行兑换。",
            false,
            ActionFeedbackModel.placeholder());
    }

    public String getServiceState() { return serviceState; }
    public String getBrowserHint() { return browserHint; }
    public List<String> getTargetCodes() { return targetCodes; }
    public List<String> getTargetLabels() { return targetLabels; }
    public String getSelectedTargetCode() { return selectedTargetCode; }
    public String getSelectedCoinCode() { return selectedCoinCode; }
    public String getSelectedTargetTitle() { return selectedTargetTitle; }
    public String getSelectedTargetSummary() { return selectedTargetSummary; }
    public String getHeldSummary() { return heldSummary; }
    public String getInputRegistryName() { return inputRegistryName; }
    public String getPairCode() { return pairCode; }
    public String getInputAssetCode() { return inputAssetCode; }
    public String getOutputAssetCode() { return outputAssetCode; }
    public String getRuleVersion() { return ruleVersion; }
    public String getLimitStatus() { return limitStatus; }
    public String getLimitStatusDisplay() {
        String normalized = limitStatus == null ? "" : limitStatus.trim().toUpperCase(java.util.Locale.ROOT);
        if ("ACTIVE".equals(normalized) || "AVAILABLE".equals(normalized) || "OK".equals(normalized)) {
            return "可兑换";
        }
        if ("EXPIRED".equals(normalized)) {
            return "报价已过期";
        }
        if ("LIMIT_EXCEEDED".equals(normalized)) {
            return "超出兑换限额";
        }
        if ("UNAVAILABLE".equals(normalized) || normalized.isEmpty() || "--".equals(normalized)) {
            return "暂不可兑换";
        }
        return "状态待确认";
    }
    public String getReasonCode() { return reasonCode; }
    public String getNotes() { return notes; }
    public String getInputQuantity() { return inputQuantity; }
    public String getNominalFaceValue() { return nominalFaceValue; }
    public String getEffectiveExchangeValue() { return effectiveExchangeValue; }
    public String getContributionValue() { return contributionValue; }
    public String getDiscountStatus() { return discountStatus; }
    public String getRateDisplay() { return rateDisplay; }
    public String getExecutionHint() { return executionHint; }
    public boolean isExecutable() { return executable; }
    public ActionFeedbackModel getActionFeedback() { return actionFeedback; }
    public List<TerminalMarketBrowseEntry> getBrowseEntries() { return browseEntries; }
    public String getBrowseQuery() { return browseQuery; }
    public int getBrowsePageIndex() { return browsePageIndex; }
    public int getBrowsePageSize() { return browsePageSize; }
    public int getBrowseTotalEntries() { return browseTotalEntries; }
    public boolean hasPreviousPage() { return hasPreviousPage; }
    public boolean hasNextPage() { return hasNextPage; }
    public boolean hasTargetOptions() { return !targetCodes.isEmpty(); }
    public boolean hasSelectedTarget() { return !selectedTargetCode.isEmpty(); }
    public boolean hasFormalQuote() { return !"--".equals(pairCode); }
    public String getDisabledReason() {
        if (!hasTargetOptions()) {
            return "当前没有可选兑换标的。";
        }
        if (!hasSelectedTarget()) {
            return "先从左侧选择兑换标的。";
        }
        if (!hasFormalQuote()) {
            return notes;
        }
        if (!executable) {
            return executionHint;
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

    private static String normalize(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    public static final class ActionFeedbackModel {
        private final String title;
        private final String body;
        private final String severityName;

        public ActionFeedbackModel(String title, String body, String severityName) {
            this.title = normalize(title, "汇率市场动作反馈");
            this.body = normalize(body, "当前没有汇率市场动作反馈。");
            this.severityName = normalize(severityName, TerminalNotificationSeverity.INFO.name());
        }

        public static ActionFeedbackModel placeholder() {
            return new ActionFeedbackModel("汇率市场动作反馈", "当前没有汇率市场动作反馈。",
                TerminalNotificationSeverity.INFO.name());
        }

        public String getTitle() { return title; }
        public String getBody() { return body; }
        public String getSeverityName() { return severityName; }
    }
}
