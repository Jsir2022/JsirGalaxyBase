package com.jsirgalaxybase.quest.core;

import java.util.OptionalLong;
import java.util.UUID;

/** Reads the authenticated player's vanilla lifetime crafted-item statistic for one registry item. */
public interface CraftingStatisticReader {
    OptionalLong craftedCount(UUID authenticatedPlayerId, String registryName);
}
