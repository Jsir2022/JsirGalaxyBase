package com.jsirgalaxybase.quest.core;

/** Explicit compatibility boundary between historical BQ party semantics and Base-only personal imports. */
public final class BqImportedTaskScope {
    public static final String PARTY_SHARE = "bq.partyShare";

    private BqImportedTaskScope() { }

    public static boolean isImported(TaskDefinition task) {
        return task != null && task.getParameters().containsKey("bq.raw");
    }

    /** Missing markers retain the historical compatibility behavior for already-published definitions. */
    public static boolean sharesParty(TaskDefinition task) {
        return task != null && QuestBooleanParameter.parse(task.getParameters().get(PARTY_SHARE), true);
    }
}
