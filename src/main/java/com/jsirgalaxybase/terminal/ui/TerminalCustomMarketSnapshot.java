package com.jsirgalaxybase.terminal.ui;

final class TerminalCustomMarketSnapshot {

    final String serviceState;
    final String browserHint;
    final String scopeLabel;
    final String[] activeListingLines;
    final String[] activeListingIds;
    final String[] activeListingIconRefs;
    final String[] sellingListingLines;
    final String[] sellingListingIds;
    final String[] sellingListingIconRefs;
    final String[] pendingListingLines;
    final String[] pendingListingIds;
    final String[] pendingListingIconRefs;
    final String selectedListingId;
    final String selectedTitle;
    final String selectedPrice;
    final String selectedStatus;
    final String selectedCounterparty;
    final String selectedItemIdentity;
    final String selectedTradeSummary;
    final String selectedActionHint;
    final String selectedCanBuyFlag;
    final String selectedCanCancelFlag;
    final String selectedCanClaimFlag;

    TerminalCustomMarketSnapshot(String serviceState, String browserHint, String scopeLabel,
        String[] activeListingLines, String[] activeListingIds, String[] activeListingIconRefs,
        String[] sellingListingLines, String[] sellingListingIds, String[] sellingListingIconRefs,
        String[] pendingListingLines, String[] pendingListingIds, String[] pendingListingIconRefs,
        String selectedListingId, String selectedTitle, String selectedPrice, String selectedStatus,
        String selectedCounterparty, String selectedItemIdentity, String selectedTradeSummary,
        String selectedActionHint, String selectedCanBuyFlag, String selectedCanCancelFlag,
        String selectedCanClaimFlag) {
        this.serviceState = serviceState;
        this.browserHint = browserHint;
        this.scopeLabel = scopeLabel;
        this.activeListingLines = activeListingLines;
        this.activeListingIds = activeListingIds;
        this.activeListingIconRefs = activeListingIconRefs;
        this.sellingListingLines = sellingListingLines;
        this.sellingListingIds = sellingListingIds;
        this.sellingListingIconRefs = sellingListingIconRefs;
        this.pendingListingLines = pendingListingLines;
        this.pendingListingIds = pendingListingIds;
        this.pendingListingIconRefs = pendingListingIconRefs;
        this.selectedListingId = selectedListingId;
        this.selectedTitle = selectedTitle;
        this.selectedPrice = selectedPrice;
        this.selectedStatus = selectedStatus;
        this.selectedCounterparty = selectedCounterparty;
        this.selectedItemIdentity = selectedItemIdentity;
        this.selectedTradeSummary = selectedTradeSummary;
        this.selectedActionHint = selectedActionHint;
        this.selectedCanBuyFlag = selectedCanBuyFlag;
        this.selectedCanCancelFlag = selectedCanCancelFlag;
        this.selectedCanClaimFlag = selectedCanClaimFlag;
    }
}
