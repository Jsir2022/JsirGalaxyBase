package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.CommandRewardInvocation;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;
import com.jsirgalaxybase.quest.core.RewardDeliveryOutcome;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class MinecraftRewardDeliveryTest {
    @Test public void rewardInsertionMergesThenUsesEmptySlotsWithoutMutatingSource() {
        Item item = new Item();
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(new ItemStack(item, 60), null));
        List<ItemStack> after = MinecraftItemRewardDeliveryHandler.plan(before,
            Arrays.asList(new ItemStack(item, 10)));
        assertEquals(60, before.get(0).stackSize);
        assertEquals(64, after.get(0).stackSize);
        assertEquals(6, after.get(1).stackSize);
    }

    @Test public void fullInventoryRejectsWholeReward() {
        Item item = new Item();
        List<ItemStack> before = Arrays.asList(new ItemStack(item, 64));
        assertNull(MinecraftItemRewardDeliveryHandler.plan(before, Arrays.asList(new ItemStack(item, 1))));
        assertEquals(64, before.get(0).stackSize);
    }

    @Test public void typedNbtReplacesPlayerVariablesRecursively() {
        NBTTagCompound decoded = BqTypedNbtCodec.compound(
            "{\"owner:8\":\"VAR_NAME\",\"nested:10\":{\"id:8\":\"VAR_UUID\"}}");
        NBTTagCompound replaced = BqTypedNbtCodec.replacePlayerVariables(decoded, "Alex", "uuid-1");
        assertEquals("Alex", replaced.getString("owner"));
        assertEquals("uuid-1", replaced.getCompoundTag("nested").getString("id"));
        assertEquals("VAR_NAME", decoded.getString("owner"));
    }

    @Test public void xpCurveMatchesBetterQuestingBoundaries() {
        assertEquals(255L, MinecraftXpRewardDeliveryHandler.levelXp(15));
        assertEquals(272L, MinecraftXpRewardDeliveryHandler.levelXp(16));
        assertEquals(825L, MinecraftXpRewardDeliveryHandler.levelXp(30));
        assertEquals(887L, MinecraftXpRewardDeliveryHandler.levelXp(31));
        for (int level : new int[] { 0, 1, 15, 16, 30, 31, 100, 1000000 }) {
            long points = MinecraftXpRewardDeliveryHandler.levelXp(level);
            assertEquals(level, MinecraftXpRewardDeliveryHandler.xpLevel(points));
            assertTrue(MinecraftXpRewardDeliveryHandler.levelXp(level + 1) > points);
        }
    }

    @Test public void entitlementMarkersAreStableAndNamespaced() {
        assertEquals(QuestRewardMarker.key("entitlement"), QuestRewardMarker.key("entitlement"));
        assertFalse(QuestRewardMarker.key("entitlement").equals(QuestRewardMarker.key("other")));
        assertEquals(64, QuestRewardMarker.key("entitlement").length());
    }

    @Test public void relativeScoreRewardComputesOneStableSaturatingAbsoluteTarget() {
        assertEquals(15, MinecraftScoreboardRewardDeliveryHandler.targetScore(10, 5, true));
        assertEquals(5, MinecraftScoreboardRewardDeliveryHandler.targetScore(10, 5, false));
        assertEquals(Integer.MAX_VALUE,
            MinecraftScoreboardRewardDeliveryHandler.targetScore(Integer.MAX_VALUE, 1, true));
        assertEquals(Integer.MIN_VALUE,
            MinecraftScoreboardRewardDeliveryHandler.targetScore(Integer.MIN_VALUE, -1, true));
    }

    @Test public void frozenGroupRecipientIsResolvedAsTheConcretePlayerAndOfflineDeliveryIsDeferred() {
        UUID recipient=UUID.randomUUID();ParticipantId team=ParticipantId.team(UUID.randomUUID());
        RewardDeliveryLease lease=new RewardDeliveryLease("team-entitlement",team,recipient,
            new RewardDefinition("reward","bq_standard:item",Collections.<String,String>emptyMap()),1,"worker",10L);
        QuestPlayerResolver offline=id->{assertEquals(recipient,id);return null;};
        QuestPlayerDataFlusher unused=player->{throw new AssertionError("offline player must not be flushed");};
        RewardDeliveryOutcome item=new MinecraftItemRewardDeliveryHandler("bq_standard:item",offline,unused).deliver(lease);
        RewardDeliveryOutcome xp=new MinecraftXpRewardDeliveryHandler(offline,unused).deliver(lease);
        RewardDeliveryOutcome score=new MinecraftScoreboardRewardDeliveryHandler(offline,unused).deliver(lease);
        RewardDeliveryOutcome command=new MinecraftCommandRewardExecutor(offline,unused).execute(
            new CommandRewardInvocation(lease.getEntitlementKey(),lease.getParticipantId(),"galaxyreward",false));
        assertEquals(team,lease.getProgressParticipantId());assertEquals(ParticipantId.player(recipient),lease.getParticipantId());
        assertTrue(item.isDeferred());assertTrue(xp.isDeferred());assertTrue(score.isDeferred());assertTrue(command.isDeferred());
    }
}
