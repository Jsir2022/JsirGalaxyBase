package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class QuestEvaluationRequest {
    private final QuestDefinition definition;
    private final ParticipantMembership membership;
    private final Set<UUID> completedPrerequisites;

    public QuestEvaluationRequest(QuestDefinition definition, ParticipantMembership membership,
        Set<UUID> completedPrerequisites) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.membership = Objects.requireNonNull(membership, "membership");
        this.completedPrerequisites = Collections.unmodifiableSet(new LinkedHashSet<UUID>(completedPrerequisites == null
            ? Collections.<UUID>emptySet() : completedPrerequisites));
    }

    public QuestDefinition getDefinition() { return definition; }
    public ParticipantMembership getMembership() { return membership; }
    public Set<UUID> getCompletedPrerequisites() { return completedPrerequisites; }
}
