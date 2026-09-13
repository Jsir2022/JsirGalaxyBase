package com.jsirgalaxybase.modules.warehouse.client.ui2;

import com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft.BaseVaultSortRequestMessage;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterRequestMessage;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalCellContentActionMessage;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;
import com.jsirgalaxybase.ui2.state.UiEffect;

/** The only asset page layer allowed to emit business network effects. */
public final class AssetCenterEffect implements UiEffect<AssetCenterUiState,AssetCenterAction> {
    @Override public void afterDispatch(AssetCenterUiState previous,AssetCenterUiState current,AssetCenterAction action){
        switch(action.getType()){
            case SELECT_TAB:
            case REQUEST_REFRESH:
            case SET_PAGE:
                TerminalNetwork.CHANNEL.sendToServer(new TerminalAssetCenterRequestMessage(current.getTab(),
                    current.getRequestedPage(),Math.max(1,Math.min(35,action.getPageSize()))));break;
            case SORT_VAULT: TerminalNetwork.CHANNEL.sendToServer(new BaseVaultSortRequestMessage());break;
            case CELL_ACTION:
                TerminalNetwork.CHANNEL.sendToServer(new TerminalCellContentActionMessage(
                    TerminalCellContentActionMessage.nextRequestId(),current.getCell().getBayVersion(),
                    action.getCellAction(),action.getTarget()));break;
            default: break;
        }
    }
}
