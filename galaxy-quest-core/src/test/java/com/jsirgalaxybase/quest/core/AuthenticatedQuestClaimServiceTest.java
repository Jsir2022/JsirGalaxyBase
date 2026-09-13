package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
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
    private static QuestDefinition definition(UUID id,int version){return new QuestDefinition(id,version,"Q","",
        QuestLogic.AND,QuestLogic.AND,Collections.emptySet(),Collections.emptyList(),Collections.emptyList(),RepeatPolicy.never());}
    private static final class RecordingClaims implements RewardClaimRepository{private UUID quest,player;private int version,cycle;private long time;
        public RewardClaimStatus claim(UUID q,int v,int c,UUID p,long t){quest=q;version=v;cycle=c;player=p;time=t;return RewardClaimStatus.CLAIMED;}}
    private static final class DirectTransaction implements QuestTransaction{public <T>T inTransaction(Work<T> work){return work.execute();}}
    private static final class Runtime implements QuestRuntimeRepository{private final QuestProgressSnapshot value;private Runtime(QuestProgressSnapshot value){this.value=value;}
        public Optional<QuestProgressSnapshot> findProgress(ParticipantId p,UUID q,int v){return Optional.ofNullable(value);}
        public boolean recordFactIfAbsent(GameplayFact f){return false;}public void saveProgress(QuestProgressSnapshot p){}public void insertEntitlementsIfAbsent(Set<RewardEntitlement> e){}}
    private static final class Definitions implements QuestDefinitionRepository{private final QuestDefinition value;private Definitions(QuestDefinition value){this.value=value;}
        public Optional<StoredQuestDefinition> findPublished(UUID id){return value==null?Optional.empty():Optional.of(new StoredQuestDefinition(value,QuestDefinitionLifecycle.PUBLISHED,"hash",1L));}
        public StoredQuestDefinition createDraft(QuestDefinition d){throw new UnsupportedOperationException();}public StoredQuestDefinition updateDraft(QuestDefinition d,String h){throw new UnsupportedOperationException();}
        public boolean publish(UUID q,int v,String h,long t){return false;}public boolean retire(UUID q,int v){return false;}public Optional<StoredQuestDefinition> find(UUID q,int v){return Optional.empty();}
        public java.util.List<StoredQuestDefinition> findAllPublished(){return Collections.emptyList();}}
}
