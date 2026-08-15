package com.jsirgalaxybase.terminal.ui;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.modules.core.market.application.TaskCoinCatalog;

final class TerminalExchangeMarketSnapshot {

    final String serviceState;
    final String browserHint;
    final String[] targetCodes;
    final String[] targetLabels;
    final String selectedTargetCode;
    final String selectedCoinCode;
    final String selectedTargetTitle;
    final String selectedTargetSummary;
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
    final String rateDisplay;
    final String executionHint;
    final String executableFlag;
    final List<TaskCoinCatalog.Entry> catalogEntries;

    TerminalExchangeMarketSnapshot(String serviceState, String browserHint, String[] targetCodes,
        String[] targetLabels, String selectedTargetCode, String selectedCoinCode, String selectedTargetTitle,
        String selectedTargetSummary, String heldSummary, String inputRegistryName, String pairCode,
        String inputAssetCode, String outputAssetCode, String ruleVersion, String limitStatus,
        String reasonCode, String notes, String inputQuantity, String nominalFaceValue,
        String effectiveExchangeValue, String contributionValue, String discountStatus,
        String rateDisplay, String executionHint, String executableFlag, List<TaskCoinCatalog.Entry> catalogEntries) {
        this.serviceState = serviceState;
        this.browserHint = browserHint;
        this.targetCodes = targetCodes;
        this.targetLabels = targetLabels;
        this.selectedTargetCode = selectedTargetCode;
        this.selectedCoinCode = selectedCoinCode;
        this.selectedTargetTitle = selectedTargetTitle;
        this.selectedTargetSummary = selectedTargetSummary;
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
        this.rateDisplay = rateDisplay;
        this.executionHint = executionHint;
        this.executableFlag = executableFlag;
        this.catalogEntries = catalogEntries == null ? Collections.<TaskCoinCatalog.Entry>emptyList()
            : Collections.unmodifiableList(new ArrayList<TaskCoinCatalog.Entry>(catalogEntries));
    }
}
