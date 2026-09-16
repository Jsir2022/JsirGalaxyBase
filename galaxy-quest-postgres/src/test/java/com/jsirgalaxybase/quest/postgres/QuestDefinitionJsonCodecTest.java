package com.jsirgalaxybase.quest.postgres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.QuestBehavior;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestParticipantScope;
import com.jsirgalaxybase.quest.core.QuestVisibility;
import com.jsirgalaxybase.quest.core.RepeatPolicy;

public class QuestDefinitionJsonCodecTest {
    @Test
    public void roundTripPreservesBehaviorAndFixedRepeatPolicy() {
        QuestBehavior behavior = QuestBehavior.builder().visibility(QuestVisibility.CHAIN)
            .iconReference("{item}").main(true).silent(true).autoClaim(true).progressWhileLocked(true)
            .simultaneous(true).global(true).globalShare(true).participantScope(QuestParticipantScope.PUBLIC)
            .updateSound("update").completeSound("complete").build();
        QuestDefinition source = new QuestDefinition(UUID.randomUUID(), 2, "Quest", "Description", QuestLogic.OR,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.emptyList(), Collections.emptyList(),
            RepeatPolicy.after(5000L, false), behavior);

        QuestDefinitionJsonCodec codec = new QuestDefinitionJsonCodec();
        QuestDefinition restored = codec.decode(codec.encode(source).json);

        assertFalse(restored.getRepeatPolicy().isRelative());
        assertEquals(5000L, restored.getRepeatPolicy().getCooldownMillis());
        assertEquals(QuestVisibility.CHAIN, restored.getBehavior().getVisibility());
        assertEquals("{item}", restored.getBehavior().getIconReference());
        assertTrue(restored.getBehavior().isMain());
        assertTrue(restored.getBehavior().isSilent());
        assertTrue(restored.getBehavior().isAutoClaim());
        assertTrue(restored.getBehavior().isProgressWhileLocked());
        assertTrue(restored.getBehavior().isSimultaneous());
        assertTrue(restored.getBehavior().isGlobal());
        assertTrue(restored.getBehavior().isGlobalShare());
        assertEquals(QuestParticipantScope.PUBLIC, restored.getBehavior().getParticipantScope());
        assertEquals("complete", restored.getBehavior().getCompleteSound());
    }

    @Test
    public void legacyStoredJsonGetsCompatibleDefaults() {
        String legacy = "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"version\":1,"
            + "\"name\":\"Legacy\",\"description\":\"\",\"prerequisiteLogic\":\"AND\","
            + "\"taskLogic\":\"AND\",\"repeatCooldownMillis\":-1,\"prerequisites\":[],"
            + "\"tasks\":[],\"rewards\":[]}";
        QuestDefinition restored = new QuestDefinitionJsonCodec().decode(legacy);
        assertEquals(QuestVisibility.NORMAL, restored.getBehavior().getVisibility());
        assertFalse(restored.getBehavior().isAutoClaim());
        assertEquals(QuestParticipantScope.PLAYER, restored.getBehavior().getParticipantScope());
        assertTrue(restored.getRepeatPolicy().isRelative());
    }
}
