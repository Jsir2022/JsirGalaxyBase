package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

public class ScopedQuestAssignmentPolicyTest {
    @Test
    public void selectsOnlyDefinitionScope() {
        UUID player = UUID.randomUUID();
        ParticipantMembership personal = ParticipantMembership.player(player);
        ParticipantMembership party = new ParticipantMembership(ParticipantId.party(UUID.randomUUID()),
            Collections.singleton(player));
        QuestDefinition definition = definition(QuestParticipantScope.PARTY);
        assertEquals(party, new ScopedQuestAssignmentPolicy().select(definition,
            Arrays.asList(personal, party)).get(0));
        assertTrue(new ScopedQuestAssignmentPolicy().select(definition,
            Collections.singletonList(personal)).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAmbiguousSameScopeMemberships() {
        UUID player = UUID.randomUUID();
        QuestDefinition definition = definition(QuestParticipantScope.TEAM);
        new ScopedQuestAssignmentPolicy().select(definition, Arrays.asList(
            new ParticipantMembership(ParticipantId.team(UUID.randomUUID()), Collections.singleton(player)),
            new ParticipantMembership(ParticipantId.team(UUID.randomUUID()), Collections.singleton(player))));
    }

    private static QuestDefinition definition(QuestParticipantScope scope) {
        return new QuestDefinition(UUID.randomUUID(), 1, "Scoped", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(scope).build());
    }
}
