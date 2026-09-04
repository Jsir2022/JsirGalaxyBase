package com.jsirgalaxybase.terminal.client.screen;

import net.minecraft.client.gui.Gui;

import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.GuiScene;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.client.gui.theme.ThemeColorKey;
import com.jsirgalaxybase.terminal.client.component.TerminalPanelFactory;
import com.jsirgalaxybase.terminal.client.component.TerminalShellPanels;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;

/** Composition helper shared by normal terminal screens and container-backed terminal screens. */
public final class TerminalShellFrame {

    private TerminalShellFrame() {}

    public static void addBackdrop(PanelContainer root, TerminalPanelFactory panels, int width, int height,
        TerminalHomeLayout layout) {
        root.addChild(new BackdropPanel(width, height, layout.getPanelBounds()));
        root.addChild(panels.createSurface(layout.getPanelBounds(), ThemeColorKey.PANEL_FILL));
    }

    public static void addStatusBand(PanelContainer root, TerminalPanelFactory panels, TerminalHomeLayout layout,
        TerminalHomeScreenModel model, Runnable refresh, Runnable info, Runnable back, Runnable close,
        Runnable accountCenter) {
        root.addChild(TerminalShellPanels.createStatusBand(panels, layout.getStatusBandBounds(), model,
            refresh, info, back, close, accountCenter));
    }

    public static void addNavigation(PanelContainer root, TerminalPanelFactory panels, TerminalHomeLayout layout,
        TerminalHomeScreenModel model, TerminalShellPanels.NavigationHandler handler) {
        root.addChild(TerminalShellPanels.createNavigationRail(panels, layout.getNavigationBounds(), model, handler));
    }

    private static final class BackdropPanel extends PanelContainer {
        private final GuiRect panelBounds;
        private BackdropPanel(int width, int height, GuiRect panelBounds) {
            setBounds(new GuiRect(0, 0, width, height));
            this.panelBounds = panelBounds == null ? new GuiRect(0, 0, 0, 0) : panelBounds;
        }
        @Override protected void drawSelf(GuiScene scene, int mouseX, int mouseY, float partialTicks) {
            GuiRect bounds = getBounds(); int overlay = 0x99000000;
            if (panelBounds.getY() > bounds.getY()) Gui.drawRect(bounds.getX(), bounds.getY(), bounds.getRight(), panelBounds.getY(), overlay);
            if (panelBounds.getBottom() < bounds.getBottom()) Gui.drawRect(bounds.getX(), panelBounds.getBottom(), bounds.getRight(), bounds.getBottom(), overlay);
            if (panelBounds.getX() > bounds.getX()) Gui.drawRect(bounds.getX(), panelBounds.getY(), panelBounds.getX(), panelBounds.getBottom(), overlay);
            if (panelBounds.getRight() < bounds.getRight()) Gui.drawRect(panelBounds.getRight(), panelBounds.getY(), bounds.getRight(), panelBounds.getBottom(), overlay);
        }
    }
}
