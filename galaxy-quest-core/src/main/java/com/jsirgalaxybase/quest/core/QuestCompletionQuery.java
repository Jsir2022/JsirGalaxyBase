package com.jsirgalaxybase.quest.core;

import java.util.Set;
import java.util.UUID;

public interface QuestCompletionQuery {
    Set<UUID> findCompletedQuestIds(ParticipantId participantId);
}
