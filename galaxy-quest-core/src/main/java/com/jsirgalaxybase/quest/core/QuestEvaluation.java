package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuestEvaluation {
    private final QuestProgressSnapshot progress;
    private final List<RewardEntitlement> entitlements;

    public QuestEvaluation(QuestProgressSnapshot progress, List<RewardEntitlement> entitlements) {
        this.progress = progress;
        this.entitlements = Collections.unmodifiableList(new ArrayList<RewardEntitlement>(entitlements));
    }

    public QuestProgressSnapshot getProgress() { return progress; }
    public List<RewardEntitlement> getEntitlements() { return entitlements; }
}
