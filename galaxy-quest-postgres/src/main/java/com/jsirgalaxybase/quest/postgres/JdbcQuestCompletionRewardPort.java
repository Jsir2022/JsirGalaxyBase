package com.jsirgalaxybase.quest.postgres;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantMembership;
import com.jsirgalaxybase.quest.core.ParticipantMembershipResolver;
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
import com.jsirgalaxybase.quest.core.ScopedQuestAssignmentPolicy;

/** PostgreSQL transaction boundary for BQ quest-completion rewards. */
public final class JdbcQuestCompletionRewardPort implements QuestCompletionRewardPort {
    private final QuestDefinitionRepository definitions;
    private final QuestRuntimeRepository runtime;
    private final QuestTransaction transaction;
    private final ParticipantMembershipResolver memberships;
    private final ScopedQuestAssignmentPolicy assignments = new ScopedQuestAssignmentPolicy();

    public JdbcQuestCompletionRewardPort(QuestDefinitionRepository definitions, QuestRuntimeRepository runtime,
        QuestTransaction transaction) {
        this(definitions,runtime,transaction,new com.jsirgalaxybase.quest.core.PlayerOnlyMembershipResolver());
    }

    public JdbcQuestCompletionRewardPort(QuestDefinitionRepository definitions,QuestRuntimeRepository runtime,
        QuestTransaction transaction, ParticipantMembershipResolver memberships) {
        if (definitions == null || runtime == null || transaction == null || memberships==null) throw new IllegalArgumentException("definitions, runtime, transaction and memberships are required");
        this.definitions = definitions;
        this.runtime = runtime;
        this.transaction = transaction;
        this.memberships=memberships;
    }

    @Override public Result complete(final String sourceEntitlementKey, final UUID recipientPlayerId,
        final UUID targetQuestId, final long completedAt) {
        if (sourceEntitlementKey == null || sourceEntitlementKey.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceEntitlementKey is required");
        }
        return transaction.inTransaction(() -> completeInTransaction(recipientPlayerId, targetQuestId, completedAt));
    }

    private Result completeInTransaction(UUID recipientPlayerId, UUID targetQuestId, long completedAt) {
        Optional<StoredQuestDefinition> stored = definitions.findPublished(targetQuestId);
        if (!stored.isPresent()) return Result.TARGET_NOT_FOUND;
        QuestDefinition definition = stored.get().getDefinition();
        java.util.List<ParticipantMembership> selected = assignments.select(definition,
            memberships.resolve(recipientPlayerId));
        if (selected.isEmpty()) return Result.TARGET_SCOPE_UNAVAILABLE;
        ParticipantMembership membership = selected.get(0);
        ParticipantId participantId = membership.getParticipantId();
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
        for(UUID recipient:membership.getPlayerIds())for (RewardDefinition reward : definition.getRewards())
            entitlements.add(new RewardEntitlement(participantId,recipient,definition,reward,cycle,
                definition.getBehavior().isAutoClaim()));
        runtime.insertEntitlementsIfAbsent(entitlements);
        return Result.COMPLETED;
    }
}
