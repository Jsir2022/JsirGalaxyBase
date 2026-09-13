package com.jsirgalaxybase.modules.warehouse.client.ui2;

import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentAction;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterTab;

public final class AssetCenterAction {
    public enum Type { APPLY_SNAPSHOT, SELECT_TAB, REQUEST_REFRESH, SET_PAGE, CELL_ACTION,
        SORT_VAULT, OPEN_HELP, CLOSE_HELP }
    private final Type type; private final TerminalAssetCenterTab tab;
    private final AssetActivitySnapshot activity; private final TerminalCellContentSnapshot cell;
    private final int page; private final int pageSize; private final TerminalCellContentAction cellAction;
    private final ItemStack target;
    private AssetCenterAction(Type type,TerminalAssetCenterTab tab,AssetActivitySnapshot activity,
        TerminalCellContentSnapshot cell,int page,int pageSize,TerminalCellContentAction cellAction,ItemStack target){
        this.type=type;this.tab=tab;this.activity=activity;this.cell=cell;this.page=page;this.pageSize=pageSize;
        this.cellAction=cellAction;this.target=target==null?null:target.copy();
    }
    public static AssetCenterAction simple(Type type){return new AssetCenterAction(type,null,null,null,0,20,null,null);}
    public static AssetCenterAction snapshot(AssetActivitySnapshot activity,TerminalCellContentSnapshot cell){return new AssetCenterAction(Type.APPLY_SNAPSHOT,null,activity,cell,cell==null?0:cell.getPageIndex(),cell==null?20:cell.getPageSize(),null,null);}
    public static AssetCenterAction tab(TerminalAssetCenterTab tab,int pageSize){return new AssetCenterAction(Type.SELECT_TAB,tab,null,null,0,pageSize,null,null);}
    public static AssetCenterAction refresh(int page,int pageSize){return new AssetCenterAction(Type.REQUEST_REFRESH,null,null,null,page,pageSize,null,null);}
    public static AssetCenterAction page(int page,int pageSize){return new AssetCenterAction(Type.SET_PAGE,null,null,null,page,pageSize,null,null);}
    public static AssetCenterAction cell(TerminalCellContentAction action,ItemStack target){return new AssetCenterAction(Type.CELL_ACTION,null,null,null,0,20,action,target);}
    public Type getType(){return type;}
    public TerminalAssetCenterTab getTab(){return tab;}
    public AssetActivitySnapshot getActivity(){return activity;}
    public TerminalCellContentSnapshot getCell(){return cell;}
    public int getPage(){return page;}
    public int getPageSize(){return pageSize;}
    public TerminalCellContentAction getCellAction(){return cellAction;}
    public ItemStack getTarget(){return target==null?null:target.copy();}
    @Override public String toString(){return "AssetCenterAction{"+type+"}";}
}
