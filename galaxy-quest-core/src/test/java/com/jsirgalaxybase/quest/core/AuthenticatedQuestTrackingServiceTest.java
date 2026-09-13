package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

public class AuthenticatedQuestTrackingServiceTest {
    @Test public void togglesOnlyPublishedQuestForAuthenticatedPlayer(){UUID player=UUID.randomUUID(),quest=UUID.randomUUID();RecordingTracking tracking=new RecordingTracking();AuthenticatedQuestTrackingService service=new AuthenticatedQuestTrackingService(new Definitions(definition(quest)),tracking,new DirectTransaction());assertEquals(Boolean.TRUE,service.toggle(player,quest,12L).get());assertEquals(player,tracking.player);assertEquals(quest,tracking.quest);assertEquals(12L,tracking.time);}
    @Test public void missingPublishedQuestDoesNotWritePreference(){RecordingTracking tracking=new RecordingTracking();AuthenticatedQuestTrackingService service=new AuthenticatedQuestTrackingService(new Definitions(null),tracking,new DirectTransaction());assertFalse(service.toggle(UUID.randomUUID(),UUID.randomUUID(),0L).isPresent());assertNull(tracking.player);}
    private static QuestDefinition definition(UUID id){return new QuestDefinition(id,1,"Q","",QuestLogic.AND,QuestLogic.AND,Collections.emptySet(),Collections.emptyList(),Collections.emptyList());}
    private static final class RecordingTracking implements QuestTrackingRepository{UUID player,quest;long time;public boolean toggle(UUID p,UUID q,long t){player=p;quest=q;time=t;return true;}}
    private static final class DirectTransaction implements QuestTransaction{public <T>T inTransaction(Work<T> work){return work.execute();}}
    private static final class Definitions implements QuestDefinitionRepository{private final QuestDefinition value;Definitions(QuestDefinition value){this.value=value;}public Optional<StoredQuestDefinition> findPublished(UUID id){return value==null?Optional.empty():Optional.of(new StoredQuestDefinition(value,QuestDefinitionLifecycle.PUBLISHED,"hash",1L));}public StoredQuestDefinition createDraft(QuestDefinition d){throw new UnsupportedOperationException();}public StoredQuestDefinition updateDraft(QuestDefinition d,String h){throw new UnsupportedOperationException();}public boolean publish(UUID q,int v,String h,long t){return false;}public boolean retire(UUID q,int v){return false;}public Optional<StoredQuestDefinition> find(UUID q,int v){return Optional.empty();}public java.util.List<StoredQuestDefinition> findAllPublished(){return Collections.emptyList();}}
}
