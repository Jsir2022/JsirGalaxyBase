package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.QuestEvaluationRequest;
import com.jsirgalaxybase.quest.core.QuestObservationPlan;
import com.jsirgalaxybase.quest.core.QuestObservationPlanProvider;
import com.jsirgalaxybase.quest.core.CraftingStatisticsDetectionTarget;
import com.jsirgalaxybase.quest.core.QuestClock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraft.item.ItemStack;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class QuestGameplayEventHandlerFailureTest {
    @Test
    public void itemInteractionDispatchMatchesBqBlockAndAirActions() {
        assertEquals(true, QuestGameplayEventHandler.isItemInteractionAction(Action.LEFT_CLICK_BLOCK));
        assertEquals(true, QuestGameplayEventHandler.isItemInteractionAction(Action.RIGHT_CLICK_BLOCK));
        assertEquals("BQ treats right-click-air as an interaction, not a block hit", true,
            QuestGameplayEventHandler.isItemInteractionAction(Action.RIGHT_CLICK_AIR));
        assertEquals(false, QuestGameplayEventHandler.isItemInteractionAction(null));
    }

    @Test
    public void lockedProgressDatabaseFailureDoesNotEscapeAndNextEligibleTickCanRetry() {
        final int[] attempts = {0};
        QuestFactSink sink = new QuestFactSink() {
            @Override public void accept(GameplayFact fact) { }
            @Override public void reconcileLockedProgress(UUID playerId, long now) {
                if (++attempts[0] == 1) throw new IllegalStateException("simulated database outage");
            }
        };
        QuestGameplayEventHandler handler = new QuestGameplayEventHandler(sink,
            new MinecraftQuestFactFactory("test"));

        handler.reconcileLockedProgress(UUID.fromString("00000000-0000-0000-0000-000000000001"), 10L);
        handler.reconcileLockedProgress(UUID.fromString("00000000-0000-0000-0000-000000000001"), 20L);

        assertEquals("the next scheduled evaluation gets another attempt", 2, attempts[0]);
    }

    @Test
    public void craftingFactIsRejectedWhenEventTimeEligibilityCannotBeCaptured() {
        final int[] accepted = {0};
        QuestFactSink sink = new QuestFactSink() {
            @Override public void accept(GameplayFact fact) { accepted[0]++; }
        };
        QuestGameplayEventHandler handler = new QuestGameplayEventHandler(sink,
            new MinecraftQuestFactFactory("snapshot-failure-test"), new QuestObservationPlanProvider() {
                @Override public QuestObservationPlan current() { return QuestObservationPlan.empty(); }
            }, new QuestClock() { @Override public long currentTimeMillis() { return 10L; } },
            new QuestGameplayEventHandler.CraftingStatisticFactProducer() {
                @Override public Map<String, String> outputAttributes(ItemStack trigger, int amount, String channel) {
                    return Collections.emptyMap();
                }
                @Override public GameplayFact create(net.minecraft.entity.player.EntityPlayer triggerPlayer,
                    UUID observedPlayerId, String registryName, int meta, ItemStack trigger, String triggerChannel,
                    boolean bqPartyOnlineSnapshot) { return null; }
            }, trigger -> Collections.<UUID>emptyList(),
            new QuestGameplayEventHandler.AutomaticCraftingStatisticsDetector() {
                @Override public void detect(UUID playerId, CraftingStatisticsDetectionTarget target, long now) { }
            }, fact -> { throw new IllegalStateException("simulated PostgreSQL membership outage"); });
        GameplayFact fact = new GameplayFact("snapshot-failure-test", "craft-1", UUID.randomUUID(),
            "galaxy:item_produced", 10L, Collections.singletonMap("channel", "craft"));

        assertFalse(handler.submitFact(fact));
        assertEquals("without a trustworthy event-time roster the fact is not retried under later state", 0,
            accepted[0]);
    }
}
