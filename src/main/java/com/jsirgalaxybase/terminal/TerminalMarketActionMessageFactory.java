package com.jsirgalaxybase.terminal;

import com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.network.TerminalActionMessage;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public final class TerminalMarketActionMessageFactory {

    private TerminalMarketActionMessageFactory() {}

    public static TerminalActionMessage createConfirmLimitBuyMessage(TerminalHomeScreenModel screenModel,
        TerminalMarketSectionModel marketModel, TerminalMarketSectionState marketSectionState) {
        if (screenModel == null || marketModel == null || marketSectionState == null) {
            return null;
        }
        if (TerminalPage.MARKET_STANDARDIZED != TerminalPage.fromId(screenModel.getSelectedPageId())) {
            return null;
        }
        if (!marketSectionState.hasCompleteLimitBuyDraft()) {
            return null;
        }
        return new TerminalActionMessage(
            screenModel.getSessionToken(),
            screenModel.getSelectedPageId(),
            TerminalActionType.fromId("market_confirm_limit_buy").getId(),
            marketSectionState.toPayload().encode());
    }

    public static TerminalActionMessage createConfirmDepositHeldMessage(TerminalHomeScreenModel screenModel,
        TerminalMarketSectionModel marketModel, TerminalMarketSectionState marketSectionState) {
        if (!isStandardizedMarket(screenModel, marketModel, marketSectionState)
            || marketSectionState.getVaultDepositQuantityText().isEmpty()
            || marketSectionState.getVaultDepositQuantityText().equals("0")) {
            return null;
        }
        return createMarketActionMessage(screenModel, TerminalActionType.MARKET_CONFIRM_DEPOSIT_HELD, marketSectionState);
    }

    public static TerminalActionMessage createConfirmLimitSellMessage(TerminalHomeScreenModel screenModel,
        TerminalMarketSectionModel marketModel, TerminalMarketSectionState marketSectionState) {
        if (!isStandardizedMarket(screenModel, marketModel, marketSectionState) || !marketSectionState.hasCompleteLimitSellDraft()) {
            return null;
        }
        return createMarketActionMessage(screenModel, TerminalActionType.MARKET_CONFIRM_LIMIT_SELL, marketSectionState);
    }

    public static TerminalActionMessage createConfirmInstantBuyMessage(TerminalHomeScreenModel screenModel,
        TerminalMarketSectionModel marketModel, TerminalMarketSectionState marketSectionState) {
        if (!isStandardizedMarket(screenModel, marketModel, marketSectionState) || !marketSectionState.hasCompleteInstantBuyDraft()) {
            return null;
        }
        return createMarketActionMessage(screenModel, TerminalActionType.MARKET_CONFIRM_INSTANT_BUY, marketSectionState);
    }

    public static TerminalActionMessage createConfirmInstantSellMessage(TerminalHomeScreenModel screenModel,
        TerminalMarketSectionModel marketModel, TerminalMarketSectionState marketSectionState) {
        if (!isStandardizedMarket(screenModel, marketModel, marketSectionState) || !marketSectionState.hasCompleteInstantSellDraft()) {
            return null;
        }
        return createMarketActionMessage(screenModel, TerminalActionType.MARKET_CONFIRM_INSTANT_SELL, marketSectionState);
    }

    public static TerminalActionMessage createConfirmOrderMessage(TerminalHomeScreenModel screenModel,
        TerminalMarketSectionModel marketModel, TerminalMarketSectionState marketSectionState) {
        if (!isStandardizedMarket(screenModel, marketModel, marketSectionState)
            || !marketSectionState.hasCompleteOrderTicket()) {
            return null;
        }
        return new TerminalActionMessage(screenModel.getSessionToken(), screenModel.getSelectedPageId(),
            TerminalActionType.MARKET_CONFIRM_ORDER.getId(),
            marketSectionState.toUnifiedOrderPayload().encodeUnifiedOrder());
    }

    public static TerminalActionMessage createCancelOrderMessage(TerminalHomeScreenModel screenModel,
        TerminalMarketSectionModel marketModel, TerminalMarketSectionState marketSectionState) {
        if (!isStandardizedMarket(screenModel, marketModel, marketSectionState)
            || !marketSectionState.hasPendingCancelOrderSelection()) {
            return null;
        }
        return createMarketActionMessage(screenModel, TerminalActionType.MARKET_CANCEL_ORDER, marketSectionState);
    }

    public static TerminalActionMessage createClaimMessage(TerminalHomeScreenModel screenModel,
        TerminalMarketSectionModel marketModel, TerminalMarketSectionState marketSectionState) {
        if (screenModel == null || marketModel == null || marketSectionState == null) {
            return null;
        }
        if (!marketSectionState.hasPendingClaimSelection()) {
            return null;
        }
        return new TerminalActionMessage(
            screenModel.getSessionToken(),
            screenModel.getSelectedPageId(),
            TerminalActionType.fromId("market_claim_asset").getId(),
            marketSectionState.toPayload().encode());
    }

    private static boolean isStandardizedMarket(TerminalHomeScreenModel screenModel, TerminalMarketSectionModel marketModel,
        TerminalMarketSectionState marketSectionState) {
        if (screenModel == null || marketModel == null || marketSectionState == null) {
            return false;
        }
        return TerminalPage.MARKET_STANDARDIZED == TerminalPage.fromId(screenModel.getSelectedPageId());
    }

    private static TerminalActionMessage createMarketActionMessage(TerminalHomeScreenModel screenModel,
        TerminalActionType actionType, TerminalMarketSectionState marketSectionState) {
        return new TerminalActionMessage(
            screenModel.getSessionToken(),
            screenModel.getSelectedPageId(),
            actionType.getId(),
            marketSectionState.toPayload().encode());
    }

    public static TerminalActionMessage createCustomListingActionMessage(TerminalHomeScreenModel screenModel,
        TerminalMarketSectionState marketSectionState, TerminalActionType actionType) {
        if (screenModel == null || marketSectionState == null || actionType == null) {
            return null;
        }
        if (TerminalPage.MARKET_CUSTOM != TerminalPage.fromId(screenModel.getSelectedPageId())) {
            return null;
        }
        if (!marketSectionState.getCustomState().hasSelectedListing()) {
            return null;
        }
        if (actionType != TerminalActionType.MARKET_CUSTOM_BUY_LISTING
            && actionType != TerminalActionType.MARKET_CUSTOM_CANCEL_LISTING
            && actionType != TerminalActionType.MARKET_CUSTOM_CLAIM_LISTING) {
            return null;
        }
        return new TerminalActionMessage(
            screenModel.getSessionToken(),
            screenModel.getSelectedPageId(),
            actionType.getId(),
            marketSectionState.getCustomState().toPayload().encode());
    }

    public static TerminalActionMessage createExchangeConfirmMessage(TerminalHomeScreenModel screenModel,
        TerminalMarketSectionState marketSectionState) {
        if (screenModel == null || marketSectionState == null) {
            return null;
        }
        if (TerminalPage.MARKET_EXCHANGE != TerminalPage.fromId(screenModel.getSelectedPageId())
            || !marketSectionState.getExchangeState().hasSelectedTarget()) {
            return null;
        }
        return new TerminalActionMessage(
            screenModel.getSessionToken(),
            screenModel.getSelectedPageId(),
            TerminalActionType.MARKET_EXCHANGE_CONFIRM.getId(),
            marketSectionState.getExchangeState().toPayload().encode());
    }
}
