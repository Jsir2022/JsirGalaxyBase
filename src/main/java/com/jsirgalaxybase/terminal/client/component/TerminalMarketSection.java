package com.jsirgalaxybase.terminal.client.component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.LabelPanel;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.framework.VerticalScrollPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

public final class TerminalMarketSection extends PanelContainer {

    public interface ActionHandler {

        void openMarketOverview();

        void openStandardizedMarket();

        void openCustomMarket();

        void openExchangeMarket();

        void selectProduct(String productKey);

        void openLimitBuyConfirm();

        void openClaimConfirm(String custodyId);
    }

    private final TerminalPanelFactory panels;
    private final TerminalMarketSectionModel model;
    private final TerminalMarketSectionState state;
    private final ActionHandler actionHandler;
    private final LabelPanel titleLabel;
    private final LabelPanel leadLabel;

    private TexturedCanvasPanel overviewSummaryCard;
    private TexturedCanvasPanel overviewEntryCard;
    private TexturedCanvasPanel overviewBoundaryCard;
    private ButtonPanel overviewOpenStandardizedButton;
    private ButtonPanel overviewOpenCustomButton;
    private ButtonPanel overviewOpenExchangeButton;
    private VerticalScrollPanel overviewBoundaryScroll;

    private TexturedCanvasPanel browserCard;
    private VerticalScrollPanel browserScroll;
    private TexturedCanvasPanel detailCard;
    private VerticalScrollPanel detailScroll;
    private TexturedCanvasPanel buyCard;
    private ButtonPanel backToOverviewButton;
    private ButtonPanel buyConfirmButton;
    private TerminalTextFieldPanel priceField;
    private TerminalTextFieldPanel quantityField;
    private final List<ButtonPanel> productButtons = new ArrayList<ButtonPanel>();

    public TerminalMarketSection(TerminalPanelFactory panels, TerminalMarketSectionModel model,
        TerminalMarketSectionState state, ActionHandler actionHandler) {
        this.panels = panels;
        this.model = model == null ? TerminalMarketSectionModel.placeholder("market") : model;
        this.state = state == null ? new TerminalMarketSectionState() : state;
        this.actionHandler = actionHandler;
        this.titleLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return TerminalMarketSection.this.model.isOverviewRoute()
                    ? "市场总入口 / 共享摘要"
                    : "标准商品市场 / 商品优先";
            }
        }, ThemeColorKey.TEXT_PRIMARY, false);
        this.leadLabel = panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return TerminalMarketSection.this.model.isOverviewRoute()
                    ? "MARKET 根页只保留共享摘要和入口卡，不再回退到混合交易巨石页。"
                    : "统一目录、订单簿、CLAIMABLE 与真实动作现在直接挂到新壳 section 上。";
            }
        }, ThemeColorKey.TEXT_SECONDARY, false);
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
        titleLabel.setBounds(new GuiRect(bounds.getX(), bounds.getY(), bounds.getWidth(), 12));
        leadLabel.setBounds(new GuiRect(bounds.getX(), bounds.getY() + 13, bounds.getWidth(), 16));
        if (model.isOverviewRoute()) {
            layoutOverview(bounds);
            return;
        }
        layoutStandardized(bounds);
    }

    private void configureOverviewRoute() {
        overviewSummaryCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        overviewEntryCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        overviewBoundaryCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        overviewBoundaryScroll = panels.createScrollPanel(new GuiRect(0, 0, 0, 0), 4, 4);
        addChild(overviewSummaryCard);
        addChild(overviewEntryCard);
        addChild(overviewBoundaryCard);
        overviewBoundaryCard.addChild(overviewBoundaryScroll);

        configureOverviewSummaryCard();
        configureOverviewEntryCard();
        configureOverviewBoundaryCard();
    }

    private void configureOverviewSummaryCard() {
        overviewSummaryCard.addChild(createCardTitle("共享运行态"));
        for (String line : TerminalMarketSectionContent.buildOverviewSummaryLines(model)) {
            overviewSummaryCard.addChild(createCardValue(line));
        }
    }

    private void configureOverviewEntryCard() {
        overviewEntryCard.addChild(createCardTitle("标准商品市场入口"));
        for (String line : TerminalMarketSectionContent.buildOverviewEntryLines(model)) {
            overviewEntryCard.addChild(createCardValue(line));
        }
        overviewOpenStandardizedButton = panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return "进入标准商品市场";
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (actionHandler != null) {
                    actionHandler.openStandardizedMarket();
                }
            }
        }, null);
        overviewEntryCard.addChild(overviewOpenStandardizedButton);
        overviewOpenCustomButton = panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return "进入定制商品市场";
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (actionHandler != null) {
                    actionHandler.openCustomMarket();
                }
            }
        }, null);
        overviewEntryCard.addChild(overviewOpenCustomButton);
        overviewOpenExchangeButton = panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return "进入汇率市场";
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (actionHandler != null) {
                    actionHandler.openExchangeMarket();
                }
            }
        }, null);
        overviewEntryCard.addChild(overviewOpenExchangeButton);
    }

    private void configureOverviewBoundaryCard() {
        overviewBoundaryCard.addChild(createCardTitle("当前迁移边界"));
        for (String line : TerminalMarketSectionContent.buildOverviewBoundaryLines()) {
            overviewBoundaryScroll.addScrollableChild(createCardValue(line), 18);
        }
    }

    private void layoutOverview(GuiRect bounds) {
        int contentY = bounds.getY() + 36;
        int gap = 6;
        int availableHeight = Math.max(120, bounds.getHeight() - 36);
        int topHeight = Math.max(96, Math.min(140, (availableHeight - gap) / 3));
        int leftWidth = (bounds.getWidth() - gap) / 2;
        overviewSummaryCard.setBounds(new GuiRect(bounds.getX(), contentY, leftWidth, topHeight));
        overviewEntryCard.setBounds(new GuiRect(bounds.getX() + leftWidth + gap, contentY, bounds.getWidth() - leftWidth - gap, topHeight));
        overviewBoundaryCard.setBounds(new GuiRect(bounds.getX(), contentY + topHeight + gap, bounds.getWidth(), availableHeight - topHeight - gap));
        overviewBoundaryScroll.setBounds(new GuiRect(
            overviewBoundaryCard.getBounds().getX() + 6,
            overviewBoundaryCard.getBounds().getY() + 24,
            overviewBoundaryCard.getBounds().getWidth() - 12,
            overviewBoundaryCard.getBounds().getHeight() - 30));
        layoutSimpleCard(overviewSummaryCard, null);
        layoutSimpleCard(overviewEntryCard, overviewOpenStandardizedButton);
        overviewOpenStandardizedButton.setBounds(new GuiRect(
            overviewEntryCard.getBounds().getRight() - 124,
            overviewEntryCard.getBounds().getBottom() - 66,
            112,
            18));
        overviewOpenCustomButton.setBounds(new GuiRect(
            overviewEntryCard.getBounds().getRight() - 124,
            overviewEntryCard.getBounds().getBottom() - 45,
            112,
            18));
        overviewOpenExchangeButton.setBounds(new GuiRect(
            overviewEntryCard.getBounds().getRight() - 124,
            overviewEntryCard.getBounds().getBottom() - 24,
            112,
            18));
    }

    private void configureStandardizedRoute() {
        browserCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        browserScroll = panels.createScrollPanel(new GuiRect(0, 0, 0, 0), 4, 4);
        detailCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        detailScroll = panels.createScrollPanel(new GuiRect(0, 0, 0, 0), 4, 4);
        buyCard = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        addChild(browserCard);
        addChild(detailCard);
        addChild(buyCard);
        browserCard.addChild(browserScroll);
        detailCard.addChild(detailScroll);

        configureBrowserCard();
        configureBuyCard();
        configureDetailCard();
    }

    private void configureBrowserCard() {
        browserCard.addChild(createCardTitle("商品浏览"));
        browserCard.addChild(createCardValue(() -> "服务状态: " + model.getServiceState()));
        browserCard.addChild(createCardValue(() -> "对象提示: " + model.getBrowserHint()));
        browserCard.addChild(createCardValue(() -> "当前商品: " + model.getSelectedProductName()));
        backToOverviewButton = panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return "返回总入口";
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (actionHandler != null) {
                    actionHandler.openMarketOverview();
                }
            }
        }, null);
        browserCard.addChild(backToOverviewButton);

        for (final TerminalMarketSectionContent.ProductEntry entry : TerminalMarketSectionContent.buildProductEntries(model)) {
            ButtonPanel button = panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return entry.isSelected() ? "当前: " + entry.getLabel() : entry.getLabel();
                }
            }, new Runnable() {
                @Override
                public void run() {
                    if (actionHandler != null && !entry.getKey().isEmpty()) {
                        actionHandler.selectProduct(entry.getKey());
                    }
                }
            }, new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.valueOf(!entry.getKey().isEmpty());
                }
            });
            productButtons.add(button);
            browserScroll.addScrollableChild(button, 20);
        }
    }

    private void configureDetailCard() {
        detailCard.addChild(createCardTitle("标准市场详情与资产"));
        addDetailSection("当前交易焦点", TerminalMarketSectionContent.buildMetricsLines(model), false);
        addDetailSection("盘口与个人订单", TerminalMarketSectionContent.buildBookLines(model), false);
        addDetailSection("CLAIMABLE 摘要", listOf("当前可提取数量: " + model.getClaimableQuantity()), false);
        for (final TerminalMarketSectionContent.ClaimEntry entry : TerminalMarketSectionContent.buildClaimEntries(model)) {
            ButtonPanel button = panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override
                public String get() {
                    return "提取: " + entry.getDetail();
                }
            }, new Runnable() {
                @Override
                public void run() {
                    if (actionHandler != null && !entry.getCustodyId().isEmpty()) {
                        state.setPendingClaimCustodyId(entry.getCustodyId());
                        actionHandler.openClaimConfirm(entry.getCustodyId());
                    }
                }
            }, new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.valueOf(!entry.getCustodyId().isEmpty());
                }
            });
            detailScroll.addScrollableChild(button, 20);
        }
        addDetailSection("规则与边界", TerminalMarketSectionContent.buildRuleLines(model), false);
    }

    private void configureBuyCard() {
        buyCard.addChild(createCardTitle("限价买单"));
        buyCard.addChild(createCardValue(() -> "目标商品: " + model.getSelectedProductName()));
        buyCard.addChild(createCardValue(() -> "资金冻结: 下单后将走真实标准化买单链路。"));
        priceField = new TerminalTextFieldPanel(
            new Supplier<String>() {
                @Override
                public String get() {
                    return state.getLimitBuyPriceText();
                }
            },
            value -> state.setLimitBuyPriceText(value),
            new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.valueOf(state.isFocused(TerminalMarketSectionState.FocusField.PRICE));
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    state.focus(TerminalMarketSectionState.FocusField.PRICE);
                }
            },
            "买单价格",
            18,
            value -> Boolean.valueOf(value.charValue() >= '0' && value.charValue() <= '9'));
        quantityField = new TerminalTextFieldPanel(
            new Supplier<String>() {
                @Override
                public String get() {
                    return state.getLimitBuyQuantityText();
                }
            },
            value -> state.setLimitBuyQuantityText(value),
            new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    return Boolean.valueOf(state.isFocused(TerminalMarketSectionState.FocusField.QUANTITY));
                }
            },
            new Runnable() {
                @Override
                public void run() {
                    state.focus(TerminalMarketSectionState.FocusField.QUANTITY);
                }
            },
            "买入数量",
            18,
            value -> Boolean.valueOf(value.charValue() >= '0' && value.charValue() <= '9'));
        buyCard.addChild(priceField);
        buyCard.addChild(quantityField);
        buyConfirmButton = panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return "打开买单确认";
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (actionHandler != null) {
                    actionHandler.openLimitBuyConfirm();
                }
            }
        }, new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                return Boolean.valueOf(state.hasCompleteLimitBuyDraft());
            }
        });
        buyCard.addChild(buyConfirmButton);
        buyCard.addChild(createCardValue(() -> model.getActionFeedback().getTitle() + " / " + model.getActionFeedback().getBody()));
    }

    private void layoutStandardized(GuiRect bounds) {
        int contentY = bounds.getY() + 36;
        int gap = 6;
        int availableHeight = Math.max(180, bounds.getHeight() - 36);
        int leftWidth = Math.max(190, Math.min(280, (int) (bounds.getWidth() * 0.29F)));
        int rightWidth = bounds.getWidth() - leftWidth - gap;
        int buyHeight = Math.max(104, Math.min(124, availableHeight / 3));

        browserCard.setBounds(new GuiRect(bounds.getX(), contentY, leftWidth, availableHeight));
        browserScroll.setBounds(new GuiRect(
            browserCard.getBounds().getX() + 6,
            browserCard.getBounds().getY() + 46,
            browserCard.getBounds().getWidth() - 12,
            browserCard.getBounds().getHeight() - 52));
        detailCard.setBounds(new GuiRect(bounds.getX() + leftWidth + gap, contentY + buyHeight + gap, rightWidth, availableHeight - buyHeight - gap));
        detailScroll.setBounds(new GuiRect(
            detailCard.getBounds().getX() + 6,
            detailCard.getBounds().getY() + 22,
            detailCard.getBounds().getWidth() - 12,
            detailCard.getBounds().getHeight() - 26));
        buyCard.setBounds(new GuiRect(bounds.getX() + leftWidth + gap, contentY, rightWidth, buyHeight));

        layoutSimpleCard(browserCard, backToOverviewButton);
        backToOverviewButton.setBounds(new GuiRect(browserCard.getBounds().getRight() - 104, browserCard.getBounds().getY() + 8, 92, 18));

        layoutSimpleCard(buyCard, buyConfirmButton);
        priceField.setBounds(new GuiRect(buyCard.getBounds().getX() + 8, buyCard.getBounds().getY() + 44,
            (buyCard.getBounds().getWidth() - 28) / 2, 18));
        quantityField.setBounds(new GuiRect(buyCard.getBounds().getX() + buyCard.getBounds().getWidth() / 2 + 6, buyCard.getBounds().getY() + 44,
            (buyCard.getBounds().getWidth() - 28) / 2, 18));
        buyConfirmButton.setBounds(new GuiRect(buyCard.getBounds().getRight() - 120, buyCard.getBounds().getBottom() - 24, 108, 18));
        buyCard.getChildren().get(buyCard.getChildren().size() - 1).setBounds(new GuiRect(buyCard.getBounds().getX() + 8,
            buyCard.getBounds().getBottom() - 22, buyCard.getBounds().getWidth() - 124, 10));
    }

    private void layoutSimpleCard(TexturedCanvasPanel card, ButtonPanel button) {
        if (card == null || card.getChildren().isEmpty()) {
            return;
        }
        GuiRect bounds = card.getBounds();
        card.getChildren().get(0).setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + 8, bounds.getWidth() - 16, 12));
        int valueIndex = 0;
        for (int i = 1; i < card.getChildren().size(); i++) {
            if (card.getChildren().get(i) == button
                || card.getChildren().get(i) instanceof TerminalTextFieldPanel
                || card.getChildren().get(i) instanceof VerticalScrollPanel) {
                continue;
            }
            card.getChildren().get(i).setBounds(new GuiRect(bounds.getX() + 8, bounds.getY() + 22 + valueIndex * 10,
                bounds.getWidth() - 16, 10));
            valueIndex++;
        }
    }

    private void addDetailSection(String title, List<String> lines, boolean primary) {
        detailScroll.addScrollableChild(createScrollSectionTitle(title), primary ? 18 : 16);
        for (String line : lines) {
            detailScroll.addScrollableChild(createCardValue(line), 18);
        }
    }

    private LabelPanel createScrollSectionTitle(final String value) {
        return panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return value;
            }
        }, ThemeColorKey.TEXT_PRIMARY, false);
    }

    private LabelPanel createCardTitle(final String value) {
        return panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override
            public String get() {
                return value;
            }
        }, ThemeColorKey.TEXT_PRIMARY, false);
    }

    private LabelPanel createCardValue(final String value) {
        return createCardValue(new Supplier<String>() {
            @Override
            public String get() {
                return value;
            }
        });
    }

    private LabelPanel createCardValue(final Supplier<String> supplier) {
        return panels.createLabel(new GuiRect(0, 0, 0, 0), supplier, ThemeColorKey.TEXT_SECONDARY, false);
    }

    private List<String> listOf(String value) {
        List<String> values = new ArrayList<String>(1);
        values.add(value == null ? "" : value);
        return values;
    }
}
