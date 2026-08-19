package com.jsirgalaxybase.terminal.client.screen;

import java.util.Arrays;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import com.jsirgalaxybase.client.gui.framework.CanvasScreen;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.ModalPopupPanel;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.TerminalHudOverlayHandler;
import com.jsirgalaxybase.terminal.TerminalBankActionMessageFactory;
import com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactory;
import com.jsirgalaxybase.terminal.TerminalMarketActionPayload;
import com.jsirgalaxybase.terminal.TerminalServerToolsActionPayload;
import com.jsirgalaxybase.terminal.client.component.TerminalBankSectionState;
import com.jsirgalaxybase.terminal.client.component.CustomListingPricePopup;
import com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState;
import com.jsirgalaxybase.terminal.client.component.MarketLiveRefreshController;
import com.jsirgalaxybase.terminal.client.component.MarketOrderEntryPopup;
import com.jsirgalaxybase.terminal.client.component.MarketCancelableOrdersPopup;
import com.jsirgalaxybase.terminal.client.component.TerminalPanelFactory;
import com.jsirgalaxybase.terminal.client.component.TerminalPopupFactory;
import com.jsirgalaxybase.terminal.client.component.TerminalServerToolsSectionState;
import com.jsirgalaxybase.terminal.client.component.TerminalShellPanels;
import com.jsirgalaxybase.terminal.client.component.VaultAssetPickerPopup;
import com.jsirgalaxybase.terminal.client.TerminalResponseSequenceGate;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalServerToolsSectionModel;
import com.jsirgalaxybase.terminal.network.TerminalActionMessage;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public class TerminalHomeScreen extends CanvasScreen {

    private TerminalHomeScreenModel model;
    private final TerminalBankSectionState bankSectionState = new TerminalBankSectionState();
    private final TerminalMarketSectionState marketSectionState = new TerminalMarketSectionState();
    private final TerminalServerToolsSectionState serverToolsSectionState = new TerminalServerToolsSectionState();
    private final MarketLiveRefreshController marketLiveRefreshController = new MarketLiveRefreshController();
    private TerminalHomeScreenModel deferredLiveMarketModel;
    private long deferredLiveMarketSequence;
    private final TerminalResponseSequenceGate responseSequenceGate = new TerminalResponseSequenceGate();

    public TerminalHomeScreen(GuiScreen parentScreen, TerminalHomeScreenModel model) {
        super(parentScreen);
        this.model = model == null ? TerminalHomeScreenModel.placeholder() : model;
        syncBankSectionStateFromModel(this.model);
        syncMarketSectionStateFromModel(this.model);
        syncServerToolsSectionStateFromModel(this.model);
    }

    public void applyModel(TerminalHomeScreenModel model) {
        applyModel(model, 0L);
    }

    public void applyModel(TerminalHomeScreenModel model, long requestSequence) {
        if (!responseSequenceGate.shouldAccept(requestSequence)) {
            marketLiveRefreshController.onSnapshotReceived();
            return;
        }
        if (marketLiveRefreshController.isPending() && hasOpenPopup()) {
            // Never rebuild a confirmation dialog under the player while a background refresh arrives.
            deferredLiveMarketModel = model == null ? TerminalHomeScreenModel.placeholder() : model;
            deferredLiveMarketSequence = requestSequence;
            marketLiveRefreshController.onSnapshotReceived();
            return;
        }
        applyModelNow(model, requestSequence);
    }

    private void applyModelNow(TerminalHomeScreenModel model, long requestSequence) {
        if (!responseSequenceGate.shouldAccept(requestSequence)) {
            marketLiveRefreshController.onSnapshotReceived();
            return;
        }
        TerminalHomeScreenModel incoming = model == null ? TerminalHomeScreenModel.placeholder() : model;
        if (!acceptsIncomingMarketSnapshot(incoming)) {
            marketLiveRefreshController.onSnapshotReceived();
            return;
        }
        this.model = incoming;
        responseSequenceGate.markApplied(requestSequence);
        syncBankSectionStateFromModel(this.model);
        syncMarketSectionStateFromModel(this.model);
        syncServerToolsSectionStateFromModel(this.model);
        marketLiveRefreshController.onSnapshotReceived();
        closePopup();
        initGui();
    }

    private boolean acceptsIncomingMarketSnapshot(TerminalHomeScreenModel incoming) {
        if (incoming == null) {
            return true;
        }
        TerminalPage page = TerminalPage.fromId(incoming.getSelectedPageId());
        TerminalHomeScreenModel.PageSnapshotModel marketSnapshot = incoming.getPageSnapshot("market");
        if (page == TerminalPage.MARKET_STANDARDIZED) {
            TerminalMarketSectionModel standardizedModel = marketSnapshot == null
                ? null : marketSnapshot.getMarketSectionModel();
            return marketSectionState.acceptsModel(standardizedModel);
        }
        if (page == TerminalPage.MARKET_CUSTOM) {
            TerminalCustomMarketSectionModel customModel = marketSnapshot == null
                ? null : marketSnapshot.getCustomMarketSectionModel();
            return marketSectionState.getCustomState().acceptsModel(customModel);
        }
        if (page != TerminalPage.MARKET_EXCHANGE) {
            return true;
        }
        TerminalExchangeMarketSectionModel exchangeModel = marketSnapshot == null
            ? null : marketSnapshot.getExchangeMarketSectionModel();
        return marketSectionState.getExchangeState().acceptsModel(exchangeModel);
    }

    @Override
    public void closePopup() {
        super.closePopup();
        if (deferredLiveMarketModel != null) {
            TerminalHomeScreenModel deferredModel = deferredLiveMarketModel;
            long deferredSequence = deferredLiveMarketSequence;
            deferredLiveMarketModel = null;
            deferredLiveMarketSequence = 0L;
            applyModelNow(deferredModel, deferredSequence);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (marketLiveRefreshController.tick(canAutoRefreshCurrentMarket())) {
            sendCurrentMarketRefresh();
        }
        marketSectionState.setMarketFreshness(marketLiveRefreshController.getStatusLabel(),
            marketLiveRefreshController.getFreshness() == MarketLiveRefreshController.Freshness.STALE);
    }

    @Override
    protected boolean shouldDrawDefaultBackground() {
        return false;
    }

    @Override
    protected PanelContainer buildRootPanel() {
        PanelContainer root = new PanelContainer();
        root.setBounds(new GuiRect(0, 0, width, height));
        TerminalHomeLayout layout = TerminalHomeLayout.compute(width, height, model);

        final TerminalPanelFactory panels = new TerminalPanelFactory();
        root.addChild(new BackdropPanel(width, height, layout.panelBounds));
        root.addChild(panels.createSurface(layout.panelBounds,
            com.jsirgalaxybase.client.gui.theme.ThemeColorKey.PANEL_FILL));
        root.addChild(TerminalShellPanels.createStatusBand(
            panels,
            layout.statusBandBounds,
            model,
            new Runnable() {
                @Override
                public void run() {
                    requestRefresh();
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    openShellInfoPopup();
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    handleShellBack();
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closeScreen();
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    openAccountCenter(TerminalPage.fromId(model.getSelectedPageId()) == TerminalPage.VAULT
                        ? TerminalMarketSectionState.AccountCenterTab.ASSETS_AND_DELIVERY
                        : TerminalMarketSectionState.AccountCenterTab.OPEN_ORDERS);
                }
            }));
        root.addChild(TerminalShellPanels.createSectionBody(
            panels,
            layout.bodyBounds,
            model,
            new Runnable() {
                @Override
                public void run() {
                    requestRefresh();
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closeScreen();
                }
            },
            bankSectionState,
            new TerminalShellPanels.BankActionHandler() {
                @Override
                public void openAccount() {
                    requestOpenAccount();
                }

                @Override
                public void openTransferConfirm() {
                    openTransferConfirmPopup();
                }
            },
            marketSectionState,
            new TerminalShellPanels.MarketActionHandler() {
                @Override
                public void openMarketOverview() {
                    switchMarketRoute(TerminalPage.MARKET.getId());
                }

                @Override
                public void openMarketHelp() {
                    openShellInfoPopup();
                }

                @Override
                public void openStandardizedMarket() {
                    switchMarketRoute(TerminalPage.MARKET_STANDARDIZED.getId());
                }

                @Override
                public void openCustomMarket() {
                    switchMarketRoute(TerminalPage.MARKET_CUSTOM.getId());
                }

                @Override
                public void openExchangeMarket() {
                    switchMarketRoute(TerminalPage.MARKET_EXCHANGE.getId());
                }

                @Override
                public void selectProduct(String productKey) {
                    selectMarketProduct(productKey);
                }

                @Override
                public void refreshProductBrowser() {
                    refreshMarketBrowser();
                }

                @Override
                public void changeProductBrowserPage(int pageIndex) {
                    marketSectionState.setBrowserPage(pageIndex);
                    refreshMarketBrowser();
                }

                @Override
                public void openDepositHeldConfirm() {
                    openDepositHeldConfirmPopup();
                }

                @Override
                public void openLimitBuyConfirm() {
                    openLimitBuyConfirmPopup();
                }

                @Override
                public void openLimitSellConfirm() {
                    openLimitSellConfirmPopup();
                }

                @Override
                public void openInstantBuyConfirm() {
                    openInstantBuyConfirmPopup();
                }

                @Override
                public void openInstantSellConfirm() {
                    openInstantSellConfirmPopup();
                }

                @Override
                public void openOrderConfirm(TerminalMarketSectionState.OrderSide side,
                    TerminalMarketSectionState.OrderType type) {
                    openUnifiedOrderConfirmPopup(side, type);
                }

                @Override
                public void openCancelOrderConfirm(String orderId) {
                    if (orderId == null || orderId.trim().isEmpty()) {
                        openCurrentProductCancelPopup();
                    } else {
                        openCancelOrderConfirmPopup(orderId);
                    }
                }

                @Override
                public void openStandardizedHistory() {
                    marketSectionState.openStandardizedHistory();
                    initGui();
                    refreshStandardizedHistory();
                }

                @Override
                public void refreshStandardizedHistory() {
                    sendActionToServer(new TerminalActionMessage(
                        model.getSessionToken(),
                        model.getSelectedPageId(),
                        TerminalActionType.MARKET_REFRESH_HISTORY.getId(),
                        marketSectionState.toHistoryPayload().encodeHistory()));
                }

                @Override
                public void openClaimConfirm(String custodyId) {
                    openClaimConfirmPopup(custodyId);
                }

                @Override
                public void selectCustomListing(String scope, String listingId) {
                    selectCustomListingForRefresh(scope, listingId);
                }

                @Override
                public void refreshCustomBrowse() {
                    selectCustomListingForRefresh(marketSectionState.getCustomState().getSelectedScope(), "");
                }

                @Override
                public void changeCustomBrowsePage(int pageIndex) {
                    marketSectionState.getCustomState().setBrowserPage(pageIndex);
                    selectCustomListingForRefresh(marketSectionState.getCustomState().getSelectedScope(), "");
                }

                @Override
                public void openCustomBuyConfirm() {
                    openCustomActionConfirmPopup(TerminalActionType.MARKET_CUSTOM_BUY_LISTING);
                }

                @Override
                public void openCustomPublishConfirm() {
                    openCustomPublishConfirmPopup();
                }

                @Override
                public void openCustomCancelConfirm() {
                    openCustomActionConfirmPopup(TerminalActionType.MARKET_CUSTOM_CANCEL_LISTING);
                }

                @Override
                public void openCustomClaimConfirm() {
                    openCustomActionConfirmPopup(TerminalActionType.MARKET_CUSTOM_CLAIM_LISTING);
                }

                @Override
                public void selectExchangeTarget(String targetCode) {
                    selectExchangeTargetForRefresh(targetCode);
                }

                @Override
                public void refreshExchangeBrowse() {
                    marketSectionState.getExchangeState().returnToBrowse();
                    sendActionToServer(new TerminalActionMessage(model.getSessionToken(), TerminalPage.MARKET_EXCHANGE.getId(),
                        TerminalActionType.MARKET_REFRESH.getId(), marketSectionState.getExchangeState().toPayload().encode()));
                }

                @Override
                public void changeExchangeBrowsePage(int pageIndex) {
                    marketSectionState.getExchangeState().setBrowserPage(pageIndex);
                    refreshExchangeBrowse();
                }

                @Override
                public void refreshExchangeQuote() {
                    sendActionToServer(new TerminalActionMessage(
                        model.getSessionToken(),
                        TerminalPage.MARKET_EXCHANGE.getId(),
                        TerminalActionType.MARKET_EXCHANGE_REFRESH_QUOTE.getId(),
                        marketSectionState.getExchangeState().toPayload().encode()));
                }

                @Override
                public void openExchangeConfirm() {
                    openExchangeConfirmPopup();
                }
            },
            serverToolsSectionState,
            new TerminalShellPanels.ServerToolsActionHandler() {
                @Override
                public void refreshServerTools() {
                    requestServerToolsRefresh();
                }

                @Override
                public void selectWarp(String warpName) {
                    selectServerToolsWarp(warpName);
                }

                @Override
                public void confirmWarp(String warpName) {
                    openServerToolsWarpConfirmPopup(warpName);
                }
            }));
        root.addChild(TerminalShellPanels.createNavigationRail(
            panels,
            layout.navigationBounds,
            model,
            new TerminalShellPanels.NavigationHandler() {
                @Override
                public void open(TerminalHomeScreenModel.NavItemModel navItem) {
                    handleNavSelection(navItem);
                }
            }));
        return root;
    }

    private void handleNavSelection(TerminalHomeScreenModel.NavItemModel navItem) {
        if (navItem == null || navItem.isSelected() || !navItem.isEnabled()) {
            return;
        }
        if (TerminalPage.fromId(navItem.getPageId()) == TerminalPage.VAULT) {
            sendActionToServer(new TerminalActionMessage(
                model.getSessionToken(), navItem.getPageId(), TerminalActionType.VAULT_OPEN.getId(), "nav_click"));
            return;
        }
        applyModel(model.withSelectedPageId(navItem.getPageId()));
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            navItem.getPageId(),
            TerminalActionType.SELECT_PAGE.getId(),
            "nav_click"));
    }

    private void handleShellBack() {
        TerminalPage selected = TerminalPage.fromId(model.getSelectedPageId());
        if (selected == TerminalPage.MARKET_STANDARDIZED && marketSectionState.isStandardizedHistoryView()) {
            marketSectionState.returnToStandardizedDetail();
            initGui();
            return;
        }
        if (selected == TerminalPage.MARKET_STANDARDIZED && marketSectionState.isStandardizedDetailView()) {
            marketSectionState.returnToStandardizedBrowse();
            initGui();
            return;
        }
        if (selected == TerminalPage.MARKET_CUSTOM && marketSectionState.getCustomState().isDetailView()) {
            marketSectionState.getCustomState().returnToBrowse();
            initGui();
            return;
        }
        if (selected == TerminalPage.MARKET_EXCHANGE && marketSectionState.getExchangeState().isDetailView()) {
            marketSectionState.getExchangeState().returnToBrowse();
            initGui();
            return;
        }
        if (selected.isMarketPage() && selected != TerminalPage.MARKET) {
            switchMarketRoute(TerminalPage.MARKET.getId());
            return;
        }
        closeScreen();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!hasOpenPopup()) {
            TerminalHudOverlayHandler.INSTANCE.drawTerminalNotifications(mc.fontRenderer, width, height);
        }
    }

    private void requestRefresh() {
        if (isBankSectionSelected()) {
            sendActionToServer(new TerminalActionMessage(
                model.getSessionToken(),
                model.getSelectedPageId(),
                TerminalActionType.BANK_REFRESH.getId(),
                bankSectionState.toPayload().encode()));
            return;
        }
        if (isMarketSectionSelected()) {
            TerminalPage selected = TerminalPage.fromId(model.getSelectedPageId());
            if (selected == TerminalPage.MARKET_CUSTOM) {
                sendActionToServer(new TerminalActionMessage(
                    model.getSessionToken(),
                    model.getSelectedPageId(),
                    TerminalActionType.MARKET_CUSTOM_REFRESH.getId(),
                    marketSectionState.getCustomState().toPayload().encode()));
                return;
            }
            if (selected == TerminalPage.MARKET_EXCHANGE) {
                sendActionToServer(new TerminalActionMessage(
                    model.getSessionToken(),
                    model.getSelectedPageId(),
                    TerminalActionType.MARKET_EXCHANGE_REFRESH_QUOTE.getId(),
                    marketSectionState.getExchangeState().toPayload().encode()));
                return;
            }
            if (selected == TerminalPage.MARKET_ACCOUNT_CENTER) {
                sendActionToServer(new TerminalActionMessage(model.getSessionToken(), selected.getId(),
                    TerminalActionType.MARKET_REFRESH_HISTORY.getId(),
                    marketSectionState.toHistoryPayload().encodeHistory()));
                return;
            }
            sendActionToServer(new TerminalActionMessage(
                model.getSessionToken(),
                model.getSelectedPageId(),
                TerminalActionType.MARKET_REFRESH.getId(),
                marketSectionState.toPayload().encode()));
            return;
        }
        if (isServerToolsSectionSelected()) {
            requestServerToolsRefresh();
            return;
        }
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            model.getSelectedPageId(),
            TerminalActionType.REFRESH_PAGE.getId(),
            "manual_refresh"));
    }

    private void requestServerToolsRefresh() {
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            TerminalPage.SERVER_TOOLS.getId(),
            TerminalActionType.SERVER_TOOLS_REFRESH.getId(),
            serverToolsSectionState.toPayload().encode()));
    }

    private void requestOpenAccount() {
        TerminalBankSectionModel bankModel = getSelectedBankModel();
        if (bankModel == null || !bankModel.getAccountStatus().isOpenAllowed()) {
            return;
        }
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            model.getSelectedPageId(),
            TerminalActionType.BANK_OPEN_ACCOUNT.getId(),
            bankSectionState.toPayload().encode()));
    }

    private void openTransferConfirmPopup() {
        TerminalBankSectionModel bankModel = getSelectedBankModel();
        if (bankModel == null || !bankModel.getTransferForm().isTransferEnabled() || !bankSectionState.hasCompleteTransferDraft()) {
            return;
        }
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认提交玩家转账",
            "确认后将按当前表单发起玩家转账，并在服务端处理完成后刷新银行页面。",
            Arrays.asList(
                "目标玩家: " + bankSectionState.getTargetPlayerName(),
                "转账金额: " + bankSectionState.getAmountText() + " STARCOIN",
                "备注说明: " + (bankSectionState.getComment().isEmpty() ? "terminal transfer" : bankSectionState.getComment()),
                "当前余额: " + bankModel.getBalanceSummary().getPlayerBalance()),
            "确认转账",
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    confirmTransfer();
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private void confirmTransfer() {
        closePopup();
        TerminalActionMessage message = TerminalBankActionMessageFactory.createConfirmTransferMessage(
            model,
            getSelectedBankModel(),
            bankSectionState);
        if (message != null) {
            sendActionToServer(message);
        }
    }

    private void switchMarketRoute(String pageId) {
        TerminalPage targetPage = TerminalPage.fromId(pageId);
        if (!targetPage.isMarketPage()) {
            return;
        }
        applyModel(model.withSelectedPageId(targetPage.getId()));
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            targetPage.getId(),
            TerminalActionType.SELECT_PAGE.getId(),
            targetPage == TerminalPage.MARKET_CUSTOM ? marketSectionState.getCustomState().toPayload().encode()
                : targetPage == TerminalPage.MARKET_EXCHANGE ? marketSectionState.getExchangeState().toPayload().encode()
                    : targetPage == TerminalPage.MARKET_ACCOUNT_CENTER ? marketSectionState.toHistoryPayload().encodeHistory()
                        : marketSectionState.toPayload().encode()));
    }

    private void openAccountCenter(TerminalMarketSectionState.AccountCenterTab tab) {
        marketSectionState.selectAccountCenterTab(tab);
        switchMarketRoute(TerminalPage.MARKET_ACCOUNT_CENTER.getId());
    }

    public void openAccountCenterFocused(TerminalMarketSectionState.AccountCenterTab tab, String recordId) {
        marketSectionState.selectAccountCenterTab(tab);
        marketSectionState.setFocusedRecordId(recordId);
        marketSectionState.setHistoryQuery(recordId == null ? "" : recordId.replaceFirst("^[COV]", ""));
        switchMarketRoute(TerminalPage.MARKET_ACCOUNT_CENTER.getId());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!hasOpenPopup() && mouseButton == 0
            && TerminalHudOverlayHandler.INSTANCE.clickTerminalNotification(mouseX, mouseY, width, height)) return;
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void selectCustomListingForRefresh(String scope, String listingId) {
        marketSectionState.getCustomState().setSelectedScope(scope);
        if (listingId != null && !listingId.trim().isEmpty()) {
            marketSectionState.getCustomState().requestDetail(listingId);
        } else {
            marketSectionState.getCustomState().returnToBrowse();
        }
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            TerminalPage.MARKET_CUSTOM.getId(),
            listingId == null || listingId.trim().isEmpty()
                ? TerminalActionType.MARKET_CUSTOM_REFRESH.getId()
                : TerminalActionType.MARKET_CUSTOM_SELECT_LISTING.getId(),
            marketSectionState.getCustomState().toPayload().encode()));
    }

    private void selectExchangeTargetForRefresh(String targetCode) {
        marketSectionState.getExchangeState().requestDetail(targetCode);
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            TerminalPage.MARKET_EXCHANGE.getId(),
            TerminalActionType.MARKET_EXCHANGE_SELECT_TARGET.getId(),
            marketSectionState.getExchangeState().toPayload().encode()));
    }

    private void selectMarketProduct(String productKey) {
        if (productKey == null || productKey.trim().isEmpty()) {
            return;
        }
        marketSectionState.setSelectedProductKey(productKey);
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            model.getSelectedPageId(),
            TerminalActionType.MARKET_REFRESH.getId(),
            marketSectionState.toBrowsePayload().encodeUnifiedOrder()));
    }

    private void refreshMarketBrowser() {
        if (!isMarketSectionSelected()
            || TerminalPage.MARKET_STANDARDIZED != TerminalPage.fromId(model.getSelectedPageId())) {
            return;
        }
        marketLiveRefreshController.reset();
        sendStandardizedMarketRefresh();
    }

    private boolean canAutoRefreshCurrentMarket() {
        TerminalPage page = TerminalPage.fromId(model.getSelectedPageId());
        return isMarketSectionSelected()
            && (page == TerminalPage.MARKET_STANDARDIZED
                || page == TerminalPage.MARKET_CUSTOM
                || page == TerminalPage.MARKET_EXCHANGE)
            && !hasOpenPopup()
            && !marketSectionState.hasFocusedField();
    }

    private void sendCurrentMarketRefresh() {
        TerminalPage page = TerminalPage.fromId(model.getSelectedPageId());
        if (page == TerminalPage.MARKET_CUSTOM) {
            sendActionToServer(new TerminalActionMessage(
                model.getSessionToken(), page.getId(), TerminalActionType.MARKET_CUSTOM_REFRESH.getId(),
                marketSectionState.getCustomState().toPayload().encode()));
            return;
        }
        if (page == TerminalPage.MARKET_EXCHANGE) {
            sendActionToServer(new TerminalActionMessage(
                model.getSessionToken(), page.getId(), TerminalActionType.MARKET_REFRESH.getId(),
                marketSectionState.getExchangeState().toPayload().encode()));
            return;
        }
        sendStandardizedMarketRefresh();
    }

    private void sendStandardizedMarketRefresh() {
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            TerminalPage.MARKET_STANDARDIZED.getId(),
            TerminalActionType.MARKET_REFRESH.getId(),
            marketSectionState.toBrowsePayload().encodeUnifiedOrder()));
    }

    private void openLimitBuyConfirmPopup() {
        TerminalMarketSectionModel marketModel = getSelectedMarketModel();
        if (marketModel == null || !marketModel.isStandardizedRoute() || !marketSectionState.hasCompleteLimitBuyDraft()) {
            return;
        }
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认提交标准商品限价买单",
            "确认后将按当前商品、价格和数量提交买单，并在服务端处理完成后刷新市场页面。",
            Arrays.asList(
                "商品: " + marketModel.getSelectedProductName(),
                "价格: " + marketSectionState.getLimitBuyPriceText() + " STARCOIN",
                "数量: " + marketSectionState.getLimitBuyQuantityText(),
                "盘口: 买一 " + marketModel.getHighestBid() + " / 卖一 " + marketModel.getLowestAsk(),
                "资金预览: " + marketModel.getLimitBuyPreview(),
                "来源: 银行可用余额；本金和手续费上限仅作预留，手续费只按实际成交收取。"),
            "确认买单",
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    confirmLimitBuy();
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private void openUnifiedOrderConfirmPopup(TerminalMarketSectionState.OrderSide side,
        TerminalMarketSectionState.OrderType type) {
        final TerminalMarketSectionModel marketModel = getSelectedMarketModel();
        if (marketModel == null || !marketModel.isStandardizedRoute()) {
            return;
        }
        marketSectionState.setOrderSide(side);
        marketSectionState.setOrderType(type);
        final boolean buy = side == TerminalMarketSectionState.OrderSide.BUY;
        String marketPrice = buy ? marketModel.getLowestAsk() : marketModel.getHighestBid();
        long availableAsset = buy ? currentPlayerBalance() : parsePositiveLong(marketModel.getSourceAvailable());
        ModalPopupPanel popup = new MarketOrderEntryPopup(width, height, marketSectionState, side, type,
            marketModel.getSelectedProductName(), marketModel.getHighestBid(), marketModel.getLatestTradePrice(),
            marketModel.getLowestAsk(),
            buy ? "余额 " + availableAsset : "可卖 " + marketModel.getSourceAvailable(),
            buy ? "银行 -> 市场交割" : "个人 Base Vault -> 市场交割",
            availableAsset,
            new Runnable() {
                @Override
                public void run() {
                    if (!marketSectionState.hasCompleteOrderTicket()) { return; }
                    closePopup();
                    TerminalActionMessage message = TerminalMarketActionMessageFactory.createConfirmOrderMessage(
                        model, marketModel, marketSectionState);
                    if (message != null) sendActionToServer(message);
                }
            }, new Runnable() {
                @Override
                public void run() { closePopup(); }
            });
        openPopup(popup);
    }

    private long currentPlayerBalance() {
        TerminalHomeScreenModel.PageSnapshotModel bankSnapshot = model.getPageSnapshot("bank");
        TerminalBankSectionModel bankModel = bankSnapshot == null ? null : bankSnapshot.getBankSectionModel();
        return bankModel == null ? 0L : parsePositiveLong(bankModel.getBalanceSummary().getPlayerBalance());
    }

    private static long parsePositiveLong(String value) {
        if (value == null) { return 0L; }
        StringBuilder digits = new StringBuilder();
        boolean started = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current >= '0' && current <= '9') {
                digits.append(current);
                started = true;
            } else if (started && current != ',') {
                break;
            }
        }
        if (digits.length() == 0) { return 0L; }
        try { return Long.parseLong(digits.toString()); }
        catch (NumberFormatException ignored) { return Long.MAX_VALUE; }
    }

    private void openDepositHeldConfirmPopup() {
        final TerminalMarketSectionModel marketModel = getSelectedMarketModel();
        if (marketModel == null || !marketModel.isStandardizedRoute() || !marketModel.isDepositEnabled()) {
            return;
        }
        if (marketSectionState.getVaultDepositQuantityText().isEmpty()) {
            openStandardizedVaultPicker(marketModel);
            return;
        }
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认从个人仓存入",
            "确认后将把已选择的个人仓标准商品转入市场可售库存，并回写最新市场数据。",
            Arrays.asList(
                "商品: " + marketModel.getSelectedProductName(),
                "数量: " + marketSectionState.getVaultDepositQuantityText(),
                "仓储状态: " + marketModel.getWarehouseNotice(),
                "来源: 个人 Base Vault（按标准商品键聚合扣除）。",
                "去向: 市场可售库存；未准入、数量不足或版本变化时服务端拒绝执行。"),
            "确认存入",
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                    TerminalActionMessage message = TerminalMarketActionMessageFactory.createConfirmDepositHeldMessage(
                        model, marketModel, marketSectionState);
                    if (message != null) {
                        marketSectionState.setVaultDepositQuantityText("");
                        sendActionToServer(message);
                    }
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private void confirmLimitBuy() {
        closePopup();
        TerminalActionMessage message = TerminalMarketActionMessageFactory.createConfirmLimitBuyMessage(
            model,
            getSelectedMarketModel(),
            marketSectionState);
        if (message != null) {
            sendActionToServer(message);
        }
    }

    private void openLimitSellConfirmPopup() {
        final TerminalMarketSectionModel marketModel = getSelectedMarketModel();
        if (marketModel == null || !marketModel.isStandardizedRoute() || !marketSectionState.hasCompleteLimitSellDraft()) {
            return;
        }
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认提交标准商品限价卖单",
            "确认后将按当前标准商品详情与表单内容提交真实卖单，卖出来源只会消耗账户仓可售库存。",
            Arrays.asList(
                "商品: " + marketModel.getSelectedProductName(),
                "价格: " + marketSectionState.getLimitSellPriceText() + " STARCOIN",
                "数量: " + marketSectionState.getLimitSellQuantityText(),
                "可售库存: " + marketModel.getSourceAvailable(),
                "成交预览: " + marketModel.getLimitSellPreview(),
                "来源: 只锁定账户仓库存；数量不足时服务端拒绝，不会直接出售手持物。"),
            "确认卖单",
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                    TerminalActionMessage message = TerminalMarketActionMessageFactory.createConfirmLimitSellMessage(
                        model, marketModel, marketSectionState);
                    if (message != null) {
                        sendActionToServer(message);
                    }
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private void openInstantBuyConfirmPopup() {
        final TerminalMarketSectionModel marketModel = getSelectedMarketModel();
        if (marketModel == null || !marketModel.isStandardizedRoute() || !marketSectionState.hasCompleteInstantBuyDraft()) {
            return;
        }
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认即时买入",
            "确认后将按当前卖盘深度执行真实即时买入，若盘口不足会收到明确失败反馈。",
            Arrays.asList(
                "商品: " + marketModel.getSelectedProductName(),
                "数量: " + marketSectionState.getInstantBuyQuantityText(),
                "卖一: " + marketModel.getLowestAsk() + " / " + marketModel.getBestAskQuantity(),
                "执行预览: " + marketModel.getInstantBuyPreview(),
                "来源: 银行余额；深度不足或余额不足时服务端拒绝执行。"),
            "确认买入",
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                    TerminalActionMessage message = TerminalMarketActionMessageFactory.createConfirmInstantBuyMessage(
                        model, marketModel, marketSectionState);
                    if (message != null) {
                        sendActionToServer(message);
                    }
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private void openInstantSellConfirmPopup() {
        final TerminalMarketSectionModel marketModel = getSelectedMarketModel();
        if (marketModel == null || !marketModel.isStandardizedRoute() || !marketSectionState.hasCompleteInstantSellDraft()) {
            return;
        }
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认即时卖出",
            "确认后将按当前买盘深度执行真实即时卖出，卖出来源只会消耗账户仓库存。",
            Arrays.asList(
                "商品: " + marketModel.getSelectedProductName(),
                "数量: " + marketSectionState.getInstantSellQuantityText(),
                "买一: " + marketModel.getHighestBid() + " / " + marketModel.getBestBidQuantity(),
                "执行预览: " + marketModel.getInstantSellPreview(),
                "来源: 可售库存=" + marketModel.getSourceAvailable()
                    + "；深度或库存不足时服务端拒绝执行。"),
            "确认卖出",
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                    TerminalActionMessage message = TerminalMarketActionMessageFactory.createConfirmInstantSellMessage(
                        model, marketModel, marketSectionState);
                    if (message != null) {
                        sendActionToServer(message);
                    }
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private void openCancelOrderConfirmPopup(String orderId) {
        final TerminalMarketSectionModel marketModel = getSelectedMarketModel();
        if (marketModel == null || orderId == null || orderId.trim().isEmpty()) {
            return;
        }
        marketSectionState.prepareCancelOrder(orderId, findOrderUpdatedAt(marketModel, orderId));
        String orderLine = findOrderLine(marketModel, orderId);
        String[] orderParts = orderLine.split("\\|");
        String side = orderPart(orderParts, 2);
        String total = stripOrderLabel(orderPart(orderParts, 4));
        String filled = stripOrderLabel(orderPart(orderParts, 5));
        String remaining = stripOrderLabel(orderPart(orderParts, 6));
        String reserved = stripOrderLabel(orderPart(orderParts, 13));
        boolean buyOrder = "BUY".equalsIgnoreCase(side);
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认撤销当前订单",
            "确认后将撤销当前订单，并在服务端处理完成后刷新市场页面。",
            Arrays.asList(
                "商品: " + marketModel.getSelectedProductName(),
                "订单: #" + shortOrderId(orderId) + " / " + (buyOrder ? "买入" : "卖出"),
                "成交/总量: " + filled + "/" + total + "；剩余: " + remaining,
                "预计返还: " + (buyOrder ? reserved + " GT" : remaining + " 件商品"),
                "返还目标: " + (buyOrder ? "银行可用余额" : "个人 Base Vault；满仓时保留待恢复")),
            "确认撤单",
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                    TerminalActionMessage message = TerminalMarketActionMessageFactory.createCancelOrderMessage(
                        model, marketModel, marketSectionState);
                    if (message != null) {
                        sendActionToServer(message);
                    }
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private void openCurrentProductCancelPopup() {
        TerminalMarketSectionModel marketModel = getSelectedMarketModel();
        if (marketModel == null) return;
        openPopup(new MarketCancelableOrdersPopup(TerminalHomeLayout.compute(width, height, model).panelBounds, marketModel,
            new MarketCancelableOrdersPopup.Handler() {
                @Override public void select(String orderId) {
                    closePopup();
                    openCancelOrderConfirmPopup(orderId);
                }
                @Override public void close() { closePopup(); }
            }));
    }

    private void openClaimConfirmPopup(String custodyId) {
        TerminalMarketSectionModel marketModel = getSelectedMarketModel();
        if (marketModel == null || custodyId == null || custodyId.trim().isEmpty()) {
            return;
        }
        marketSectionState.setPendingClaimCustodyId(custodyId);
        final String claimDetail = findClaimLine(marketModel, custodyId);
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认接收待收货资产",
            "确认后将接收当前待收货资产，并在服务端处理完成后刷新市场页面。",
            Arrays.asList(
                "待收货编号: " + custodyId,
                "明细: " + claimDetail,
                "当前商品: " + marketModel.getSelectedProductName(),
                "去向: 个人 Base Vault；个人仓满或投递异常时资产保持待收货状态。"),
            "确认提取",
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    confirmClaim();
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private void confirmClaim() {
        closePopup();
        TerminalActionMessage message = TerminalMarketActionMessageFactory.createClaimMessage(
            model,
            getSelectedMarketModel(),
            marketSectionState);
        if (message != null) {
            sendActionToServer(message);
        }
    }

    private void openCustomActionConfirmPopup(final TerminalActionType actionType) {
        if (!marketSectionState.getCustomState().hasSelectedListing()) {
            return;
        }
        final com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel customModel =
            model.getSelectedPageSnapshot().getCustomMarketSectionModel();
        if (customModel == null) {
            return;
        }
        boolean allowed = actionType == TerminalActionType.MARKET_CUSTOM_BUY_LISTING ? customModel.isCanBuy()
            : actionType == TerminalActionType.MARKET_CUSTOM_CANCEL_LISTING ? customModel.isCanCancel() : customModel.isCanClaim();
        if (!allowed) {
            return;
        }
        String title = actionType == TerminalActionType.MARKET_CUSTOM_BUY_LISTING ? "确认购买定制商品"
            : actionType == TerminalActionType.MARKET_CUSTOM_CANCEL_LISTING ? "确认下架定制挂牌" : "确认领取成交物";
        String button = actionType == TerminalActionType.MARKET_CUSTOM_BUY_LISTING ? "确认购买"
            : actionType == TerminalActionType.MARKET_CUSTOM_CANCEL_LISTING ? "确认下架" : "确认领取";
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            title,
            "确认后将执行当前定制商品操作，并在服务端处理完成后刷新页面。",
            Arrays.asList(
                "listingId: " + marketSectionState.getCustomState().getSelectedListingId(),
                "标题: " + customModel.getSelectedTitle(),
                "价格: " + customModel.getSelectedPrice(),
                "状态: " + customModel.getSelectedStatus()),
            button,
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                    sendActionToServer(new TerminalActionMessage(
                        model.getSessionToken(),
                        TerminalPage.MARKET_CUSTOM.getId(),
                        actionType.getId(),
                        marketSectionState.getCustomState().toPayload().encode()));
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private void openCustomPublishConfirmPopup() {
        if (marketSectionState.getCustomState().getSelectedVaultSlot() < 0) {
            openExactVaultPicker("选择个人仓单件", new VaultAssetPickerPopup.SelectionHandler() {
                @Override public void select(com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel.VaultAssetModel asset, int quantity) {
                    marketSectionState.getCustomState().setSelectedVaultSlot(asset.getSlotIndex());
                    closePopup();
                    openCustomPublishConfirmPopup();
                }
                @Override public void cancel() { closePopup(); }
            });
            return;
        }
        if (!marketSectionState.getCustomState().hasPublishPrice()) {
            openPopup(new CustomListingPricePopup(width, height, value -> {
                marketSectionState.getCustomState().setPublishPriceText(value);
                closePopup();
                openCustomPublishConfirmPopup();
            }, () -> {
                marketSectionState.getCustomState().setSelectedVaultSlot(-1);
                closePopup();
            }));
            return;
        }
        final String askingPrice = marketSectionState.getCustomState().getPublishPriceText();
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认发布单件挂牌",
            "服务端会验证选中的个人仓格位为单件物品；确认后该物品移入定制市场托管。",
            Arrays.asList(
                "来源库存: 个人仓第 " + (marketSectionState.getCustomState().getSelectedVaultSlot() + 1) + " 格",
                "数量: 1",
                "挂牌价格: " + askingPrice + " STARCOIN",
                "手续费: 0 STARCOIN",
                "不可执行原因: 个人仓格位变动、物品堆叠不为 1 或价格无效时将被拒绝"),
            "确认发布",
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                    sendActionToServer(new TerminalActionMessage(
                        model.getSessionToken(),
                        TerminalPage.MARKET_CUSTOM.getId(),
                        TerminalActionType.MARKET_CUSTOM_PUBLISH_HELD.getId(),
                        marketSectionState.getCustomState().toPayload().encode()));
                    marketSectionState.getCustomState().setSelectedVaultSlot(-1);
                    marketSectionState.getCustomState().setPublishPriceText("");
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private void openExchangeConfirmPopup() {
        final com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel exchangeModel =
            model.getSelectedPageSnapshot().getExchangeMarketSectionModel();
        if (exchangeModel == null || !marketSectionState.getExchangeState().hasSelectedTarget()) {
            return;
        }
        if (marketSectionState.getExchangeState().getSelectedVaultSlot() < 0) {
            openExactVaultPicker("选择个人仓任务书硬币", new VaultAssetPickerPopup.SelectionHandler() {
                @Override public void select(com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel.VaultAssetModel asset, int quantity) {
                    marketSectionState.getExchangeState().setSelectedVaultSlot(asset.getSlotIndex());
                    closePopup();
                    requestRefresh();
                }
                @Override public void cancel() { closePopup(); }
            });
            return;
        }
        if (!exchangeModel.isExecutable()) {
            return;
        }
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认执行汇率兑换",
            "确认后将按当前正式报价执行兑换，并在服务端处理完成后刷新页面。",
            Arrays.asList(
                "兑换对: " + exchangeModel.getPairCode(),
                "输入: 个人仓第 " + (marketSectionState.getExchangeState().getSelectedVaultSlot() + 1) + " 格 / "
                    + exchangeModel.getInputAssetCode(),
                "到账: " + exchangeModel.getEffectiveExchangeValue() + " " + exchangeModel.getOutputAssetCode(),
                "规则: " + exchangeModel.getRuleVersion() + " / " + exchangeModel.getLimitStatusDisplay()),
            "确认兑换",
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                    sendActionToServer(new TerminalActionMessage(
                        model.getSessionToken(),
                        TerminalPage.MARKET_EXCHANGE.getId(),
                        TerminalActionType.MARKET_EXCHANGE_CONFIRM.getId(),
                        marketSectionState.getExchangeState().toPayload().encode()));
                    marketSectionState.getExchangeState().setSelectedVaultSlot(-1);
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private void openStandardizedVaultPicker(final TerminalMarketSectionModel marketModel) {
        openPopup(new VaultAssetPickerPopup(width, height, marketModel.getVaultAssets(), asset ->
            asset.isStandardizedEligible() && marketModel.getSelectedProductKey().equals(asset.getStandardizedProductKey()),
            new VaultAssetPickerPopup.SelectionHandler() {
                @Override public void select(com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel.VaultAssetModel asset, int quantity) {
                    marketSectionState.setVaultDepositQuantityText(String.valueOf(quantity));
                    closePopup();
                    openDepositHeldConfirmPopup();
                }
                @Override public void cancel() { closePopup(); }
            }));
    }

    private void openExactVaultPicker(String title, VaultAssetPickerPopup.SelectionHandler handler) {
        TerminalMarketSectionModel marketModel = getSelectedMarketModel();
        if (marketModel == null) return;
        openPopup(new VaultAssetPickerPopup(width, height, marketModel.getVaultAssets(), asset -> {
            if (asset.getQuantity() <= 0) {
                return false;
            }
            return TerminalPage.fromId(model.getSelectedPageId()) != TerminalPage.MARKET_CUSTOM
                || asset.getQuantity() == 1;
        }, handler));
    }

    private void selectServerToolsWarp(String warpName) {
        if (warpName == null || warpName.trim().isEmpty()) {
            return;
        }
        serverToolsSectionState.setSelectedWarpName(warpName);
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            TerminalPage.SERVER_TOOLS.getId(),
            TerminalActionType.SERVER_TOOLS_SELECT_WARP.getId(),
            serverToolsSectionState.toPayload().encode()));
    }

    private void openServerToolsWarpConfirmPopup(String warpName) {
        final TerminalServerToolsSectionModel serverToolsModel = getSelectedServerToolsModel();
        final String selectedWarp = warpName == null || warpName.trim().isEmpty()
            ? serverToolsSectionState.getSelectedWarpName() : warpName.trim();
        if (serverToolsModel == null || selectedWarp.isEmpty() || !serverToolsModel.isSelectedWarpEnabled()) {
            return;
        }
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认执行群组服传送",
            "确认后将通过 ServerTools 现有 warp 主链发起传送，并由服务端回写传送反馈。",
            Arrays.asList(
                "warp: " + selectedWarp,
                "标题: " + serverToolsModel.getSelectedWarpTitle(),
                "当前服务器: " + serverToolsModel.getCurrentServerId(),
                "目标服务器: " + serverToolsModel.getSelectedTargetServerId(),
                "传送说明: " + serverToolsModel.getSelectedWarpDescription()),
            "确认传送",
            "取消",
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                    serverToolsSectionState.setSelectedWarpName(selectedWarp);
                    sendActionToServer(new TerminalActionMessage(
                        model.getSessionToken(),
                        TerminalPage.SERVER_TOOLS.getId(),
                        TerminalActionType.SERVER_TOOLS_CONFIRM_WARP.getId(),
                        TerminalServerToolsActionPayload.forWarp(selectedWarp).encode()));
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    protected void sendActionToServer(TerminalActionMessage message) {
        if (message == null) {
            return;
        }
        TerminalNetwork.CHANNEL.sendToServer(message.withRequestSequence(responseSequenceGate.issueNext()));
    }

    private void openShellInfoPopup() {
        final boolean serverToolsPage = isServerToolsSectionSelected();
        final boolean marketPage = isMarketSectionSelected();
        final boolean marketOverviewPage = TerminalPage.MARKET.getId().equals(model.getSelectedPageId());
        final boolean standardizedMarketPage = TerminalPage.MARKET_STANDARDIZED.getId().equals(model.getSelectedPageId());
        ModalPopupPanel popup = TerminalPopupFactory.createInfoPopup(
            width,
            height,
            serverToolsPage ? "群组服传送说明"
                : marketOverviewPage ? "市场总入口说明"
                : standardizedMarketPage ? "标准商品市场说明"
                : marketPage ? "市场工作页说明"
                : "终端页面说明",
            serverToolsPage
                ? "当前页面展示服务器目录、系统 warp、最近传送状态，并通过服务端现有 warp 主链发起确认传送。"
                    + "\n\n确认前请先在中栏选择目标 warp。"
                : marketOverviewPage
                    ? "MARKET 根页现在只做三类市场分流与共享状态。"
                        + "\n\n标准商品市场: 目录商品、订单簿、仓储与即时成交。"
                        + "\n\n定制商品市场: 单件挂牌、购买、待交付与 claim。"
                        + "\n\n汇率市场: 正式报价、规则校验与兑换确认。"
                : standardizedMarketPage
                    ? "标准商品市场页现在按工作台节奏组织：左侧选商品，中间看行情与个人状态，右侧执行交易动作。"
                        + "\n\n订单、待收货和规则提示都继续留在同一页，但不再抢交易动作主区。"
                : marketPage
                    ? "市场工作页主体应以可执行动作、结果反馈和共享状态为主。"
                        + "\n\n帮助、制度说明和风险解释继续留在弹层或辅助位，不回到正文主区域。"
                : "当前页面已接入终端统一请求与刷新流程。"
                    + "\n\n银行、市场和传送页都通过同一套新壳承载。",
            serverToolsPage
                ? "传送过程中请勿移动或下线，避免 gateway 派发或目标服落点恢复失败。"
                : standardizedMarketPage
                    ? "确认前留意可售库存、卖单锁定、待收货、冻结资金与最近反馈。"
                : marketPage
                    ? "先选市场，再做动作；不要把 MARKET 根页当成交易详情页。"
                : "当前说明只覆盖正式终端壳，不会回接旧终端实现。",
            new Runnable() {
                @Override
                public void run() {
                    closePopup();
                }
            });
        openPopup(popup);
    }

    private boolean isBankSectionSelected() {
        return "bank".equals(model.getSelectedSectionPageId());
    }

    private boolean isMarketSectionSelected() {
        return "market".equals(model.getSelectedSectionPageId());
    }

    private boolean isServerToolsSectionSelected() {
        return TerminalPage.SERVER_TOOLS.getId().equals(model.getSelectedSectionPageId());
    }

    private TerminalBankSectionModel getSelectedBankModel() {
        if (!isBankSectionSelected()) {
            return null;
        }
        return model.getSelectedPageSnapshot().getBankSectionModel();
    }

    private TerminalMarketSectionModel getSelectedMarketModel() {
        if (!isMarketSectionSelected()) {
            return null;
        }
        return model.getSelectedPageSnapshot().getMarketSectionModel();
    }

    private TerminalServerToolsSectionModel getSelectedServerToolsModel() {
        if (!isServerToolsSectionSelected()) {
            return null;
        }
        return model.getSelectedPageSnapshot().getServerToolsSectionModel();
    }

    private void syncBankSectionStateFromModel(TerminalHomeScreenModel model) {
        if (model == null) {
            bankSectionState.applyModel(null);
            return;
        }
        TerminalHomeScreenModel.PageSnapshotModel bankSnapshot = model.getPageSnapshot("bank");
        bankSectionState.applyModel(bankSnapshot == null ? null : bankSnapshot.getBankSectionModel() == null ? null : bankSnapshot.getBankSectionModel().getTransferForm());
    }

    private void syncMarketSectionStateFromModel(TerminalHomeScreenModel model) {
        if (model == null) {
            marketSectionState.applyModel(null);
            return;
        }
        TerminalHomeScreenModel.PageSnapshotModel marketSnapshot = model.getPageSnapshot("market");
        marketSectionState.applyModel(marketSnapshot == null ? null : marketSnapshot.getMarketSectionModel());
        if (marketSnapshot != null) {
            marketSectionState.getCustomState().applyModel(marketSnapshot.getCustomMarketSectionModel());
            marketSectionState.getExchangeState().applyModel(marketSnapshot.getExchangeMarketSectionModel());
        }
    }

    private void syncServerToolsSectionStateFromModel(TerminalHomeScreenModel model) {
        if (model == null) {
            serverToolsSectionState.applyModel(null);
            return;
        }
        TerminalHomeScreenModel.PageSnapshotModel serverToolsSnapshot = model.getPageSnapshot(TerminalPage.SERVER_TOOLS.getId());
        serverToolsSectionState.applyModel(serverToolsSnapshot == null ? null : serverToolsSnapshot.getServerToolsSectionModel());
    }

    private String findClaimLine(TerminalMarketSectionModel marketModel, String custodyId) {
        if (marketModel == null || custodyId == null) {
            return "当前没有 claim 明细。";
        }
        for (int i = 0; i < marketModel.getClaimIds().size() && i < marketModel.getClaimLines().size(); i++) {
            String claimId = marketModel.getClaimIds().get(i);
            if (custodyId.equals(claimId == null ? "" : claimId.trim())) {
                String claimLine = marketModel.getClaimLines().get(i);
                return claimLine == null || claimLine.trim().isEmpty() ? "当前没有 claim 明细。" : claimLine.trim();
            }
        }
        return "当前没有 claim 明细。";
    }

    private String findOrderLine(TerminalMarketSectionModel marketModel, String orderId) {
        if (marketModel == null || orderId == null) {
            return "当前没有订单明细。";
        }
        for (int i = 0; i < marketModel.getMyOrderIds().size() && i < marketModel.getMyOrderLines().size(); i++) {
            String currentOrderId = marketModel.getMyOrderIds().get(i);
            if (orderId.equals(currentOrderId == null ? "" : currentOrderId.trim())) {
                String orderLine = marketModel.getMyOrderLines().get(i);
                return orderLine == null || orderLine.trim().isEmpty() ? "当前没有订单明细。" : orderLine.trim();
            }
        }
        return "当前没有订单明细。";
    }

    private long findOrderUpdatedAt(TerminalMarketSectionModel marketModel, String orderId) {
        String line = findOrderLine(marketModel, orderId);
        String[] parts = line.split("\\|");
        if (parts.length <= 10) return 0L;
        try {
            return Long.parseLong(parts[10].trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private String orderPart(String[] parts, int index) {
        return index < parts.length && parts[index] != null ? parts[index].trim() : "--";
    }

    private String stripOrderLabel(String value) {
        String normalized = value == null ? "" : value.trim();
        int separator = normalized.indexOf(' ');
        return separator < 0 ? normalized : normalized.substring(separator + 1).trim();
    }

    private String shortOrderId(String orderId) {
        String normalized = orderId == null ? "" : orderId.trim();
        return normalized.length() <= 8 ? normalized : normalized.substring(normalized.length() - 8);
    }

    private static final class BackdropPanel extends PanelContainer {

        private final GuiRect panelBounds;

        private BackdropPanel(int width, int height, GuiRect panelBounds) {
            setBounds(new GuiRect(0, 0, width, height));
            this.panelBounds = panelBounds == null ? new GuiRect(0, 0, 0, 0) : panelBounds;
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            int overlay = 0x99000000;
            if (panelBounds.getWidth() <= 0 || panelBounds.getHeight() <= 0) {
                Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), overlay);
                return;
            }
            if (panelBounds.getY() > bounds.getY()) {
                Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), panelBounds.getY(), overlay);
            }
            if (panelBounds.getBottom() < bounds.getBottom()) {
                Gui.drawRect(bounds.getX(), panelBounds.getBottom(), bounds.getRight(), bounds.getBottom(), overlay);
            }
            if (panelBounds.getX() > bounds.getX()) {
                Gui.drawRect(bounds.getX(), panelBounds.getY(), panelBounds.getX(), panelBounds.getBottom(), overlay);
            }
            if (panelBounds.getRight() < bounds.getRight()) {
                Gui.drawRect(panelBounds.getRight(), panelBounds.getY(), bounds.getRight(), panelBounds.getBottom(), overlay);
            }
        }
    }
}
