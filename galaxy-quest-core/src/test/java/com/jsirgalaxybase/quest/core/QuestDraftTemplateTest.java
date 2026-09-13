package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.UUID;
import org.junit.Test;

public class QuestDraftTemplateTest {
    @Test public void allTemplatesProduceDefinitionsAcceptedByStandardRegistry(){QuestDefinitionValidator validator=new QuestDefinitionValidator();for(QuestDraftTemplate template:QuestDraftTemplate.values()){QuestDefinition value=template.create(UUID.randomUUID(),"Template");assertEquals(1,value.getVersion());assertTrue(template.name(),validator.validate(value,StandardQuestEditorTypeRegistry.create()).isEmpty());}}
}
