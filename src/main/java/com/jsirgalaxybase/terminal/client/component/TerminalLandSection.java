package com.jsirgalaxybase.terminal.client.component;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.resources.I18n;

import com.jsirgalaxybase.client.gui.framework.ButtonPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiPanel;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.framework.TexturedCanvasPanel;
import com.jsirgalaxybase.client.gui.framework.VerticalScrollPanel;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.TerminalLandActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalLandSectionModel;

public final class TerminalLandSection extends PanelContainer {

    private static final int HEADER_HEIGHT = 16;
    private static final int TAB_ROW_TOP = 18;
    private static final int TAB_HEIGHT = 12;
    private static final int CONTENT_TOP = 32;
    private static final int CONTENT_GAP = 4;

    public interface ActionHandler {
        void selectChunk(int chunkX, int chunkZ, long version);
        void selectTab(TerminalLandActionPayload.Tab tab);
        void changePage(int pageIndex);
        void changeViewport(int chunkX, int chunkZ, TerminalLandActionPayload.Zoom zoom);
        void refreshMap();
        void openClaimConfirm();
        void openUnclaimConfirm();
    }

    private final TerminalPanelFactory panels;
    private final TerminalLandSectionModel model;
    private final TerminalLandSectionState state;
    private final ActionHandler handler;
    private final TexturedCanvasPanel summary;
    private final PanelContainer content = new PanelContainer();
    private final ButtonPanel nearbyTab;
    private final ButtonPanel mineTab;
    private final GuiPanel shortcutHint;
    private TerminalLandMapPanel mapPanel;

    public TerminalLandSection(TerminalPanelFactory panels, TerminalLandSectionModel model,
        TerminalLandSectionState state, ActionHandler handler) {
        this.panels = panels;
        this.model = model == null ? TerminalLandSectionModel.unavailable() : model;
        this.state = state == null ? new TerminalLandSectionState() : state;
        this.handler = handler;
        summary = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        summary.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override public String get() {
                return tr("jsirgalaxybase.land.summary", TerminalLandSection.this.model.getServerId(),
                    Integer.valueOf(TerminalLandSection.this.model.getDimensionId()),
                    Integer.valueOf(TerminalLandSection.this.model.getCenterChunkX()),
                    Integer.valueOf(TerminalLandSection.this.model.getCenterChunkZ()),
                    TerminalLandSection.this.model.getProtectionMode(),
                    Integer.valueOf(TerminalLandSection.this.model.getUsedClaims()),
                    Integer.valueOf(TerminalLandSection.this.model.getMaxClaims()));
            }
        }, ThemeColorKey.TEXT_PRIMARY, false));
        nearbyTab = tabButton("jsirgalaxybase.land.tab.nearby", TerminalLandActionPayload.Tab.NEARBY);
        mineTab = tabButton("jsirgalaxybase.land.tab.mine", TerminalLandActionPayload.Tab.MINE);
        shortcutHint = panels.createLabel(new GuiRect(0, 0, 0, 0), () ->
            tr("jsirgalaxybase.land.map.shortcuts"), ThemeColorKey.TEXT_SECONDARY, false);
        addChild(summary);
        addChild(nearbyTab);
        addChild(mineTab);
        addChild(shortcutHint);
        addChild(content);
        rebuildContent();
    }

    @Override
    public void setBounds(GuiRect bounds) {
        super.setBounds(bounds);
        int x = bounds.getX();
        int y = bounds.getY();
        int width = bounds.getWidth();
        summary.setBounds(new GuiRect(x, y, width, HEADER_HEIGHT));
        if (!summary.getChildren().isEmpty()) {
            summary.getChildren().get(0).setBounds(new GuiRect(x + 4, y + 2, width - 8, 11));
        }
        int tabWidth = Math.min(70, Math.max(34, width / 10));
        nearbyTab.setBounds(new GuiRect(x, y + TAB_ROW_TOP, tabWidth, TAB_HEIGHT));
        mineTab.setBounds(new GuiRect(x + tabWidth + CONTENT_GAP, y + TAB_ROW_TOP, tabWidth, TAB_HEIGHT));
        int hintX = x + tabWidth * 2 + CONTENT_GAP * 2 + 3;
        shortcutHint.setBounds(new GuiRect(hintX, y + TAB_ROW_TOP,
            Math.max(0, x + width - hintX), TAB_HEIGHT));
        content.setBounds(new GuiRect(x, y + CONTENT_TOP, width, Math.max(0, bounds.getHeight() - CONTENT_TOP)));
        layoutContent();
    }

    private ButtonPanel tabButton(final String key, final TerminalLandActionPayload.Tab tab) {
        return panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
            @Override public String get() { return (state.getTab() == tab ? "[" : "") + tr(key)
                + (state.getTab() == tab ? "]" : ""); }
        }, new Runnable() {
            @Override public void run() { if (handler != null) handler.selectTab(tab); }
        }, null);
    }

    private void rebuildContent() {
        if (state.getTab() == TerminalLandActionPayload.Tab.MINE) buildMine(); else buildNearby();
    }

    private void buildNearby() {
        mapPanel = new TerminalLandMapPanel(model, state, new TerminalLandMapPanel.Handler() {
            @Override public void select(int chunkX, int chunkZ, long version, boolean terrainLoaded) {
                state.select(chunkX, chunkZ, version, terrainLoaded);
                if (handler != null) handler.selectChunk(chunkX, chunkZ, version);
            }
            @Override public void viewport(int chunkX, int chunkZ, TerminalLandActionPayload.Zoom zoom) {
                state.setViewport(chunkX, chunkZ, zoom);
                if (handler != null) handler.changeViewport(chunkX, chunkZ, zoom);
            }
            @Override public void refresh() {
                if (handler != null) handler.refreshMap();
            }
            @Override public void contextAction(boolean unclaim) {
                if (handler == null) return;
                if (unclaim) handler.openUnclaimConfirm(); else handler.openClaimConfirm();
            }
        });
        content.addChild(mapPanel);
        addDetailsCard();
    }

    private void buildMine() {
        VerticalScrollPanel list = panels.createScrollPanel(new GuiRect(0, 0, 0, 0), 0, 4);
        int count = Math.min(model.getOwnedTitleIds().size(), Math.min(model.getOwnedChunkXs().size(), model.getOwnedChunkZs().size()));
        for (int i = 0; i < count; i++) {
            final int index = i;
            ButtonPanel row = panels.createButton(new GuiRect(0, 0, 0, 0), new Supplier<String>() {
                @Override public String get() {
                    return "#" + model.getOwnedTitleIds().get(index) + "  [" + model.getOwnedChunkXs().get(index)
                        + ", " + model.getOwnedChunkZs().get(index) + "]  v" + model.getOwnedVersions().get(index);
                }
            }, new Runnable() {
                @Override public void run() {
                    state.selectTab(TerminalLandActionPayload.Tab.NEARBY);
                    state.setViewport(model.getOwnedChunkXs().get(index), model.getOwnedChunkZs().get(index),
                        TerminalLandActionPayload.Zoom.MEDIUM);
                    if (handler != null) handler.selectChunk(model.getOwnedChunkXs().get(index),
                        model.getOwnedChunkZs().get(index), model.getOwnedVersions().get(index));
                }
            }, null);
            list.addScrollableChild(row, 30);
        }
        content.addChild(list);
        content.addChild(pagerButton("<", -1));
        content.addChild(pagerButton(">", 1));
        addDetailsCard();
    }

    private ButtonPanel pagerButton(final String label, final int delta) {
        return panels.createButton(new GuiRect(0, 0, 0, 0), () -> label, () -> {
            if (handler != null) handler.changePage(model.getPageIndex() + delta);
        }, () -> Boolean.valueOf(delta < 0 ? model.getPageIndex() > 0 : model.getPageIndex() + 1 < model.getTotalPages()));
    }

    private void addDetailsCard() {
        TexturedCanvasPanel detail = panels.createSurface(new GuiRect(0, 0, 0, 0), ThemeColorKey.PANEL_FILL);
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0), () -> tr("jsirgalaxybase.land.inspector.title"),
            ThemeColorKey.TEXT_SECONDARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0), () -> tr("jsirgalaxybase.land.selected",
            Integer.valueOf(model.getSelectedChunkX()), Integer.valueOf(model.getSelectedChunkZ()),
            tr("jsirgalaxybase.land.state." + model.getSelectedState().toLowerCase())), ThemeColorKey.TEXT_PRIMARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0), () -> tr("jsirgalaxybase.land.blocks",
            Long.valueOf((long) model.getSelectedChunkX() * 16L), Long.valueOf((long) model.getSelectedChunkX() * 16L + 15L),
            Long.valueOf((long) model.getSelectedChunkZ() * 16L), Long.valueOf((long) model.getSelectedChunkZ() * 16L + 15L)),
            ThemeColorKey.TEXT_SECONDARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0), () -> tr("jsirgalaxybase.land.inspector.owner.row",
            "OWNED".equals(model.getSelectedState()) ? tr("jsirgalaxybase.land.owner.self")
                : "OTHER".equals(model.getSelectedState()) ? tr("jsirgalaxybase.land.owner.other") : "—"),
            ThemeColorKey.TEXT_SECONDARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0), () -> tr("jsirgalaxybase.land.inspector.title.row",
            model.getSelectedTitleId() <= 0L ? "—" : "#" + model.getSelectedTitleId(),
            model.getSelectedVersion() <= 0L ? "—" : "v" + model.getSelectedVersion()),
            ThemeColorKey.TEXT_SECONDARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0), () -> tr("jsirgalaxybase.land.inspector.protection.row",
            model.getProtectionMode()), ThemeColorKey.TEXT_SECONDARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0), () -> tr("jsirgalaxybase.land.inspector.terrain.row",
            state.isSelectedTerrainLoaded() ? tr("jsirgalaxybase.land.map.loaded")
                : tr("jsirgalaxybase.land.map.unloaded.short")),
            ThemeColorKey.TEXT_SECONDARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0), () -> tr("jsirgalaxybase.land.inspector.market"),
            ThemeColorKey.TEXT_PRIMARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0),
            () -> tr("jsirgalaxybase.land.inspector.claim_fee"), ThemeColorKey.TEXT_SECONDARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0),
            () -> tr("jsirgalaxybase.land.inspector.assessed_value"), ThemeColorKey.TEXT_SECONDARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0),
            () -> tr("jsirgalaxybase.land.inspector.listing"), ThemeColorKey.TEXT_SECONDARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0),
            () -> tr("jsirgalaxybase.land.inspector.last_transfer"), ThemeColorKey.TEXT_SECONDARY, false));
        detail.addChild(panels.createLabel(new GuiRect(0, 0, 0, 0), () -> model.isCanClaim()
            && !state.isSelectedTerrainLoaded() ? tr("jsirgalaxybase.land.map.unloaded")
                : tr("jsirgalaxybase.land.feedback." + model.getFeedbackCode().toLowerCase()),
            ThemeColorKey.TEXT_SECONDARY, false));
        detail.addChild(panels.createButton(new GuiRect(0, 0, 0, 0), () -> model.isCanUnclaim()
            ? tr("jsirgalaxybase.land.unclaim") : tr("jsirgalaxybase.land.claim"), () -> {
                if (handler == null) return;
                if (model.isCanUnclaim()) handler.openUnclaimConfirm(); else handler.openClaimConfirm();
            }, () -> Boolean.valueOf(model.isCanUnclaim()
                || model.isCanClaim() && state.isSelectedTerrainLoaded())));
        content.addChild(detail);
    }

    private void layoutContent() {
        if (content.getChildren().isEmpty()) return;
        GuiRect bounds = content.getBounds();
        int detailWidth = inspectorWidth(bounds.getWidth());
        int leftWidth = Math.max(1, bounds.getWidth() - detailWidth - CONTENT_GAP);
        if (state.getTab() == TerminalLandActionPayload.Tab.NEARBY) {
            content.getChildren().get(0).setBounds(new GuiRect(bounds.getX(), bounds.getY(),
                leftWidth, bounds.getHeight()));
        } else {
            content.getChildren().get(0).setBounds(new GuiRect(bounds.getX(), bounds.getY(), leftWidth,
                Math.max(0, bounds.getHeight() - 34)));
            content.getChildren().get(1).setBounds(new GuiRect(bounds.getX(), bounds.getBottom() - 30, 42, 26));
            content.getChildren().get(2).setBounds(new GuiRect(bounds.getX() + 46, bounds.getBottom() - 30, 42, 26));
        }
        PanelContainer detail = (PanelContainer) content.getChildren().get(content.getChildren().size() - 1);
        detail.setBounds(new GuiRect(bounds.getX() + leftWidth + CONTENT_GAP, bounds.getY(), detailWidth,
            bounds.getHeight()));
        List<GuiPanel> children = detail.getChildren();
        if (children.size() >= 14) {
            int innerX = detail.getBounds().getX() + 4;
            int innerWidth = Math.max(8, detailWidth - 8);
            int top = detail.getBounds().getY() + 3;
            children.get(0).setBounds(new GuiRect(innerX, top, innerWidth, 8));
            children.get(1).setBounds(new GuiRect(innerX, top + 9, innerWidth, 14));
            children.get(2).setBounds(new GuiRect(innerX, top + 24, innerWidth, 14));
            for (int index = 3; index <= 6; index++) {
                children.get(index).setBounds(new GuiRect(innerX, top + 39 + (index - 3) * 9, innerWidth, 8));
            }
            children.get(7).setBounds(new GuiRect(innerX, top + 76, innerWidth, 8));
            for (int index = 8; index <= 11; index++) {
                children.get(index).setBounds(new GuiRect(innerX, top + 85 + (index - 8) * 9, innerWidth, 8));
            }
            int buttonHeight = 16;
            int buttonTop = detail.getBounds().getBottom() - buttonHeight - 3;
            children.get(12).setBounds(new GuiRect(innerX, top + 122, innerWidth,
                Math.max(0, buttonTop - top - 124)));
            children.get(13).setBounds(new GuiRect(innerX, buttonTop, innerWidth, buttonHeight));
        }
    }

    static int inspectorWidth(int contentWidth) {
        return Math.min(176, Math.max(84, Math.round(contentWidth * 0.24F)));
    }

    private long selectedVersion(int chunkX, int chunkZ) {
        return chunkX == model.getSelectedChunkX() && chunkZ == model.getSelectedChunkZ()
            ? model.getSelectedVersion() : 0L;
    }

    private static String tr(String key, Object... args) { return I18n.format(key, args); }
}
