package com.jsirgalaxybase.terminal.client.component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

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

        default void openStandardizedHistory() {}

        default void refreshStandardizedHistory() {}

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
    private HistoryWorkbenchPanel historyPanel;

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
        historyPanel = new HistoryWorkbenchPanel();
        addChild(browserPanel);
        addChild(detailPanel);
        addChild(historyPanel);
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
        if (state.isStandardizedHistoryView()) {
            browserPanel.setVisible(false);
            detailPanel.setVisible(false);
            historyPanel.setVisible(true);
            historyPanel.setBounds(bounds);
            return;
        }
        if (state.isStandardizedDetailView()) {
            browserPanel.setVisible(false);
            detailPanel.setVisible(true);
            historyPanel.setVisible(false);
            detailPanel.setBounds(bounds);
            return;
        }
        browserPanel.setVisible(true);
        detailPanel.setVisible(false);
        historyPanel.setVisible(false);
        browserPanel.setBounds(bounds);
        detailPanel.setBounds(new GuiRect(bounds.getX(), bounds.getY(), 0, 0));
        historyPanel.setBounds(new GuiRect(bounds.getX(), bounds.getY(), 0, 0));
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

    private List<TerminalMarketSectionModel.PricePointModel> selectedPricePointsForSelectedProduct() {
        String key = model.getSelectedProductKey();
        for (TerminalMarketSectionModel.CatalogProductModel product : model.getCatalogProducts()) {
            if (product != null && product.getProductKey().equals(key)) {
                return product.getMarketSummary().getPricePoints();
            }
        }
        return Collections.emptyList();
    }

    static List<TerminalMarketSectionModel.PricePointModel> latestRealTrades(
        List<TerminalMarketSectionModel.PricePointModel> points, int limit) {
        List<TerminalMarketSectionModel.PricePointModel> trades =
            new ArrayList<TerminalMarketSectionModel.PricePointModel>();
        if (points == null || limit <= 0) { return trades; }
        for (int i = points.size() - 1; i >= 0 && trades.size() < limit; i--) {
            TerminalMarketSectionModel.PricePointModel point = points.get(i);
            if (point != null && point.isTrade() && !point.isEmpty()
                && point.getPrice() > 0L && point.getQuantity() > 0L) {
                trades.add(point);
            }
        }
        return trades;
    }

    private String selectedProductIconRef() {
        String key = model.getSelectedProductKey();
        for (TerminalMarketSectionModel.CatalogProductModel product : model.getCatalogProducts()) {
            if (product != null && product.getProductKey().equals(key)) {
                return product.getRegistryName() + ":" + product.getMeta();
            }
        }
        return key;
    }

    private String selectedLocalizedProductName() {
        return TerminalMarketVisuals.resolveLocalizedItemName(selectedProductIconRef(),
            model.getSelectedProductName());
    }

    private String localizedOrderDisplayName(TerminalMarketSectionContent.OrderEntry entry) {
        if (entry == null) { return "--"; }
        String productKey = entry.getProductKey();
        for (TerminalMarketSectionModel.CatalogProductModel product : model.getCatalogProducts()) {
            if (product != null && product.getProductKey().equals(productKey)) {
                return TerminalMarketVisuals.resolveLocalizedItemName(
                    product.getRegistryName() + ":" + product.getMeta(), entry.getDisplayName());
            }
        }
        return TerminalMarketVisuals.resolveLocalizedItemName(productKey, entry.getDisplayName());
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

    private long[] parseBookLevel(String line) {
        if (line == null) { return null; }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("([0-9,]+)\\s*[xX]\\s*([0-9,]+)")
            .matcher(compactBookLine(line));
        if (!matcher.find()) { return null; }
        try {
            long price = Long.parseLong(matcher.group(1).replace(",", ""));
            long quantity = Long.parseLong(matcher.group(2).replace(",", ""));
            return price > 0L && quantity > 0L ? new long[] { price, quantity } : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
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
            String iconRef = product.getRegistryName() + ":" + product.getMeta();
            String localizedName = TerminalMarketVisuals.resolveLocalizedItemName(iconRef, product.getDisplayName());
            MarketBrowseItemModel item = new MarketBrowseItemModel(product.getProductKey(), iconRef,
                localizedName, String.valueOf(product.getReferencePrice()), product.getTradability(),
                summary.getLatestTrade(), summary.getBestBid(), summary.getBestAsk(), summary.getVolume24h(),
                summary.getAvailable(), summary.getEscrow(), summary.getClaimable(), summary.getDayChange(),
                summary.getPricePoints());
            if (state.getBrowserFilter() == TerminalMarketSectionState.BrowserFilter.TRADED
                && item.getPricePoints().isEmpty()) { continue; }
            if (state.getBrowserFilter() == TerminalMarketSectionState.BrowserFilter.BOOK
                && "无盘口".equals(item.getLiquidityLabel())) { continue; }
            items.add(item);
        }
        return items;
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
        private final ActionWorkbenchPanel actionPanel;

        private DetailWorkbenchPanel() {
            this.summaryPanel = new ProductSummaryPanel();
            this.chartPanel = new MarketChartPanel();
            this.orderBookPanel = new OrderBookPanel();
            this.actionPanel = new ActionWorkbenchPanel();
            addChild(summaryPanel);
            addChild(chartPanel);
            addChild(orderBookPanel);
            addChild(actionPanel);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            MarketDetailLayout layout = MarketDetailLayout.withinStandardSplit(bounds);
            summaryPanel.setBounds(layout.hero);
            chartPanel.setBounds(layout.chart);
            orderBookPanel.setBounds(layout.orderBook);
            actionPanel.setBounds(layout.ticket);
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            drawCardFrame(getBounds(), 0xFF324152, 0xFF19222C);
        }
    }

    private final class ActionWorkbenchPanel extends PanelContainer {

        private final MarketActionButtonPanel buyButton;
        private final MarketActionButtonPanel sellButton;
        private final MarketActionButtonPanel historyButton;

        private ActionWorkbenchPanel() {
            buyButton = new MarketActionButtonPanel("买入", () -> {
                if (actionHandler != null) {
                    actionHandler.openOrderConfirm(TerminalMarketSectionState.OrderSide.BUY,
                        TerminalMarketSectionState.OrderType.MARKET);
                }
            }, () -> Boolean.valueOf(TerminalMarketSectionContent.hasSelectedProduct(model)),
                0xEE236A3A, 0xEE31864A);
            sellButton = new MarketActionButtonPanel("卖出", () -> {
                if (actionHandler != null) {
                    actionHandler.openOrderConfirm(TerminalMarketSectionState.OrderSide.SELL,
                        TerminalMarketSectionState.OrderType.MARKET);
                }
            }, () -> Boolean.valueOf(TerminalMarketSectionContent.hasSelectedProduct(model)),
                0xEE8B3437, 0xEEAA4447);
            this.historyButton = new MarketActionButtonPanel("历史", new Runnable() {
                @Override
                public void run() {
                    if (actionHandler != null) actionHandler.openStandardizedHistory();
                }
            }, () -> Boolean.TRUE, 0xEE355A79, 0xEE456F91);
            addChild(buyButton);
            addChild(sellButton);
            addChild(historyButton);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int innerX = bounds.getX() + CARD_PADDING;
            int innerWidth = Math.max(0, bounds.getWidth() - CARD_PADDING * 2);
            int gap = 4;
            int buttonHeight = Math.min(18, Math.max(14, bounds.getHeight() / 5));
            int buttonWidth = Math.max(30, (innerWidth - gap * 2) / 3);
            int actionY = bounds.getBottom() - buttonHeight - 6;
            buyButton.setBounds(new GuiRect(innerX, actionY, buttonWidth, buttonHeight));
            sellButton.setBounds(new GuiRect(innerX + buttonWidth + gap, actionY, buttonWidth, buttonHeight));
            historyButton.setBounds(new GuiRect(innerX + (buttonWidth + gap) * 2, actionY,
                Math.max(0, innerWidth - buttonWidth * 2 - gap * 2), buttonHeight));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            drawCardFrame(getBounds(), 0xFF324152, 0xFF19222C);
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            if (font != null) {
                int x = getBounds().getX() + CARD_PADDING;
                int y = getBounds().getY() + 5;
                int innerWidth = Math.max(0, getBounds().getWidth() - CARD_PADDING * 2);
                int columnGap = 10;
                int columnWidth = Math.max(24, (innerWidth - columnGap) / 2);
                int rightX = x + columnWidth + columnGap;
                List<TerminalMarketSectionModel.PricePointModel> points = selectedPricePointsForSelectedProduct();
                drawCompact(font, "24h行情", x, y, columnWidth, 0xFFD8E3EE);
                drawCompact(font, "账户状态", rightX, y, columnWidth, 0xFFD8E3EE);
                drawCompact(font, compactPriceRange(points), x, y + 10, columnWidth, 0xFFBFCBDA);
                drawCompact(font, "可用库存 " + compactCurrency(model.getSourceAvailable()), rightX, y + 10,
                    columnWidth, 0xFF62D478);
                drawCompact(font, "成交量 " + model.getVolume24h(), x, y + 20,
                    columnWidth, 0xFFA9B8C8);
                drawCompact(font, "冻结资金 " + compactCurrency(model.getFrozenFunds()), rightX, y + 20,
                    columnWidth, 0xFFA9B8C8);
                drawCompact(font, "成交额 " + compactCurrency(model.getTurnover24h()), x, y + 30,
                    columnWidth, 0xFFA9B8C8);
                drawCompact(font, "当前委托 " + compactOrderCount() + " / 可撤 " + countCancelableOrders(), rightX,
                    y + 30, columnWidth, 0xFFA9B8C8);
                TerminalMarketSectionModel.ActionFeedbackModel feedback = model.getActionFeedback();
                if (hasMeaningfulActionFeedback(feedback)) {
                    drawCompact(font, feedback.getTitle() + ": " + feedback.getBody(), x, y + 40,
                        innerWidth, feedbackColor(feedback.getSeverity()));
                } else {
                    drawCompact(font, "待入库 " + compactCurrency(model.getClaimableQuantity()), rightX, y + 40,
                        columnWidth, 0xFFA9B8C8);
                }
                int splitX = x + columnWidth + columnGap / 2;
                Gui.drawRect(splitX, y, splitX + 1, Math.min(getBounds().getBottom() - 28, y + 50), 0x334C6277);
                Gui.drawRect(x, y + 8, x + columnWidth, y + 9, 0x223B5268);
                Gui.drawRect(rightX, y + 8, rightX + columnWidth, y + 9, 0x223B5268);
            }
        }

        private String compactPriceRange(List<TerminalMarketSectionModel.PricePointModel> points) {
            if (points == null || points.isEmpty()) {
                return "高/低 -- / --";
            }
            long high = Long.MIN_VALUE;
            long low = Long.MAX_VALUE;
            for (TerminalMarketSectionModel.PricePointModel point : points) {
                high = Math.max(high, point.getPrice());
                low = Math.min(low, point.getPrice());
            }
            return "高/低 " + high + " / " + low;
        }

        private void drawCompact(FontRenderer font, String text, int x, int y, int width, int color) {
            String trimmed = MarketCompactText.trim(font, text, Math.max(8, width), MarketCompactText.CONTENT_SCALE);
            MarketCompactText.draw(font, trimmed, x, y, color, MarketCompactText.CONTENT_SCALE);
        }

        private boolean hasMeaningfulActionFeedback(TerminalMarketSectionModel.ActionFeedbackModel feedback) {
            return feedback != null
                && feedback.getBody() != null
                && !feedback.getBody().isEmpty()
                && !"当前没有市场动作反馈。".equals(feedback.getBody());
        }

        private int feedbackColor(TerminalNotificationSeverity severity) {
            if (severity == TerminalNotificationSeverity.ERROR) { return 0xFFE56A64; }
            if (severity == TerminalNotificationSeverity.WARNING) { return 0xFFF0C75E; }
            if (severity == TerminalNotificationSeverity.SUCCESS) { return 0xFF62D478; }
            return 0xFFA9B8C8;
        }
    }

    private final class HistoryWorkbenchPanel extends PanelContainer {

        private final List<TerminalMarketSectionContent.OrderEntry> entries;
        private final TerminalTextFieldPanel queryField;
        private final ButtonPanel searchButton;
        private final ButtonPanel resetButton;
        private final ButtonPanel scopeButton;
        private final ButtonPanel sideButton;
        private final ButtonPanel statusButton;
        private final ButtonPanel timeButton;
        private final ButtonPanel previousButton;
        private final ButtonPanel nextButton;
        private final List<MarketActionButtonPanel> cancelButtons = new ArrayList<MarketActionButtonPanel>();

        private HistoryWorkbenchPanel() {
            entries = TerminalMarketSectionContent.buildOrderEntries(model);
            queryField = textField(() -> state.getHistoryQuery(), value -> state.setHistoryQuery(value),
                TerminalMarketSectionState.FocusField.HISTORY_QUERY, "搜索商品名称或物品键...");
            searchButton = dynamicButton(() -> "搜索", () -> {
                state.focus(TerminalMarketSectionState.FocusField.NONE);
                refreshHistory();
            });
            resetButton = dynamicButton(() -> "清空", () -> {
                state.resetHistoryFilters();
                refreshHistory();
            });
            scopeButton = dynamicButton(() -> state.getHistoryProductScopeLabel(), () -> {
                state.toggleHistoryProductScope();
                refreshHistory();
            });
            sideButton = dynamicButton(() -> state.getHistorySideLabel(), () -> {
                state.cycleHistorySide();
                refreshHistory();
            });
            statusButton = dynamicButton(() -> state.getHistoryStatusLabel(), () -> {
                state.cycleHistoryStatus();
                refreshHistory();
            });
            timeButton = dynamicButton(() -> state.getHistoryTimeLabel(), () -> {
                state.cycleHistoryTime();
                refreshHistory();
            });
            previousButton = dynamicButton(() -> "<", () -> {
                if (model.hasHistoryPreviousPage()) {
                    state.setHistoryPage(model.getHistoryPageIndex() - 1);
                    refreshHistory();
                }
            });
            nextButton = dynamicButton(() -> ">", () -> {
                if (model.hasHistoryNextPage()) {
                    state.setHistoryPage(model.getHistoryPageIndex() + 1);
                    refreshHistory();
                }
            });
            addChild(queryField);
            addChild(searchButton);
            addChild(resetButton);
            addChild(scopeButton);
            addChild(sideButton);
            addChild(statusButton);
            addChild(timeButton);
            addChild(previousButton);
            addChild(nextButton);
            for (final TerminalMarketSectionContent.OrderEntry entry : entries) {
                MarketActionButtonPanel cancel = new MarketActionButtonPanel("撤单", () -> {
                    if (actionHandler != null && entry.isCancelable()) {
                        state.setPendingCancelOrderId(entry.getOrderId());
                        actionHandler.openCancelOrderConfirm(entry.getOrderId());
                    }
                }, () -> Boolean.valueOf(entry.isCancelable()), 0xEE8A641F, 0xEEAA7B27);
                cancelButtons.add(cancel);
                addChild(cancel);
            }
        }

        private ButtonPanel dynamicButton(Supplier<String> text, Runnable action) {
            return panels.createButton(new GuiRect(0, 0, 0, 0), text, action, () -> Boolean.TRUE);
        }

        @Override
        public void setBounds(GuiRect bounds) {
            super.setBounds(bounds);
            int x = bounds.getX() + 8;
            int y = bounds.getY() + 8;
            int right = bounds.getRight() - 8;
            int actionWidth = 46;
            queryField.setBounds(new GuiRect(x, y, Math.max(80, right - x - actionWidth * 2 - 8), 18));
            searchButton.setBounds(new GuiRect(right - actionWidth * 2 - 4, y, actionWidth, 18));
            resetButton.setBounds(new GuiRect(right - actionWidth, y, actionWidth, 18));
            y += 23;
            int filterWidth = Math.max(50, (bounds.getWidth() - 8 * 2 - 6 * 3) / 4);
            scopeButton.setBounds(new GuiRect(x, y, filterWidth, 18));
            sideButton.setBounds(new GuiRect(x + filterWidth + 6, y, filterWidth, 18));
            statusButton.setBounds(new GuiRect(x + (filterWidth + 6) * 2, y, filterWidth, 18));
            timeButton.setBounds(new GuiRect(x + (filterWidth + 6) * 3, y,
                Math.max(0, bounds.getRight() - 8 - x - (filterWidth + 6) * 3), 18));
            previousButton.setBounds(new GuiRect(bounds.getRight() - 58, bounds.getBottom() - 22, 24, 16));
            nextButton.setBounds(new GuiRect(bounds.getRight() - 30, bounds.getBottom() - 22, 24, 16));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            drawCardFrame(getBounds(), 0xFF324152, 0xFF19222C);
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            if (font == null) return;
            int pageCount = model.getHistoryTotalPages();
            int page = model.getHistoryPageIndex();
            if (page != state.getHistoryPage()) state.setHistoryPage(page);
            int rowX = getBounds().getX() + 8;
            int rowWidth = Math.max(0, getBounds().getWidth() - 16);
            int headerY = getBounds().getY() + 56;
            int rowY = headerY + 13;
            int footerY = getBounds().getBottom() - 25;
            int pageSize = Math.max(1, model.getHistoryPageSize());
            int rowHeight = Math.max(22, Math.min(29, Math.max(1, footerY - rowY) / pageSize));
            int productX = rowX + 26;
            int priceX = rowX + rowWidth * 37 / 100;
            int filledX = rowX + rowWidth * 47 / 100;
            int remainingX = rowX + rowWidth * 58 / 100;
            int statusX = rowX + rowWidth * 68 / 100;
            int timeX = rowX + rowWidth * 80 / 100;
            font.drawString("方向", rowX + 3, headerY, 0xFF71879B);
            font.drawString("商品", productX, headerY, 0xFF71879B);
            font.drawString("价格", priceX, headerY, 0xFF71879B);
            font.drawString("成交/总量", filledX, headerY, 0xFF71879B);
            font.drawString("剩余", remainingX, headerY, 0xFF71879B);
            font.drawString("状态", statusX, headerY, 0xFF71879B);
            font.drawString("创建时间", timeX, headerY, 0xFF71879B);
            for (MarketActionButtonPanel button : cancelButtons) button.setVisible(false);
            for (int index = 0; index < entries.size(); index++) {
                TerminalMarketSectionContent.OrderEntry entry = entries.get(index);
                int y = rowY + index * rowHeight;
                if (y + rowHeight > footerY) break;
                Gui.drawRect(rowX, y, rowX + rowWidth, y + rowHeight - 2,
                    index % 2 == 0 ? 0xFF111922 : 0xFF151F29);
                String side = entry.getSideLabel();
                int sideColor = "BUY".equals(entry.getSide()) ? 0xFF65D879 : 0xFFE16767;
                int textY = y + Math.max(4, (rowHeight - font.FONT_HEIGHT) / 2);
                font.drawString(side, rowX + 6, textY, sideColor);
                font.drawString(font.trimStringToWidth(localizedOrderDisplayName(entry),
                    Math.max(40, priceX - productX - 8)),
                    productX, textY, 0xFFE5EDF5);
                font.drawString(font.trimStringToWidth(entry.getUnitPrice(), Math.max(24, filledX - priceX - 6)),
                    priceX, textY, 0xFFF0C75E);
                font.drawString(font.trimStringToWidth(entry.getFilledQuantity() + "/" + entry.getOriginalQuantity(),
                    Math.max(30, remainingX - filledX - 6)), filledX, textY, 0xFFB8C6D4);
                font.drawString(font.trimStringToWidth(entry.getRemainingQuantity(), Math.max(24, statusX - remainingX - 6)),
                    remainingX, textY, 0xFFB8C6D4);
                font.drawString(font.trimStringToWidth(entry.getStatusLabel(), Math.max(36, timeX - statusX - 6)),
                    statusX, textY, entry.isCancelable() ? 0xFFF0C75E : 0xFF9FB0BF);
                int timeWidth = entry.isCancelable() ? rowX + rowWidth - 58 - timeX : rowX + rowWidth - timeX - 6;
                font.drawString(font.trimStringToWidth(entry.getCreatedAt(), Math.max(36, timeWidth)),
                    timeX, textY, 0xFF71879B);
                if (entry.isCancelable()) {
                    MarketActionButtonPanel cancel = cancelButtons.get(index);
                    cancel.setVisible(true);
                    cancel.setBounds(new GuiRect(rowX + rowWidth - 52, y + 3, 46, Math.max(14, rowHeight - 8)));
                }
            }
            if (entries.isEmpty()) {
                String empty = model.getHistoryTotalEntries() == 0
                    ? "当前筛选条件下没有订单。" : "当前页没有订单，请返回上一页。";
                font.drawString(empty, rowX + 8, rowY + 10, 0xFF9FB0BF);
            }
            font.drawString("订单历史  " + model.getHistoryTotalEntries() + " 条  " + (page + 1) + "/" + pageCount,
                rowX, getBounds().getBottom() - 19, 0xFFC7D3DE);
        }

        private void refreshHistory() {
            if (actionHandler != null) actionHandler.refreshStandardizedHistory();
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
                        ? selectedLocalizedProductName()
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
            int iconSize = Math.max(14, Math.min(20, bounds.getHeight() - 8));
            int iconSpace = iconSize + 8;
            int innerX = bounds.getX() + 6 + iconSpace;
            int innerWidth = Math.max(0, bounds.getWidth() - 12 - iconSpace);
            int metricWidth = Math.min(74, Math.max(42, innerWidth / 3));
            int textWidth = Math.max(0, innerWidth - metricWidth - 8);
            int firstLineY = bounds.getY() + Math.max(3, (bounds.getHeight() - 10) / 2);
            int secondLineHeight = 0;
            nameLabel.setBounds(new GuiRect(innerX, firstLineY, textWidth, 12));
            typeLabel.setBounds(new GuiRect(innerX, firstLineY + 15, textWidth, secondLineHeight));
            priceLabel.setBounds(new GuiRect(innerX + textWidth + 8, firstLineY, metricWidth, 12));
            volumeLabel.setBounds(new GuiRect(innerX + textWidth + 8, firstLineY + 15, metricWidth, secondLineHeight));
        }

        @Override
        protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds();
            drawCardFrame(bounds, 0xFF283544, 0xFF16202A);
            String seed = selectedLocalizedProductName() + " " + model.getSelectedProductKey();
            int iconSize = Math.max(14, Math.min(20, bounds.getHeight() - 8));
            TerminalMarketVisuals.drawItemIconOrBadge(bounds.getX() + 6,
                bounds.getY() + Math.max(4, (bounds.getHeight() - iconSize) / 2),
                iconSize,
                selectedProductIconRef(), seed);
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
            int chartHeight = Math.max(20, bounds.getHeight() - 30);
            Gui.drawRect(chartX, chartY, chartX + chartWidth, chartY + chartHeight, 0xFF0E1720);
            List<TerminalMarketSectionModel.PricePointModel> points = selectedPricePointsForSelectedProduct();
            if (points.isEmpty()) {
                String empty = "暂无足够成交数据";
                font.drawStringWithShadow(empty, chartX + Math.max(4, (chartWidth - font.getStringWidth(empty)) / 2),
                    chartY + chartHeight / 2 - 4, 0xFF718396);
            } else {
                drawMarketChart(font, points, chartX, chartY, chartWidth, chartHeight, mouseX, mouseY);
            }
        }

        private void drawRangeTab(FontRenderer font, GuiRect tab, String label, boolean selected) {
            Gui.drawRect(tab.getX(), tab.getY(), tab.getRight(), tab.getBottom(),
                selected ? 0xFF285B88 : 0xFF1A2937);
            int x = tab.getX() + Math.max(2, (tab.getWidth() - font.getStringWidth(label)) / 2);
            font.drawStringWithShadow(label, x, tab.getY() + 3, selected ? 0xFFFFFFFF : 0xFF91A4B6);
        }

        private void drawMarketChart(FontRenderer font, List<TerminalMarketSectionModel.PricePointModel> points,
            int x, int y, int width, int height, int mouseX, int mouseY) {
            long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            long maximumVolume = 0L;
            for (TerminalMarketSectionModel.PricePointModel point : points) {
                if (point.isEmpty() || point.getPrice() <= 0L) { continue; }
                min = Math.min(min, point.getLow());
                max = Math.max(max, point.getHigh());
                if (point.isTrade()) { maximumVolume = Math.max(maximumVolume, point.getQuantity()); }
            }
            if (min == Long.MAX_VALUE || max == Long.MIN_VALUE) { return; }
            if (min == max) {
                long padding = Math.max(1L, max / 100L);
                min = Math.max(0L, min - padding);
                max += padding;
            }
            int axisWidth = Math.max(MarketCompactText.width(font, String.valueOf(max), MarketCompactText.AXIS_SCALE),
                MarketCompactText.width(font, String.valueOf(min), MarketCompactText.AXIS_SCALE)) + 4;
            int xAxisHeight = 7;
            int plotX = x + 3;
            int plotY = y + 3;
            int plotWidth = Math.max(12, width - axisWidth - 6);
            int plotHeight = Math.max(12, height - xAxisHeight - 6);
            int volumeHeight = Math.max(8, plotHeight / 4);
            int priceHeight = Math.max(8, plotHeight - volumeHeight - 3);
            for (int line = 0; line <= 3; line++) {
                int gridY = plotY + line * priceHeight / 3;
                Gui.drawRect(plotX, gridY, plotX + plotWidth, gridY + 1, 0x2B3B5268);
            }
            for (int line = 0; line <= 3; line++) {
                int gridX = plotX + line * plotWidth / 3;
                Gui.drawRect(gridX, plotY, gridX + 1, plotY + plotHeight, 0x223B5268);
            }
            long range = Math.max(1L, max - min);
            int pointDivisor = Math.max(1, points.size() - 1);
            int barWidth = Math.max(1, Math.min(7, plotWidth / Math.max(1, points.size()) - 2));
            int previousX = -1;
            int previousY = -1;
            long previousPrice = 0L;
            for (int index = 0; index < points.size(); index++) {
                TerminalMarketSectionModel.PricePointModel point = points.get(index);
                if (point.isEmpty() || point.getPrice() <= 0L) { continue; }
                int candleX = plotX + index * plotWidth / pointDivisor;
                int openY = priceY(point.getOpen(), min, range, plotY, priceHeight);
                int highY = priceY(point.getHigh(), min, range, plotY, priceHeight);
                int lowY = priceY(point.getLow(), min, range, plotY, priceHeight);
                int closeY = priceY(point.getPrice(), min, range, plotY, priceHeight);
                boolean up = point.getPrice() >= point.getOpen();
                int movementColor = up ? 0xFF4FCB63 : 0xFFE05252;
                if (previousX >= 0) {
                    int lineColor = point.isReference() ? 0xFFB69A4A
                        : point.isCarryForward() ? 0xFF7890A5
                        : point.getPrice() >= previousPrice ? 0xFF4FCB63 : 0xFFE05252;
                    drawSmoothLine(previousX, previousY, candleX, closeY, lineColor);
                }
                if (point.isTrade()) {
                    Gui.drawRect(candleX, highY, candleX + 1, lowY + 1, movementColor);
                    int bodyTop = Math.min(openY, closeY);
                    int bodyBottom = Math.max(openY, closeY);
                    Gui.drawRect(candleX - barWidth / 2, bodyTop,
                        candleX + Math.max(1, (barWidth + 1) / 2), Math.max(bodyTop + 1, bodyBottom + 1),
                        movementColor);
                } else {
                    int markerColor = point.isReference() ? 0xFFB69A4A : 0xFF7890A5;
                    Gui.drawRect(candleX - 1, closeY, candleX + 2, closeY + 1, markerColor);
                }
                if (point.isTrade() && point.getQuantity() > 0L && maximumVolume > 0L) {
                    int volumeBar = (int) Math.max(1L, point.getQuantity() * volumeHeight / maximumVolume);
                    int volumeY = plotY + plotHeight - volumeBar;
                    Gui.drawRect(Math.max(plotX, candleX - barWidth / 2), volumeY,
                        Math.min(plotX + plotWidth, candleX + Math.max(1, (barWidth + 1) / 2)),
                        plotY + plotHeight, up ? 0xAA4FCB63 : 0xAAE05252);
                }
                previousX = candleX;
                previousY = closeY;
                previousPrice = point.getPrice();
            }
            String middle = String.valueOf(min + range / 2L);
            MarketCompactText.draw(font, String.valueOf(max), plotX + plotWidth + 2, plotY,
                0xFF8FA2B8, MarketCompactText.AXIS_SCALE);
            MarketCompactText.draw(font, middle, plotX + plotWidth + 2,
                plotY + priceHeight / 2 - 3, 0xFF8FA2B8, MarketCompactText.AXIS_SCALE);
            MarketCompactText.draw(font, String.valueOf(min), plotX + plotWidth + 2,
                plotY + priceHeight - 6, 0xFF8FA2B8, MarketCompactText.AXIS_SCALE);
            String start = "-" + state.getSelectedChartRange();
            MarketCompactText.draw(font, start, plotX, y + height - 6, 0xFF718396,
                MarketCompactText.AXIS_SCALE);
            drawTimeLabel(font, timeLabel(1), plotX + plotWidth / 4, y + height - 6);
            drawTimeLabel(font, timeLabel(2), plotX + plotWidth / 2, y + height - 6);
            drawTimeLabel(font, timeLabel(3), plotX + plotWidth * 3 / 4, y + height - 6);
            String now = "现在";
            MarketCompactText.draw(font, now,
                plotX + plotWidth - MarketCompactText.width(font, now, MarketCompactText.AXIS_SCALE),
                y + height - 6, 0xFF718396, MarketCompactText.AXIS_SCALE);
            String volumeAxis = maximumVolume <= 0L ? "0" : compactAxisValue(maximumVolume);
            MarketCompactText.draw(font, volumeAxis, plotX + plotWidth + 2,
                plotY + plotHeight - 6, 0xFF718396, MarketCompactText.AXIS_SCALE);
            if (mouseX >= plotX && mouseX <= plotX + plotWidth && mouseY >= plotY
                && mouseY <= plotY + plotHeight) {
                drawCrosshair(font, points, plotX, plotY, plotWidth, plotHeight, priceHeight, min, range,
                    mouseX, mouseY);
            }
        }

        private int priceY(long price, long min, long range, int plotY, int priceHeight) {
            return plotY + priceHeight - (int) ((price - min) * priceHeight / Math.max(1L, range));
        }

        private void drawSmoothLine(int x0, int y0, int x1, int y1, int color) {
            boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
            boolean smooth = GL11.glIsEnabled(GL11.GL_LINE_SMOOTH);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(1.35F);
            GL11.glColor4f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F, ((color >>> 24) & 0xFF) / 255.0F);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(x0 + 0.5F, y0 + 0.5F);
            GL11.glVertex2f(x1 + 0.5F, y1 + 0.5F);
            GL11.glEnd();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glLineWidth(1.0F);
            if (!smooth) { GL11.glDisable(GL11.GL_LINE_SMOOTH); }
            if (!blend) { GL11.glDisable(GL11.GL_BLEND); }
            if (texture) { GL11.glEnable(GL11.GL_TEXTURE_2D); }
        }

        private void drawCrosshair(FontRenderer font, List<TerminalMarketSectionModel.PricePointModel> points,
            int plotX, int plotY, int plotWidth, int plotHeight, int priceHeight, long min, long range,
            int mouseX, int mouseY) {
            int divisor = Math.max(1, points.size() - 1);
            int index = points.size() == 1 ? 0
                : Math.max(0, Math.min(points.size() - 1,
                    Math.round((mouseX - plotX) * divisor / (float) Math.max(1, plotWidth))));
            TerminalMarketSectionModel.PricePointModel point = points.get(index);
            int pointX = plotX + index * plotWidth / divisor;
            drawDashedVertical(pointX, plotY, plotY + plotHeight, 0x999EB0C0);
            if (!point.isEmpty() && point.getPrice() > 0L) {
                int pointY = priceY(point.getPrice(), min, range, plotY, priceHeight);
                drawDashedHorizontal(plotX, plotX + plotWidth, pointY, 0x999EB0C0);
                Gui.drawRect(pointX - 1, pointY - 1, pointX + 2, pointY + 2, 0xFFE5EEF7);
            }

            int tooltipWidth = Math.min(116, Math.max(88, plotWidth / 2));
            int tooltipHeight = 58;
            int tooltipX = pointX + 7;
            if (tooltipX + tooltipWidth > plotX + plotWidth) { tooltipX = pointX - tooltipWidth - 7; }
            tooltipX = Math.max(plotX + 2, Math.min(tooltipX, plotX + plotWidth - tooltipWidth - 2));
            int tooltipY = mouseY + 6;
            if (tooltipY + tooltipHeight > plotY + plotHeight) { tooltipY = mouseY - tooltipHeight - 6; }
            tooltipY = Math.max(plotY + 2, Math.min(tooltipY, plotY + plotHeight - tooltipHeight - 2));
            RoundedRectPainter.drawSolid(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight,
                4, 0xFF60788E);
            RoundedRectPainter.drawSolid(tooltipX + 1, tooltipY + 1, tooltipX + tooltipWidth - 1,
                tooltipY + tooltipHeight - 1, 3, 0xF0182532);
            int textX = tooltipX + 5;
            int textY = tooltipY + 4;
            int textWidth = Math.max(12, tooltipWidth - 10);
            drawTooltipLine(font, "时间 " + formatChartTime(point.getCreatedAtEpochSeconds()), textX, textY,
                textWidth, 0xFFDDE8F2);
            if (point.isEmpty()) {
                drawTooltipLine(font, "该时段无价格数据", textX, textY + 13, textWidth, 0xFF8FA2B8);
            } else if (point.isReference()) {
                drawTooltipLine(font, "无成交 / 目录参考基线", textX, textY + 9, textWidth, 0xFFD0B66A);
                drawTooltipLine(font, "价格 " + point.getPrice(), textX, textY + 21, textWidth, 0xFFBFCBDA);
                drawTooltipLine(font, "成交量 0", textX, textY + 33, textWidth, 0xFF8FA2B8);
            } else if (point.isCarryForward()) {
                drawTooltipLine(font, "无成交 / 沿用前收", textX, textY + 9, textWidth, 0xFFA7B8C8);
                drawTooltipLine(font, "价格 " + point.getPrice(), textX, textY + 21, textWidth, 0xFFBFCBDA);
                drawTooltipLine(font, "成交量 0", textX, textY + 33, textWidth, 0xFF8FA2B8);
            } else {
                drawTooltipLine(font, "开 " + point.getOpen() + "  高 " + point.getHigh(), textX, textY + 9,
                    textWidth, 0xFFBFCBDA);
                drawTooltipLine(font, "低 " + point.getLow() + "  收 " + point.getPrice(), textX, textY + 18,
                    textWidth, 0xFFBFCBDA);
                drawTooltipLine(font, "成交量 " + compactAxisValue(point.getQuantity()), textX, textY + 27,
                    textWidth, 0xFF9EB0C0);
                drawTooltipLine(font, "成交额 " + compactAxisValue(point.getTurnover()), textX, textY + 36,
                    textWidth, 0xFF9EB0C0);
            }
        }

        private void drawTooltipLine(FontRenderer font, String value, int x, int y, int maxWidth, int color) {
            MarketCompactText.draw(font, MarketCompactText.trim(font, value, maxWidth, MarketCompactText.CONTENT_SCALE),
                x, y, color, MarketCompactText.CONTENT_SCALE);
        }

        private void drawDashedVertical(int x, int top, int bottom, int color) {
            for (int y = top; y < bottom; y += 5) { Gui.drawRect(x, y, x + 1, Math.min(bottom, y + 3), color); }
        }

        private void drawDashedHorizontal(int left, int right, int y, int color) {
            for (int x = left; x < right; x += 5) { Gui.drawRect(x, y, Math.min(right, x + 3), y + 1, color); }
        }

        private String formatChartTime(long epochSeconds) {
            if (epochSeconds <= 0L) { return "--"; }
            return new SimpleDateFormat("MM-dd HH:mm", Locale.ROOT).format(new Date(epochSeconds * 1000L));
        }

        private String timeLabel(int quarter) {
            if ("1h".equals(state.getSelectedChartRange())) { return "-" + (60 - quarter * 15) + "m"; }
            if ("7d".equals(state.getSelectedChartRange())) { return "-" + (7 - quarter * 2) + "d"; }
            return "-" + (24 - quarter * 6) + "h";
        }

        private void drawTimeLabel(FontRenderer font, String label, int centerX, int y) {
            MarketCompactText.draw(font, label,
                centerX - MarketCompactText.width(font, label, MarketCompactText.AXIS_SCALE) / 2,
                y, 0xFF718396, MarketCompactText.AXIS_SCALE);
        }

        private String compactAxisValue(long value) {
            if (value >= 1000000L) { return (value / 1000000L) + "M"; }
            if (value >= 1000L) { return (value / 1000L) + "K"; }
            return String.valueOf(value);
        }

        private String buildPriceRange(List<TerminalMarketSectionModel.PricePointModel> points) {
            if (points == null || points.isEmpty()) { return "高/低 -- / --"; }
            long high = Long.MIN_VALUE;
            long low = Long.MAX_VALUE;
            for (TerminalMarketSectionModel.PricePointModel point : points) {
                if (point.isEmpty() || point.getPrice() <= 0L) { continue; }
                high = Math.max(high, point.getHigh());
                low = Math.min(low, point.getLow());
            }
            if (high == Long.MIN_VALUE || low == Long.MAX_VALUE) { return "高/低 -- / --"; }
            return "高/低 " + high + " / " + low;
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
            bidHeader = sectionTitle("\u00A7a买盘");
            tradeHeader = sectionTitle("最新成交");
            askHeader = sectionTitle("\u00A7c卖盘");
            addChild(bidHeader);
            addChild(tradeHeader);
            addChild(askHeader);
            for (int i = 0; i < 5; i++) {
                final int row = i;
                LabelPanel bid = secondaryLabel(new Supplier<String>() {
                    @Override
                    public String get() {
                        List<String> lines = limitedBookLines(model.getBidLines(), 5);
                        return "\u00A7a" + (row < lines.size() ? lines.get(row) : "--");
                    }
                });
                LabelPanel trade = secondaryLabel(new Supplier<String>() {
                    @Override
                    public String get() {
                        List<TerminalMarketSectionModel.PricePointModel> points = latestRealTrades(
                            selectedPricePointsForSelectedProduct(), 5);
                        if (row >= points.size()) { return "\u00A77--"; }
                        TerminalMarketSectionModel.PricePointModel point = points.get(row);
                        return "\u00A77" + point.getPrice() + " x" + point.getQuantity();
                    }
                });
                LabelPanel ask = secondaryLabel(new Supplier<String>() {
                    @Override
                    public String get() {
                        List<String> lines = limitedBookLines(model.getAskLines(), 5);
                        return "\u00A7c" + (row < lines.size() ? lines.get(row) : "--");
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
            int rows = 5;
            int rowStartY = bounds.getY() + (headerHeight > 0 ? 20 : 6);
            int rowArea = Math.max(0, bounds.getBottom() - 6 - rowStartY);
            int rowStep = rows == 0 ? 0 : Math.max(8, rowArea / rows);
            int rowHeight = Math.max(0, Math.min(10, rowStep));
            for (int i = 0; i < 5; i++) {
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

        @Override
        public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
            if (mouseButton != 0) { return false; }
            for (int row = 0; row < 5; row++) {
                if (bidRows.get(row).getBounds().contains(mouseX, mouseY)) {
                    return openBookLevel(model.getBidLines(), row,
                        TerminalMarketSectionState.OrderSide.SELL);
                }
                if (askRows.get(row).getBounds().contains(mouseX, mouseY)) {
                    return openBookLevel(model.getAskLines(), row,
                        TerminalMarketSectionState.OrderSide.BUY);
                }
            }
            return false;
        }

        private boolean openBookLevel(List<String> source, int row,
            TerminalMarketSectionState.OrderSide side) {
            List<String> levels = limitedBookLines(source, 5);
            if (row < 0 || row >= levels.size()) { return false; }
            long[] level = parseBookLevel(levels.get(row));
            if (level == null) { return false; }
            state.setOrderSide(side);
            state.setOrderType(TerminalMarketSectionState.OrderType.LIMIT);
            if (side == TerminalMarketSectionState.OrderSide.BUY) {
                state.setLimitBuyPriceText(String.valueOf(level[0]));
                state.setLimitBuyQuantityText(String.valueOf(level[1]));
            } else {
                state.setLimitSellPriceText(String.valueOf(level[0]));
                state.setLimitSellQuantityText(String.valueOf(level[1]));
            }
            if (actionHandler != null) {
                actionHandler.openOrderConfirm(side, TerminalMarketSectionState.OrderType.LIMIT);
            }
            return true;
        }
    }

    private final class StatusTileRowPanel extends PanelContainer {

        private final StatusTilePanel availableTile;
        private final StatusTilePanel claimableTile;
        private final StatusTilePanel orderTile;

        private StatusTileRowPanel() {
            availableTile = new StatusTilePanel("可用库存", new Supplier<String>() {
                @Override
                public String get() {
                    return model.getSourceAvailable();
                }
            }, "Base Vault", TerminalMarketVisuals.StatusIconKind.INVENTORY);
            claimableTile = new StatusTilePanel("待入库", new Supplier<String>() {
                @Override
                public String get() {
                    return model.getClaimableQuantity();
                }
            }, "仅异常交付显示", TerminalMarketVisuals.StatusIconKind.DELIVERY);
            orderTile = new StatusTilePanel("当前委托", new Supplier<String>() {
                @Override
                public String get() {
                    return compactOrderCount();
                }
            }, "可撤订单 / 冻结资金", TerminalMarketVisuals.StatusIconKind.ORDERS);
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
        private final TerminalMarketVisuals.StatusIconKind iconKind;

        private StatusTilePanel(final String title, final Supplier<String> valueSupplier, final String subtitle,
            TerminalMarketVisuals.StatusIconKind iconKind) {
            this.iconKind = iconKind;
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
            TerminalMarketVisuals.drawStatusIcon(bounds.getX() + 6, bounds.getY() + bounds.getHeight() / 2 - 4,
                true, iconKind);
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
