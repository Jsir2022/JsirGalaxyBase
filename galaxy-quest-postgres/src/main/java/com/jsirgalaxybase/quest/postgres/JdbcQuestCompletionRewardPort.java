package com.jsirgalaxybase.quest.postgres;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestCompletionRewardPort;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestDefinitionRepository;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestRuntimeRepository;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.QuestTransaction;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.RewardEntitlement;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskProgress;

/** PostgreSQL transaction boundary for BQ quest-completion rewards. */
public final class JdbcQuestCompletionRewardPort implements QuestCompletionRewardPort {
    private final QuestDefinitionRepository definitions;
    private final QuestRuntimeRepository runtime;
    private final QuestTransaction transaction;

    public JdbcQuestCompletionRewardPort(QuestDefinitionRepository definitions, QuestRuntimeRepository runtime,
        QuestTransaction transaction) {
        if (definitions == null || runtime == null || transaction == null) {
            throw new IllegalArgumentException("definitions, runtime and transaction are required");
        }
        this.definitions = definitions;
        this.runtime = runtime;
        this.transaction = transaction;
    }

    @Override public Result complete(final String sourceEntitlementKey, final ParticipantId participantId,
        final UUID targetQuestId, final long completedAt) {
        if (sourceEntitlementKey == null || sourceEntitlementKey.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceEntitlementKey is required");
        }
        return transaction.inTransaction(() -> completeInTransaction(participantId, targetQuestId, completedAt));
    }

    private Result completeInTransaction(ParticipantId participantId, UUID targetQuestId, long completedAt) {
        Optional<StoredQuestDefinition> stored = definitions.findPublished(targetQuestId);
        if (!stored.isPresent()) return Result.TARGET_NOT_FOUND;
        QuestDefinition definition = stored.get().getDefinition();
        Optional<QuestProgressSnapshot> prior = runtime.findProgress(participantId, targetQuestId,
            definition.getVersion());
        if (prior.isPresent() && prior.get().getStatus() == QuestStatus.COMPLETED) {
            return Result.ALREADY_COMPLETED;
        }

        Map<String, TaskProgress> tasks = new LinkedHashMap<String, TaskProgress>();
        for (TaskDefinition task : definition.getTasks()) {
            TaskProgress empty = TaskProgress.empty(task);
            tasks.put(task.getKey(), new TaskProgress(task.getKey(), empty.getTargets(), empty.getTargets(), true));
        }
        int cycle = prior.isPresent() ? prior.get().getCycle() : 0;
        runtime.saveProgress(new QuestProgressSnapshot(participantId, targetQuestId, definition.getVersion(),
            QuestStatus.COMPLETED, tasks, completedAt, cycle));
        Set<RewardEntitlement> entitlements = new LinkedHashSet<RewardEntitlement>();
        for (RewardDefinition reward : definition.getRewards()) {
            entitlements.add(new RewardEntitlement(participantId, definition, reward, cycle,
                definition.getBehavior().isAutoClaim()));
        }
        runtime.insertEntitlementsIfAbsent(entitlements);
        return Result.COMPLETED;
    }
}
