package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

public final class TerminalExchangeMarketSectionSnapshot {

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
    private final ActionFeedback actionFeedback;
    private final List<TerminalMarketBrowseEntry> browseEntries;
    private final String browseQuery;
    private final int browsePageIndex;
    private final int browsePageSize;
    private final int browseTotalEntries;
    private final boolean hasPreviousPage;
    private final boolean hasNextPage;

    public TerminalExchangeMarketSectionSnapshot(String serviceState, String browserHint, List<String> targetCodes,
        List<String> targetLabels, String selectedTargetCode, String selectedTargetTitle,
        String selectedTargetSummary, String heldSummary, String inputRegistryName, String pairCode,
        String inputAssetCode, String outputAssetCode, String ruleVersion, String limitStatus, String reasonCode,
        String notes, String inputQuantity, String nominalFaceValue, String effectiveExchangeValue,
        String contributionValue, String discountStatus, String rateDisplay, String executionHint,
        boolean executable, ActionFeedback actionFeedback) {
        this(serviceState, browserHint, targetCodes, targetLabels, selectedTargetCode, "", selectedTargetTitle,
            selectedTargetSummary, heldSummary, inputRegistryName, pairCode, inputAssetCode, outputAssetCode,
            ruleVersion, limitStatus, reasonCode, notes, inputQuantity, nominalFaceValue, effectiveExchangeValue,
            contributionValue, discountStatus, rateDisplay, executionHint, executable, actionFeedback);
    }

    public TerminalExchangeMarketSectionSnapshot(String serviceState, String browserHint, List<String> targetCodes,
        List<String> targetLabels, String selectedTargetCode, String selectedCoinCode, String selectedTargetTitle,
        String selectedTargetSummary, String heldSummary, String inputRegistryName, String pairCode,
        String inputAssetCode, String outputAssetCode, String ruleVersion, String limitStatus, String reasonCode,
        String notes, String inputQuantity, String nominalFaceValue, String effectiveExchangeValue,
        String contributionValue, String discountStatus, String rateDisplay, String executionHint,
        boolean executable, ActionFeedback actionFeedback) {
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
        this.actionFeedback = actionFeedback == null ? ActionFeedback.placeholder() : actionFeedback;
        this.browseEntries = Collections.emptyList();
        this.browseQuery = "";
        this.browsePageIndex = 0;
        this.browsePageSize = 12;
        this.browseTotalEntries = 0;
        this.hasPreviousPage = false;
        this.hasNextPage = false;
    }

    private TerminalExchangeMarketSectionSnapshot(TerminalExchangeMarketSectionSnapshot source,
        List<TerminalMarketBrowseEntry> browseEntries, String browseQuery, int browsePageIndex,
        int browsePageSize, int browseTotalEntries, boolean hasPreviousPage, boolean hasNextPage) {
        this.serviceState = source.serviceState;
        this.browserHint = source.browserHint;
        this.targetCodes = source.targetCodes;
        this.targetLabels = source.targetLabels;
        this.selectedTargetCode = source.selectedTargetCode;
        this.selectedCoinCode = source.selectedCoinCode;
        this.selectedTargetTitle = source.selectedTargetTitle;
        this.selectedTargetSummary = source.selectedTargetSummary;
        this.heldSummary = source.heldSummary;
        this.inputRegistryName = source.inputRegistryName;
        this.pairCode = source.pairCode;
        this.inputAssetCode = source.inputAssetCode;
        this.outputAssetCode = source.outputAssetCode;
        this.ruleVersion = source.ruleVersion;
        this.limitStatus = source.limitStatus;
        this.reasonCode = source.reasonCode;
        this.notes = source.notes;
        this.inputQuantity = source.inputQuantity;
        this.nominalFaceValue = source.nominalFaceValue;
        this.effectiveExchangeValue = source.effectiveExchangeValue;
        this.contributionValue = source.contributionValue;
        this.discountStatus = source.discountStatus;
        this.rateDisplay = source.rateDisplay;
        this.executionHint = source.executionHint;
        this.executable = source.executable;
        this.actionFeedback = source.actionFeedback;
        this.browseEntries = freeze(browseEntries, Collections.<TerminalMarketBrowseEntry>emptyList());
        this.browseQuery = normalize(browseQuery, "");
        this.browsePageIndex = Math.max(0, browsePageIndex);
        this.browsePageSize = Math.max(1, browsePageSize);
        this.browseTotalEntries = Math.max(0, browseTotalEntries);
        this.hasPreviousPage = hasPreviousPage;
        this.hasNextPage = hasNextPage;
    }

    public TerminalExchangeMarketSectionSnapshot withBrowsePage(List<TerminalMarketBrowseEntry> entries,
        String query, int pageIndex, int pageSize, int totalEntries, boolean previous, boolean next) {
        return new TerminalExchangeMarketSectionSnapshot(this, entries, query, pageIndex, pageSize, totalEntries,
            previous, next);
    }

    public static TerminalExchangeMarketSectionSnapshot placeholder() {
        return new TerminalExchangeMarketSectionSnapshot(
            "汇率市场正在连接",
            "正在读取兑换目录，请稍候。",
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
            ActionFeedback.placeholder());
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
    public ActionFeedback getActionFeedback() { return actionFeedback; }
    public List<TerminalMarketBrowseEntry> getBrowseEntries() { return browseEntries; }
    public String getBrowseQuery() { return browseQuery; }
    public int getBrowsePageIndex() { return browsePageIndex; }
    public int getBrowsePageSize() { return browsePageSize; }
    public int getBrowseTotalEntries() { return browseTotalEntries; }
    public boolean hasPreviousPage() { return hasPreviousPage; }
    public boolean hasNextPage() { return hasNextPage; }

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
            this.title = normalize(title, "汇率市场动作反馈");
            this.body = normalize(body, "当前没有汇率市场动作反馈。");
            this.severityName = normalize(severityName, TerminalNotificationSeverity.INFO.name());
        }

        public static ActionFeedback placeholder() {
            return new ActionFeedback("汇率市场动作反馈", "当前没有汇率市场动作反馈。",
                TerminalNotificationSeverity.INFO.name());
        }

        public String getTitle() { return title; }
        public String getBody() { return body; }
        public String getSeverityName() { return severityName; }
    }
}
