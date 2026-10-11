package com.jsirgalaxybase.quest.core;

import java.util.Optional;
import java.util.UUID;

/** Persists only active Quest Detect batches; per-task submission rows remain the durable operation journal. */
public interface ConsumptionDetectBatchRepository {
    /** Finds the active operation regardless of version; publication must not orphan an already-started batch. */
    Optional<ConsumptionDetectBatch> findActive(UUID playerId, UUID questId);

    default Optional<ConsumptionDetectBatch> findActive(UUID playerId, UUID questId, int definitionVersion) {
        Optional<ConsumptionDetectBatch> active = findActive(playerId, questId);
        return active.isPresent() && active.get().getDefinitionVersion() == definitionVersion
            ? active : Optional.<ConsumptionDetectBatch>empty();
    }

    /** Creates a batch or returns the already-active batch for this player/Quest, across versions and servers. */
    ConsumptionDetectBatch createIfAbsent(ConsumptionDetectBatch batch);

    /** Advances exactly one terminal task operation; an already-advanced cursor is returned idempotently. */
    ConsumptionDetectBatch advance(String batchId, int expectedTaskIndex, long updatedAt);

    /** Deletes a fully advanced batch. Must participate in the caller's transaction with Quest settlement. */
    void deleteCompleted(String batchId, int expectedTaskCount, long updatedAt);
}
