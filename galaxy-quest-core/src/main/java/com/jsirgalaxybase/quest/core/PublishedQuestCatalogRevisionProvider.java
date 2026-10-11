package com.jsirgalaxybase.quest.core;

/** Reads the authoritative, monotonically increasing revision of the published definition catalog. */
public interface PublishedQuestCatalogRevisionProvider {
    long currentRevision();
}
