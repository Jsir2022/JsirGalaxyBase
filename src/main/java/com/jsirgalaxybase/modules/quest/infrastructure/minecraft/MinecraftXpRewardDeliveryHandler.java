package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.quest.core.ParticipantType;
import com.jsirgalaxybase.quest.core.RewardDeliveryHandler;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;
import com.jsirgalaxybase.quest.core.RewardDeliveryOutcome;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S1FPacketSetExperience;

/** BQ-compatible XP reward with a persisted entitlement marker. */
public final class MinecraftXpRewardDeliveryHandler implements RewardDeliveryHandler {
    private final QuestPlayerResolver players;
    private final QuestPlayerDataFlusher flusher;

    public MinecraftXpRewardDeliveryHandler(QuestPlayerResolver players, QuestPlayerDataFlusher flusher) {
        if (players == null || flusher == null) throw new IllegalArgumentException("players and flusher are required");
        this.players = players;
        this.flusher = flusher;
    }

    @Override public String getTypeId() { return "bq_standard:xp"; }

    @Override public RewardDeliveryOutcome deliver(RewardDeliveryLease lease) {
        if (lease.getParticipantId().getType() != ParticipantType.PLAYER) {
            return RewardDeliveryOutcome.failed("unsupported-participant:" + lease.getParticipantId().getType(), false);
        }
        EntityPlayerMP player = players.findOnline(lease.getParticipantId().getId());
        if (player == null) return RewardDeliveryOutcome.failed("player-offline", true);
        String marker = QuestRewardMarker.key(lease.getEntitlementKey());
        NBTTagCompound persisted = QuestRewardMarker.persisted(player);
        NBTTagCompound markers = persisted.getCompoundTag(QuestRewardMarker.ROOT);
        if (markers.getBoolean(marker)) return RewardDeliveryOutcome.delivered();

        int oldTotal = player.experienceTotal;
        int oldLevel = player.experienceLevel;
        float oldProgress = player.experience;
        int amount;
        try { amount = Integer.parseInt(lease.getReward().getParameters().get("amount")); }
        catch (RuntimeException invalid) { return RewardDeliveryOutcome.failed("invalid-reward:amount", false); }
        long points = "levels".equals(lease.getReward().getParameters().get("xpUnit")) ? levelXp(amount) : amount;
        addPoints(player, points);
        markers.setBoolean(marker, true);
        persisted.setTag(QuestRewardMarker.ROOT, markers);
        try {
            flusher.flush(player);
        } catch (RuntimeException failure) {
            player.experienceTotal = oldTotal;
            player.experienceLevel = oldLevel;
            player.experience = oldProgress;
            markers.removeTag(marker);
            persisted.setTag(QuestRewardMarker.ROOT, markers);
            return RewardDeliveryOutcome.failed("player-save-failed:" + failure.getClass().getSimpleName(), true);
        }
        player.playerNetServerHandler.sendPacket(
            new S1FPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel));
        return RewardDeliveryOutcome.delivered();
    }

    static void addPoints(EntityPlayerMP player, long points) {
        long total = Math.max(0L, playerXp(player) + points);
        player.experienceTotal = total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        player.experienceLevel = xpLevel(total);
        long start = levelXp(player.experienceLevel);
        player.experience = Math.max(0F, (float) ((double) (total - start) / (double) barCap(player.experienceLevel)));
    }

    static long levelXp(int level) {
        if (level <= 0) return 0L;
        if (level < 16) return level * 17L;
        if (level < 31) return (long) (1.5D * level * level - 29.5D * level + 360D);
        return (long) (3.5D * level * level - 151.5D * level + 2220D);
    }

    static long barCap(int level) {
        if (level < 16) return 2L * level + 7L;
        if (level < 31) return 5L * level - 38L;
        return 9L * level - 158L;
    }

    static int xpLevel(long points) {
        if (points <= 0L) return 0;
        int low = 0;
        int high = Integer.MAX_VALUE;
        while (low < high) {
            int middle = low + (int) (((long) high - low + 1L) / 2L);
            if (levelXp(middle) <= points) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    static long playerXp(EntityPlayerMP player) {
        return levelXp(player.experienceLevel) + (long) (barCap(player.experienceLevel)
            * Math.max(0D, player.experience));
    }
}
