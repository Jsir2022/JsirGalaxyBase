package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Semantic chapter editor; layout UIs never mutate the published chapter object directly. */
public final class QuestChapterDraftEditor {
    private final UUID id;
    private int version;
    private String name;
    private String description;
    private String iconReference;
    private String backgroundReference;
    private int backgroundSize;
    private QuestVisibility visibility;
    private final List<QuestChapterEntry> entries;

    private QuestChapterDraftEditor(QuestChapterDefinition source, int version) {
        id = source.getId();
        this.version = version;
        name = source.getName();
        description = source.getDescription();
        iconReference = source.getIconReference();
        backgroundReference = source.getBackgroundReference();
        backgroundSize = source.getBackgroundSize();
        visibility = source.getVisibility();
        entries = new ArrayList<QuestChapterEntry>(source.getEntries());
    }

    public static QuestChapterDraftEditor edit(QuestChapterDefinition source) {
        Objects.requireNonNull(source, "source");
        return new QuestChapterDraftEditor(source, source.getVersion());
    }

    public static QuestChapterDraftEditor nextVersion(QuestChapterDefinition source) {
        Objects.requireNonNull(source, "source");
        if (source.getVersion() == Integer.MAX_VALUE) throw new IllegalArgumentException("chapter version overflow");
        return new QuestChapterDraftEditor(source, source.getVersion() + 1);
    }

    public QuestChapterDraftEditor name(String value) {
        String normalized = Objects.requireNonNull(value, "name").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("name must not be blank");
        name = normalized;
        return this;
    }

    public QuestChapterDraftEditor description(String value) { description = value == null ? "" : value; return this; }
    public QuestChapterDraftEditor iconReference(String value) { iconReference = value == null ? "" : value; return this; }
    public QuestChapterDraftEditor backgroundReference(String value) {
        backgroundReference = value == null ? "" : value;
        return this;
    }
    public QuestChapterDraftEditor backgroundSize(int value) {
        if (value < 1) throw new IllegalArgumentException("backgroundSize must be positive");
        backgroundSize = value;
        return this;
    }
    public QuestChapterDraftEditor visibility(QuestVisibility value) {
        visibility = Objects.requireNonNull(value, "visibility");
        return this;
    }

    public QuestChapterDraftEditor place(QuestChapterEntry entry) {
        QuestChapterEntry value = Objects.requireNonNull(entry, "entry");
        if (index(value.getQuestId()) >= 0) throw new IllegalArgumentException(
            "duplicate quest placement: " + value.getQuestId());
        entries.add(value);
        return this;
    }

    public QuestChapterDraftEditor move(UUID questId, int x, int y) {
        int index = requireIndex(questId);
        QuestChapterEntry old = entries.get(index);
        entries.set(index, new QuestChapterEntry(old.getQuestId(), x, y, old.getWidth(), old.getHeight()));
        return this;
    }

    public QuestChapterDraftEditor resize(UUID questId, int width, int height) {
        int index = requireIndex(questId);
        QuestChapterEntry old = entries.get(index);
        entries.set(index, new QuestChapterEntry(old.getQuestId(), old.getX(), old.getY(), width, height));
        return this;
    }

    public QuestChapterDraftEditor remove(UUID questId) {
        entries.remove(requireIndex(questId));
        return this;
    }

    public QuestChapterDefinition build() {
        return new QuestChapterDefinition(id, version, name, description, iconReference, backgroundReference,
            backgroundSize, visibility, entries);
    }

    private int requireIndex(UUID questId) {
        int found = index(Objects.requireNonNull(questId, "questId"));
        if (found < 0) throw new IllegalArgumentException("quest is not placed: " + questId);
        return found;
    }

    private int index(UUID questId) {
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).getQuestId().equals(questId)) return i;
        return -1;
    }
}
