package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class AuthenticatedRewardChoiceServiceTest {
    @Test
    public void derivesEntitlementFromAuthenticatedPlayerPublishedVersionAndCurrentCycle() {
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        RewardDefinition choice = new RewardDefinition("pick", "bq_standard:choice",
            Collections.singletonMap("item.count", "2"));
        QuestDefinition quest = new QuestDefinition(questId, 3, "Quest", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(), Arrays.asList(choice));
        AtomicReference<String> key = new AtomicReference<String>();
        AtomicReference<UUID> owner = new AtomicReference<UUID>();
        RewardChoiceSelectionRepository repository = (entitlementKey, currentPlayerId, choiceIndex, selectedAt) -> {
            key.set(entitlementKey); owner.set(currentPlayerId); assertEquals(1, choiceIndex);
            return RewardChoiceSelectionStatus.SELECTED;
        };
        AuthenticatedRewardChoiceService service = new AuthenticatedRewardChoiceService(
            new Definitions(quest),
            new RuntimeStub(player, questId, 3, 7), repository, new DirectTransaction());

        assertEquals(RewardChoiceSelectionStatus.SELECTED, service.select(player, questId, "pick", 1, 99L));
        assertEquals(player, owner.get());
        assertEquals("player:" + player + ":" + questId + ":3:7:pick", key.get());
    }

    @Test
    public void rejectsNonChoiceRewardBeforeRepositoryCall() {
        UUID player = UUID.randomUUID(); UUID questId = UUID.randomUUID();
        QuestDefinition quest = new QuestDefinition(questId, 1, "Quest", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(), Arrays.asList(
                new RewardDefinition("coins", "bq_standard:command", Collections.<String,String>emptyMap())));
        AuthenticatedRewardChoiceService service = new AuthenticatedRewardChoiceService(
            new Definitions(quest),
            new RuntimeStub(player, questId, 1, 0), (key, owner, index, time) -> {
                throw new AssertionError("repository must not be called");
            }, new DirectTransaction());
        assertEquals(RewardChoiceSelectionStatus.INVALID, service.select(player, questId, "coins", 0, 1L));
    }

    @Test public void frozenGroupRecipientEntitlementWinsOverLegacyPersonalCycle(){
        UUID player=UUID.randomUUID(),questId=UUID.randomUUID();RewardDefinition choice=new RewardDefinition(
            "pick","bq_standard:choice",Collections.singletonMap("item.count","2"));
        QuestDefinition quest=new QuestDefinition(questId,3,"Quest","",QuestLogic.AND,QuestLogic.AND,
            Collections.<UUID>emptySet(),Collections.<TaskDefinition>emptyList(),Arrays.asList(choice));
        RewardChoiceSelectionRepository repository=new RewardChoiceSelectionRepository(){
            public RewardChoiceSelectionStatus select(String key,UUID owner,int index,long time){
                throw new AssertionError("legacy personal entitlement must not be selected");
            }
            public RewardChoiceSelectionStatus selectLatest(UUID q,int version,String reward,UUID owner,int index,long time){
                assertEquals(questId,q);assertEquals(3,version);assertEquals("pick",reward);assertEquals(player,owner);
                return RewardChoiceSelectionStatus.SELECTED;
            }
        };
        AuthenticatedRewardChoiceService service=new AuthenticatedRewardChoiceService(new Definitions(quest),
            new RuntimeStub(player,questId,3,7),repository,new DirectTransaction());
        assertEquals(RewardChoiceSelectionStatus.SELECTED,service.select(player,questId,"pick",1,99L));
    }

    private static final class RuntimeStub implements QuestRuntimeRepository {
        private final UUID player, quest; private final int version, cycle;
        RuntimeStub(UUID player, UUID quest, int version, int cycle) {
            this.player=player;this.quest=quest;this.version=version;this.cycle=cycle;
        }
        @Override public Optional<QuestProgressSnapshot> findProgress(ParticipantId participant,UUID questId,int questVersion) {
            assertEquals(ParticipantId.player(player), participant);assertEquals(quest,questId);assertEquals(version,questVersion);
            return Optional.of(new QuestProgressSnapshot(participant,questId,questVersion,
                QuestStatus.COMPLETED,Collections.<String,TaskProgress>emptyMap(),0L,cycle));
        }
        @Override public boolean recordFactIfAbsent(GameplayFact fact){return false;}
        @Override public void saveProgress(QuestProgressSnapshot snapshot){throw new UnsupportedOperationException();}
        @Override public void insertEntitlementsIfAbsent(Set<RewardEntitlement> entitlements){throw new UnsupportedOperationException();}
    }
    private static final class DirectTransaction implements QuestTransaction {
        @Override public <T>T inTransaction(Work<T> work){return work.execute();}
    }
    private static final class Definitions implements QuestDefinitionRepository {
        private final QuestDefinition value; Definitions(QuestDefinition value){this.value=value;}
        @Override public Optional<StoredQuestDefinition> findPublished(UUID id){return Optional.of(new StoredQuestDefinition(value,QuestDefinitionLifecycle.PUBLISHED,"hash",1L));}
        @Override public StoredQuestDefinition createDraft(QuestDefinition value){throw new UnsupportedOperationException();}
        @Override public StoredQuestDefinition updateDraft(QuestDefinition value,String hash){throw new UnsupportedOperationException();}
        @Override public boolean publish(UUID id,int version,String hash,long time){return false;}
        @Override public boolean retire(UUID id,int version){return false;}
        @Override public Optional<StoredQuestDefinition> find(UUID id,int version){return Optional.empty();}
        @Override public java.util.List<StoredQuestDefinition> findAllPublished(){return Collections.emptyList();}
    }
}
