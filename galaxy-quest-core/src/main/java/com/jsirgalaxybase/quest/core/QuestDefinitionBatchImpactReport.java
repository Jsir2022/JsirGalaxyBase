package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable, bounded impact preview for retiring several published quests as one operation. */
public final class QuestDefinitionBatchImpactReport {
    public static final class Placement {
        private final UUID targetId;
        private final UUID chapterId;
        private final String chapterName;

        Placement(UUID targetId, UUID chapterId, String chapterName) {
            this.targetId = Objects.requireNonNull(targetId, "targetId");
            this.chapterId = Objects.requireNonNull(chapterId, "chapterId");
            this.chapterName = Objects.requireNonNull(chapterName, "chapterName");
        }

        public UUID getTargetId() { return targetId; }
        public UUID getChapterId() { return chapterId; }
        public String getChapterName() { return chapterName; }
    }

    private final List<UUID> targetIds;
    private final List<UUID> unpublishedTargetIds;
    private final List<QuestDefinitionImpactReport.Dependent> externalDependents;
    private final List<Placement> placements;
    private final boolean truncated;

    QuestDefinitionBatchImpactReport(List<UUID> targetIds, List<UUID> unpublishedTargetIds,
        List<QuestDefinitionImpactReport.Dependent> externalDependents, List<Placement> placements,
        boolean truncated) {
        this.targetIds = immutable(targetIds);
        this.unpublishedTargetIds = immutable(unpublishedTargetIds);
        this.externalDependents = Collections.unmodifiableList(
            new ArrayList<QuestDefinitionImpactReport.Dependent>(externalDependents));
        this.placements = Collections.unmodifiableList(new ArrayList<Placement>(placements));
        this.truncated = truncated;
    }

    public List<UUID> getTargetIds() { return targetIds; }
    public List<UUID> getUnpublishedTargetIds() { return unpublishedTargetIds; }
    public List<QuestDefinitionImpactReport.Dependent> getExternalDependents() { return externalDependents; }
    public List<Placement> getPlacements() { return placements; }
    public boolean isTruncated() { return truncated; }
    public boolean isSafeToRetire() {
        return !targetIds.isEmpty() && unpublishedTargetIds.isEmpty() && externalDependents.isEmpty()
            && placements.isEmpty() && !truncated;
    }

    private static List<UUID> immutable(List<UUID> values) {
        return Collections.unmodifiableList(new ArrayList<UUID>(values));
    }
}
