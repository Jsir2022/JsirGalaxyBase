package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.terminal.TerminalExchangeMarketActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;

public final class TerminalExchangeMarketSectionState {

    private String selectedTargetCode = "";

    public void applyModel(TerminalExchangeMarketSectionModel model) {
        if (model == null) {
            selectedTargetCode = "";
            return;
        }
        selectedTargetCode = TerminalExchangeMarketActionPayload.TARGET_TASK_COIN.equals(model.getSelectedTargetCode())
            ? TerminalExchangeMarketActionPayload.TARGET_TASK_COIN
            : "";
    }

    public TerminalExchangeMarketActionPayload toPayload() {
        return new TerminalExchangeMarketActionPayload(selectedTargetCode);
    }

    public String getSelectedTargetCode() {
        return selectedTargetCode;
    }

    public void setSelectedTargetCode(String selectedTargetCode) {
        this.selectedTargetCode = TerminalExchangeMarketActionPayload.TARGET_TASK_COIN.equals(selectedTargetCode)
            ? TerminalExchangeMarketActionPayload.TARGET_TASK_COIN
            : "";
    }

    public boolean hasSelectedTarget() {
        return TerminalExchangeMarketActionPayload.TARGET_TASK_COIN.equals(selectedTargetCode);
    }
}
