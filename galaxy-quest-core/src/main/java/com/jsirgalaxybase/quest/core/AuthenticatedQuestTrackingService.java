package com.jsirgalaxybase.quest.core;

import java.util.Optional;
import java.util.UUID;

/** Toggles tracking for an authenticated player after verifying the quest is currently published. */
public final class AuthenticatedQuestTrackingService {
    private final QuestDefinitionRepository definitions;
    private final QuestTrackingRepository tracking;
    private final QuestTransaction transaction;

    public AuthenticatedQuestTrackingService(QuestDefinitionRepository definitions,QuestTrackingRepository tracking,
        QuestTransaction transaction){if(definitions==null||tracking==null||transaction==null)throw new IllegalArgumentException("dependencies are required");this.definitions=definitions;this.tracking=tracking;this.transaction=transaction;}

    public Optional<Boolean> toggle(final UUID authenticatedPlayerId,final UUID questId,final long updatedAt){
        if(authenticatedPlayerId==null||questId==null||updatedAt<0L)throw new IllegalArgumentException("authenticated player, quest, and time are required");
        return transaction.inTransaction(() -> definitions.findPublished(questId).isPresent()
            ?Optional.of(Boolean.valueOf(tracking.toggle(authenticatedPlayerId,questId,updatedAt)))
            :Optional.<Boolean>empty());
    }
}
