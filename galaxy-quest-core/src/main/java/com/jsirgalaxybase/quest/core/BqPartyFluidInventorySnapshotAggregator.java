package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Combines fluid observations from only currently active party member leases. */
public final class BqPartyFluidInventorySnapshotAggregator {
    public List<FluidObservationEntry> aggregate(List<UUID> memberOrder,
        Map<UUID, List<FluidObservationEntry>> activeSnapshots) {
        if (memberOrder == null || activeSnapshots == null) {
            throw new IllegalArgumentException("party fluid snapshots are required");
        }
        List<FluidObservationEntry> entries = new ArrayList<FluidObservationEntry>();
        for (UUID memberId : memberOrder) {
            if (memberId == null) throw new IllegalArgumentException("party member id must not be null");
            List<FluidObservationEntry> snapshot = activeSnapshots.get(memberId);
            if (snapshot != null) entries.addAll(snapshot);
        }
        return new FluidObservationCanonicalizer().canonicalize(entries);
    }

    public List<FluidObservationEntry> empty() { return Collections.emptyList(); }
}
