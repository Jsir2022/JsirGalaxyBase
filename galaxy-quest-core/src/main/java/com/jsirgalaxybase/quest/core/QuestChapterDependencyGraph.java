package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only dependency projection for one chapter canvas.
 * Only server-loaded definitions participate; it never accepts a client-supplied edge list.
 */
public final class QuestChapterDependencyGraph {
    public enum EdgeKind { INTERNAL, EXTERNAL, MISSING }
    public static final class Edge {
        private final UUID prerequisiteId, questId;
        private final EdgeKind kind;
        private Edge(UUID prerequisiteId, UUID questId, EdgeKind kind) {
            this.prerequisiteId = prerequisiteId; this.questId = questId; this.kind = kind;
        }
        public UUID getPrerequisiteId() { return prerequisiteId; }
        public UUID getQuestId() { return questId; }
        public EdgeKind getKind() { return kind; }
    }
    private final List<Edge> edges;
    private QuestChapterDependencyGraph(List<Edge> edges) { this.edges = Collections.unmodifiableList(edges); }

    public static QuestChapterDependencyGraph build(QuestChapterDefinition chapter,
        Map<UUID, QuestDefinition> definitions) {
        if (chapter == null || definitions == null) throw new IllegalArgumentException("chapter and definitions are required");
        Set<UUID> placed = new LinkedHashSet<UUID>();
        for (QuestChapterEntry entry : chapter.getEntries()) placed.add(entry.getQuestId());
        List<Edge> result = new ArrayList<Edge>();
        for (QuestChapterEntry entry : chapter.getEntries()) {
            QuestDefinition definition = definitions.get(entry.getQuestId());
            if (definition == null) continue;
            for (UUID prerequisite : definition.getPrerequisites()) result.add(new Edge(prerequisite, entry.getQuestId(),
                !definitions.containsKey(prerequisite) ? EdgeKind.MISSING
                    : placed.contains(prerequisite) ? EdgeKind.INTERNAL : EdgeKind.EXTERNAL));
        }
        return new QuestChapterDependencyGraph(result);
    }
    public List<Edge> getEdges() { return edges; }
    public List<Edge> getInternalEdges() {
        List<Edge> result = new ArrayList<Edge>(); for (Edge edge : edges) if (edge.kind == EdgeKind.INTERNAL) result.add(edge);
        return Collections.unmodifiableList(result);
    }
    public Map<UUID, Integer> inboundCounts() {
        Map<UUID, Integer> result = new LinkedHashMap<UUID, Integer>();
        for (Edge edge : edges) { Integer count = result.get(edge.questId); result.put(edge.questId, Integer.valueOf(count == null ? 1 : count.intValue() + 1)); }
        return Collections.unmodifiableMap(result);
    }
}
