package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Server command input for cloning existing immutable quest versions. */
public final class QuestDefinitionCloneRequest {
    public static final class Source {
        private final UUID questId;
        private final int version;
        public Source(UUID questId, int version) {
            this.questId = Objects.requireNonNull(questId, "questId");
            if (version < 1) throw new IllegalArgumentException("version must be positive");
            this.version = version;
        }
        public UUID getQuestId() { return questId; }
        public int getVersion() { return version; }
        @Override public boolean equals(Object other) { return other instanceof Source
            && questId.equals(((Source)other).questId) && version == ((Source)other).version; }
        @Override public int hashCode() { return 31 * questId.hashCode() + version; }
    }

    private final List<Source> sources;
    public QuestDefinitionCloneRequest(List<Source> sources) {
        if (sources == null || sources.isEmpty() || sources.size() > 128) {
            throw new IllegalArgumentException("clone request requires 1 to 128 sources");
        }
        Set<Source> unique = new LinkedHashSet<Source>();
        for (Source source : sources) if (!unique.add(Objects.requireNonNull(source, "source"))) {
            throw new IllegalArgumentException("duplicate clone source");
        }
        this.sources = Collections.unmodifiableList(new ArrayList<Source>(unique));
    }
    public List<Source> getSources() { return sources; }
}
