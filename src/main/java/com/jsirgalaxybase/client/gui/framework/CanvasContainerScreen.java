package com.jsirgalaxybase.client.gui.framework;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.jsirgalaxybase.client.gui.theme.GuiTheme;
import com.jsirgalaxybase.client.gui.theme.TerminalThemeRegistry;

/** GuiContainer host for the same retained Canvas scene used by CanvasScreen. */
public abstract class CanvasContainerScreen<C extends Container> extends GuiContainer implements GuiScene {

    private final GuiScreen parentScreen;
    private final CanvasSceneRuntime sceneRuntime;

    protected CanvasContainerScreen(C container, GuiScreen parentScreen) {
        this(container, parentScreen, TerminalThemeRegistry.getDefaultTheme());
    }

    protected CanvasContainerScreen(C container, GuiScreen parentScreen, GuiTheme theme) {
        super(container);
        this.parentScreen = parentScreen;
        this.sceneRuntime = new CanvasSceneRuntime(theme);
    }

    @SuppressWarnings("unchecked")
    protected final C getTypedContainer() { return (C) inventorySlots; }

    @Override public GuiTheme getTheme() { return sceneRuntime.getTheme(); }

    @Override public void initGui() {
        configureContainerBounds();
        super.initGui();
        configureContainerOrigin();
        Keyboard.enableRepeatEvents(true);
        sceneRuntime.initialize(this, buildRootPanel());
    }

    @Override public void onGuiClosed() {
        sceneRuntime.close();
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        sceneRuntime.drawOverlays(this, mouseX, mouseY, partialTicks, width, height);
    }

    @Override public void drawDefaultBackground() {
        if (shouldDrawDefaultBackground()) super.drawDefaultBackground();
    }

    @Override protected final void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        sceneRuntime.drawBase(this, mouseX, mouseY, partialTicks);
        drawNativeContainerBackground(partialTicks, mouseX, mouseY);
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (sceneRuntime.hasOpenPopup()) {
            sceneRuntime.mouseClicked(this, mouseX, mouseY, mouseButton);
            return;
        }
        if (sceneRuntime.mouseClicked(this, mouseX, mouseY, mouseButton)) return;
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton) {
        if (sceneRuntime.hasOpenPopup()) {
            sceneRuntime.mouseReleased(this, mouseX, mouseY, mouseButton);
            return;
        }
        if (sceneRuntime.mouseReleased(this, mouseX, mouseY, mouseButton)) return;
        super.mouseMovedOrUp(mouseX, mouseY, mouseButton);
    }

    @Override public void handleMouseInput() {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta == 0) return;
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        sceneRuntime.mouseScrolled(this, mouseX, mouseY, delta);
    }

    @Override protected void keyTyped(char typedChar, int keyCode) {
        if (sceneRuntime.hasOpenPopup()) {
            if (keyCode == Keyboard.KEY_ESCAPE) closePopup();
            else sceneRuntime.keyTyped(this, typedChar, keyCode);
            return;
        }
        if (sceneRuntime.keyTyped(this, typedChar, keyCode)) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override public void openPopup(GuiPanel panel) { sceneRuntime.openPopup(this, panel); }
    @Override public void closePopup() { sceneRuntime.closePopup(); }
    protected final boolean hasOpenPopup() { return sceneRuntime.hasOpenPopup(); }
    @Override public void openHoverOverlay(GuiPanel panel) { sceneRuntime.openHoverOverlay(this, panel); }
    @Override public void closeHoverOverlay() { sceneRuntime.closeHoverOverlay(); }

    protected void closeToParent() {
        if (parentScreen == null) mc.thePlayer.closeScreen();
        else mc.displayGuiScreen(parentScreen);
    }

    protected void configureContainerOrigin() {}
    protected boolean shouldDrawDefaultBackground() { return true; }
    protected abstract void configureContainerBounds();
    protected abstract PanelContainer buildRootPanel();
    protected abstract void drawNativeContainerBackground(float partialTicks, int mouseX, int mouseY);
}
