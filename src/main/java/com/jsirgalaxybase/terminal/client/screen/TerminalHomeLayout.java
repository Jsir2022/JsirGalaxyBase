package com.jsirgalaxybase.terminal.client.screen;

import com.jsirgalaxybase.client.gui.framework.GuiRect;

final class TerminalHomeLayout {

    private static final float PANEL_WIDTH_RATIO = 0.88F;
    private static final float PANEL_HEIGHT_RATIO = 0.86F;

    final GuiRect panelBounds;
    final GuiRect statusBandBounds;
    final GuiRect navigationBounds;
    final GuiRect bodyBounds;

    private TerminalHomeLayout(GuiRect panelBounds, GuiRect statusBandBounds, GuiRect navigationBounds,
        GuiRect bodyBounds) {
        this.panelBounds = panelBounds;
        this.statusBandBounds = statusBandBounds;
        this.navigationBounds = navigationBounds;
        this.bodyBounds = bodyBounds;
    }

    static TerminalHomeLayout compute(int screenWidth, int screenHeight) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int marginX = safeWidth < 520 ? 8 : 12;
        int marginY = safeHeight < 360 ? 8 : 14;
        int maxPanelWidth = Math.max(1, safeWidth - marginX * 2);
        int maxPanelHeight = Math.max(1, safeHeight - marginY * 2);
        int panelWidth = clampFlexible(Math.round(safeWidth * PANEL_WIDTH_RATIO), Math.min(468, maxPanelWidth),
            Math.min(920, maxPanelWidth));
        int panelHeight = clampFlexible(Math.round(safeHeight * PANEL_HEIGHT_RATIO), Math.min(320, maxPanelHeight),
            Math.min(680, maxPanelHeight));
        int panelX = (safeWidth - panelWidth) / 2;
        int panelY = (safeHeight - panelHeight) / 2;

        int innerPadding = safeHeight < 300 ? 8 : 12;
        int statusBandHeight = clampFlexible(Math.round(panelHeight * 0.15F),
            Math.min(38, Math.max(1, panelHeight / 4)), Math.min(50, panelHeight));
        int contentGap = safeWidth < 520 ? 10 : 14;
        int railWidth = clampFlexible(Math.round(panelWidth * 0.24F),
            Math.min(88, Math.max(1, panelWidth / 3)), Math.min(118, Math.max(1, panelWidth / 2)));
        int contentTop = panelY + innerPadding + statusBandHeight + 10;
        int contentBottom = panelY + panelHeight - innerPadding;
        int contentHeight = Math.max(40, contentBottom - contentTop);
        int railX = panelX + innerPadding;
        int railRight = railX + railWidth;
        int bodyX = railRight + contentGap;
        int bodyRight = panelX + panelWidth - innerPadding;
        int bodyWidth = Math.max(64, bodyRight - bodyX);
        if (bodyX + bodyWidth > bodyRight) {
            bodyWidth = Math.max(1, bodyRight - bodyX);
        }
        return new TerminalHomeLayout(
            new GuiRect(panelX, panelY, panelWidth, panelHeight),
            new GuiRect(panelX + innerPadding, panelY + innerPadding, Math.max(1, panelWidth - innerPadding * 2),
                statusBandHeight),
            new GuiRect(railX, contentTop, railWidth, contentHeight),
            new GuiRect(bodyX, contentTop, bodyWidth, contentHeight));
    }

    private static int clampFlexible(int value, int min, int max) {
        int safeMax = Math.max(1, max);
        int safeMin = Math.max(1, Math.min(min, safeMax));
        return Math.max(safeMin, Math.min(safeMax, value));
    }
}
