package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class CommandRewardDeliveryHandlerTest {
    @Test public void substitutesLiteralVariablesAndPassesStableEntitlementToAllowlistedExecutor() {
        final CommandRewardInvocation[] captured = new CommandRewardInvocation[1];
        CommandRewardDeliveryHandler handler = new CommandRewardDeliveryHandler(
            new AllowlistedCommandRewardPolicy(Collections.singleton("galaxyreward")), invocation -> {
                captured[0] = invocation;
                return RewardDeliveryOutcome.delivered();
            }, participant -> "Alex$1");
        RewardDeliveryOutcome result = handler.deliver(lease("entitlement-1",
            map("command", "/galaxyreward grant VAR_NAME VAR_UUID", "viaPlayer", "1")));
        assertTrue(result.isDelivered());
        assertEquals("entitlement-1", captured[0].getEntitlementKey());
        assertEquals("galaxyreward grant Alex$1 " + captured[0].getParticipantId().getId(), captured[0].getCommand());
        assertTrue(captured[0].isViaPlayer());
    }

    @Test public void arbitraryAndControlCharacterCommandsArePermanentlyRejectedWithoutExecution() {
        final int[] calls = { 0 };
        CommandRewardDeliveryHandler handler = new CommandRewardDeliveryHandler(
            new AllowlistedCommandRewardPolicy(Collections.singleton("give")), invocation -> {
                calls[0]++;
                return RewardDeliveryOutcome.delivered();
            }, participant -> "Alex");
        RewardDeliveryOutcome denied = handler.deliver(lease("e1", map("command", "op Alex")));
        RewardDeliveryOutcome multiline = handler.deliver(lease("e2", map("command", "give Alex x\ngive Alex y")));
        assertFalse(denied.isDelivered());
        assertFalse(denied.isRetryable());
        assertFalse(multiline.isDelivered());
        assertEquals(0, calls[0]);
    }

    private RewardDeliveryLease lease(String key, Map<String, String> parameters) {
        return new RewardDeliveryLease(key, ParticipantId.player(new UUID(1L, 2L)),
            new RewardDefinition("reward", "bq_standard:command", parameters), 1, "worker", 100L);
    }
    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }
}
