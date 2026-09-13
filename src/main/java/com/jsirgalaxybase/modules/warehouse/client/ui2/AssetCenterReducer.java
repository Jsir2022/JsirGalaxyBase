package com.jsirgalaxybase.modules.warehouse.client.ui2;

import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterTab;
import com.jsirgalaxybase.ui2.state.UiReducer;

public final class AssetCenterReducer implements UiReducer<AssetCenterUiState,AssetCenterAction> {
    @Override public AssetCenterUiState reduce(AssetCenterUiState state,AssetCenterAction action){
        if(action==null)return state;
        TerminalAssetCenterTab tab=state.getTab();AssetActivitySnapshot activity=state.getActivity();
        TerminalCellContentSnapshot cell=state.getCell();int page=state.getRequestedPage();boolean help=state.isHelpOpen();
        switch(action.getType()){
            case APPLY_SNAPSHOT: activity=action.getActivity();cell=action.getCell();page=cell==null?0:cell.getPageIndex();break;
            case SELECT_TAB: tab=action.getTab()==null?TerminalAssetCenterTab.STORAGE:action.getTab();page=0;break;
            case SET_PAGE: page=Math.max(0,action.getPage());break;
            case OPEN_HELP: help=true;break;
            case CLOSE_HELP: help=false;break;
            default: break;
        }
        return new AssetCenterUiState(tab,activity,cell,page,help);
    }
}
