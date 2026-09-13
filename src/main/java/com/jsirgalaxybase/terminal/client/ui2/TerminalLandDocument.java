package com.jsirgalaxybase.terminal.client.ui2;

import java.util.List;

import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.TerminalLandActionPayload;
import com.jsirgalaxybase.terminal.client.component.ClientLandTerrainCache;
import com.jsirgalaxybase.terminal.client.component.TerminalLandMapPanel;
import com.jsirgalaxybase.terminal.client.component.TerminalLandSectionState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalLandSectionModel;
import com.jsirgalaxybase.ui2.component.ComponentUiDocument;
import com.jsirgalaxybase.ui2.component.StandardWidgets;
import com.jsirgalaxybase.ui2.component.UiActionHandler;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.InputResult;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiInputHandler;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.layout.LayoutKind;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;

/** Modern property workspace embedding the proven real-terrain renderer as a UI2 external surface. */
public final class TerminalLandDocument extends ComponentUiDocument implements TerminalPageDocument {
    public interface Actions { void sendLand(TerminalActionType action, TerminalLandActionPayload payload); }
    private static final String MAP_ID = "land:terrain";
    private final TerminalLandSectionState state;
    private final TerminalAppShell.Actions shellActions;
    private final Actions actions;
    private final Runnable invalidation;
    private TerminalHomeScreenModel model;
    private TerminalLandMapPanel mapPanel;
    private boolean confirmOpen;
    private boolean confirmUnclaim;
    private boolean helpOpen;
    private int width;
    private int height;

    public TerminalLandDocument(TerminalLandSectionState state, TerminalHomeScreenModel model,
        TerminalAppShell.Actions shellActions, Actions actions, Runnable invalidation) {
        super(StandardWidgets.create());
        this.state = state;
        this.model = model;
        this.shellActions = shellActions;
        this.actions = actions;
        this.invalidation = invalidation;
        rebuildMap();
    }

    @Override public void update(TerminalHomeScreenModel value) { model = value; rebuildMap(); }
    @Override public void openHelp() { helpOpen = true; invalidation.run(); }
    @Override public boolean refresh() { actions.sendLand(TerminalActionType.REFRESH_PAGE, state.toPayload()); return true; }

    @Override
    public UiElement build(UiContext context) {
        final TerminalLandSectionModel land = land();
        UiElement tabs = UiElement.type("Row").key("land-tabs")
            .child(tab("land-nearby", "附近地皮", TerminalLandActionPayload.Tab.NEARBY))
            .child(tab(TerminalLandLayoutKeys.TAB_MINE, "我的产权", TerminalLandActionPayload.Tab.MINE))
            .child(label("land-status", land.getServerId() + " · 维度 " + land.getDimensionId() + " · "
                + land.getProtectionMode() + " · " + land.getUsedClaims() + "/" + land.getMaxClaims(), "caption", 1, false, "textMuted"))
            .build();
        UiElement content = state.getTab() == TerminalLandActionPayload.Tab.MINE ? mine(land)
            : UiElement.type("Row").key("land-workspace")
                .child(UiElement.type("ExternalSurface").key("land-map").prop(StandardWidgets.EXTERNAL_ID, MAP_ID).build())
                .child(inspector(land)).build();
        UiElement base = UiElement.type("Column").key("land-base")
            .child(label("land-title", "个人地产", "section", 1, true, "text"))
            .child(tabs).child(content).build();
        UiElement.Builder root = UiElement.type("Stack").key("land-root").child(base);
        if (confirmOpen) root.child(UiElement.type("Dialog").key("land-confirm")
            .prop(StandardWidgets.TEXT, confirmUnclaim ? "确认放弃个人地皮" : "确认认领个人地皮")
            .prop(StandardWidgets.DETAIL, confirmDetail(land))
            .prop(StandardWidgets.CANCEL_TEXT, "取消")
            .prop(StandardWidgets.CONFIRM_TEXT, confirmUnclaim ? "确认放弃" : "确认认领").build());
        if (helpOpen) root.child(UiElement.type("Dialog").key("land-help")
            .prop(StandardWidgets.TEXT, "地产地图帮助")
            .prop(StandardWidgets.DETAIL, "左键选择、拖动平移、滚轮缩放；F 定位玩家、R 刷新、+/- 缩放、Tab 临时隐藏产权层。地图只读取客户端已经加载的区块，认领范围仍由服务端限制。" ).build());
        return TerminalAppShell.build(model, root.build(), shellActions, "个人地产");
    }

    private UiElement mine(final TerminalLandSectionModel land) {
        UiElement.Builder list = UiElement.type("Column").key("land-mine-list");
        int count = Math.min(6, Math.min(land.getOwnedTitleIds().size(),
            Math.min(land.getOwnedChunkXs().size(), land.getOwnedChunkZs().size())));
        if (count == 0) list.child(UiElement.type("EmptyState").key("land-mine-empty")
            .prop(StandardWidgets.TEXT, "当前没有个人产权").build());
        for (int i = 0; i < count; i++) {
            final int index = i;
            list.child(button("land-mine-" + i, "#" + land.getOwnedTitleIds().get(i) + "  ["
                + land.getOwnedChunkXs().get(i) + ", " + land.getOwnedChunkZs().get(i) + "]  v"
                + land.getOwnedVersions().get(i), true, new Runnable() {
                    @Override public void run() {
                        state.selectTab(TerminalLandActionPayload.Tab.NEARBY);
                        state.setViewport(land.getOwnedChunkXs().get(index), land.getOwnedChunkZs().get(index), TerminalLandActionPayload.Zoom.MEDIUM);
                        state.select(land.getOwnedChunkXs().get(index), land.getOwnedChunkZs().get(index), land.getOwnedVersions().get(index));
                        actions.sendLand(TerminalActionType.LAND_SELECT, state.toPayload());
                    }
                }));
        }
        UiElement pager = UiElement.type("Row").key("land-mine-pager")
            .child(button("land-mine-prev", "‹", land.getPageIndex() > 0, new Runnable() {
                @Override public void run() { state.setPageIndex(land.getPageIndex() - 1); actions.sendLand(TerminalActionType.LAND_CHANGE_PAGE, state.toPayload()); }
            }))
            .child(label("land-mine-page", (land.getPageIndex() + 1) + " / " + Math.max(1, land.getTotalPages()), "caption", 1, false, "textMuted"))
            .child(button("land-mine-next", "›", land.getPageIndex() + 1 < land.getTotalPages(), new Runnable() {
                @Override public void run() { state.setPageIndex(land.getPageIndex() + 1); actions.sendLand(TerminalActionType.LAND_CHANGE_PAGE, state.toPayload()); }
            })).build();
        return UiElement.type("Row").key(TerminalLandLayoutKeys.CONTENT_MINE)
            .child(UiElement.type("Column").key("land-mine-left").child(list.build()).child(pager).build())
            .child(inspector(land)).build();
    }

    private UiElement inspector(final TerminalLandSectionModel land) {
        long x0 = (long) land.getSelectedChunkX() * 16L, z0 = (long) land.getSelectedChunkZ() * 16L;
        boolean enabled = land.isCanUnclaim() || land.isCanClaim() && state.isSelectedTerrainLoaded();
        return UiElement.type("Card").key("land-inspector")
            .child(label("land-selected", "区块 [" + land.getSelectedChunkX() + ", " + land.getSelectedChunkZ() + "] · " + land.getSelectedState(), "body", 1, true, "text"))
            .child(label("land-blocks", "X " + x0 + ".." + (x0 + 15L) + " / Z " + z0 + ".." + (z0 + 15L), "caption", 2, false, "textMuted"))
            .child(label("land-owner", "产权 #" + (land.getSelectedTitleId() <= 0 ? "—" : land.getSelectedTitleId()) + " · v" + (land.getSelectedVersion() <= 0 ? "—" : land.getSelectedVersion()), "caption", 1, false, "textMuted"))
            .child(label("land-terrain", "地形 " + (state.isSelectedTerrainLoaded() ? "已加载" : "未知") + " · 费用未启用 · 未挂牌", "caption", 2, false, "textMuted"))
            .child(label("land-feedback", land.getFeedbackCode(), "caption", 2, false, "textMuted"))
            .child(button("land-primary", land.isCanUnclaim() ? "放弃产权" : "认领地皮", enabled, new Runnable() {
                @Override public void run() { confirmUnclaim = land.isCanUnclaim(); confirmOpen = true; invalidation.run(); }
            })).build();
    }

    @Override
    public LayoutSpec layout(UiElement root, UiSize viewport, UiContext context) {
        width = viewport.getWidth(); height = viewport.getHeight();
        boolean compact = TerminalWindowMetrics.compute(viewport).getBounds().getHeight() <= 220;
        LayoutSpec tabs = LayoutSpec.of("land-tabs", LayoutKind.ROW).preferred(0, compact ? 15 : 18).gap(2)
            .child(LayoutSpec.of("land-nearby", LayoutKind.LEAF).preferred(50, 0).build())
            .child(LayoutSpec.of(TerminalLandLayoutKeys.TAB_MINE, LayoutKind.LEAF).preferred(50, 0).build())
            .child(LayoutSpec.of("land-status", LayoutKind.LEAF).flex(1F).build()).build();
        LayoutSpec inspector = inspectorLayout(compact);
        LayoutSpec content;
        if (state.getTab() == TerminalLandActionPayload.Tab.MINE) {
            LayoutSpec.Builder list = LayoutSpec.of("land-mine-list", LayoutKind.COLUMN).flex(1F).gap(2);
            int count = Math.min(6, Math.min(land().getOwnedTitleIds().size(), Math.min(land().getOwnedChunkXs().size(), land().getOwnedChunkZs().size())));
            if (count == 0) list.child(LayoutSpec.of("land-mine-empty", LayoutKind.LEAF).flex(1F).build());
            for (int i = 0; i < count; i++) list.child(LayoutSpec.of("land-mine-" + i, LayoutKind.LEAF).flex(1F).build());
            LayoutSpec pager = LayoutSpec.of("land-mine-pager", LayoutKind.ROW).preferred(0, compact ? 15 : 18).gap(2)
                .child(LayoutSpec.of("land-mine-prev", LayoutKind.LEAF).preferred(26, 0).build())
                .child(LayoutSpec.of("land-mine-page", LayoutKind.LEAF).flex(1F).build())
                .child(LayoutSpec.of("land-mine-next", LayoutKind.LEAF).preferred(26, 0).build()).build();
            content = LayoutSpec.of(TerminalLandLayoutKeys.CONTENT_MINE, LayoutKind.ROW).flex(1F).gap(3)
                .child(LayoutSpec.of("land-mine-left", LayoutKind.COLUMN).flex(1F).gap(2)
                    .child(list.build()).child(pager).build()).child(inspector).build();
        } else {
            content = LayoutSpec.of("land-workspace", LayoutKind.ROW).flex(1F).gap(3)
                .child(LayoutSpec.of("land-map", LayoutKind.LEAF).flex(3F).build()).child(inspector).build();
        }
        LayoutSpec base = LayoutSpec.of("land-base", LayoutKind.COLUMN).padding(new Insets(3, 4, 3, 4)).gap(2)
            .child(LayoutSpec.of("land-title", LayoutKind.LEAF).preferred(0, 14).build()).child(tabs).child(content).build();
        LayoutSpec.Builder stack = LayoutSpec.of("land-root", LayoutKind.STACK).flex(1F).child(base);
        if (confirmOpen) stack.child(LayoutSpec.of("land-confirm", LayoutKind.LEAF).build());
        if (helpOpen) stack.child(LayoutSpec.of("land-help", LayoutKind.LEAF).build());
        return TerminalAppShell.layout(viewport, model, stack.build());
    }

    private LayoutSpec inspectorLayout(boolean compact) {
        return LayoutSpec.of("land-inspector", LayoutKind.COLUMN).preferred(compact ? 84 : 150, 0)
            .padding(new Insets(4, 5, 4, 5)).gap(2)
            .child(LayoutSpec.of("land-selected", LayoutKind.LEAF).preferred(0, 14).build())
            .child(LayoutSpec.of("land-blocks", LayoutKind.LEAF).preferred(0, 22).build())
            .child(LayoutSpec.of("land-owner", LayoutKind.LEAF).preferred(0, 11).build())
            .child(LayoutSpec.of("land-terrain", LayoutKind.LEAF).preferred(0, 22).build())
            .child(LayoutSpec.of("land-feedback", LayoutKind.LEAF).flex(1F).build())
            .child(LayoutSpec.of("land-primary", LayoutKind.LEAF).preferred(0, compact ? 16 : 19).build()).build();
    }

    public void drawExternal(DrawList list, int mouseX, int mouseY, float partialTicks) {
        com.jsirgalaxybase.ui2.geometry.UiRect bounds = externalBounds(list);
        if (bounds == null || mapPanel == null || state.getTab() != TerminalLandActionPayload.Tab.NEARBY) return;
        mapPanel.setBounds(bounds);
        mapPanel.draw(mouseX, mouseY, partialTicks);
    }
    @Override public boolean externalPointer(UiEvent.Type type, int x, int y, int button) {
        if (mapPanel == null || state.getTab() != TerminalLandActionPayload.Tab.NEARBY) return false;
        return type == UiEvent.Type.POINTER_DOWN ? mapPanel.mouseClicked(x, y, button)
            : mapPanel.mouseReleased(x, y, button);
    }
    @Override public boolean externalScroll(int x, int y, int delta) { return mapPanel != null && state.getTab() == TerminalLandActionPayload.Tab.NEARBY && mapPanel.mouseScrolled(x, y, delta); }
    @Override public boolean externalKey(char typedChar, int keyCode) { return mapPanel != null && state.getTab() == TerminalLandActionPayload.Tab.NEARBY && mapPanel.keyTyped(typedChar, keyCode); }
    public void close() { ClientLandTerrainCache.INSTANCE.clearAll(); }

    @Override
    public UiInputNode modalInput(UiNode root, UiContext context) {
        if (!confirmOpen && !helpOpen) return null;
        return new UiInputNode(confirmOpen ? "land-confirm" : "land-help", new com.jsirgalaxybase.ui2.geometry.UiRect(0, 0, width, height), true,
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

    private void rebuildMap() {
        state.applyModel(land());
        mapPanel = new TerminalLandMapPanel(land(), state, new TerminalLandMapPanel.Handler() {
            @Override public void select(int x, int z, long version, boolean loaded) { state.select(x, z, version, loaded); actions.sendLand(TerminalActionType.LAND_SELECT, state.toPayload()); }
            @Override public void viewport(int x, int z, TerminalLandActionPayload.Zoom zoom) { state.setViewport(x, z, zoom); actions.sendLand(TerminalActionType.LAND_VIEWPORT, state.toPayload()); }
            @Override public void refresh() { actions.sendLand(TerminalActionType.REFRESH_PAGE, state.toPayload()); }
            @Override public void contextAction(boolean unclaim) { confirmUnclaim = unclaim; confirmOpen = true; invalidation.run(); }
        });
    }
    private void submit() { actions.sendLand(confirmUnclaim ? TerminalActionType.LAND_UNCLAIM : TerminalActionType.LAND_CLAIM, state.toActionPayload(confirmUnclaim ? "ui2-land-unclaim" : "ui2-land-claim")); confirmOpen = false; invalidation.run(); }
    private UiElement tab(String key, String text, final TerminalLandActionPayload.Tab tab) { return UiElement.type("Tab").key(key).prop(StandardWidgets.TEXT, text).prop(StandardWidgets.SELECTED, state.getTab() == tab).prop(StandardWidgets.ACTION, handler(new Runnable() { @Override public void run() { state.selectTab(tab); actions.sendLand(TerminalActionType.LAND_SELECT_TAB, state.toPayload()); invalidation.run(); } })).build(); }
    private TerminalLandSectionModel land() { TerminalHomeScreenModel.PageSnapshotModel page = model.getPageSnapshot("property"); return page.hasLandSectionModel() ? page.getLandSectionModel() : TerminalLandSectionModel.unavailable(); }
    private String confirmDetail(TerminalLandSectionModel land) { long x = (long) land.getSelectedChunkX() * 16L, z = (long) land.getSelectedChunkZ() * 16L; return land.getServerId() + " · 维度 " + land.getDimensionId() + " · 区块 [" + land.getSelectedChunkX() + ", " + land.getSelectedChunkZ() + "] · X " + x + ".." + (x + 15) + " / Z " + z + ".." + (z + 15) + "。服务端将重新校验产权版本与操作资格。"; }
    private com.jsirgalaxybase.ui2.geometry.UiRect externalBounds(DrawList list) { if (list == null) return null; for (DrawCommand command : list.ordered()) if (command.getKind() == DrawCommand.Kind.EXTERNAL_REGION && MAP_ID.equals(command.getExternalId())) return command.getBounds(); return null; }
    private boolean confirmHit(UiEvent event) { int dw = Math.min(230, Math.max(80, width - 40)), dh = Math.min(90, Math.max(50, height - 30)), dx = (width - dw) / 2, dy = (height - dh) / 2, half = Math.max(0, (dw - 28) / 2); return new com.jsirgalaxybase.ui2.geometry.UiRect(dx + 10 + half + 8, dy + dh - 22, half, 16).contains(event.getX(), event.getY()); }
    private static UiElement button(String key, String text, boolean enabled, Runnable run) { return UiElement.type("Button").key(key).prop(StandardWidgets.TEXT, text).prop(StandardWidgets.ENABLED, enabled).prop(StandardWidgets.ACTION, handler(run)).build(); }
    private static UiActionHandler handler(final Runnable run) { return new UiActionHandler() { @Override public InputResult handle(UiEvent event) { run.run(); return InputResult.CONSUMED; } }; }
    private static UiElement label(String key, String text, String role, int lines, boolean bold, String tone) { return UiElement.type("Label").key(key).prop(StandardWidgets.TEXT, text).prop(StandardWidgets.TEXT_ROLE, role).prop(StandardWidgets.MAX_LINES, lines).prop(StandardWidgets.BOLD, bold).prop(StandardWidgets.TONE, tone).build(); }
}
