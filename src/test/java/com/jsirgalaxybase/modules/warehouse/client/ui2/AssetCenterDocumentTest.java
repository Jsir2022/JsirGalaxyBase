package com.jsirgalaxybase.modules.warehouse.client.ui2;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentEntry;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterTab;
import com.jsirgalaxybase.terminal.client.ui2.TerminalAppShell;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.state.UiStore;
import com.jsirgalaxybase.ui2.theme.DarkIndustrialTheme;

public class AssetCenterDocumentTest {
    @Test public void storageFitsAllAcceptanceSizesAndExposesNativeRegions(){
        for(int[] size:new int[][]{{350,193},{427,240},{620,340}}){
            UiStore<AssetCenterUiState,AssetCenterAction> store=store();AssetCenterDocument document=document(store);
            UiRuntime runtime=runtime(document,size[0],size[1]);DrawList list=runtime.frame();UiRect viewport=new UiRect(0,0,size[0],size[1]);
            UiRect window=com.jsirgalaxybase.terminal.client.ui2.TerminalWindowMetrics.compute(new com.jsirgalaxybase.ui2.geometry.UiSize(size[0],size[1])).getBounds();
            assertRegion(list,"asset:vault",window);assertRegion(list,"asset:player",window);assertRegion(list,"asset:bay",window);
            assertTreeInside(runtime.getRoot(),viewport);
        }
    }

    @Test public void installedCellProducesStableExternalItemAndHelpModal(){
        UiStore<AssetCenterUiState,AssetCenterAction> store=store();
        ItemStack iron=new ItemStack(new Item().setUnlocalizedName("test_iron"),1);TerminalCellContentSnapshot cell=new TerminalCellContentSnapshot(true,7L,65536L,32L,64L,1L,63L,0,1,20,"",Arrays.asList(new TerminalCellContentEntry(iron,6400L)));
        store.dispatch(AssetCenterAction.snapshot(AssetActivitySnapshot.empty(),cell));AssetCenterDocument document=document(store);UiRuntime runtime=runtime(document,427,240);DrawList list=runtime.frame();
        assertNotNull(document.externalItems().get("asset:cell:0"));assertRegion(list,"asset:cell:0",new UiRect(0,0,427,240));
        store.dispatch(AssetCenterAction.simple(AssetCenterAction.Type.OPEN_HELP));runtime.invalidate();list=runtime.frame();assertTrue(runtime.hasModal());assertTrue(hasLayer(list,400));
        assertTrue(runtime.dispatch(com.jsirgalaxybase.ui2.input.UiEvent.pointer(com.jsirgalaxybase.ui2.input.UiEvent.Type.POINTER_DOWN,213,120,0)));
        assertTrue(store.getState().isHelpOpen());
        assertTrue(runtime.dispatch(com.jsirgalaxybase.ui2.input.UiEvent.pointer(com.jsirgalaxybase.ui2.input.UiEvent.Type.POINTER_DOWN,1,1,0)));
        assertTrue(!store.getState().isHelpOpen());
    }

    private static UiStore<AssetCenterUiState,AssetCenterAction> store(){return new UiStore<AssetCenterUiState,AssetCenterAction>(AssetCenterUiState.initial(TerminalAssetCenterTab.STORAGE),new AssetCenterReducer());}
    private static AssetCenterDocument document(UiStore<AssetCenterUiState,AssetCenterAction> store){return new AssetCenterDocument(store,TerminalHomeScreenModel.placeholder(),new TerminalAppShell.Actions(){public void navigate(String pageId){}public void refresh(){}public void help(){}public void back(){}public void close(){}},new AssetCenterDocument.CursorAccess(){public ItemStack current(){return null;}});}
    private static UiRuntime runtime(AssetCenterDocument document,int width,int height){UiRuntime runtime=new UiRuntime(document,new UiContext(DarkIndustrialTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,false)));runtime.setViewport(width,height);return runtime;}
    private static void assertRegion(DrawList list,String id,UiRect viewport){for(DrawCommand command:list.ordered())if(command.getKind()==DrawCommand.Kind.EXTERNAL_REGION&&id.equals(command.getExternalId())){assertTrue(viewport.contains(command.getBounds()));assertTrue(command.getBounds().getWidth()>0);assertTrue(command.getBounds().getHeight()>0);return;}throw new AssertionError("missing region "+id);}
    private static boolean hasLayer(DrawList list,int layer){for(DrawCommand command:list.ordered())if(command.getLayer()>=layer)return true;return false;}
    private static void assertTreeInside(UiNode node,UiRect viewport){assertTrue(viewport.contains(node.getBounds()));for(UiNode child:node.getChildren())assertTreeInside(child,viewport);}
}
