package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.junit.Test;

public class QuestDefinitionBatchImpactAnalyzerTest {
    private static final UUID A = id(1), B = id(2), C = id(3), CHAPTER = id(10);

    @Test public void internalReferencesDoNotBlockButExternalDependentsDo() {
        QuestDefinitionImpactAnalyzer analyzer = new QuestDefinitionImpactAnalyzer();
        QuestDefinitionBatchImpactReport internal = analyzer.analyzeBatch(Arrays.asList(A, B),
            Arrays.asList(stored(quest(A)), stored(quest(B, A))), Collections.<StoredQuestChapter>emptyList());
        assertTrue(internal.isSafeToRetire());
        QuestDefinitionBatchImpactReport external = analyzer.analyzeBatch(Arrays.asList(A, B),
            Arrays.asList(stored(quest(A)), stored(quest(B, A)), stored(quest(C, B))),
            Collections.<StoredQuestChapter>emptyList());
        assertFalse(external.isSafeToRetire());
        assertEquals(C, external.getExternalDependents().get(0).getQuestId());
    }

    @Test public void missingTargetAndAnyPublishedPlacementFailClosed() {
        StoredQuestChapter chapter = new StoredQuestChapter(new QuestChapterDefinition(CHAPTER, 1, "Start", "", "", "",
            Collections.singletonList(new QuestChapterEntry(B, 0, 0, 24, 24))),
            QuestDefinitionLifecycle.PUBLISHED, "chapter", 1L);
        QuestDefinitionBatchImpactReport report = new QuestDefinitionImpactAnalyzer().analyzeBatch(Arrays.asList(A, B),
            Collections.singletonList(stored(quest(B))), Collections.singletonList(chapter));
        assertEquals(Collections.singletonList(A), report.getUnpublishedTargetIds());
        assertEquals(B, report.getPlacements().get(0).getTargetId());
        assertFalse(report.isSafeToRetire());
    }

    @Test(expected=IllegalArgumentException.class)
    public void duplicateTargetsAreRejected() {
        new QuestDefinitionImpactAnalyzer().analyzeBatch(Arrays.asList(A, A),
            Collections.<StoredQuestDefinition>emptyList(), Collections.<StoredQuestChapter>emptyList());
    }

    private static UUID id(long value) { return new UUID(0L, value); }
    private static StoredQuestDefinition stored(QuestDefinition value) {
        return new StoredQuestDefinition(value, QuestDefinitionLifecycle.PUBLISHED, value.getId().toString(), 1L);
    }
    private static QuestDefinition quest(UUID id, UUID... prerequisites) {
        return new QuestDefinition(id, 1, "Quest " + id, "", QuestLogic.AND, QuestLogic.AND,
            new LinkedHashSet<UUID>(Arrays.asList(prerequisites)), Collections.<TaskDefinition>emptyList(),
            Collections.<RewardDefinition>emptyList());
    }
}
