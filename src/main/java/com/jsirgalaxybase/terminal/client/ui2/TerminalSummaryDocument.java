package com.jsirgalaxybase.terminal.client.ui2;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.terminal.SummaryVisualDocument;
import com.jsirgalaxybase.ui2.terminal.TerminalActionPort;

/** Minecraft model adapter for the shared summary document. */
public final class TerminalSummaryDocument extends SummaryVisualDocument implements TerminalPageDocument {
    private final String pageId;
    public TerminalSummaryDocument(String pageId,TerminalHomeScreenModel model,final TerminalAppShell.Actions actions){super(TerminalVisualModelAdapter.summary(pageId,model),TerminalVisualModelAdapter.windowProfile(),new TerminalActionPort(){public void navigate(String id){actions.navigate(id);}public void refresh(){actions.refresh();}public void help(){actions.help();}public void back(){actions.back();}public void close(){actions.close();}});this.pageId=pageId;}
    @Override public void update(TerminalHomeScreenModel value){updateVisualModel(TerminalVisualModelAdapter.summary(pageId,value));updateWindowProfile(TerminalVisualModelAdapter.windowProfile());}
    @Override public void openHelp(){}
}
