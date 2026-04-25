package com.jsirgalaxybase.terminal.client.component;

import java.util.List;
import java.util.function.Supplier;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel;

public final class TerminalBankSection extends PanelContainer {

    public interface ActionHandler {

        void openAccount();

        void openTransferConfirm();
    }

    private final TerminalPanelFactory panels;
    private final TerminalBankSectionModel model;
    private final TerminalBankSectionState state;
    private final ActionHandler actionHandler;
    private final LabelPanel titleLabel;
    private final LabelPanel leadLabel;
    private final TexturedCanvasPanel accountCard;
    private final TexturedCanvasPanel summaryCard;
    private final TexturedCanvasPanel transferCard;
    private final TexturedCanvasPanel feedbackCard;
    private final TexturedCanvasPanel ledgerCard;
    private final ButtonPanel openAccountButton;
    private final ButtonPanel transferConfirmButton;
    private final TerminalTextFieldPanel targetField;
    private final TerminalTextFieldPanel amountField;
    private final TerminalTextFieldPanel commentField;
    private final LabelPanel[] ledgerLabels;

    public TerminalBankSection(TerminalPanelFactory panels, TerminalBankSectionModel model,
        TerminalBankSectionState state, ActionHandler actionHandler) {
        this.panels = panels;
        this.model = model == null ? TerminalBankSectionModel.placeholder() : model;
        this.state = state == null ? new TerminalBankSectionState() : state;
        this.actionHandler = actionHandler;
        this.titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return "银行业务页 / 银河银行";
            }
        }, ThemeColorKey.TEXT_PRIMARY, false);
        this.leadLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return "开户、余额摘要、转账确认和 snapshot 回写都已迁入当前新壳。";
            }
        }, ThemeColorKey.TEXT_SECONDARY, false);
        this.accountCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        this.summaryCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        this.transferCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        this.feedbackCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        this.ledgerCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        this.openAccountButton = panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return TerminalBankSection.this.model.getAccountStatus().isOpened() ? "账户已存在" : "立即开户";
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (TerminalBankSection.this.actionHandler != null) {
                    TerminalBankSection.this.actionHandler.openAccount();
                }
            }
        }, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return Boolean.valueOf(TerminalBankSection.this.model.getAccountStatus().isOpenAllowed());
            }
        });
        this.transferConfirmButton = panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return "打开转账确认";
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (TerminalBankSection.this.actionHandler != null) {
                    TerminalBankSection.this.actionHandler.openTransferConfirm();
                }
            }
        }, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return Boolean.valueOf(TerminalBankSection.this.model.getTransferForm().isTransferEnabled()
                    && TerminalBankSection.this.state.hasCompleteTransferDraft());
            }
        });
        this.targetField = new TerminalTextFieldPanel(
            new Supplier<String>() {
                @Override
                public String get() {
                    return TerminalBankSection.this.state.getTargetPlayerName();
                }
            },
            value -> TerminalBankSection.this.state.setTargetPlayerName(value),
            new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.valueOf(TerminalBankSection.this.state.isFocused(TerminalBankSectionState.FocusField.TARGET));
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    TerminalBankSection.this.state.focus(TerminalBankSectionState.FocusField.TARGET);
                }
            },
            "收款玩家名",
            16,
            value -> Boolean.valueOf((value.charValue() >= 'A' && value.charValue() <= 'Z')
                || (value.charValue() >= 'a' && value.charValue() <= 'z')
                || (value.charValue() >= '0' && value.charValue() <= '9')
                || value.charValue() == '_'));
        this.amountField = new TerminalTextFieldPanel(
            new Supplier<String>() {
                @Override
                public String get() {
                    return TerminalBankSection.this.state.getAmountText();
                }
            },
            value -> TerminalBankSection.this.state.setAmountText(value),
            new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.valueOf(TerminalBankSection.this.state.isFocused(TerminalBankSectionState.FocusField.AMOUNT));
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    TerminalBankSection.this.state.focus(TerminalBankSectionState.FocusField.AMOUNT);
                }
            },
            "转账金额",
            18,
            value -> Boolean.valueOf(value.charValue() >= '0' && value.charValue() <= '9'));
        this.commentField = new TerminalTextFieldPanel(
            new Supplier<String>() {
                @Override
                public String get() {
                    return TerminalBankSection.this.state.getComment();
                }
            },
            value -> TerminalBankSection.this.state.setComment(value),
            new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.valueOf(TerminalBankSection.this.state.isFocused(TerminalBankSectionState.FocusField.COMMENT));
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    TerminalBankSection.this.state.focus(TerminalBankSectionState.FocusField.COMMENT);
                }
            },
            "备注说明",
            96,
            value -> Boolean.valueOf(!Character.isISOControl(value.charValue())));
        this.ledgerLabels = new LabelPanel[4];
        for (int i = 0; i < ledgerLabels.length; i++) {
            final int lineIndex = i;
            ledgerLabels[i] = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    List<String> lines = TerminalBankSection.this.model.getPlayerLedgerLines();
                    return lineIndex < lines.size() ? lines.get(lineIndex) : "";
                }
            }, ThemeColorKey.TEXT_SECONDARY, false);
        }

        addChild(titleLabel);
        addChild(leadLabel);
        addChild(accountCard);
        addChild(summaryCard);
        addChild(transferCard);
        addChild(feedbackCard);
        addChild(ledgerCard);

        configureAccountCard();
        configureSummaryCard();
        configureTransferCard();
        configureFeedbackCard();
        configureLedgerCard();
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        int leftWidth = Math.max(200, (bounds.getWidth() * 58) / 100);
        int rightWidth = bounds.getWidth() - leftWidth - 10;
        int topY = bounds.getY();
        titleLabel.setBounds(new GuiRect(bounds.getX(), topY, bounds.getWidth(), 12));
        leadLabel.setBounds(new GuiRect(bounds.getX(), topY + 14, bounds.getWidth(), 20));

        accountCard.setBounds(new GuiRect(bounds.getX(), topY + 40, leftWidth, 82));
        transferCard.setBounds(new GuiRect(bounds.getX(), topY + 128, leftWidth, 132));
        summaryCard.setBounds(new GuiRect(bounds.getX() + leftWidth + 10, topY + 40, rightWidth, 82));
        feedbackCard.setBounds(new GuiRect(bounds.getX() + leftWidth + 10, topY + 128, rightWidth, 64));
        ledgerCard.setBounds(new GuiRect(bounds.getX() + leftWidth + 10, topY + 198, rightWidth, 62));

        layoutAccountCard(accountCard.getBounds());
        layoutSummaryCard(summaryCard.getBounds());
        layoutTransferCard(transferCard.getBounds());
        layoutFeedbackCard(feedbackCard.getBounds());
        layoutLedgerCard(ledgerCard.getBounds());
    }

    private void configureAccountCard() {
        accountCard.addChild(createCardTitle("账户状态与开户"));
        accountCard.addChild(createCardValue(24, () -> "服务状态: " + model.getAccountStatus().getServiceState()));
        accountCard.addChild(createCardValue(36, () -> "账户状态: " + model.getAccountStatus().getAccountLabel()));
        accountCard.addChild(createCardValue(48, () -> "账户编号: " + model.getAccountStatus().getAccountNo()));
        accountCard.addChild(createCardValue(60, () -> "最近更新: " + model.getAccountStatus().getUpdatedAt()));
        accountCard.addChild(openAccountButton);
    }

    private void configureSummaryCard() {
        summaryCard.addChild(createCardTitle("余额与公开储备"));
        summaryCard.addChild(createCardValue(24, () -> "个人余额: " + model.getBalanceSummary().getPlayerBalance()));
        summaryCard.addChild(createCardValue(36, () -> "公开储备: " + model.getBalanceSummary().getExchangeBalance()));
        summaryCard.addChild(createCardValue(48, () -> "公开状态: " + model.getBalanceSummary().getExchangeStatus()));
        summaryCard.addChild(createCardValue(60, () -> "转账说明: " + model.getBalanceSummary().getTransferHint()));
    }

    private void configureTransferCard() {
        transferCard.addChild(createCardTitle("转账表单与确认"));
        transferCard.addChild(createMutedLabel(() -> "收款玩家", 24));
        transferCard.addChild(targetField);
        transferCard.addChild(createMutedLabel(() -> "转账金额", 54));
        transferCard.addChild(amountField);
        transferCard.addChild(createMutedLabel(() -> "备注说明", 84));
        transferCard.addChild(commentField);
        transferCard.addChild(createCardValue(108, () -> "当前门禁: "
            + (model.getTransferForm().isTransferEnabled() ? (state.hasCompleteTransferDraft() ? "可确认提交" : "等待完整表单") : "当前不可转账")));
        transferCard.addChild(transferConfirmButton);
    }

    private void configureFeedbackCard() {
        feedbackCard.addChild(createCardTitle("最近动作反馈"));
        feedbackCard.addChild(createCardValue(24, () -> model.getActionFeedback().getTitle()));
        feedbackCard.addChild(createMutedBody(() -> model.getActionFeedback().getBody(), 36));
    }

    private void configureLedgerCard() {
        ledgerCard.addChild(createCardTitle("近期个人流水"));
        for (LabelPanel ledgerLabel : ledgerLabels) {
            ledgerCard.addChild(ledgerLabel);
        }
    }

    private void layoutAccountCard(GuiRect bounds) {
        layoutCardChildren(bounds, accountCard, openAccountButton, null, null, null);
        openAccountButton.setBounds(new GuiRect(bounds.getRight() - 104, bounds.getY() + bounds.getHeight() - 24, 92, 18));
    }

    private void layoutSummaryCard(GuiRect bounds) {
        layoutCardChildren(bounds, summaryCard, null, null, null, null);
    }

    private void layoutTransferCard(GuiRect bounds) {
        layoutCardChildren(bounds, transferCard, transferConfirmButton, targetField, amountField, commentField);
        targetField.setBounds(new GuiRect(bounds.getX() + 72, bounds.getY() + 22, bounds.getWidth() - 84, 18));
        amountField.setBounds(new GuiRect(bounds.getX() + 72, bounds.getY() + 52, bounds.getWidth() - 84, 18));
        commentField.setBounds(new GuiRect(bounds.getX() + 72, bounds.getY() + 82, bounds.getWidth() - 84, 18));
        transferConfirmButton.setBounds(new GuiRect(bounds.getRight() - 116, bounds.getBottom() - 24, 104, 18));
    }

    private void layoutFeedbackCard(GuiRect bounds) {
        layoutCardChildren(bounds, feedbackCard, null, null, null, null);
    }

    private void layoutLedgerCard(GuiRect bounds) {
        layoutCardChildren(bounds, ledgerCard, null, null, null, null);
        for (int i = 0; i < ledgerLabels.length; i++) {
            ledgerLabels[i].setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + 22 + i * 10, bounds.getWidth() - 16, 10));
        }
    }

    private void layoutCardChildren(GuiRect bounds, TexturedCanvasPanel card, ButtonPanel button,
        TerminalTextFieldPanel firstField, TerminalTextFieldPanel secondField, TerminalTextFieldPanel thirdField) {
        if (card.getChildren().isEmpty()) {
            return;
        }
        card.getChildren().get(0).setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + 8, bounds.getWidth() - 16, 12));
        for (int i = 1; i < card.getChildren().size(); i++) {
            if (card.getChildren().get(i) == button || card.getChildren().get(i) == firstField || card.getChildren().get(i) == secondField
                || card.getChildren().get(i) == thirdField) {
                continue;
            }
            int offset = 12 + i * 12;
            card.getChildren().get(i).setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + offset, bounds.getWidth() - 16, 12));
        }
    }

    private LabelPanel createCardTitle(final String value) {
        return panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return value;
            }
        }, ThemeColorKey.TEXT_PRIMARY, false);
    }

    private LabelPanel createCardValue(final int offsetY, final Supplier<String> supplier) {
        return panels.createLabel(new GuiRect(0, 0, 0, 0), supplier, ThemeColorKey.TEXT_SECONDARY, false);
    }

    private LabelPanel createMutedLabel(final Supplier<String> supplier, final int offsetY) {
        return panels.createLabel(new GuiRect(0, 0, 0, 0), supplier, ThemeColorKey.TEXT_SECONDARY, false);
    }

    private LabelPanel createMutedBody(final Supplier<String> supplier, final int offsetY) {
        return panels.createLabel(new GuiRect(0, 0, 0, 0), supplier, ThemeColorKey.TEXT_SECONDARY, false);
    }
}