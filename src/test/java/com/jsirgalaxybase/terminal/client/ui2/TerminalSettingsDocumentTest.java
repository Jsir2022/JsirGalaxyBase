package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.settings.TerminalPreferences;
import com.jsirgalaxybase.terminal.client.settings.TerminalSettingsAction;
import com.jsirgalaxybase.terminal.client.settings.TerminalSettingsReducer;
import com.jsirgalaxybase.terminal.client.settings.TerminalSettingsState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.state.UiStore;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;

public class TerminalSettingsDocumentTest {
    @Test public void settingsFitsAllAcceptanceSizesAndSelectsOnlySettingsNav(){
        for(int[] size:new int[][]{{350,193},{427,240},{620,340}}){
            UiStore<TerminalSettingsState,TerminalSettingsAction> store=store();
            UiRuntime runtime=new UiRuntime(new TerminalSettingsDocument(store,TerminalHomeScreenModel.placeholder(),actions()),context());
            runtime.setViewport(size[0],size[1]);
            for(DrawCommand command:runtime.frame().ordered()){
                if(command.getKind()!=DrawCommand.Kind.TEXT)continue;
                UiRect bounds=command.getBounds();assertTrue(bounds.getX()>=0);assertTrue(bounds.getY()>=0);
                assertTrue(bounds.getRight()<=size[0]);assertTrue(bounds.getBottom()<=size[1]);
            }
            UiNode settings=find(runtime.getRoot(),"terminal-nav-settings");
            assertEquals(Boolean.TRUE,settings.getElement().getProp(com.jsirgalaxybase.ui2.component.StandardWidgets.SELECTED));
            UiNode home=find(runtime.getRoot(),"terminal-nav-home");
            assertEquals(Boolean.FALSE,home.getElement().getProp(com.jsirgalaxybase.ui2.component.StandardWidgets.SELECTED));
            assertTrue(find(runtime.getRoot(),"settings-font-label").getElement().getProp(com.jsirgalaxybase.ui2.component.StandardWidgets.TEXT).toString().contains("Minecraft"));
        }
    }
    @Test public void reducerAppliesDiscreteChoicesAndReset(){
        UiStore<TerminalSettingsState,TerminalSettingsAction> store=store();
        store.dispatch(TerminalSettingsAction.value(TerminalSettingsAction.Type.WINDOW,com.jsirgalaxybase.terminal.client.settings.TerminalWindowSize.LARGE.name()));
        assertEquals(com.jsirgalaxybase.terminal.client.settings.TerminalWindowSize.LARGE,store.getState().getPreferences().getWindowSize());
        store.dispatch(TerminalSettingsAction.of(TerminalSettingsAction.Type.RESET));
        assertEquals(TerminalPreferences.defaults(),store.getState().getPreferences());
    }
    private static UiStore<TerminalSettingsState,TerminalSettingsAction> store(){return new UiStore<TerminalSettingsState,TerminalSettingsAction>(new TerminalSettingsState(TerminalPreferences.defaults(),false),new TerminalSettingsReducer());}
    private static UiContext context(){return new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,false));}
    private static TerminalAppShell.Actions actions(){return new TerminalAppShell.Actions(){public void navigate(String pageId){}public void refresh(){}public void help(){}public void back(){}public void close(){}};}
    private static UiNode find(UiNode node,String key){if(node.getElement().getKey()!=null&&key.equals(node.getElement().getKey().getValue()))return node;for(UiNode child:node.getChildren()){UiNode found=findOrNull(child,key);if(found!=null)return found;}throw new AssertionError("missing "+key);}
    private static UiNode findOrNull(UiNode node,String key){if(node.getElement().getKey()!=null&&key.equals(node.getElement().getKey().getValue()))return node;for(UiNode child:node.getChildren()){UiNode found=findOrNull(child,key);if(found!=null)return found;}return null;}
}
