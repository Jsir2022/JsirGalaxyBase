package com.jsirgalaxybase.terminal.client.ui2;

import java.util.List;

import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.TerminalMarketBrowseEntry;
import com.jsirgalaxybase.terminal.client.component.TerminalCustomMarketSectionState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.ui2.component.ComponentUiDocument;
import com.jsirgalaxybase.ui2.component.StandardWidgets;
import com.jsirgalaxybase.ui2.component.UiActionHandler;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.InputResult;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiInputHandler;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;

/** UI2 front end for the server-authoritative single-item custom market. */
public final class TerminalCustomMarketDocument extends ComponentUiDocument implements TerminalPageDocument {
    private final TerminalCustomMarketSectionState state;
    private final TerminalAppShell.Actions shellActions;
    private final TerminalStandardMarketDocument.Actions actions;
    private final Runnable invalidation;
    private TerminalHomeScreenModel model;
    private Pending pending;
    private boolean helpOpen;
    private int width;
    private int height;

    public TerminalCustomMarketDocument(TerminalCustomMarketSectionState state, TerminalHomeScreenModel model,
        TerminalAppShell.Actions shellActions, TerminalStandardMarketDocument.Actions actions, Runnable invalidation) {
        super(StandardWidgets.create());
        this.state = state;
        this.model = model;
        this.shellActions = shellActions;
        this.actions = actions;
        this.invalidation = invalidation;
        state.applyModel(custom());
    }

    @Override
    public void update(TerminalHomeScreenModel value) {
        model = value;
        TerminalCustomMarketSectionModel next = custom();
        if (state.acceptsModel(next)) state.applyModel(next);
    }

    @Override
    public void openHelp() {
        helpOpen = true;
        invalidation.run();
    }

    @Override
    public boolean refresh() {
        send(TerminalActionType.MARKET_CUSTOM_REFRESH);
        return true;
    }

    @Override
    public UiElement build(UiContext context) {
        final TerminalCustomMarketSectionModel custom = custom();
        UiElement toolbar = UiElement.type("Row").key("custom-toolbar")
            .child(tab("custom-active", "在售", "active"))
            .child(tab("custom-selling", "我的挂牌", "selling"))
            .child(tab("custom-pending", "待领取", "pending"))
            .child(field("custom-query", "搜索挂牌", new Binding() {
                @Override public String read() { return state.getBrowserQuery(); }
                @Override public void write(String value) { state.setBrowserQuery(value); }
            }))
            .child(button("custom-search", "搜索", true, new Runnable() {
                @Override public void run() { state.returnToBrowse(); send(TerminalActionType.MARKET_CUSTOM_REFRESH); }
            }))
            .build();
        UiElement workspace = UiElement.type("Row").key("custom-workspace")
            .child(browser(custom)).child(detail(custom)).build();
        UiElement base = UiElement.type("Column").key("custom-base")
            .child(label("custom-title", "定制商品市场 · " + custom.getServiceState(), "section", true, "text"))
            .child(toolbar).child(workspace).build();
        UiElement.Builder root = UiElement.type("Stack").key("custom-root").child(base);
        if (pending != null) root.child(dialog("custom-confirm", pending.title, pending.detail, pending.confirm));
        if (helpOpen) root.child(dialog("custom-help", "定制市场帮助",
            "定制市场只交易单件托管物。选择挂牌后可购买、下架或领取；发布时必须选择 Base Vault 中数量为 1 的真实物品。所有权、余额与托管状态由服务端复核。", ""));
        return TerminalAppShell.build(model, root.build(), shellActions, "定制商品市场");
    }

    private UiElement browser(final TerminalCustomMarketSectionModel custom) {
        UiElement.Builder list = UiElement.type("Column").key("custom-browser");
        List<TerminalMarketBrowseEntry> entries = custom.getBrowseEntries();
        int count = Math.min(5, entries.size());
        if (count == 0) list.child(UiElement.type("EmptyState").key("custom-empty")
            .prop(StandardWidgets.TEXT, custom.getBrowserHint()).build());
        for (int i = 0; i < count; i++) {
            final TerminalMarketBrowseEntry entry = entries.get(i);
            list.child(button("custom-entry-" + i,
                entry.getTitle() + " · " + entry.getPrimaryValue() + " · " + entry.getStatus(),
                true, new Runnable() {
                    @Override public void run() {
                        state.requestDetail(entry.getKey());
                        send(TerminalActionType.MARKET_CUSTOM_SELECT_LISTING);
                    }
                }));
        }
        list.child(UiElement.type("Row").key("custom-pager")
            .child(button("custom-prev", "‹", custom.hasPreviousPage(), new Runnable() {
                @Override public void run() { state.setBrowserPage(Math.max(0, state.getBrowserPage() - 1)); state.returnToBrowse(); send(TerminalActionType.MARKET_CUSTOM_REFRESH); }
            }))
            .child(label("custom-page", (custom.getBrowsePageIndex() + 1) + " / " + pageCount(custom), "caption", false, "textMuted"))
            .child(button("custom-next", "›", custom.hasNextPage(), new Runnable() {
                @Override public void run() { state.setBrowserPage(state.getBrowserPage() + 1); state.returnToBrowse(); send(TerminalActionType.MARKET_CUSTOM_REFRESH); }
            })).build());
        return list.build();
    }

    private UiElement detail(final TerminalCustomMarketSectionModel custom) {
        UiElement.Builder panel = UiElement.type("Column").key("custom-detail")
            .child(UiElement.type("Card").key("custom-summary")
                .child(label("custom-name", custom.getSelectedTitle(), "body", true, "text"))
                .child(label("custom-meta", "价格 " + custom.getSelectedPrice() + " · 状态 " + custom.getSelectedStatus(), "caption", false, "textMuted"))
                .child(label("custom-trade", custom.getSelectedTradeSummary(), "caption", false, "textMuted"))
                .child(label("custom-owner", custom.getSelectedCounterparty() + " · " + custom.getSelectedItemIdentity(), "caption", false, "textMuted"))
                .build());
        UiElement.Builder actionsRow = UiElement.type("Row").key("custom-actions");
        if (custom.isCanBuy()) actionsRow.child(action("custom-buy", "购买", TerminalActionType.MARKET_CUSTOM_BUY_LISTING, "确认购买"));
        if (custom.isCanCancel()) actionsRow.child(action("custom-cancel", "下架", TerminalActionType.MARKET_CUSTOM_CANCEL_LISTING, "确认下架"));
        if (custom.isCanClaim()) actionsRow.child(action("custom-claim", "领取", TerminalActionType.MARKET_CUSTOM_CLAIM_LISTING, "确认领取"));
        panel.child(actionsRow.build());
        panel.child(vaultPicker());
        panel.child(field("custom-price", "挂牌价格", new Binding() {
            @Override public String read() { return state.getPublishPriceText(); }
            @Override public void write(String value) { state.setPublishPriceText(value); }
        }));
        panel.child(button("custom-publish", "发布所选单件", state.getSelectedVaultSlot() >= 0 && state.hasPublishPrice(), new Runnable() {
            @Override public void run() {
                pending = new Pending(TerminalActionType.MARKET_CUSTOM_PUBLISH_HELD, "确认发布",
                    "Base Vault 第 " + (state.getSelectedVaultSlot() + 1) + " 格，数量必须为 1；挂牌价 " + state.getPublishPriceText() + "。服务端将复核格位、物品和价格。", "发布挂牌");
                invalidation.run();
            }
        }));
        panel.child(label("custom-feedback", custom.getActionFeedback().getTitle() + " · " + custom.getActionFeedback().getBody(), "caption", false, "textMuted"));
        return panel.build();
    }

    private UiElement vaultPicker() {
        UiElement.Builder row = UiElement.type("Row").key("custom-vault");
        List<TerminalMarketSectionModel.VaultAssetModel> assets = market().getVaultAssets();
        int shown = 0;
        for (int i = 0; i < assets.size() && shown < 4; i++) {
            final TerminalMarketSectionModel.VaultAssetModel asset = assets.get(i);
            if (asset.getQuantity() != 1) continue;
            row.child(button("custom-vault-" + shown,
                (state.getSelectedVaultSlot() == asset.getSlotIndex() ? "✓ " : "") + asset.getDisplayName(), true,
                new Runnable() {
                    @Override public void run() { state.setSelectedVaultSlot(asset.getSlotIndex()); invalidation.run(); }
                }));
            shown++;
        }
        if (shown == 0) row.child(label("custom-vault-empty", "Vault 中没有可发布的单件物品", "caption", false, "textMuted"));
        return row.build();
    }

    private UiElement action(String key, String text, final TerminalActionType type, final String confirm) {
        return button(key, text, true, new Runnable() {
            @Override public void run() {
                TerminalCustomMarketSectionModel custom = custom();
                pending = new Pending(type, confirm,
                    custom.getSelectedTitle() + " · " + custom.getSelectedPrice() + " · " + custom.getSelectedStatus() + "。服务端将重新校验当前挂牌。", confirm);
                invalidation.run();
            }
        });
    }

    @Override
    public LayoutSpec layout(UiElement root, UiSize viewport, UiContext context) {
        width = viewport.getWidth();
        height = viewport.getHeight();
        boolean compact = TerminalWindowMetrics.compute(viewport).getBounds().getHeight() <= 220;
        LayoutSpec toolbar = LayoutSpec.of("custom-toolbar", LayoutKind.ROW).preferred(0, compact ? 15 : 18).gap(2)
            .child(leaf("custom-active", 42)).child(leaf("custom-selling", 54)).child(leaf("custom-pending", 50))
            .child(LayoutSpec.of("custom-query", LayoutKind.LEAF).flex(1F).build()).child(leaf("custom-search", 38)).build();
        LayoutSpec.Builder browser = LayoutSpec.of("custom-browser", LayoutKind.COLUMN).preferred(compact ? 105 : 150, 0).gap(2);
        int count = Math.min(5, custom().getBrowseEntries().size());
        if (count == 0) browser.child(LayoutSpec.of("custom-empty", LayoutKind.LEAF).flex(1F).build());
        for (int i = 0; i < count; i++) browser.child(LayoutSpec.of("custom-entry-" + i, LayoutKind.LEAF).flex(1F).build());
        browser.child(LayoutSpec.of("custom-pager", LayoutKind.ROW).preferred(0, compact ? 14 : 18).gap(2)
            .child(leaf("custom-prev", 24)).child(LayoutSpec.of("custom-page", LayoutKind.LEAF).flex(1F).build()).child(leaf("custom-next", 24)).build());
        LayoutSpec summary = LayoutSpec.of("custom-summary", LayoutKind.COLUMN).preferred(0, compact ? 48 : 62)
            .padding(new Insets(3, 4, 3, 4)).gap(1)
            .child(LayoutSpec.of("custom-name", LayoutKind.LEAF).preferred(0, 13).build())
            .child(LayoutSpec.of("custom-meta", LayoutKind.LEAF).preferred(0, 10).build())
            .child(LayoutSpec.of("custom-trade", LayoutKind.LEAF).preferred(0, 10).build())
            .child(LayoutSpec.of("custom-owner", LayoutKind.LEAF).flex(1F).build()).build();
        LayoutSpec.Builder actionLayout = LayoutSpec.of("custom-actions", LayoutKind.ROW)
            .preferred(0, compact ? 15 : 18).gap(2);
        TerminalCustomMarketSectionModel custom = custom();
        if (custom.isCanBuy()) actionLayout.child(LayoutSpec.of("custom-buy", LayoutKind.LEAF).flex(1F).build());
        if (custom.isCanCancel()) actionLayout.child(LayoutSpec.of("custom-cancel", LayoutKind.LEAF).flex(1F).build());
        if (custom.isCanClaim()) actionLayout.child(LayoutSpec.of("custom-claim", LayoutKind.LEAF).flex(1F).build());
        LayoutSpec.Builder vaultLayout = LayoutSpec.of("custom-vault", LayoutKind.ROW)
            .preferred(0, compact ? 15 : 18).gap(2);
        int vaultCount = eligibleVaultAssetCount();
        if (vaultCount == 0) vaultLayout.child(LayoutSpec.of("custom-vault-empty", LayoutKind.LEAF).flex(1F).build());
        for (int i = 0; i < vaultCount; i++) vaultLayout.child(LayoutSpec.of("custom-vault-" + i, LayoutKind.LEAF).flex(1F).build());
        LayoutSpec detail = LayoutSpec.of("custom-detail", LayoutKind.COLUMN).flex(1F).gap(2)
            .child(summary)
            .child(actionLayout.build())
            .child(vaultLayout.build())
            .child(LayoutSpec.of("custom-price", LayoutKind.LEAF).preferred(0, compact ? 15 : 18).build())
            .child(LayoutSpec.of("custom-publish", LayoutKind.LEAF).preferred(0, compact ? 15 : 18).build())
            .child(LayoutSpec.of("custom-feedback", LayoutKind.LEAF).flex(1F).build()).build();
        LayoutSpec page = LayoutSpec.of("custom-base", LayoutKind.COLUMN).padding(new Insets(3, 4, 3, 4)).gap(2)
            .child(LayoutSpec.of("custom-title", LayoutKind.LEAF).preferred(0, 14).build()).child(toolbar)
            .child(LayoutSpec.of("custom-workspace", LayoutKind.ROW).flex(1F).gap(3).child(browser.build()).child(detail).build()).build();
        LayoutSpec.Builder stack = LayoutSpec.of("custom-root", LayoutKind.STACK).flex(1F).child(page);
        if (pending != null) stack.child(LayoutSpec.of("custom-confirm", LayoutKind.LEAF).build());
        if (helpOpen) stack.child(LayoutSpec.of("custom-help", LayoutKind.LEAF).build());
        return TerminalAppShell.layout(viewport, model, stack.build());
    }

    @Override
    public UiInputNode modalInput(UiNode root, UiContext context) {
        if (pending == null && !helpOpen) return null;
        return new UiInputNode(pending == null ? "custom-help" : "custom-confirm", new UiRect(0, 0, width, height), true,
            new UiInputHandler() {
                @Override public InputResult handle(UiInputNode target, UiEvent event) {
                    if (event.getPhase() != UiEvent.Phase.TARGET) return InputResult.PASS;
                    if (pending != null && event.getType() == UiEvent.Type.KEY_DOWN && event.getKeyCode() == UiKeyCode.ENTER) {
                        submit(); return InputResult.CONSUMED;
                    }
                    if (pending != null && event.getType() == UiEvent.Type.POINTER_DOWN && confirmHit(event)) {
                        submit(); return InputResult.CONSUMED;
                    }
                    if ((event.getType() == UiEvent.Type.KEY_DOWN && event.getKeyCode() == UiKeyCode.ESCAPE)
                        || event.getType() == UiEvent.Type.POINTER_DOWN) {
                        pending = null; helpOpen = false; invalidation.run(); return InputResult.CONSUMED;
                    }
                    return InputResult.PASS;
                }
            });
    }

    private void submit() {
        Pending value = pending;
        if (value != null) send(value.action);
        if (value != null && value.action == TerminalActionType.MARKET_CUSTOM_PUBLISH_HELD) {
            state.setSelectedVaultSlot(-1); state.setPublishPriceText("");
        }
        pending = null;
        invalidation.run();
    }

    private void send(TerminalActionType action) { actions.sendMarket(action, state.toPayload().encode()); }
    private TerminalCustomMarketSectionModel custom() {
        TerminalHomeScreenModel.PageSnapshotModel page = model.getPageSnapshot("market");
        return page.hasCustomMarketSectionModel() ? page.getCustomMarketSectionModel() : TerminalCustomMarketSectionModel.placeholder();
    }
    private TerminalMarketSectionModel market() {
        TerminalHomeScreenModel.PageSnapshotModel page = model.getPageSnapshot("market");
        return page.hasMarketSectionModel() ? page.getMarketSectionModel() : TerminalMarketSectionModel.placeholder("market_custom");
    }
    private int pageCount(TerminalCustomMarketSectionModel value) {
        return Math.max(1, (value.getBrowseTotalEntries() + value.getBrowsePageSize() - 1) / value.getBrowsePageSize());
    }
    private int eligibleVaultAssetCount() {
        int count = 0;
        for (TerminalMarketSectionModel.VaultAssetModel asset : market().getVaultAssets()) {
            if (asset.getQuantity() == 1 && ++count == 4) break;
        }
        return count;
    }
    private UiElement tab(String key, String text, final String scope) {
        return UiElement.type("Tab").key(key).prop(StandardWidgets.TEXT, text)
            .prop(StandardWidgets.SELECTED, scope.equals(state.getSelectedScope())).prop(StandardWidgets.ACTION, handler(new Runnable() {
                @Override public void run() { state.setSelectedScope(scope); state.returnToBrowse(); state.setBrowserPage(0); send(TerminalActionType.MARKET_CUSTOM_REFRESH); }
            })).build();
    }
    private UiElement field(String key, String placeholder, final Binding binding) {
        return UiElement.type("TextField").key(key).prop(StandardWidgets.TEXT, binding.read())
            .prop(StandardWidgets.PLACEHOLDER, placeholder).prop(StandardWidgets.FOCUSED, false)
            .prop(StandardWidgets.ACTION, new UiActionHandler() {
                @Override public InputResult handle(UiEvent event) {
                    String current = binding.read();
                    if (event.getType() == UiEvent.Type.TEXT_INPUT && !Character.isISOControl(event.getTypedChar())) binding.write(current + event.getTypedChar());
                    else if (event.getType() == UiEvent.Type.KEY_DOWN && (event.getKeyCode() == UiKeyCode.BACKSPACE || event.getKeyCode() == UiKeyCode.DELETE)) binding.write(current.isEmpty() ? current : current.substring(0, current.length() - 1));
                    invalidation.run(); return InputResult.CONSUMED;
                }
            }).build();
    }
    private UiElement dialog(String key, String title, String detail, String confirm) {
        return UiElement.type("Dialog").key(key).prop(StandardWidgets.TEXT, title).prop(StandardWidgets.DETAIL, detail)
            .prop(StandardWidgets.CANCEL_TEXT, "取消").prop(StandardWidgets.CONFIRM_TEXT, confirm).build();
    }
    private boolean confirmHit(UiEvent event) {
        int dw = Math.min(230, Math.max(80, width - 40)), dh = Math.min(90, Math.max(50, height - 30));
        int dx = (width - dw) / 2, dy = (height - dh) / 2, half = Math.max(0, (dw - 28) / 2);
        return new UiRect(dx + 10 + half + 8, dy + dh - 22, half, 16).contains(event.getX(), event.getY());
    }
    private static LayoutSpec leaf(String key, int width) { return LayoutSpec.of(key, LayoutKind.LEAF).preferred(width, 0).build(); }
    private static UiElement button(String key, String text, boolean enabled, Runnable run) { return UiElement.type("Button").key(key).prop(StandardWidgets.TEXT, text).prop(StandardWidgets.ENABLED, enabled).prop(StandardWidgets.ACTION, handler(run)).build(); }
    private static UiActionHandler handler(final Runnable run) { return new UiActionHandler() { @Override public InputResult handle(UiEvent event) { run.run(); return InputResult.CONSUMED; } }; }
    private static UiElement label(String key, String text, String role, boolean bold, String tone) { return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT, text).prop(StandardWidgets.TEXT_ROLE, role).prop(StandardWidgets.MAX_LINES, 1).prop(StandardWidgets.BOLD, bold).prop(StandardWidgets.TONE, tone).build(); }
    private interface Binding { String read(); void write(String value); }
    private static final class Pending {
        private final TerminalActionType action; private final String title; private final String detail; private final String confirm;
        private Pending(TerminalActionType action, String title, String detail, String confirm) { this.action = action; this.title = title; this.detail = detail; this.confirm = confirm; }
    }
}
