package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Result of the single-transaction quest and chapter clone command. */
public final class QuestChapterCloneResult {
    public enum Status { SUCCESS, FORBIDDEN, NOT_FOUND, INVALID, CONFLICT }
    private final Status status;
    private final StoredQuestChapter chapter;
    private final Map<UUID, UUID> remappedIds;
    private QuestChapterCloneResult(Status status, StoredQuestChapter chapter, Map<UUID, UUID> remappedIds) {
        this.status = status; this.chapter = chapter;
        this.remappedIds = remappedIds == null ? Collections.<UUID, UUID>emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<UUID, UUID>(remappedIds));
    }
    public static QuestChapterCloneResult success(StoredQuestChapter chapter, Map<UUID, UUID> ids) {
        return new QuestChapterCloneResult(Status.SUCCESS, chapter, ids);
    }
    public static QuestChapterCloneResult status(Status status) { return new QuestChapterCloneResult(status, null, null); }
    public Status getStatus() { return status; }
    public StoredQuestChapter getChapter() { return chapter; }
    public Map<UUID, UUID> getRemappedIds() { return remappedIds; }
    public boolean isSuccess() { return status == Status.SUCCESS; }
}
