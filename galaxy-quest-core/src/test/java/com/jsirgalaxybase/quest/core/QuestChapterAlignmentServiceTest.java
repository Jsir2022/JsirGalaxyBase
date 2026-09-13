package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

public class QuestChapterAlignmentServiceTest {
    @Test public void alignsStoredEntriesAtomicallyWithoutTrustingClientCoordinates(){
        UUID a=UUID.randomUUID(),b=UUID.randomUUID(),chapter=UUID.randomUUID(); Chapters chapters=new Chapters(chapter,a,b);
        QuestChapterAlignmentService service=service(chapters,true); StoredQuestChapter before=chapters.value;
        QuestChapterManagementResult result=service.align(actor(),new QuestChapterAlignmentRequest(chapter,1,before.getContentHash(),Arrays.asList(a,b),QuestChapterAlignment.RIGHT));
        assertTrue(result.isSuccess()); QuestChapterEntry first=result.getChapter().getDefinition().getEntries().get(0),second=result.getChapter().getDefinition().getEntries().get(1);
        assertEquals(first.getX()+first.getWidth(),second.getX()+second.getWidth()); assertEquals(1,chapters.writes);
    }
    @Test public void rejectsUnauthorizedMissingAndDuplicateSelectionBeforeWrite(){
        UUID a=UUID.randomUUID(),b=UUID.randomUUID(),chapter=UUID.randomUUID(); Chapters chapters=new Chapters(chapter,a,b); QuestChapterAlignmentRequest request=new QuestChapterAlignmentRequest(chapter,1,chapters.value.getContentHash(),Arrays.asList(a,b),QuestChapterAlignment.LEFT);
        assertEquals(QuestChapterManagementResult.Status.FORBIDDEN,service(chapters,false).align(actor(),request).getStatus());assertEquals(0,chapters.writes);
        try{new QuestChapterAlignmentRequest(chapter,1,"hash",Arrays.asList(a,a),QuestChapterAlignment.LEFT);assertFalse(true);}catch(IllegalArgumentException expected){}
        assertEquals(QuestChapterManagementResult.Status.INVALID,service(chapters,true).align(actor(),new QuestChapterAlignmentRequest(chapter,1,chapters.value.getContentHash(),Arrays.asList(a,UUID.randomUUID()),QuestChapterAlignment.LEFT)).getStatus());assertEquals(0,chapters.writes);
    }
    @Test public void centersOnTheSelectedGroupBoundsRatherThanTheSmallestEntryCenter(){
        UUID a=UUID.randomUUID(),b=UUID.randomUUID(),chapter=UUID.randomUUID();Chapters chapters=new Chapters(chapter,a,b);
        QuestChapterManagementResult result=service(chapters,true).align(actor(),new QuestChapterAlignmentRequest(chapter,1,
            chapters.value.getContentHash(),Arrays.asList(a,b),QuestChapterAlignment.CENTER_X));
        assertEquals(55,result.getChapter().getDefinition().getEntries().get(0).getX());
        assertEquals(45,result.getChapter().getDefinition().getEntries().get(1).getX());
    }
    private static QuestEditorActor actor(){return new QuestEditorActor(UUID.randomUUID(),"editor");}
    private static QuestChapterAlignmentService service(Chapters chapters,final boolean allowed){return new QuestChapterAlignmentService(chapters,new QuestTransaction(){public <T>T inTransaction(Work<T> work){return work.execute();}},new QuestEditorAuthorization(){public boolean canManageDefinitions(QuestEditorActor actor){return allowed;}});}
    private static final class Chapters implements QuestChapterRepository {private StoredQuestChapter value;private int writes; Chapters(UUID chapter,UUID a,UUID b){QuestChapterDefinition definition=new QuestChapterDefinition(chapter,1,"C","","","",Arrays.asList(new QuestChapterEntry(a,10,30,20,10),new QuestChapterEntry(b,80,60,40,20)));value=new StoredQuestChapter(definition,QuestDefinitionLifecycle.DRAFT,"hash",-1L);}
        public Optional<StoredQuestChapter> find(UUID id,int version){return value.getDefinition().getId().equals(id)&&version==1?Optional.of(value):Optional.<StoredQuestChapter>empty();} public Optional<StoredQuestChapter> findPublished(UUID id){return Optional.empty();}public java.util.List<StoredQuestChapter> findAllPublished(){return Collections.emptyList();} public StoredQuestChapter createDraft(QuestChapterDefinition definition){throw new UnsupportedOperationException();} public StoredQuestChapter updateDraft(QuestChapterDefinition definition,String expectedHash){if(!value.getContentHash().equals(expectedHash))throw new QuestDefinitionConflictException("changed");writes++;value=new StoredQuestChapter(definition,QuestDefinitionLifecycle.DRAFT,"changed",-1L);return value;} public boolean publish(UUID id,int version,String expectedHash,long at){return false;}public boolean retire(UUID id,int version){return false;}
    }
}
