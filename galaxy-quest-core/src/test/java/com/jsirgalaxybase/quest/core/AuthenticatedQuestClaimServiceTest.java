package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.UUID;

import org.junit.Test;

public class AuthenticatedQuestClaimServiceTest {
    @Test public void derivesPublishedVersionAndCycleInsteadOfAcceptingThemFromClient(){
        UUID player=UUID.randomUUID(),quest=UUID.randomUUID();QuestDefinition definition=definition(quest,7);
        RecordingClaims claims=new RecordingClaims();AuthenticatedQuestClaimService service=new AuthenticatedQuestClaimService(
            new Definitions(definition),new Runtime(new QuestProgressSnapshot(ParticipantId.player(player),quest,7,
                QuestStatus.COMPLETED,Collections.emptyMap(),10L,4)),claims,new DirectTransaction());
        assertEquals(RewardClaimStatus.CLAIMED,service.claim(player,quest,123L));
        assertEquals(quest,claims.quest);assertEquals(7,claims.version);assertEquals(4,claims.cycle);
        assertEquals(player,claims.player);assertEquals(123L,claims.time);
    }
    @Test public void missingPublishedDefinitionDoesNotCallClaimRepository(){
        RecordingClaims claims=new RecordingClaims();AuthenticatedQuestClaimService service=new AuthenticatedQuestClaimService(
            new Definitions(null),new Runtime(null),claims,new DirectTransaction());
        assertEquals(RewardClaimStatus.NOT_FOUND,service.claim(UUID.randomUUID(),UUID.randomUUID(),0L));
        assertEquals(null,claims.quest);
    }
    @Test public void frozenGroupRecipientCycleWinsOverLegacyPersonalProgress(){
        UUID player=UUID.randomUUID(),quest=UUID.randomUUID();QuestDefinition definition=definition(quest,7);
        RecordingClaims claims=new RecordingClaims();claims.recipientCycle=OptionalInt.of(9);
        AuthenticatedQuestClaimService service=new AuthenticatedQuestClaimService(new Definitions(definition),
            new Runtime(new QuestProgressSnapshot(ParticipantId.player(player),quest,7,QuestStatus.COMPLETED,
                Collections.emptyMap(),10L,4)),claims,new DirectTransaction());
        assertEquals(RewardClaimStatus.CLAIMED,service.claim(player,quest,123L));
        assertEquals(9,claims.cycle);
    }
    @Test public void exactFrozenGroupTargetIsUsedInsteadOfAmbiguousCycleClaim(){
        UUID player=UUID.randomUUID(),quest=UUID.randomUUID();QuestDefinition definition=definition(quest,7);
        RecordingClaims claims=new RecordingClaims();claims.target=Optional.of(new RewardClaimTarget(
            new ParticipantId(ParticipantType.TEAM,UUID.randomUUID()),2));
        AuthenticatedQuestClaimService service=new AuthenticatedQuestClaimService(new Definitions(definition),
            new Runtime(null),claims,new DirectTransaction());
        assertEquals(RewardClaimStatus.CLAIMED,service.claim(player,quest,123L));
        assertEquals(claims.target.get().getParticipantId(),claims.claimedTarget.getParticipantId());
        assertEquals(2,claims.claimedTarget.getCycle());
    }
    @Test public void normalClaimStillRequiresAnExplicitChoice(){
        UUID player=UUID.randomUUID(),quest=UUID.randomUUID();QuestDefinition definition=definition(quest,3,
            Collections.singletonList(choice("choice", "minecraft:diamond", "minecraft:emerald")));
        RecordingClaims claims=new RecordingClaims();claims.results.add(RewardClaimStatus.NEEDS_CHOICE);
        RecordingChoices choices=new RecordingChoices();
        AuthenticatedQuestClaimService service=service(player,definition,claims,choices,randomIndex(0));
        assertEquals(RewardClaimStatus.NEEDS_CHOICE,service.claim(player,quest,100L));
        assertTrue(choices.indexes.isEmpty());
    }
    @Test public void forceClaimPrefersLootbagOptionsAndClaimsTheSameRecipientTarget(){
        UUID player=UUID.randomUUID(),questId=UUID.randomUUID();
        RewardDefinition reward=choice("choice", "minecraft:diamond", "enhancedlootbags:common", "minecraft:emerald",
            "enhancedlootbags:rare");
        QuestDefinition quest=definition(questId,4,Collections.singletonList(reward));
        RewardClaimTarget target=new RewardClaimTarget(new ParticipantId(ParticipantType.PARTY,UUID.randomUUID()),2);
        RecordingClaims claims=new RecordingClaims();claims.target=Optional.of(target);
        claims.results.add(RewardClaimStatus.NEEDS_CHOICE);claims.results.add(RewardClaimStatus.CLAIMED);
        RecordingChoices choices=new RecordingChoices();
        AuthenticatedQuestClaimService service=service(player,quest,claims,choices,randomIndex(1));

        assertEquals(RewardClaimStatus.CLAIMED,service.forceClaimWithBqRandomChoices(player,questId,123L));
        assertEquals(Collections.singletonList(Integer.valueOf(3)),choices.indexes);
        assertEquals(Collections.singletonList(new RewardEntitlement(target.getParticipantId(),player,quest,reward,
            target.getCycle(),true).getEntitlementKey()),choices.keys);
        assertEquals(target,claims.claimedTarget);
        assertEquals(2,claims.calls);
    }
    @Test public void forceClaimFallsBackToAnyOptionWhenThereAreNoLootbags(){
        UUID player=UUID.randomUUID(),questId=UUID.randomUUID();
        RewardDefinition reward=choice("choice", "minecraft:diamond", "gregtech:ingotCopper", "minecraft:emerald");
        QuestDefinition quest=definition(questId,1,Collections.singletonList(reward));
        RecordingClaims claims=new RecordingClaims();claims.target=Optional.of(new RewardClaimTarget(
            ParticipantId.player(player),0));
        claims.results.add(RewardClaimStatus.NEEDS_CHOICE);claims.results.add(RewardClaimStatus.CLAIMED);
        RecordingChoices choices=new RecordingChoices();
        AuthenticatedQuestClaimService service=service(player,quest,claims,choices,randomIndex(2));

        assertEquals(RewardClaimStatus.CLAIMED,service.forceClaimWithBqRandomChoices(player,questId,10L));
        assertEquals(Collections.singletonList(Integer.valueOf(2)),choices.indexes);
    }
    @Test public void forceClaimPreservesExistingChoiceAndRollsBackOnInvalidSelection(){
        UUID player=UUID.randomUUID(),questId=UUID.randomUUID();
        RewardDefinition first=choice("first", "minecraft:diamond", "enhancedlootbags:common");
        RewardDefinition second=choice("second", "minecraft:emerald", "minecraft:gold_ingot");
        QuestDefinition quest=definition(questId,1,java.util.Arrays.asList(first,second));
        RecordingClaims claims=new RecordingClaims();claims.target=Optional.of(new RewardClaimTarget(
            ParticipantId.player(player),0));
        claims.results.add(RewardClaimStatus.NEEDS_CHOICE);claims.results.add(RewardClaimStatus.CLAIMED);
        RecordingChoices choices=new RecordingChoices();choices.results.add(RewardChoiceSelectionStatus.CONFLICT);
        choices.results.add(RewardChoiceSelectionStatus.SELECTED);
        AuthenticatedQuestClaimService service=service(player,quest,claims,choices,randomIndex(0));
        assertEquals(RewardClaimStatus.CLAIMED,service.forceClaimWithBqRandomChoices(player,questId,11L));
        assertEquals(2,choices.indexes.size());
        assertEquals(2,claims.calls);

        RecordingClaims rejectedClaims=new RecordingClaims();rejectedClaims.target=claims.target;
        rejectedClaims.results.add(RewardClaimStatus.NEEDS_CHOICE);
        RecordingChoices rejectedChoices=new RecordingChoices();
        rejectedChoices.results.add(RewardChoiceSelectionStatus.SELECTED);
        rejectedChoices.results.add(RewardChoiceSelectionStatus.INVALID);
        RecordingTransaction transaction=new RecordingTransaction();
        QuestProgressSnapshot progress=new QuestProgressSnapshot(ParticipantId.player(player),questId,1,
            QuestStatus.COMPLETED,Collections.emptyMap(),10L,0);
        AuthenticatedQuestClaimService rejectedService=new AuthenticatedQuestClaimService(new Definitions(quest),
            new Runtime(progress),rejectedClaims,transaction,new RewardSuppressionPolicy(false),rejectedChoices,
            randomIndex(0));
        assertEquals(RewardClaimStatus.INVALID_STATE,rejectedService.forceClaimWithBqRandomChoices(player,questId,12L));
        assertEquals(1,rejectedClaims.calls);
        assertEquals(1,transaction.rollbacks);
    }

    private static AuthenticatedQuestClaimService service(UUID player,QuestDefinition definition,RecordingClaims claims,
        RecordingChoices choices,Random random){return new AuthenticatedQuestClaimService(new Definitions(definition),
            new Runtime(new QuestProgressSnapshot(ParticipantId.player(player),definition.getId(),definition.getVersion(),
                QuestStatus.COMPLETED,Collections.emptyMap(),10L,0)),claims,new DirectTransaction(),
            new RewardSuppressionPolicy(false),choices,random);}
    private static Random randomIndex(final int value){return new Random(1L){private static final long serialVersionUID=1L;
        @Override public int nextInt(int bound){return Math.min(value,bound-1);}};}
    private static RewardDefinition choice(String key,String... registries){Map<String,String> values=new LinkedHashMap<String,String>();
        values.put("item.count",Integer.toString(registries.length));for(int i=0;i<registries.length;i++)values.put("item."+i+".registryName",registries[i]);
        return new RewardDefinition(key,"bq_standard:choice",values);}
    private static QuestDefinition definition(UUID id,int version){return definition(id,version,Collections.<RewardDefinition>emptyList());}
    private static QuestDefinition definition(UUID id,int version,List<RewardDefinition> rewards){return new QuestDefinition(id,version,"Q","",
        QuestLogic.AND,QuestLogic.AND,Collections.emptySet(),Collections.emptyList(),rewards,RepeatPolicy.never());}
    private static final class RecordingChoices implements RewardChoiceSelectionRepository{private final List<String> keys=new ArrayList<String>();
        private final List<Integer> indexes=new ArrayList<Integer>();private final List<RewardChoiceSelectionStatus> results=new ArrayList<RewardChoiceSelectionStatus>();
        public RewardChoiceSelectionStatus select(String key,UUID player,int index,long at){keys.add(key);indexes.add(index);
            return results.isEmpty()?RewardChoiceSelectionStatus.SELECTED:results.remove(0);}}
    private static final class RecordingClaims implements RewardClaimRepository{private UUID quest,player;private int version,cycle;private long time;private int calls;
        private final List<RewardClaimStatus> results=new ArrayList<RewardClaimStatus>();
        private OptionalInt recipientCycle=OptionalInt.empty();private Optional<RewardClaimTarget> target=Optional.empty();private RewardClaimTarget claimedTarget;
        public Optional<RewardClaimTarget> findNextRecipientTarget(UUID q,int v,UUID p){return target;}
        public RewardClaimStatus claim(RewardClaimTarget target,UUID q,int v,UUID p,long t){claimedTarget=target;return claim(q,v,target.getCycle(),p,t);}
        public OptionalInt findLatestRecipientCycle(UUID q,int v,UUID p){return recipientCycle;}
        public RewardClaimStatus claim(UUID q,int v,int c,UUID p,long t){quest=q;version=v;cycle=c;player=p;time=t;calls++;
            return results.isEmpty()?RewardClaimStatus.CLAIMED:results.remove(0);}}
    private static final class DirectTransaction implements QuestTransaction{public <T>T inTransaction(Work<T> work){return work.execute();}}
    private static final class RecordingTransaction implements QuestTransaction{private int rollbacks;
        public <T>T inTransaction(Work<T> work){try{return work.execute();}catch(RuntimeException failure){rollbacks++;throw failure;}}}
    private static final class Runtime implements QuestRuntimeRepository{private final QuestProgressSnapshot value;private Runtime(QuestProgressSnapshot value){this.value=value;}
        public Optional<QuestProgressSnapshot> findProgress(ParticipantId p,UUID q,int v){return Optional.ofNullable(value);}
        public boolean hasPendingConsumptionSubmission(ParticipantId p,UUID q,int v,int c){return false;}
        public boolean hasClaimableRewardEntitlement(ParticipantId p,UUID q,int v,int c){return false;}
        public long findRepeatCycleAnchorMillis(ParticipantId p,UUID q,int v,int c,long fallback){return fallback;}
        public boolean recordFactIfAbsent(GameplayFact f){return false;}public void saveProgress(QuestProgressSnapshot p){}public void insertEntitlementsIfAbsent(Set<RewardEntitlement> e){}}
    private static final class Definitions implements QuestDefinitionRepository{private final QuestDefinition value;private Definitions(QuestDefinition value){this.value=value;}
        public Optional<StoredQuestDefinition> findPublished(UUID id){return value==null?Optional.empty():Optional.of(new StoredQuestDefinition(value,QuestDefinitionLifecycle.PUBLISHED,"hash",1L));}
        public StoredQuestDefinition createDraft(QuestDefinition d){throw new UnsupportedOperationException();}public StoredQuestDefinition updateDraft(QuestDefinition d,String h){throw new UnsupportedOperationException();}
        public boolean publish(UUID q,int v,String h,long t){return false;}public boolean retire(UUID q,int v){return false;}public Optional<StoredQuestDefinition> find(UUID q,int v){return Optional.empty();}
        public java.util.List<StoredQuestDefinition> findAllPublished(){return Collections.emptyList();}}
}
