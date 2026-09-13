package com.jsirgalaxybase.terminal.client.screen;

import net.minecraft.client.gui.GuiScreen;

import com.jsirgalaxybase.client.ui2.host.ModernScreenHost;
import com.jsirgalaxybase.terminal.client.TerminalRouteCoordinator;
import com.jsirgalaxybase.terminal.client.ui2.TerminalAppShell;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.network.TerminalActionMessage;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;
import com.jsirgalaxybase.terminal.ui.TerminalPage;
import com.jsirgalaxybase.terminal.client.settings.TerminalAppearance;
import com.jsirgalaxybase.ui2.theme.UiTheme;
import com.jsirgalaxybase.terminal.client.ui2.TerminalWindowMetrics;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;

public abstract class TerminalScreenV2Base extends ModernScreenHost {
    private TerminalHomeScreenModel shellModel;
    protected TerminalScreenV2Base(GuiScreen parent,TerminalHomeScreenModel model){super(parent);shellModel=model==null?TerminalHomeScreenModel.placeholder():model;}
    protected final TerminalHomeScreenModel shellModel(){return shellModel;}
    protected final void updateShellModel(TerminalHomeScreenModel model){shellModel=model==null?TerminalHomeScreenModel.placeholder():model;}
    protected final TerminalAppShell.Actions shellActions(){return new TerminalAppShell.Actions(){
        @Override public void navigate(String pageId){
            TerminalPage target=TerminalPage.fromId(pageId);
            if(target==TerminalPage.WAREHOUSE||target==TerminalPage.VAULT){
                TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(shellModel.getSessionToken(),
                    target.getId(),TerminalActionType.VAULT_OPEN.getId(),"ui2_nav"));
                return;
            }
            TerminalRouteCoordinator.openTerminalPage(pageId);
        }
        @Override public void refresh(){refreshPage();}
        @Override public void help(){showHelp();}
        @Override public void back(){
            if(TerminalPage.HOME.getId().equals(shellModel.getSelectedPageId())) TerminalRouteCoordinator.closeTerminal();
            else TerminalRouteCoordinator.openTerminalPage(TerminalPage.HOME.getId());
        }
        @Override public void close(){TerminalRouteCoordinator.closeTerminal();}
    };}
    protected abstract void refreshPage();
    protected abstract void showHelp();
    @Override public void drawDefaultBackground() {}
    @Override protected UiTheme uiTheme(){return TerminalAppearance.INSTANCE.theme();}
    @Override protected boolean isReducedMotion(){return TerminalAppearance.INSTANCE.preferences().isReducedMotion();}
    @Override protected UiRect uiClipBounds(){return TerminalWindowMetrics.compute(new UiSize(width,height)).getBounds();}
}
