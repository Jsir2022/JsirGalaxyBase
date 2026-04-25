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

    public TerminalExchangeMarketSectionSnapshot(String serviceState, String browserHint, List<String> targetCodes,
        List<String> targetLabels, String selectedTargetCode, String selectedTargetTitle,
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
        this.selectedTargetTitle = normalize(selectedTargetTitle, "未选择兑换标的");
        this.selectedTargetSummary = normalize(selectedTargetSummary, "请选择标的后查看报价。");
        this.heldSummary = normalize(heldSummary, "当前未检测到手持物品");
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
    }

    public static TerminalExchangeMarketSectionSnapshot placeholder() {
        return new TerminalExchangeMarketSectionSnapshot(
            "汇率市场 section 已接入",
            "quote-first 页面等待服务端 snapshot。",
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            "",
            "未选择兑换标的",
            "请选择标的后查看报价。",
            "当前未检测到手持物品",
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
