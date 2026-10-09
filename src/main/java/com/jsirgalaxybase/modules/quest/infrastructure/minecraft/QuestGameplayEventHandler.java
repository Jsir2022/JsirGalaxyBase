package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.quest.core.QuestObservationPlan;
import com.jsirgalaxybase.quest.core.QuestObservationPlanProvider;
import com.jsirgalaxybase.quest.core.QuestClock;
import com.jsirgalaxybase.quest.core.QuestScoreObservation;
import com.jsirgalaxybase.quest.core.QuestCraftingStatisticObservation;
import com.jsirgalaxybase.quest.core.GameplayFact;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** Captures server facts and delegates bounded periodic progress reconciliation to the runtime sink. */
public final class QuestGameplayEventHandler {
    private final QuestFactSink sink;
    private final MinecraftQuestFactFactory facts;
    private final QuestObservationPlanProvider observationPlans;
    private final QuestClock clock;
    private final Map<net.minecraft.entity.player.EntityPlayer, InventorySession> inventorySessions =
        new IdentityHashMap<net.minecraft.entity.player.EntityPlayer, InventorySession>();

    public QuestGameplayEventHandler(QuestFactSink sink, MinecraftQuestFactFactory facts) {
        this(sink, facts, new QuestObservationPlanProvider() {
            @Override public QuestObservationPlan current() { return QuestObservationPlan.empty(); }
        });
    }

    public QuestGameplayEventHandler(QuestFactSink sink, MinecraftQuestFactFactory facts,
        QuestObservationPlanProvider observationPlans) {
        this(sink, facts, observationPlans, new QuestClock() {
            @Override public long currentTimeMillis() { return System.currentTimeMillis(); }
        });
    }

    public QuestGameplayEventHandler(QuestFactSink sink, MinecraftQuestFactFactory facts,
        QuestObservationPlanProvider observationPlans, QuestClock clock) {
        if (sink == null || facts == null) throw new IllegalArgumentException("sink and facts are required");
        if (observationPlans == null) throw new IllegalArgumentException("observationPlans is required");
        if (clock == null) throw new IllegalArgumentException("clock is required");
        this.sink = sink;
        this.facts = facts;
        this.observationPlans = observationPlans;
        this.clock = clock;
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
        if (produced > 0) {
            final ItemStack output = event.crafting.copy();
            submit(() -> facts.itemProduced(event.player, output, produced, "craft"));
            submitCraftingStatistics(event.player, output, "craft");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemSmelted(ItemSmeltedEvent event) {
        if (!valid(event.player) || event.smelting == null) return;
        final ItemStack output = event.smelting.copy();
        submit(() -> facts.itemProduced(event.player, output, Math.max(1, output.stackSize), "smelt"));
        submitCraftingStatistics(event.player, output, "smelt");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemAnvil(AnvilRepairEvent event) {
        if (!valid(event.entityPlayer) || event.output == null) return;
        final ItemStack output = event.output.copy();
        if (output.stackSize <= 0) return;
        submit(() -> facts.itemProduced(event.entityPlayer, output, output.stackSize, "anvil"));
        submitCraftingStatistics(event.entityPlayer, output, "anvil");
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
            if (plan.observesInventory()) {
                if (plan.observesBqPartyInventory()) {
                    InventorySession session = inventorySession(event.player);
                    final GameplayFact snapshot = facts.inventoryObserved(event.player, event.player.inventory,
                        session.id);
                    // A login event may claim a new session. Tick retries must be fenced
                    // updates; they must never reclaim a lease held by another server.
                    submit(() -> snapshot);
                } else {
                    submit(() -> facts.inventoryObserved(event.player, event.player.inventory));
                }
            }
            if (plan.observesFluidInventory()) submit(() -> facts.fluidInventoryObserved(event.player, event.player.inventory));
            for (final QuestScoreObservation score : plan.getScores()) {
                submit(() -> facts.scoreObserved(event.player, score.getName(), score.getDisplayName(),
                    score.getCriteria()));
            }
        }
        if (event.player.ticksExisted % 60 == 0) {
            if (plan.observesMeeting()) submit(() -> facts.nearbyEntitiesObserved(event.player,
                plan.getMeetingRadius()));
            if (plan.observesXp()) submit(() -> facts.xpObserved(event.player));
            if (plan.observesPlayerLockedProgress()) {
                sink.reconcileLockedProgress(event.player.getUniqueID(), clock.currentTimeMillis());
            }
        }
        if (event.player.ticksExisted % 100 == 0 && plan.observesLocation()) {
            submit(() -> facts.locationObserved(event.player));
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event == null || !valid(event.player)) return;
        QuestObservationPlan plan = observationPlans.current();
        if (plan == null || !plan.observesInventory() || !plan.observesBqPartyInventory()) return;
        InventorySession session = new InventorySession(UUID.randomUUID());
        inventorySessions.put(event.player, session);
        GameplayFact snapshot = facts.inventoryObserved(event.player, event.player.inventory, session.id);
        submitInventorySession(snapshot);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        if (event == null || event.player == null || !(event.player instanceof net.minecraft.entity.player.EntityPlayerMP)
            || event.player instanceof FakePlayer) return;
        InventorySession session = inventorySessions.remove(event.player);
        if (session == null) return;
        try {
            sink.endInventorySession(event.player.getUniqueID(), session.id);
        } catch (RuntimeException failure) {
            GalaxyBase.LOG.error("Failed to expire quest inventory session", failure);
        }
    }

    private InventorySession inventorySession(net.minecraft.entity.player.EntityPlayer player) {
        InventorySession session = inventorySessions.get(player);
        if (session == null) {
            session = new InventorySession(UUID.randomUUID());
            inventorySessions.put(player, session);
        }
        return session;
    }

    private void submitInventorySession(GameplayFact fact) {
        try {
            sink.beginInventorySession(fact);
        } catch (Exception failure) {
            GalaxyBase.LOG.error("Failed to publish quest inventory session", failure);
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

    private static final class InventorySession {
        private final UUID id;
        private InventorySession(UUID id) { this.id = id; }
    }

    private void submitCraftingStatistics(final net.minecraft.entity.player.EntityPlayer player, ItemStack trigger,
        final String triggerChannel) {
        QuestObservationPlan plan = observationPlans.current();
        if (plan == null || !plan.observesCraftingStatistics() || trigger == null || trigger.getItem() == null) return;
        final ItemStack output = trigger.copy();
        for (final QuestCraftingStatisticObservation statistic : plan.getCraftingStatistics()) {
            submit(() -> facts.craftingStatisticObserved(player, statistic.getRegistryName(), statistic.getMeta(),
                output, triggerChannel));
        }
    }
}
