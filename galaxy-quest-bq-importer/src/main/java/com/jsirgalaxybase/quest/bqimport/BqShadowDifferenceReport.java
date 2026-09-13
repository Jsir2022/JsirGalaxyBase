package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Immutable report produced by the read-only BQ shadow comparison. */
public final class BqShadowDifferenceReport {
    private final List<BqShadowDifference> entries;
    private final Map<BqShadowRelation, Integer> counts;
    private final boolean truncated;

    BqShadowDifferenceReport(List<BqShadowDifference> entries, boolean truncated) {
        this.entries = Collections.unmodifiableList(new ArrayList<BqShadowDifference>(entries));
        EnumMap<BqShadowRelation, Integer> totals = new EnumMap<BqShadowRelation, Integer>(BqShadowRelation.class);
        for (BqShadowRelation relation : BqShadowRelation.values()) totals.put(relation, 0);
        for (BqShadowDifference entry : entries) totals.put(entry.getRelation(), totals.get(entry.getRelation()) + 1);
        this.counts = Collections.unmodifiableMap(totals);
        this.truncated = truncated;
    }

    public List<BqShadowDifference> getEntries() { return entries; }
    public int count(BqShadowRelation relation) { return counts.get(relation); }
    public Map<BqShadowRelation, Integer> getCounts() { return counts; }
    public boolean isTruncated() { return truncated; }

    public String toText() {
        return "entries=" + entries.size() + "\ncounts=" + counts + "\ntruncated=" + truncated + "\n";
    }
}
