package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.junit.Test;

public class QuestDefinitionImpactAnalyzerTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CHAPTER = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test public void reportsDirectTransitiveDependentsAndPublishedPlacements() {
        QuestDefinitionImpactReport report = new QuestDefinitionImpactAnalyzer().analyze(A,
            Arrays.asList(stored(quest(C, B)), stored(quest(A)), stored(quest(B, A))),
            Collections.singletonList(new StoredQuestChapter(new QuestChapterDefinition(CHAPTER, 1, "Start", "", "", "",
                Collections.singletonList(new QuestChapterEntry(A, 0, 0, 24, 24))),
                QuestDefinitionLifecycle.PUBLISHED, "chapter", 1L)));
        assertTrue(report.isPublished());
        assertEquals(2, report.getDependents().size());
        assertEquals(B, report.getDependents().get(0).getQuestId());
        assertTrue(report.getDependents().get(0).isDirect());
        assertEquals(C, report.getDependents().get(1).getQuestId());
        assertEquals(2, report.getDependents().get(1).getDistance());
        assertEquals(CHAPTER, report.getPlacements().get(0).getChapterId());
        assertFalse(report.isSafeToRetire());
    }

    @Test public void ignoresDraftCatalogRowsAndAllowsAnUnreferencedPublishedQuest() {
        StoredQuestDefinition draftDependent = new StoredQuestDefinition(quest(B, A), QuestDefinitionLifecycle.DRAFT,
            "draft", -1L);
        QuestDefinitionImpactReport report = new QuestDefinitionImpactAnalyzer().analyze(A,
            Arrays.asList(stored(quest(A)), draftDependent), Collections.<StoredQuestChapter>emptyList());
        assertTrue(report.getDependents().isEmpty());
        assertTrue(report.isSafeToRetire());
    }

    @Test public void completionRewardReferenceAlsoPreventsRetirement() {
        java.util.Map<String, String> parameters = Collections.singletonMap("questUuid", A.toString());
        QuestDefinition source = new QuestDefinition(B, 1, "Completion bridge", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(),
            Collections.singletonList(new RewardDefinition("complete", "bq_standard:questcompletion", parameters)));
        QuestDefinitionImpactReport report = new QuestDefinitionImpactAnalyzer().analyze(A,
            Arrays.asList(stored(quest(A)), stored(source)), Collections.<StoredQuestChapter>emptyList());
        assertEquals(1, report.getDependents().size());
        assertEquals(QuestDefinitionImpactReport.Dependent.ReferenceKind.REWARD_REFERENCE,
            report.getDependents().get(0).getReferenceKind());
        assertFalse(report.isSafeToRetire());
    }

    private static StoredQuestDefinition stored(QuestDefinition value) {
        return new StoredQuestDefinition(value, QuestDefinitionLifecycle.PUBLISHED, value.getId().toString(), 1L);
    }
    private static QuestDefinition quest(UUID id, UUID... prerequisites) {
        return new QuestDefinition(id, 1, "Quest " + id.toString().substring(35), "", QuestLogic.AND, QuestLogic.AND,
            new LinkedHashSet<UUID>(Arrays.asList(prerequisites)), Collections.<TaskDefinition>emptyList(),
            Collections.<RewardDefinition>emptyList());
    }
}
