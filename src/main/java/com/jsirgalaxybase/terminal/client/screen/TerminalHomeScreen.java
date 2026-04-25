package com.jsirgalaxybase.terminal.client.screen;

import java.util.Arrays;

import net.minecraft.client.gui.GuiScreen;

import com.jsirgalaxybase.client.gui.framework.CanvasScreen;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.ModalPopupPanel;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.TerminalBankActionMessageFactory;
import com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactory;
import com.jsirgalaxybase.terminal.client.component.TerminalBankSectionState;
import com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState;
import com.jsirgalaxybase.terminal.client.component.TerminalPanelFactory;
import com.jsirgalaxybase.terminal.client.component.TerminalPopupFactory;
import com.jsirgalaxybase.terminal.client.component.TerminalShellPanels;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.network.TerminalActionMessage;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public class TerminalHomeScreen extends CanvasScreen {

    private TerminalHomeScreenModel model;
    private final TerminalBankSectionState bankSectionState = new TerminalBankSectionState();
    private final TerminalMarketSectionState marketSectionState = new TerminalMarketSectionState();

    public TerminalHomeScreen(GuiScreen parentScreen, TerminalHomeScreenModel model) {
        super(parentScreen);
        this.model = model == null ? TerminalHomeScreenModel.placeholder() : model;
        syncBankSectionStateFromModel(this.model);
        syncMarketSectionStateFromModel(this.model);
    }

    public void applyModel(TerminalHomeScreenModel model) {
        this.model = model == null ? TerminalHomeScreenModel.placeholder() : model;
        syncBankSectionStateFromModel(this.model);
        syncMarketSectionStateFromModel(this.model);
        closePopup();
        initGui();
    }

    @Override
    protected PanelContainer buildRootPanel() {
        PanelContainer root = new PanelContainer();
        root.setBounds(new GuiRect(0, 0, width, height));
        TerminalHomeLayout layout = TerminalHomeLayout.compute(width, height);

        final TerminalPanelFactory panels = new TerminalPanelFactory();
        root.addChild(panels.createSurface(layout.panelBounds,
            com.jsirgalaxybase.client.gui.theme.ThemeColorKey.PANEL_FILL));
        root.addChild(TerminalShellPanels.createStatusBand(
            panels,
            layout.statusBandBounds,
            model,
            new Runnable() {
                @Override
                public void run() {
                    openShellInfoPopup();
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
                public void openLimitBuyConfirm() {
                    openLimitBuyConfirmPopup();
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
                public void openCustomBuyConfirm() {
                    openCustomActionConfirmPopup(TerminalActionType.MARKET_CUSTOM_BUY_LISTING);
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
            }));
        return root;
    }

    private void handleNavSelection(TerminalHomeScreenModel.NavItemModel navItem) {
        if (navItem == null || navItem.isSelected() || !navItem.isEnabled()) {
            return;
        }
        applyModel(model.withSelectedPageId(navItem.getPageId()));
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            navItem.getPageId(),
            TerminalActionType.SELECT_PAGE.getId(),
            "nav_click"));
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
            sendActionToServer(new TerminalActionMessage(
                model.getSessionToken(),
                model.getSelectedPageId(),
                TerminalActionType.MARKET_REFRESH.getId(),
                marketSectionState.toPayload().encode()));
            return;
        }
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            model.getSelectedPageId(),
            TerminalActionType.REFRESH_PAGE.getId(),
            "manual_refresh"));
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
            "确认后将按当前银行页表单内容发起真实玩家转账，并在服务端处理后回写新的银行 snapshot。",
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
                    : marketSectionState.toPayload().encode()));
    }

    private void selectCustomListingForRefresh(String scope, String listingId) {
        marketSectionState.getCustomState().setSelectedScope(scope);
        marketSectionState.getCustomState().setSelectedListingId(listingId);
        sendActionToServer(new TerminalActionMessage(
            model.getSessionToken(),
            TerminalPage.MARKET_CUSTOM.getId(),
            TerminalActionType.MARKET_CUSTOM_SELECT_LISTING.getId(),
            marketSectionState.getCustomState().toPayload().encode()));
    }

    private void selectExchangeTargetForRefresh(String targetCode) {
        marketSectionState.getExchangeState().setSelectedTargetCode(targetCode);
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
            marketSectionState.toPayload().encode()));
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
            "确认后将按当前标准商品详情与表单内容提交真实买单，并通过 TerminalSnapshotMessage 回写新的标准市场 snapshot。",
            Arrays.asList(
                "商品: " + marketModel.getSelectedProductName(),
                "价格: " + marketSectionState.getLimitBuyPriceText() + " STARCOIN",
                "数量: " + marketSectionState.getLimitBuyQuantityText(),
                "盘口: 买一 " + marketModel.getHighestBid() + " / 卖一 " + marketModel.getLowestAsk()),
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
            "确认提取 CLAIMABLE 资产",
            "确认后将对当前 custody 执行真实 claim 链路，并在服务端处理后回写新的标准市场 snapshot。",
            Arrays.asList(
                "custodyId: " + custodyId,
                "明细: " + claimDetail,
                "当前商品: " + marketModel.getSelectedProductName()),
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
            "确认后将通过新 TerminalActionMessage 主链执行定制商品市场动作，并回写 MARKET_CUSTOM snapshot。",
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

    private void openExchangeConfirmPopup() {
        final com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel exchangeModel =
            model.getSelectedPageSnapshot().getExchangeMarketSectionModel();
        if (exchangeModel == null || !exchangeModel.isExecutable() || !marketSectionState.getExchangeState().hasSelectedTarget()) {
            return;
        }
        ModalPopupPanel popup = TerminalPopupFactory.createConfirmationPopup(
            width,
            height,
            "确认执行汇率兑换",
            "确认后将按当前 formal quote 执行真实兑换，并通过 TerminalSnapshotMessage 回写 MARKET_EXCHANGE。",
            Arrays.asList(
                "pair: " + exchangeModel.getPairCode(),
                "输入: " + exchangeModel.getInputQuantity() + " / " + exchangeModel.getInputAssetCode(),
                "到账: " + exchangeModel.getEffectiveExchangeValue() + " " + exchangeModel.getOutputAssetCode(),
                "规则: " + exchangeModel.getRuleVersion() + " / " + exchangeModel.getLimitStatus()),
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
        TerminalNetwork.CHANNEL.sendToServer(message);
    }

    private void openShellInfoPopup() {
        ModalPopupPanel popup = TerminalPopupFactory.createInfoPopup(
            width,
            height,
            "第七阶段市场页说明",
            "新 TerminalHomeScreen 现在承接 MARKET 总入口、MARKET_STANDARDIZED、MARKET_CUSTOM 与 MARKET_EXCHANGE。"
                + "\n\n银行与三类市场都继续沿用 selectedPageId / TerminalActionMessage / TerminalSnapshotMessage 主链。"
                + "\n\ncutover 与旧 ModularUI 删除仍明确留给 phase 8 / phase 9。",
            "当前弹窗只说明 phase 7 边界，不会顺手做正式 cutover 或删除旧终端实现。",
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
}
