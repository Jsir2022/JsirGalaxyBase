package com.jsirgalaxybase.terminal.client.ui2;

import com.jsirgalaxybase.terminal.client.settings.TerminalNotificationAction;
import com.jsirgalaxybase.terminal.client.settings.TerminalNotificationReducer;
import com.jsirgalaxybase.terminal.client.settings.TerminalNotificationUiState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.ui.TerminalPage;
import com.jsirgalaxybase.ui2.state.UiStore;

/** Registry of terminal routes that have a production UI2 document. */
public final class TerminalPageRegistry {
    public interface Invalidation { void invalidate(); }
    public interface BusinessActions extends TerminalNotificationDocument.TargetHandler, TerminalBankDocument.Actions,
        TerminalServerToolsDocument.Actions, TerminalStandardMarketDocument.Actions, TerminalLandDocument.Actions,
        TerminalQuestCenterDocument.Actions {}

    private TerminalPageRegistry() {}

    public static boolean supports(String pageId) {
        TerminalPage page = TerminalPage.fromId(pageId);
        return page == TerminalPage.HOME || page == TerminalPage.NOTIFICATIONS || page.isBankPage()
            || page == TerminalPage.SERVER_TOOLS || page == TerminalPage.MARKET
            || page == TerminalPage.MARKET_STANDARDIZED || page == TerminalPage.MARKET_CUSTOM
            || page == TerminalPage.MARKET_EXCHANGE || page == TerminalPage.MARKET_ACCOUNT_CENTER
            || page == TerminalPage.CAREER || page == TerminalPage.PUBLIC_SERVICE
            || page == TerminalPage.ITEM_POLICY || page == TerminalPage.PROPERTY;
    }

    public static TerminalPageDocument create(String pageId, TerminalHomeScreenModel model,
        TerminalAppShell.Actions actions, BusinessActions businessActions,
        final Invalidation invalidation) {
        if (TerminalPage.NOTIFICATIONS.getId().equals(pageId)) {
            UiStore<TerminalNotificationUiState, TerminalNotificationAction> store =
                new UiStore<TerminalNotificationUiState, TerminalNotificationAction>(
                    TerminalNotificationUiState.initial(), new TerminalNotificationReducer());
            store.addListener(new UiStore.Listener<TerminalNotificationUiState>() {
                @Override public void onStateChanged(TerminalNotificationUiState state) {
                    invalidation.invalidate();
                }
            });
            return new TerminalNotificationDocument(store, model, actions, businessActions);
        }
        if (TerminalPage.fromId(pageId).isBankPage()) {
            com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel bank =
                model.getPageSnapshot(TerminalPage.BANK.getId()).getBankSectionModel();
            UiStore<BankUiState, BankUiAction> store = new UiStore<BankUiState, BankUiAction>(
                BankUiState.initial(bank == null ? null : bank.getTransferForm()), new BankUiReducer());
            store.addListener(new UiStore.Listener<BankUiState>() {
                @Override public void onStateChanged(BankUiState state) { invalidation.invalidate(); }
            });
            return new TerminalBankDocument(store, model, actions, businessActions);
        }
        if (TerminalPage.SERVER_TOOLS.getId().equals(pageId)) {
            com.jsirgalaxybase.terminal.client.viewmodel.TerminalServerToolsSectionModel tools =
                model.getPageSnapshot(TerminalPage.SERVER_TOOLS.getId()).getServerToolsSectionModel();
            UiStore<ServerToolsUiState, ServerToolsUiAction> store =
                new UiStore<ServerToolsUiState, ServerToolsUiAction>(ServerToolsUiState.initial(tools),
                    new ServerToolsUiReducer());
            store.addListener(new UiStore.Listener<ServerToolsUiState>() {
                @Override public void onStateChanged(ServerToolsUiState state) { invalidation.invalidate(); }
            });
            return new TerminalServerToolsDocument(store, model, actions, businessActions);
        }
        if (TerminalPage.MARKET.getId().equals(pageId)) {
            return new TerminalMarketOverviewDocument(model, actions, businessActions);
        }
        if (TerminalPage.MARKET_STANDARDIZED.getId().equals(pageId)) {
            com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState state =
                new com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState();
            return new TerminalStandardMarketDocument(state, model, actions, businessActions,
                new Runnable() { @Override public void run() { invalidation.invalidate(); } });
        }
        if (TerminalPage.MARKET_ACCOUNT_CENTER.getId().equals(pageId)) {
            com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState state =
                new com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState();
            return new TerminalMarketAccountDocument(state, model, actions, businessActions,
                new Runnable() { @Override public void run() { invalidation.invalidate(); } });
        }
        if (TerminalPage.MARKET_CUSTOM.getId().equals(pageId)) {
            com.jsirgalaxybase.terminal.client.component.TerminalCustomMarketSectionState state =
                new com.jsirgalaxybase.terminal.client.component.TerminalCustomMarketSectionState();
            return new TerminalCustomMarketDocument(state, model, actions, businessActions,
                new Runnable() { @Override public void run() { invalidation.invalidate(); } });
        }
        if (TerminalPage.MARKET_EXCHANGE.getId().equals(pageId)) {
            com.jsirgalaxybase.terminal.client.component.TerminalExchangeMarketSectionState state =
                new com.jsirgalaxybase.terminal.client.component.TerminalExchangeMarketSectionState();
            return new TerminalExchangeMarketDocument(state, model, actions, businessActions,
                new Runnable() { @Override public void run() { invalidation.invalidate(); } });
        }
        TerminalPage page = TerminalPage.fromId(pageId);
        if (page == TerminalPage.CAREER) {
            return new TerminalQuestCenterDocument(model, actions, businessActions,
                new Runnable(){public void run(){invalidation.invalidate();}});
        }
        if (page == TerminalPage.PUBLIC_SERVICE || page == TerminalPage.ITEM_POLICY) {
            return new TerminalSummaryDocument(pageId, model, actions);
        }
        if (page == TerminalPage.PROPERTY) {
            com.jsirgalaxybase.terminal.client.component.TerminalLandSectionState state =
                new com.jsirgalaxybase.terminal.client.component.TerminalLandSectionState();
            return new TerminalLandDocument(state, model, actions, businessActions,
                new Runnable() { @Override public void run() { invalidation.invalidate(); } });
        }
        return new TerminalHomeDocument(model, actions);
    }
}
