package com.jsirgalaxybase.quest.core;

import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestFactProjector {
    private final TaskEvaluatorRegistry registry;

    public QuestFactProjector(TaskEvaluatorRegistry registry) {
        this.registry = registry;
    }

    public Map<String, TaskProgress> project(QuestDefinition definition, QuestProgressSnapshot current,
        GameplayFact fact) {
        return project(definition, current, ParticipantMembership.player(current.getPlayerId()), fact);
    }

    public Map<String, TaskProgress> project(QuestDefinition definition, QuestProgressSnapshot current,
        ParticipantMembership membership, GameplayFact fact) {
        return project(definition, current, membership, membership, fact);
    }

    public Map<String, TaskProgress> project(QuestDefinition definition, QuestProgressSnapshot current,
        ParticipantMembership membership, ParticipantMembership factMembership, GameplayFact fact) {
        if (!membership.getParticipantId().equals(current.getParticipantId())) {
            throw new IllegalArgumentException("membership participant does not match progress owner");
        }
        if (!factMembership.contains(fact.getPlayerId())) {
            throw new IllegalArgumentException("fact player does not match authorized fact membership");
        }
        if (!sameMembership(membership, factMembership)
            && (factMembership.getParticipantId().getType() != ParticipantType.PARTY
                || membership.getParticipantId().getType() != ParticipantType.PLAYER
                || !factMembership.contains(membership.getParticipantId().getId()))) {
            throw new IllegalArgumentException("fact membership cannot authorize this progress owner");
        }
        Map<String, TaskProgress> result = new LinkedHashMap<String, TaskProgress>();
        for (TaskDefinition task : definition.getTasks()) {
            TaskProgress progress = current.getTasks().get(task.getKey());
            if (progress == null) progress = TaskProgress.empty(task);
            if (isSharedRetrievalSnapshot(task, fact, membership)) {
                result.put(task.getKey(), progress);
                continue;
            }
            result.put(task.getKey(), registry.require(task.getTypeId()).evaluate(task, progress, fact));
        }
        return result;
    }

    private static boolean sameMembership(ParticipantMembership left, ParticipantMembership right) {
        return left.getParticipantId().equals(right.getParticipantId())
            && left.getPlayerIds().equals(right.getPlayerIds());
    }

    private boolean isSharedRetrievalSnapshot(TaskDefinition task, GameplayFact fact,
        ParticipantMembership membership) {
        String type = task.getTypeId();
        if (!"galaxy:inventory_observed".equals(fact.getTypeId())
            || !("bq_standard:retrieval".equals(type) || "bq_standard:optional_retrieval".equals(type))
            || QuestBooleanParameter.parse(task.getParameters().get("consume"), false)) return false;

        ParticipantId owner = membership.getParticipantId();
        if (BqPartyInventorySnapshotScope.isPartySnapshot(fact)) {
            // Every imported non-consuming BQ Retrieval scans active party inventories. groupDetect
            // changes component reset semantics, not whether this aggregate fact is authoritative.
            return owner.getType() != ParticipantType.PLAYER || !BqImportedTaskScope.isImported(task)
                || !BqImportedTaskScope.sharesParty(task);
        }
        if (BqImportedTaskScope.isImported(task) && BqImportedTaskScope.sharesParty(task)
            && !QuestBooleanParameter.parse(task.getParameters().get("consume"), false)) return true;
        if (InventorySnapshotScope.isGroupSnapshot(fact)) {
            // Aggregate facts are scoped to exactly one authenticated group and never update
            // personal progress or another team's/party's task.
            return !InventorySnapshotScope.matches(fact, owner);
        }
        if (InventorySnapshotScope.hasScopeMetadata(fact)) {
            throw new IllegalArgumentException("inventory snapshot has incomplete group scope metadata");
        }
        // A single player's inventory is not a complete observation of shared active members.
        return owner.getType() != ParticipantType.PLAYER;
    }

}
