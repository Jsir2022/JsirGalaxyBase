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
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;

public final class TerminalExchangeMarketSection extends PanelContainer {

    public interface ActionHandler {
        void openMarketOverview();
        void selectTarget(String targetCode);
        void refreshQuote();
        void openExchangeConfirm();
    }

    private final TerminalPanelFactory panels;
    private final TerminalExchangeMarketSectionModel model;
    private final TerminalExchangeMarketSectionState state;
    private final ActionHandler actionHandler;
    private final TexturedCanvasPanel browserCard;
    private final TexturedCanvasPanel quoteCard;
    private final TexturedCanvasPanel actionCard;
    private final VerticalScrollPanel quoteScroll;
    private final ButtonPanel backButton;
    private final ButtonPanel refreshButton;
    private final ButtonPanel confirmButton;

    public TerminalExchangeMarketSection(TerminalPanelFactory panels, TerminalExchangeMarketSectionModel model,
        TerminalExchangeMarketSectionState state, ActionHandler actionHandler) {
        this.panels = panels;
        this.model = model == null ? TerminalExchangeMarketSectionModel.placeholder() : model;
        this.state = state == null ? new TerminalExchangeMarketSectionState() : state;
        this.actionHandler = actionHandler;
        browserCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        quoteCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        actionCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        quoteScroll = panels.createScrollPanel(new GuiRect(0, 0, 0, 0), 4, 4);
        addChild(browserCard);
        addChild(quoteCard);
        addChild(actionCard);

        browserCard.addChild(label("兑换标的", true));
        browserCard.addChild(label(() -> this.model.getBrowserHint(), false));
        addTargetButtons(this.model.getTargetCodes(), this.model.getTargetLabels());
        backButton = panels.createButton(new GuiRect(0, 0, 0, 0), () -> "返回总入口", () -> {
            if (actionHandler != null) actionHandler.openMarketOverview();
        }, null);
        browserCard.addChild(backButton);

        quoteCard.addChild(label("quote / pair / rule", true));
        quoteCard.addChild(quoteScroll);
        addQuoteLine("标的: " + this.model.getSelectedTargetTitle());
        addQuoteLine("摘要: " + this.model.getSelectedTargetSummary());
        addQuoteLine("手持: " + this.model.getHeldSummary());
        addQuoteLine("输入物品: " + this.model.getInputRegistryName());
        addQuoteLine("pair: " + this.model.getPairCode() + " / " + this.model.getInputAssetCode() + " -> "
            + this.model.getOutputAssetCode());
        addQuoteLine("rule: " + this.model.getRuleVersion() + " / " + this.model.getLimitStatus()
            + " / reason=" + this.model.getReasonCode());
        addQuoteLine("数量: input=" + this.model.getInputQuantity() + " / face=" + this.model.getNominalFaceValue());
        addQuoteLine("报价: effective=" + this.model.getEffectiveExchangeValue() + " / contribution="
            + this.model.getContributionValue());
        addQuoteLine("折扣: " + this.model.getDiscountStatus() + " / rate=" + this.model.getRateDisplay());
        addQuoteLine("说明: " + this.model.getNotes());
        addQuoteLine("执行: " + this.model.getExecutionHint());

        actionCard.addChild(label("报价动作与反馈", true));
        actionCard.addChild(label(() -> this.model.getActionFeedback().getTitle() + " / " + this.model.getActionFeedback().getBody(), false));
        refreshButton = panels.createButton(new GuiRect(0, 0, 0, 0), () -> "刷新报价", () -> {
            if (actionHandler != null) actionHandler.refreshQuote();
        }, () -> Boolean.valueOf(state.hasSelectedTarget()));
        confirmButton = panels.createButton(new GuiRect(0, 0, 0, 0), () -> "确认兑换", () -> {
            if (actionHandler != null) actionHandler.openExchangeConfirm();
        }, () -> Boolean.valueOf(this.model.isExecutable() && state.hasSelectedTarget()));
        actionCard.addChild(refreshButton);
        actionCard.addChild(confirmButton);
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        int gap = 6;
        int leftWidth = Math.max(210, Math.min(292, (int) (bounds.getWidth() * 0.32F)));
        int actionHeight = Math.max(78, Math.min(104, bounds.getHeight() / 3));
        browserCard.setBounds(new GuiRect(bounds.getX(), bounds.getY(), leftWidth, bounds.getHeight()));
        quoteCard.setBounds(new GuiRect(bounds.getX() + leftWidth + gap, bounds.getY(), bounds.getWidth() - leftWidth - gap,
            bounds.getHeight() - actionHeight - gap));
        actionCard.setBounds(new GuiRect(quoteCard.getBounds().getX(), quoteCard.getBounds().getBottom() + gap,
            quoteCard.getBounds().getWidth(), actionHeight));
        layoutCard(browserCard);
        backButton.setBounds(new GuiRect(browserCard.getBounds().getRight() - 104, browserCard.getBounds().getBottom() - 24, 92, 18));
        layoutCard(quoteCard);
        quoteScroll.setBounds(new GuiRect(quoteCard.getBounds().getX() + 6, quoteCard.getBounds().getY() + 24,
            quoteCard.getBounds().getWidth() - 12, quoteCard.getBounds().getHeight() - 30));
        layoutCard(actionCard);
        refreshButton.setBounds(new GuiRect(actionCard.getBounds().getRight() - 226, actionCard.getBounds().getBottom() - 24, 104, 18));
        confirmButton.setBounds(new GuiRect(actionCard.getBounds().getRight() - 114, actionCard.getBounds().getBottom() - 24, 104, 18));
    }

    private void addTargetButtons(List<String> codes, List<String> labels) {
        int count = Math.max(codes.size(), labels.size());
        for (int i = 0; i < count; i++) {
            final String code = i < codes.size() ? codes.get(i) : "";
            final String label = i < labels.size() ? labels.get(i) : code;
            ButtonPanel button = panels.createButton(new GuiRect(0, 0, 0, 0),
                () -> code.equals(model.getSelectedTargetCode()) ? "当前: " + label : label,
                () -> {
                    state.setSelectedTargetCode(code);
                    if (actionHandler != null) actionHandler.selectTarget(code);
                },
                () -> Boolean.valueOf(code != null && !code.trim().isEmpty()));
            browserCard.addChild(button);
        }
    }

    private void addQuoteLine(String value) {
        quoteScroll.addScrollableChild(label(value, false), 18);
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
        int buttonY = bounds.getY() + 40;
        for (int i = 0; i < card.getChildren().size(); i++) {
            if (card.getChildren().get(i) instanceof ButtonPanel && card.getChildren().get(i) != backButton
                && card.getChildren().get(i) != refreshButton && card.getChildren().get(i) != confirmButton) {
                card.getChildren().get(i).setBounds(new GuiRect(bounds.getX() + 8, buttonY, bounds.getWidth() - 16, 20));
                buttonY += 24;
            }
        }
    }
}
