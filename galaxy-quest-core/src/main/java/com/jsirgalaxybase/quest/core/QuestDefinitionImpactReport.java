package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable, bounded preview of the published content affected by changing one quest. */
public final class QuestDefinitionImpactReport {
    public static final class Dependent {
        public enum ReferenceKind { PREREQUISITE, TASK_REFERENCE, REWARD_REFERENCE }
        private final UUID questId;
        private final String name;
        private final int distance;
        private final ReferenceKind referenceKind;

        Dependent(UUID questId, String name, int distance, ReferenceKind referenceKind) {
            this.questId = Objects.requireNonNull(questId, "questId");
            this.name = Objects.requireNonNull(name, "name");
            this.distance = distance;
            this.referenceKind = Objects.requireNonNull(referenceKind, "referenceKind");
        }

        public UUID getQuestId() { return questId; }
        public String getName() { return name; }
        public int getDistance() { return distance; }
        public ReferenceKind getReferenceKind() { return referenceKind; }
        public boolean isDirect() { return distance == 1; }
    }

    public static final class Placement {
        private final UUID chapterId;
        private final String chapterName;

        Placement(UUID chapterId, String chapterName) {
            this.chapterId = Objects.requireNonNull(chapterId, "chapterId");
            this.chapterName = Objects.requireNonNull(chapterName, "chapterName");
        }

        public UUID getChapterId() { return chapterId; }
        public String getChapterName() { return chapterName; }
    }

    private final UUID targetId;
    private final boolean published;
    private final List<Dependent> dependents;
    private final List<Placement> placements;
    private final boolean truncated;

    QuestDefinitionImpactReport(UUID targetId, boolean published, List<Dependent> dependents,
        List<Placement> placements, boolean truncated) {
        this.targetId = Objects.requireNonNull(targetId, "targetId");
        this.published = published;
        this.dependents = Collections.unmodifiableList(new ArrayList<Dependent>(dependents));
        this.placements = Collections.unmodifiableList(new ArrayList<Placement>(placements));
        this.truncated = truncated;
    }

    public UUID getTargetId() { return targetId; }
    public boolean isPublished() { return published; }
    public List<Dependent> getDependents() { return dependents; }
    public List<Placement> getPlacements() { return placements; }
    public boolean isTruncated() { return truncated; }
    public boolean isSafeToRetire() { return published && dependents.isEmpty() && placements.isEmpty() && !truncated; }
}
