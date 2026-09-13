package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class QuestCompletionRewardDeliveryHandlerTest {
    @Test public void passesStableEntitlementParticipantAndTargetToTransactionalPort() {
        final Capture port = new Capture(QuestCompletionRewardPort.Result.COMPLETED);
        UUID target = new UUID(7L, 8L);
        RewardDeliveryOutcome outcome = new QuestCompletionRewardDeliveryHandler(port, () -> 1234L)
            .deliver(lease(target.toString()));
        assertTrue(outcome.isDelivered());
        assertEquals("entitlement", port.entitlement);
        assertEquals(new UUID(1L, 2L), port.participant.getId());
        assertEquals(target, port.target);
        assertEquals(1234L, port.time);
    }

    @Test public void alreadyCompletedIsIdempotentSuccess() {
        RewardDeliveryOutcome outcome = new QuestCompletionRewardDeliveryHandler(
            new Capture(QuestCompletionRewardPort.Result.ALREADY_COMPLETED), () -> 1L)
                .deliver(lease(new UUID(3L, 4L).toString()));
        assertTrue(outcome.isDelivered());
    }

    @Test public void malformedAndMissingTargetArePermanentFailures() {
        RewardDeliveryOutcome malformed = new QuestCompletionRewardDeliveryHandler(
            new Capture(QuestCompletionRewardPort.Result.COMPLETED), () -> 1L).deliver(lease("bad"));
        assertFalse(malformed.isDelivered());
        assertFalse(malformed.isRetryable());
        RewardDeliveryOutcome missing = new QuestCompletionRewardDeliveryHandler(
            new Capture(QuestCompletionRewardPort.Result.TARGET_NOT_FOUND), () -> 1L)
                .deliver(lease(new UUID(3L, 4L).toString()));
        assertFalse(missing.isDelivered());
        assertFalse(missing.isRetryable());
    }

    private static RewardDeliveryLease lease(String target) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("questUuid", target);
        return new RewardDeliveryLease("entitlement", ParticipantId.player(new UUID(1L, 2L)),
            new RewardDefinition("reward", "bq_standard:questcompletion", values), 1, "worker", 100L);
    }

    private static final class Capture implements QuestCompletionRewardPort {
        private final Result result;
        private String entitlement;
        private ParticipantId participant;
        private UUID target;
        private long time;
        private Capture(Result result) { this.result = result; }
        @Override public Result complete(String sourceEntitlementKey, ParticipantId participantId, UUID targetQuestId,
            long completedAt) {
            entitlement = sourceEntitlementKey;
            participant = participantId;
            target = targetQuestId;
            time = completedAt;
            return result;
        }
    }
}
