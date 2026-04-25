package com.jsirgalaxybase.terminal;

import com.jsirgalaxybase.terminal.client.component.TerminalBankSectionState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.network.TerminalActionMessage;

public final class TerminalBankActionMessageFactory {

    private TerminalBankActionMessageFactory() {}

    public static TerminalActionMessage createConfirmTransferMessage(TerminalHomeScreenModel screenModel,
        TerminalBankSectionModel bankModel, TerminalBankSectionState bankSectionState) {
        if (screenModel == null || bankModel == null || bankSectionState == null) {
            return null;
        }
        if (!bankModel.getTransferForm().isTransferEnabled() || !bankSectionState.hasCompleteTransferDraft()) {
            return null;
        }
        return new TerminalActionMessage(
            screenModel.getSessionToken(),
            screenModel.getSelectedPageId(),
            TerminalActionType.BANK_CONFIRM_TRANSFER.getId(),
            bankSectionState.toPayload().encode());
    }
}