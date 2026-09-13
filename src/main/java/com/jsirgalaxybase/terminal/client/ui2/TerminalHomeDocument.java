package com.jsirgalaxybase.terminal.client.ui2;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.terminal.TerminalActionPort;
import com.jsirgalaxybase.ui2.terminal.TerminalHomeVisualDocument;

/** Minecraft compatibility adapter around the platform-neutral production home document. */
public final class TerminalHomeDocument extends TerminalHomeVisualDocument implements TerminalPageDocument {
    public TerminalHomeDocument(TerminalHomeScreenModel model,final TerminalAppShell.Actions actions){super(TerminalVisualModelAdapter.home(model),new TerminalActionPort(){@Override public void navigate(String pageId){actions.navigate(pageId);}@Override public void refresh(){actions.refresh();}@Override public void help(){actions.help();}@Override public void back(){actions.back();}@Override public void close(){actions.close();}},TerminalVisualModelAdapter.windowProfile());}
    @Override public void update(TerminalHomeScreenModel value){updateVisualModel(TerminalVisualModelAdapter.home(value));updateWindowProfile(TerminalVisualModelAdapter.windowProfile());}
    @Override public void openHelp(){openVisualHelp();}
}
