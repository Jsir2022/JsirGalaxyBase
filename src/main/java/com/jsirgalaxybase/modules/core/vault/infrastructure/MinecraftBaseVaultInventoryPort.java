package com.jsirgalaxybase.modules.core.vault.infrastructure;

import java.util.UUID;

import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.application.VaultException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * The only allowed bridge between a player's local inventory and Base Vault.
 * It writes a PROCESSING audit record before changing the inventory and never
 * retries an ambiguous operation automatically.
 */
public final class MinecraftBaseVaultInventoryPort {

    private final BaseVaultService vaultService;

    public MinecraftBaseVaultInventoryPort(BaseVaultService vaultService) {
        if (vaultService == null) {
            throw new VaultException("base vault service is required");
        }
        this.vaultService = vaultService;
    }

    public BaseVaultService.VaultDeliveryResult depositSlot(EntityPlayerMP player, int slotIndex, int quantity) {
        if (player == null) {
            throw new VaultException("an online player is required for Base Vault deposit");
        }
        ItemStack original = player.inventory.getStackInSlot(slotIndex);
        if (original == null || original.stackSize <= 0) {
            throw new VaultException("player inventory slot is empty");
        }
        if (quantity <= 0 || quantity > original.stackSize) {
            throw new VaultException("deposit quantity is outside the inventory stack");
        }
        ItemStack moving = original.copy();
        moving.stackSize = quantity;
        String requestId = "vault-player-deposit:" + player.getUniqueID() + ":" + UUID.randomUUID().toString();
        vaultService.preparePersonalDeposit(requestId, player.getUniqueID().toString(), "PLAYER_INVENTORY", moving);
        try {
            ItemStack remaining = original.copy();
            remaining.stackSize -= quantity;
            player.inventory.setInventorySlotContents(slotIndex, remaining.stackSize <= 0 ? null : remaining);
            player.inventory.markDirty();
            return vaultService.completePreparedPersonalDeposit(requestId, player.getUniqueID().toString(), moving);
        } catch (RuntimeException exception) {
            try {
                player.inventory.setInventorySlotContents(slotIndex, original);
                player.inventory.markDirty();
                vaultService.markOperationFailed(requestId, "inventory deposit restored after failure: " + exception.getMessage(), false);
            } catch (RuntimeException restoreFailure) {
                vaultService.markOperationFailed(requestId,
                    "inventory deposit delivery cannot be proven: " + restoreFailure.getMessage(), true);
            }
            throw exception;
        }
    }

    public BaseVaultService.VaultWithdrawal withdrawSlot(EntityPlayerMP player, int vaultSlotIndex, int quantity) {
        if (player == null) {
            throw new VaultException("an online player is required for Base Vault withdrawal");
        }
        BaseVaultService.VaultView view = vaultService.viewPersonalVault(player.getUniqueID().toString());
        if (vaultSlotIndex < 0 || vaultSlotIndex >= view.getSlots().size()
            || view.getSlots().get(vaultSlotIndex).getStack() == null) {
            throw new VaultException("Base Vault slot is empty");
        }
        ItemStack requested = view.getSlots().get(vaultSlotIndex).getStack().copy();
        requested.stackSize = quantity;
        if (quantity <= 0 || quantity > view.getSlots().get(vaultSlotIndex).getStack().stackSize) {
            throw new VaultException("withdraw quantity is outside the Base Vault stack");
        }
        if (!canAccept(player, requested)) {
            throw new VaultException("player inventory has insufficient space for Base Vault withdrawal");
        }
        String requestId = "vault-player-withdraw:" + player.getUniqueID() + ":" + UUID.randomUUID().toString();
        BaseVaultService.VaultWithdrawal prepared = vaultService.preparePersonalWithdrawal(requestId,
            player.getUniqueID().toString(), vaultSlotIndex, quantity, "PLAYER_INVENTORY");
        ItemStack delivery = prepared.getStack();
        try {
            ItemStack attempt = delivery.copy();
            if (!player.inventory.addItemStackToInventory(attempt) || attempt.stackSize > 0) {
                throw new VaultException("player inventory has insufficient space for Base Vault withdrawal");
            }
            player.inventory.markDirty();
            return vaultService.completePreparedWithdrawal(requestId, prepared);
        } catch (RuntimeException exception) {
            vaultService.markOperationFailed(requestId,
                "withdrawal delivery cannot be proven; inspect player inventory before recovery: " + exception.getMessage(), true);
            throw exception;
        }
    }

    private boolean canAccept(EntityPlayerMP player, ItemStack incoming) {
        int remaining = incoming.stackSize;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack == null) {
                remaining -= incoming.getMaxStackSize();
            } else if (stack.isItemEqual(incoming) && ItemStack.areItemStackTagsEqual(stack, incoming)) {
                remaining -= Math.max(0, stack.getMaxStackSize() - stack.stackSize);
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }
}
