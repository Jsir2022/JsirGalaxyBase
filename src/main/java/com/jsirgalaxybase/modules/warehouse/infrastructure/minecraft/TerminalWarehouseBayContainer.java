package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseBayService;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBay;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBayReceipt;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Native one-slot GUI: vanilla owns mouse/drag semantics, the service owns durable Bay state. */
public final class TerminalWarehouseBayContainer extends Container {
    public static final int BAY_SLOT_COUNT = 1;
    private final EntityPlayer player;
    private final String ownerPlayerRef;
    private final TerminalWarehouseBayService service;
    private final TerminalWarehouseBaySessionInventory bayInventory;

    public TerminalWarehouseBayContainer(EntityPlayer player, TerminalWarehouseBayService service) {
        if (player == null || service == null) throw new IllegalArgumentException("player and Bay service are required");
        this.player = player; this.ownerPlayerRef = player.getUniqueID().toString(); this.service = service;
        this.bayInventory = new TerminalWarehouseBaySessionInventory(service.view(ownerPlayerRef));
        addSlotToContainer(new Slot(bayInventory, 0, 98, 28) {
            @Override public int getSlotStackLimit() { return 1; }
            @Override public boolean isItemValid(ItemStack stack) { return bayInventory.isItemValidForSlot(0, stack); }
        });
        addPlayerSlots(player.inventory);
    }

    public TerminalWarehouseBaySessionInventory getBayInventory() { return bayInventory; }
    @Override public boolean canInteractWith(EntityPlayer actor) { return actor != null && ownerPlayerRef.equals(actor.getUniqueID().toString()); }
    @Override public ItemStack slotClick(int slotId, int button, int mode, EntityPlayer actor) {
        if (!canInteractWith(actor) || mode == 3) return null;
        ItemStack bayBefore = bayInventory.getCell(); List<ItemStack> playerBefore = snapshot(actor.inventory); ItemStack cursorBefore = copy(actor.inventory.getItemStack());
        try {
            ItemStack result = super.slotClick(slotId, button, mode, actor);
            commitIfChanged(bayBefore, "slot=" + slotId + ",button=" + button + ",mode=" + mode);
            return result;
        } catch (RuntimeException exception) {
            bayInventory.restore(bayBefore); restore(actor.inventory, playerBefore, cursorBefore);
            GalaxyBase.LOG.warn("Terminal warehouse Bay interaction restored for {}", ownerPlayerRef, exception);
            // A rejected persistence write must never turn a harmless inventory click into a player disconnect
            // or leave the client predicting a vanished Cell. Vanilla will receive the restored slot state.
            detectAndSendChanges();
            return null;
        }
    }
    @Override public ItemStack transferStackInSlot(EntityPlayer actor, int containerSlot) {
        Slot slot = containerSlot < 0 || containerSlot >= inventorySlots.size() ? null : (Slot) inventorySlots.get(containerSlot);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack source = slot.getStack(); ItemStack original = source.copy();
        if (containerSlot < BAY_SLOT_COUNT) {
            if (!mergeItemStack(source, BAY_SLOT_COUNT, inventorySlots.size(), true)) return null;
        } else if (!mergeItemStack(source, 0, BAY_SLOT_COUNT, false)) return null;
        if (source.stackSize == 0) slot.putStack(null); else slot.onSlotChanged();
        if (source.stackSize == original.stackSize) return null;
        slot.onPickupFromSlot(actor, source); return original;
    }
    private void commitIfChanged(ItemStack before, String detail) {
        ItemStack after = bayInventory.getCell(); if (same(before, after)) return;
        TerminalWarehouseBayReceipt receipt = service.commit(service.nextRequestId(), ownerPlayerRef, bayInventory.getOpeningVersion(), before, after, detail);
        if (!receipt.isSuccess()) throw new IllegalStateException("Terminal Bay mutation rejected: " + receipt.getResult());
        TerminalWarehouseBay refreshed = receipt.getAfter(); bayInventory.refresh(refreshed);
    }
    private void addPlayerSlots(InventoryPlayer inventory) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlotToContainer(new Slot(inventory, column + row * 9 + 9, 18 + column * 18, 76 + row * 18));
        for (int column = 0; column < 9; column++) addSlotToContainer(new Slot(inventory, column, 18 + column * 18, 134));
    }
    private static boolean same(ItemStack left, ItemStack right) { return left == null ? right == null : right != null && ItemStack.areItemStacksEqual(left, right); }
    private static ItemStack copy(ItemStack stack) { return stack == null ? null : stack.copy(); }
    private static List<ItemStack> snapshot(InventoryPlayer inventory) { List<ItemStack> result = new ArrayList<ItemStack>(); for (ItemStack stack : inventory.mainInventory) result.add(copy(stack)); return result; }
    private static void restore(InventoryPlayer inventory, List<ItemStack> snapshot, ItemStack cursor) { for (int index = 0; index < inventory.mainInventory.length; index++) inventory.mainInventory[index] = index < snapshot.size() ? copy(snapshot.get(index)) : null; inventory.setItemStack(copy(cursor)); inventory.markDirty(); }
}
