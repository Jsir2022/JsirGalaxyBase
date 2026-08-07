package com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.application.VaultException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Native chest semantics backed by a session inventory.  Vanilla performs the
 * click/drag/merge rules; the resulting Vault delta is version-checked and
 * committed immediately after each real mutation.
 */
public final class BaseVaultContainer extends Container {

    public static final int VAULT_SLOT_COUNT = 27;
    private final EntityPlayer player;
    private final String playerRef;
    private final BaseVaultService vaultService;
    private final BaseVaultSessionInventory vaultInventory;

    public BaseVaultContainer(EntityPlayer player, BaseVaultService vaultService) {
        if (player == null || vaultService == null) {
            throw new VaultException("player and Base Vault service are required");
        }
        this.player = player;
        this.playerRef = player.getUniqueID().toString();
        this.vaultService = vaultService;
        this.vaultInventory = new BaseVaultSessionInventory(vaultService.viewPersonalVault(playerRef));
        addVaultSlots();
        addPlayerSlots(player.inventory);
        vaultInventory.openInventory();
    }

    public BaseVaultSessionInventory getVaultInventory() { return vaultInventory; }

    /** Server-only entrypoint used by the explicit audited sort request. */
    public void sortVault() {
        if (player.inventory.getItemStack() != null) {
            throw new VaultException("Place the cursor stack before sorting the Base Vault");
        }
        BaseVaultService.VaultSortResult result = vaultService.sortPersonalVault(
            "vault-sort:" + playerRef + ":" + UUID.randomUUID(), playerRef);
        vaultInventory.refreshExpectedSlots(result.getView());
        detectAndSendChanges();
    }

    @Override
    public boolean canInteractWith(EntityPlayer actor) {
        return actor != null && playerRef.equals(actor.getUniqueID().toString());
    }

    @Override
    public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer actor) {
        if (!canInteractWith(actor)) {
            return null;
        }
        // Vanilla mode 3 is creative middle-click cloning. A persistent Vault
        // must never create a cursor copy that is not backed by a Vault debit.
        if (mode == 3) {
            return null;
        }
        List<ItemStack> vaultBefore = vaultInventory.snapshotStacks();
        List<ItemStack> playerBefore = snapshotPlayer(actor.inventory);
        ItemStack cursorBefore = copy(actor.inventory.getItemStack());
        String requestId = "vault-container:" + playerRef + ":" + UUID.randomUUID();
        boolean prepared = false;
        try {
            ItemStack auditStack = resolveAuditStack(slotId, actor);
            if (auditStack != null) {
                vaultService.preparePersonalContainerMutation(requestId, playerRef, auditStack,
                    "native container interaction slot=" + slotId + ",button=" + clickedButton + ",mode=" + mode);
                prepared = true;
            }
            ItemStack result = super.slotClick(slotId, clickedButton, mode, actor);
            if (prepared) {
                if (sameSnapshot(vaultBefore, vaultInventory.snapshotStacks())) {
                    vaultService.markOperationFailed(requestId, "native container interaction did not change Vault state", false);
                } else {
                    commitIfVaultChanged(requestId, vaultBefore, "slot=" + slotId + ",button=" + clickedButton + ",mode=" + mode);
                }
            } else if (!sameSnapshot(vaultBefore, vaultInventory.snapshotStacks())) {
                // A drag release can start from an empty cursor. It still needs
                // a durable record before the resulting Vault state is accepted.
                ItemStack fallback = firstNonEmpty(vaultBefore, vaultInventory.snapshotStacks());
                vaultService.preparePersonalContainerMutation(requestId, playerRef, fallback,
                    "native container interaction late audit slot=" + slotId);
                commitIfVaultChanged(requestId, vaultBefore, "slot=" + slotId + ",button=" + clickedButton + ",mode=" + mode);
            }
            return result;
        } catch (RuntimeException exception) {
            vaultInventory.restore(vaultBefore);
            restorePlayer(actor.inventory, playerBefore, cursorBefore);
            if (prepared) {
                try {
                    vaultService.markOperationFailed(requestId, "native container interaction restored: " + exception.getMessage(), false);
                } catch (RuntimeException ignored) { }
            }
            GalaxyBase.LOG.warn("Base Vault container interaction restored for {}", playerRef, exception);
            throw exception;
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer actor, int containerSlot) {
        Slot slot = containerSlot < 0 || containerSlot >= inventorySlots.size() ? null : (Slot) inventorySlots.get(containerSlot);
        if (slot == null || !slot.getHasStack()) {
            return null;
        }
        ItemStack source = slot.getStack();
        ItemStack original = source.copy();
        if (containerSlot < VAULT_SLOT_COUNT) {
            if (!mergeItemStack(source, VAULT_SLOT_COUNT, inventorySlots.size(), true)) {
                return null;
            }
        } else if (!mergeItemStack(source, 0, VAULT_SLOT_COUNT, false)) {
            return null;
        }
        if (source.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        if (source.stackSize == original.stackSize) {
            return null;
        }
        slot.onPickupFromSlot(actor, source);
        return original;
    }

    @Override
    public void onContainerClosed(EntityPlayer actor) {
        super.onContainerClosed(actor);
        vaultInventory.closeInventory();
    }

    private void addVaultSlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(vaultInventory, column + row * 9, 18 + column * 18, 32 + row * 18));
            }
        }
    }

    private void addPlayerSlots(InventoryPlayer inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(inventory, column + row * 9 + 9, 18 + column * 18, 98 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(inventory, column, 18 + column * 18, 156));
        }
    }

    private void commitIfVaultChanged(String requestId, List<ItemStack> before, String context) {
        List<ItemStack> after = vaultInventory.snapshotStacks();
        if (sameSnapshot(before, after)) {
            return;
        }
        vaultService.commitPersonalContainerMutation(requestId, playerRef,
            vaultInventory.getOpeningSlots(), after, context);
        // Refresh expected versions after a successful write so later clicks do
        // not conflict with our own session's completed mutation.
        vaultInventory.refreshExpectedSlots(vaultService.viewPersonalVault(playerRef));
    }

    private ItemStack resolveAuditStack(int slotId, EntityPlayer actor) {
        if (slotId >= 0 && slotId < inventorySlots.size()) {
            Slot slot = (Slot) inventorySlots.get(slotId);
            if (slot != null && slot.getHasStack()) return copy(slot.getStack());
        }
        if (actor.inventory.getItemStack() != null) return copy(actor.inventory.getItemStack());
        for (ItemStack stack : actor.inventory.mainInventory) if (stack != null) return copy(stack);
        return firstNonEmpty(vaultInventory.snapshotStacks(), null);
    }

    private static ItemStack firstNonEmpty(List<ItemStack> first, List<ItemStack> second) {
        if (first != null) for (ItemStack stack : first) if (stack != null && stack.stackSize > 0) return copy(stack);
        if (second != null) for (ItemStack stack : second) if (stack != null && stack.stackSize > 0) return copy(stack);
        return null;
    }

    private static boolean sameSnapshot(List<ItemStack> before, List<ItemStack> after) {
        if (before.size() != after.size()) return false;
        for (int index = 0; index < before.size(); index++) {
            ItemStack left = before.get(index);
            ItemStack right = after.get(index);
            if (left == null ? right != null : right == null || left.stackSize != right.stackSize
                || !ItemStack.areItemStacksEqual(left, right)) return false;
        }
        return true;
    }

    private static List<ItemStack> snapshotPlayer(InventoryPlayer inventory) {
        List<ItemStack> stacks = new ArrayList<ItemStack>(inventory.mainInventory.length);
        for (ItemStack stack : inventory.mainInventory) stacks.add(copy(stack));
        return stacks;
    }

    private static void restorePlayer(InventoryPlayer inventory, List<ItemStack> stacks, ItemStack cursor) {
        for (int index = 0; index < inventory.mainInventory.length; index++) {
            inventory.mainInventory[index] = index < stacks.size() ? copy(stacks.get(index)) : null;
        }
        inventory.setItemStack(copy(cursor));
        inventory.markDirty();
    }

    private static ItemStack copy(ItemStack stack) { return stack == null ? null : stack.copy(); }
}
