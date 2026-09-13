package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.TerminalServerToolsActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalServerToolsSectionModel;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.state.UiStore;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;

public class TerminalServerToolsDocumentTest {
    @Test public void draftsAreSanitizedAndTabsStayLocal(){UiStore<ServerToolsUiState,ServerToolsUiAction> store=store();for(char c:"Player!_1".toCharArray())store.dispatch(ServerToolsUiAction.input(ServerToolsUiState.Field.TPA_PLAYER,c));assertEquals("Player_1",store.getState().getPlayerDraft());store.dispatch(ServerToolsUiAction.tab(ServerToolsUiState.Tab.TPA));assertEquals(ServerToolsUiState.Tab.TPA,store.getState().getTab());}
    @Test public void eachTabFitsProductionWindows(){for(ServerToolsUiState.Tab tab:ServerToolsUiState.Tab.values())for(int[] size:new int[][]{{350,193},{427,240},{620,340}}){UiStore<ServerToolsUiState,ServerToolsUiAction> store=store();store.dispatch(ServerToolsUiAction.tab(tab));UiRuntime runtime=new UiRuntime(document(store),context());runtime.setViewport(size[0],size[1]);UiRect window=find(runtime,"terminal-window");for(DrawCommand command:runtime.frame().ordered()){if(command.getKind()!=DrawCommand.Kind.TEXT)continue;UiRect b=command.getBounds();assertTrue(b.getX()>=window.getX());assertTrue(b.getY()>=window.getY());assertTrue(b.getRight()<=window.getRight());assertTrue(b.getBottom()<=window.getBottom());}}}
    @Test public void confirmationsKeepTypedActionAndPayload(){ServerToolsUiState.Confirmation confirmation=new ServerToolsUiState.Confirmation(TerminalActionType.SERVER_TOOLS_TPA_REQUEST,TerminalServerToolsActionPayload.forTpa("Alex","s2"),"title","detail","go");UiStore<ServerToolsUiState,ServerToolsUiAction> store=store();store.dispatch(ServerToolsUiAction.confirm(confirmation));assertEquals(TerminalActionType.SERVER_TOOLS_TPA_REQUEST,store.getState().getConfirmation().getAction());assertEquals("Alex",store.getState().getConfirmation().getPayload().getTpaPlayerName());}
    private static UiStore<ServerToolsUiState,ServerToolsUiAction> store(){return new UiStore<ServerToolsUiState,ServerToolsUiAction>(ServerToolsUiState.initial(TerminalServerToolsSectionModel.placeholder()),new ServerToolsUiReducer());}
    private static TerminalServerToolsDocument document(UiStore<ServerToolsUiState,ServerToolsUiAction> store){return new TerminalServerToolsDocument(store,TerminalHomeScreenModel.placeholder().withSelectedPageId("server_tools"),actions(),new TerminalServerToolsDocument.Actions(){public void send(TerminalActionType action,TerminalServerToolsActionPayload payload){}});}
    private static TerminalAppShell.Actions actions(){return new TerminalAppShell.Actions(){public void navigate(String pageId){}public void refresh(){}public void help(){}public void back(){}public void close(){}};}
    private static UiContext context(){return new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,false));}
    private static UiRect find(UiRuntime runtime,String key){runtime.frame();return find(runtime.getRoot(),key).getBounds();}
    private static com.jsirgalaxybase.ui2.core.UiNode find(com.jsirgalaxybase.ui2.core.UiNode node,String key){if(node.getElement().getKey()!=null&&key.equals(node.getElement().getKey().getValue()))return node;for(com.jsirgalaxybase.ui2.core.UiNode child:node.getChildren()){try{return find(child,key);}catch(AssertionError ignored){}}throw new AssertionError("missing "+key);}
}
