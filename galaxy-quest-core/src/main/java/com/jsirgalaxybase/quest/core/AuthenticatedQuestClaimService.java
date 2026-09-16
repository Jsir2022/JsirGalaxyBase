package com.jsirgalaxybase.quest.core;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/** Resolves version and cycle on the server before claiming rewards for the authenticated player. */
public final class AuthenticatedQuestClaimService {
    private final QuestDefinitionRepository definitions;
    private final QuestRuntimeRepository runtime;
    private final RewardClaimRepository claims;
    private final QuestTransaction transaction;

    public AuthenticatedQuestClaimService(QuestDefinitionRepository definitions,QuestRuntimeRepository runtime,
        RewardClaimRepository claims,QuestTransaction transaction) {
        if(definitions==null||runtime==null||claims==null||transaction==null)throw new IllegalArgumentException("dependencies are required");
        this.definitions=definitions;this.runtime=runtime;this.claims=claims;this.transaction=transaction;
    }

    public RewardClaimStatus claim(final UUID authenticatedPlayerId,final UUID questId,final long claimedAt) {
        if(authenticatedPlayerId==null||questId==null||claimedAt<0L)throw new IllegalArgumentException("authenticated player, quest, and time are required");
        return transaction.inTransaction(() -> {
            Optional<StoredQuestDefinition> published=definitions.findPublished(questId);
            if(!published.isPresent())return RewardClaimStatus.NOT_FOUND;
            int version=published.get().getDefinition().getVersion();
            Optional<RewardClaimTarget> target=claims.findNextRecipientTarget(questId,version,authenticatedPlayerId);
            if(target.isPresent()) return claims.claim(target.get(),questId,version,authenticatedPlayerId,claimedAt);
            OptionalInt recipientCycle=claims.findLatestRecipientCycle(questId,version,authenticatedPlayerId);
            int cycle;
            if(recipientCycle.isPresent()) cycle=recipientCycle.getAsInt();
            else {
                // Compatibility for entitlement rows created before recipient_player_id existed.
                Optional<QuestProgressSnapshot> progress=runtime.findProgress(
                    ParticipantId.player(authenticatedPlayerId),questId,version);
                if(!progress.isPresent())return RewardClaimStatus.NOT_FOUND;
                cycle=progress.get().getCycle();
            }
            return claims.claim(questId,version,cycle,authenticatedPlayerId,claimedAt);
        });
    }
}
