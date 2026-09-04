package com.jsirgalaxybase.client.gui.framework;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import com.jsirgalaxybase.client.gui.theme.GuiTheme;
import com.jsirgalaxybase.client.gui.theme.TerminalThemeRegistry;

public abstract class CanvasScreen extends GuiScreen implements GuiScene {

    private final GuiScreen parentScreen;
    private final CanvasSceneRuntime sceneRuntime;

    protected CanvasScreen(GuiScreen parentScreen) {
        this(parentScreen, TerminalThemeRegistry.getDefaultTheme());
    }

    protected CanvasScreen(GuiScreen parentScreen, GuiTheme theme) {
        this.parentScreen = parentScreen;
        this.sceneRuntime = new CanvasSceneRuntime(theme);
    }

    @Override
    public GuiTheme getTheme() {
        return sceneRuntime.getTheme();
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        // A rebuilt root can represent a different terminal route. Hover panels belong
        // to the old pointer target and must never survive that route transition.
        sceneRuntime.initialize(this, buildRootPanel());
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        sceneRuntime.close();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (shouldDrawDefaultBackground()) {
            drawDefaultBackground();
        }
        sceneRuntime.drawBase(this, mouseX, mouseY, partialTicks);
        sceneRuntime.drawOverlays(this, mouseX, mouseY, partialTicks, width, height);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        sceneRuntime.mouseClicked(this, mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton) {
        sceneRuntime.mouseReleased(this, mouseX, mouseY, mouseButton);
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
        sceneRuntime.mouseScrolled(this, mouseX, mouseY, wheelDelta);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (sceneRuntime.hasOpenPopup()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                closePopup();
                return;
            }
            sceneRuntime.keyTyped(this, typedChar, keyCode);
            return;
        }
        if (sceneRuntime.keyTyped(this, typedChar, keyCode)) return;

        if (keyCode == Keyboard.KEY_ESCAPE) {
            closeScreen();
        }
    }

    @Override
    public void openPopup(GuiPanel panel) {
        sceneRuntime.openPopup(this, panel);
    }

    @Override
    public void closePopup() {
        sceneRuntime.closePopup();
    }

    protected final boolean hasOpenPopup() {
        return sceneRuntime.hasOpenPopup();
    }

    @Override
    public void openHoverOverlay(GuiPanel panel) {
        sceneRuntime.openHoverOverlay(this, panel);
    }

    @Override
    public void closeHoverOverlay() {
        sceneRuntime.closeHoverOverlay();
    }

    protected void closeScreen() {
        mc.displayGuiScreen(parentScreen);
    }

    protected boolean shouldDrawDefaultBackground() {
        return true;
    }

    protected abstract PanelContainer buildRootPanel();
}
