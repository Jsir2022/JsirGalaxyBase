package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.terminal.TerminalExchangeMarketActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;

public final class TerminalExchangeMarketSectionState {

    private String selectedTargetCode = "";
    private final MarketBrowseDetailController browser = new MarketBrowseDetailController();
    private boolean pendingDetail;
    private int selectedVaultSlot = -1;

    public void applyModel(TerminalExchangeMarketSectionModel model) {
        if (model == null) {
            selectedTargetCode = "";
            browser.reset();
            pendingDetail = false;
            selectedVaultSlot = -1;
            return;
        }
        selectedTargetCode = TerminalExchangeMarketActionPayload.TARGET_TASK_COIN.equals(model.getSelectedTargetCode())
            ? TerminalExchangeMarketActionPayload.TARGET_TASK_COIN
            : "";
        if (pendingDetail) {
            browser.openDetail(browser.getSelectedKey());
            pendingDetail = false;
        }
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
    }

    public boolean isDetailView() { return browser.isDetail(); }
    public void returnToBrowse() { browser.openBrowse(); pendingDetail = false; }
    public String getBrowserQuery() { return browser.getQuery(); }
    public void setBrowserQuery(String value) { browser.setQuery(value); }
    public int getBrowserPage() { return browser.getPageIndex(); }
    public void setBrowserPage(int value) { browser.setPageIndex(value); }
    public int getBrowserGridScrollOffset() { return browser.getGridScrollOffset(); }
    public void setBrowserGridScrollOffset(int value) { browser.setGridScrollOffset(value); }

    public boolean hasSelectedTarget() {
        return TerminalExchangeMarketActionPayload.TARGET_TASK_COIN.equals(selectedTargetCode);
    }

    public int getSelectedVaultSlot() { return selectedVaultSlot; }
    public void setSelectedVaultSlot(int slotIndex) { selectedVaultSlot = slotIndex < 0 ? -1 : slotIndex; }
}
