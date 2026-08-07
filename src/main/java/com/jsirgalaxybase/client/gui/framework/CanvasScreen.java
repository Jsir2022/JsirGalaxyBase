package com.jsirgalaxybase.client.gui.framework;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.client.gui.theme.GuiTheme;
import com.jsirgalaxybase.client.gui.theme.TerminalThemeRegistry;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;

public abstract class CanvasScreen extends GuiScreen implements GuiScene {

    private final GuiScreen parentScreen;
    private final GuiTheme theme;
    private PanelContainer rootPanel;
    private GuiPanel popupPanel;
    private GuiPanel hoverOverlay;

    protected CanvasScreen(GuiScreen parentScreen) {
        this(parentScreen, TerminalThemeRegistry.getDefaultTheme());
    }

    protected CanvasScreen(GuiScreen parentScreen, GuiTheme theme) {
        this.parentScreen = parentScreen;
        this.theme = theme;
    }

    @Override
    public GuiTheme getTheme() {
        return theme;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        // A rebuilt root can represent a different terminal route. Hover panels belong
        // to the old pointer target and must never survive that route transition.
        closeHoverOverlay();
        rootPanel = buildRootPanel();
        if (rootPanel != null) {
            rootPanel.init(this);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        closeHoverOverlay();
        closePopup();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (shouldDrawDefaultBackground()) {
            drawDefaultBackground();
        }
        if (rootPanel != null) {
            rootPanel.draw(this, mouseX, mouseY, partialTicks);
        }
        if (hoverOverlay != null && hoverOverlay.isVisible() && popupPanel == null) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            hoverOverlay.draw(this, mouseX, mouseY, partialTicks);
        }
        if (popupPanel != null && popupPanel.isVisible()) {
            // RenderItem writes depth while drawing real ItemStacks. A modal must always be
            // composited above that layer, otherwise icons from the underlying page bleed through.
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            Gui.drawRect(0, 0, width, height, theme.color(ThemeColorKey.SCREEN_OVERLAY));
            popupPanel.draw(this, mouseX, mouseY, partialTicks);
            GL11.glDepthMask(true);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        closeHoverOverlay();
        if (popupPanel != null && popupPanel.isVisible()) {
            popupPanel.mouseClicked(this, mouseX, mouseY, mouseButton);
            return;
        }
        if (rootPanel != null) {
            rootPanel.mouseClicked(this, mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton) {
        if (popupPanel != null && popupPanel.isVisible()) {
            popupPanel.mouseReleased(this, mouseX, mouseY, mouseButton);
            return;
        }
        if (rootPanel != null) {
            rootPanel.mouseReleased(this, mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheelDelta = Mouse.getEventDWheel();
        if (wheelDelta == 0) {
            return;
        }
        closeHoverOverlay();

        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (popupPanel != null && popupPanel.isVisible()) {
            popupPanel.mouseScrolled(this, mouseX, mouseY, wheelDelta);
            return;
        }
        if (rootPanel != null) {
            rootPanel.mouseScrolled(this, mouseX, mouseY, wheelDelta);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (popupPanel != null && popupPanel.isVisible()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                closePopup();
                return;
            }
            popupPanel.keyTyped(this, typedChar, keyCode);
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            closeScreen();
            return;
        }

        if (rootPanel != null) {
            rootPanel.keyTyped(this, typedChar, keyCode);
        }
    }

    @Override
    public void openPopup(GuiPanel panel) {
        closeHoverOverlay();
        this.popupPanel = panel;
        if (this.popupPanel != null) {
            this.popupPanel.init(this);
        }
    }

    @Override
    public void closePopup() {
        this.popupPanel = null;
    }

    @Override
    public void openHoverOverlay(GuiPanel panel) {
        if (popupPanel != null && popupPanel.isVisible()) {
            return;
        }
        this.hoverOverlay = panel;
        if (hoverOverlay != null) {
            hoverOverlay.init(this);
        }
    }

    @Override
    public void closeHoverOverlay() {
        this.hoverOverlay = null;
    }

    protected void closeScreen() {
        mc.displayGuiScreen(parentScreen);
    }

    protected boolean shouldDrawDefaultBackground() {
        return true;
    }

    protected abstract PanelContainer buildRootPanel();
}
