package com.jsirgalaxybase.quest.core;
import java.util.List;
public interface QuestContentMigrationQuery {
    List<QuestContentMigrationSummary> listRecent(int limit);
    default java.util.Optional<QuestRuntimeHealthSummary> health() { return java.util.Optional.empty(); }
}
