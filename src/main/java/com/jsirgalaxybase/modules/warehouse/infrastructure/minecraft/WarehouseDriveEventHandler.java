package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;

import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveReceipt;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Ensures placement, removal and explosions cannot bypass the Drive ownership service. */
public final class WarehouseDriveEventHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event.world == null || event.world.isRemote || !WarehouseBlockRegistry.isWarehouseDrive(event.block)) return;
        WarehouseDriveReceipt receipt = WarehouseDriveRuntime.register(event.world, event.x, event.y, event.z, event.player);
        if (receipt == null || !receipt.isSuccess()) {
            event.setCanceled(true); message(event.player, receipt == null ? "unavailable" : receipt.getResult().name().toLowerCase());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.world == null || event.world.isRemote || !WarehouseBlockRegistry.isWarehouseDrive(event.block)) return;
        WarehouseDriveReceipt receipt = WarehouseDriveRuntime.authorizeBreak(event.world, event.x, event.y, event.z, event.getPlayer());
        if (receipt == null || !receipt.isSuccess()) {
            event.setCanceled(true); message(event.getPlayer(), receipt == null ? "unavailable" : receipt.getResult().name().toLowerCase());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExplosion(ExplosionEvent.Detonate event) {
        if (event.world == null || event.world.isRemote) return;
        event.getAffectedBlocks().removeIf(position -> WarehouseBlockRegistry.isWarehouseDrive(
            event.world.getBlock(position.chunkPosX, position.chunkPosY, position.chunkPosZ)));
    }

    private static void message(EntityPlayer player, String code) {
        if (player != null) player.addChatComponentMessage(new ChatComponentTranslation("jsirgalaxybase.warehouse." + code));
    }
}
