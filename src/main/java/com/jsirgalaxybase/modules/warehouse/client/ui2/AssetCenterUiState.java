package com.jsirgalaxybase.modules.warehouse.client.ui2;

import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterTab;

public final class AssetCenterUiState {
    private final TerminalAssetCenterTab tab;
    private final AssetActivitySnapshot activity;
    private final TerminalCellContentSnapshot cell;
    private final int requestedPage;
    private final boolean helpOpen;

    public AssetCenterUiState(TerminalAssetCenterTab tab,AssetActivitySnapshot activity,
        TerminalCellContentSnapshot cell,int requestedPage,boolean helpOpen){
        this.tab=tab==null?TerminalAssetCenterTab.STORAGE:tab;
        this.activity=activity==null?AssetActivitySnapshot.empty():activity;
        this.cell=cell==null?TerminalCellContentSnapshot.empty():cell;
        this.requestedPage=Math.max(0,requestedPage);this.helpOpen=helpOpen;
    }
    public static AssetCenterUiState initial(TerminalAssetCenterTab tab){return new AssetCenterUiState(tab,
        AssetActivitySnapshot.empty(),TerminalCellContentSnapshot.empty(),0,false);}
    public TerminalAssetCenterTab getTab(){return tab;}
    public AssetActivitySnapshot getActivity(){return activity;}
    public TerminalCellContentSnapshot getCell(){return cell;}
    public int getRequestedPage(){return requestedPage;}
    public boolean isHelpOpen(){return helpOpen;}
}
