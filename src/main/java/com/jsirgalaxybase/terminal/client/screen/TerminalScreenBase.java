package com.jsirgalaxybase.terminal.client.screen;

import net.minecraft.client.gui.GuiScreen;

import com.jsirgalaxybase.client.gui.framework.CanvasScreen;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;

/** Common terminal policy for non-inventory screens. */
public abstract class TerminalScreenBase extends CanvasScreen {

    protected TerminalScreenBase(GuiScreen parentScreen) { super(parentScreen); }

    protected final TerminalHomeLayout terminalLayout(TerminalHomeScreenModel model) {
        return TerminalHomeLayout.compute(width, height, model);
    }

    @Override protected boolean shouldDrawDefaultBackground() { return false; }
}
