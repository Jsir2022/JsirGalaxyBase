package com.jsirgalaxybase.client.ui2.lab;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Disposable local inventory used only to verify GuiContainer platform behavior. */
public final class Ui2LabLocalContainer extends Container {
    private static final int VAULT_SIZE = 9;
    private static final int CELL_SIZE = 5;
    private final InventoryBasic vault = new InventoryBasic("Galaxy UI 2 Vault Lab", false, VAULT_SIZE);
    private final InventoryBasic cell = new InventoryBasic("Galaxy UI 2 Cell Lab", false, CELL_SIZE);
    public Ui2LabLocalContainer(){
        vault.setInventorySlotContents(0,new ItemStack(Items.iron_ingot,64));
        vault.setInventorySlotContents(1,new ItemStack(Items.gold_ingot,32));
        vault.setInventorySlotContents(2,new ItemStack(Items.diamond,8));
        cell.setInventorySlotContents(0,new ItemStack(Items.redstone,48));
        for(int i=0;i<VAULT_SIZE;i++)addSlotToContainer(new Slot(vault,i,-10000,-10000));
        for(int i=0;i<CELL_SIZE;i++)addSlotToContainer(new Slot(cell,i,-10000,-10000));
    }
    @Override public boolean canInteractWith(EntityPlayer player){return true;}
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int index){
        if(index<0||index>=inventorySlots.size())return null;
        Slot slot=(Slot)inventorySlots.get(index);
        if(slot==null||!slot.getHasStack())return null;
        ItemStack source=slot.getStack(),original=source.copy();
        boolean moved=index<VAULT_SIZE
            ?mergeItemStack(source,VAULT_SIZE,VAULT_SIZE+CELL_SIZE,false)
            :mergeItemStack(source,0,VAULT_SIZE,false);
        if(!moved)return null;
        if(source.stackSize<=0)slot.putStack(null);else slot.onSlotChanged();
        return original;
    }
}
