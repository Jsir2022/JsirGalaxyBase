package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.*;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

public class BqProgressConflictAnalyzerTest {
    private final BqProgressConflictAnalyzer analyzer = new BqProgressConflictAnalyzer();

    @Test
    public void classifiesMonotonicAndDivergentProgressWithoutMerging() {
        UUID player = UUID.randomUUID();
        UUID quest = UUID.randomUUID();
        assertEquals(BqProgressRelation.IDENTICAL, analyzer.relation(quest(quest, false, false, 2, 3),
            quest(quest, false, false, 2, 3)));
        assertEquals(BqProgressRelation.LEFT_AHEAD, analyzer.relation(quest(quest, true, false, 4, 3),
            quest(quest, false, false, 2, 3)));
        assertEquals(BqProgressRelation.RIGHT_AHEAD, analyzer.relation(quest(quest, false, false, 2, 3),
            quest(quest, true, true, 4, 3)));
        assertEquals(BqProgressRelation.DIVERGENT, analyzer.relation(quest(quest, false, false, 4, 1),
            quest(quest, false, false, 2, 3)));

        BqProgressConflictReport report = analyzer.compare(
            Collections.singletonList(player(player, quest(quest, true, false, 4, 3))),
            Collections.singletonList(player(player, quest(quest, false, false, 2, 3))));
        assertEquals(1, report.count(BqProgressRelation.LEFT_AHEAD));
        assertEquals(player, report.getEntries().get(0).getPlayerId());
    }

    @Test
    public void reportsOneSidedPlayersAndQuestsExplicitly() {
        UUID leftPlayer = new UUID(0L, 1L);
        UUID rightPlayer = new UUID(0L, 2L);
        BqProgressConflictReport report = analyzer.compare(
            Collections.singletonList(player(leftPlayer, quest(UUID.randomUUID(), false, false, 1))),
            Collections.singletonList(player(rightPlayer, quest(UUID.randomUUID(), false, false, 1))));
        assertEquals(1, report.count(BqProgressRelation.LEFT_ONLY));
        assertEquals(1, report.count(BqProgressRelation.RIGHT_ONLY));
    }

    @Test
    public void differentBqCompletionTimestampsAreNotClassifiedAsIdentical() {
        UUID quest = UUID.randomUUID();
        BqImportedQuestProgress earlier = questWithTimestamp(quest, true, true, 1000L, 4L, 2L);
        BqImportedQuestProgress later = questWithTimestamp(quest, true, true, 2000L, 4L, 2L);

        assertEquals("BqQuestInstance timestamp can distinguish completion/claim and cooldown state",
            BqProgressRelation.DIVERGENT, analyzer.relation(earlier, later));
    }

    private static BqImportedPlayerProgress player(UUID player, BqImportedQuestProgress... quests) {
        return new BqImportedPlayerProgress(player, Paths.get(player + ".json"), Arrays.asList(quests));
    }

    private static BqImportedQuestProgress quest(UUID id, boolean complete, boolean claimed, long... values) {
        BqImportedTaskProgress task = new BqImportedTaskProgress("task", "bq_standard:retrieval", complete,
            longs(values), "{}");
        return new BqImportedQuestProgress(id, complete, claimed, 0L, Collections.singletonList(task), "{}");
    }

    private static BqImportedQuestProgress questWithTimestamp(UUID id, boolean complete, boolean claimed,
        long timestamp, long... values) {
        BqImportedTaskProgress task = new BqImportedTaskProgress("task", "bq_standard:retrieval", complete,
            longs(values), "{}");
        return new BqImportedQuestProgress(id, complete, claimed, timestamp, Collections.singletonList(task), "{}");
    }

    private static java.util.List<Long> longs(long... values) {
        java.util.List<Long> result = new java.util.ArrayList<Long>();
        for (long value : values) result.add(value);
        return result;
    }
}
