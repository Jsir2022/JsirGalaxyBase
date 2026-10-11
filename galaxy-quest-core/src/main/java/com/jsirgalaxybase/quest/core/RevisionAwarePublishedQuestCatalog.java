package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caches decoded published definitions while checking a cheap authoritative revision on each lookup.
 * A failed revision read is deliberately propagated: callers must not plan a new fact from stale definitions.
 */
public final class RevisionAwarePublishedQuestCatalog implements PublishedQuestCatalog {
    private final PublishedQuestCatalog source;
    private final PublishedQuestCatalogRevisionProvider revisions;
    private volatile Snapshot snapshot;

    public RevisionAwarePublishedQuestCatalog(PublishedQuestCatalog source,
        PublishedQuestCatalogRevisionProvider revisions) {
        if (source == null || revisions == null) throw new IllegalArgumentException("catalog and revision reader are required");
        this.source = source;
        this.revisions = revisions;
    }

    @Override
    public List<StoredQuestDefinition> findAllPublished() {
        long revision = revisions.currentRevision();
        Snapshot current = snapshot;
        if (current != null && current.revision == revision) return current.definitions;
        synchronized (this) {
            // Re-read after acquiring the lock so a delayed caller cannot overwrite a newer snapshot.
            revision = revisions.currentRevision();
            current = snapshot;
            if (current != null && current.revision == revision) return current.definitions;
            List<StoredQuestDefinition> loaded = source.findAllPublished();
            if (loaded == null) throw new IllegalStateException("published quest catalog returned null");
            List<StoredQuestDefinition> immutable = Collections.unmodifiableList(
                new ArrayList<StoredQuestDefinition>(loaded));
            snapshot = new Snapshot(revision, immutable);
            return immutable;
        }
    }

    @Override
    public Optional<StoredQuestDefinition> findPublished(UUID questId, int definitionVersion) {
        if (questId == null || definitionVersion < 1) return Optional.empty();
        // Historical versions are intentionally fetched from the authoritative source rather than the latest-only
        // catalog cache, so a durable Detect batch can finish with the exact definition it originally pinned.
        Optional<StoredQuestDefinition> stored = source.findPublished(questId, definitionVersion);
        if (!stored.isPresent() || stored.get().getLifecycle() != QuestDefinitionLifecycle.PUBLISHED
            || !questId.equals(stored.get().getDefinition().getId())
            || definitionVersion != stored.get().getDefinition().getVersion()) return Optional.empty();
        return stored;
    }

    private static final class Snapshot {
        private final long revision;
        private final List<StoredQuestDefinition> definitions;

        private Snapshot(long revision, List<StoredQuestDefinition> definitions) {
            this.revision = revision;
            this.definitions = definitions;
        }
    }
}
