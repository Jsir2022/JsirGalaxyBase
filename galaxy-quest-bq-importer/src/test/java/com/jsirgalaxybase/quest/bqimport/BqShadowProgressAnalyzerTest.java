package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskProgress;

public class BqShadowProgressAnalyzerTest {
    @Test
    public void classifiesIdenticalAheadAndOneSidedRowsWithReasons() {
        UUID player = UUID.randomUUID();
        UUID quest = UUID.randomUUID();
        QuestDefinition definition = definition(quest);
        BqImportedQuestProgress bq = imported(quest, false, 2L);
        QuestProgressSnapshot same = base(player, quest, QuestStatus.IN_PROGRESS, 2L);
        BqShadowProgressAnalyzer analyzer = new BqShadowProgressAnalyzer();
        assertEquals(BqShadowRelation.IDENTICAL, analyzer.compare(
            Collections.singletonList(player(player, bq)), Collections.singletonList(same),
            Collections.singletonMap(quest, definition)).getEntries().get(0).getRelation());

        QuestProgressSnapshot ahead = base(player, quest, QuestStatus.IN_PROGRESS, 3L);
        BqShadowDifferenceReport report = analyzer.compare(Collections.singletonList(player(player, bq)),
            Collections.singletonList(ahead), Collections.singletonMap(quest, definition));
        assertEquals(BqShadowRelation.BASE_AHEAD, report.getEntries().get(0).getRelation());
        assertTrue(report.getEntries().get(0).getReasons().get(0).contains("task-values"));
    }

    @Test
    public void reportsMissingDefinitionAndOneSidedProgress() {
        UUID player = UUID.randomUUID();
        UUID quest = UUID.randomUUID();
        BqShadowProgressAnalyzer analyzer = new BqShadowProgressAnalyzer();
        BqShadowDifferenceReport onlyBq = analyzer.compare(
            Collections.singletonList(player(player, imported(quest, false, 1L))),
            Collections.<QuestProgressSnapshot>emptyList(), Collections.<UUID, QuestDefinition>emptyMap());
        assertEquals(BqShadowRelation.BQ_ONLY, onlyBq.getEntries().get(0).getRelation());
        BqShadowDifferenceReport incomparable = analyzer.compare(
            Collections.singletonList(player(player, imported(quest, false, 1L))),
            Collections.singletonList(base(player, quest, QuestStatus.IN_PROGRESS, 1L)),
            Collections.<UUID, QuestDefinition>emptyMap());
        assertEquals(BqShadowRelation.INCOMPARABLE, incomparable.getEntries().get(0).getRelation());
    }

    @Test
    public void rejectsGroupProgressBecausePlayerShadowHasNoSafeMapping() {
        UUID group = UUID.randomUUID();
        UUID quest = UUID.randomUUID();
        QuestProgressSnapshot party = new QuestProgressSnapshot(ParticipantId.party(group), quest, 1,
            QuestStatus.IN_PROGRESS, Collections.<String, TaskProgress>emptyMap(), 0L, 0);
        try {
            new BqShadowProgressAnalyzer().compare(Collections.<BqImportedPlayerProgress>emptyList(),
                Collections.singletonList(party), Collections.<UUID, QuestDefinition>emptyMap());
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("PLAYER"));
            return;
        }
        throw new AssertionError("group progress must not be compared as a player");
    }

    @Test
    public void marksTaskShapeOrTypeMismatchIncomparable() {
        UUID player = UUID.randomUUID();
        UUID quest = UUID.randomUUID();
        QuestDefinition definition = definition(quest);
        BqImportedQuestProgress bq = new BqImportedQuestProgress(quest, false, false, 0L,
            Collections.singletonList(new BqImportedTaskProgress("task-0", "bq_standard:fluid", false,
                Arrays.asList(1L, 2L), "{}")), "{}");
        QuestProgressSnapshot base = base(player, quest, QuestStatus.IN_PROGRESS, 1L);
        BqShadowDifferenceReport report = new BqShadowProgressAnalyzer().compare(
            Collections.singletonList(player(player, bq)), Collections.singletonList(base),
            Collections.singletonMap(quest, definition));
        assertEquals(BqShadowRelation.INCOMPARABLE, report.getEntries().get(0).getRelation());
        assertTrue(report.getEntries().get(0).getReasons().toString().contains("task-shape"));
    }

    private static QuestDefinition definition(UUID id) {
        TaskDefinition task = new TaskDefinition("task-0", "bq_standard:retrieval", false,
            map("target", "3"));
        return new QuestDefinition(id, 1, "Q", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(task), Collections.emptyList(),
            com.jsirgalaxybase.quest.core.RepeatPolicy.never(),
            com.jsirgalaxybase.quest.core.QuestBehavior.DEFAULT);
    }

    private static QuestProgressSnapshot base(UUID player, UUID quest, QuestStatus status, long value) {
        LinkedHashMap<String, TaskProgress> tasks = new LinkedHashMap<String, TaskProgress>();
        tasks.put("task-0", new TaskProgress("task-0", value, 3L, value >= 3L));
        return new QuestProgressSnapshot(player, quest, 1, status, tasks, 0L);
    }

    private static BqImportedQuestProgress imported(UUID quest, boolean complete, long value) {
        return new BqImportedQuestProgress(quest, complete, false, 0L,
            Collections.singletonList(new BqImportedTaskProgress("task-0", "bq_standard:retrieval", complete,
                Collections.singletonList(value), "{}")), "{}");
    }

    private static BqImportedPlayerProgress player(UUID id, BqImportedQuestProgress quest) {
        return new BqImportedPlayerProgress(id, Paths.get(id + ".json"), Collections.singletonList(quest));
    }

    private static java.util.Map<String, String> map(String key, String value) {
        java.util.Map<String, String> result = new LinkedHashMap<String, String>();
        result.put(key, value);
        return result;
    }
}
