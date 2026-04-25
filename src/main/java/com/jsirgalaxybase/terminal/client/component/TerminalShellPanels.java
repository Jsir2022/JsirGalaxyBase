package com.jsirgalaxybase.terminal.client.component;

import java.util.List;
import java.util.function.Supplier;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.framework.VerticalScrollPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;

public final class TerminalShellPanels {

    private static final int PADDING = 10;

    private TerminalShellPanels() {}

    public interface NavigationHandler {

        void open(TerminalHomeScreenModel.NavItemModel navItem);
    }

    public interface BankActionHandler {

        void openAccount();

        void openTransferConfirm();
    }

    public interface MarketActionHandler {

        void openMarketOverview();

        void openStandardizedMarket();

        void openCustomMarket();

        void openExchangeMarket();

        void selectProduct(String productKey);

        void openLimitBuyConfirm();

        void openClaimConfirm(String custodyId);

        void selectCustomListing(String scope, String listingId);

        void openCustomBuyConfirm();

        void openCustomCancelConfirm();

        void openCustomClaimConfirm();

        void selectExchangeTarget(String targetCode);

        void refreshExchangeQuote();

        void openExchangeConfirm();
    }

    public static PanelContainer createStatusBand(TerminalPanelFactory panels, GuiRect bounds,
        final TerminalHomeScreenModel model, Runnable infoAction) {
        TexturedCanvasPanel band = panels.createSurface(bounds, ThemeColorKey.PANEL_ACCENT);
        final TerminalHomeScreenModel.StatusBandModel statusBand = model.getStatusBand();
        int badgeWidth = Math.min(96, Math.max(72, bounds.getWidth() / 6));
        int infoButtonWidth = 84;
        band.addChild(panels.createLabel(
            new GuiRect(bounds.getX() + PADDING, bounds.getY() + 6, bounds.getWidth() - badgeWidth - infoButtonWidth - 28, 10),
            new Supplier<String>() {
                @Override
                public String get() {
                    return statusBand.getEyebrow() + " / " + model.getSelectedNavItem().getLabel();
                }
            },
            ThemeColorKey.TEXT_SECONDARY,
            false));
        band.addChild(panels.createLabel(
            new GuiRect(bounds.getX() + PADDING, bounds.getY() + 16, bounds.getWidth() - badgeWidth - infoButtonWidth - 28, 12),
            new Supplier<String>() {
                @Override
                public String get() {
                    return model.getTerminalTitle();
                }
            },
            ThemeColorKey.TEXT_PRIMARY,
            false));
        band.addChild(panels.createLabel(
            new GuiRect(bounds.getX() + PADDING, bounds.getY() + 28, bounds.getWidth() - badgeWidth - infoButtonWidth - 28, 16),
            new Supplier<String>() {
                @Override
                public String get() {
                    return model.getTerminalSubtitle() + " | " + statusBand.getDetail();
                }
            },
            ThemeColorKey.TEXT_SECONDARY,
            false));
        band.addChild(panels.createBadge(
            new GuiRect(bounds.getRight() - badgeWidth - infoButtonWidth - 12, bounds.getY() + 6, badgeWidth, bounds.getHeight() - 12),
            new Supplier<String>() {
                @Override
                public String get() {
                    return statusBand.getBadgeLabel();
                }
            },
            new Supplier<String>() {
                @Override
                public String get() {
                    return statusBand.getBadgeValue();
                }
            }));
        band.addChild(panels.createButton(
            new GuiRect(bounds.getRight() - infoButtonWidth - 10, bounds.getY() + bounds.getHeight() - 24, infoButtonWidth, 16),
            new Supplier<String>() {
                @Override
                public String get() {
                    return "壳层说明";
                }
            },
            infoAction,
            null));
        return band;
    }

    public static PanelContainer createNavigationRail(TerminalPanelFactory panels, GuiRect bounds,
        TerminalHomeScreenModel model, NavigationHandler handler) {
        TexturedCanvasPanel navSurface = panels.createSurface(bounds, ThemeColorKey.PANEL_FILL);
        navSurface.addChild(panels.createLabel(
            new GuiRect(bounds.getX() + 8, bounds.getY() + 6, bounds.getWidth() - 16, 12),
            new Supplier<String>() {
                @Override
                public String get() {
                    return "终端导航";
                }
            },
            ThemeColorKey.TEXT_PRIMARY,
            false));
        navSurface.addChild(panels.createLabel(
            new GuiRect(bounds.getX() + 8, bounds.getY() + 18, bounds.getWidth() - 16, 16),
            new Supplier<String>() {
                @Override
                public String get() {
                    return "phase 4 起，主体区由 selectedPageId 驱动 section 宿主切换。";
                }
            },
            ThemeColorKey.TEXT_SECONDARY,
            false));
        List<TerminalHomeScreenModel.NavItemModel> navItems = model.getNavItems();
        int scrollY = bounds.getY() + 40;
        int scrollHeight = Math.max(20, bounds.getBottom() - scrollY - 6);
        VerticalScrollPanel navScroll = panels.createScrollPanel(
            new GuiRect(bounds.getX() + 4, scrollY, bounds.getWidth() - 8, scrollHeight),
            2,
            4);
        for (final TerminalHomeScreenModel.NavItemModel navItem : navItems) {
            navScroll.addScrollableChild(
                panels.createNavigationItem(
                    new GuiRect(bounds.getX() + 6, 0, bounds.getWidth() - 12, 34),
                    navItem,
                    new Runnable() {
                        @Override
                        public void run() {
                            if (handler != null) {
                                handler.open(navItem);
                            }
                        }
                    }),
                34);
        }
        navSurface.addChild(navScroll);
        return navSurface;
    }

    public static PanelContainer createSectionBody(TerminalPanelFactory panels, GuiRect bounds,
        final TerminalHomeScreenModel model, Runnable refreshAction, Runnable closeAction,
        TerminalBankSectionState bankSectionState, BankActionHandler bankActionHandler,
        TerminalMarketSectionState marketSectionState, MarketActionHandler marketActionHandler) {
        TexturedCanvasPanel content = panels.createSurface(bounds, ThemeColorKey.PANEL_FILL);
        final TerminalHomeScreenModel.PageSnapshotModel snapshot = TerminalSectionRouter.resolveSnapshot(model);
        content.addChild(panels.createLabel(
            new GuiRect(bounds.getX() + PADDING, bounds.getY() + 10, bounds.getWidth() - 24, 12),
            new Supplier<String>() {
                @Override
                public String get() {
                    return snapshot.getTitle();
                }
            },
            ThemeColorKey.TEXT_PRIMARY,
            false));
        content.addChild(panels.createLabel(
            new GuiRect(bounds.getX() + PADDING, bounds.getY() + 22, bounds.getWidth() - 24, 20),
            new Supplier<String>() {
                @Override
                public String get() {
                    return snapshot.getLead();
                }
            },
            ThemeColorKey.TEXT_SECONDARY,
            false));

        int footerHeight = 24;
        int topBarHeight = 42;
        int bodyAvailableHeight = Math.max(28, bounds.getHeight() - topBarHeight - footerHeight - 6);
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
            bankSection.setBounds(new GuiRect(
                bounds.getX() + PADDING,
                bounds.getY() + topBarHeight,
                bounds.getWidth() - PADDING * 2,
                bodyAvailableHeight));
            content.addChild(bankSection);
        } else if (snapshot.hasCustomMarketSectionModel()) {
            TerminalCustomMarketSection customSection = new TerminalCustomMarketSection(
                panels,
                snapshot.getCustomMarketSectionModel(),
                marketSectionState == null ? null : marketSectionState.getCustomState(),
                new TerminalCustomMarketSection.ActionHandler() {
                    @Override
                    public void openMarketOverview() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openMarketOverview();
                        }
                    }

                    @Override
                    public void selectListing(String scope, String listingId) {
                        if (marketActionHandler != null) {
                            marketActionHandler.selectCustomListing(scope, listingId);
                        }
                    }

                    @Override
                    public void openBuyConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openCustomBuyConfirm();
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
            customSection.setBounds(new GuiRect(
                bounds.getX() + PADDING,
                bounds.getY() + topBarHeight,
                bounds.getWidth() - PADDING * 2,
                bodyAvailableHeight));
            content.addChild(customSection);
        } else if (snapshot.hasExchangeMarketSectionModel()) {
            TerminalExchangeMarketSection exchangeSection = new TerminalExchangeMarketSection(
                panels,
                snapshot.getExchangeMarketSectionModel(),
                marketSectionState == null ? null : marketSectionState.getExchangeState(),
                new TerminalExchangeMarketSection.ActionHandler() {
                    @Override
                    public void openMarketOverview() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openMarketOverview();
                        }
                    }

                    @Override
                    public void selectTarget(String targetCode) {
                        if (marketActionHandler != null) {
                            marketActionHandler.selectExchangeTarget(targetCode);
                        }
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
            exchangeSection.setBounds(new GuiRect(
                bounds.getX() + PADDING,
                bounds.getY() + topBarHeight,
                bounds.getWidth() - PADDING * 2,
                bodyAvailableHeight));
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
                    public void openLimitBuyConfirm() {
                        if (marketActionHandler != null) {
                            marketActionHandler.openLimitBuyConfirm();
                        }
                    }

                    @Override
                    public void openClaimConfirm(String custodyId) {
                        if (marketActionHandler != null) {
                            marketActionHandler.openClaimConfirm(custodyId);
                        }
                    }
                });
            marketSection.setBounds(new GuiRect(
                bounds.getX() + PADDING,
                bounds.getY() + topBarHeight,
                bounds.getWidth() - PADDING * 2,
                bodyAvailableHeight));
            content.addChild(marketSection);
        } else {
            int scrollX = bounds.getX() + PADDING;
            int scrollY = bounds.getY() + topBarHeight;
            int scrollWidth = Math.max(1, bounds.getWidth() - PADDING * 2);
            VerticalScrollPanel bodyScroll = panels.createScrollPanel(
                new GuiRect(scrollX, scrollY, scrollWidth, bodyAvailableHeight),
                0,
                6);
            List<TerminalHomeScreenModel.SectionModel> sections = snapshot.getSections();
            int sectionHeight = computeEvenSectionHeight(bodyAvailableHeight, sections.size(), 6, 54);
            for (int i = 0; i < sections.size(); i++) {
                TerminalHomeSection section = new TerminalHomeSection(sections.get(i));
                section.setBounds(new GuiRect(scrollX, 0, scrollWidth, sectionHeight));
                bodyScroll.addScrollableChild(section, sectionHeight);
            }

            List<TerminalHomeScreenModel.NotificationModel> notifications = model.getNotifications();
            for (int i = 0; i < notifications.size(); i++) {
                bodyScroll.addScrollableChild(
                    panels.createNotificationCard(new GuiRect(scrollX, 0, scrollWidth, 52), notifications.get(i)),
                    52);
            }
            content.addChild(bodyScroll);
        }

        content.addChild(panels.createLabel(
            new GuiRect(bounds.getX() + PADDING, bounds.getBottom() - footerHeight + 1, bounds.getWidth() - 248, 16),
            new Supplier<String>() {
                @Override
                public String get() {
                    return "会话标识: " + model.getSessionToken();
                }
            },
            ThemeColorKey.TEXT_SECONDARY,
            false));
        ButtonPanel refreshButton = panels.createButton(
            new GuiRect(bounds.getRight() - 216, bounds.getBottom() - footerHeight, 96, 18),
            new Supplier<String>() {
                @Override
                public String get() {
                    return snapshot.hasBankSectionModel() ? "刷新银行页"
                        : snapshot.hasMarketSectionModel() ? ("market_standardized".equals(model.getSelectedPageId()) ? "刷新标准市场" : "刷新市场总览")
                            : "刷新分区";
                }
            },
            refreshAction,
            null);
        ButtonPanel closeButton = panels.createButton(
            new GuiRect(bounds.getRight() - 110, bounds.getBottom() - footerHeight, 96, 18),
            new Supplier<String>() {
                @Override
                public String get() {
                    return "关闭终端";
                }
            },
            closeAction,
            null);
        content.addChild(refreshButton);
        content.addChild(closeButton);
        return content;
    }

    static int computeEvenSectionHeight(int availableHeight, int sectionCount, int gap, int preferredHeight) {
        int count = Math.max(1, sectionCount);
        int safeGap = Math.max(0, gap);
        int totalGap = Math.max(0, count - 1) * safeGap;
        int heightFromAvailable = Math.max(1, (Math.max(1, availableHeight) - totalGap) / count);
        return Math.max(1, Math.min(Math.max(1, preferredHeight), heightFromAvailable));
    }

    static int computeStackHeight(int rowCount, int rowHeight, int gap) {
        int count = Math.max(0, rowCount);
        if (count == 0) {
            return 0;
        }
        return count * Math.max(1, rowHeight) + Math.max(0, count - 1) * Math.max(0, gap);
    }
}
