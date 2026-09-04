package com.jsirgalaxybase.client.gui.framework;

import net.minecraft.client.gui.Gui;

import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.client.gui.theme.GuiTheme;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;

/** Shared retained-scene lifecycle used by both GuiScreen and GuiContainer hosts. */
public final class CanvasSceneRuntime {

    private final GuiTheme theme;
    private PanelContainer rootPanel;
    private GuiPanel popupPanel;
    private GuiPanel hoverOverlay;

    public CanvasSceneRuntime(GuiTheme theme) {
        if (theme == null) throw new IllegalArgumentException("theme is required");
        this.theme = theme;
    }

    public GuiTheme getTheme() { return theme; }

    public void initialize(GuiScene scene, PanelContainer root) {
        closeHoverOverlay();
        rootPanel = root;
        if (rootPanel != null) rootPanel.init(scene);
    }

    public void close() {
        rootPanel = null;
        closeHoverOverlay();
        closePopup();
    }

    public void drawBase(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
        if (rootPanel != null) rootPanel.draw(scene, mouseX, mouseY, partialTicks);
    }

    public void drawOverlays(GuiScene scene, int mouseX, int mouseY, float partialTicks, int width, int height) {
        if (hoverOverlay != null && hoverOverlay.isVisible() && !hasOpenPopup()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            hoverOverlay.draw(scene, mouseX, mouseY, partialTicks);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        if (hasOpenPopup()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            Gui.drawRect(0, 0, width, height, theme.color(ThemeColorKey.SCREEN_OVERLAY));
            popupPanel.draw(scene, mouseX, mouseY, partialTicks);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
    }

    public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int button) {
        closeHoverOverlay();
        if (hasOpenPopup()) return popupPanel.mouseClicked(scene, mouseX, mouseY, button);
        return rootPanel != null && rootPanel.mouseClicked(scene, mouseX, mouseY, button);
    }

    public boolean mouseReleased(GuiScene scene, int mouseX, int mouseY, int button) {
        if (hasOpenPopup()) return popupPanel.mouseReleased(scene, mouseX, mouseY, button);
        return rootPanel != null && rootPanel.mouseReleased(scene, mouseX, mouseY, button);
    }

    public boolean mouseScrolled(GuiScene scene, int mouseX, int mouseY, int delta) {
        closeHoverOverlay();
        if (hasOpenPopup()) return popupPanel.mouseScrolled(scene, mouseX, mouseY, delta);
        return rootPanel != null && rootPanel.mouseScrolled(scene, mouseX, mouseY, delta);
    }

    public boolean keyTyped(GuiScene scene, char typedChar, int keyCode) {
        if (hasOpenPopup()) return popupPanel.keyTyped(scene, typedChar, keyCode);
        return rootPanel != null && rootPanel.keyTyped(scene, typedChar, keyCode);
    }

    public void openPopup(GuiScene scene, GuiPanel panel) {
        closeHoverOverlay();
        popupPanel = panel;
        if (popupPanel != null) popupPanel.init(scene);
    }

    public void closePopup() { popupPanel = null; }
    public boolean hasOpenPopup() { return popupPanel != null && popupPanel.isVisible(); }

    public void openHoverOverlay(GuiScene scene, GuiPanel panel) {
        if (hasOpenPopup()) return;
        hoverOverlay = panel;
        if (hoverOverlay != null) hoverOverlay.init(scene);
    }

    public void closeHoverOverlay() { hoverOverlay = null; }
    boolean hasHoverOverlay() { return hoverOverlay != null && hoverOverlay.isVisible(); }
}
