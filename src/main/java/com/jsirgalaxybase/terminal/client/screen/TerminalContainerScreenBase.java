package com.jsirgalaxybase.terminal.client.screen;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;

import com.jsirgalaxybase.client.gui.framework.CanvasContainerScreen;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;

/** Common terminal policy and coordinates for native inventory screens. */
public abstract class TerminalContainerScreenBase<C extends Container> extends CanvasContainerScreen<C> {

    private final TerminalHomeScreenModel shellModel;
    private TerminalHomeLayout terminalLayout;

    protected TerminalContainerScreenBase(C container, GuiScreen parentScreen, TerminalHomeScreenModel shellModel) {
        super(container, parentScreen);
        this.shellModel = shellModel == null ? TerminalHomeScreenModel.placeholder() : shellModel;
    }

    protected final TerminalHomeScreenModel getShellModel() { return shellModel; }
    protected final TerminalHomeLayout getTerminalLayout() { return terminalLayout; }

    @Override protected final void configureContainerBounds() {
        terminalLayout = TerminalHomeLayout.compute(width, height, shellModel);
        xSize = terminalLayout.getPanelBounds().getWidth();
        ySize = terminalLayout.getPanelBounds().getHeight();
    }

    @Override protected final void configureContainerOrigin() {
        guiLeft = terminalLayout.getPanelBounds().getX();
        guiTop = terminalLayout.getPanelBounds().getY();
    }

    @Override public boolean doesGuiPauseGame() { return false; }
    @Override protected boolean shouldDrawDefaultBackground() { return false; }
}
