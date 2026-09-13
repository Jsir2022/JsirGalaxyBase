package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class QuestDefinitionBatchClonerTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID EXTERNAL = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID COPY_A = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID COPY_B = UUID.fromString("10000000-0000-0000-0000-000000000002");

    @Test public void remapsInternalPrerequisitesAndPreservesExternalReferencesAndSemantics() {
        QuestDefinition first = quest(A, Collections.singleton(EXTERNAL));
        QuestDefinition second = quest(B, new LinkedHashSet<UUID>(Arrays.asList(A, EXTERNAL)));
        QuestDefinitionBatchCloner.Result result = new QuestDefinitionBatchCloner(sequence(COPY_A, COPY_B))
            .clone(Arrays.asList(first, second));

        assertEquals(COPY_A, result.getRemappedIds().get(A));
        assertEquals(COPY_B, result.getRemappedIds().get(B));
        assertEquals(Collections.singleton(EXTERNAL), result.getDefinitions().get(0).getPrerequisites());
        assertEquals(new LinkedHashSet<UUID>(Arrays.asList(COPY_A, EXTERNAL)),
            result.getDefinitions().get(1).getPrerequisites());
        assertEquals(1, result.getDefinitions().get(1).getVersion());
        assertSame(second.getRepeatPolicy(), result.getDefinitions().get(1).getRepeatPolicy());
        assertSame(second.getBehavior(), result.getDefinitions().get(1).getBehavior());
        assertSame(second.getTasks().get(0), result.getDefinitions().get(1).getTasks().get(0));
    }

    @Test public void retriesCollidingOrNullServerIdsWithoutChangingSourceOrder() {
        QuestDefinitionBatchCloner.Result result = new QuestDefinitionBatchCloner(
            sequence(null, A, COPY_A, COPY_A, COPY_B)).clone(Arrays.asList(quest(A, Collections.<UUID>emptySet()),
                quest(B, Collections.<UUID>emptySet())));
        assertEquals(Arrays.asList(COPY_A, COPY_B), Arrays.asList(result.getDefinitions().get(0).getId(),
            result.getDefinitions().get(1).getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDuplicateSourceDefinitions() {
        new QuestDefinitionBatchCloner(sequence(COPY_A)).clone(Arrays.asList(quest(A, Collections.<UUID>emptySet()),
            quest(A, Collections.<UUID>emptySet())));
    }

    @Test public void resultCollectionsAreImmutable() {
        QuestDefinitionBatchCloner.Result result = new QuestDefinitionBatchCloner(sequence(COPY_A))
            .clone(Collections.singletonList(quest(A, Collections.<UUID>emptySet())));
        boolean definitions = false, mapping = false;
        try { result.getDefinitions().clear(); } catch (UnsupportedOperationException expected) { definitions = true; }
        try { result.getRemappedIds().clear(); } catch (UnsupportedOperationException expected) { mapping = true; }
        assertTrue(definitions && mapping);
    }

    private static QuestDefinition quest(UUID id, java.util.Set<UUID> prerequisites) {
        TaskDefinition task = new TaskDefinition("task", "bq_standard:checkbox", false,
            Collections.<String, String>emptyMap());
        return new QuestDefinition(id, 7, "任务 " + id, "说明", QuestLogic.AND, QuestLogic.AND, prerequisites,
            Collections.singletonList(task), Collections.<RewardDefinition>emptyList(), RepeatPolicy.after(1000L),
            QuestBehavior.builder().main(true).build());
    }

    private static QuestDefinitionBatchCloner.IdSource sequence(final UUID... values) {
        return new QuestDefinitionBatchCloner.IdSource() { private int index;
            @Override public UUID next() { return values[index++]; }
        };
    }
}
