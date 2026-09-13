package com.jsirgalaxybase.modules.warehouse.client.ui2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterTab;

public class AssetCenterReducerTest {
    @Test public void tabPageSnapshotAndHelpRemainSingleSourceOfViewState(){
        AssetCenterReducer reducer=new AssetCenterReducer();AssetCenterUiState state=AssetCenterUiState.initial(TerminalAssetCenterTab.STORAGE);
        state=reducer.reduce(state,AssetCenterAction.tab(TerminalAssetCenterTab.ACTIVITY,20));assertEquals(TerminalAssetCenterTab.ACTIVITY,state.getTab());assertEquals(0,state.getRequestedPage());
        state=reducer.reduce(state,AssetCenterAction.page(3,20));assertEquals(3,state.getRequestedPage());
        state=reducer.reduce(state,AssetCenterAction.snapshot(AssetActivitySnapshot.empty(),TerminalCellContentSnapshot.empty()));assertEquals(0,state.getRequestedPage());
        state=reducer.reduce(state,AssetCenterAction.simple(AssetCenterAction.Type.OPEN_HELP));assertTrue(state.isHelpOpen());
        state=reducer.reduce(state,AssetCenterAction.simple(AssetCenterAction.Type.CLOSE_HELP));assertFalse(state.isHelpOpen());
    }
}
