package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.RoundedRectPainter;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;
import com.jsirgalaxybase.terminal.TerminalMarketBrowseEntry;

/** Custom listings use the shared browse/detail interaction, not a permanent two-pane page. */
public final class TerminalCustomMarketSection extends PanelContainer {

    public interface ActionHandler {
        void selectListing(String scope, String listingId);
        default void refreshBrowse() {}
        default void changeBrowsePage(int pageIndex) {}
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
        List<MarketBrowseItemModel> rows = new ArrayList<MarketBrowseItemModel>();
        for (TerminalMarketBrowseEntry entry : model.getBrowseEntries()) {
            rows.add(MarketBrowseItemModel.customListing(entry.getKey(), entry.getItemIdentity(), entry.getTitle(),
                entry.getPrimaryValue(), entry.getSubtitle(), entry.getStatus()));
        }
        return rows;
    }

    private final class BrowseToolbarPanel extends PanelContainer {
        private boolean pressed;
        private final TerminalTextFieldPanel query = new TerminalTextFieldPanel(
            () -> state.getBrowserQuery(), value -> state.setBrowserQuery(value),
            () -> Boolean.valueOf(state.isBrowserQueryFocused()), () -> state.focusBrowserQuery(), "搜索挂牌...", 32, null);
        private final ButtonPanel search = button("查", () -> { if (actions != null) actions.refreshBrowse(); }, () -> true);
        private final ButtonPanel publish = button("发布", () -> { if (actions != null) actions.openPublishConfirm(); }, () -> true);
        private final ButtonPanel previous = button("<", () -> { if (actions != null) actions.changeBrowsePage(Math.max(0, model.getBrowsePageIndex() - 1)); },
            () -> Boolean.valueOf(model.hasPreviousPage()));
        private final ButtonPanel next = button(">", () -> { if (actions != null) actions.changeBrowsePage(model.getBrowsePageIndex() + 1); },
            () -> Boolean.valueOf(model.hasNextPage()));
        private BrowseToolbarPanel() { addChild(query); addChild(search); addChild(publish); addChild(previous); addChild(next); }
        @Override public void setBounds(GuiRect b) {
            super.setBounds(b);
            // Reserve fixed slots for search, publish, scope and paging before sizing the text input.
            // This keeps every child inside the toolbar at every supported terminal width.
            int queryWidth = Math.max(64, b.getWidth() - 258);
            query.setBounds(new GuiRect(b.getX() + 5, b.getY() + 4, queryWidth, 19));
            search.setBounds(new GuiRect(query.getBounds().getRight() + 3, b.getY() + 4, 20, 19));
            publish.setBounds(new GuiRect(search.getBounds().getRight() + 3, b.getY() + 4, 43, 19));
            int pageX = b.getRight() - 49;
            previous.setBounds(new GuiRect(pageX, b.getY() + 4, 20, 19));
            next.setBounds(new GuiRect(pageX + 24, b.getY() + 4, 20, 19));
        }
        @Override protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect b = getBounds(); RoundedRectPainter.draw(b, 0xFF324152, 0xFF121B25);
            FontRenderer f = Minecraft.getMinecraft().fontRenderer;
            String[] scopes = { "全部", "出售", "待领" };
            String[] values = { "active", "selling", "pending" };
            int buttonWidth = 38;
            for (int i = 0; i < scopes.length; i++) {
                int x = publish.getBounds().getRight() + 4 + i * (buttonWidth + 2); boolean selected = values[i].equals(state.getSelectedScope());
                RoundedRectPainter.draw(new GuiRect(x, b.getY() + 4, buttonWidth, 19), selected ? 0xFF4D91D9 : 0xFF2C4052,
                    selected ? 0xFF1E3E59 : 0xFF14222E);
                f.drawStringWithShadow(f.trimStringToWidth(scopes[i], buttonWidth - 8), x + 4, b.getY() + 9, 0xFFE4EDF7);
            }
        }
        @Override protected boolean onContainerClicked(GuiScene scene, int x, int y, int button) { pressed = button == 0 && contains(x, y); return pressed; }
        @Override protected boolean onContainerReleased(GuiScene scene, int x, int y, int button) {
            boolean click = pressed && button == 0 && contains(x, y); pressed = false; if (!click) return false;
            GuiRect b = getBounds(); int buttonWidth = 38;
            String[] values = { "active", "selling", "pending" };
            for (int i = 0; i < values.length; i++) if (new GuiRect(publish.getBounds().getRight() + 4 + i * (buttonWidth + 2), b.getY() + 4, buttonWidth, 19).contains(x, y)) {
                state.setSelectedScope(values[i]);
                state.setSelectedListingId("");
                state.returnToBrowse();
                if (actions != null) {
                    actions.selectListing(values[i], "");
                }
                return true;
            }
            return false;
        }
    }

    private final class CustomDetailPanel extends PanelContainer {
        private final ButtonPanel buy = button("购买", () -> actions.openBuyConfirm(), () -> model.isCanBuy());
        private final ButtonPanel cancel = button("下架", () -> actions.openCancelConfirm(), () -> model.isCanCancel());
        private final ButtonPanel claim = button("领取", () -> actions.openClaimConfirm(), () -> model.isCanClaim());
        private CustomDetailPanel() { addChild(buy); addChild(cancel); addChild(claim); }
        @Override public void setBounds(GuiRect b) {
            super.setBounds(b); CustomDetailLayout layout = CustomDetailLayout.within(b); int w = Math.max(1, (layout.actions.getWidth() - 20) / 3);
            buy.setBounds(new GuiRect(layout.actions.getX(), layout.actions.getY() + 5, w, 20));
            cancel.setBounds(new GuiRect(buy.getBounds().getRight() + 10, layout.actions.getY() + 5, w, 20));
            claim.setBounds(new GuiRect(cancel.getBounds().getRight() + 10, layout.actions.getY() + 5,
                Math.max(1, layout.actions.getRight() - cancel.getBounds().getRight() - 10), 20));
        }
        @Override protected void drawSelf(GuiScene scene, int mx, int my, float pt) {
            GuiRect b = getBounds(); CustomDetailLayout l = CustomDetailLayout.within(b); FontRenderer f = Minecraft.getMinecraft().fontRenderer;
            RoundedRectPainter.draw(b, 0xFF324152, 0xFF121B25); card(l.hero); card(l.listing); card(l.delivery); card(l.actions);
            TerminalMarketVisuals.drawItemIconOrBadge(l.hero.getX() + 10, l.hero.getY() + 8, 30, model.getSelectedItemIdentity(), model.getSelectedTitle());
            line(f, model.getSelectedTitle(), l.hero, 48, 9, 0xFFE4EDF7);
            lineRight(f, "价格 " + model.getSelectedPrice(), l.hero, 10, 9, 0xFFF0C95B);
            line(f, "状态 " + model.getSelectedStatus(), l.hero, 48, 27, 0xFFBFCBDA);

            line(f, "挂牌信息", l.listing, 10, 10, 0xFFE4EDF7);
            line(f, "物品 " + model.getSelectedTitle(), l.listing, 10, 31, 0xFFBFCBDA);
            line(f, "标价 " + model.getSelectedPrice(), l.listing, 10, 49, 0xFFF0C95B);
            line(f, "状态 " + model.getSelectedStatus(), l.listing, 10, 67, 0xFF9FB0C2);

            line(f, "交易与交付", l.delivery, 10, 10, 0xFFE4EDF7);
            line(f, model.getSelectedCounterparty(), l.delivery, 10, 31, 0xFFBFCBDA);
            line(f, "交付 " + model.getSelectedTradeSummary(), l.delivery, 10, 49, 0xFFBFCBDA);
            String feedback = model.getDisabledReason().isEmpty() ? model.getSelectedActionHint() : model.getDisabledReason();
            line(f, feedback, l.delivery, 10, 72, model.getDisabledReason().isEmpty() ? 0xFF63D478 : 0xFFE7B75A);
        }
        private void card(GuiRect r) { RoundedRectPainter.draw(r, 0xFF2C3D4E, 0xFF101923); }
    }
    private ButtonPanel button(String label, Runnable action, Supplier<Boolean> enabled) { return panels.createButton(new GuiRect(0, 0, 0, 0), () -> label, action, enabled); }
    private static void line(FontRenderer f, String value, GuiRect card, int offsetX, int offsetY, int color) {
        int width = Math.max(1, card.getWidth() - offsetX - 9);
        f.drawStringWithShadow(f.trimStringToWidth(value == null ? "--" : value, width),
            card.getX() + offsetX, card.getY() + offsetY, color);
    }
    private static void lineRight(FontRenderer f, String value, GuiRect card, int inset, int offsetY, int color) {
        String display = value == null ? "--" : value;
        int maxWidth = Math.max(1, card.getWidth() / 3);
        display = f.trimStringToWidth(display, maxWidth);
        f.drawStringWithShadow(display, card.getRight() - inset - f.getStringWidth(display), card.getY() + offsetY, color);
    }
}
