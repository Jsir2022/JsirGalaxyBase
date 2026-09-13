package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** The player identity is supplied by the authenticated server session, never decoded from a client payload. */
public interface RewardChoiceSelectionRepository {
    RewardChoiceSelectionStatus select(String entitlementKey, UUID currentPlayerId, int choiceIndex, long selectedAt);
}
