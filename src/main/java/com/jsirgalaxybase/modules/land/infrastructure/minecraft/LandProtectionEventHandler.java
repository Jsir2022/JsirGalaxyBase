/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Derived from the Forge protection event routing in GTNH ServerUtilities handlers.
 * Team and global singleton checks were replaced with JGB personal-title decisions.
 */
package com.jsirgalaxybase.modules.land.infrastructure.minecraft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.land.application.LandProtectionRuntime;
import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.LandProtectionAction;
import com.jsirgalaxybase.modules.land.domain.LandProtectionDecision;
import com.jsirgalaxybase.modules.land.domain.LandProtectionMode;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class LandProtectionEventHandler {

    private static final long NOTICE_COOLDOWN_MILLIS = 5000L;

    private final String localServerId;
    private final LandProtectionRuntime runtime;
    private final Map<String, Long> notices = new HashMap<String, Long>();

    public LandProtectionEventHandler(String localServerId, LandProtectionRuntime runtime) {
        this.localServerId = localServerId;
        this.runtime = runtime;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        handlePlayer(event.getPlayer(), event.world, event.x, event.z, LandProtectionAction.BREAK_BLOCK,
            () -> event.setCanceled(true));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        handlePlayer(event.player, event.world, event.x, event.z, LandProtectionAction.PLACE_BLOCK,
            () -> event.setCanceled(true));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.world == null || event.world.isRemote) return;
        int x = event.x;
        int z = event.z;
        LandProtectionAction action = event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR
            ? LandProtectionAction.USE_ITEM
            : LandProtectionAction.INTERACT_BLOCK;
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            MovingObjectPosition target = event.entityPlayer.rayTrace(5.0D, 1.0F);
            if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
            x = target.blockX;
            z = target.blockZ;
        }
        handlePlayer(event.entityPlayer, event.world, x, z, action, () -> event.setCanceled(true));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.target instanceof EntityPlayer || event.target == null || event.target.worldObj == null) return;
        Entity target = event.target;
        handlePlayer(event.entityPlayer, target.worldObj, MathHelper.floor_double(target.posX),
            MathHelper.floor_double(target.posZ), LandProtectionAction.ATTACK_ENTITY, () -> event.setCanceled(true));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onExplosion(ExplosionEvent.Detonate event) {
        if (event.world == null || event.world.isRemote || event.getAffectedBlocks().isEmpty()) return;
        Iterator<ChunkPosition> iterator = event.getAffectedBlocks().iterator();
        while (iterator.hasNext()) {
            ChunkPosition block = iterator.next();
            LandProtectionDecision decision = runtime.evaluate(null, false,
                LandChunkKey.fromBlockCoordinates(localServerId, event.world.provider.dimensionId,
                    block.chunkPosX, block.chunkPosZ),
                LandProtectionAction.EXPLOSION);
            recordDenied(decision, "environment");
            if (runtime.shouldCancel(decision)) iterator.remove();
        }
    }

    private void handlePlayer(EntityPlayer player, World world, int blockX, int blockZ,
        LandProtectionAction action, Runnable cancel) {
        if (player == null || world == null || world.isRemote) return;
        boolean fake = player instanceof FakePlayer;
        String playerRef = player.getUniqueID() == null ? null : player.getUniqueID().toString();
        LandProtectionDecision decision = runtime.evaluate(playerRef, fake,
            LandChunkKey.fromBlockCoordinates(localServerId, world.provider.dimensionId, blockX, blockZ), action);
        recordDenied(decision, playerRef == null ? "unknown" : playerRef);
        if (!runtime.shouldCancel(decision)) return;
        cancel.run();
        if (!fake && shouldNotify(playerRef, decision)) {
            player.addChatComponentMessage(new ChatComponentTranslation("jsirgalaxybase.land.protected"));
        }
    }

    private void recordDenied(LandProtectionDecision decision, String actor) {
        if (decision.isAllowed() || !shouldLog(actor, decision)) return;
        GalaxyBase.LOG.warn("[land-{}] denied action={} actor={} chunk={} owner={} reason={}",
            runtime.getMode().name().toLowerCase(), decision.getAction(), actor, decision.getChunkKey(),
            decision.getTitle().getOwnerPlayerRef(), decision.getReason());
    }

    private synchronized boolean shouldLog(String actor, LandProtectionDecision decision) {
        return consumeCooldown("log|" + actor + "|" + decision.getAction() + "|" + decision.getChunkKey());
    }

    private synchronized boolean shouldNotify(String actor, LandProtectionDecision decision) {
        return consumeCooldown("notice|" + actor + "|" + decision.getChunkKey());
    }

    private boolean consumeCooldown(String key) {
        long now = System.currentTimeMillis();
        Long previous = notices.get(key);
        if (previous != null && now - previous.longValue() < NOTICE_COOLDOWN_MILLIS) return false;
        notices.put(key, now);
        return true;
    }

    public LandProtectionMode getMode() {
        return runtime.getMode();
    }
}
