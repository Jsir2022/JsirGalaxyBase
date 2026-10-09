package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** Server-authored view of players online on the local game server. */
public interface QuestOnlinePlayerDirectory {
    boolean isOnline(UUID playerId);
}
