package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Combines only the currently active party members' snapshots in authoritative membership order. */
public final class BqPartyInventorySnapshotAggregator {
    public List<InventoryObservationEntry> aggregate(List<UUID> memberOrder,
        Map<UUID, List<InventoryObservationEntry>> activeSnapshots) {
        if (memberOrder == null || activeSnapshots == null) throw new IllegalArgumentException("party snapshots are required");
        List<InventoryObservationEntry> entries = new ArrayList<InventoryObservationEntry>();
        for (UUID memberId : memberOrder) {
            if (memberId == null) throw new IllegalArgumentException("party member id must not be null");
            List<InventoryObservationEntry> snapshot = activeSnapshots.get(memberId);
            if (snapshot != null) entries.addAll(snapshot);
        }
        return new InventoryObservationCanonicalizer().canonicalize(entries);
    }

    public List<InventoryObservationEntry> empty() {
        return Collections.emptyList();
    }
}
