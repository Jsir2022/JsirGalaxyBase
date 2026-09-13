package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class QuestChapterDependencyGraphTest {
    @Test public void classifiesOnlyServerLoadedPrerequisitesAgainstChapterPlacement() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), external = UUID.randomUUID(), missing = UUID.randomUUID();
        QuestDefinition a = quest(first); QuestDefinition b = new QuestDefinition(second, 1, "B", "", QuestLogic.AND,
            QuestLogic.AND, new LinkedHashSet<UUID>(Arrays.asList(first, external, missing)), Arrays.<TaskDefinition>asList(), Arrays.<RewardDefinition>asList(),
            RepeatPolicy.never(), QuestBehavior.DEFAULT);
        Map<UUID, QuestDefinition> values = new LinkedHashMap<UUID, QuestDefinition>(); values.put(first, a); values.put(second, b); values.put(external, quest(external));
        QuestChapterDefinition chapter = new QuestChapterDefinition(UUID.randomUUID(), 1, "C", "", "", "", Arrays.asList(
            new QuestChapterEntry(first, 0, 0, 16, 16), new QuestChapterEntry(second, 32, 0, 16, 16)));
        QuestChapterDependencyGraph graph = QuestChapterDependencyGraph.build(chapter, values);
        assertEquals(3, graph.getEdges().size()); assertEquals(1, graph.getInternalEdges().size());
        assertEquals(QuestChapterDependencyGraph.EdgeKind.INTERNAL, graph.getEdges().get(0).getKind());
        assertEquals(QuestChapterDependencyGraph.EdgeKind.EXTERNAL, graph.getEdges().get(1).getKind());
        assertEquals(QuestChapterDependencyGraph.EdgeKind.MISSING, graph.getEdges().get(2).getKind());
        assertEquals(Integer.valueOf(3), graph.inboundCounts().get(second));
    }
    private static QuestDefinition quest(UUID id) { return new QuestDefinition(id, 1, "A", "", QuestLogic.AND, QuestLogic.AND,
        new LinkedHashSet<UUID>(), Arrays.<TaskDefinition>asList(), Arrays.<RewardDefinition>asList(), RepeatPolicy.never(), QuestBehavior.DEFAULT); }
}
