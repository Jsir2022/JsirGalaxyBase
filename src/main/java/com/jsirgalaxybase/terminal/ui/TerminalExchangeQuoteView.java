package com.jsirgalaxybase.terminal.ui;

import java.util.Locale;

import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketLimitStatus;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketQuoteResult;

final class TerminalExchangeQuoteView {

    final String serviceState;
    final String heldSummary;
    final String inputRegistryName;
    final String pairCode;
    final String inputAssetCode;
    final String outputAssetCode;
    final String ruleVersion;
    final String limitStatus;
    final String reasonCode;
    final String notes;
    final String inputQuantity;
    final String nominalFaceValue;
    final String effectiveExchangeValue;
    final String contributionValue;
    final String discountStatus;
    final String exchangeRateDisplay;
    final String executionHint;
    final String executableFlag;

    private TerminalExchangeQuoteView(String serviceState, String heldSummary, String inputRegistryName,
        String pairCode, String inputAssetCode, String outputAssetCode, String ruleVersion, String limitStatus,
        String reasonCode, String notes, String inputQuantity, String nominalFaceValue,
        String effectiveExchangeValue, String contributionValue, String discountStatus, String exchangeRateDisplay,
        String executionHint, String executableFlag) {
        this.serviceState = serviceState;
        this.heldSummary = heldSummary;
        this.inputRegistryName = inputRegistryName;
        this.pairCode = pairCode;
        this.inputAssetCode = inputAssetCode;
        this.outputAssetCode = outputAssetCode;
        this.ruleVersion = ruleVersion;
        this.limitStatus = limitStatus;
        this.reasonCode = reasonCode;
        this.notes = notes;
        this.inputQuantity = inputQuantity;
        this.nominalFaceValue = nominalFaceValue;
        this.effectiveExchangeValue = effectiveExchangeValue;
        this.contributionValue = contributionValue;
        this.discountStatus = discountStatus;
        this.exchangeRateDisplay = exchangeRateDisplay;
        this.executionHint = executionHint;
        this.executableFlag = executableFlag;
    }

    static TerminalExchangeQuoteView fromQuote(String heldSummary, ExchangeMarketQuoteResult quote) {
        ExchangeMarketLimitStatus status = quote.getLimitPolicy().getStatus();
        String serviceState;
        String executionHint;
        switch (status) {
            case DISCOUNTED:
                serviceState = "汇率市场在线 / 已生成折扣报价";
                executionHint = "确认后将按折扣报价执行，本次预计到账 "
                    + formatAmount(quote.getEffectiveExchangeValue()) + " STARCOIN。";
                break;
            case DISALLOWED:
                serviceState = "汇率市场在线 / 当前输入禁兑";
                executionHint = "当前输入已被正式禁兑，不能继续执行。";
                break;
            case ALLOWED:
            default:
                serviceState = "汇率市场在线 / 已生成正式报价";
                executionHint = "确认后将把当前手持硬币兑换为 "
                    + formatAmount(quote.getEffectiveExchangeValue()) + " STARCOIN。";
                break;
        }
        return new TerminalExchangeQuoteView(
            serviceState,
            safeText(heldSummary, "当前未检测到手持物品"),
            safeText(quote.getInputRegistryName(), "--"),
            safeText(quote.getPairDefinition().getPairCode(), "--"),
            safeText(quote.getPairDefinition().getInputAssetCode(), "--"),
            safeText(quote.getPairDefinition().getOutputAssetCode(), "--"),
            safeText(quote.getRuleVersion().getRuleKey(), "--"),
            status.name(),
            safeText(quote.getLimitPolicy().getReasonCode(), "--"),
            safeText(quote.getNotes(), "--"),
            formatAmount(quote.getInputQuantity()),
            formatAmount(quote.getInputTotalFaceValue()),
            formatAmount(quote.getEffectiveExchangeValue()),
            formatAmount(quote.getContributionValue()),
            describeDiscountStatus(quote),
            formatAmount(quote.getExchangeRateNumerator()) + " / " + formatAmount(quote.getExchangeRateDenominator()),
            executionHint,
            quote.getLimitPolicy().isExecutable() ? "1" : "0");
    }

    static TerminalExchangeQuoteView empty(String serviceState, String heldSummary, String inputRegistryName,
        String notes, String executionHint) {
        return new TerminalExchangeQuoteView(
            safeText(serviceState, "汇率市场离线"),
            safeText(heldSummary, "当前未检测到手持物品"),
            safeText(inputRegistryName, "--"),
            "--",
            "--",
            "--",
            "--",
            "UNAVAILABLE",
            "--",
            safeText(notes, "--"),
            "0",
            "0",
            "0",
            "0",
            "当前暂无可执行报价",
            "--",
            safeText(executionHint, "当前不能继续执行兑换。"),
            "0");
    }

    boolean hasFormalQuote() {
        return !"--".equals(pairCode);
    }

    private static String describeDiscountStatus(ExchangeMarketQuoteResult quote) {
        switch (quote.getLimitPolicy().getStatus()) {
            case DISCOUNTED:
                return "折扣报价 / " + quote.getDiscountBasisPoints() + " bp";
            case DISALLOWED:
                return "禁兑 / 不允许执行";
            case ALLOWED:
            default:
                return "按面值执行 / 无折扣";
        }
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String formatAmount(long value) {
        return String.format(Locale.ROOT, "%,d", Long.valueOf(value));
    }
}