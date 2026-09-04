package com.jsirgalaxybase.modules.land.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.LandProtectionAction;
import com.jsirgalaxybase.modules.land.domain.LandProtectionDecision;
import com.jsirgalaxybase.modules.land.domain.LandProtectionMode;
import com.jsirgalaxybase.modules.land.domain.LandProtectionReason;
import com.jsirgalaxybase.modules.land.domain.LandTitleStatus;
import com.jsirgalaxybase.modules.land.domain.PersonalLandTitle;

public class LandProtectionRuntimeTest {

    @Test
    public void ownerAndUnclaimedChunksAreAllowedButOtherPlayerIsDenied() {
        LandChunkKey claimed = new LandChunkKey("lobby", 0, 1, 1);
        LandProtectionIndex index = indexWith(claimed);
        LandProtectionPolicy policy = new LandProtectionPolicy(index, false);

        assertEquals(LandProtectionReason.OWNER,
            policy.decide("owner", false, claimed, LandProtectionAction.BREAK_BLOCK).getReason());
        assertTrue(policy.decide("owner", false, claimed, LandProtectionAction.BREAK_BLOCK).isAllowed());
        assertFalse(policy.decide("other", false, claimed, LandProtectionAction.INTERACT_BLOCK).isAllowed());
        assertTrue(policy.decide("other", false, new LandChunkKey("lobby", 0, 2, 2),
            LandProtectionAction.PLACE_BLOCK).isAllowed());
    }

    @Test
    public void fakePlayersAreDeniedByDefaultAndCanBeEnabledExplicitly() {
        LandChunkKey claimed = new LandChunkKey("lobby", 0, 1, 1);
        LandProtectionIndex index = indexWith(claimed);

        assertEquals(LandProtectionReason.DENIED_FAKE_PLAYER,
            new LandProtectionPolicy(index, false)
                .decide("owner", true, claimed, LandProtectionAction.USE_ITEM).getReason());
        assertEquals(LandProtectionReason.FAKE_PLAYER_ALLOWED,
            new LandProtectionPolicy(index, true)
                .decide("automation", true, claimed, LandProtectionAction.USE_ITEM).getReason());
    }

    @Test
    public void shadowCountsDenialsWithoutCancellingWhileEnforceCancels() {
        LandChunkKey claimed = new LandChunkKey("lobby", 0, 1, 1);
        LandProtectionPolicy policy = new LandProtectionPolicy(indexWith(claimed), false);
        LandProtectionRuntime shadow = new LandProtectionRuntime(policy, LandProtectionMode.SHADOW);
        LandProtectionRuntime enforce = new LandProtectionRuntime(policy, LandProtectionMode.ENFORCE);

        LandProtectionDecision shadowDecision = shadow.evaluate("other", false, claimed,
            LandProtectionAction.ATTACK_ENTITY);
        LandProtectionDecision enforceDecision = enforce.evaluate(null, false, claimed,
            LandProtectionAction.EXPLOSION);

        assertFalse(shadow.shouldCancel(shadowDecision));
        assertTrue(enforce.shouldCancel(enforceDecision));
        assertEquals(Long.valueOf(1L), shadow.deniedCountSnapshot().get(LandProtectionAction.ATTACK_ENTITY));
        assertEquals(Long.valueOf(1L), enforce.deniedCountSnapshot().get(LandProtectionAction.EXPLOSION));
        assertEquals("ATTACK_ENTITY", shadow.getLatestShadowNotice("other").getAction());
        assertTrue(shadow.getLatestShadowNotice("other").getChunk().contains("lobby"));
        assertEquals(null, enforce.getLatestShadowNotice("other"));
    }

    @Test
    public void everyMinimumProtectionActionUsesTheSameOwnershipBoundary() {
        LandChunkKey claimed = new LandChunkKey("lobby", 0, 1, 1);
        LandProtectionRuntime runtime = new LandProtectionRuntime(
            new LandProtectionPolicy(indexWith(claimed), false), LandProtectionMode.ENFORCE);

        for (LandProtectionAction action : LandProtectionAction.values()) {
            assertTrue(action.name(), runtime.shouldCancel(runtime.evaluate("other", false, claimed, action)));
        }
    }

    private static LandProtectionIndex indexWith(LandChunkKey key) {
        LandProtectionIndex index = new LandProtectionIndex();
        index.put(new PersonalLandTitle(1L, key, "owner", LandTitleStatus.ACTIVE, 1L));
        return index;
    }
}
