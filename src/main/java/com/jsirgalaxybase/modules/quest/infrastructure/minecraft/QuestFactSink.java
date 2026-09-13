package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.quest.core.GameplayFact;

/** Server-side boundary; implementations resolve applicable published quests and persist the fact transactionally. */
public interface QuestFactSink {
    void accept(GameplayFact fact);
}
