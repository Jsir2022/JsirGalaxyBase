package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Mirrors BetterQuesting's per-player party fan-out for imported event-driven tasks and server-authored
 * aggregate non-consuming Retrieval snapshots. Party membership comes from the trusted server resolver.
 */
public final class BqHuntPartyAssignmentPolicy implements QuestAssignmentPolicy {
    private final ScopedQuestAssignmentPolicy scoped = new ScopedQuestAssignmentPolicy();

    @Override
    public List<ParticipantMembership> select(QuestDefinition definition,
        List<ParticipantMembership> memberships) {
        return scoped.select(definition, memberships);
    }

    @Override
    public List<ParticipantMembership> select(QuestDefinition definition,
        List<ParticipantMembership> memberships, GameplayFact fact) {
        List<ParticipantMembership> selected = scoped.select(definition, memberships);
        boolean taskEvent = BqPartyTaskProgressScope.supports(definition, fact);
        boolean retrieval = matchesBqPartyRetrievalSnapshot(definition, fact);
        if ((!taskEvent && !retrieval)
            || definition.getBehavior().getParticipantScope() != QuestParticipantScope.PLAYER
            || selected.isEmpty()) return selected;

        ParticipantMembership player = selected.get(0);
        if (player.getParticipantId().getType() != ParticipantType.PLAYER) return selected;
        ParticipantMembership party = null;
        for (ParticipantMembership membership : memberships) {
            if (membership.getParticipantId().getType() != ParticipantType.PARTY
            || !membership.contains(fact.getPlayerId())) continue;
            if (retrieval && !BqPartyInventorySnapshotScope.matches(fact, membership)) continue;
            if (party != null) throw new IllegalArgumentException(
                "multiple active PARTY memberships for BQ progress fact player");
            party = membership;
        }
        if (party == null) return selected;

        Set<UUID> members = new LinkedHashSet<UUID>(party.getPlayerIds());
        if (!members.contains(player.getParticipantId().getId())) throw new IllegalArgumentException(
            "selected player is outside BQ PARTY membership");
        List<ParticipantMembership> fanout = new ArrayList<ParticipantMembership>(members.size());
        for (UUID member : members) fanout.add(ParticipantMembership.player(member));
        return Collections.unmodifiableList(fanout);
    }

    private static boolean matchesBqPartyRetrievalSnapshot(QuestDefinition definition, GameplayFact fact) {
        if (!BqPartyInventorySnapshotScope.isPartySnapshot(fact)
            || !"galaxy:inventory_observed".equals(fact.getTypeId())) return false;
        for (TaskDefinition task : definition.getTasks()) {
            if (("bq_standard:retrieval".equals(task.getTypeId())
                || "bq_standard:optional_retrieval".equals(task.getTypeId()))
                && "galaxy:inventory_observed".equals(task.getParameters().get("factType"))
                && task.getParameters().containsKey("bq.raw")
                && !QuestBooleanParameter.parse(task.getParameters().get("consume"), false)) return true;
        }
        return false;
    }
}
