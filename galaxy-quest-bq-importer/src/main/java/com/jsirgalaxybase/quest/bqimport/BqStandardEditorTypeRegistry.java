package com.jsirgalaxybase.quest.bqimport;

import com.jsirgalaxybase.quest.core.QuestEditorTypeRegistry;
import com.jsirgalaxybase.quest.core.StandardQuestEditorTypeRegistry;

/** Compatibility facade for importer callers; the production schema lives in pure quest core. */
public final class BqStandardEditorTypeRegistry {
    private BqStandardEditorTypeRegistry() {}
    public static QuestEditorTypeRegistry create() { return StandardQuestEditorTypeRegistry.create(); }
}
