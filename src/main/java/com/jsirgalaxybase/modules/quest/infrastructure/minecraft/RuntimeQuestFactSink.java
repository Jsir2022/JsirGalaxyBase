package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.quest.core.FactProcessingResult;
import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.QuestClock;
import com.jsirgalaxybase.quest.core.QuestEvaluationRequestProvider;
import com.jsirgalaxybase.quest.core.QuestRuntimeService;

public final class RuntimeQuestFactSink implements QuestFactSink {
    private final QuestRuntimeService runtime;
    private final QuestEvaluationRequestProvider plans;
    private final QuestClock clock;

    public RuntimeQuestFactSink(QuestRuntimeService runtime, QuestEvaluationRequestProvider plans, QuestClock clock) {
        if (runtime == null || plans == null || clock == null) throw new IllegalArgumentException(
            "runtime, plans and clock are required");
        this.runtime = runtime;
        this.plans = plans;
        this.clock = clock;
    }

    @Override
    public void accept(GameplayFact fact) {
        FactProcessingResult result = runtime.apply(fact, plans, clock.currentTimeMillis());
        // ACCEPTED, DUPLICATE and IGNORED are all terminal for this immutable fact.
        if (result == null) throw new IllegalStateException("quest runtime returned no fact result");
    }
}
