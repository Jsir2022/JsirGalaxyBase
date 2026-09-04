package com.jsirgalaxybase.terminal.client.component;

import java.util.List;
import java.util.function.Supplier;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.VerticalScrollPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalNotificationCenterModel;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

/** Compact filterable notification page; entries are read-only and route only through the terminal navigation handler. */
public final class TerminalNotificationCenterSection extends PanelContainer {

    private static final int PAGE_SIZE = 6;
    private static final int TOOLBAR_HEIGHT = 18;
    private static final int FOOTER_HEIGHT = 16;
    private final TerminalPanelFactory panels;
    private final TerminalNotificationCenterModel model;
    private final TerminalNotificationCenterState state;
    private final ActionHandler handler;

    public interface ActionHandler { void openTarget(String pageId, String recordId); void rebuild(); }

    public TerminalNotificationCenterSection(TerminalPanelFactory panels, TerminalNotificationCenterModel model,
        TerminalNotificationCenterState state, ActionHandler handler) {
        this.panels = panels;
        this.model = model == null ? TerminalNotificationCenterModel.empty() : model;
        this.state = state == null ? new TerminalNotificationCenterState() : state;
        this.handler = handler;
        rebuild();
    }

    private void rebuild() {
        // This section instance is reconstructed by the shell after filter/page changes; children are configured once.
        List<TerminalNotificationCenterModel.EntryModel> filtered = state.filtered(model);
        int pages = filtered.isEmpty() ? 1 : (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        state.setPageIndex(Math.min(state.getPageIndex(), pages - 1));
        addChild(filterButton(0, "来源:" + sourceLabel(), new Runnable() {
            @Override public void run() { state.setSource(nextSource(state.getSource())); notifyRebuild(); }}));
        addChild(filterButton(78, "等级:" + severityLabel(), new Runnable() {
            @Override public void run() { state.setSeverity(nextSeverity(state.getSeverity())); notifyRebuild(); }}));
        addChild(panels.createLabel(new GuiRect(160, 0, 130, 12), new Supplier<String>() {
            @Override public String get() { return model.getServiceState(); }
        }, ThemeColorKey.TEXT_SECONDARY, false));
        VerticalScrollPanel scroll = panels.createScrollPanel(new GuiRect(0, TOOLBAR_HEIGHT, 0, 0), 0, 2);
        int start = state.getPageIndex() * PAGE_SIZE;
        int end = Math.min(filtered.size(), start + PAGE_SIZE);
        if (filtered.isEmpty()) {
            scroll.addScrollableChild(panels.createNotificationCard(new GuiRect(0, 0, 0, 40),
                new TerminalHomeScreenModel.NotificationModel("暂无匹配通知", "可切换来源或等级筛选；新的重要业务结果会自动进入这里。", "INFO")), 40);
        } else {
            for (int i = start; i < end; i++) {
                final TerminalNotificationCenterModel.EntryModel entry = filtered.get(i);
                TerminalHomeScreenModel.NotificationModel card = new TerminalHomeScreenModel.NotificationModel(
                    entry.getTitle() + (entry.getOccurrences() > 1 ? " ×" + entry.getOccurrences() : ""), entry.getBody(),
                    entry.getSeverityName(), entry.getSourceId(), entry.getTargetPageId(), entry.getTargetRecordId());
                scroll.addScrollableChild(panels.createNotificationCard(new GuiRect(0, 0, 0, 42), card, new Runnable() {
                    @Override public void run() { if (handler != null && !empty(entry.getTargetPageId())) handler.openTarget(entry.getTargetPageId(), entry.getTargetRecordId()); }
                }), 42);
            }
        }
        addChild(scroll);
        addChild(panels.createButton(new GuiRect(0, 0, 32, 14), new Supplier<String>() { @Override public String get() { return "‹"; }},
            new Runnable() { @Override public void run() { state.setPageIndex(state.getPageIndex() - 1); notifyRebuild(); }},
            new Supplier<Boolean>() { @Override public Boolean get() { return Boolean.valueOf(state.getPageIndex() > 0); }}));
        addChild(panels.createLabel(new GuiRect(34, 0, 78, 12), new Supplier<String>() { @Override public String get() {
            return (state.getPageIndex() + 1) + "/" + pages + " · " + filtered.size() + " 条"; }}, ThemeColorKey.TEXT_SECONDARY, true));
        addChild(panels.createButton(new GuiRect(114, 0, 32, 14), new Supplier<String>() { @Override public String get() { return "›"; }},
            new Runnable() { @Override public void run() { state.setPageIndex(state.getPageIndex() + 1); notifyRebuild(); }},
            new Supplier<Boolean>() { @Override public Boolean get() { return Boolean.valueOf(state.getPageIndex() + 1 < pages); }}));
    }

    @Override public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        int width = Math.max(1, bounds.getWidth()); int height = Math.max(1, bounds.getHeight());
        List<com.jsirgalaxybase.client.gui.framework.GuiPanel> children = getChildren();
        if (children.size() < 7) return;
        children.get(0).setBounds(new GuiRect(bounds.getX(), bounds.getY(), 74, 14));
        children.get(1).setBounds(new GuiRect(bounds.getX() + 76, bounds.getY(), 78, 14));
        children.get(2).setBounds(new GuiRect(bounds.getX() + 157, bounds.getY() + 2, Math.max(1, width - 157), 10));
        children.get(3).setBounds(new GuiRect(bounds.getX(), bounds.getY() + TOOLBAR_HEIGHT, width,
            Math.max(1, height - TOOLBAR_HEIGHT - FOOTER_HEIGHT)));
        int footerY = bounds.getBottom() - FOOTER_HEIGHT;
        children.get(4).setBounds(new GuiRect(bounds.getX(), footerY, 30, 14));
        children.get(5).setBounds(new GuiRect(bounds.getX() + 32, footerY + 2, Math.max(48, width - 96), 10));
        children.get(6).setBounds(new GuiRect(bounds.getRight() - 30, footerY, 30, 14));
    }

    private ButtonPanel filterButton(int x, final String label, Runnable action) {
        return panels.createButton(new GuiRect(x, 0, 1, 1), new Supplier<String>() { @Override public String get() { return label; }}, action,
            new Supplier<Boolean>() { @Override public Boolean get() { return Boolean.TRUE; }});
    }
    private void notifyRebuild() { if (handler != null) handler.rebuild(); }
    private String sourceLabel() { return sourceLabel(state.getSource()); }
    private static String sourceLabel(TerminalNotificationCenterState.SourceFilter value) { return value == TerminalNotificationCenterState.SourceFilter.ALL ? "全部" : value == TerminalNotificationCenterState.SourceFilter.MARKET ? "市场" : value == TerminalNotificationCenterState.SourceFilter.TRANSFER ? "传送" : value == TerminalNotificationCenterState.SourceFilter.LAND ? "地产" : value == TerminalNotificationCenterState.SourceFilter.BANK ? "银行" : "其他"; }
    private String severityLabel() { return severityLabel(state.getSeverity()); }
    private static String severityLabel(TerminalNotificationCenterState.SeverityFilter value) { return value == TerminalNotificationCenterState.SeverityFilter.ALL ? "全部" : value == TerminalNotificationCenterState.SeverityFilter.ERROR ? "错误" : value == TerminalNotificationCenterState.SeverityFilter.WARNING ? "警告" : value == TerminalNotificationCenterState.SeverityFilter.SUCCESS ? "成功" : "信息"; }
    private static TerminalNotificationCenterState.SourceFilter nextSource(TerminalNotificationCenterState.SourceFilter value) { TerminalNotificationCenterState.SourceFilter[] values = TerminalNotificationCenterState.SourceFilter.values(); return values[(value.ordinal() + 1) % values.length]; }
    private static TerminalNotificationCenterState.SeverityFilter nextSeverity(TerminalNotificationCenterState.SeverityFilter value) { TerminalNotificationCenterState.SeverityFilter[] values = TerminalNotificationCenterState.SeverityFilter.values(); return values[(value.ordinal() + 1) % values.length]; }
    private static boolean empty(String value) { return value == null || value.trim().isEmpty(); }
}
