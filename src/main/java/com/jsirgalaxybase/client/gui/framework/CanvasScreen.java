package com.jsirgalaxybase.client.gui.framework;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.jsirgalaxybase.client.gui.theme.GuiTheme;
import com.jsirgalaxybase.client.gui.theme.TerminalThemeRegistry;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;

public abstract class CanvasScreen extends GuiScreen implements GuiScene {

    private final GuiScreen parentScreen;
    private final GuiTheme theme;
    private PanelContainer rootPanel;
    private GuiPanel popupPanel;

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
        rootPanel = buildRootPanel();
        if (rootPanel != null) {
            rootPanel.init(this);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        if (rootPanel != null) {
            rootPanel.draw(this, mouseX, mouseY, partialTicks);
        }
        if (popupPanel != null && popupPanel.isVisible()) {
            Gui.drawRect(0, 0, width, height, theme.color(ThemeColorKey.SCREEN_OVERLAY));
            popupPanel.draw(this, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
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
        this.popupPanel = panel;
        if (this.popupPanel != null) {
            this.popupPanel.init(this);
        }
    }

    @Override
    public void closePopup() {
        this.popupPanel = null;
    }

    protected void closeScreen() {
        mc.displayGuiScreen(parentScreen);
    }

    protected abstract PanelContainer buildRootPanel();
}