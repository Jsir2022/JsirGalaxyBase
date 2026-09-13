package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.quest.core.ParticipantType;
import com.jsirgalaxybase.quest.core.RewardDeliveryHandler;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;
import com.jsirgalaxybase.quest.core.RewardDeliveryOutcome;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreDummyCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

/** Crash-recoverable scoreboard reward: persist the absolute target first, then idempotently set it. */
public final class MinecraftScoreboardRewardDeliveryHandler implements RewardDeliveryHandler {
    private final QuestPlayerResolver players;
    private final QuestPlayerDataFlusher flusher;
    public MinecraftScoreboardRewardDeliveryHandler(QuestPlayerResolver players, QuestPlayerDataFlusher flusher) {
        if (players == null || flusher == null) throw new IllegalArgumentException("players and flusher are required");
        this.players = players;
        this.flusher = flusher;
    }
    @Override public String getTypeId() { return "bq_standard:scoreboard"; }
    @Override public RewardDeliveryOutcome deliver(RewardDeliveryLease lease) {
        if (lease.getParticipantId().getType() != ParticipantType.PLAYER) return RewardDeliveryOutcome.failed(
            "unsupported-participant:" + lease.getParticipantId().getType(), false);
        EntityPlayerMP player = players.findOnline(lease.getParticipantId().getId());
        if (player == null) return RewardDeliveryOutcome.failed("player-offline", true);
        String objectiveName = lease.getReward().getParameters().get("score");
        if (objectiveName == null || objectiveName.trim().isEmpty()) return RewardDeliveryOutcome.failed(
            "invalid-reward:score", false);
        int value;
        try { value = Integer.parseInt(lease.getReward().getParameters().get("value")); }
        catch (RuntimeException invalid) { return RewardDeliveryOutcome.failed("invalid-reward:value", false); }
        Scoreboard board = player.getWorldScoreboard();
        if (board == null) return RewardDeliveryOutcome.failed("scoreboard-unavailable", true);
        ScoreObjective objective;
        try { objective = objective(board, objectiveName, lease.getReward().getParameters().get("type")); }
        catch (RuntimeException invalid) { return RewardDeliveryOutcome.failed("invalid-reward:objective", false); }
        if (objective.getCriteria().isReadOnly()) return RewardDeliveryOutcome.failed("readonly-score", false);
        Score score = board.func_96529_a(player.getCommandSenderName(), objective);
        String marker = QuestRewardMarker.key(lease.getEntitlementKey());
        NBTTagCompound persisted = QuestRewardMarker.persisted(player);
        NBTTagCompound markers = persisted.getCompoundTag(QuestRewardMarker.ROOT);
        int target;
        if (markers.hasKey(marker)) {
            NBTBase stored = markers.getTag(marker);
            if (!(stored instanceof NBTTagCompound)) return RewardDeliveryOutcome.delivered();
            target = ((NBTTagCompound) stored).getInteger("scoreTarget");
        } else {
            target = targetScore(score.getScorePoints(), value,
                bool(lease.getReward().getParameters().get("relative"), true));
            NBTTagCompound stored = new NBTTagCompound();
            stored.setInteger("scoreTarget", target);
            markers.setTag(marker, stored);
            persisted.setTag(QuestRewardMarker.ROOT, markers);
            try { flusher.flush(player); }
            catch (RuntimeException failure) {
                markers.removeTag(marker);
                persisted.setTag(QuestRewardMarker.ROOT, markers);
                return RewardDeliveryOutcome.failed("player-save-failed:" + failure.getClass().getSimpleName(), true);
            }
        }
        score.setScorePoints(target);
        return RewardDeliveryOutcome.delivered();
    }
    static int targetScore(int current, int value, boolean relative) {
        if (!relative) return value;
        long target = (long) current + value;
        return target > Integer.MAX_VALUE ? Integer.MAX_VALUE : target < Integer.MIN_VALUE ? Integer.MIN_VALUE
            : (int) target;
    }
    private static ScoreObjective objective(Scoreboard board, String name, String criteriaName) {
        ScoreObjective existing = board.getObjective(name);
        if (existing != null) return existing;
        IScoreObjectiveCriteria criteria = (IScoreObjectiveCriteria) IScoreObjectiveCriteria.field_96643_a.get(criteriaName);
        ScoreObjective created = board.addScoreObjective(name, criteria == null ? new ScoreDummyCriteria(name) : criteria);
        created.setDisplayName(name);
        return created;
    }
    private static boolean bool(String value, boolean fallback) {
        return value == null ? fallback : "1".equals(value) || Boolean.parseBoolean(value);
    }
}
