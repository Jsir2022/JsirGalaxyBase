package com.jsirgalaxybase.quest.core;

/** Authorization must be evaluated from server-authenticated state, never client flags. */
public interface QuestEditorAuthorization {
    boolean canManageDefinitions(QuestEditorActor actor);
}
