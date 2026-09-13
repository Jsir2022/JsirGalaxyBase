package com.jsirgalaxybase.modules.warehouse.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseCellInspector;
import com.jsirgalaxybase.modules.warehouse.client.ui2.AssetCenterAction;
import com.jsirgalaxybase.modules.warehouse.client.ui2.AssetCenterDocument;
import com.jsirgalaxybase.modules.warehouse.client.ui2.AssetCenterEffect;
import com.jsirgalaxybase.modules.warehouse.client.ui2.AssetCenterReducer;
import com.jsirgalaxybase.modules.warehouse.client.ui2.AssetCenterUiState;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterContainer;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterTab;
import com.jsirgalaxybase.terminal.TerminalHudOverlayHandler;
import com.jsirgalaxybase.terminal.client.TerminalClientScreenController;
import com.jsirgalaxybase.terminal.client.screen.TerminalContainerScreenV2Base;
import com.jsirgalaxybase.terminal.ui.TerminalPage;
import com.jsirgalaxybase.ui2.core.UiDocument;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.state.UiEffect;
import com.jsirgalaxybase.ui2.state.UiStore;

/** Server-authoritative asset Container rendered by the production Galaxy UI 2 host. */
public final class GuiTerminalAssetCenter extends TerminalContainerScreenV2Base<GuiTerminalAssetCenter.ClientContainer> {
    private final UiStore<AssetCenterUiState,AssetCenterAction> store;
    private AssetCenterDocument document;

    public GuiTerminalAssetCenter(InventoryPlayer inventory,int initialTabCode){
        this(new ClientContainer(inventory),TerminalAssetCenterTab.fromCode(initialTabCode));
    }

    private GuiTerminalAssetCenter(ClientContainer container,TerminalAssetCenterTab initialTab){
        super(container,(GuiScreen)null,TerminalClientScreenController.INSTANCE.getLastHomeScreen(TerminalPage.WAREHOUSE.getId()));
        store=new UiStore<AssetCenterUiState,AssetCenterAction>(AssetCenterUiState.initial(initialTab),
            new AssetCenterReducer(),Collections.<UiEffect<AssetCenterUiState,AssetCenterAction>>singletonList(new AssetCenterEffect()));
        store.addListener(new UiStore.Listener<AssetCenterUiState>(){@Override public void onStateChanged(AssetCenterUiState state){invalidateUi();}});
    }

    @Override public void initGui(){super.initGui();refreshPage();}

    @Override protected UiDocument createDocument(){
        if(document==null)document=new AssetCenterDocument(store,shellModel(),shellActions(),new AssetCenterDocument.CursorAccess(){
            @Override public ItemStack current(){return Minecraft.getMinecraft().thePlayer==null?null:Minecraft.getMinecraft().thePlayer.inventory.getItemStack();}
        });
        return document;
    }

    public void applyActivitySnapshot(AssetActivitySnapshot snapshot){applySnapshot(snapshot,store.getState().getCell());}
    public void applySnapshot(AssetActivitySnapshot activity,TerminalCellContentSnapshot cell){store.dispatch(AssetCenterAction.snapshot(activity,cell));}

    @Override protected void refreshPage(){store.dispatch(AssetCenterAction.refresh(store.getState().getRequestedPage(),20));}
    @Override protected void showHelp(){store.dispatch(AssetCenterAction.simple(AssetCenterAction.Type.OPEN_HELP));}

    @Override protected void syncNativeSlots(DrawList list){
        Map<String,UiRect> regions=slotBridge.regions(list);List<Slot> slots=NativeSlots.list(inventorySlots.inventorySlots);
        if(store.getState().getTab()!=TerminalAssetCenterTab.STORAGE){slotBridge.hide(slots);container().setActiveTab(store.getState().getTab());return;}
        slotBridge.layoutGrid(slots.subList(0,Math.min(27,slots.size())),regions.get("asset:vault"),9,guiLeft,guiTop);
        if(slots.size()>27)slotBridge.layoutGrid(slots.subList(27,Math.min(28,slots.size())),regions.get("asset:bay"),1,guiLeft,guiTop);
        if(slots.size()>28)slotBridge.layoutGrid(slots.subList(28,slots.size()),regions.get("asset:player"),9,guiLeft,guiTop);
        container().setActiveTab(store.getState().getTab());
    }

    @Override protected void drawNativeSlotBackgrounds(){}

    @Override protected void drawExternalContent(DrawList list,int mouseX,int mouseY){
        if(document!=null)itemBridge.draw(list,document.externalItems(),itemRender,fontRendererObj,mc.getTextureManager());
    }

    @Override protected void drawExternalTooltips(DrawList list,int mouseX,int mouseY){
        if(document!=null){String tooltip=document.tooltipFor(list,mouseX,mouseY);if(!tooltip.isEmpty())drawHoveringText(Arrays.asList(tooltip.split("\\|",-1)),mouseX+8,mouseY,fontRendererObj);}
        TerminalHudOverlayHandler.INSTANCE.drawTerminalNotifications(fontRendererObj,width,height);
    }

    @Override protected void handleMouseClick(Slot slot,int slotId,int button,int mode){
        if(store.getState().getTab()!=TerminalAssetCenterTab.STORAGE||slotId<0||!container().isVisibleSlot(slotId))return;
        super.handleMouseClick(slot,slotId,button,mode);invalidateUi();
    }

    private static final class NativeSlots {
        @SuppressWarnings("unchecked") private static List<Slot> list(Object raw){return raw instanceof List<?>?new ArrayList<Slot>((List<Slot>)raw):Collections.<Slot>emptyList();}
    }

    static final class ClientContainer extends Container {
        private final InventoryBasic vault=new InventoryBasic("Base Vault",true,27);
        private final InventoryBasic bay=new InventoryBasic("AE2 Cell Bay",true,1);
        private TerminalAssetCenterTab currentTab=TerminalAssetCenterTab.STORAGE;
        private ClientContainer(InventoryPlayer inventory){
            for(int index=0;index<27;index++)addSlotToContainer(new Slot(vault,index,-1000,-1000));
            addSlotToContainer(new Slot(bay,0,-1000,-1000){
                @Override public int getSlotStackLimit(){return 1;}
                @Override public boolean isItemValid(ItemStack stack){return TerminalWarehouseCellInspector.isValidCell(stack);}
            });
            for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlotToContainer(new Slot(inventory,column+row*9+9,-1000,-1000));
            for(int column=0;column<9;column++)addSlotToContainer(new Slot(inventory,column,-1000,-1000));
        }
        private void setActiveTab(TerminalAssetCenterTab tab){currentTab=tab==null?TerminalAssetCenterTab.STORAGE:tab;}
        private boolean isVisibleSlot(int slotId){return currentTab==TerminalAssetCenterTab.STORAGE&&slotId>=0&&slotId<inventorySlots.size();}
        @Override public boolean canInteractWith(EntityPlayer player){return true;}
        @Override public ItemStack transferStackInSlot(EntityPlayer player,int slotId){
            if(!isVisibleSlot(slotId))return null;Slot slot=(Slot)inventorySlots.get(slotId);if(slot==null||!slot.getHasStack())return null;
            ItemStack source=slot.getStack(),original=source.copy();
            if(slotId<TerminalAssetCenterContainer.VAULT_SLOT_COUNT||slotId==TerminalAssetCenterContainer.BAY_SLOT_INDEX){
                if(!mergeItemStack(source,TerminalAssetCenterContainer.PLAYER_SLOT_START,inventorySlots.size(),true))return null;
            }else if(slotId>=TerminalAssetCenterContainer.PLAYER_SLOT_START){
                if(!mergeItemStack(source,0,TerminalAssetCenterContainer.VAULT_SLOT_COUNT,false))return null;
            }else return null;
            if(source.stackSize==0)slot.putStack(null);else slot.onSlotChanged();if(source.stackSize==original.stackSize)return null;
            slot.onPickupFromSlot(player,source);return original;
        }
    }
}
