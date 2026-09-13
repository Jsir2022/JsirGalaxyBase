package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.UUID;

/** Authoritative chapter catalog ordering; all mutations require an active quest transaction. */
public interface QuestChapterCatalogRepository {
    void ensureCatalogEntry(UUID chapterId);
    List<QuestChapterCatalogEntry> listOrdered();
    void replaceOrder(List<UUID> orderedChapterIds);
}
