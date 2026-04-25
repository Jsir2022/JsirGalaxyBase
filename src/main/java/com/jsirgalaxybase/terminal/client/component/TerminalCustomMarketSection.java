package com.jsirgalaxybase.terminal.client.component;

import java.util.List;
import java.util.function.Supplier;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.framework.VerticalScrollPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;

public final class TerminalCustomMarketSection extends PanelContainer {

    public interface ActionHandler {
        void openMarketOverview();
        void selectListing(String scope, String listingId);
        void openBuyConfirm();
        void openCancelConfirm();
        void openClaimConfirm();
    }

    private final TerminalPanelFactory panels;
    private final TerminalCustomMarketSectionModel model;
    private final TerminalCustomMarketSectionState state;
    private final ActionHandler actionHandler;
    private final TexturedCanvasPanel browserCard;
    private final TexturedCanvasPanel detailCard;
    private final TexturedCanvasPanel actionCard;
    private final VerticalScrollPanel browserScroll;
    private final VerticalScrollPanel detailScroll;
    private final ButtonPanel backButton;
    private final ButtonPanel buyButton;
    private final ButtonPanel cancelButton;
    private final ButtonPanel claimButton;

    public TerminalCustomMarketSection(TerminalPanelFactory panels, TerminalCustomMarketSectionModel model,
        TerminalCustomMarketSectionState state, ActionHandler actionHandler) {
        this.panels = panels;
        this.model = model == null ? TerminalCustomMarketSectionModel.placeholder() : model;
        this.state = state == null ? new TerminalCustomMarketSectionState() : state;
        this.actionHandler = actionHandler;
        browserCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        detailCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        actionCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        browserScroll = panels.createScrollPanel(new GuiRect(0, 0, 0, 0), 4, 4);
        detailScroll = panels.createScrollPanel(new GuiRect(0, 0, 0, 0), 4, 4);
        addChild(browserCard);
        addChild(detailCard);
        addChild(actionCard);
        browserCard.addChild(label("挂牌浏览 / " + this.model.getScopeLabel(), true));
        browserCard.addChild(label(() -> this.model.getBrowserHint(), false));
        browserCard.addChild(browserScroll);
        addListingGroup("active", "全部挂牌", this.model.getActiveListingIds(), this.model.getActiveListingLines());
        addListingGroup("selling", "我的出售", this.model.getSellingListingIds(), this.model.getSellingListingLines());
        addListingGroup("pending", "我的待处理", this.model.getPendingListingIds(), this.model.getPendingListingLines());
        backButton = panels.createButton(new GuiRect(0, 0, 0, 0), () -> "返回总入口", () -> {
            if (actionHandler != null) actionHandler.openMarketOverview();
        }, null);
        browserCard.addChild(backButton);

        detailCard.addChild(label("listing 摘要与资产", true));
        detailCard.addChild(detailScroll);
        addDetailLine("标题: " + this.model.getSelectedTitle());
        addDetailLine("价格: " + this.model.getSelectedPrice());
        addDetailLine("状态: " + this.model.getSelectedStatus());
        addDetailLine("交易方: " + this.model.getSelectedCounterparty());
        addDetailLine("物品: " + this.model.getSelectedItemIdentity());
        addDetailLine("成交: " + this.model.getSelectedTradeSummary());
        addDetailLine("动作: " + this.model.getSelectedActionHint());

        actionCard.addChild(label("玩家动作与反馈", true));
        actionCard.addChild(label(() -> this.model.getActionFeedback().getTitle() + " / " + this.model.getActionFeedback().getBody(), false));
        buyButton = button("购买确认", () -> actionHandler.openBuyConfirm(), () -> Boolean.valueOf(this.model.isCanBuy()));
        cancelButton = button("下架确认", () -> actionHandler.openCancelConfirm(), () -> Boolean.valueOf(this.model.isCanCancel()));
        claimButton = button("领取确认", () -> actionHandler.openClaimConfirm(), () -> Boolean.valueOf(this.model.isCanClaim()));
        actionCard.addChild(buyButton);
        actionCard.addChild(cancelButton);
        actionCard.addChild(claimButton);
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        int gap = 6;
        int leftWidth = Math.max(210, Math.min(300, (int) (bounds.getWidth() * 0.34F)));
        int actionHeight = Math.max(86, Math.min(110, bounds.getHeight() / 3));
        browserCard.setBounds(new GuiRect(bounds.getX(), bounds.getY(), leftWidth, bounds.getHeight()));
        detailCard.setBounds(new GuiRect(bounds.getX() + leftWidth + gap, bounds.getY(), bounds.getWidth() - leftWidth - gap,
            bounds.getHeight() - actionHeight - gap));
        actionCard.setBounds(new GuiRect(detailCard.getBounds().getX(), detailCard.getBounds().getBottom() + gap,
            detailCard.getBounds().getWidth(), actionHeight));
        layoutCard(browserCard);
        browserScroll.setBounds(new GuiRect(browserCard.getBounds().getX() + 6, browserCard.getBounds().getY() + 34,
            browserCard.getBounds().getWidth() - 12, browserCard.getBounds().getHeight() - 62));
        backButton.setBounds(new GuiRect(browserCard.getBounds().getRight() - 104, browserCard.getBounds().getBottom() - 24, 92, 18));
        layoutCard(detailCard);
        detailScroll.setBounds(new GuiRect(detailCard.getBounds().getX() + 6, detailCard.getBounds().getY() + 24,
            detailCard.getBounds().getWidth() - 12, detailCard.getBounds().getHeight() - 30));
        layoutCard(actionCard);
        int buttonY = actionCard.getBounds().getBottom() - 24;
        buyButton.setBounds(new GuiRect(actionCard.getBounds().getX() + 8, buttonY, 86, 18));
        cancelButton.setBounds(new GuiRect(actionCard.getBounds().getX() + 100, buttonY, 86, 18));
        claimButton.setBounds(new GuiRect(actionCard.getBounds().getX() + 192, buttonY, 86, 18));
    }

    private void addListingGroup(final String scope, String title, List<String> ids, List<String> lines) {
        browserScroll.addScrollableChild(label(title, true), 18);
        int count = Math.max(ids.size(), lines.size());
        for (int i = 0; i < count; i++) {
            final String id = i < ids.size() ? ids.get(i) : "";
            final String line = i < lines.size() ? lines.get(i) : id;
            browserScroll.addScrollableChild(button(line, () -> {
                state.setSelectedScope(scope);
                state.setSelectedListingId(id);
                if (actionHandler != null) actionHandler.selectListing(scope, id);
            }, () -> Boolean.valueOf(id != null && !id.trim().isEmpty())), 20);
        }
    }

    private void addDetailLine(String value) {
        detailScroll.addScrollableChild(label(value, false), 18);
    }

    private ButtonPanel button(final String text, final Runnable action, final Supplier<Boolean> enabled) {
        return panels.createButton(new GuiRect(0, 0, 0, 0), () -> text, action, enabled);
    }

    private LabelPanel label(final String text, boolean primary) {
        return label(() -> text, primary);
    }

    private LabelPanel label(Supplier<String> text, boolean primary) {
        return panels.createLabel(new GuiRect(0, 0, 0, 0), text, primary ? ThemeColorKey.TEXT_PRIMARY : ThemeColorKey.TEXT_SECONDARY, false);
    }

    private void layoutCard(PanelContainer card) {
        GuiRect bounds = card.getBounds();
        int row = 0;
        for (int i = 0; i < card.getChildren().size(); i++) {
            if (card.getChildren().get(i) instanceof VerticalScrollPanel || card.getChildren().get(i) instanceof ButtonPanel) {
                continue;
            }
            card.getChildren().get(i).setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + 8 + row * 12,
                bounds.getWidth() - 16, 12));
            row++;
        }
    }
}
