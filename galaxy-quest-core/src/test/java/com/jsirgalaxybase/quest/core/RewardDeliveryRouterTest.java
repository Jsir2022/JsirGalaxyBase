package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

public class RewardDeliveryRouterTest {
    @Test
    public void routesByExactTypeAndPreservesStableLease() {
        final RewardDeliveryLease[] seen = new RewardDeliveryLease[1];
        RewardDeliveryOutcome delivered = RewardDeliveryOutcome.delivered();
        RewardDeliveryRouter router = new RewardDeliveryRouter(Arrays.asList(handler("bq_standard:item", lease -> {
            seen[0] = lease;
            return delivered;
        })));
        RewardDeliveryLease lease = lease("bq_standard:item");
        assertSame(delivered, router.deliver(lease));
        assertSame(lease, seen[0]);
    }

    @Test
    public void unknownTypeIsNonRetryableAndNeverSilentlyClaimed() {
        RewardDeliveryOutcome outcome = new RewardDeliveryRouter(Collections.<RewardDeliveryHandler>emptyList())
            .deliver(lease("addon:unknown"));
        assertFalse(outcome.isDelivered());
        assertFalse(outcome.isRetryable());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateHandlerRegistrationFailsFast() {
        new RewardDeliveryRouter(Arrays.asList(handler("bq_standard:item", lease -> RewardDeliveryOutcome.delivered()),
            handler("bq_standard:item", lease -> RewardDeliveryOutcome.delivered())));
    }

    private RewardDeliveryHandler handler(final String type, final Delivery delivery) {
        return new RewardDeliveryHandler() {
            @Override public String getTypeId() { return type; }
            @Override public RewardDeliveryOutcome deliver(RewardDeliveryLease lease) { return delivery.run(lease); }
        };
    }

    private RewardDeliveryLease lease(String type) {
        return new RewardDeliveryLease("entitlement", ParticipantId.player(new UUID(1L, 2L)),
            new RewardDefinition("reward", type, Collections.<String, String>emptyMap()), 1, "worker", 100L);
    }

    private interface Delivery { RewardDeliveryOutcome run(RewardDeliveryLease lease); }
}
