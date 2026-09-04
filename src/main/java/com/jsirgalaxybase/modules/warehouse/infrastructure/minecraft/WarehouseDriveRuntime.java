package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import java.util.UUID;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import com.jsirgalaxybase.modules.warehouse.ae2.WarehouseDriveTile;
import com.jsirgalaxybase.modules.warehouse.application.WarehouseDriveService;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveKey;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveReceipt;

/** Process-local bridge used only by server-side Forge events and the registered block. */
public final class WarehouseDriveRuntime {
    private static volatile WarehouseDriveService service;
    private static volatile String localServerId;
    private WarehouseDriveRuntime() {}

    public static void install(WarehouseDriveService newService, String serverId) {
        service = newService; localServerId = serverId;
    }
    public static void clear() { service = null; localServerId = null; }
    public static boolean isReady() { return service != null && localServerId != null; }

    public static WarehouseDriveReceipt register(World world, int x, int y, int z, EntityPlayer player) {
        WarehouseDriveService current = service;
        return current == null ? null : current.register(requestId(), playerRef(player), isFake(player), key(world, x, y, z));
    }

    public static WarehouseDriveReceipt authorizeBreak(World world, int x, int y, int z, EntityPlayer player) {
        WarehouseDriveService current = service;
        WarehouseDriveTile tile = tile(world, x, y, z);
        return current == null || tile == null ? null : current.authorizeBreak(requestId(), playerRef(player), isFake(player),
            key(world, x, y, z), tile.hasCell(), tile.getStoredItemCount() == 0L);
    }

    public static void handleActivated(World world, int x, int y, int z, EntityPlayer player) {
        WarehouseDriveService current = service;
        WarehouseDriveTile tile = tile(world, x, y, z);
        if (current == null || tile == null) { message(player, "jsirgalaxybase.warehouse.unavailable"); return; }
        ItemStack held = player.getHeldItem();
        WarehouseDriveKey key = key(world, x, y, z);
        if (player.isSneaking() && (held == null || held.stackSize <= 0)) {
            WarehouseDriveReceipt receipt = current.authorizeRemoveCell(requestId(), playerRef(player), isFake(player), key,
                tile.hasCell(), tile.getStoredItemCount() == 0L);
            if (!receipt.isSuccess()) { message(player, resultKey(receipt)); return; }
            IInventory inventory = tile.getInternalInventory();
            ItemStack removed = inventory.getStackInSlot(0);
            inventory.setInventorySlotContents(0, null);
            if (removed != null && !player.inventory.addItemStackToInventory(removed)) {
                world.spawnEntityInWorld(new EntityItem(world, player.posX, player.posY, player.posZ, removed));
            }
            message(player, "jsirgalaxybase.warehouse.cell_removed");
            return;
        }
        if (held != null && held.stackSize > 0) {
            WarehouseDriveReceipt receipt = current.authorizeInstall(requestId(), playerRef(player), isFake(player), key,
                tile.isItemValidForSlot(0, held), tile.hasCell());
            if (!receipt.isSuccess()) { message(player, resultKey(receipt)); return; }
            tile.getInternalInventory().setInventorySlotContents(0, held.splitStack(1));
            message(player, "jsirgalaxybase.warehouse.cell_installed");
            return;
        }
        message(player, "jsirgalaxybase.warehouse.open_terminal");
    }

    public static WarehouseDriveKey key(World world, int x, int y, int z) {
        return new WarehouseDriveKey(localServerId, world.provider.dimensionId, x, y, z);
    }
    private static WarehouseDriveTile tile(World world, int x, int y, int z) {
        return world != null && world.getTileEntity(x, y, z) instanceof WarehouseDriveTile
            ? (WarehouseDriveTile) world.getTileEntity(x, y, z) : null;
    }
    private static String playerRef(EntityPlayer player) { return player == null || player.getUniqueID() == null ? "unknown" : player.getUniqueID().toString(); }
    private static boolean isFake(EntityPlayer player) { return player instanceof FakePlayer; }
    private static String requestId() { return "warehouse-" + UUID.randomUUID().toString(); }
    private static void message(EntityPlayer player, String key) { if (player != null) player.addChatComponentMessage(new ChatComponentTranslation(key)); }
    private static String resultKey(WarehouseDriveReceipt receipt) { return "jsirgalaxybase.warehouse." + receipt.getResult().name().toLowerCase(); }
}
