package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.jsirgalaxybase.client.gui.framework.AbstractGuiPanel;
import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;
import com.jsirgalaxybase.modules.core.market.application.TaskCoinCatalog;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;

/** Quote market browse/detail shell. The catalog is informational; execution validates the selected Base Vault coin server-side. */
public final class TerminalExchangeMarketSection extends com.jsirgalaxybase.client.gui.framework.PanelContainer {

    public interface ActionHandler {
        void selectTarget(String targetCode);
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
        String query = state.getBrowserQuery().toLowerCase(); List<MarketBrowseItemModel> result = new ArrayList<MarketBrowseItemModel>();
        for (TaskCoinCatalog.Entry entry : TaskCoinCatalog.defaultCatalog().getEntries()) {
            String title = entry.getDisplayName();
            if (!query.isEmpty() && !(title + " " + entry.getFamilyCode() + " " + entry.getTier()).toLowerCase().contains(query)) continue;
            result.add(new MarketBrowseItemModel(entry.getRegistryName(), entry.getRegistryName(), title,
                String.valueOf(entry.getFaceValue()), entry.getFamilyCode() + " / " + entry.getTier(), "--", "--", "--", "--", Collections.emptyList()));
        }
        return result;
    }

    private final class ExchangeToolbar extends AbstractGuiPanel {
        @Override public void draw(GuiScene scene, int mx, int my, float pt) {
            GuiRect b = getBounds(); RoundedRectPainter.draw(b, 0xFF324152, 0xFF121B25); FontRenderer f = Minecraft.getMinecraft().fontRenderer;
            f.drawStringWithShadow("任务书硬币目录 / " + TaskCoinCatalog.defaultCatalog().getEntries().size() + " 种", b.getX() + 8, b.getY() + 9, 0xFFE4EDF7);
            String held = model.getHeldSummary(); f.drawStringWithShadow(f.trimStringToWidth("个人仓选择: " + held, Math.max(20, b.getWidth() / 2)), b.getX() + b.getWidth() / 2, b.getY() + 9, 0xFFBFCBDA);
        }
    }

    private final class ExchangeDetail extends AbstractGuiPanel {
        private final ButtonPanel back = button("返回浏览", () -> { state.returnToBrowse(); setBounds(TerminalExchangeMarketSection.this.getBounds()); }, () -> true);
        private final ButtonPanel refresh = button("刷新报价", () -> { if (actions != null) actions.refreshQuote(); }, () -> true);
        private final ButtonPanel confirm = button("确认兑换", () -> { if (actions != null) actions.openExchangeConfirm(); }, () -> model.isExecutable());
        private ExchangeDetail() { addChild(back); addChild(refresh); addChild(confirm); }
        @Override public void setBounds(GuiRect b) {
            super.setBounds(b); MarketDetailLayout l = MarketDetailLayout.within(b); int w = Math.max(1, (l.actions.getWidth() - 10) / 3);
            back.setBounds(new GuiRect(l.actions.getX(), l.actions.getY() + 5, w, 20));
            refresh.setBounds(new GuiRect(back.getBounds().getRight() + 5, l.actions.getY() + 5, w, 20));
            confirm.setBounds(new GuiRect(refresh.getBounds().getRight() + 5, l.actions.getY() + 5,
                Math.max(1, l.actions.getRight() - refresh.getBounds().getRight() - 5), 20));
        }
        @Override public void draw(GuiScene scene, int mx, int my, float pt) {
            GuiRect b = getBounds(); MarketDetailLayout l = MarketDetailLayout.within(b); FontRenderer f = Minecraft.getMinecraft().fontRenderer;
            RoundedRectPainter.draw(b, 0xFF324152, 0xFF121B25); card(l.hero); card(l.orderBook); card(l.assets);
            TerminalMarketVisuals.drawItemIconOrBadge(l.hero.getX() + 12, l.hero.getY() + 12, 32, model.getInputRegistryName(), model.getHeldSummary());
            TerminalMarketVisuals.drawCoin(l.hero.getRight() - 44, l.hero.getY() + 14, 28, TerminalMarketVisuals.COLOR_GOLD);
            text(f, model.getInputAssetCode() + " -> " + model.getOutputAssetCode(), l.hero.getX() + 54, l.hero.getY() + 10, 0xFFE4EDF7);
            text(f, model.getRateDisplay(), l.hero.getX() + 54, l.hero.getY() + 27, 0xFFF0C95B);
            text(f, model.getRuleVersion() + " / " + model.getLimitStatus(), l.hero.getX() + 54, l.hero.getY() + 43, 0xFFBFCBDA);
            text(f, "输入 " + model.getHeldSummary(), l.orderBook.getX() + 10, l.orderBook.getY() + 10, 0xFFBFCBDA);
            text(f, "面值 " + model.getNominalFaceValue() + "  实际 " + model.getEffectiveExchangeValue(), l.orderBook.getX() + 10, l.orderBook.getY() + 27, 0xFFBFCBDA);
            text(f, "输出 " + model.getContributionValue() + " " + model.getOutputAssetCode(), l.orderBook.getX() + 10, l.orderBook.getY() + 44, 0xFFE4EDF7);
            text(f, model.getDisabledReason().isEmpty() ? model.getExecutionHint() : model.getDisabledReason(), l.assets.getX() + 10, l.assets.getY() + 12, 0xFFBFCBDA);
        }
        private void card(GuiRect r) { RoundedRectPainter.draw(r, 0xFF2C3D4E, 0xFF101923); }
    }
    private ButtonPanel button(String label, Runnable action, java.util.function.Supplier<Boolean> enabled) { return panels.createButton(new GuiRect(0, 0, 0, 0), () -> label, action, enabled); }
    private static void text(FontRenderer f, String value, int x, int y, int color) { f.drawStringWithShadow(f.trimStringToWidth(value == null ? "--" : value, 260), x, y, color); }
}
