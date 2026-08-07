package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import com.jsirgalaxybase.client.gui.framework.AbstractGuiPanel;
import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;

/** Custom listings use the shared browse/detail interaction, not a permanent two-pane page. */
public final class TerminalCustomMarketSection extends PanelContainer {

    public interface ActionHandler {
        void selectListing(String scope, String listingId);
        void openPublishConfirm();
        void openBuyConfirm();
        void openCancelConfirm();
        void openClaimConfirm();
    }

    private final TerminalPanelFactory panels;
    private final TerminalCustomMarketSectionModel model;
    private final TerminalCustomMarketSectionState state;
    private final ActionHandler actions;
    private final BrowseToolbarPanel toolbar;
    private final MarketItemGridPanel grid;
    private final CustomDetailPanel detail;

    public TerminalCustomMarketSection(TerminalPanelFactory panels, TerminalCustomMarketSectionModel model,
        TerminalCustomMarketSectionState state, ActionHandler actions) {
        this.panels = panels;
        this.model = model == null ? TerminalCustomMarketSectionModel.placeholder() : model;
        this.state = state == null ? new TerminalCustomMarketSectionState() : state;
        this.actions = actions;
        toolbar = new BrowseToolbarPanel();
        grid = new MarketItemGridPanel(buildItems(), new MarketItemGridPanel.Listener() {
            @Override public void select(MarketBrowseItemModel item) {
                TerminalCustomMarketSection.this.state.requestDetail(item.getKey());
                if (TerminalCustomMarketSection.this.actions != null) {
                    TerminalCustomMarketSection.this.actions.selectListing(TerminalCustomMarketSection.this.state.getSelectedScope(), item.getKey());
                }
            }
            @Override public void scrollOffsetChanged(int offset) { TerminalCustomMarketSection.this.state.setBrowserGridScrollOffset(offset); }
        });
        grid.setScrollOffset(this.state.getBrowserGridScrollOffset());
        detail = new CustomDetailPanel();
        addChild(toolbar); addChild(grid); addChild(detail);
    }

    @Override public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        if (state.isDetailView()) {
            toolbar.setVisible(false); grid.setVisible(false); detail.setVisible(true); detail.setBounds(bounds); return;
        }
        toolbar.setVisible(true); grid.setVisible(true); detail.setVisible(false);
        toolbar.setBounds(new GuiRect(bounds.getX(), bounds.getY(), bounds.getWidth(), 27));
        grid.setBounds(new GuiRect(bounds.getX(), bounds.getY() + 32, bounds.getWidth(), Math.max(0, bounds.getHeight() - 32)));
    }

    private List<MarketBrowseItemModel> buildItems() {
        List<String> ids = "selling".equals(state.getSelectedScope()) ? model.getSellingListingIds()
            : "pending".equals(state.getSelectedScope()) ? model.getPendingListingIds() : model.getActiveListingIds();
        List<String> lines = "selling".equals(state.getSelectedScope()) ? model.getSellingListingLines()
            : "pending".equals(state.getSelectedScope()) ? model.getPendingListingLines() : model.getActiveListingLines();
        List<String> icons = "selling".equals(state.getSelectedScope()) ? model.getSellingListingIconRefs()
            : "pending".equals(state.getSelectedScope()) ? model.getPendingListingIconRefs() : model.getActiveListingIconRefs();
        List<MarketBrowseItemModel> result = new ArrayList<MarketBrowseItemModel>();
        String query = state.getBrowserQuery().toLowerCase();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i); if (id == null || id.trim().isEmpty()) continue;
            String line = i < lines.size() ? lines.get(i) : "挂牌 #" + id;
            String title = primary(line), subtitle = secondary(line);
            if (!query.isEmpty() && !(title + " " + subtitle).toLowerCase().contains(query)) continue;
            result.add(new MarketBrowseItemModel(id, i < icons.size() ? icons.get(i) : "", title,
                price(line), subtitle, "--", "--", "--", "--", Collections.emptyList()));
        }
        return result;
    }

    private final class BrowseToolbarPanel extends AbstractGuiPanel {
        private boolean pressed;
        @Override public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect b = getBounds(); RoundedRectPainter.draw(b, 0xFF324152, 0xFF121B25);
            FontRenderer f = Minecraft.getMinecraft().fontRenderer;
            String[] scopes = { "全部挂牌", "我的出售", "待领取" };
            String[] values = { "active", "selling", "pending" };
            int buttonWidth = Math.max(44, b.getWidth() / 7);
            for (int i = 0; i < scopes.length; i++) {
                int x = b.getX() + 5 + i * (buttonWidth + 4); boolean selected = values[i].equals(state.getSelectedScope());
                RoundedRectPainter.draw(new GuiRect(x, b.getY() + 4, buttonWidth, 19), selected ? 0xFF4D91D9 : 0xFF2C4052,
                    selected ? 0xFF1E3E59 : 0xFF14222E);
                f.drawStringWithShadow(f.trimStringToWidth(scopes[i], buttonWidth - 8), x + 4, b.getY() + 9, 0xFFE4EDF7);
            }
            int publishWidth = Math.max(56, b.getWidth() / 6);
            int publishX = b.getRight() - publishWidth - 5;
            RoundedRectPainter.draw(new GuiRect(publishX, b.getY() + 4, publishWidth, 19), 0xFF4D91D9, 0xFF173551);
            f.drawStringWithShadow("发布手持", publishX + 7, b.getY() + 9, 0xFFE4EDF7);
        }
        @Override public boolean mouseClicked(GuiScene scene, int x, int y, int button) { pressed = button == 0 && contains(x, y); return pressed; }
        @Override public boolean mouseReleased(GuiScene scene, int x, int y, int button) {
            boolean click = pressed && button == 0 && contains(x, y); pressed = false; if (!click) return false;
            GuiRect b = getBounds(); int buttonWidth = Math.max(44, b.getWidth() / 7);
            String[] values = { "active", "selling", "pending" };
            for (int i = 0; i < values.length; i++) if (new GuiRect(b.getX() + 5 + i * (buttonWidth + 4), b.getY() + 4, buttonWidth, 19).contains(x, y)) {
                state.setSelectedScope(values[i]);
                state.setSelectedListingId("");
                state.returnToBrowse();
                if (actions != null) {
                    actions.selectListing(values[i], "");
                }
                return true;
            }
            if (x >= b.getRight() - Math.max(56, b.getWidth() / 6) - 5 && actions != null) { actions.openPublishConfirm(); return true; }
            return false;
        }
    }

    private final class CustomDetailPanel extends AbstractGuiPanel {
        private final ButtonPanel buy = button("购买", () -> actions.openBuyConfirm(), () -> model.isCanBuy());
        private final ButtonPanel cancel = button("下架", () -> actions.openCancelConfirm(), () -> model.isCanCancel());
        private final ButtonPanel claim = button("领取", () -> actions.openClaimConfirm(), () -> model.isCanClaim());
        private final ButtonPanel back = button("返回浏览", () -> { state.returnToBrowse(); setBounds(TerminalCustomMarketSection.this.getBounds()); }, () -> true);
        private CustomDetailPanel() { addChild(buy); addChild(cancel); addChild(claim); addChild(back); }
        @Override public void setBounds(GuiRect b) {
            super.setBounds(b); MarketDetailLayout layout = MarketDetailLayout.within(b); int w = Math.max(1, layout.actions.getWidth() / 4 - 4);
            back.setBounds(new GuiRect(layout.actions.getX(), layout.actions.getY() + 5, w, 20));
            buy.setBounds(new GuiRect(back.getBounds().getRight() + 5, layout.actions.getY() + 5, w, 20));
            cancel.setBounds(new GuiRect(buy.getBounds().getRight() + 5, layout.actions.getY() + 5, w, 20));
            claim.setBounds(new GuiRect(cancel.getBounds().getRight() + 5, layout.actions.getY() + 5,
                Math.max(1, layout.actions.getRight() - cancel.getBounds().getRight() - 5), 20));
        }
        @Override public void draw(GuiScene scene, int mx, int my, float pt) {
            GuiRect b = getBounds(); MarketDetailLayout l = MarketDetailLayout.within(b); FontRenderer f = Minecraft.getMinecraft().fontRenderer;
            RoundedRectPainter.draw(b, 0xFF324152, 0xFF121B25); card(l.hero); card(l.orderBook); card(l.assets);
            TerminalMarketVisuals.drawItemIconOrBadge(l.hero.getX() + 12, l.hero.getY() + 12, 32, model.getSelectedItemIdentity(), model.getSelectedTitle());
            text(f, model.getSelectedTitle(), l.hero.getX() + 54, l.hero.getY() + 12, 0xFFE4EDF7);
            text(f, "价格 " + model.getSelectedPrice(), l.hero.getX() + 54, l.hero.getY() + 29, 0xFFF0C95B);
            text(f, "状态 " + model.getSelectedStatus(), l.hero.getX() + 54, l.hero.getY() + 43, 0xFFBFCBDA);
            text(f, "交易方", l.orderBook.getX() + 10, l.orderBook.getY() + 10, 0xFFE4EDF7);
            text(f, model.getSelectedCounterparty(), l.orderBook.getX() + 10, l.orderBook.getY() + 26, 0xFFBFCBDA);
            text(f, "交付 " + model.getSelectedTradeSummary(), l.orderBook.getX() + 10, l.orderBook.getY() + 42, 0xFFBFCBDA);
            text(f, model.getDisabledReason().isEmpty() ? model.getSelectedActionHint() : model.getDisabledReason(), l.assets.getX() + 10, l.assets.getY() + 12, 0xFFBFCBDA);
        }
        private void card(GuiRect r) { RoundedRectPainter.draw(r, 0xFF2C3D4E, 0xFF101923); }
    }
    private ButtonPanel button(String label, Runnable action, Supplier<Boolean> enabled) { return panels.createButton(new GuiRect(0, 0, 0, 0), () -> label, action, enabled); }
    private static void text(FontRenderer f, String v, int x, int y, int c) { f.drawStringWithShadow(f.trimStringToWidth(v == null ? "--" : v, 240), x, y, c); }
    private static String primary(String v) { int i = v == null ? -1 : v.indexOf('|'); return (i < 0 ? v : v.substring(0, i)).trim(); }
    private static String secondary(String v) { int i = v == null ? -1 : v.indexOf('|'); return i < 0 ? "挂牌" : v.substring(i + 1).trim(); }
    private static String price(String v) { String s = secondary(v); int i = s.indexOf("价格"); return i < 0 ? "挂牌" : s.substring(i); }
}
