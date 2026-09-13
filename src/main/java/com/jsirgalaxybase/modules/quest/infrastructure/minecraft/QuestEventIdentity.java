package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Unique for one server process; the generated ID is retained by the immutable fact during retries. */
public final class QuestEventIdentity {
    private final String bootId;
    private final AtomicLong sequence = new AtomicLong();

    public QuestEventIdentity() { this(UUID.randomUUID().toString()); }

    QuestEventIdentity(String bootId) {
        if (bootId == null || bootId.trim().isEmpty()) throw new IllegalArgumentException("bootId must not be blank");
        this.bootId = bootId;
    }

    public String next(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) throw new IllegalArgumentException("eventType must not be blank");
        return bootId + ":" + eventType + ":" + sequence.incrementAndGet();
    }
}
