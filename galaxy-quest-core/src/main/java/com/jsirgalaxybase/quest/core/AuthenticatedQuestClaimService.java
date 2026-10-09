package com.jsirgalaxybase.quest.core;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;

/** Resolves version and cycle on the server before claiming rewards for the authenticated player. */
public final class AuthenticatedQuestClaimService {
    private final QuestDefinitionRepository definitions;
    private final QuestRuntimeRepository runtime;
    private final RewardClaimRepository claims;
    private final QuestTransaction transaction;
    private final RewardSuppressionPolicy suppressionPolicy;
    private final RewardChoiceSelectionRepository choiceSelections;
    private final Random choiceRandom;

    public AuthenticatedQuestClaimService(QuestDefinitionRepository definitions,QuestRuntimeRepository runtime,
        RewardClaimRepository claims,QuestTransaction transaction) {
        this(definitions, runtime, claims, transaction, new RewardSuppressionPolicy(false), null, new Random());
    }

    public AuthenticatedQuestClaimService(QuestDefinitionRepository definitions,QuestRuntimeRepository runtime,
        RewardClaimRepository claims,QuestTransaction transaction,RewardSuppressionPolicy suppressionPolicy) {
        this(definitions, runtime, claims, transaction, suppressionPolicy, null, new Random());
    }

    public AuthenticatedQuestClaimService(QuestDefinitionRepository definitions,QuestRuntimeRepository runtime,
        RewardClaimRepository claims,QuestTransaction transaction,RewardSuppressionPolicy suppressionPolicy,
        RewardChoiceSelectionRepository choiceSelections) {
        this(definitions, runtime, claims, transaction, suppressionPolicy, choiceSelections, new Random());
    }

    AuthenticatedQuestClaimService(QuestDefinitionRepository definitions,QuestRuntimeRepository runtime,
        RewardClaimRepository claims,QuestTransaction transaction,RewardSuppressionPolicy suppressionPolicy,
        RewardChoiceSelectionRepository choiceSelections,Random choiceRandom) {
        if(definitions==null||runtime==null||claims==null||transaction==null)throw new IllegalArgumentException("dependencies are required");
        if(suppressionPolicy==null)throw new IllegalArgumentException("suppressionPolicy is required");
        if(choiceRandom==null)throw new IllegalArgumentException("choiceRandom is required");
        this.definitions=definitions;this.runtime=runtime;this.claims=claims;this.transaction=transaction;
        this.suppressionPolicy=suppressionPolicy;
        this.choiceSelections=choiceSelections;this.choiceRandom=choiceRandom;
    }

    public RewardClaimStatus claim(final UUID authenticatedPlayerId,final UUID questId,final long claimedAt) {
        if(authenticatedPlayerId==null||questId==null||claimedAt<0L)throw new IllegalArgumentException("authenticated player, quest, and time are required");
        return transaction.inTransaction(() -> {
            Optional<StoredQuestDefinition> published=definitions.findPublished(questId);
            if(!published.isPresent())return RewardClaimStatus.NOT_FOUND;
            int version=published.get().getDefinition().getVersion();
            Optional<RewardClaimTarget> target=claims.findNextRecipientTarget(questId,version,authenticatedPlayerId);
            if(target.isPresent()) return claims.claim(target.get(),questId,version,authenticatedPlayerId,claimedAt,
                suppressionPolicy.isNoRewardsEnabled());
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
            return claims.claim(questId,version,cycle,authenticatedPlayerId,claimedAt,
                suppressionPolicy.isNoRewardsEnabled());
        });
    }

    /**
     * BetterQuesting's explicit claim-all intent chooses one random option for each still-unselected choice reward.
     * Call only with a player identity resolved by the authenticated server connection. This is separate from normal
     * single-quest claiming; the definition, recipient cycle, and claimable reward rows remain server-resolved.
     */
    public RewardClaimStatus forceClaimWithBqRandomChoices(final UUID authenticatedPlayerId,final UUID questId,
        final long claimedAt) {
        if(authenticatedPlayerId==null||questId==null||claimedAt<0L)throw new IllegalArgumentException(
            "authenticated player, quest, and time are required");
        if(choiceSelections==null)return RewardClaimStatus.INVALID_STATE;
        try {
            return transaction.inTransaction(() -> {
                Optional<StoredQuestDefinition> published=definitions.findPublished(questId);
                if(!published.isPresent())return RewardClaimStatus.NOT_FOUND;
                QuestDefinition quest=published.get().getDefinition();
                Optional<RewardClaimTarget> next=claims.findNextRecipientTarget(questId,quest.getVersion(),
                    authenticatedPlayerId);
                RewardClaimTarget target;
                if(next.isPresent())target=next.get();
                else {
                    OptionalInt recipientCycle=claims.findLatestRecipientCycle(questId,quest.getVersion(),
                        authenticatedPlayerId);
                    if(recipientCycle.isPresent())target=new RewardClaimTarget(ParticipantId.player(
                        authenticatedPlayerId),recipientCycle.getAsInt());
                    else {
                        Optional<QuestProgressSnapshot> progress=runtime.findProgress(
                            ParticipantId.player(authenticatedPlayerId),questId,quest.getVersion());
                        if(!progress.isPresent())return RewardClaimStatus.NOT_FOUND;
                        target=new RewardClaimTarget(ParticipantId.player(authenticatedPlayerId),
                            progress.get().getCycle());
                    }
                }

                RewardClaimStatus preflight=claims.claim(target,questId,quest.getVersion(),authenticatedPlayerId,
                    claimedAt,suppressionPolicy.isNoRewardsEnabled());
                if(preflight!=RewardClaimStatus.NEEDS_CHOICE)return preflight;

                boolean hasChoice=false;
                for(RewardDefinition reward:quest.getRewards()) {
                    if(!"bq_standard:choice".equals(reward.getTypeId()))continue;
                    hasChoice=true;
                    int index=randomChoiceIndex(reward);
                    String key=new RewardEntitlement(target.getParticipantId(),authenticatedPlayerId,quest,reward,
                        target.getCycle(),true).getEntitlementKey();
                    RewardChoiceSelectionStatus selected=choiceSelections.selectForForcedClaim(key,
                        authenticatedPlayerId,index,claimedAt);
                    if(selected!=RewardChoiceSelectionStatus.SELECTED
                        &&selected!=RewardChoiceSelectionStatus.ALREADY_SELECTED
                        &&selected!=RewardChoiceSelectionStatus.CONFLICT) {
                        throw new ForceClaimAbort(RewardClaimStatus.INVALID_STATE);
                    }
                }
                if(!hasChoice)throw new ForceClaimAbort(RewardClaimStatus.INVALID_STATE);

                RewardClaimStatus claimed=claims.claim(target,questId,quest.getVersion(),authenticatedPlayerId,
                    claimedAt,suppressionPolicy.isNoRewardsEnabled());
                if(claimed!=RewardClaimStatus.CLAIMED&&claimed!=RewardClaimStatus.SUPPRESSED) {
                    throw new ForceClaimAbort(claimed);
                }
                return claimed;
            });
        } catch(ForceClaimAbort abort) {
            return abort.status;
        }
    }

    private int randomChoiceIndex(RewardDefinition reward) {
        int count;
        try { count=Integer.parseInt(reward.getParameters().get("item.count")); }
        catch(RuntimeException invalid) { throw new ForceClaimAbort(RewardClaimStatus.INVALID_STATE); }
        if(count<1)throw new ForceClaimAbort(RewardClaimStatus.INVALID_STATE);
        int lootbagCount=0;
        for(int index=0;index<count;index++)if(isLootbag(reward,index))lootbagCount++;
        if(lootbagCount==0)return choiceRandom.nextInt(count);
        int selected=choiceRandom.nextInt(lootbagCount);
        for(int index=0;index<count;index++) {
            if(!isLootbag(reward,index))continue;
            if(selected--==0)return index;
        }
        throw new ForceClaimAbort(RewardClaimStatus.INVALID_STATE);
    }

    private static boolean isLootbag(RewardDefinition reward,int index) {
        String registry=reward.getParameters().get("item."+index+".registryName");
        if(registry==null)return false;
        int separator=registry.indexOf(':');
        return separator>0&&"enhancedlootbags".equals(registry.substring(0,separator));
    }

    private static final class ForceClaimAbort extends RuntimeException {
        private static final long serialVersionUID=1L;
        private final RewardClaimStatus status;
        private ForceClaimAbort(RewardClaimStatus status){this.status=status;}
    }
}
