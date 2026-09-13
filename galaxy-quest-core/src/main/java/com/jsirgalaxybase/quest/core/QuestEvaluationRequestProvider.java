package com.jsirgalaxybase.quest.core;

import java.util.List;

/** Called inside the quest transaction so membership, prerequisites and definitions form one consistent plan. */
public interface QuestEvaluationRequestProvider {
    List<QuestEvaluationRequest> requestsFor(GameplayFact fact);
}
