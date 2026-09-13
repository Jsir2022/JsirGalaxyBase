package com.jsirgalaxybase.client.ui2.lab;

import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import com.jsirgalaxybase.client.ui2.host.ModernContainerHost;
import com.jsirgalaxybase.client.ui2.host.NativeSlotBridge;
import com.jsirgalaxybase.ui2.core.UiDocument;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.lab.UiLabAction;
import com.jsirgalaxybase.ui2.lab.UiLabDocument;
import com.jsirgalaxybase.ui2.lab.UiLabDiagnostics;
import com.jsirgalaxybase.ui2.lab.UiLabPage;
import com.jsirgalaxybase.ui2.lab.UiLabReducer;
import com.jsirgalaxybase.ui2.lab.UiLabState;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.state.UiStore;

public final class Ui2LabContainerScreen extends ModernContainerHost<Ui2LabLocalContainer> {
    private final UiStore<UiLabState,UiLabAction> store=new UiStore<UiLabState,UiLabAction>(new UiLabState(UiLabPage.INVENTORY,false,false,false),new UiLabReducer());
    private final UiLabDocument document=new UiLabDocument(store,new UiLabDiagnostics(){
        @Override public String summary(boolean reducedMotion){return com.jsirgalaxybase.client.ui2.MinecraftUiPlatform.INSTANCE.diagnosticSummary(reducedMotion);}
    });
    private final ItemStack originalCursor;
    private final ItemStack[] originalMain;
    private final ItemStack[] originalArmor;
    public Ui2LabContainerScreen(GuiScreen parent){
        super(new Ui2LabLocalContainer(),parent);
        net.minecraft.entity.player.EntityPlayer player=net.minecraft.client.Minecraft.getMinecraft().thePlayer;
        originalCursor=copy(player.inventory.getItemStack());
        originalMain=copy(player.inventory.mainInventory);
        originalArmor=copy(player.inventory.armorInventory);
        store.addListener(new UiStore.Listener<UiLabState>(){@Override public void onStateChanged(UiLabState state){invalidateUi();}});
    }
    @Override protected UiDocument createDocument(){return document;}
    @Override protected void syncNativeSlots(DrawList list){
        Map<String,UiRect> regions=slotBridge.regions(list);List<Slot> slots=NativeSlotBridge.slots(inventorySlots.inventorySlots);
        slotBridge.layoutGrid(slots.subList(0,Math.min(9,slots.size())),regions.get("vault:region"),9,guiLeft,guiTop);
        if(slots.size()>9)slotBridge.layoutGrid(slots.subList(9,slots.size()),regions.get("cell:region"),5,guiLeft,guiTop);
    }
    /** Slot chrome is drawn by UI2; this layer only contributes native items and hit behavior. */
    @Override protected void drawNativeSlotBackgrounds(){}
    @Override protected void handleMouseClick(Slot slot,int slotId,int button,int mode){inventorySlots.slotClick(slotId,button,mode,mc.thePlayer);}
    @Override protected boolean handleUnhandledKey(char typedChar,int keyCode){
        char key=Character.toLowerCase(typedChar);
        if(key=='d'){store.dispatch(UiLabAction.of(UiLabAction.Type.TOGGLE_DEBUG));return true;}
        if(key=='m'){store.dispatch(UiLabAction.of(UiLabAction.Type.TOGGLE_MOTION));return true;}
        if(keyCode==Keyboard.KEY_RETURN||keyCode==Keyboard.KEY_NUMPADENTER){store.dispatch(UiLabAction.of(UiLabAction.Type.OPEN_MODAL));return true;}
        return false;
    }
    @Override public void onGuiClosed(){
        restore(mc.thePlayer.inventory.mainInventory,originalMain);
        restore(mc.thePlayer.inventory.armorInventory,originalArmor);
        mc.thePlayer.inventory.setItemStack(copy(originalCursor));
        mc.thePlayer.inventory.markDirty();
        super.onGuiClosed();
    }
    private static ItemStack copy(ItemStack stack){return stack==null?null:stack.copy();}
    private static ItemStack[] copy(ItemStack[] source){ItemStack[] result=new ItemStack[source.length];for(int i=0;i<source.length;i++)result[i]=copy(source[i]);return result;}
    private static void restore(ItemStack[] target,ItemStack[] source){for(int i=0;i<target.length&&i<source.length;i++)target[i]=copy(source[i]);}
}
