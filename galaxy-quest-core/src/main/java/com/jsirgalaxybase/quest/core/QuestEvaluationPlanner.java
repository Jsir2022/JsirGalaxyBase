package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.UUID;
import java.util.Optional;

public final class QuestEvaluationPlanner implements QuestEvaluationRequestProvider {
    private final PublishedQuestCatalog definitions;
    private final QuestCompletionQuery completions;
    private final ParticipantMembershipResolver memberships;
    private final QuestAssignmentPolicy assignments;
    private final QuestPartyActivityQuery partyActivity;
    private volatile CatalogPlan cachedPlan;

    public QuestEvaluationPlanner(PublishedQuestCatalog definitions, QuestCompletionQuery completions,
        ParticipantMembershipResolver memberships, QuestAssignmentPolicy assignments) {
        this(definitions, completions, memberships, assignments, QuestPartyActivityQuery.NONE);
    }

    public QuestEvaluationPlanner(PublishedQuestCatalog definitions, QuestCompletionQuery completions,
        ParticipantMembershipResolver memberships, QuestAssignmentPolicy assignments,
        QuestPartyActivityQuery partyActivity) {
        this.definitions = require(definitions, "definitions");
        this.completions = require(completions, "completions");
        this.memberships = require(memberships, "memberships");
        this.assignments = require(assignments, "assignments");
        this.partyActivity = require(partyActivity, "partyActivity");
    }

    @Override
    public List<QuestEvaluationRequest> requestsFor(GameplayFact fact) {
        List<ParticipantMembership> resolved = forcedMemberships(fact);
        if (resolved == null) resolved = Collections.emptyList();
        validateMemberships(fact.getPlayerId(), resolved, isForcedConsumption(fact));
        List<QuestEvaluationRequest> result = new ArrayList<QuestEvaluationRequest>();
        Map<ParticipantId, Set<UUID>> completedByParticipant = new HashMap<ParticipantId, Set<UUID>>();
        Set<UUID> originCompleted = null;
        for (StoredQuestDefinition stored : definitionsForFact(fact)) {
            QuestDefinition definition = stored.getDefinition();
            if (!accepts(definition, fact, resolved)) continue;
            if (BqPartyInventorySnapshotScope.isPartySnapshot(fact)) {
                if (originCompleted == null) {
                    originCompleted = completedFor(completedByParticipant,
                        ParticipantId.player(fact.getPlayerId()));
                }
                if (!originCanDetect(definition, originCompleted)) continue;
            }
            ParticipantMembership party = bqPartyActivityMembership(definition, fact, resolved);
            if (party != null && !partyActivity.hasActiveMember(definition, fact, party)) continue;
            for (ParticipantMembership membership : assignments.select(definition, resolved, fact)) {
                ParticipantMembership factMembership = factMembershipFor(definition, fact, membership, resolved,
                    party);
                result.add(new QuestEvaluationRequest(definition, membership, factMembership,
                    completedFor(completedByParticipant, membership.getParticipantId())));
            }
        }
        return result;
    }

    private List<StoredQuestDefinition> definitionsForFact(GameplayFact fact) {
        if (!"galaxy:consumption_applied".equals(fact.getTypeId())) return plan().index.forFact(fact);
        String quest = fact.getAttributes().get("questId");
        String version = fact.getAttributes().get("definitionVersion");
        if (quest == null || version == null) throw new IllegalArgumentException(
            "confirmed consumption fact is missing its pinned definition identity");
        try {
            UUID questId = UUID.fromString(quest);
            int definitionVersion = Integer.parseInt(version);
            Optional<StoredQuestDefinition> exact = definitions.findPublished(questId, definitionVersion);
            // A durable APPLIED consumption may outlive publication of its pinned definition. In that
            // case do not evaluate against a newer version; returning no requests keeps the saga APPLIED
            // for explicit recovery/compensation instead of confirming the wrong task.
            if (!exact.isPresent()) return Collections.emptyList();
            return Collections.singletonList(exact.get());
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("confirmed consumption fact has an invalid pinned definition", invalid);
        }
    }

    @Override
    public List<QuestEvaluationRequest> requestsForNewlyUnlockedLockedProgress(ParticipantId player,
        UUID completedQuestId) {
        if (player == null || player.getType() != ParticipantType.PLAYER || completedQuestId == null) {
            return Collections.emptyList();
        }
        Set<UUID> completed = completions.findCompletedQuestIds(player);
        if (completed == null || !completed.contains(completedQuestId)) return Collections.emptyList();
        List<QuestEvaluationRequest> result = new ArrayList<QuestEvaluationRequest>();
        ParticipantMembership membership = ParticipantMembership.player(player.getId());
        for (StoredQuestDefinition stored : plan().index.newlyUnlockedBy(completedQuestId)) {
            QuestDefinition definition = stored.getDefinition();
            if (!definition.getBehavior().isProgressWhileLocked()
                || definition.getBehavior().getParticipantScope() != QuestParticipantScope.PLAYER
                || !definition.getPrerequisites().contains(completedQuestId)) continue;
            int completedCount = 0;
            for (UUID prerequisite : definition.getPrerequisites()) {
                if (completed.contains(prerequisite)) completedCount++;
            }
            if (!definition.getPrerequisiteLogic().satisfied(completedCount, definition.getPrerequisites().size())) {
                continue;
            }
            result.add(new QuestEvaluationRequest(definition, membership, completed));
        }
        return result;
    }

    @Override
    public List<QuestEvaluationRequest> requestsForLockedProgressReconciliation(ParticipantId player,
        Set<UUID> lockedQuestIds) {
        if (player == null || player.getType() != ParticipantType.PLAYER || lockedQuestIds == null
            || lockedQuestIds.isEmpty()) return Collections.emptyList();
        Set<UUID> completed = completions.findCompletedQuestIds(player);
        if (completed == null) return Collections.emptyList();
        List<QuestEvaluationRequest> result = new ArrayList<QuestEvaluationRequest>();
        ParticipantMembership membership = ParticipantMembership.player(player.getId());
        for (StoredQuestDefinition stored : plan().index.forIds(lockedQuestIds)) {
            QuestDefinition definition = stored.getDefinition();
            if (!lockedQuestIds.contains(definition.getId()) || !definition.getBehavior().isProgressWhileLocked()
                || definition.getBehavior().getParticipantScope() != QuestParticipantScope.PLAYER) continue;
            int completedCount = 0;
            for (UUID prerequisite : definition.getPrerequisites()) {
                if (completed.contains(prerequisite)) completedCount++;
            }
            if (definition.getPrerequisites().isEmpty()
                || definition.getPrerequisiteLogic().satisfied(completedCount, definition.getPrerequisites().size())) {
                result.add(new QuestEvaluationRequest(definition, membership, completed));
            }
        }
        return result;
    }

    private ParticipantMembership factMembershipFor(QuestDefinition definition, GameplayFact fact,
        ParticipantMembership progressMembership, List<ParticipantMembership> resolved,
        ParticipantMembership partyActivityMembership) {
        if (progressMembership.getParticipantId().getType() == ParticipantType.PLAYER
            && BqPartyCraftingStatisticsScope.isAutomaticTaskSnapshot(fact)
            && partyActivityMembership != null
            && partyActivityMembership.contains(fact.getPlayerId())
            && partyActivityMembership.contains(progressMembership.getParticipantId().getId())) {
            return partyActivityMembership;
        }
        if (progressMembership.getParticipantId().getType() != ParticipantType.PLAYER
            || !hasMatchingPartyFanoutFact(definition, fact)) return progressMembership;
        ParticipantMembership source = null;
        UUID progressOwner = progressMembership.getParticipantId().getId();
        for (ParticipantMembership membership : resolved) {
            if (membership.getParticipantId().getType() != ParticipantType.PARTY
                || !membership.contains(fact.getPlayerId()) || !membership.contains(progressOwner)) continue;
            if (BqPartyInventorySnapshotScope.isPartySnapshot(fact)
                && !BqPartyInventorySnapshotScope.matches(fact, membership)) continue;
            if (source != null) throw new IllegalArgumentException(
                "multiple PARTY snapshots authorize BQ progress owner");
            source = membership;
        }
        return source == null ? progressMembership : source;
    }

    private boolean hasMatchingPartyFanoutFact(QuestDefinition definition, GameplayFact fact) {
        return BqPartyTaskProgressScope.supports(definition, fact)
            || BqPartyInventorySnapshotScope.matchesBqPartySnapshot(definition, fact);
    }

    private ParticipantMembership bqPartyActivityMembership(QuestDefinition definition, GameplayFact fact,
        List<ParticipantMembership> resolved) {
        if (definition.getBehavior().getParticipantScope() != QuestParticipantScope.PLAYER
            || (!hasMatchingPartyFanoutFact(definition, fact)
                && !BqPartyCraftingStatisticsScope.matchesAutomaticPartyActivity(definition, fact))) return null;
        ParticipantMembership selected = null;
        for (ParticipantMembership membership : resolved) {
            if (membership.getParticipantId().getType() != ParticipantType.PARTY
                || !membership.contains(fact.getPlayerId())) continue;
            if (selected != null) throw new IllegalArgumentException(
                "multiple PARTY memberships for BQ active-quest source");
            selected = membership;
        }
        return selected;
    }

    private List<ParticipantMembership> forcedMemberships(GameplayFact fact) {
        if (BqPartyInventorySnapshotScope.hasMetadata(fact)) {
            if (!BqPartyInventorySnapshotScope.isSupportedObservationType(fact.getTypeId())) {
                throw new IllegalArgumentException("BQ party scope metadata is only valid on inventory observations");
            }
            ParticipantId party = BqPartyInventorySnapshotScope.party(fact);
            List<ParticipantMembership> resolved = memberships.resolve(fact.getPlayerId());
            if (resolved == null) throw new IllegalArgumentException("BQ party membership is unavailable");
            boolean found = false;
            for (ParticipantMembership membership : resolved) {
                if (party.equals(membership.getParticipantId())
                    && membership.contains(fact.getPlayerId())) found = true;
            }
            if (!found) throw new IllegalArgumentException(
                "BQ party inventory source is not a member of the resolved party");
            return resolved;
        }
        if (InventorySnapshotScope.hasScopeMetadata(fact)) {
            if (!"galaxy:inventory_observed".equals(fact.getTypeId())) throw new IllegalArgumentException(
                "group inventory scope metadata is only valid on inventory observations");
            ParticipantId participant = InventorySnapshotScope.participant(fact);
            ParticipantMembership membership = memberships.resolveParticipant(participant);
            if (membership == null) throw new IllegalArgumentException(
                "group inventory snapshot participant is unavailable");
            return Collections.singletonList(membership);
        }
        if (!"galaxy:consumption_applied".equals(fact.getTypeId())) return memberships.resolve(fact.getPlayerId());
        String type = fact.getAttributes().get("progressParticipantType");
        String id = fact.getAttributes().get("progressParticipantId");
        if (type == null || id == null) return memberships.resolve(fact.getPlayerId());
        ParticipantId participant;
        try { participant = new ParticipantId(ParticipantType.valueOf(type), UUID.fromString(id)); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("invalid authorized consumption participant", invalid); }
        ParticipantMembership membership = memberships.resolveParticipant(participant);
        if (membership == null) throw new IllegalArgumentException("authorized consumption participant is unavailable");
        return Collections.singletonList(membership);
    }

    private boolean isForcedConsumption(GameplayFact fact) {
        return "galaxy:consumption_applied".equals(fact.getTypeId())
            && fact.getAttributes().containsKey("progressParticipantType")
            && fact.getAttributes().containsKey("progressParticipantId");
    }

    private boolean accepts(QuestDefinition definition, GameplayFact fact,
        List<ParticipantMembership> resolvedMemberships) {
        String factType = fact.getTypeId();
        String targetQuestId = fact.getAttributes().get("targetQuestId");
        if (targetQuestId != null) {
            if (!"galaxy:item_produced".equals(factType)
                || !"craft-statistics".equals(fact.getAttributes().get("channel"))
                || !definition.getId().toString().equals(targetQuestId)
                || !Integer.toString(definition.getVersion()).equals(fact.getAttributes().get("targetQuestVersion"))) {
                return false;
            }
            String targetTaskKey = fact.getAttributes().get("targetTaskKey");
            for (TaskDefinition task : definition.getTasks()) {
                if (task.getKey().equals(targetTaskKey) && "bq_standard:crafting".equals(task.getTypeId())
                    && QuestBooleanParameter.parse(task.getParameters().get("allowCraftedFromStatistics"), false)) {
                    return !BqPartyCraftingStatisticsScope.isAutomaticTaskSnapshot(fact)
                        || !BqImportedTaskScope.sharesParty(task)
                        || BqPartyCraftingStatisticsScope.matchesAutomaticPartyActivity(definition, fact);
                }
            }
            return false;
        }
        if (BqPartyInventorySnapshotScope.isPartySnapshot(fact)) {
            return BqPartyInventorySnapshotScope.isSupportedObservationType(factType)
                && definition.getBehavior().getParticipantScope() == QuestParticipantScope.PLAYER
                && BqPartyInventorySnapshotScope.matchesBqPartySnapshot(definition, fact);
        }
        if (InventorySnapshotScope.isGroupSnapshot(fact)) {
            ParticipantId participant = InventorySnapshotScope.participant(fact);
            if (!"galaxy:inventory_observed".equals(factType)
                || !definition.getBehavior().getParticipantScope().name().equals(participant.getType().name())) {
                return false;
            }
            for (TaskDefinition task : definition.getTasks()) {
                if (factType.equals(task.getParameters().get("factType"))
                    && ("bq_standard:retrieval".equals(task.getTypeId())
                        || "bq_standard:optional_retrieval".equals(task.getTypeId()))
                    && !task.getParameters().containsKey("bq.raw")
                    && !QuestBooleanParameter.parse(task.getParameters().get("consume"), false)) return true;
            }
            return false;
        }
        if ("galaxy:task_confirmed".equals(factType)) {
            if (!definition.getId().toString().equals(fact.getAttributes().get("questId"))
                || !Integer.toString(definition.getVersion()).equals(fact.getAttributes().get("definitionVersion"))) {
                return false;
            }
            String taskKey = fact.getAttributes().get("taskKey");
            for (TaskDefinition task : definition.getTasks()) {
                if (task.getKey().equals(taskKey) && "bq_standard:checkbox".equals(task.getTypeId())
                    && factType.equals(task.getParameters().get("factType"))) return true;
            }
            return false;
        }
        if ("galaxy:consumption_applied".equals(factType)) {
            if (!definition.getId().toString().equals(fact.getAttributes().get("questId"))
                || !Integer.toString(definition.getVersion()).equals(fact.getAttributes().get("definitionVersion"))) {
                return false;
            }
            String taskKey = fact.getAttributes().get("taskKey");
            for (TaskDefinition task : definition.getTasks()) {
                if (task.getKey().equals(taskKey)
                    && QuestBooleanParameter.parse(task.getParameters().get("consume"), false)) return true;
            }
            return false;
        }
        for (TaskDefinition task : definition.getTasks()) {
            if (!factType.equals(task.getParameters().get("factType"))) continue;
            boolean importedPartyInventoryRetrieval = "galaxy:inventory_observed".equals(factType)
                && BqImportedTaskScope.isImported(task) && BqImportedTaskScope.sharesParty(task)
                && ("bq_standard:retrieval".equals(task.getTypeId())
                    || "bq_standard:optional_retrieval".equals(task.getTypeId()))
                && !QuestBooleanParameter.parse(task.getParameters().get("consume"), false);
            if (importedPartyInventoryRetrieval) {
                // BQ's ParticipantInfo uses the player's own UUID and inventory when no party exists.
                // With a party, however, a plain player snapshot would omit the other active members.
                // Only an unscoped, server-authored personal observation is valid for the solo case.
                if (BqPartyInventorySnapshotScope.isPartySnapshot(fact)) return true;
                if (InventorySnapshotScope.hasScopeMetadata(fact) || hasPartyMembership(resolvedMemberships)) continue;
            }
            return true;
        }
        return false;
    }

    private boolean hasPartyMembership(List<ParticipantMembership> memberships) {
        if (memberships == null) return false;
        for (ParticipantMembership membership : memberships) {
            if (membership != null && membership.getParticipantId().getType() == ParticipantType.PARTY) return true;
        }
        return false;
    }

    private boolean originCanDetect(QuestDefinition definition, Set<UUID> completed) {
        if (definition.getPrerequisites().isEmpty()) return true;
        int count = 0;
        for (UUID prerequisite : definition.getPrerequisites()) if (completed.contains(prerequisite)) count++;
        return definition.getPrerequisiteLogic().satisfied(count, definition.getPrerequisites().size());
    }

    private Set<UUID> completedFor(Map<ParticipantId, Set<UUID>> completedByParticipant,
        ParticipantId participant) {
        if (!completedByParticipant.containsKey(participant)) {
            completedByParticipant.put(participant, completions.findCompletedQuestIds(participant));
        }
        return completedByParticipant.get(participant);
    }

    private CatalogPlan plan() {
        List<StoredQuestDefinition> published = definitions.findAllPublished();
        CatalogPlan current = cachedPlan;
        if (current != null && current.catalog == published) return current;
        synchronized (this) {
            current = cachedPlan;
            if (current != null && current.catalog == published) return current;
            CatalogPlan refreshed = new CatalogPlan(published, new QuestEvaluationCandidateIndex(published));
            cachedPlan = refreshed;
            return refreshed;
        }
    }

    private void validateMemberships(UUID playerId, List<ParticipantMembership> values, boolean authorizedConsumption) {
        Set<ParticipantId> ids = new HashSet<ParticipantId>();
        for (ParticipantMembership membership : values) {
            if (membership == null || !membership.contains(playerId) && !authorizedConsumption)
                throw new IllegalArgumentException("resolved membership must contain fact player");
            if (membership.getParticipantId().getType() == ParticipantType.PLAYER && !membership.contains(playerId))
                throw new IllegalArgumentException("resolved player membership must contain fact player");
            if (!ids.add(membership.getParticipantId())) throw new IllegalArgumentException(
                "duplicate participant membership: " + membership.getParticipantId());
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " must not be null");
        return value;
    }

    private static final class CatalogPlan {
        private final List<StoredQuestDefinition> catalog;
        private final QuestEvaluationCandidateIndex index;

        private CatalogPlan(List<StoredQuestDefinition> catalog, QuestEvaluationCandidateIndex index) {
            this.catalog = catalog;
            this.index = index;
        }
    }
}
