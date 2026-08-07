package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.AbstractGuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

public final class TerminalMarketSection extends PanelContainer {

    private static final ResourceLocation CLICK_SOUND = new ResourceLocation("gui.button.press");
    private static final int CARD_GAP = 8;
    private static final int CARD_PADDING = 8;
    private static final int HEADER_HEIGHT = 12;

    public interface ActionHandler {

        void openMarketOverview();

        void openHelp();

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

        void openClaimConfirm(String custodyId);
    }

    private final TerminalPanelFactory panels;
    private final TerminalMarketSectionModel model;
    private final TerminalMarketSectionState state;
    private final ActionHandler actionHandler;
    private final LabelPanel titleLabel;
    private final LabelPanel leadLabel;

    private OverviewMarketCardPanel overviewStandardizedCard;
    private OverviewMarketCardPanel overviewCustomCard;
    private OverviewMarketCardPanel overviewExchangeCard;
    private OverviewStatusCardPanel overviewStatusCard;
    private OverviewHelpCardPanel overviewHelpCard;

    private BrowserWorkbenchPanel browserPanel;
    private DetailWorkbenchPanel detailPanel;

    private TerminalTextFieldPanel limitBuyPriceField;
    private TerminalTextFieldPanel limitBuyQuantityField;
    private TerminalTextFieldPanel limitSellPriceField;
    private TerminalTextFieldPanel limitSellQuantityField;
    private TerminalTextFieldPanel instantBuyQuantityField;
    private TerminalTextFieldPanel instantSellQuantityField;

    public TerminalMarketSection(TerminalPanelFactory panels, TerminalMarketSectionModel model,
        TerminalMarketSectionState state, ActionHandler actionHandler) {
        this.panels = panels;
        this.model = model == null ? TerminalMarketSectionModel.placeholder("market") : model;
        this.state = state == null ? new TerminalMarketSectionState() : state;
        this.actionHandler = actionHandler;
        final boolean centeredHeader = this.model.isOverviewRoute();
        this.titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return TerminalMarketShell.buildSectionTitle(TerminalMarketSection.this.model);
            }
        }, ThemeColorKey.TEXT_PRIMARY, centeredHeader);
        this.leadLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return TerminalMarketShell.buildSectionLead(TerminalMarketSection.this.model);
            }
        }, ThemeColorKey.TEXT_SECONDARY, centeredHeader);
        addChild(titleLabel);
        addChild(leadLabel);

        if (this.model.isOverviewRoute()) {
            configureOverviewRoute();
        } else {
            configureStandardizedRoute();
        }
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        if (model.isOverviewRoute()) {
            titleLabel.setBounds(new GuiRect(bounds.getX(), bounds.getY(), bounds.getWidth(), 12));
            leadLabel.setBounds(new GuiRect(bounds.getX(), bounds.getY() + 13, bounds.getWidth(), 16));
            layoutOverview(bounds);
        } else {
            titleLabel.setBounds(new GuiRect(bounds.getX(), bounds.getY(), 0, 0));
            leadLabel.setBounds(new GuiRect(bounds.getX(), bounds.getY(), 0, 0));
            layoutStandardized(bounds);
        }
    }

    private void configureOverviewRoute() {
        TerminalMarketSectionContent.OverviewEntrySummary standardizedEntry =
            TerminalMarketSectionContent.buildStandardizedOverviewEntry(model);
        TerminalMarketSectionContent.OverviewEntrySummary customEntry =
            TerminalMarketSectionContent.buildCustomOverviewEntry();
        TerminalMarketSectionContent.OverviewEntrySummary exchangeEntry =
            TerminalMarketSectionContent.buildExchangeOverviewEntry();
        TerminalMarketSectionContent.OverviewEntrySummary helpEntry =
            TerminalMarketSectionContent.buildOverviewHelpEntry();

        overviewStandardizedCard = new OverviewMarketCardPanel(
            standardizedEntry,
            OverviewIconKind.STANDARDIZED,
            new Runnable() {
                @Override
                public void run() {
                    if (actionHandler != null) {
                        actionHandler.openStandardizedMarket();
                    }
                }
            });
        overviewCustomCard = new OverviewMarketCardPanel(
            customEntry,
            OverviewIconKind.CUSTOM,
            new Runnable() {
                @Override
                public void run() {
                    if (actionHandler != null) {
                        actionHandler.openCustomMarket();
                    }
                }
            });
        overviewExchangeCard = new OverviewMarketCardPanel(
            exchangeEntry,
            OverviewIconKind.EXCHANGE,
            new Runnable() {
                @Override
                public void run() {
                    if (actionHandler != null) {
                        actionHandler.openExchangeMarket();
                    }
                }
            });
        overviewStatusCard = new OverviewStatusCardPanel();
        overviewHelpCard = new OverviewHelpCardPanel(helpEntry);

        addChild(overviewStandardizedCard);
        addChild(overviewCustomCard);
        addChild(overviewExchangeCard);
        addChild(overviewStatusCard);
        addChild(overviewHelpCard);
    }

    private void configureStandardizedRoute() {
        limitBuyPriceField = numberField(new Supplier<String>() {
            @Override
            public String get() {
                return state.getLimitBuyPriceText();
            }
        }, new java.util.function.Consumer<String>() {
            @Override
            public void accept(String value) {
                state.setLimitBuyPriceText(value);
            }
        }, TerminalMarketSectionState.FocusField.LIMIT_BUY_PRICE, "买单价格");
        limitBuyQuantityField = numberField(new Supplier<String>() {
            @Override
            public String get() {
                return state.getLimitBuyQuantityText();
            }
        }, new java.util.function.Consumer<String>() {
            @Override
            public void accept(String value) {
                state.setLimitBuyQuantityText(value);
            }
        }, TerminalMarketSectionState.FocusField.LIMIT_BUY_QUANTITY, "买入数量");
        limitSellPriceField = numberField(new Supplier<String>() {
            @Override
            public String get() {
                return state.getLimitSellPriceText();
            }
        }, new java.util.function.Consumer<String>() {
            @Override
            public void accept(String value) {
                state.setLimitSellPriceText(value);
            }
        }, TerminalMarketSectionState.FocusField.LIMIT_SELL_PRICE, "卖单价格");
        limitSellQuantityField = numberField(new Supplier<String>() {
            @Override
            public String get() {
                return state.getLimitSellQuantityText();
            }
        }, new java.util.function.Consumer<String>() {
            @Override
            public void accept(String value) {
                state.setLimitSellQuantityText(value);
            }
        }, TerminalMarketSectionState.FocusField.LIMIT_SELL_QUANTITY, "卖出数量");
        instantBuyQuantityField = numberField(new Supplier<String>() {
            @Override
            public String get() {
                return state.getInstantBuyQuantityText();
            }
        }, new java.util.function.Consumer<String>() {
            @Override
            public void accept(String value) {
                state.setInstantBuyQuantityText(value);
            }
        }, TerminalMarketSectionState.FocusField.INSTANT_BUY_QUANTITY, "即时买入数量");
        instantSellQuantityField = numberField(new Supplier<String>() {
            @Override
            public String get() {
                return state.getInstantSellQuantityText();
            }
        }, new java.util.function.Consumer<String>() {
            @Override
            public void accept(String value) {
                state.setInstantSellQuantityText(value);
            }
        }, TerminalMarketSectionState.FocusField.INSTANT_SELL_QUANTITY, "即时卖出数量");

        browserPanel = new BrowserWorkbenchPanel();
        detailPanel = new DetailWorkbenchPanel();
        addChild(browserPanel);
        addChild(detailPanel);
    }

    private void layoutOverview(GuiRect bounds) {
        TerminalMarketShell.OverviewLayout layout = TerminalMarketShell.computeOverviewLayout(bounds);
        overviewStandardizedCard.setBounds(layout.getStandardizedBounds());
        overviewCustomCard.setBounds(layout.getCustomBounds());
        overviewExchangeCard.setBounds(layout.getExchangeBounds());
        overviewStatusCard.setBounds(layout.getStatusBounds());
        overviewHelpCard.setBounds(layout.getHelpBounds());
    }

    private void layoutStandardized(GuiRect bounds) {
        if (state.isStandardizedDetailView()) {
            browserPanel.setVisible(false);
            detailPanel.setVisible(true);
            detailPanel.setBounds(bounds);
            return;
        }
        browserPanel.setVisible(true);
        detailPanel.setVisible(false);
        browserPanel.setBounds(bounds);
        detailPanel.setBounds(new GuiRect(bounds.getX(), bounds.getY(), 0, 0));
    }

    private ButtonPanel button(final String text, Runnable action, Supplier<Boolean> enabled) {
        return panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return text;
            }
        }, action, enabled);
    }

    private TerminalTextFieldPanel numberField(Supplier<String> valueSupplier,
        java.util.function.Consumer<String> setter, final TerminalMarketSectionState.FocusField focusField, String hint) {
        return new TerminalTextFieldPanel(
            valueSupplier,
            setter,
            new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.valueOf(state.isFocused(focusField));
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    state.focus(focusField);
                }
            },
            hint,
            18,
            value -> Boolean.valueOf(value.charValue() >= '0' && value.charValue() <= '9'));
    }

    private TerminalTextFieldPanel textField(Supplier<String> valueSupplier,
        java.util.function.Consumer<String> setter, final TerminalMarketSectionState.FocusField focusField, String hint) {
        return new TerminalTextFieldPanel(
            valueSupplier,
            setter,
            new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.valueOf(state.isFocused(focusField));
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    state.focus(focusField);
                }
            },
            hint,
            32,
            null);
    }

    private LabelPanel sectionTitle(final String value) {
        return panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return value;
            }
        }, ThemeColorKey.TEXT_PRIMARY, false);
    }

    private LabelPanel secondaryLabel(final Supplier<String> value) {
        return panels.createLabel(new GuiRect(0, 0, 0, 0), value, ThemeColorKey.TEXT_SECONDARY, false);
    }

    private int actionBlockHeight(int fieldCount) {
        return 48 + fieldCount * 20;
    }

    private String compactLatestTradeSummary() {
        return "参考 " + compactCurrency(model.getLatestTradePrice());
    }

    private String compactVolumeSummary() {
        return "24h量 " + model.getVolume24h() + " / 额 " + compactCurrency(model.getTurnover24h());
    }

    private String compactOrderCount() {
        int count = 0;
        for (TerminalMarketSectionContent.OrderEntry entry : TerminalMarketSectionContent.buildOrderEntries(model)) {
            if (!entry.getOrderId().isEmpty()) {
                count++;
            }
        }
        return String.valueOf(count);
    }

    private int countCancelableOrders() {
        int count = 0;
        for (TerminalMarketSectionContent.OrderEntry entry : TerminalMarketSectionContent.buildOrderEntries(model)) {
            if (entry.isCancelable()) {
                count++;
            }
        }
        return count;
    }

    private int countClaimableEntries() {
        int count = 0;
        for (TerminalMarketSectionContent.ClaimEntry entry : TerminalMarketSectionContent.buildClaimEntries(model)) {
            if (!entry.getCustodyId().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private List<String> limitedBookLines(List<String> source, int max) {
        List<String> results = new ArrayList<String>();
        if (source == null || source.isEmpty()) {
            results.add("--");
            return results;
        }
        for (String line : source) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            results.add(compactBookLine(line.trim()));
            if (results.size() >= max) {
                break;
            }
        }
        if (results.isEmpty()) {
            results.add("--");
        }
        return results;
    }

    private String compactBookLine(String line) {
        if (line == null) { return "--"; }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("[买卖]价\\s+([0-9,]+)\\s*\\|\\s*剩余\\s+([0-9,]+).*")
            .matcher(line);
        return matcher.matches() ? matcher.group(1) + " x" + matcher.group(2) : line;
    }

    private static String compactCurrency(String value) {
        if (value == null) { return "--"; }
        return value.replace(" / STARCOIN", "").replace(" STARCOIN", "").trim();
    }

    private String compactBrowserFooter() {
        return "共 " + TerminalMarketSectionContent.countActiveProducts(model) + " 项";
    }

    private List<MarketBrowseItemModel> buildBrowseItems() {
        List<MarketBrowseItemModel> items = new ArrayList<MarketBrowseItemModel>();
        for (TerminalMarketSectionModel.CatalogProductModel product : model.getCatalogProducts()) {
            if (product == null || !product.isEnabled()) { continue; }
            TerminalMarketSectionModel.CatalogMarketSummaryModel summary = product.getMarketSummary();
            MarketBrowseItemModel item = new MarketBrowseItemModel(product.getProductKey(), product.getRegistryName() + ":" + product.getMeta(),
                product.getDisplayName(), String.valueOf(product.getReferencePrice()), product.getTradability(),
                summary.getLatestTrade(), summary.getBestBid(), summary.getBestAsk(), summary.getVolume24h(),
                summary.getAvailable(), summary.getEscrow(), summary.getClaimable(), summary.getDayChange(),
                summary.getPricePoints());
            if (state.getBrowserFilter() == TerminalMarketSectionState.BrowserFilter.TRADED
                && item.getPricePoints().isEmpty()) { continue; }
            if (state.getBrowserFilter() == TerminalMarketSectionState.BrowserFilter.BOOK
                && "无盘口".equals(item.getLiquidityLabel())) { continue; }
            items.add(item);
        }
        sortBrowseItems(items);
        return items;
    }

    private void sortBrowseItems(List<MarketBrowseItemModel> items) {
        final TerminalMarketSectionState.BrowserSort sort = state.getBrowserSort();
        if (sort == TerminalMarketSectionState.BrowserSort.DIRECTORY) { return; }
        Collections.sort(items, new java.util.Comparator<MarketBrowseItemModel>() {
            @Override
            public int compare(MarketBrowseItemModel left, MarketBrowseItemModel right) {
                if (sort == TerminalMarketSectionState.BrowserSort.GAIN) {
                    return Double.compare(right.getChangePercent(), left.getChangePercent());
                }
                if (sort == TerminalMarketSectionState.BrowserSort.LOSS) {
                    return Double.compare(left.getChangePercent(), right.getChangePercent());
                }
                long leftValue = sort == TerminalMarketSectionState.BrowserSort.VOLUME
                    ? parsePositiveLong(left.getVolume24h()) : parsePositiveLong(left.getLatestTrade());
                long rightValue = sort == TerminalMarketSectionState.BrowserSort.VOLUME
                    ? parsePositiveLong(right.getVolume24h()) : parsePositiveLong(right.getLatestTrade());
                return Long.compare(rightValue, leftValue);
            }
        });
    }

    private void drawCardFrame(GuiRect bounds, int borderColor, int fillColor) {
        RoundedRectPainter.draw(bounds, borderColor, fillColor);
    }

    private enum OverviewIconKind {
        STANDARDIZED,
        CUSTOM,
        EXCHANGE
    }

    private final class OverviewMarketCardPanel extends PanelContainer {

        private final TerminalMarketSectionContent.OverviewEntrySummary entry;
        private final OverviewIconKind iconKind;
        private final LabelPanel titleLabel;
        private final LabelPanel summaryLabel;
        private final LabelPanel statusLabel;
        private final ButtonPanel actionButton;

        private OverviewMarketCardPanel(TerminalMarketSectionContent.OverviewEntrySummary entry,
            OverviewIconKind iconKind, Runnable action) {
            this.entry = entry;
            this.iconKind = iconKind;
            this.titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return OverviewMarketCardPanel.this.entry.getTitle();
                }
            }, ThemeColorKey.TEXT_PRIMARY, true);
            this.summaryLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return OverviewMarketCardPanel.this.entry.getSummary();
                }
            }, ThemeColorKey.TEXT_SECONDARY, true);
            this.statusLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return OverviewMarketCardPanel.this.entry.getStatus();
                }
            }, ThemeColorKey.TEXT_SECONDARY, true);
            this.actionButton = button(entry.getActionLabel(), action, null);
            addChild(titleLabel);
            addChild(summaryLabel);
            addChild(statusLabel);
            addChild(actionButton);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int innerX = bounds.getX() + CARD_PADDING;
            int innerWidth = bounds.getWidth() - CARD_PADDING * 2;
            int iconSize = Math.min(32, Math.max(22, bounds.getHeight() / 4));
            int buttonY = bounds.getBottom() - 22;
            int titleY = Math.min(buttonY - 36, bounds.getY() + Math.max(34, iconSize + 12));
            titleLabel.setBounds(new GuiRect(innerX, titleY, innerWidth, 12));
            summaryLabel.setBounds(new GuiRect(innerX, titleY + 14, innerWidth, 10));
            statusLabel.setBounds(new GuiRect(innerX, titleY + 25, innerWidth, buttonY - titleY >= 36 ? 8 : 0));
            int buttonWidth = Math.min(118, Math.max(88, bounds.getWidth() - 24));
            actionButton.setBounds(new GuiRect(bounds.getX() + (bounds.getWidth() - buttonWidth) / 2,
                buttonY, buttonWidth, 18));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            drawCardFrame(bounds, 0xFF324152, 0xFF1A232E);
            int iconSize = Math.min(32, Math.max(22, bounds.getHeight() / 4));
            int iconX = bounds.getX() + (bounds.getWidth() - iconSize) / 2;
            int iconY = bounds.getY() + 10;
            TerminalMarketVisuals.drawMarketIcon(iconX, iconY, iconSize, iconKind.ordinal());
        }
    }

    private final class OverviewStatusCardPanel extends PanelContainer {

        private final LabelPanel headerLabel;
        private final List<LabelPanel> valueLabels = new ArrayList<LabelPanel>();

        private OverviewStatusCardPanel() {
            this.headerLabel = sectionTitle("共享运行状态");
            addChild(headerLabel);
            for (final String line : TerminalMarketSectionContent.buildOverviewSummaryLines(model)) {
                LabelPanel label = secondaryLabel(new Supplier<String>() {
                    @Override
                    public String get() {
                        return line;
                    }
                });
                valueLabels.add(label);
                addChild(label);
            }
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int innerX = bounds.getX() + CARD_PADDING;
            int innerWidth = bounds.getWidth() - CARD_PADDING * 2;
            headerLabel.setBounds(new GuiRect(innerX, bounds.getY() + 8, innerWidth, 12));
            int visible = Math.min(valueLabels.size(), Math.max(1, (bounds.getHeight() - 24) / 10));
            for (int i = 0; i < valueLabels.size(); i++) {
                valueLabels.get(i).setBounds(new GuiRect(innerX, bounds.getY() + 22 + i * 10,
                    innerWidth, i < visible ? 10 : 0));
            }
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            drawCardFrame(getBounds(), 0xFF324152, 0xFF1A232E);
        }
    }

    private final class OverviewHelpCardPanel extends PanelContainer {

        private final LabelPanel headerLabel;
        private final LabelPanel bodyLabel;
        private final LabelPanel hintLabel;

        private OverviewHelpCardPanel(final TerminalMarketSectionContent.OverviewEntrySummary entry) {
            this.headerLabel = sectionTitle("规则提示");
            this.bodyLabel = secondaryLabel(new Supplier<String>() {
                @Override
                public String get() {
                    return entry.getSummary();
                }
            });
            this.hintLabel = secondaryLabel(new Supplier<String>() {
                @Override
                public String get() {
                    return entry.getStatus();
                }
            });
            addChild(headerLabel);
            addChild(bodyLabel);
            addChild(hintLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int innerX = bounds.getX() + CARD_PADDING;
            int innerWidth = bounds.getWidth() - CARD_PADDING * 2;
            headerLabel.setBounds(new GuiRect(innerX, bounds.getY() + 8, innerWidth, 12));
            bodyLabel.setBounds(new GuiRect(innerX, bounds.getY() + 22, innerWidth, 10));
            hintLabel.setBounds(new GuiRect(innerX, bounds.getY() + 34, innerWidth, Math.max(0, bounds.getBottom() - bounds.getY() - 40)));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            drawCardFrame(getBounds(), 0xFF324152, 0xFF1A232E);
        }
    }

    private final class BrowserWorkbenchPanel extends PanelContainer {

        private final BrowserSearchPanel searchPanel;
        private final MarketItemGridPanel browserGrid;
        private final BrowserPagerPanel pagerPanel;

        private BrowserWorkbenchPanel() {
            this.searchPanel = new BrowserSearchPanel();
            this.browserGrid = new MarketItemGridPanel(buildBrowseItems(), new MarketItemGridPanel.Listener() {
                @Override
                public void select(MarketBrowseItemModel item) {
                    if (item == null || item.getKey().isEmpty()) { return; }
                    state.requestDetailProduct(item.getKey());
                    if (actionHandler != null) { actionHandler.selectProduct(item.getKey()); }
                }

                @Override
                public void scrollOffsetChanged(int offset) {
                    state.setBrowserGridScrollOffset(offset);
                }
            });
            this.pagerPanel = new BrowserPagerPanel();
            addChild(searchPanel);
            addChild(browserGrid);
            addChild(pagerPanel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int innerX = bounds.getX() + CARD_PADDING;
            int innerWidth = bounds.getWidth() - CARD_PADDING * 2;
            searchPanel.setBounds(new GuiRect(innerX, bounds.getY() + 8, innerWidth, 20));
            browserGrid.setBounds(new GuiRect(innerX, bounds.getY() + 28, innerWidth, Math.max(24, bounds.getHeight() - 52)));
            browserGrid.setScrollOffset(state.getBrowserGridScrollOffset());
            pagerPanel.setBounds(new GuiRect(innerX, bounds.getBottom() - 18, innerWidth, 14));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            drawCardFrame(getBounds(), 0xFF324152, 0xFF19222C);
        }
    }

    private final class BrowserSearchPanel extends PanelContainer {

        private final TerminalTextFieldPanel queryField;
        private final ButtonPanel searchButton;
        private final ButtonPanel filterButton;
        private final ButtonPanel sortButton;

        private BrowserSearchPanel() {
            this.queryField = textField(new Supplier<String>() {
                @Override
                public String get() {
                    return state.getBrowserQuery();
                }
            }, new java.util.function.Consumer<String>() {
                @Override
                public void accept(String value) {
                    state.setBrowserQuery(value);
                }
            }, TerminalMarketSectionState.FocusField.BROWSER_QUERY, "搜索商品名...");
            this.searchButton = button("查", new Runnable() {
                @Override
                public void run() {
                    state.focus(TerminalMarketSectionState.FocusField.NONE);
                    if (actionHandler != null) {
                        actionHandler.refreshProductBrowser();
                    }
                }
            }, null);
            this.filterButton = panels.createButton(new GuiRect(0, 0, 0, 0),
                () -> state.getBrowserFilterLabel(), () -> {
                    state.cycleBrowserFilter();
                    if (actionHandler != null) actionHandler.refreshProductBrowser();
                }, () -> Boolean.TRUE);
            this.sortButton = panels.createButton(new GuiRect(0, 0, 0, 0),
                () -> state.getBrowserSortLabel(), () -> {
                    state.cycleBrowserSort();
                    if (actionHandler != null) actionHandler.refreshProductBrowser();
                }, () -> Boolean.TRUE);
            addChild(queryField);
            addChild(searchButton);
            addChild(filterButton);
            addChild(sortButton);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int searchWidth = 18;
            int filterWidth = Math.min(48, Math.max(34, bounds.getWidth() / 10));
            int sortWidth = Math.min(44, Math.max(32, bounds.getWidth() / 11));
            int controlsWidth = searchWidth + filterWidth + sortWidth + 8;
            queryField.setBounds(new GuiRect(bounds.getX() + 3, bounds.getY() + 2,
                Math.max(0, bounds.getWidth() - controlsWidth - 6), Math.max(12, bounds.getHeight() - 4)));
            int controlX = bounds.getRight() - controlsWidth + 2;
            searchButton.setBounds(new GuiRect(controlX, bounds.getY() + 2, searchWidth, Math.max(12, bounds.getHeight() - 4)));
            filterButton.setBounds(new GuiRect(controlX + searchWidth + 2, bounds.getY() + 2, filterWidth,
                Math.max(12, bounds.getHeight() - 4)));
            sortButton.setBounds(new GuiRect(controlX + searchWidth + filterWidth + 4, bounds.getY() + 2, sortWidth,
                Math.max(12, bounds.getHeight() - 4)));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            drawCardFrame(bounds, 0xFF283544, 0xFF10161D);
        }
    }

    private final class BrowserPagerPanel extends PanelContainer {

        private final LabelPanel pageLabel;
        private final ButtonPanel previousButton;
        private final ButtonPanel nextButton;

        private BrowserPagerPanel() {
            this.pageLabel = secondaryLabel(new Supplier<String>() {
                @Override
                public String get() {
                    int pages = model.getCatalogTotalPages();
                    int current = model.getCatalogTotalEntries() <= 0 ? 0 : model.getCatalogPageIndex() + 1;
                    return "共 " + model.getCatalogTotalEntries() + " 项  " + current + " / " + pages;
                }
            });
            this.previousButton = button("<", new Runnable() {
                @Override
                public void run() {
                    if (actionHandler != null) {
                        actionHandler.changeProductBrowserPage(Math.max(0, model.getCatalogPageIndex() - 1));
                    }
                }
            }, new Supplier<Boolean>() {
                @Override
                public Boolean get() { return Boolean.valueOf(model.hasCatalogPreviousPage()); }
            });
            this.nextButton = button(">", new Runnable() {
                @Override
                public void run() {
                    if (actionHandler != null) {
                        actionHandler.changeProductBrowserPage(model.getCatalogPageIndex() + 1);
                    }
                }
            }, new Supplier<Boolean>() {
                @Override
                public Boolean get() { return Boolean.valueOf(model.hasCatalogNextPage()); }
            });
            addChild(pageLabel);
            addChild(previousButton);
            addChild(nextButton);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int buttonWidth = Math.min(16, Math.max(12, bounds.getHeight()));
            nextButton.setBounds(new GuiRect(bounds.getRight() - buttonWidth, bounds.getY(), buttonWidth, bounds.getHeight()));
            previousButton.setBounds(new GuiRect(bounds.getRight() - buttonWidth * 2 - 2, bounds.getY(), buttonWidth, bounds.getHeight()));
            pageLabel.setBounds(new GuiRect(bounds.getX(), bounds.getY() + 2,
                Math.max(0, bounds.getWidth() - buttonWidth * 2 - 6), 10));
        }
    }

    private final class DetailWorkbenchPanel extends PanelContainer {

        private final ProductSummaryPanel summaryPanel;
        private final MarketChartPanel chartPanel;
        private final OrderBookPanel orderBookPanel;
        private final StatusTileRowPanel statusTileRowPanel;
        private final ActionWorkbenchPanel actionPanel;

        private DetailWorkbenchPanel() {
            this.summaryPanel = new ProductSummaryPanel();
            this.chartPanel = new MarketChartPanel();
            this.orderBookPanel = new OrderBookPanel();
            this.statusTileRowPanel = new StatusTileRowPanel();
            this.actionPanel = new ActionWorkbenchPanel();
            addChild(summaryPanel);
            addChild(chartPanel);
            addChild(orderBookPanel);
            addChild(statusTileRowPanel);
            addChild(actionPanel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            MarketDetailLayout layout = MarketDetailLayout.within(bounds);
            summaryPanel.setBounds(layout.hero);
            chartPanel.setBounds(layout.chart);
            orderBookPanel.setBounds(layout.orderBook);
            actionPanel.setBounds(layout.ticket);
            statusTileRowPanel.setBounds(layout.footer);
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            drawCardFrame(getBounds(), 0xFF324152, 0xFF19222C);
        }
    }

    private final class ActionWorkbenchPanel extends PanelContainer {

        private final ButtonPanel buySideButton;
        private final ButtonPanel sellSideButton;
        private final ButtonPanel marketTypeButton;
        private final ButtonPanel limitTypeButton;
        private final ButtonPanel confirmButton;
        private final ButtonPanel cancelOrderButton;

        private ActionWorkbenchPanel() {
            buySideButton = button("买入", () -> { state.setOrderSide(TerminalMarketSectionState.OrderSide.BUY); setBounds(getBounds()); },
                () -> Boolean.TRUE);
            sellSideButton = button("卖出", () -> { state.setOrderSide(TerminalMarketSectionState.OrderSide.SELL); setBounds(getBounds()); },
                () -> Boolean.TRUE);
            marketTypeButton = button("市价", () -> { state.setOrderType(TerminalMarketSectionState.OrderType.MARKET); setBounds(getBounds()); },
                () -> Boolean.TRUE);
            limitTypeButton = button("限价", () -> { state.setOrderType(TerminalMarketSectionState.OrderType.LIMIT); setBounds(getBounds()); },
                () -> Boolean.TRUE);
            confirmButton = panels.createButton(new GuiRect(0, 0, 0, 0),
                () -> state.getOrderSide() == TerminalMarketSectionState.OrderSide.BUY ? "确认买入" : "确认卖出",
                () -> {
                    if (actionHandler != null) actionHandler.openOrderConfirm(state.getOrderSide(), state.getOrderType());
                }, () -> Boolean.valueOf(state.hasCompleteOrderTicket()));
            this.cancelOrderButton = button("撤单", new Runnable() {
                @Override
                public void run() {
                    if (actionHandler != null && state.hasPendingCancelOrderSelection()) {
                        actionHandler.openCancelOrderConfirm(state.getPendingCancelOrderId());
                    }
                }
            }, new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.valueOf(state.hasPendingCancelOrderSelection());
                }
            });
            addChild(limitBuyPriceField);
            addChild(limitBuyQuantityField);
            addChild(limitSellPriceField);
            addChild(limitSellQuantityField);
            addChild(instantBuyQuantityField);
            addChild(instantSellQuantityField);
            addChild(buySideButton);
            addChild(sellSideButton);
            addChild(marketTypeButton);
            addChild(limitTypeButton);
            addChild(confirmButton);
            addChild(cancelOrderButton);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int innerX = bounds.getX() + CARD_PADDING;
            int innerWidth = Math.max(0, bounds.getWidth() - CARD_PADDING * 2);
            int gap = 4;
            int fieldHeight = 14;
            int fieldWidth = Math.max(0, (innerWidth - gap) / 2);
            int y = bounds.getY() + 8;
            int buttonHeight = 16;
            buySideButton.setBounds(new GuiRect(innerX, y, fieldWidth, buttonHeight));
            sellSideButton.setBounds(new GuiRect(innerX + fieldWidth + gap, y,
                Math.max(0, innerWidth - fieldWidth - gap), buttonHeight));
            y += buttonHeight + gap;
            marketTypeButton.setBounds(new GuiRect(innerX, y, fieldWidth, buttonHeight));
            limitTypeButton.setBounds(new GuiRect(innerX + fieldWidth + gap, y,
                Math.max(0, innerWidth - fieldWidth - gap), buttonHeight));
            y += buttonHeight + gap;
            hideTradeFields();
            boolean buy = state.getOrderSide() == TerminalMarketSectionState.OrderSide.BUY;
            boolean limit = state.getOrderType() == TerminalMarketSectionState.OrderType.LIMIT;
            TerminalTextFieldPanel quantity = buy
                ? (limit ? limitBuyQuantityField : instantBuyQuantityField)
                : (limit ? limitSellQuantityField : instantSellQuantityField);
            TerminalTextFieldPanel price = buy ? limitBuyPriceField : limitSellPriceField;
            quantity.setVisible(true);
            quantity.setBounds(new GuiRect(innerX, y, limit ? fieldWidth : innerWidth, fieldHeight));
            if (limit) {
                price.setVisible(true);
                price.setBounds(new GuiRect(innerX + fieldWidth + gap, y,
                    Math.max(0, innerWidth - fieldWidth - gap), fieldHeight));
            }
            int actionY = bounds.getBottom() - buttonHeight - 8;
            boolean canCancel = state.hasPendingCancelOrderSelection();
            cancelOrderButton.setVisible(canCancel);
            if (canCancel) {
                int cancelWidth = Math.max(42, innerWidth / 3);
                confirmButton.setBounds(new GuiRect(innerX, actionY, innerWidth - cancelWidth - gap, buttonHeight));
                cancelOrderButton.setBounds(new GuiRect(bounds.getRight() - CARD_PADDING - cancelWidth, actionY,
                    cancelWidth, buttonHeight));
            } else {
                confirmButton.setBounds(new GuiRect(innerX, actionY, innerWidth, buttonHeight));
                cancelOrderButton.setBounds(new GuiRect(0, 0, 0, 0));
            }
        }

        private void hideTradeFields() {
            TerminalTextFieldPanel[] fields = { limitBuyPriceField, limitBuyQuantityField, limitSellPriceField,
                limitSellQuantityField, instantBuyQuantityField, instantSellQuantityField };
            for (TerminalTextFieldPanel field : fields) {
                field.setVisible(false);
                field.setBounds(new GuiRect(0, 0, 0, 0));
            }
        }

        private void layoutTradeFields(int fieldY, int fieldHeight, int gap) {
            int innerX = getBounds().getX() + CARD_PADDING;
            int innerWidth = getBounds().getWidth() - CARD_PADDING * 2;
            int fieldWidth = fieldHeight <= 0 ? 0 : Math.max(0, (innerWidth - gap * 3) / 4);
            limitBuyPriceField.setBounds(new GuiRect(innerX, fieldY, fieldWidth, fieldHeight));
            limitBuyQuantityField.setBounds(new GuiRect(innerX + fieldWidth + gap, fieldY, fieldWidth, fieldHeight));
            limitSellPriceField.setBounds(new GuiRect(innerX + (fieldWidth + gap) * 2, fieldY, fieldWidth, fieldHeight));
            limitSellQuantityField.setBounds(new GuiRect(innerX + (fieldWidth + gap) * 3, fieldY,
                Math.max(0, innerX + innerWidth - (innerX + (fieldWidth + gap) * 3)), fieldHeight));
            int instantWidth = fieldHeight <= 0 ? 0 : Math.max(0, (innerWidth - gap) / 2);
            instantBuyQuantityField.setBounds(new GuiRect(innerX, fieldY + fieldHeight + 2, instantWidth, fieldHeight));
            instantSellQuantityField.setBounds(new GuiRect(innerX + instantWidth + gap, fieldY + fieldHeight + 2,
                Math.max(0, innerX + innerWidth - (innerX + instantWidth + gap)), fieldHeight));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            drawCardFrame(getBounds(), 0xFF324152, 0xFF19222C);
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            if (font != null) {
                boolean buy = state.getOrderSide() == TerminalMarketSectionState.OrderSide.BUY;
                boolean limit = state.getOrderType() == TerminalMarketSectionState.OrderType.LIMIT;
                long quantity = parsePositiveLong(buy
                    ? (limit ? state.getLimitBuyQuantityText() : state.getInstantBuyQuantityText())
                    : (limit ? state.getLimitSellQuantityText() : state.getInstantSellQuantityText()));
                long price = limit
                    ? parsePositiveLong(buy ? state.getLimitBuyPriceText() : state.getLimitSellPriceText())
                    : parseMarketPrice(buy ? model.getLowestAsk() : model.getHighestBid());
                long estimate = saturatedMultiply(quantity, price);
                int x = getBounds().getX() + CARD_PADDING;
                int y = getBounds().getY() + 70;
                drawTrimmed(font, "数量 " + quantity + (limit ? "  限价 " + price : "  市价"), x, y,
                    getBounds().getWidth() - CARD_PADDING * 2, 0xFFBFCBDA);
                drawTrimmed(font, "预估 " + estimate, x, y + 12, getBounds().getWidth() - CARD_PADDING * 2,
                    0xFFF0C75E);
                String source = buy ? "结算: 银行余额" : "来源: 个人 Base Vault";
                drawTrimmed(font, source, x, y + 24, getBounds().getWidth() - CARD_PADDING * 2, 0xFFA9B8C8);
                drawTrimmed(font, "服务端复核价格与手续费", x, y + 36,
                    getBounds().getWidth() - CARD_PADDING * 2, 0xFF718396);
            }
        }

        private void drawTrimmed(FontRenderer font, String text, int x, int y, int width, int color) {
            font.drawStringWithShadow(font.trimStringToWidth(text, Math.max(8, width)), x, y, color);
        }
    }

    private final class ProductSummaryPanel extends PanelContainer {

        private final LabelPanel nameLabel;
        private final LabelPanel typeLabel;
        private final LabelPanel priceLabel;
        private final LabelPanel volumeLabel;

        private ProductSummaryPanel() {
            this.nameLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return TerminalMarketSectionContent.hasSelectedProduct(model)
                        ? model.getSelectedProductName()
                        : TerminalMarketSectionContent.buildProductCatalogEmptyTitle(model);
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            this.typeLabel = secondaryLabel(new Supplier<String>() {
                @Override
                public String get() {
                    String reason = TerminalMarketSectionContent.buildProductSelectionReason(model);
                    return reason.isEmpty() ? model.getSelectedProductUnit() : reason;
                }
            });
            this.priceLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return compactLatestTradeSummary();
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            this.volumeLabel = secondaryLabel(new Supplier<String>() {
                @Override
                public String get() {
                    return compactVolumeSummary();
                }
            });
            addChild(nameLabel);
            addChild(typeLabel);
            addChild(priceLabel);
            addChild(volumeLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int iconSize = Math.max(18, Math.min(30, bounds.getHeight() - 16));
            int iconSpace = iconSize + 12;
            int innerX = bounds.getX() + 8 + iconSpace;
            int innerWidth = Math.max(0, bounds.getWidth() - 16 - iconSpace);
            int metricWidth = Math.min(112, Math.max(58, innerWidth / 3));
            int textWidth = Math.max(0, innerWidth - metricWidth - 8);
            int firstLineY = bounds.getY() + Math.max(5, (bounds.getHeight() - 24) / 2);
            int secondLineHeight = bounds.getHeight() >= 38 ? 10 : 0;
            nameLabel.setBounds(new GuiRect(innerX, firstLineY, textWidth, 12));
            typeLabel.setBounds(new GuiRect(innerX, firstLineY + 15, textWidth, secondLineHeight));
            priceLabel.setBounds(new GuiRect(innerX + textWidth + 8, firstLineY, metricWidth, 12));
            volumeLabel.setBounds(new GuiRect(innerX + textWidth + 8, firstLineY + 15, metricWidth, secondLineHeight));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            drawCardFrame(bounds, 0xFF283544, 0xFF16202A);
            String seed = model.getSelectedProductName() + " " + model.getSelectedProductKey();
            int iconSize = Math.max(18, Math.min(30, bounds.getHeight() - 16));
            TerminalMarketVisuals.drawItemIconOrBadge(bounds.getX() + 10,
                bounds.getY() + Math.max(6, (bounds.getHeight() - iconSize) / 2),
                iconSize,
                model.getSelectedProductKey(), seed);
        }
    }

    private final class MarketChartPanel extends AbstractGuiPanel {

        private GuiRect oneHourTab = new GuiRect(0, 0, 0, 0);
        private GuiRect dayTab = new GuiRect(0, 0, 0, 0);
        private GuiRect weekTab = new GuiRect(0, 0, 0, 0);

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int x = bounds.getX() + 8;
            oneHourTab = new GuiRect(x, bounds.getY() + 4, 22, 14);
            dayTab = new GuiRect(x + 24, bounds.getY() + 4, 26, 14);
            weekTab = new GuiRect(x + 52, bounds.getY() + 4, 22, 14);
        }

        @Override
        public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
            if (mouseButton != 0) { return false; }
            String range = oneHourTab.contains(mouseX, mouseY) ? "1h"
                : dayTab.contains(mouseX, mouseY) ? "24h" : weekTab.contains(mouseX, mouseY) ? "7d" : "";
            if (range.isEmpty()) { return false; }
            state.setSelectedChartRange(range);
            if (actionHandler != null) { actionHandler.refreshProductBrowser(); }
            return true;
        }

        @Override
        public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            drawCardFrame(bounds, 0xFF283544, 0xFF121B25);
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            drawRangeTab(font, oneHourTab, "1h", "1h".equals(state.getSelectedChartRange()));
            drawRangeTab(font, dayTab, "24h", "24h".equals(state.getSelectedChartRange()));
            drawRangeTab(font, weekTab, "7d", "7d".equals(state.getSelectedChartRange()));
            int chartX = bounds.getX() + 8;
            int chartY = bounds.getY() + 22;
            int chartWidth = Math.max(0, bounds.getWidth() - 16);
            int chartHeight = Math.max(20, bounds.getHeight() - 58);
            Gui.drawRect(chartX, chartY, chartX + chartWidth, chartY + chartHeight, 0xFF0E1720);
            for (int line = 1; line < 4; line++) {
                int gridY = chartY + line * chartHeight / 4;
                Gui.drawRect(chartX, gridY, chartX + chartWidth, gridY + 1, 0x223B5268);
            }
            List<TerminalMarketSectionModel.PricePointModel> points = selectedPricePoints();
            if (points.size() < 2) {
                String empty = "暂无足够成交数据";
                font.drawStringWithShadow(empty, chartX + Math.max(4, (chartWidth - font.getStringWidth(empty)) / 2),
                    chartY + chartHeight / 2 - 4, 0xFF718396);
            } else {
                int volumeHeight = Math.max(7, chartHeight / 4);
                drawVolumeSeries(points, chartX + 3, chartY + chartHeight - volumeHeight - 2,
                    Math.max(1, chartWidth - 6), volumeHeight);
                drawPriceSeries(points, chartX + 3, chartY + 3, Math.max(1, chartWidth - 6),
                    Math.max(1, chartHeight - volumeHeight - 8));
            }
            int metricsY = bounds.getBottom() - 28;
            String rangeText = font.trimStringToWidth(buildPriceRange(points), Math.max(8, bounds.getWidth() - 16));
            String volumeText = font.trimStringToWidth("24h量 " + model.getVolume24h() + "  成交额 "
                + compactCurrency(model.getTurnover24h()), Math.max(8, bounds.getWidth() - 16));
            font.drawStringWithShadow(rangeText, bounds.getX() + 8, metricsY, 0xFFBFCBDA);
            font.drawStringWithShadow(volumeText, bounds.getX() + 8, metricsY + 12, 0xFFBFCBDA);
        }

        private void drawRangeTab(FontRenderer font, GuiRect tab, String label, boolean selected) {
            Gui.drawRect(tab.getX(), tab.getY(), tab.getRight(), tab.getBottom(),
                selected ? 0xFF285B88 : 0xFF1A2937);
            int x = tab.getX() + Math.max(2, (tab.getWidth() - font.getStringWidth(label)) / 2);
            font.drawStringWithShadow(label, x, tab.getY() + 3, selected ? 0xFFFFFFFF : 0xFF91A4B6);
        }

        private List<TerminalMarketSectionModel.PricePointModel> selectedPricePoints() {
            String key = model.getSelectedProductKey();
            for (TerminalMarketSectionModel.CatalogProductModel product : model.getCatalogProducts()) {
                if (product != null && product.getProductKey().equals(key)) {
                    return product.getMarketSummary().getPricePoints();
                }
            }
            return Collections.emptyList();
        }

        private void drawPriceSeries(List<TerminalMarketSectionModel.PricePointModel> points, int x, int y, int width,
            int height) {
            long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            for (TerminalMarketSectionModel.PricePointModel point : points) {
                min = Math.min(min, point.getPrice()); max = Math.max(max, point.getPrice());
            }
            long range = Math.max(1L, max - min);
            int lastX = x;
            int lastY = y + height - (int) ((points.get(0).getPrice() - min) * height / range);
            for (int index = 1; index < points.size(); index++) {
                int nextX = x + index * width / Math.max(1, points.size() - 1);
                int nextY = y + height - (int) ((points.get(index).getPrice() - min) * height / range);
                drawSeriesLine(lastX, lastY, nextX, nextY, 0xFF55B7ED);
                lastX = nextX; lastY = nextY;
            }
        }

        private void drawVolumeSeries(List<TerminalMarketSectionModel.PricePointModel> points, int x, int y, int width,
            int height) {
            long maximum = 1L;
            for (TerminalMarketSectionModel.PricePointModel point : points) {
                maximum = Math.max(maximum, point.getQuantity());
            }
            int barWidth = Math.max(1, width / Math.max(1, points.size()));
            for (int index = 0; index < points.size(); index++) {
                int barHeight = (int) Math.max(1L, points.get(index).getQuantity() * height / maximum);
                int barX = x + index * width / Math.max(1, points.size());
                Gui.drawRect(barX, y + height - barHeight, Math.min(x + width, barX + barWidth), y + height,
                    0x884C91A8);
            }
        }

        private String buildPriceRange(List<TerminalMarketSectionModel.PricePointModel> points) {
            if (points == null || points.isEmpty()) { return "高/低 -- / --"; }
            long high = Long.MIN_VALUE;
            long low = Long.MAX_VALUE;
            for (TerminalMarketSectionModel.PricePointModel point : points) {
                high = Math.max(high, point.getPrice());
                low = Math.min(low, point.getPrice());
            }
            return "高/低 " + high + " / " + low;
        }

        private void drawSeriesLine(int x0, int y0, int x1, int y1, int color) {
            int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
            for (int step = 0; step <= steps; step++) {
                int x = x0 + (x1 - x0) * step / Math.max(1, steps);
                int y = y0 + (y1 - y0) * step / Math.max(1, steps);
                Gui.drawRect(x, y, x + 1, y + 1, color);
            }
        }
    }

    private final class OrderBookPanel extends PanelContainer {

        private final LabelPanel bidHeader;
        private final LabelPanel tradeHeader;
        private final LabelPanel askHeader;
        private final List<LabelPanel> bidRows = new ArrayList<LabelPanel>();
        private final List<LabelPanel> tradeRows = new ArrayList<LabelPanel>();
        private final List<LabelPanel> askRows = new ArrayList<LabelPanel>();

        private OrderBookPanel() {
            bidHeader = sectionTitle("买单");
            tradeHeader = sectionTitle("最新成交");
            askHeader = sectionTitle("卖单");
            addChild(bidHeader);
            addChild(tradeHeader);
            addChild(askHeader);
            for (int i = 0; i < 4; i++) {
                final int row = i;
                LabelPanel bid = secondaryLabel(new Supplier<String>() {
                    @Override
                    public String get() {
                        List<String> lines = limitedBookLines(model.getBidLines(), 4);
                        return row < lines.size() ? lines.get(row) : "--";
                    }
                });
                LabelPanel trade = secondaryLabel(new Supplier<String>() {
                    @Override
                    public String get() {
                        if (row == 0) {
                            return "价 " + model.getLatestTradePrice();
                        }
                        if (row == 1) {
                            return "买一 " + model.getHighestBid();
                        }
                        if (row == 2) {
                            return "卖一 " + model.getLowestAsk();
                        }
                        return "24H " + model.getVolume24h();
                    }
                });
                LabelPanel ask = secondaryLabel(new Supplier<String>() {
                    @Override
                    public String get() {
                        List<String> lines = limitedBookLines(model.getAskLines(), 4);
                        return row < lines.size() ? lines.get(row) : "--";
                    }
                });
                bidRows.add(bid);
                tradeRows.add(trade);
                askRows.add(ask);
                addChild(bid);
                addChild(trade);
                addChild(ask);
            }
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int innerX = bounds.getX() + 8;
            int innerWidth = bounds.getWidth() - 16;
            int colGap = 6;
            int colWidth = Math.max(42, (innerWidth - colGap * 2) / 3);
            int lastWidth = innerWidth - colWidth * 2 - colGap * 2;
            int headerY = bounds.getY() + 6;
            int headerHeight = bounds.getHeight() >= 24 ? 10 : 0;
            bidHeader.setBounds(new GuiRect(innerX, headerY, colWidth, headerHeight));
            tradeHeader.setBounds(new GuiRect(innerX + colWidth + colGap, headerY, colWidth, headerHeight));
            askHeader.setBounds(new GuiRect(innerX + colWidth * 2 + colGap * 2, headerY, lastWidth, headerHeight));
            int rows = 4;
            int rowStartY = bounds.getY() + (headerHeight > 0 ? 20 : 6);
            int rowArea = Math.max(0, bounds.getBottom() - 6 - rowStartY);
            int rowStep = rows == 0 ? 0 : Math.max(8, rowArea / rows);
            int rowHeight = Math.max(0, Math.min(10, rowStep));
            for (int i = 0; i < 4; i++) {
                int rowY = rowStartY + i * rowStep;
                int visibleHeight = rowY + rowHeight <= bounds.getBottom() - 4 ? rowHeight : 0;
                bidRows.get(i).setBounds(new GuiRect(innerX, rowY, colWidth, visibleHeight));
                tradeRows.get(i).setBounds(new GuiRect(innerX + colWidth + colGap, rowY, colWidth, visibleHeight));
                askRows.get(i).setBounds(new GuiRect(innerX + colWidth * 2 + colGap * 2, rowY, lastWidth, visibleHeight));
            }
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            drawCardFrame(bounds, 0xFF283544, 0xFF16202A);
            int innerX = bounds.getX() + 8;
            int innerWidth = bounds.getWidth() - 16;
            int colGap = 6;
            int colWidth = Math.max(42, (innerWidth - colGap * 2) / 3);
            int splitA = innerX + colWidth + colGap / 2;
            int splitB = innerX + colWidth * 2 + colGap + colGap / 2;
            Gui.drawRect(splitA, bounds.getY() + 6, splitA + 1, bounds.getBottom() - 6, 0x223B4A5F);
            Gui.drawRect(splitB, bounds.getY() + 6, splitB + 1, bounds.getBottom() - 6, 0x223B4A5F);
        }
    }

    private final class StatusTileRowPanel extends PanelContainer {

        private final StatusTilePanel availableTile;
        private final StatusTilePanel claimableTile;
        private final StatusTilePanel orderTile;

        private StatusTileRowPanel() {
            availableTile = new StatusTilePanel("账户仓可卖", new Supplier<String>() {
                @Override
                public String get() {
                    return model.getSourceAvailable();
                }
            }, "Base Vault");
            claimableTile = new StatusTilePanel("待收货", new Supplier<String>() {
                @Override
                public String get() {
                    return model.getClaimableQuantity();
                }
            }, "仅异常交付显示");
            orderTile = new StatusTilePanel("当前委托", new Supplier<String>() {
                @Override
                public String get() {
                    return compactOrderCount();
                }
            }, "可撤订单 / 冻结资金");
            addChild(availableTile);
            addChild(claimableTile);
            addChild(orderTile);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int gap = 6;
            int tileWidth = Math.max(40, (bounds.getWidth() - gap * 2) / 3);
            int lastWidth = bounds.getWidth() - tileWidth * 2 - gap * 2;
            availableTile.setBounds(new GuiRect(bounds.getX(), bounds.getY(), tileWidth, bounds.getHeight()));
            claimableTile.setBounds(new GuiRect(bounds.getX() + tileWidth + gap, bounds.getY(), tileWidth, bounds.getHeight()));
            orderTile.setBounds(new GuiRect(bounds.getX() + tileWidth * 2 + gap * 2, bounds.getY(), lastWidth, bounds.getHeight()));
        }
    }

    private final class StatusTilePanel extends PanelContainer {

        private final LabelPanel titleLabel;
        private final LabelPanel valueLabel;
        private final LabelPanel subtitleLabel;

        private StatusTilePanel(final String title, final Supplier<String> valueSupplier, final String subtitle) {
            titleLabel = secondaryLabel(new Supplier<String>() {
                @Override
                public String get() {
                    return title;
                }
            });
            valueLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return valueSupplier == null ? "" : valueSupplier.get();
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            subtitleLabel = secondaryLabel(new Supplier<String>() {
                @Override
                public String get() {
                    return subtitle == null ? "" : subtitle;
                }
            });
            addChild(titleLabel);
            addChild(valueLabel);
            addChild(subtitleLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int innerX = bounds.getX() + 18;
            int innerWidth = bounds.getWidth() - 24;
            titleLabel.setBounds(new GuiRect(innerX, bounds.getY() + 4, innerWidth, 10));
            valueLabel.setBounds(new GuiRect(innerX, bounds.getY() + 15, innerWidth, 10));
            subtitleLabel.setVisible(false);
            subtitleLabel.setBounds(new GuiRect(0, 0, 0, 0));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            drawCardFrame(bounds, 0xFF283544, 0xFF16202A);
            TerminalMarketVisuals.drawStatusDot(bounds.getX() + 6, bounds.getY() + bounds.getHeight() / 2 - 3, true);
        }
    }

    private final class DetailActionItemPanel extends PanelContainer {

        private final LabelPanel titleLabel;
        private final LabelPanel bodyLabel;
        private final ButtonPanel actionButton;

        private DetailActionItemPanel(final String title, final String body, String actionText, boolean enabled,
            Runnable action) {
            titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return title;
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            bodyLabel = secondaryLabel(new Supplier<String>() {
                @Override
                public String get() {
                    return body;
                }
            });
            actionButton = button(actionText, action, new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.valueOf(enabled);
                }
            });
            addChild(titleLabel);
            addChild(bodyLabel);
            addChild(actionButton);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int innerX = bounds.getX() + 8;
            int innerWidth = bounds.getWidth() - 16;
            int buttonWidth = Math.min(70, Math.max(60, innerWidth / 3));
            titleLabel.setBounds(new GuiRect(innerX, bounds.getY() + 6, innerWidth - buttonWidth - 8, 10));
            bodyLabel.setBounds(new GuiRect(innerX, bounds.getY() + 18, innerWidth - buttonWidth - 8, bounds.getHeight() - 24));
            actionButton.setBounds(new GuiRect(bounds.getRight() - buttonWidth - 8,
                bounds.getY() + Math.max(6, (bounds.getHeight() - 18) / 2), buttonWidth, 18));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            drawCardFrame(getBounds(), 0xFF283544, 0xFF16202A);
        }
    }

    private final class SummaryLinePanel extends PanelContainer {

        private final LabelPanel textLabel;

        private SummaryLinePanel(final String text) {
            textLabel = secondaryLabel(new Supplier<String>() {
                @Override
                public String get() {
                    return text;
                }
            });
            addChild(textLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            textLabel.setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + 6, bounds.getWidth() - 16, bounds.getHeight() - 10));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            drawCardFrame(getBounds(), 0xFF283544, 0xFF16202A);
        }
    }

    private final class EmptyStatePanel extends PanelContainer {

        private final LabelPanel titleLabel;
        private final LabelPanel bodyLabel;

        private EmptyStatePanel(final String title, final String body) {
            titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return title;
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            bodyLabel = secondaryLabel(new Supplier<String>() {
                @Override
                public String get() {
                    return body;
                }
            });
            addChild(titleLabel);
            addChild(bodyLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            titleLabel.setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + 8, bounds.getWidth() - 16, 12));
            bodyLabel.setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + 24, bounds.getWidth() - 16,
                bounds.getHeight() - 30));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            drawCardFrame(getBounds(), 0xFF283544, 0xFF16202A);
        }
    }

    private final class FlatActionRowPanel extends PanelContainer {

        private final LabelPanel titleLabel;
        private final LabelPanel hintLabel;
        private final ButtonPanel actionButton;
        private final TerminalTextFieldPanel[] fields;

        private FlatActionRowPanel(final String title, Supplier<String> hintSupplier, ButtonPanel actionButton,
            TerminalTextFieldPanel... fields) {
            this.titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return title;
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            this.hintLabel = secondaryLabel(hintSupplier);
            this.actionButton = actionButton;
            this.fields = fields == null ? new TerminalTextFieldPanel[0] : fields;
            addChild(titleLabel);
            addChild(hintLabel);
            for (TerminalTextFieldPanel field : this.fields) {
                addChild(field);
            }
            addChild(actionButton);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int innerX = bounds.getX() + 7;
            int innerWidth = bounds.getWidth() - 14;
            int buttonWidth = Math.min(70, Math.max(52, innerWidth / 4));
            int fieldWidth = fields.length == 0 ? 0
                : Math.max(34, (innerWidth - buttonWidth - 8 - (fields.length - 1) * 4) / fields.length);
            titleLabel.setBounds(new GuiRect(innerX + 12, bounds.getY() + 4, Math.max(32, innerWidth - buttonWidth - 18), 10));
            hintLabel.setBounds(new GuiRect(innerX + 12, bounds.getY() + 15,
                Math.max(0, innerWidth - buttonWidth - 18), bounds.getHeight() >= 28 ? 8 : 0));
            int fieldX = bounds.getRight() - buttonWidth - 8 - fields.length * fieldWidth - Math.max(0, fields.length - 1) * 4;
            for (TerminalTextFieldPanel field : fields) {
                field.setBounds(new GuiRect(fieldX, bounds.getY() + 5, fieldWidth, Math.max(14, bounds.getHeight() - 10)));
                fieldX += fieldWidth + 4;
            }
            actionButton.setBounds(new GuiRect(bounds.getRight() - buttonWidth - 7,
                bounds.getY() + 5, buttonWidth, Math.max(14, bounds.getHeight() - 10)));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            drawCardFrame(bounds, 0xFF283544, 0xFF16202A);
            TerminalMarketVisuals.drawStatusDot(bounds.getX() + 7, bounds.getY() + Math.max(8, bounds.getHeight() / 2 - 4), true);
        }
    }

    private final class ActionBlockPanel extends PanelContainer {

        private final LabelPanel titleLabel;
        private final LabelPanel hintLabel;
        private final ButtonPanel actionButton;
        private final TerminalTextFieldPanel[] fields;

        private ActionBlockPanel(final String title, Supplier<String> hintSupplier, ButtonPanel actionButton,
            TerminalTextFieldPanel... fields) {
            this.titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return title;
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            this.hintLabel = secondaryLabel(hintSupplier);
            this.actionButton = actionButton;
            this.fields = fields == null ? new TerminalTextFieldPanel[0] : fields;
            addChild(titleLabel);
            addChild(hintLabel);
            for (TerminalTextFieldPanel field : this.fields) {
                addChild(field);
            }
            addChild(actionButton);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int innerX = bounds.getX() + 8;
            int innerWidth = bounds.getWidth() - 16;
            titleLabel.setBounds(new GuiRect(innerX + 12, bounds.getY() + 6, innerWidth - 12, 10));
            hintLabel.setBounds(new GuiRect(innerX, bounds.getY() + 20, innerWidth, 16));
            int fieldY = bounds.getY() + 40;
            for (TerminalTextFieldPanel field : fields) {
                field.setBounds(new GuiRect(innerX, fieldY, innerWidth, 18));
                fieldY += 22;
            }
            actionButton.setBounds(new GuiRect(innerX, bounds.getBottom() - 22, innerWidth, 18));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            drawCardFrame(bounds, 0xFF283544, 0xFF16202A);
            TerminalMarketVisuals.drawStatusDot(bounds.getX() + 8, bounds.getY() + 7, true);
        }
    }

    private final class FeedbackSummaryPanel extends PanelContainer {

        private final LabelPanel titleLabel;
        private final LabelPanel bodyLabel;

        private FeedbackSummaryPanel() {
            titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return TerminalMarketSectionContent.latestFeedbackLine(model);
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            bodyLabel = secondaryLabel(new Supplier<String>() {
                @Override
                public String get() {
                    return model.getActionFeedback().getBody();
                }
            });
            addChild(titleLabel);
            addChild(bodyLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            titleLabel.setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + 6, bounds.getWidth() - 16, 10));
            bodyLabel.setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + 18, bounds.getWidth() - 16, bounds.getHeight() - 22));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            TerminalNotificationSeverity severity = model.getActionFeedback().getSeverity();
            RoundedRectPainter.draw(bounds, 0xFF283544, severity.getBackgroundColor());
            Gui.drawRect(bounds.getX() + 2, bounds.getY() + 2, bounds.getX() + 5, bounds.getBottom() - 2,
                severity.getAccentColor());
        }
    }

    private int actionItemHeight(String detail, int width) {
        return Math.max(40, TerminalLayoutMetrics.rowHeight(detail, width) + 22);
    }

    private static long parsePositiveLong(String value) {
        if (value == null) { return 0L; }
        StringBuilder digits = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character >= '0' && character <= '9') {
                digits.append(character);
            } else if (character == ',' && digits.length() > 0) {
                continue;
            } else if (digits.length() > 0) {
                break;
            }
        }
        if (digits.length() == 0) { return 0L; }
        try {
            return Math.max(0L, Long.parseLong(digits.toString()));
        } catch (NumberFormatException ignored) {
            return Long.MAX_VALUE;
        }
    }

    private static long parseMarketPrice(String value) {
        return parsePositiveLong(value);
    }

    private static long saturatedMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) { return 0L; }
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private final class ProductBrowserItemPanel extends PanelContainer {

        private final TerminalMarketSectionContent.ProductEntry entry;
        private final LabelPanel titleLabel;
        private final LabelPanel subtitleLabel;
        private final LabelPanel statusLabel;
        private boolean pressed;

        private ProductBrowserItemPanel(final TerminalMarketSectionContent.ProductEntry entry) {
            this.entry = entry;
            this.titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return entry.getTitle();
                }
            }, ThemeColorKey.TEXT_PRIMARY, false);
            this.subtitleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return entry.getSubtitle();
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            this.statusLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return entry.getStateLabel();
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
            addChild(titleLabel);
            addChild(subtitleLabel);
            addChild(statusLabel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            GuiRect itemBounds = getBounds();
            int textWidth = Math.max(44, itemBounds.getWidth() - 104);
            titleLabel.setBounds(new GuiRect(itemBounds.getX() + 40, itemBounds.getY() + 8, textWidth, 10));
            subtitleLabel.setBounds(new GuiRect(itemBounds.getX() + 40, itemBounds.getY() + 22, textWidth, 10));
            statusLabel.setBounds(new GuiRect(itemBounds.getRight() - 54, itemBounds.getY() + 18, 46, 10));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            boolean selected = entry.isSelected() || entry.getKey().equals(state.getSelectedProductKey());
            boolean hovered = contains(mouseX, mouseY) && !entry.getKey().isEmpty() && entry.isEnabled();
            int borderColor = selected ? 0xFF529BED : 0xFF1A1E26;
            int fillColor = selected ? 0xFF18283B : hovered ? 0xFF152232 : 0xFF10161D;
            RoundedRectPainter.draw(bounds, borderColor, fillColor);
            if (selected) {
                Gui.drawRect(bounds.getX() + 2, bounds.getY() + 2, bounds.getX() + 5, bounds.getBottom() - 2, 0xFF68A7F0);
            }
            int iconX = bounds.getX() + 8;
            int iconY = bounds.getY() + 10;
            TerminalMarketVisuals.drawItemIconOrBadge(iconX, iconY, 24,
                entry.getIconRef(), entry.getTitle() + " " + entry.getKey());
            int dotColor = !entry.isEnabled() || entry.getKey().isEmpty() ? 0xFF5E6670 : 0xFF6AD46F;
            Gui.drawRect(bounds.getRight() - 16, bounds.getY() + bounds.getHeight() / 2 - 3,
                bounds.getRight() - 10, bounds.getY() + bounds.getHeight() / 2 + 3, dotColor);
        }

        @Override
        public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
            if (entry.getKey().isEmpty() || !entry.isEnabled()) {
                return false;
            }
            pressed = mouseButton == 0 && contains(mouseX, mouseY);
            return pressed;
        }

        @Override
        public boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
            boolean shouldClick = pressed && mouseButton == 0 && contains(mouseX, mouseY) && !entry.getKey().isEmpty() && entry.isEnabled();
            pressed = false;
            if (!shouldClick) {
                return false;
            }
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null) {
                minecraft.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(CLICK_SOUND, 1.0F));
            }
            state.setSelectedProductKey(entry.getKey());
            if (actionHandler != null) {
                actionHandler.selectProduct(entry.getKey());
            }
            return true;
        }
    }
}
