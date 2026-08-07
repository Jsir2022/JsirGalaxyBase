package com.jsirgalaxybase.terminal.client.screen;

import net.minecraft.client.Minecraft;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.config.ModConfiguration;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;

final class TerminalHomeLayout {

    private static final float FALLBACK_PANEL_WIDTH_RATIO = 0.72F;
    private static final float FALLBACK_PANEL_HEIGHT_RATIO = 0.44F;
    private static final float FALLBACK_NAVIGATION_WIDTH_RATIO = 0.12F;
    private static final int NAV_RAIL_MIN_WIDTH = 64;
    private static final int NAV_RAIL_MAX_WIDTH = 118;

    final GuiRect panelBounds;
    final GuiRect statusBandBounds;
    final GuiRect navigationBounds;
    final GuiRect bodyBounds;
    final boolean navigationVisible;
    final int navigationGap;

    private TerminalHomeLayout(GuiRect panelBounds, GuiRect statusBandBounds, GuiRect navigationBounds,
        GuiRect bodyBounds, boolean navigationVisible, int navigationGap) {
        this.panelBounds = panelBounds;
        this.statusBandBounds = statusBandBounds;
        this.navigationBounds = navigationBounds;
        this.bodyBounds = bodyBounds;
        this.navigationVisible = navigationVisible;
        this.navigationGap = navigationGap;
    }

    static TerminalHomeLayout compute(int screenWidth, int screenHeight) {
        return compute(screenWidth, screenHeight, null);
    }

    static TerminalHomeLayout compute(int screenWidth, int screenHeight, TerminalHomeScreenModel model) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        int marginX = safeWidth < 520 ? 6 : 8;
        int marginY = safeHeight < 360 ? 6 : 8;
        int maxPanelWidth = Math.max(1, safeWidth - marginX * 2);
        int maxPanelHeight = Math.max(1, safeHeight - marginY * 2);
        ModConfiguration configuration = GalaxyBase.proxy == null ? null : GalaxyBase.proxy.getConfiguration();
        float panelWidthRatio = Math.min(0.80F,
            configuration == null ? FALLBACK_PANEL_WIDTH_RATIO : configuration.getTerminalPanelWidthRatio());
        float panelHeightRatio = Math.min(0.54F,
            configuration == null ? FALLBACK_PANEL_HEIGHT_RATIO : configuration.getTerminalPanelHeightRatio());
        float navigationWidthRatio = configuration == null ? FALLBACK_NAVIGATION_WIDTH_RATIO
            : Math.max(0.11F, Math.min(0.14F, configuration.getTerminalNavigationWidthRatio()));
        float automaticPanelScale = computeAutomaticPanelScale(safeWidth, safeHeight);
        int maxPanelWidthCap = Math.round(980 * automaticPanelScale);
        int maxPanelHeightCap = Math.round(400 * automaticPanelScale);
        int panelWidth = clampFlexible(Math.round(safeWidth * panelWidthRatio * automaticPanelScale),
            Math.min(420, maxPanelWidth), Math.min(maxPanelWidthCap, maxPanelWidth));
        int panelHeight = clampFlexible(Math.round(safeHeight * panelHeightRatio * automaticPanelScale),
            Math.min(210, maxPanelHeight), Math.min(maxPanelHeightCap, maxPanelHeight));
        int panelX = (safeWidth - panelWidth) / 2;
        int hudReserve = clampFlexible(Math.round(56 + safeHeight * 0.03F), 64, 82);
        int panelY = Math.max(marginY, safeHeight - hudReserve - panelHeight);

        int innerPadding = safeHeight < 300 ? 2 : 3;
        int statusBandHeight = clampFlexible(Math.round(panelHeight * 0.05F),
            Math.min(13, Math.max(1, panelHeight / 15)), Math.min(15, panelHeight));
        int contentTop = panelY + statusBandHeight + innerPadding;
        int contentBottom = panelY + panelHeight - innerPadding;
        int contentHeight = Math.max(40, contentBottom - contentTop);
        int minRailWidth = panelWidth < 440 ? NAV_RAIL_MIN_WIDTH : panelWidth < 720 ? 76 : 86;
        int railWidth = clampFlexible(Math.round(panelWidth * navigationWidthRatio),
            minRailWidth, Math.min(NAV_RAIL_MAX_WIDTH, Math.max(minRailWidth, panelWidth / 4)));
        int navGap = 1;
        int railX = panelX + 1;
        int bodyX = railX + railWidth + navGap;
        int bodyRight = panelX + panelWidth - 1;
        int bodyWidth = Math.max(64, bodyRight - bodyX);
        if (bodyWidth < 64) {
            bodyX = railX;
            bodyWidth = Math.max(64, bodyRight - bodyX);
            navGap = 0;
        }
        return new TerminalHomeLayout(
            new GuiRect(panelX, panelY, panelWidth, panelHeight),
            new GuiRect(panelX + 1, panelY + 1, Math.max(1, panelWidth - 2), statusBandHeight),
            new GuiRect(railX, contentTop, railWidth, contentHeight),
            new GuiRect(bodyX, contentTop, bodyWidth, contentHeight),
            true,
            navGap);
    }

    private static float computeAutomaticPanelScale(int screenWidth, int screenHeight) {
        float scale = screenWidth >= 1100 || screenHeight >= 700 ? 0.90F
            : screenWidth >= 900 || screenHeight >= 520 ? 0.86F : 0.82F;
        float displayScale = getClientDisplayScale(screenWidth, screenHeight);
        if (displayScale > 1.1F) {
            scale = Math.min(scale, 0.98F - Math.min(0.14F, (displayScale - 1.0F) * 0.10F));
        }
        return Math.max(0.80F, Math.min(0.92F, scale));
    }

    private static float getClientDisplayScale(int screenWidth, int screenHeight) {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || screenWidth <= 0 || screenHeight <= 0) {
                return 1.0F;
            }
            float widthScale = minecraft.displayWidth <= 0 ? 1.0F : minecraft.displayWidth / (float) screenWidth;
            float heightScale = minecraft.displayHeight <= 0 ? 1.0F : minecraft.displayHeight / (float) screenHeight;
            return Math.max(widthScale, heightScale);
        } catch (LinkageError error) {
            return 1.0F;
        } catch (RuntimeException exception) {
            return 1.0F;
        }
    }

    private static int clampFlexible(int value, int min, int max) {
        int safeMax = Math.max(1, max);
        int safeMin = Math.max(1, Math.min(min, safeMax));
        return Math.max(safeMin, Math.min(safeMax, value));
    }
}
