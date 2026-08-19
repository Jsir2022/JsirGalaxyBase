package com.jsirgalaxybase.terminal.client.component;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.client.gui.Gui;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.framework.VerticalScrollPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.client.gui.theme.ThemeTextureKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalServerToolsSectionModel;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public final class TerminalShellPanels {

    private static final int PADDING = 0;

    private TerminalShellPanels() {}

    private static String compactServerDisplay(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty() || "--".equals(value)) {
            return "--";
        }
        String normalized = value.toLowerCase();
        if (normalized.contains("lobby")) {
            return "Lobby";
        }
        int sIndex = normalized.lastIndexOf("_s");
        if (sIndex >= 0 && sIndex + 2 < normalized.length()) {
            String suffix = normalized.substring(sIndex + 2).replaceAll("[^0-9].*$", "");
            if (!suffix.isEmpty()) {
                return "S" + suffix;
            }
        }
        String compact = value
            .replace("galaxy_gtnh284_", "")
            .replace("galaxy_gtnh_", "")
            .replace("galaxy_", "")
            .replace('_', ' ')
            .trim();
        return compact.isEmpty() ? value : compact;
    }

    public interface NavigationHandler {

        void open(TerminalHomeScreenModel.NavItemModel navItem);
    }

    public interface BankActionHandler {

        void openAccount();

        void openTransferConfirm();
    }

    public interface MarketActionHandler {

        void openMarketOverview();

        void openMarketHelp();

        void openStandardizedMarket();

        void openCustomMarket();

        void openExchangeMarket();

        void selectProduct(String productKey);

        void refreshProductBrowser();

        void changeProductBrowserPage(int pageIndex);

        void openDepositHeldConfirm();

        void openLimitBuyConfirm();

        void openLimitSellConfirm();

        void openInstantBuyConfirm();

        void openInstantSellConfirm();

        default void openOrderConfirm(TerminalMarketSectionState.OrderSide side,
            TerminalMarketSectionState.OrderType type) {
            if (side == TerminalMarketSectionState.OrderSide.BUY) {
                if (type == TerminalMarketSectionState.OrderType.MARKET) openInstantBuyConfirm();
                else openLimitBuyConfirm();
            } else if (type == TerminalMarketSectionState.OrderType.MARKET) openInstantSellConfirm();
            else openLimitSellConfirm();
        }

        void openCancelOrderConfirm(String orderId);

        default void openStandardizedHistory() {}

        default void refreshStandardizedHistory() {}

        void openClaimConfirm(String custodyId);

        void selectCustomListing(String scope, String listingId);

        void refreshCustomBrowse();

        void changeCustomBrowsePage(int pageIndex);

        void openCustomBuyConfirm();

        void openCustomPublishConfirm();

        void openCustomCancelConfirm();

        void openCustomClaimConfirm();

        void selectExchangeTarget(String targetCode);

        void refreshExchangeBrowse();

        void changeExchangeBrowsePage(int pageIndex);

        void refreshExchangeQuote();

        void openExchangeConfirm();
    }

    public interface ServerToolsActionHandler {

        void refreshServerTools();

        void selectWarp(String warpName);

        void confirmWarp(String warpName);
    }

    public static PanelContainer createStatusBand(TerminalPanelFactory panels, GuiRect bounds,
        final TerminalHomeScreenModel model, final Runnable refreshAction, Runnable infoAction,
        Runnable backAction, Runnable closeAction) {
        return createStatusBand(panels, bounds, model, refreshAction, infoAction, backAction, closeAction, null);
    }

    public static PanelContainer createStatusBand(TerminalPanelFactory panels, GuiRect bounds,
        final TerminalHomeScreenModel model, final Runnable refreshAction, Runnable infoAction,
        Runnable backAction, Runnable closeAction, Runnable accountCenterAction) {
        PanelContainer band = new WindowTitleBarPanel();
        band.setBounds(bounds);
        final TerminalHomeScreenModel.StatusBandModel statusBand = model.getStatusBand();
        final TerminalHomeScreenModel.PageSnapshotModel snapshot = TerminalSectionRouter.resolveSnapshot(model);
        boolean marketPage = snapshot.hasMarketSectionModel() || snapshot.hasCustomMarketSectionModel()
            || snapshot.hasExchangeMarketSectionModel();
        boolean refreshVisible = snapshot.hasServerToolsSectionModel() || marketPage;
        TerminalPage selectedPage = TerminalPage.fromId(model.getSelectedPageId());
        boolean centerVisible = accountCenterAction != null
            && (selectedPage == TerminalPage.MARKET || selectedPage == TerminalPage.MARKET_STANDARDIZED
                || selectedPage == TerminalPage.MARKET_ACCOUNT_CENTER || selectedPage == TerminalPage.VAULT);
        boolean centerSelected = selectedPage == TerminalPage.MARKET_ACCOUNT_CENTER;
        int controlInset = bounds.getHeight() <= 12 ? 1 : 2;
        int controlMaxHeight = Math.max(6, bounds.getHeight() - controlInset * 2);
        int iconButton = Math.max(8, Math.min(12, controlMaxHeight));
        int iconGap = 2;
        int badgeWidth = Math.min(68, Math.max(44, bounds.getWidth() / 13));
        int signalWidth = 26;
        int rowY = bounds.getY() + Math.max(0, (bounds.getHeight() - 9) / 2);
        int buttonY = bounds.getY() + Math.max(controlInset, (bounds.getHeight() - iconButton) / 2);
        int rightCursor = bounds.getRight() - controlInset;
        int closeX = rightCursor - iconButton;
        int backX = closeX - iconGap - iconButton;
        int infoX = backX - iconGap - iconButton;
        int refreshX = refreshVisible ? infoX - iconGap - iconButton : infoX;
        int centerWidth = centerVisible ? Math.max(54, Math.min(66, bounds.getWidth() / 10)) : 0;
        int centerX = centerVisible ? (refreshVisible ? refreshX : infoX) - iconGap - centerWidth
            : (refreshVisible ? refreshX : infoX);
        int signalX = (centerVisible ? centerX : (refreshVisible ? refreshX : infoX)) - signalWidth - 5;
        int badgeX = signalX - badgeWidth - 5;
        int textWidth = Math.max(40, badgeX - bounds.getX() - 2);
        band.addChild(panels.createLabel(
            new GuiRect(bounds.getX() + 10, rowY, textWidth - 8, 10),
            new Supplier<String>() {
                @Override
                public String get() {
                    String lead = snapshot.getLead();
                    if (snapshot.hasServerToolsSectionModel()) {
                        TerminalServerToolsSectionModel serverTools = snapshot.getServerToolsSectionModel();
                        String currentServer = serverTools == null ? "" : serverTools.getCurrentServerId();
                        String targetServer = serverTools == null ? "" : serverTools.getSelectedTargetServerId();
                        boolean hasRoute = currentServer != null && currentServer.length() > 0
                            && targetServer != null && targetServer.length() > 0
                            && !"--".equals(targetServer);
                        return model.getSelectedNavItem().getLabel() + " / " + snapshot.getTitle()
                            + (hasRoute ? " / " + compactServerDisplay(currentServer) + " -> " + compactServerDisplay(targetServer) : "");
                    }
                    if (snapshot.hasMarketSectionModel()) {
                        return TerminalMarketShell.buildStatusBandText(model, snapshot);
                    }
                    if (snapshot.hasCustomMarketSectionModel()) {
                        return TerminalMarketShell.buildStatusBandText(model, snapshot);
                    }
                    if (snapshot.hasExchangeMarketSectionModel()) {
                        return TerminalMarketShell.buildStatusBandText(model, snapshot);
                    }
                    return model.getSelectedNavItem().getLabel() + " / " + snapshot.getTitle()
                        + (lead == null || lead.length() == 0 ? "" : " / " + lead)
                        + " | " + statusBand.getDetail();
                }
            },
            ThemeColorKey.TEXT_PRIMARY,
            false));
        band.addChild(panels.createLabel(
            new GuiRect(badgeX, rowY, badgeWidth, 10),
            new Supplier<String>() {
                @Override
                public String get() {
                    return statusBand.getBadgeLabel() + " " + statusBand.getBadgeValue();
                }
            },
            ThemeColorKey.TEXT_SECONDARY,
            false));
        int signalHeight = Math.max(8, Math.min(10, controlMaxHeight));
        band.addChild(new SignalStatusPanel(new GuiRect(signalX, bounds.getY() + Math.max(0, (bounds.getHeight() - signalHeight) / 2),
            signalWidth, signalHeight)));
        if (refreshVisible) {
            TerminalIconButtonPanel refreshButton = new TerminalIconButtonPanel(TerminalIconKind.REFRESH, refreshAction, null);
            refreshButton.setBounds(new GuiRect(refreshX, buttonY, iconButton, iconButton));
            band.addChild(refreshButton);
        }
        if (centerVisible) {
            TerminalIconButtonPanel centerButton = new TerminalIconButtonPanel(TerminalIconKind.ORDER_ASSET,
                accountCenterAction, null, selectedPage == TerminalPage.VAULT ? "资产中心" : "订单中心",
                centerSelected);
            centerButton.setBounds(new GuiRect(centerX, buttonY, centerWidth, iconButton));
            band.addChild(centerButton);
        }
        TerminalIconButtonPanel infoButton = new TerminalIconButtonPanel(TerminalIconKind.HELP, infoAction, null);
        infoButton.setBounds(new GuiRect(infoX, buttonY, iconButton, iconButton));
        band.addChild(infoButton);
        TerminalIconButtonPanel backButton = new TerminalIconButtonPanel(TerminalIconKind.BACK, backAction, null);
        backButton.setBounds(new GuiRect(backX, buttonY, iconButton, iconButton));
        band.addChild(backButton);
        TerminalIconButtonPanel closeButton = new TerminalIconButtonPanel(TerminalIconKind.CLOSE, closeAction, null);
        closeButton.setBounds(new GuiRect(closeX, buttonY, iconButton, iconButton));
        band.addChild(closeButton);
        return band;
    }

    public static PanelContainer createNavigationRail(TerminalPanelFactory panels, GuiRect bounds,
        TerminalHomeScreenModel model, NavigationHandler handler) {
        PanelContainer navSurface = new FixedNavigationPanel();
        navSurface.setBounds(bounds);
        List<TerminalHomeScreenModel.NavItemModel> navItems = model.getNavItems();
        int padX = 0;
        int scrollY = bounds.getY();
        int scrollHeight = Math.max(20, bounds.getBottom() - scrollY);
        VerticalScrollPanel navScroll = panels.createScrollPanel(
            new GuiRect(bounds.getX() + padX, scrollY, Math.max(10, bounds.getWidth() - padX * 2 - 2), scrollHeight),
            0,
            1);
        for (final TerminalHomeScreenModel.NavItemModel navItem : navItems) {
            navScroll.addScrollableChild(
                panels.createNavigationItem(
                    new GuiRect(bounds.getX() + padX, 0, Math.max(10, bounds.getWidth() - padX * 2 - 2), TerminalLayoutMetrics.BUTTON_HEIGHT),
                    navItem,
                    new Runnable() {
                        @Override
                        public void run() {
                            if (handler != null) {
                                handler.open(navItem);
                            }
                        }
                    }),
                TerminalLayoutMetrics.BUTTON_HEIGHT + 1);
        }
        navSurface.addChild(navScroll);
        return navSurface;
    }

    public static PanelContainer createSectionBody(TerminalPanelFactory panels, GuiRect bounds,
        final TerminalHomeScreenModel model, Runnable refreshAction, Runnable closeAction,
        TerminalBankSectionState bankSectionState, BankActionHandler bankActionHandler,
        TerminalMarketSectionState marketSectionState, MarketActionHandler marketActionHandler) {
        return createSectionBody(panels, bounds, model, refreshAction, closeAction, bankSectionState, bankActionHandler,
            marketSectionState, marketActionHandler, null, null);
    }

    public static PanelContainer createSectionBody(TerminalPanelFactory panels, GuiRect bounds,
        final TerminalHomeScreenModel model, Runnable refreshAction, Runnable closeAction,
        TerminalBankSectionState bankSectionState, BankActionHandler bankActionHandler,
        TerminalMarketSectionState marketSectionState, MarketActionHandler marketActionHandler,
        @Nullable TerminalServerToolsSectionState serverToolsSectionState,
        @Nullable ServerToolsActionHandler serverToolsActionHandler) {
        PanelContainer content = new PanelContainer();
        content.setBounds(bounds);
        final TerminalHomeScreenModel.PageSnapshotModel snapshot = TerminalSectionRouter.resolveSnapshot(model);
        boolean serverToolsPage = snapshot.hasServerToolsSectionModel();
        int footerHeight = 0;
        int topInset = 0;
        int bodyAvailableHeight = Math.max(28, bounds.getHeight() - topInset - footerHeight);
        int sectionX = bounds.getX();
        int sectionY = bounds.getY() + topInset;
        int sectionWidth = Math.max(1, bounds.getWidth());
        if (snapshot.hasBankSectionModel()) {
            TerminalBankSection bankSection = new TerminalBankSection(panels, snapshot.getBankSectionModel(), bankSectionState, new TerminalBankSection.ActionHandler() {
                @Override
                public void openAccount() {
                    if (bankActionHandler != null) {
                        bankActionHandler.openAccount();
                    }
                }

                @Override
                public void openTransferConfirm() {
                    if (bankActionHandler != null) {
                        bankActionHandler.openTransferConfirm();
                    }
                }
            });
            VerticalScrollPanel bankScroll = panels.createScrollPanel(new GuiRect(sectionX, sectionY, sectionWidth, bodyAvailableHeight), 0, 0);
            int preferredHeight = Math.max(bodyAvailableHeight, sectionWidth < 500 ? 530 : 292);
            bankSection.setBounds(new GuiRect(sectionX, 0, sectionWidth, preferredHeight));
            bankScroll.addScrollableChild(bankSection, preferredHeight);
            content.addChild(bankScroll);
        } else if (snapshot.hasCustomMarketSectionModel()) {
            TerminalCustomMarketSection customSection = new TerminalCustomMarketSection(
                panels,
                snapshot.getCustomMarketSectionModel(),
                marketSectionState == null ? null : marketSectionState.getCustomState(),
                new TerminalCustomMarketSection.ActionHandler() {
                    @Override
                    public void selectListing(String scope, String listingId) {
                        if (marketActionHandler != null) {
                            marketActionHandler.selectCustomListing(scope, listingId);
                        }
                    }

                    @Override
                    public void refreshBrowse() {
                        if (marketActionHandler != null) marketActionHandler.refreshCustomBrowse();
                    }

                    @Override
                    public void changeBrowsePage(int pageIndex) {
                        if (marketActionHandler != null) marketActionHandler.changeCustomBrowsePage(pageIndex);
                    }

                    @Override
                    public void openBuyConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openCustomBuyConfirm();
                        }
                    }

                    @Override
                    public void openPublishConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openCustomPublishConfirm();
                        }
                    }

                    @Override
                    public void openCancelConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openCustomCancelConfirm();
                        }
                    }

                    @Override
                    public void openClaimConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openCustomClaimConfirm();
                        }
                    }
            });
            customSection.setBounds(new GuiRect(sectionX, sectionY, sectionWidth, bodyAvailableHeight));
            content.addChild(customSection);
        } else if (snapshot.hasExchangeMarketSectionModel()) {
            TerminalExchangeMarketSection exchangeSection = new TerminalExchangeMarketSection(
                panels,
                snapshot.getExchangeMarketSectionModel(),
                marketSectionState == null ? null : marketSectionState.getExchangeState(),
                new TerminalExchangeMarketSection.ActionHandler() {
                    @Override
                    public void selectTarget(String targetCode) {
                        if (marketActionHandler != null) {
                            marketActionHandler.selectExchangeTarget(targetCode);
                        }
                    }

                    @Override
                    public void refreshBrowse() {
                        if (marketActionHandler != null) marketActionHandler.refreshExchangeBrowse();
                    }

                    @Override
                    public void changeBrowsePage(int pageIndex) {
                        if (marketActionHandler != null) marketActionHandler.changeExchangeBrowsePage(pageIndex);
                    }

                    @Override
                    public void refreshQuote() {
                        if (marketActionHandler != null) {
                            marketActionHandler.refreshExchangeQuote();
                        }
                    }

                    @Override
                    public void openExchangeConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openExchangeConfirm();
                        }
                    }
            });
            exchangeSection.setBounds(new GuiRect(sectionX, sectionY, sectionWidth, bodyAvailableHeight));
            content.addChild(exchangeSection);
        } else if (snapshot.hasMarketSectionModel()) {
            TerminalMarketSection marketSection = new TerminalMarketSection(
                panels,
                snapshot.getMarketSectionModel(),
                marketSectionState,
                new TerminalMarketSection.ActionHandler() {
                    @Override
                    public void openMarketOverview() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openMarketOverview();
                        }
                    }

                    @Override
                    public void openHelp() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openMarketHelp();
                        }
                    }

                    @Override
                    public void openStandardizedMarket() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openStandardizedMarket();
                        }
                    }

                    @Override
                    public void openCustomMarket() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openCustomMarket();
                        }
                    }

                    @Override
                    public void openExchangeMarket() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openExchangeMarket();
                        }
                    }

                    @Override
                    public void selectProduct(String productKey) {
                        if (marketActionHandler != null) {
                            marketActionHandler.selectProduct(productKey);
                        }
                    }

                    @Override
                    public void refreshProductBrowser() {
                        if (marketActionHandler != null) {
                            marketActionHandler.refreshProductBrowser();
                        }
                    }

                    @Override
                    public void changeProductBrowserPage(int pageIndex) {
                        if (marketActionHandler != null) {
                            marketActionHandler.changeProductBrowserPage(pageIndex);
                        }
                    }

                    @Override
                    public void openDepositHeldConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openDepositHeldConfirm();
                        }
                    }

                    @Override
                    public void openLimitBuyConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openLimitBuyConfirm();
                        }
                    }

                    @Override
                    public void openLimitSellConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openLimitSellConfirm();
                        }
                    }

                    @Override
                    public void openInstantBuyConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openInstantBuyConfirm();
                        }
                    }

                    @Override
                    public void openInstantSellConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openInstantSellConfirm();
                        }
                    }

                    @Override
                    public void openOrderConfirm(TerminalMarketSectionState.OrderSide side,
                        TerminalMarketSectionState.OrderType type) {
                        if (marketActionHandler != null) {
                            marketActionHandler.openOrderConfirm(side, type);
                        }
                    }

                    @Override
                    public void openCancelOrderConfirm(String orderId) {
                        if (marketActionHandler != null) {
                            marketActionHandler.openCancelOrderConfirm(orderId);
                        }
                    }

                    @Override
                    public void openStandardizedHistory() {
                        if (marketActionHandler != null) marketActionHandler.openStandardizedHistory();
                    }

                    @Override
                    public void refreshStandardizedHistory() {
                        if (marketActionHandler != null) marketActionHandler.refreshStandardizedHistory();
                    }

                    @Override
                    public void openClaimConfirm(String custodyId) {
                        if (marketActionHandler != null) {
                            marketActionHandler.openClaimConfirm(custodyId);
                        }
                    }
                });
            marketSection.setBounds(new GuiRect(sectionX, sectionY, sectionWidth, bodyAvailableHeight));
            content.addChild(marketSection);
        } else if (snapshot.hasServerToolsSectionModel()) {
            TerminalServerToolsSection serverToolsSection = new TerminalServerToolsSection(
                panels,
                snapshot.getServerToolsSectionModel(),
                serverToolsSectionState,
                new TerminalServerToolsSection.ActionHandler() {
                    @Override
                    public void selectWarp(String warpName) {
                        if (serverToolsActionHandler != null) {
                            serverToolsActionHandler.selectWarp(warpName);
                        }
                    }

                    @Override
                    public void confirmWarp() {
                        if (serverToolsActionHandler != null) {
                            String selectedName = serverToolsSectionState != null ? serverToolsSectionState.getSelectedWarpName() : "";
                            serverToolsActionHandler.confirmWarp(selectedName);
                        }
                    }
                });
            serverToolsSection.setBounds(new GuiRect(sectionX, sectionY, sectionWidth, bodyAvailableHeight));
            content.addChild(serverToolsSection);
        } else {
            int scrollX = sectionX;
            int scrollY = sectionY;
            int scrollWidth = sectionWidth;
            VerticalScrollPanel bodyScroll = panels.createScrollPanel(
                new GuiRect(scrollX, scrollY, scrollWidth, bodyAvailableHeight),
                0,
                1);
            List<TerminalHomeScreenModel.SectionModel> sections = snapshot.getSections();
            int homeSectionContentWidth = TerminalLayoutMetrics.contentWidth(scrollWidth, 0);
            int sectionHeight = computeEvenSectionHeight(bodyAvailableHeight, sections.size(), 1,
                computeHomeSectionHeight(sections, homeSectionContentWidth));
            for (int i = 0; i < sections.size(); i++) {
                TerminalHomeSection section = new TerminalHomeSection(sections.get(i));
                section.setBounds(new GuiRect(scrollX, 0, scrollWidth, sectionHeight));
                bodyScroll.addScrollableChild(section, sectionHeight);
            }

            List<TerminalHomeScreenModel.NotificationModel> notifications = model.getNotifications();
            for (int i = 0; i < notifications.size(); i++) {
                bodyScroll.addScrollableChild(
                    panels.createNotificationCard(new GuiRect(scrollX, 0, scrollWidth, computeNotificationHeight(notifications.get(i), homeSectionContentWidth)),
                        notifications.get(i)),
                    computeNotificationHeight(notifications.get(i), homeSectionContentWidth));
            }
            content.addChild(bodyScroll);
        }

        return content;
    }

    static int computeEvenSectionHeight(int availableHeight, int sectionCount, int gap, int preferredHeight) {
        int count = Math.max(1, sectionCount);
        int safeGap = Math.max(0, gap);
        int totalGap = Math.max(0, count - 1) * safeGap;
        int heightFromAvailable = Math.max(1, (Math.max(1, availableHeight) - totalGap) / count);
        int preferred = Math.max(1, preferredHeight);
        return Math.max(preferred, Math.min(heightFromAvailable, preferred + 8));
    }

    static int computeStackHeight(int rowCount, int rowHeight, int gap) {
        int count = Math.max(0, rowCount);
        if (count == 0) {
            return 0;
        }
        return count * Math.max(1, rowHeight) + Math.max(0, count - 1) * Math.max(0, gap);
    }

    private static int computeHomeSectionHeight(List<TerminalHomeScreenModel.SectionModel> sections, int width) {
        int maxHeight = 38;
        for (TerminalHomeScreenModel.SectionModel section : sections) {
            int height = 6
                + TerminalLayoutMetrics.labelHeight(section.getTitle(), width, 1)
                + TerminalLayoutMetrics.labelHeight(section.getSummary(), width, 1)
                + TerminalLayoutMetrics.labelHeight(section.getDetail(), width, 1);
            maxHeight = Math.max(maxHeight, height);
        }
        return maxHeight;
    }

    private static int computeNotificationHeight(TerminalHomeScreenModel.NotificationModel notification, int width) {
        if (notification == null) {
            return 38;
        }
        return Math.max(32, 6
            + TerminalLayoutMetrics.labelHeight(notification.getTitle(), width, 1)
            + TerminalLayoutMetrics.labelHeight(notification.getBody(), width, 1));
    }

    private static final class SignalStatusPanel extends PanelContainer {

        private SignalStatusPanel(GuiRect bounds) {
            setBounds(bounds);
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            TerminalIconPainter.draw(TerminalIconKind.SIGNAL, bounds.getX() + 2, bounds.getY(), Math.max(8, Math.min(10, bounds.getHeight())),
                TerminalIconPainter.ICON_GREEN);
        }
    }

    private static final class FixedNavigationPanel extends PanelContainer {

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            Gui.drawRect(bounds.getRight() - 1, bounds.getY(), bounds.getRight(), bounds.getBottom(),
                scene.getTheme().color(ThemeColorKey.PANEL_BORDER));
        }
    }

    private static final class WindowTitleBarPanel extends PanelContainer {

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            RoundedRectPainter.drawSolid(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(),
                scene.getTheme().color(ThemeColorKey.PANEL_ACCENT));
            Gui.drawRect(bounds.getX(), bounds.getBottom() - 1, bounds.getRight(), bounds.getBottom(),
                scene.getTheme().color(ThemeColorKey.PANEL_BORDER));
        }
    }
}
