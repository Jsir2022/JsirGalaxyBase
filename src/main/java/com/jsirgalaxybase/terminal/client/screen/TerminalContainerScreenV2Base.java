package com.jsirgalaxybase.terminal.client.screen;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;

import com.jsirgalaxybase.client.ui2.host.ModernContainerHost;
import com.jsirgalaxybase.terminal.client.TerminalRouteCoordinator;
import com.jsirgalaxybase.terminal.client.ui2.TerminalAppShell;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.ui.TerminalPage;
import com.jsirgalaxybase.terminal.client.settings.TerminalAppearance;
import com.jsirgalaxybase.ui2.theme.UiTheme;
import com.jsirgalaxybase.terminal.client.ui2.TerminalWindowMetrics;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;

public abstract class TerminalContainerScreenV2Base<C extends Container> extends ModernContainerHost<C> {
    private final TerminalHomeScreenModel shellModel;
    protected TerminalContainerScreenV2Base(C container,GuiScreen parent,TerminalHomeScreenModel model){
        super(container,parent); shellModel=model==null?TerminalHomeScreenModel.placeholder():model;
    }
    protected final TerminalHomeScreenModel shellModel(){return shellModel;}
    protected final TerminalAppShell.Actions shellActions(){return new TerminalAppShell.Actions(){
        @Override public void navigate(String pageId){if(!TerminalPage.WAREHOUSE.getId().equals(pageId))TerminalRouteCoordinator.openTerminalPage(pageId);}
        @Override public void refresh(){refreshPage();}
        @Override public void help(){showHelp();}
        @Override public void back(){TerminalRouteCoordinator.openTerminalPage(TerminalPage.HOME.getId());}
        @Override public void close(){TerminalRouteCoordinator.closeTerminal();}
    };}
    protected abstract void refreshPage();
    protected abstract void showHelp();
    @Override public void drawDefaultBackground() {}
    @Override protected UiTheme uiTheme(){return TerminalAppearance.INSTANCE.theme();}
    @Override protected boolean isReducedMotion(){return TerminalAppearance.INSTANCE.preferences().isReducedMotion();}
    @Override protected UiRect uiClipBounds(){return TerminalWindowMetrics.compute(new UiSize(width,height)).getBounds();}
}
