package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Bounded client intent. Definitions, relationships and safety decisions are never accepted from the client. */
public final class QuestDefinitionBatchRetirementRequest {
    public static final class Target {
        private final UUID questId;
        private final int version;
        public Target(UUID questId, int version) {
            if (questId == null || version < 1) throw new IllegalArgumentException("quest and version are required");
            this.questId = questId; this.version = version;
        }
        public UUID getQuestId() { return questId; }
        public int getVersion() { return version; }
    }

    private final List<Target> targets;
    public QuestDefinitionBatchRetirementRequest(List<Target> targets) {
        if (targets == null || targets.isEmpty() || targets.size() > 128)
            throw new IllegalArgumentException("1 to 128 targets are required");
        Set<UUID> ids = new LinkedHashSet<UUID>();
        for (Target target : targets) {
            if (target == null || !ids.add(target.getQuestId()))
                throw new IllegalArgumentException("targets must be non-null and unique");
        }
        this.targets = Collections.unmodifiableList(new ArrayList<Target>(targets));
    }
    public List<Target> getTargets() { return targets; }
}
