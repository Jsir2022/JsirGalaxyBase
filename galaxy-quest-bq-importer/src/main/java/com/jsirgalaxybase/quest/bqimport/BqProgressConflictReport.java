package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BqProgressConflictReport {
    private final List<BqProgressConflict> entries;
    private final Map<BqProgressRelation, Integer> counts;

    BqProgressConflictReport(List<BqProgressConflict> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<BqProgressConflict>(entries));
        EnumMap<BqProgressRelation, Integer> totals = new EnumMap<BqProgressRelation, Integer>(BqProgressRelation.class);
        for (BqProgressRelation relation : BqProgressRelation.values()) totals.put(relation, 0);
        for (BqProgressConflict entry : entries) totals.put(entry.getRelation(), totals.get(entry.getRelation()) + 1);
        this.counts = Collections.unmodifiableMap(totals);
    }

    public List<BqProgressConflict> getEntries() { return entries; }
    public int count(BqProgressRelation relation) { return counts.get(relation); }
    public Map<BqProgressRelation, Integer> getCounts() { return counts; }
}
