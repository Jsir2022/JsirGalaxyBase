package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** Builds a resource-free consumption intent for the player authenticated by the server. */
public final class AuthenticatedQuestConsumptionService {
    private final ConsumptionSubmissionService submissions;
    private final ConsumptionRequestAuthorizer authorizer;
    private final String sourceServer;

    public AuthenticatedQuestConsumptionService(ConsumptionSubmissionService submissions,
        ConsumptionRequestAuthorizer authorizer, String sourceServer) {
        if (submissions == null || authorizer == null) throw new IllegalArgumentException(
            "submission service and authorizer are required");
        if (sourceServer == null || sourceServer.trim().isEmpty()) throw new IllegalArgumentException(
            "sourceServer is required");
        this.submissions = submissions;
        this.authorizer = authorizer;
        this.sourceServer = sourceServer.trim();
    }

    public ConsumptionSubmission submit(UUID authenticatedPlayerId, UUID questId, String taskKey, long now) {
        return submit(authenticatedPlayerId, questId, taskKey, now, null);
    }

    /**
     * Submits one inventory stack selected from server-side player state. Selection must never originate from the
     * terminal payload; the Minecraft adapter captures its slot and fingerprint from the authenticated player.
     */
    public ConsumptionSubmission submit(UUID authenticatedPlayerId, UUID questId, String taskKey, long now,
        ConsumptionSelection selection) {
        if (authenticatedPlayerId == null) throw new IllegalArgumentException("authenticated player is required");
        if (questId == null) throw new IllegalArgumentException("questId is required");
        String normalizedTaskKey = taskKey == null ? "" : taskKey.trim();
        if (normalizedTaskKey.isEmpty()) throw new IllegalArgumentException("taskKey is required");
        ConsumptionIntent intent = new ConsumptionIntent(UUID.randomUUID().toString(), sourceServer,
            authenticatedPlayerId, questId, normalizedTaskKey, now, selection);
        return submissions.submit(intent, authorizer, now);
    }

    public java.util.List<ConsumptionSubmission> resumePendingForPlayer(UUID authenticatedPlayerId, long now) {
        if (authenticatedPlayerId == null) throw new IllegalArgumentException("authenticated player is required");
        return submissions.resumePendingForPlayer(authenticatedPlayerId, sourceServer, now);
    }
}
