package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.quest.core.QuestObservationPlan;
import com.jsirgalaxybase.quest.core.QuestObservationPlanProvider;
import com.jsirgalaxybase.quest.core.QuestScoreObservation;
import com.jsirgalaxybase.quest.core.QuestCraftingStatisticObservation;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;

/** Event capture only. It never traverses quest definitions or mutates player progress directly. */
public final class QuestGameplayEventHandler {
    private final QuestFactSink sink;
    private final MinecraftQuestFactFactory facts;
    private final QuestObservationPlanProvider observationPlans;

    public QuestGameplayEventHandler(QuestFactSink sink, MinecraftQuestFactFactory facts) {
        this(sink, facts, new QuestObservationPlanProvider() {
            @Override public QuestObservationPlan current() { return QuestObservationPlan.empty(); }
        });
    }

    public QuestGameplayEventHandler(QuestFactSink sink, MinecraftQuestFactFactory facts,
        QuestObservationPlanProvider observationPlans) {
        if (sink == null || facts == null) throw new IllegalArgumentException("sink and facts are required");
        if (observationPlans == null) throw new IllegalArgumentException("observationPlans is required");
        this.sink = sink;
        this.facts = facts;
        this.observationPlans = observationPlans;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemCrafted(ItemCraftedEvent event) {
        if (!valid(event.player) || event.crafting == null) return;
        int amount = event.crafting.stackSize;
        if (amount <= 0 && event.craftMatrix instanceof InventoryCrafting) {
            ItemStack actual = CraftingManager.getInstance().findMatchingRecipe(
                (InventoryCrafting) event.craftMatrix, event.player.worldObj);
            amount = actual == null ? 0 : actual.stackSize;
        }
        final int produced = amount;
        if (produced > 0) submit(() -> facts.itemProduced(event.player, event.crafting, produced, "craft"));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemSmelted(ItemSmeltedEvent event) {
        if (!valid(event.player) || event.smelting == null) return;
        submit(() -> facts.itemProduced(event.player, event.smelting, Math.max(1, event.smelting.stackSize), "smelt"));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemAnvil(AnvilRepairEvent event) {
        if (!valid(event.entityPlayer) || event.output == null) return;
        submit(() -> facts.itemProduced(event.entityPlayer, event.output, Math.max(1, event.output.stackSize), "anvil"));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityKilled(LivingDeathEvent event) {
        if (event.isCanceled() || event.source == null || !(event.source.getEntity() instanceof net.minecraft.entity.player.EntityPlayer)) return;
        net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) event.source.getEntity();
        if (!valid(player)) return;
        submit(() -> facts.entityKilled(player, event.entityLiving, event.source.damageType));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BreakEvent event) {
        if (event.isCanceled() || !valid(event.getPlayer())) return;
        submit(() -> facts.blockBroken(event.getPlayer(), event.block, event.blockMetadata, event.x, event.y, event.z));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityAttack(AttackEntityEvent event) {
        if (event.isCanceled() || !valid(event.entityPlayer) || event.target == null) return;
        submit(() -> facts.entityInteracted(event.entityPlayer, event.target, event.entityPlayer.getHeldItem(), true));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityInteract(EntityInteractEvent event) {
        if (event.isCanceled() || !valid(event.entityPlayer) || event.target == null) return;
        submit(() -> facts.entityInteracted(event.entityPlayer, event.target, event.entityPlayer.getHeldItem(), false));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCanceled() || !valid(event.entityPlayer)) return;
        if (event.action != Action.LEFT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_BLOCK) return;
        net.minecraft.block.Block block = event.entityPlayer.worldObj.getBlock(event.x, event.y, event.z);
        int meta = event.entityPlayer.worldObj.getBlockMetadata(event.x, event.y, event.z);
        submit(() -> facts.itemInteracted(event.entityPlayer, event.entityPlayer.getHeldItem(), block, meta,
            event.x, event.y, event.z, event.action == Action.LEFT_CLICK_BLOCK));
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !valid(event.player)) return;
        QuestObservationPlan plan = observationPlans.current();
        if (plan == null) return;
        if (event.player.ticksExisted % 20 == 0) {
            if (plan.observesInventory()) submit(() -> facts.inventoryObserved(event.player, event.player.inventory));
            if (plan.observesFluidInventory()) submit(() -> facts.fluidInventoryObserved(event.player, event.player.inventory));
            for (final QuestScoreObservation score : plan.getScores()) {
                submit(() -> facts.scoreObserved(event.player, score.getName(), score.getDisplayName(),
                    score.getCriteria()));
            }
            for (final QuestCraftingStatisticObservation statistic : plan.getCraftingStatistics()) {
                submit(() -> facts.craftingStatisticObserved(event.player, statistic.getRegistryName(), statistic.getMeta()));
            }
        }
        if (event.player.ticksExisted % 60 == 0) {
            if (plan.observesMeeting()) submit(() -> facts.nearbyEntitiesObserved(event.player,
                plan.getMeetingRadius()));
            if (plan.observesXp()) submit(() -> facts.xpObserved(event.player));
        }
        if (event.player.ticksExisted % 100 == 0 && plan.observesLocation()) {
            submit(() -> facts.locationObserved(event.player));
        }
    }

    private boolean valid(net.minecraft.entity.player.EntityPlayer player) {
        // Facts are a server-player boundary.  In particular, FakePlayer and
        // client/integrated-world observations must never enter the shared
        // cross-server progress ledger.
        return player instanceof net.minecraft.entity.player.EntityPlayerMP
            && !(player instanceof FakePlayer) && player.worldObj != null && !player.worldObj.isRemote;
    }

    private void submit(java.util.concurrent.Callable<com.jsirgalaxybase.quest.core.GameplayFact> factory) {
        try {
            sink.accept(factory.call());
        } catch (Exception failure) {
            GalaxyBase.LOG.error("Failed to persist quest gameplay fact", failure);
        }
    }
}
