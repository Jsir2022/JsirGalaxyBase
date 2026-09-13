package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Immutable presentation grouping for quests; it never owns runtime progress. */
public final class QuestChapterDefinition {
    private final UUID id;
    private final int version;
    private final String name;
    private final String description;
    private final String iconReference;
    private final String backgroundReference;
    private final int backgroundSize;
    private final QuestVisibility visibility;
    private final List<QuestChapterEntry> entries;

    public QuestChapterDefinition(UUID id, int version, String name, String description, String iconReference,
        String backgroundReference, List<QuestChapterEntry> entries) {
        this(id, version, name, description, iconReference, backgroundReference, 256, QuestVisibility.NORMAL, entries);
    }

    public QuestChapterDefinition(UUID id, int version, String name, String description, String iconReference,
        String backgroundReference, int backgroundSize, QuestVisibility visibility, List<QuestChapterEntry> entries) {
        this.id = Objects.requireNonNull(id, "id");
        if (version < 1) throw new IllegalArgumentException("version must be positive");
        this.version = version;
        this.name = requireText(name, "name");
        this.description = description == null ? "" : description;
        this.iconReference = iconReference == null ? "" : iconReference;
        this.backgroundReference = backgroundReference == null ? "" : backgroundReference;
        if (backgroundSize < 1) throw new IllegalArgumentException("backgroundSize must be positive");
        this.backgroundSize = backgroundSize;
        this.visibility = Objects.requireNonNull(visibility, "visibility");
        List<QuestChapterEntry> copy = new ArrayList<QuestChapterEntry>(entries == null
            ? Collections.<QuestChapterEntry>emptyList() : entries);
        Set<UUID> questIds = new HashSet<UUID>();
        for (QuestChapterEntry entry : copy) {
            if (entry == null) throw new NullPointerException("entry");
            if (!questIds.add(entry.getQuestId())) throw new IllegalArgumentException(
                "duplicate quest placement: " + entry.getQuestId());
        }
        this.entries = Collections.unmodifiableList(copy);
    }

    public UUID getId() { return id; }
    public int getVersion() { return version; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIconReference() { return iconReference; }
    public String getBackgroundReference() { return backgroundReference; }
    public int getBackgroundSize() { return backgroundSize; }
    public QuestVisibility getVisibility() { return visibility; }
    public List<QuestChapterEntry> getEntries() { return entries; }

    public QuestChapterEntry entryAt(int x, int y) {
        for (int i = entries.size() - 1; i >= 0; i--) if (entries.get(i).contains(x, y)) return entries.get(i);
        return null;
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
