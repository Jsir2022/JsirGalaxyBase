package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class QuestEvaluationPlanner implements QuestEvaluationRequestProvider {
    private final PublishedQuestCatalog definitions;
    private final QuestCompletionQuery completions;
    private final ParticipantMembershipResolver memberships;
    private final QuestAssignmentPolicy assignments;

    public QuestEvaluationPlanner(PublishedQuestCatalog definitions, QuestCompletionQuery completions,
        ParticipantMembershipResolver memberships, QuestAssignmentPolicy assignments) {
        this.definitions = require(definitions, "definitions");
        this.completions = require(completions, "completions");
        this.memberships = require(memberships, "memberships");
        this.assignments = require(assignments, "assignments");
    }

    @Override
    public List<QuestEvaluationRequest> requestsFor(GameplayFact fact) {
        List<ParticipantMembership> resolved = memberships.resolve(fact.getPlayerId());
        if (resolved == null) resolved = Collections.emptyList();
        validateMemberships(fact.getPlayerId(), resolved);
        List<QuestEvaluationRequest> result = new ArrayList<QuestEvaluationRequest>();
        for (StoredQuestDefinition stored : definitions.findAllPublished()) {
            QuestDefinition definition = stored.getDefinition();
            if (!accepts(definition, fact)) continue;
            for (ParticipantMembership membership : assignments.select(definition, resolved)) {
                result.add(new QuestEvaluationRequest(definition, membership,
                    completions.findCompletedQuestIds(membership.getParticipantId())));
            }
        }
        return result;
    }

    private boolean accepts(QuestDefinition definition, GameplayFact fact) {
        String factType = fact.getTypeId();
        if ("galaxy:consumption_applied".equals(factType)) {
            if (!definition.getId().toString().equals(fact.getAttributes().get("questId"))
                || !Integer.toString(definition.getVersion()).equals(fact.getAttributes().get("definitionVersion"))) {
                return false;
            }
            String taskKey = fact.getAttributes().get("taskKey");
            for (TaskDefinition task : definition.getTasks()) {
                if (task.getKey().equals(taskKey)
                    && Boolean.parseBoolean(task.getParameters().get("consume"))) return true;
            }
            return false;
        }
        for (TaskDefinition task : definition.getTasks()) {
            if (factType.equals(task.getParameters().get("factType"))) return true;
        }
        return false;
    }

    private void validateMemberships(UUID playerId, List<ParticipantMembership> values) {
        Set<ParticipantId> ids = new HashSet<ParticipantId>();
        for (ParticipantMembership membership : values) {
            if (membership == null || !membership.contains(playerId)) throw new IllegalArgumentException(
                "resolved membership must contain fact player");
            if (!ids.add(membership.getParticipantId())) throw new IllegalArgumentException(
                "duplicate participant membership: " + membership.getParticipantId());
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " must not be null");
        return value;
    }
}
