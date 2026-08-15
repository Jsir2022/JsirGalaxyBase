package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.terminal.TerminalExchangeMarketActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;

public final class TerminalExchangeMarketSectionState {

    private String selectedTargetCode = "";
    private final MarketBrowseDetailController browser = new MarketBrowseDetailController();
    private boolean pendingDetail;
    private String pendingDetailCoinCode = "";
    private int selectedVaultSlot = -1;
    private boolean browserQueryFocused;

    public void applyModel(TerminalExchangeMarketSectionModel model) {
        if (model == null) {
            selectedTargetCode = "";
            browser.reset();
            pendingDetail = false;
            pendingDetailCoinCode = "";
            selectedVaultSlot = -1;
            browserQueryFocused = false;
            return;
        }
        selectedTargetCode = TerminalExchangeMarketActionPayload.TARGET_TASK_COIN.equals(model.getSelectedTargetCode())
            ? TerminalExchangeMarketActionPayload.TARGET_TASK_COIN
            : "";
        if (pendingDetail && pendingDetailCoinCode.equals(model.getSelectedCoinCode())) {
            browser.openDetail(pendingDetailCoinCode);
            pendingDetail = false;
            pendingDetailCoinCode = "";
        }
    }

    /** Rejects a late detail snapshot before it can replace the screen model. */
    public boolean acceptsModel(TerminalExchangeMarketSectionModel model) {
        if (model == null) {
            return true;
        }
        String responseCoinCode = normalize(model.getSelectedCoinCode());
        if (pendingDetail) {
            return pendingDetailCoinCode.equals(responseCoinCode);
        }
        if (browser.isDetail()) {
            return browser.getSelectedKey().equals(responseCoinCode);
        }
        return browser.getQuery().equals(normalize(model.getBrowseQuery()))
            && browser.getPageIndex() == Math.max(0, model.getBrowsePageIndex());
    }

    public TerminalExchangeMarketActionPayload toPayload() {
        return new TerminalExchangeMarketActionPayload(selectedTargetCode, browser.getSelectedKey(), browser.getQuery(),
            browser.getPageIndex(), selectedVaultSlot);
    }

    public String getSelectedTargetCode() {
        return selectedTargetCode;
    }

    public void setSelectedTargetCode(String selectedTargetCode) {
        this.selectedTargetCode = TerminalExchangeMarketActionPayload.TARGET_TASK_COIN.equals(selectedTargetCode)
            ? TerminalExchangeMarketActionPayload.TARGET_TASK_COIN
            : "";
    }

    public void requestDetail(String coinCode) {
        browser.setSelectedKey(coinCode);
        selectedTargetCode = TerminalExchangeMarketActionPayload.TARGET_TASK_COIN;
        pendingDetail = true;
        pendingDetailCoinCode = browser.getSelectedKey();
    }

    public boolean isDetailView() { return browser.isDetail(); }
    public void returnToBrowse() {
        browser.openBrowse();
        pendingDetail = false;
        pendingDetailCoinCode = "";
        browserQueryFocused = false;
    }
    public String getBrowserQuery() { return browser.getQuery(); }
    public void setBrowserQuery(String value) { browser.setQuery(value); }
    public boolean isBrowserQueryFocused() { return browserQueryFocused; }
    public void focusBrowserQuery() { browserQueryFocused = true; }
    public int getBrowserPage() { return browser.getPageIndex(); }
    public void setBrowserPage(int value) { browser.setPageIndex(value); }
    public int getBrowserGridScrollOffset() { return browser.getGridScrollOffset(); }
    public void setBrowserGridScrollOffset(int value) { browser.setGridScrollOffset(value); }

    public boolean hasSelectedTarget() {
        return TerminalExchangeMarketActionPayload.TARGET_TASK_COIN.equals(selectedTargetCode);
    }

    public int getSelectedVaultSlot() { return selectedVaultSlot; }
    public void setSelectedVaultSlot(int slotIndex) { selectedVaultSlot = slotIndex < 0 ? -1 : slotIndex; }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
