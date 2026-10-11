package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.quest.core.QuestObservationPlan;
import com.jsirgalaxybase.quest.core.QuestObservationPlanProvider;
import com.jsirgalaxybase.quest.core.CraftingStatisticsDetectionTarget;
import com.jsirgalaxybase.quest.core.QuestClock;
import com.jsirgalaxybase.quest.core.QuestScoreObservation;
import com.jsirgalaxybase.quest.core.QuestCraftingStatisticObservation;
import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.QuestEvaluationRequest;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

/** Captures server facts and delegates bounded periodic progress reconciliation to the runtime sink. */
public final class QuestGameplayEventHandler {
    private final QuestFactSink sink;
    private final MinecraftQuestFactFactory facts;
    private final CraftingStatisticFactProducer craftingStatisticFacts;
    private final CraftingStatisticMemberResolver craftingStatisticMembers;
    private final AutomaticCraftingStatisticsDetector automaticCraftingStatisticsDetector;
    private final CraftingEventEligibilitySnapshotter craftingEventEligibilitySnapshotter;
    private final QuestObservationPlanProvider observationPlans;
    private final QuestClock clock;
    private final Map<net.minecraft.entity.player.EntityPlayer, InventorySession> inventorySessions =
        new IdentityHashMap<net.minecraft.entity.player.EntityPlayer, InventorySession>();
    private final OrderedQuestFactRetryQueue retryQueue = new OrderedQuestFactRetryQueue();
    private int retryTicksRemaining = OrderedQuestFactRetryQueue.RETRY_INTERVAL_TICKS;

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
        this(sink, facts, observationPlans, clock, new CraftingStatisticFactProducer() {
            @Override public Map<String, String> outputAttributes(ItemStack trigger, int amount, String channel) {
                return facts.itemProducedAttributes(trigger, amount, channel);
            }

            @Override public GameplayFact create(net.minecraft.entity.player.EntityPlayer triggerPlayer,
                UUID observedPlayerId, String registryName, int meta, ItemStack trigger, String triggerChannel,
                boolean bqPartyOnlineSnapshot) {
                if (triggerPlayer == null || !triggerPlayer.getUniqueID().equals(observedPlayerId)) return null;
                return facts.craftingStatisticObserved(triggerPlayer, registryName, meta, trigger, triggerChannel,
                    bqPartyOnlineSnapshot);
            }
        });
    }

    QuestGameplayEventHandler(QuestFactSink sink, MinecraftQuestFactFactory facts,
        QuestObservationPlanProvider observationPlans, QuestClock clock,
        CraftingStatisticFactProducer craftingStatisticFacts) {
        this(sink, facts, observationPlans, clock, craftingStatisticFacts,
            new CraftingStatisticMemberResolver() {
                @Override public List<UUID> localOnlineMembers(net.minecraft.entity.player.EntityPlayer player) {
                    return player == null ? Collections.<UUID>emptyList()
                        : Collections.singletonList(player.getUniqueID());
                }
            });
    }

    public QuestGameplayEventHandler(QuestFactSink sink, MinecraftQuestFactFactory facts,
        QuestObservationPlanProvider observationPlans, QuestClock clock,
        CraftingStatisticFactProducer craftingStatisticFacts, CraftingStatisticMemberResolver statisticMembers) {
        this(sink, facts, observationPlans, clock, craftingStatisticFacts, statisticMembers,
            new AutomaticCraftingStatisticsDetector() {
                @Override public void detect(UUID playerId, CraftingStatisticsDetectionTarget target, long now) { }
            });
    }

    public QuestGameplayEventHandler(QuestFactSink sink, MinecraftQuestFactFactory facts,
        QuestObservationPlanProvider observationPlans, QuestClock clock,
        CraftingStatisticFactProducer craftingStatisticFacts, CraftingStatisticMemberResolver statisticMembers,
        AutomaticCraftingStatisticsDetector automaticCraftingStatisticsDetector) {
        this(sink, facts, observationPlans, clock, craftingStatisticFacts, statisticMembers,
            automaticCraftingStatisticsDetector, null);
    }

    public QuestGameplayEventHandler(QuestFactSink sink, MinecraftQuestFactFactory facts,
        QuestObservationPlanProvider observationPlans, QuestClock clock,
        CraftingStatisticFactProducer craftingStatisticFacts, CraftingStatisticMemberResolver statisticMembers,
        AutomaticCraftingStatisticsDetector automaticCraftingStatisticsDetector,
        CraftingEventEligibilitySnapshotter craftingEventEligibilitySnapshotter) {
        if (sink == null || facts == null) throw new IllegalArgumentException("sink and facts are required");
        if (observationPlans == null) throw new IllegalArgumentException("observationPlans is required");
        if (clock == null) throw new IllegalArgumentException("clock is required");
        if (craftingStatisticFacts == null) throw new IllegalArgumentException("craftingStatisticFacts is required");
        if (statisticMembers == null) throw new IllegalArgumentException("statisticMembers is required");
        if (automaticCraftingStatisticsDetector == null) {
            throw new IllegalArgumentException("automaticCraftingStatisticsDetector is required");
        }
        this.sink = sink;
        this.facts = facts;
        this.craftingStatisticFacts = craftingStatisticFacts;
        this.observationPlans = observationPlans;
        this.clock = clock;
        this.craftingStatisticMembers = statisticMembers;
        this.automaticCraftingStatisticsDetector = automaticCraftingStatisticsDetector;
        this.craftingEventEligibilitySnapshotter = craftingEventEligibilitySnapshotter;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemCrafted(ItemCraftedEvent event) {
        if (!valid(event.player) || event.crafting == null) return;
        int amount = event.crafting.stackSize;
        if (amount == 0 && event.craftMatrix instanceof InventoryCrafting) {
            amount = resolveCraftedOutputAmount(amount, true, new IntSupplier() {
                @Override public int getAsInt() {
                    ItemStack actual = CraftingManager.getInstance().findMatchingRecipe(
                        (InventoryCrafting) event.craftMatrix, event.player.worldObj);
                    return actual == null ? event.crafting.stackSize : actual.stackSize;
                }
            });
        }
        final int produced = amount;
        if (produced > 0) {
            final ItemStack output = event.crafting.copy();
            submit(() -> facts.itemProduced(event.player, output, produced, "craft"));
            submitCraftingStatistics(event.player, output, produced, "craft");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemSmelted(ItemSmeltedEvent event) {
        if (!valid(event.player) || event.smelting == null) return;
        final ItemStack output = event.smelting.copy();
        final int produced = Math.max(1, output.stackSize);
        submit(() -> facts.itemProduced(event.player, output, produced, "smelt"));
        submitCraftingStatistics(event.player, output, produced, "smelt");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemAnvil(AnvilRepairEvent event) {
        if (!valid(event.entityPlayer) || event.output == null) return;
        final ItemStack output = event.output.copy();
        if (output.stackSize <= 0) return;
        submit(() -> facts.itemProduced(event.entityPlayer, output, output.stackSize, "anvil"));
        submitCraftingStatistics(event.entityPlayer, output, output.stackSize, "anvil");
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
        if (!isItemInteractionAction(event.action)) return;
        net.minecraft.block.Block block = event.entityPlayer.worldObj.getBlock(event.x, event.y, event.z);
        int meta = event.entityPlayer.worldObj.getBlockMetadata(event.x, event.y, event.z);
        submit(() -> facts.itemInteracted(event.entityPlayer, event.entityPlayer.getHeldItem(), block, meta,
            event.x, event.y, event.z, event.action == Action.LEFT_CLICK_BLOCK));
    }

    static boolean isItemInteractionAction(Action action) {
        return action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK
            || action == Action.RIGHT_CLICK_AIR;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !valid(event.player)) return;
        QuestObservationPlan plan = observationPlans.current();
        if (plan == null) return;
        if (PeriodicTickCadence.isDue(event.player.ticksExisted, 20)) {
            boolean observeItems = plan.observesInventory();
            boolean observeFluids = plan.observesFluidInventory();
            if (plan.observesBqPartyInventory() && (observeItems || observeFluids)) {
                InventorySession session = inventorySession(event.player);
                final GameplayFact snapshot = facts.inventoryObserved(event.player, event.player.inventory,
                    session.id, observeItems, observeFluids);
                // One fenced lease carries every observed inventory domain so an item refresh
                // can never erase the fluid snapshot (or vice versa) for this live session.
                submit(() -> snapshot);
            } else {
                if (observeItems) submit(() -> facts.inventoryObserved(event.player, event.player.inventory));
                if (observeFluids) submit(() -> facts.fluidInventoryObserved(event.player, event.player.inventory));
            }
            for (final QuestScoreObservation score : plan.getScores()) {
                submit(() -> facts.scoreObserved(event.player, score.getName(), score.getDisplayName(),
                    score.getCriteria()));
            }
        }
        if (PeriodicTickCadence.isDue(event.player.ticksExisted, 60)) {
            if (plan.observesMeeting()) submit(() -> facts.nearbyEntitiesObserved(event.player,
                plan.getMeetingRadius()));
            if (plan.observesXp()) submit(() -> facts.xpObserved(event.player));
            detectAutomaticCraftingStatistics(event.player.getUniqueID(), plan, clock.currentTimeMillis());
            if (plan.observesPlayerLockedProgress()) {
                reconcileLockedProgress(event.player.getUniqueID(), clock.currentTimeMillis());
            }
        }
        if (PeriodicTickCadence.isDue(event.player.ticksExisted, 100) && plan.observesLocation()) {
            submit(() -> facts.locationObserved(event.player));
        }
    }

    void detectAutomaticCraftingStatistics(UUID playerId, QuestObservationPlan plan, long now) {
        if (playerId == null || plan == null) return;
        for (CraftingStatisticsDetectionTarget target : plan.getAutomaticCraftingStatisticsTargets()) {
            try {
                automaticCraftingStatisticsDetector.detect(playerId, target, now);
            } catch (RuntimeException failure) {
                GalaxyBase.LOG.error("Failed automatic BQ crafting-statistics detection for player {} task {}",
                    playerId, target.getTaskKey(), failure);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event == null || !valid(event.player)) return;
        QuestObservationPlan plan = observationPlans.current();
        if (plan == null || !plan.observesBqPartyInventory()
            || (!plan.observesInventory() && !plan.observesFluidInventory())) return;
        InventorySession session = new InventorySession(UUID.randomUUID());
        inventorySessions.put(event.player, session);
        GameplayFact snapshot = facts.inventoryObserved(event.player, event.player.inventory, session.id,
            plan.observesInventory(), plan.observesFluidInventory());
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

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        if (!retryQueue.isEmpty()) {
            if (--retryTicksRemaining <= 0) {
                retryTicksRemaining = OrderedQuestFactRetryQueue.RETRY_INTERVAL_TICKS;
                OrderedQuestFactRetryQueue.FlushResult result = retryQueue.retry(sink,
                    OrderedQuestFactRetryQueue.MAX_FACTS_PER_RETRY);
                if (result.getFailure() != null) {
                    GalaxyBase.LOG.error("Quest fact retry retained {} fact(s); next retry is scheduled",
                        retryQueue.pendingCount(), result.getFailure());
                }
            }
        }
        long rejected = retryQueue.drainRejectedCount();
        if (rejected > 0) {
            GalaxyBase.LOG.error("Quest fact retry capacity was exceeded; {} newest fact(s) could not be retained", rejected);
        }
    }

    @SubscribeEvent
    public void onServerStopping(FMLServerStoppingEvent event) {
        int pending = retryQueue.pendingCount();
        if (pending > 0) {
            GalaxyBase.LOG.error("Server is stopping with {} unpersisted quest fact(s); in-memory retry queue is not durable",
                pending);
        }
        long rejected = retryQueue.drainRejectedCount();
        if (rejected > 0) {
            GalaxyBase.LOG.error("Server stopped after rejecting {} quest fact(s) because retry capacity was exceeded",
                rejected);
        }
        retryQueue.clear();
    }

    void reconcileLockedProgress(UUID playerId, long now) {
        try {
            sink.reconcileLockedProgress(playerId, now);
        } catch (RuntimeException failure) {
            GalaxyBase.LOG.error("Failed to reconcile locked quest progress for player {}", playerId, failure);
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
        OrderedQuestFactRetryQueue.SubmissionResult result = retryQueue.submitInventorySession(fact, sink);
        if (result.getFailure() != null) {
            if (result.isRejected()) {
                GalaxyBase.LOG.error("Failed to publish quest inventory session {}; retry capacity exceeded",
                    fact.getFactKey(), result.getFailure());
            } else {
                GalaxyBase.LOG.error("Failed to publish quest inventory session {}; retained for ordered retry",
                    fact.getFactKey(), result.getFailure());
            }
        }
    }

    private boolean valid(net.minecraft.entity.player.EntityPlayer player) {
        // Facts are a server-player boundary.  In particular, FakePlayer and
        // client/integrated-world observations must never enter the shared
        // cross-server progress ledger.
        return player instanceof net.minecraft.entity.player.EntityPlayerMP
            && !(player instanceof FakePlayer) && player.worldObj != null && !player.worldObj.isRemote;
    }

    private boolean submit(java.util.concurrent.Callable<com.jsirgalaxybase.quest.core.GameplayFact> factory) {
        try {
            com.jsirgalaxybase.quest.core.GameplayFact fact = factory.call();
            return submitFact(fact);
        } catch (Exception failure) {
            GalaxyBase.LOG.error("Failed to persist quest gameplay fact", failure);
            return false;
        }
    }

    boolean submitFact(GameplayFact fact) {
        if (fact == null) return false;
        List<QuestEvaluationRequest> eventTimeRequests = null;
        if (isCraftingObservation(fact) && craftingEventEligibilitySnapshotter != null) {
            try {
                eventTimeRequests = craftingEventEligibilitySnapshotter.capture(fact);
                if (eventTimeRequests == null) throw new IllegalStateException(
                    "crafting event eligibility snapshot is unavailable");
            } catch (RuntimeException failure) {
                // A delayed retry must never be re-assigned to a Party or Quest that became eligible later.
                // If the authoritative event-time snapshot cannot be captured, reject this event fail-closed.
                GalaxyBase.LOG.error("Unable to freeze event-time Quest eligibility for crafting fact {}; "
                    + "the fact is being rejected", fact.getFactKey(), failure);
                return false;
            }
        }
        OrderedQuestFactRetryQueue.SubmissionResult result = eventTimeRequests == null
            ? retryQueue.submit(fact, sink) : retryQueue.submit(fact, sink, eventTimeRequests);
        if (result.getFailure() != null) {
            if (result.isRejected()) {
                GalaxyBase.LOG.error("Failed to persist quest gameplay fact {}; retry capacity exceeded",
                    fact.getFactKey(), result.getFailure());
            } else {
                GalaxyBase.LOG.error("Failed to persist quest gameplay fact {}; retained for ordered retry",
                    fact.getFactKey(), result.getFailure());
            }
        }
        return !result.isRejected();
    }

    private static boolean isCraftingObservation(GameplayFact fact) {
        if (fact == null || !"galaxy:item_produced".equals(fact.getTypeId())) return false;
        String channel = fact.getAttributes().get("channel");
        if ("craft".equals(channel) || "smelt".equals(channel) || "anvil".equals(channel)) return true;
        return "craft-statistics".equals(channel)
            && ("craft".equals(fact.getAttributes().get("trigger.channel"))
                || "smelt".equals(fact.getAttributes().get("trigger.channel"))
                || "anvil".equals(fact.getAttributes().get("trigger.channel")));
    }

    private static final class InventorySession {
        private final UUID id;
        private InventorySession(UUID id) { this.id = id; }
    }

    void submitCraftingStatistics(final net.minecraft.entity.player.EntityPlayer player, ItemStack trigger,
        int amount, final String triggerChannel) {
        try {
            QuestObservationPlan plan = observationPlans.current();
            if (plan == null || !plan.observesCraftingStatistics() || trigger == null
                || trigger.getItem() == null) return;
            final ItemStack output = trigger.copy();
            Map<String, String> triggerAttributes = craftingStatisticFacts.outputAttributes(output, amount,
                triggerChannel);
            List<QuestCraftingStatisticObservation> personalTargets =
                plan.otherCraftingStatisticsForOutput(triggerAttributes);
            if (player != null) for (final QuestCraftingStatisticObservation statistic : personalTargets) {
                UUID playerId = player.getUniqueID();
                submit(() -> craftingStatisticFacts.create(player, playerId, statistic.getRegistryName(),
                    statistic.getMeta(), output, triggerChannel, false));
            }

            List<QuestCraftingStatisticObservation> bqTargets =
                plan.bqPartyCraftingStatisticsForOutput(triggerAttributes);
            if (!bqTargets.isEmpty()) {
                final List<UUID> onlineMembers = localOnlineStatisticMembers(player);
                // BQ TaskCrafting.detect traverses participant UUIDs first, then the
                // task's required-item vector for each player's independent statistics.
                for (final UUID memberId : onlineMembers) {
                    for (final QuestCraftingStatisticObservation statistic : bqTargets) {
                        submit(() -> craftingStatisticFacts.create(player, memberId, statistic.getRegistryName(),
                            statistic.getMeta(), output, triggerChannel, true));
                    }
                }
            }
        } catch (RuntimeException failure) {
            // Observation failures must never escape a Forge event and disrupt the game tick/action.
            GalaxyBase.LOG.error("Failed to dispatch BQ crafting-statistics observations", failure);
        }
    }

    private List<UUID> localOnlineStatisticMembers(net.minecraft.entity.player.EntityPlayer player) {
        UUID sourcePlayerId = player == null ? null : player.getUniqueID();
        try {
            List<UUID> resolved = craftingStatisticMembers.localOnlineMembers(player);
            if (resolved != null && !resolved.isEmpty()) {
                LinkedHashSet<UUID> unique = new LinkedHashSet<UUID>();
                for (UUID member : resolved) if (member != null) unique.add(member);
                if (!unique.isEmpty()) return Collections.unmodifiableList(new ArrayList<UUID>(unique));
            }
        } catch (RuntimeException failure) {
            GalaxyBase.LOG.warn("Unable to resolve local online Party members for crafting statistics of {}; "
                + "falling back to the triggering player", sourcePlayerId, failure);
        }
        return sourcePlayerId == null ? Collections.<UUID>emptyList()
            : Collections.singletonList(sourcePlayerId);
    }

    static int resolveCraftedOutputAmount(int reportedAmount, boolean hasCraftMatrix,
        IntSupplier recipeAmountFallback) {
        if (reportedAmount != 0 || !hasCraftMatrix || recipeAmountFallback == null) return reportedAmount;
        return recipeAmountFallback.getAsInt();
    }

    public interface CraftingStatisticFactProducer {
        Map<String, String> outputAttributes(ItemStack trigger, int amount, String channel);
        GameplayFact create(net.minecraft.entity.player.EntityPlayer triggerPlayer, UUID observedPlayerId,
            String registryName, int meta, ItemStack trigger, String triggerChannel,
            boolean bqPartyOnlineSnapshot);
    }

    public interface CraftingStatisticMemberResolver {
        List<UUID> localOnlineMembers(net.minecraft.entity.player.EntityPlayer triggerPlayer);
    }

    public interface AutomaticCraftingStatisticsDetector {
        void detect(UUID authenticatedPlayerId, CraftingStatisticsDetectionTarget target, long now);
    }

    /** Captures the authoritative request set synchronously at a crafting event's origin. */
    public interface CraftingEventEligibilitySnapshotter {
        List<QuestEvaluationRequest> capture(GameplayFact fact);
    }
}
