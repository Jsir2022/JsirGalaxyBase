package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.UUID;

/** Copies selected tasks from one draft chapter back into the same chapter at an explicit anchor. */
public final class QuestChapterCloneRequest {
    private final UUID chapterId;
    private final int chapterVersion;
    private final String expectedChapterHash;
    private final QuestDefinitionCloneRequest sources;
    private final int anchorX;
    private final int anchorY;

    public QuestChapterCloneRequest(UUID chapterId, int chapterVersion, String expectedChapterHash,
        QuestDefinitionCloneRequest sources, int anchorX, int anchorY) {
        this.chapterId = Objects.requireNonNull(chapterId, "chapterId");
        if (chapterVersion < 1) throw new IllegalArgumentException("chapterVersion must be positive");
        this.chapterVersion = chapterVersion;
        this.expectedChapterHash = expectedChapterHash == null ? "" : expectedChapterHash.trim();
        if (this.expectedChapterHash.isEmpty()) throw new IllegalArgumentException("expectedChapterHash is required");
        this.sources = Objects.requireNonNull(sources, "sources");
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    public UUID getChapterId() { return chapterId; }
    public int getChapterVersion() { return chapterVersion; }
    public String getExpectedChapterHash() { return expectedChapterHash; }
    public QuestDefinitionCloneRequest getSources() { return sources; }
    public int getAnchorX() { return anchorX; }
    public int getAnchorY() { return anchorY; }
}
