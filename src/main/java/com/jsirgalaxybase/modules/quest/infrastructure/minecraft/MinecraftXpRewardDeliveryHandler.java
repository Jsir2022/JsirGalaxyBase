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
        if (player == null) return RewardDeliveryOutcome.deferred("player-offline");
        return deliverToPlayer(lease, new MinecraftExperiencePort(player, flusher));
    }

    static RewardDeliveryOutcome deliverToPlayer(RewardDeliveryLease lease, ExperiencePort player) {
        String marker = QuestRewardMarker.key(lease.getEntitlementKey());
        if (player.hasMarker(marker)) return RewardDeliveryOutcome.delivered();

        int oldTotal = player.experienceTotal();
        int oldLevel = player.experienceLevel();
        float oldProgress = player.experienceProgress();
        int amount;
        try { amount = Integer.parseInt(lease.getReward().getParameters().get("amount")); }
        catch (RuntimeException invalid) { return RewardDeliveryOutcome.failed("invalid-reward:amount", false); }
        long points = "levels".equals(lease.getReward().getParameters().get("xpUnit")) ? levelXp(amount) : amount;
        long total = Math.max(0L, playerXp(oldLevel, oldProgress) + points);
        int storedTotal = total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        int level = xpLevel(total);
        long start = levelXp(level);
        float progress = Math.max(0F, (float) ((double) (total - start) / (double) barCap(level)));
        player.setExperience(storedTotal, level, progress);
        player.setMarker(marker);
        try {
            player.flush();
        } catch (RuntimeException failure) {
            player.setExperience(oldTotal, oldLevel, oldProgress);
            player.removeMarker(marker);
            return RewardDeliveryOutcome.failed("player-save-failed:" + failure.getClass().getSimpleName(), true);
        }
        player.syncExperience();
        return RewardDeliveryOutcome.delivered();
    }

    interface ExperiencePort {
        int experienceTotal();
        int experienceLevel();
        float experienceProgress();
        void setExperience(int total, int level, float progress);
        boolean hasMarker(String marker);
        void setMarker(String marker);
        void removeMarker(String marker);
        void flush();
        void syncExperience();
    }

    private static final class MinecraftExperiencePort implements ExperiencePort {
        private final EntityPlayerMP player;
        private final QuestPlayerDataFlusher flusher;
        private final NBTTagCompound persisted;
        private final NBTTagCompound markers;

        private MinecraftExperiencePort(EntityPlayerMP player, QuestPlayerDataFlusher flusher) {
            this.player = player;
            this.flusher = flusher;
            this.persisted = QuestRewardMarker.persisted(player);
            this.markers = persisted.getCompoundTag(QuestRewardMarker.ROOT);
        }

        @Override public int experienceTotal() { return player.experienceTotal; }
        @Override public int experienceLevel() { return player.experienceLevel; }
        @Override public float experienceProgress() { return player.experience; }
        @Override public void setExperience(int total, int level, float progress) {
            player.experienceTotal = total;
            player.experienceLevel = level;
            player.experience = progress;
        }
        @Override public boolean hasMarker(String marker) { return markers.getBoolean(marker); }
        @Override public void setMarker(String marker) {
            markers.setBoolean(marker, true);
            persisted.setTag(QuestRewardMarker.ROOT, markers);
        }
        @Override public void removeMarker(String marker) {
            markers.removeTag(marker);
            persisted.setTag(QuestRewardMarker.ROOT, markers);
        }
        @Override public void flush() { flusher.flush(player); }
        @Override public void syncExperience() {
            player.playerNetServerHandler.sendPacket(
                new S1FPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel));
        }
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
        return playerXp(player.experienceLevel, player.experience);
    }

    private static long playerXp(int level, float progress) {
        return levelXp(level) + (long) (barCap(level) * Math.max(0D, progress));
    }
}
