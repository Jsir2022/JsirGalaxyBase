package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

public class QuestDefinitionGraphValidatorTest {
    @Test public void acceptsPublishedAcyclicPrerequisite(){Catalog catalog=new Catalog();QuestDefinition prerequisite=definition(UUID.randomUUID(),Collections.<UUID>emptySet());catalog.publish(prerequisite);QuestDefinition candidate=definition(UUID.randomUUID(),Collections.singleton(prerequisite.getId()));assertTrue(new QuestDefinitionGraphValidator().validateForPublish(candidate,catalog).isEmpty());}

    @Test public void rejectsMissingDirectPrerequisite(){UUID missing=UUID.randomUUID();List<QuestEditorValidationIssue> issues=new QuestDefinitionGraphValidator().validateForPublish(definition(UUID.randomUUID(),Collections.singleton(missing)),new Catalog());assertEquals(1,issues.size());assertEquals("missing_prerequisite",issues.get(0).getCode());}

    @Test public void rejectsCycleCreatedByReplacingLatestVersion(){UUID candidateId=UUID.randomUUID(),otherId=UUID.randomUUID();Catalog catalog=new Catalog();catalog.publish(definition(candidateId,Collections.<UUID>emptySet()));catalog.publish(definition(otherId,Collections.singleton(candidateId)));List<QuestEditorValidationIssue> issues=new QuestDefinitionGraphValidator().validateForPublish(definition(candidateId,Collections.singleton(otherId)),catalog);assertEquals(1,issues.size());assertEquals("dependency_cycle",issues.get(0).getCode());}

    @Test public void rejectsIncompletePublishedGraph(){UUID missing=UUID.randomUUID();QuestDefinition root=definition(UUID.randomUUID(),Collections.singleton(missing));Catalog catalog=new Catalog();catalog.publish(root);List<QuestEditorValidationIssue> issues=new QuestDefinitionGraphValidator().validateForPublish(definition(UUID.randomUUID(),Collections.singleton(root.getId())),catalog);assertEquals(1,issues.size());assertEquals("incomplete_graph",issues.get(0).getCode());}

    private static QuestDefinition definition(UUID id,Set<UUID> prerequisites){return new QuestDefinition(id,1,"Quest","",QuestLogic.AND,QuestLogic.AND,prerequisites,Collections.<TaskDefinition>emptyList(),Collections.<RewardDefinition>emptyList(),RepeatPolicy.never());}
    private static final class Catalog implements QuestDefinitionRepository{private final Map<UUID,StoredQuestDefinition> values=new LinkedHashMap<UUID,StoredQuestDefinition>();void publish(QuestDefinition value){values.put(value.getId(),new StoredQuestDefinition(value,QuestDefinitionLifecycle.PUBLISHED,"hash",1L));}public Optional<StoredQuestDefinition> findPublished(UUID id){return Optional.ofNullable(values.get(id));}public Optional<StoredQuestDefinition> find(UUID id,int version){return findPublished(id);}public List<StoredQuestDefinition> findAllPublished(){return new java.util.ArrayList<StoredQuestDefinition>(values.values());}public StoredQuestDefinition createDraft(QuestDefinition value){throw new UnsupportedOperationException();}public StoredQuestDefinition updateDraft(QuestDefinition value,String hash){throw new UnsupportedOperationException();}public boolean publish(UUID id,int version,String hash,long at){return false;}public boolean retire(UUID id,int version){return false;}}
}
