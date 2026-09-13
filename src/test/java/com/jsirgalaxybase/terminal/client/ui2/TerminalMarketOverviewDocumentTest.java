package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;

public class TerminalMarketOverviewDocumentTest {
    @Test public void marketLaunchpadFitsAndExposesAllWorkflows(){for(int[] size:new int[][]{{350,193},{427,240},{620,340}}){UiRuntime runtime=new UiRuntime(new TerminalMarketOverviewDocument(TerminalHomeScreenModel.placeholder().withSelectedPageId("market"),actions(),new TerminalNotificationDocument.TargetHandler(){public void open(String pageId,String recordId){}}),context());runtime.setViewport(size[0],size[1]);UiRect window=find(runtime,"terminal-window");find(runtime,"market-overview-standard");find(runtime,"market-overview-custom");find(runtime,"market-overview-exchange");find(runtime,"market-overview-account");for(DrawCommand command:runtime.frame().ordered()){if(command.getKind()!=DrawCommand.Kind.TEXT)continue;UiRect b=command.getBounds();assertTrue(b.getX()>=window.getX());assertTrue(b.getY()>=window.getY());assertTrue(b.getRight()<=window.getRight());assertTrue(b.getBottom()<=window.getBottom());}}}
    private static TerminalAppShell.Actions actions(){return new TerminalAppShell.Actions(){public void navigate(String pageId){}public void refresh(){}public void help(){}public void back(){}public void close(){}};}
    private static UiContext context(){return new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,false));}
    private static UiRect find(UiRuntime runtime,String key){runtime.frame();return find(runtime.getRoot(),key).getBounds();}
    private static com.jsirgalaxybase.ui2.core.UiNode find(com.jsirgalaxybase.ui2.core.UiNode node,String key){if(node.getElement().getKey()!=null&&key.equals(node.getElement().getKey().getValue()))return node;for(com.jsirgalaxybase.ui2.core.UiNode child:node.getChildren()){try{return find(child,key);}catch(AssertionError ignored){}}throw new AssertionError("missing "+key);}
}
