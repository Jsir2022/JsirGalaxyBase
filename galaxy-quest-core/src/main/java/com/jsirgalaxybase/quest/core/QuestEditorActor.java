package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.UUID;

/** Authenticated server-side identity used for quest definition administration. */
public final class QuestEditorActor {
    private final UUID id;
    private final String displayName;

    public QuestEditorActor(UUID id, String displayName) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = displayName == null ? "" : displayName.trim();
    }

    public UUID getId() { return id; }
    public String getDisplayName() { return displayName; }
}
