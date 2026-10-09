package com.jsirgalaxybase.quest.core;

/** Resolves whether a BQ-style active-quest union contains an online member of the source Party. */
public interface QuestPartyActivityQuery {
    QuestPartyActivityQuery NONE = new QuestPartyActivityQuery() {
        @Override public boolean hasActiveMember(QuestDefinition definition, GameplayFact fact,
            ParticipantMembership party) { return false; }
    };

    boolean hasActiveMember(QuestDefinition definition, GameplayFact fact, ParticipantMembership party);
}
