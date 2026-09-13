package com.jsirgalaxybase.terminal.client.ui2;

import java.util.List;

import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.TerminalExchangeMarketActionPayload;
import com.jsirgalaxybase.terminal.TerminalMarketBrowseEntry;
import com.jsirgalaxybase.terminal.client.component.TerminalExchangeMarketSectionState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;
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

/** UI2 front end for the server-authoritative exchange quote workflow. */
public final class TerminalExchangeMarketDocument extends ComponentUiDocument implements TerminalPageDocument {
    private final TerminalExchangeMarketSectionState state;
    private final TerminalAppShell.Actions shellActions;
    private final TerminalStandardMarketDocument.Actions actions;
    private final Runnable invalidation;
    private TerminalHomeScreenModel model;
    private boolean confirmOpen;
    private boolean helpOpen;
    private int width;
    private int height;

    public TerminalExchangeMarketDocument(TerminalExchangeMarketSectionState state, TerminalHomeScreenModel model,
        TerminalAppShell.Actions shellActions, TerminalStandardMarketDocument.Actions actions, Runnable invalidation) {
        super(StandardWidgets.create());
        this.state = state;
        this.model = model;
        this.shellActions = shellActions;
        this.actions = actions;
        this.invalidation = invalidation;
        state.applyModel(exchange());
    }

    @Override
    public void update(TerminalHomeScreenModel value) {
        model = value;
        TerminalExchangeMarketSectionModel next = exchange();
        if (state.acceptsModel(next)) state.applyModel(next);
    }

    @Override public void openHelp() { helpOpen = true; invalidation.run(); }
    @Override public boolean refresh() { send(TerminalActionType.MARKET_REFRESH); return true; }

    @Override
    public UiElement build(UiContext context) {
        final TerminalExchangeMarketSectionModel exchange = exchange();
        UiElement toolbar = UiElement.type("Row").key("exchange-toolbar")
            .child(field("exchange-query", "搜索兑换资产"))
            .child(button("exchange-search", "搜索", true, new Runnable() {
                @Override public void run() { state.returnToBrowse(); send(TerminalActionType.MARKET_REFRESH); }
            }))
            .child(button("exchange-refresh", "刷新报价", state.hasSelectedTarget(), new Runnable() {
                @Override public void run() { send(TerminalActionType.MARKET_EXCHANGE_REFRESH_QUOTE); }
            })).build();
        UiElement base = UiElement.type("Column").key("exchange-base")
            .child(label("exchange-title", "汇率市场 · " + exchange.getServiceState(), "section", true, "text"))
            .child(toolbar)
            .child(UiElement.type("Row").key("exchange-workspace").child(browser(exchange)).child(quote(exchange)).build())
            .build();
        UiElement.Builder root = UiElement.type("Stack").key("exchange-root").child(base);
        if (confirmOpen) root.child(dialog("exchange-confirm", "确认执行兑换",
            exchange.getPairCode() + "；从 Base Vault 第 " + (state.getSelectedVaultSlot() + 1) + " 格输入 "
                + exchange.getInputQuantity() + " " + exchange.getInputAssetCode() + "，预计到账 "
                + exchange.getEffectiveExchangeValue() + " " + exchange.getOutputAssetCode()
                + "。服务端将重新校验资产、报价版本和限额。", "确认兑换"));
        if (helpOpen) root.child(dialog("exchange-help", "汇率市场帮助",
            "先选择兑换资产，再从 Base Vault 选择符合规则的输入物。报价和限额会随服务端刷新；确认时始终按服务端当前有效报价执行。", ""));
        return TerminalAppShell.build(model, root.build(), shellActions, "汇率市场");
    }

    private UiElement browser(final TerminalExchangeMarketSectionModel exchange) {
        UiElement.Builder list = UiElement.type("Column").key("exchange-browser");
        List<TerminalMarketBrowseEntry> entries = exchange.getBrowseEntries();
        int count = Math.min(5, entries.size());
        if (count == 0) list.child(UiElement.type("EmptyState").key("exchange-empty")
            .prop(StandardWidgets.TEXT, exchange.getBrowserHint()).build());
        for (int i = 0; i < count; i++) {
            final TerminalMarketBrowseEntry entry = entries.get(i);
            list.child(button("exchange-entry-" + i,
                entry.getTitle() + " · " + entry.getPrimaryValue() + " · " + entry.getStatus(), true,
                new Runnable() {
                    @Override public void run() {
                        state.requestDetail(entry.getKey());
                        send(TerminalActionType.MARKET_EXCHANGE_SELECT_TARGET);
                    }
                }));
        }
        list.child(UiElement.type("Row").key("exchange-pager")
            .child(button("exchange-prev", "‹", exchange.hasPreviousPage(), new Runnable() {
                @Override public void run() { state.setBrowserPage(Math.max(0, state.getBrowserPage() - 1)); state.returnToBrowse(); send(TerminalActionType.MARKET_REFRESH); }
            }))
            .child(label("exchange-page", (exchange.getBrowsePageIndex() + 1) + " / " + pageCount(exchange), "caption", false, "textMuted"))
            .child(button("exchange-next", "›", exchange.hasNextPage(), new Runnable() {
                @Override public void run() { state.setBrowserPage(state.getBrowserPage() + 1); state.returnToBrowse(); send(TerminalActionType.MARKET_REFRESH); }
            })).build());
        return list.build();
    }

    private UiElement quote(final TerminalExchangeMarketSectionModel exchange) {
        UiElement.Builder panel = UiElement.type("Column").key("exchange-quote")
            .child(UiElement.type("Card").key("exchange-summary")
                .child(label("exchange-name", exchange.getSelectedTargetTitle(), "body", true, "text"))
                .child(label("exchange-pair", exchange.getPairCode() + " · " + exchange.getRateDisplay(), "caption", false, "warning"))
                .child(label("exchange-values", "面值 " + exchange.getNominalFaceValue() + " · 有效 " + exchange.getEffectiveExchangeValue() + " · 贡献 " + exchange.getContributionValue(), "caption", false, "textMuted"))
                .child(label("exchange-limit", exchange.getLimitStatusDisplay() + " · " + exchange.getDiscountStatus(), "caption", false, "textMuted"))
                .build())
            .child(vaultPicker())
            .child(label("exchange-held", exchange.getHeldSummary(), "caption", false, "textMuted"))
            .child(button("exchange-submit", "核对兑换", exchange.isExecutable() && state.getSelectedVaultSlot() >= 0,
                new Runnable() { @Override public void run() { confirmOpen = true; invalidation.run(); } }))
            .child(label("exchange-hint", exchange.getExecutionHint(), "caption", false, "textMuted"))
            .child(label("exchange-feedback", exchange.getActionFeedback().getTitle() + " · " + exchange.getActionFeedback().getBody(), "caption", false, "textMuted"));
        return panel.build();
    }

    private UiElement vaultPicker() {
        UiElement.Builder row = UiElement.type("Row").key("exchange-vault");
        List<TerminalMarketSectionModel.VaultAssetModel> assets = market().getVaultAssets();
        int shown = 0;
        for (int i = 0; i < assets.size() && shown < 4; i++) {
            final TerminalMarketSectionModel.VaultAssetModel asset = assets.get(i);
            if (!asset.getRegistryName().equals(exchange().getInputRegistryName())) continue;
            row.child(button("exchange-vault-" + shown,
                (state.getSelectedVaultSlot() == asset.getSlotIndex() ? "✓ " : "") + asset.getDisplayName() + " ×" + asset.getQuantity(), true,
                new Runnable() { @Override public void run() { state.setSelectedVaultSlot(asset.getSlotIndex()); invalidation.run(); } }));
            shown++;
        }
        if (shown == 0) row.child(label("exchange-vault-empty", "Vault 中没有符合当前报价的资产", "caption", false, "textMuted"));
        return row.build();
    }

    @Override
    public LayoutSpec layout(UiElement root, UiSize viewport, UiContext context) {
        width = viewport.getWidth(); height = viewport.getHeight();
        boolean compact = TerminalWindowMetrics.compute(viewport).getBounds().getHeight() <= 220;
        LayoutSpec toolbar = LayoutSpec.of("exchange-toolbar", LayoutKind.ROW).preferred(0, compact ? 15 : 18).gap(2)
            .child(LayoutSpec.of("exchange-query", LayoutKind.LEAF).flex(1F).build())
            .child(leaf("exchange-search", 38)).child(leaf("exchange-refresh", 54)).build();
        LayoutSpec.Builder browser = LayoutSpec.of("exchange-browser", LayoutKind.COLUMN).preferred(compact ? 105 : 150, 0).gap(2);
        int count = Math.min(5, exchange().getBrowseEntries().size());
        if (count == 0) browser.child(LayoutSpec.of("exchange-empty", LayoutKind.LEAF).flex(1F).build());
        for (int i = 0; i < count; i++) browser.child(LayoutSpec.of("exchange-entry-" + i, LayoutKind.LEAF).flex(1F).build());
        browser.child(LayoutSpec.of("exchange-pager", LayoutKind.ROW).preferred(0, compact ? 14 : 18).gap(2)
            .child(leaf("exchange-prev", 24)).child(LayoutSpec.of("exchange-page", LayoutKind.LEAF).flex(1F).build()).child(leaf("exchange-next", 24)).build());
        LayoutSpec summary = LayoutSpec.of("exchange-summary", LayoutKind.COLUMN).preferred(0, compact ? 49 : 64)
            .padding(new Insets(3, 4, 3, 4)).gap(1)
            .child(LayoutSpec.of("exchange-name", LayoutKind.LEAF).preferred(0, 13).build())
            .child(LayoutSpec.of("exchange-pair", LayoutKind.LEAF).preferred(0, 10).build())
            .child(LayoutSpec.of("exchange-values", LayoutKind.LEAF).preferred(0, 10).build())
            .child(LayoutSpec.of("exchange-limit", LayoutKind.LEAF).flex(1F).build()).build();
        LayoutSpec.Builder vaultLayout = LayoutSpec.of("exchange-vault", LayoutKind.ROW)
            .preferred(0, compact ? 15 : 18).gap(2);
        int vaultCount = eligibleVaultAssetCount();
        if (vaultCount == 0) vaultLayout.child(LayoutSpec.of("exchange-vault-empty", LayoutKind.LEAF).flex(1F).build());
        for (int i = 0; i < vaultCount; i++) vaultLayout.child(LayoutSpec.of("exchange-vault-" + i, LayoutKind.LEAF).flex(1F).build());
        LayoutSpec quote = LayoutSpec.of("exchange-quote", LayoutKind.COLUMN).flex(1F).gap(2)
            .child(summary).child(vaultLayout.build())
            .child(LayoutSpec.of("exchange-held", LayoutKind.LEAF).preferred(0, 11).build())
            .child(LayoutSpec.of("exchange-submit", LayoutKind.LEAF).preferred(0, compact ? 15 : 18).build())
            .child(LayoutSpec.of("exchange-hint", LayoutKind.LEAF).preferred(0, 11).build())
            .child(LayoutSpec.of("exchange-feedback", LayoutKind.LEAF).flex(1F).build()).build();
        LayoutSpec base = LayoutSpec.of("exchange-base", LayoutKind.COLUMN).padding(new Insets(3, 4, 3, 4)).gap(2)
            .child(LayoutSpec.of("exchange-title", LayoutKind.LEAF).preferred(0, 14).build()).child(toolbar)
            .child(LayoutSpec.of("exchange-workspace", LayoutKind.ROW).flex(1F).gap(3).child(browser.build()).child(quote).build()).build();
        LayoutSpec.Builder stack = LayoutSpec.of("exchange-root", LayoutKind.STACK).flex(1F).child(base);
        if (confirmOpen) stack.child(LayoutSpec.of("exchange-confirm", LayoutKind.LEAF).build());
        if (helpOpen) stack.child(LayoutSpec.of("exchange-help", LayoutKind.LEAF).build());
        return TerminalAppShell.layout(viewport, model, stack.build());
    }

    @Override
    public UiInputNode modalInput(UiNode root, UiContext context) {
        if (!confirmOpen && !helpOpen) return null;
        return new UiInputNode(confirmOpen ? "exchange-confirm" : "exchange-help", new UiRect(0, 0, width, height), true,
            new UiInputHandler() {
                @Override public InputResult handle(UiInputNode target, UiEvent event) {
                    if (event.getPhase() != UiEvent.Phase.TARGET) return InputResult.PASS;
                    if (confirmOpen && event.getType() == UiEvent.Type.KEY_DOWN && event.getKeyCode() == UiKeyCode.ENTER) { submit(); return InputResult.CONSUMED; }
                    if (confirmOpen && event.getType() == UiEvent.Type.POINTER_DOWN && confirmHit(event)) { submit(); return InputResult.CONSUMED; }
                    if ((event.getType() == UiEvent.Type.KEY_DOWN && event.getKeyCode() == UiKeyCode.ESCAPE) || event.getType() == UiEvent.Type.POINTER_DOWN) {
                        confirmOpen = false; helpOpen = false; invalidation.run(); return InputResult.CONSUMED;
                    }
                    return InputResult.PASS;
                }
            });
    }

    private void submit() { send(TerminalActionType.MARKET_EXCHANGE_CONFIRM); state.setSelectedVaultSlot(-1); confirmOpen = false; invalidation.run(); }
    private void send(TerminalActionType action) { actions.sendMarket(action, state.toPayload().encode()); }
    private TerminalExchangeMarketSectionModel exchange() {
        TerminalHomeScreenModel.PageSnapshotModel page = model.getPageSnapshot("market");
        return page.hasExchangeMarketSectionModel() ? page.getExchangeMarketSectionModel() : TerminalExchangeMarketSectionModel.placeholder();
    }
    private TerminalMarketSectionModel market() {
        TerminalHomeScreenModel.PageSnapshotModel page = model.getPageSnapshot("market");
        return page.hasMarketSectionModel() ? page.getMarketSectionModel() : TerminalMarketSectionModel.placeholder("market_exchange");
    }
    private int pageCount(TerminalExchangeMarketSectionModel value) { return Math.max(1, (value.getBrowseTotalEntries() + value.getBrowsePageSize() - 1) / value.getBrowsePageSize()); }
    private int eligibleVaultAssetCount() {
        int count = 0;
        String registry = exchange().getInputRegistryName();
        for (TerminalMarketSectionModel.VaultAssetModel asset : market().getVaultAssets()) {
            if (asset.getRegistryName().equals(registry) && ++count == 4) break;
        }
        return count;
    }
    private UiElement field(String key, String placeholder) {
        return UiElement.type("TextField").key(key).prop(StandardWidgets.TEXT, state.getBrowserQuery())
            .prop(StandardWidgets.PLACEHOLDER, placeholder).prop(StandardWidgets.FOCUSED, false)
            .prop(StandardWidgets.ACTION, new UiActionHandler() {
                @Override public InputResult handle(UiEvent event) {
                    String current = state.getBrowserQuery();
                    if (event.getType() == UiEvent.Type.TEXT_INPUT && !Character.isISOControl(event.getTypedChar())) state.setBrowserQuery(current + event.getTypedChar());
                    else if (event.getType() == UiEvent.Type.KEY_DOWN && (event.getKeyCode() == UiKeyCode.BACKSPACE || event.getKeyCode() == UiKeyCode.DELETE)) state.setBrowserQuery(current.isEmpty() ? current : current.substring(0, current.length() - 1));
                    invalidation.run(); return InputResult.CONSUMED;
                }
            }).build();
    }
    private UiElement dialog(String key, String title, String detail, String confirm) { return UiElement.type("Dialog").key(key).prop(StandardWidgets.TEXT, title).prop(StandardWidgets.DETAIL, detail).prop(StandardWidgets.CANCEL_TEXT, "取消").prop(StandardWidgets.CONFIRM_TEXT, confirm).build(); }
    private boolean confirmHit(UiEvent event) { int dw = Math.min(230, Math.max(80, width - 40)), dh = Math.min(90, Math.max(50, height - 30)), dx = (width - dw) / 2, dy = (height - dh) / 2, half = Math.max(0, (dw - 28) / 2); return new UiRect(dx + 10 + half + 8, dy + dh - 22, half, 16).contains(event.getX(), event.getY()); }
    private static LayoutSpec leaf(String key, int width) { return LayoutSpec.of(key, LayoutKind.LEAF).preferred(width, 0).build(); }
    private static UiElement button(String key, String text, boolean enabled, Runnable run) { return UiElement.type("Button").key(key).prop(StandardWidgets.TEXT, text).prop(StandardWidgets.ENABLED, enabled).prop(StandardWidgets.ACTION, handler(run)).build(); }
    private static UiActionHandler handler(final Runnable run) { return new UiActionHandler() { @Override public InputResult handle(UiEvent event) { run.run(); return InputResult.CONSUMED; } }; }
    private static UiElement label(String key, String text, String role, boolean bold, String tone) { return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT, text).prop(StandardWidgets.TEXT_ROLE, role).prop(StandardWidgets.MAX_LINES, 1).prop(StandardWidgets.BOLD, bold).prop(StandardWidgets.TONE, tone).build(); }
}
