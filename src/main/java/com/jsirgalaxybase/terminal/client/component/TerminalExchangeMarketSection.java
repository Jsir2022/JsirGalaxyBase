package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;
import com.jsirgalaxybase.terminal.TerminalMarketBrowseEntry;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;

/** Quote market browse/detail shell. The catalog is informational; execution validates the selected Base Vault coin server-side. */
public final class TerminalExchangeMarketSection extends com.jsirgalaxybase.client.gui.framework.PanelContainer {

    public interface ActionHandler {
        void selectTarget(String targetCode);
        default void refreshBrowse() {}
        default void changeBrowsePage(int pageIndex) {}
        void refreshQuote();
        void openExchangeConfirm();
    }

    private final TerminalPanelFactory panels;
    private final TerminalExchangeMarketSectionModel model;
    private final TerminalExchangeMarketSectionState state;
    private final ActionHandler actions;
    private final ExchangeToolbar toolbar;
    private final MarketItemGridPanel grid;
    private final ExchangeDetail detail;

    public TerminalExchangeMarketSection(TerminalPanelFactory panels, TerminalExchangeMarketSectionModel model,
        TerminalExchangeMarketSectionState state, ActionHandler actions) {
        this.panels = panels;
        this.model = model == null ? TerminalExchangeMarketSectionModel.placeholder() : model;
        this.state = state == null ? new TerminalExchangeMarketSectionState() : state;
        this.actions = actions;
        toolbar = new ExchangeToolbar();
        grid = new MarketItemGridPanel(buildItems(), new MarketItemGridPanel.Listener() {
            @Override public void select(MarketBrowseItemModel item) {
                TerminalExchangeMarketSection.this.state.requestDetail(item.getKey());
                if (TerminalExchangeMarketSection.this.actions != null) {
                    TerminalExchangeMarketSection.this.actions.selectTarget(item.getKey());
                }
            }
            @Override public void scrollOffsetChanged(int offset) { TerminalExchangeMarketSection.this.state.setBrowserGridScrollOffset(offset); }
        });
        grid.setScrollOffset(this.state.getBrowserGridScrollOffset());
        detail = new ExchangeDetail(); addChild(toolbar); addChild(grid); addChild(detail);
    }

    @Override public void setBounds(GuiRect b) {
        super.setBounds(b);
        if (state.isDetailView()) { toolbar.setVisible(false); grid.setVisible(false); detail.setVisible(true); detail.setBounds(b); return; }
        toolbar.setVisible(true); grid.setVisible(true); detail.setVisible(false);
        toolbar.setBounds(new GuiRect(b.getX(), b.getY(), b.getWidth(), 27));
        grid.setBounds(new GuiRect(b.getX(), b.getY() + 32, b.getWidth(), Math.max(0, b.getHeight() - 32)));
    }

    private List<MarketBrowseItemModel> buildItems() {
        List<MarketBrowseItemModel> result = new ArrayList<MarketBrowseItemModel>();
        for (TerminalMarketBrowseEntry entry : model.getBrowseEntries()) {
            result.add(MarketBrowseItemModel.exchangeCoin(entry.getKey(), entry.getItemIdentity(), entry.getTitle(),
                entry.getPrimaryValue(), entry.getSubtitle(), entry.getStatus()));
        }
        return result;
    }

    private final class ExchangeToolbar extends com.jsirgalaxybase.client.gui.framework.PanelContainer {
        private final TerminalTextFieldPanel query = new TerminalTextFieldPanel(
            () -> state.getBrowserQuery(), value -> state.setBrowserQuery(value),
            () -> Boolean.valueOf(state.isBrowserQueryFocused()), () -> state.focusBrowserQuery(), "搜索任务书硬币...", 32, null);
        private final ButtonPanel refresh = button("刷新", () -> { if (actions != null) actions.refreshBrowse(); }, () -> true);
        private final ButtonPanel previous = button("<", () -> { if (actions != null) actions.changeBrowsePage(Math.max(0, model.getBrowsePageIndex() - 1)); },
            () -> Boolean.valueOf(model.hasPreviousPage()));
        private final ButtonPanel next = button(">", () -> { if (actions != null) actions.changeBrowsePage(model.getBrowsePageIndex() + 1); },
            () -> Boolean.valueOf(model.hasNextPage()));
        private ExchangeToolbar() { addChild(query); addChild(refresh); addChild(previous); addChild(next); }
        @Override public void setBounds(GuiRect b) {
            super.setBounds(b);
            query.setBounds(new GuiRect(b.getX() + 5, b.getY() + 4, Math.max(64, b.getWidth() - 176), 19));
            refresh.setBounds(new GuiRect(query.getBounds().getRight() + 4, b.getY() + 4, 38, 19));
            previous.setBounds(new GuiRect(b.getRight() - 49, b.getY() + 4, 20, 19));
            next.setBounds(new GuiRect(b.getRight() - 25, b.getY() + 4, 20, 19));
        }
        @Override protected void drawSelf(GuiScene scene, int mx, int my, float pt) {
            GuiRect b = getBounds(); RoundedRectPainter.draw(b, 0xFF324152, 0xFF121B25);
            FontRenderer f = Minecraft.getMinecraft().fontRenderer;
            String page = "支持 " + model.getBrowseTotalEntries() + " 种  " + (model.getBrowsePageIndex() + 1) + " / "
                + Math.max(1, (model.getBrowseTotalEntries() + Math.max(1, model.getBrowsePageSize()) - 1)
                    / Math.max(1, model.getBrowsePageSize()));
            int pageX = refresh.getBounds().getRight() + 7;
            int pageWidth = Math.max(1, previous.getBounds().getX() - pageX - 5);
            f.drawStringWithShadow(f.trimStringToWidth(page, pageWidth), pageX, b.getY() + 9, 0xFF9FB0C2);
        }
    }

    private final class ExchangeDetail extends com.jsirgalaxybase.client.gui.framework.PanelContainer {
        private final ButtonPanel refresh = button("刷新报价", () -> { if (actions != null) actions.refreshQuote(); },
            () -> Boolean.valueOf(state.getSelectedVaultSlot() >= 0));
        private final ButtonPanel confirm = panels.createButton(new GuiRect(0, 0, 0, 0),
            () -> state.getSelectedVaultSlot() < 0 ? "选择个人仓来源" : "确认兑换",
            () -> { if (actions != null) actions.openExchangeConfirm(); },
            () -> Boolean.valueOf(model.hasSelectedTarget()));
        private ExchangeDetail() { addChild(refresh); addChild(confirm); }
        @Override public void setBounds(GuiRect b) {
            super.setBounds(b);
            ExchangeDetailLayout l = ExchangeDetailLayout.within(b);
            int inset = 5;
            int buttonWidth = Math.max(1, (l.actions.getWidth() - inset * 3) / 2);
            refresh.setBounds(new GuiRect(l.actions.getX() + inset, l.actions.getY() + inset, buttonWidth,
                Math.max(1, l.actions.getHeight() - inset * 2)));
            confirm.setBounds(new GuiRect(refresh.getBounds().getRight() + inset, l.actions.getY() + inset,
                Math.max(1, l.actions.getRight() - refresh.getBounds().getRight() - inset * 2),
                Math.max(1, l.actions.getHeight() - inset * 2)));
        }
        @Override protected void drawSelf(GuiScene scene, int mx, int my, float pt) {
            GuiRect b = getBounds(); ExchangeDetailLayout l = ExchangeDetailLayout.within(b);
            FontRenderer f = Minecraft.getMinecraft().fontRenderer;
            RoundedRectPainter.draw(b, 0xFF324152, 0xFF121B25);
            card(l.hero); card(l.source); card(l.quote); card(l.actions);

            MarketBrowseItemModel selected = selectedBrowseItem();
            String itemIdentity = selected == null ? model.getInputRegistryName() : selected.getIconRef();
            String title = selected == null ? model.getSelectedTargetTitle() : selected.getTitle();
            String summary = selected == null ? model.getSelectedTargetSummary() : selected.getTooltipPrimary();
            String outputAsset = displayOrFallback(model.getOutputAssetCode(), "STARCOIN");
            TerminalMarketVisuals.drawItemIconOrBadge(l.hero.getX() + 10, l.hero.getY() + 8, 30, itemIdentity, title);
            TerminalMarketVisuals.drawCoin(l.hero.getRight() - 38, l.hero.getY() + 10, 24,
                TerminalMarketVisuals.COLOR_GOLD);
            line(f, title, l.hero, 48, 9, 0xFFE4EDF7);
            line(f, summary, l.hero, 48, 26, 0xFFBFCBDA);
            lineRight(f, "-> " + outputAsset, l.hero, 45, 17, 0xFFF0C95B);

            line(f, "个人仓来源", l.source, 10, 10, 0xFFE4EDF7);
            line(f, state.getSelectedVaultSlot() < 0 ? "尚未选择格位" : "已选择第 "
                + (state.getSelectedVaultSlot() + 1) + " 格", l.source, 10, 29,
                state.getSelectedVaultSlot() < 0 ? 0xFFF0C95B : 0xFF63D478);
            line(f, state.getSelectedVaultSlot() < 0 ? "从个人仓选择实际任务书硬币" : model.getHeldSummary(),
                l.source, 10, 46, 0xFFBFCBDA);
            line(f, "币种 " + title, l.source, 10, 68, 0xFF9FB0C2);
            line(f, "输入数量 " + model.getInputQuantity(), l.source, 10, 86, 0xFF9FB0C2);

            line(f, "正式报价", l.quote, 10, 10, 0xFFE4EDF7);
            line(f, "兑换对 " + displayOrFallback(model.getPairCode(), title + " -> " + outputAsset),
                l.quote, 10, 29, 0xFFBFCBDA);
            line(f, "汇率 " + model.getRateDisplay(), l.quote, 10, 46, 0xFFF0C95B);
            line(f, "面值 " + model.getNominalFaceValue() + " / 实际 " + model.getEffectiveExchangeValue(),
                l.quote, 10, 63, 0xFFBFCBDA);
            line(f, "预计输出 " + model.getContributionValue() + " " + outputAsset,
                l.quote, 10, 80, 0xFFE4EDF7);
            line(f, model.getRuleVersion() + " / " + model.getLimitStatusDisplay(), l.quote, 10, 102, 0xFF9FB0C2);
            String feedback = model.getDisabledReason().isEmpty() ? model.getExecutionHint() : model.getDisabledReason();
            line(f, feedback, l.quote, 10, 119, model.isExecutable() ? 0xFF63D478 : 0xFFE7B75A);
        }
        private void card(GuiRect r) { RoundedRectPainter.draw(r, 0xFF2C3D4E, 0xFF101923); }
    }
    private MarketBrowseItemModel selectedBrowseItem() {
        String selectedKey = state.toPayload().getSelectedCoinCode();
        if (selectedKey == null || selectedKey.isEmpty()) return null;
        for (MarketBrowseItemModel item : buildItems()) {
            if (selectedKey.equals(item.getKey())) return item;
        }
        return null;
    }
    private ButtonPanel button(String label, Runnable action, java.util.function.Supplier<Boolean> enabled) { return panels.createButton(new GuiRect(0, 0, 0, 0), () -> label, action, enabled); }
    private static void line(FontRenderer f, String value, GuiRect card, int offsetX, int offsetY, int color) {
        int width = Math.max(1, card.getWidth() - offsetX - 9);
        f.drawStringWithShadow(f.trimStringToWidth(value == null ? "--" : value, width),
            card.getX() + offsetX, card.getY() + offsetY, color);
    }
    private static void lineRight(FontRenderer f, String value, GuiRect card, int rightInset, int offsetY, int color) {
        String display = value == null ? "--" : value;
        int maxWidth = Math.max(1, card.getWidth() / 3);
        display = f.trimStringToWidth(display, maxWidth);
        f.drawStringWithShadow(display, card.getRight() - rightInset - f.getStringWidth(display),
            card.getY() + offsetY, color);
    }
    private static String displayOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() || "--".equals(value) ? fallback : value;
    }
}
