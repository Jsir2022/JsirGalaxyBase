package com.jsirgalaxybase.modules.land.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.jsirgalaxybase.modules.land.domain.LandActionResult;
import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.LandTitleStatus;
import com.jsirgalaxybase.modules.land.domain.PersonalLandActionReceipt;
import com.jsirgalaxybase.modules.land.domain.PersonalLandTitle;
import com.jsirgalaxybase.modules.land.infrastructure.InMemoryPersonalLandRepository;

public class PersonalLandServiceTest {

    @Test
    public void claimCreatesVersionedPersonalTitleAndProtectsIt() {
        TestContext context = context(PersonalLandRules.allowAll(4));
        LandChunkKey key = new LandChunkKey("lobby", 0, 3, 7);

        PersonalLandActionReceipt result = context.service.claim("claim-1", "player-a", key);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getAfterTitle().getTitleId());
        assertEquals(1L, result.getAfterTitle().getVersion());
        assertEquals(LandTitleStatus.ACTIVE, result.getAfterTitle().getStatus());
        assertTrue(context.index.mayModify("player-a", key));
        assertFalse(context.index.mayModify("player-b", key));
        assertFalse(context.index.mayModify(null, key));
        assertEquals(1, context.service.listOwned("player-a").size());
    }

    @Test
    public void repeatedRequestReturnsOriginalReceiptWithoutCreatingAnotherTitle() {
        TestContext context = context(PersonalLandRules.allowAll(4));
        LandChunkKey key = new LandChunkKey("lobby", 0, 1, 2);

        PersonalLandActionReceipt first = context.service.claim("claim-repeat", "player-a", key);
        PersonalLandActionReceipt replay = context.service.claim("claim-repeat", "player-a", key);

        assertSame(first, replay);
        assertEquals(1, context.service.listOwned("player-a").size());
    }

    @Test
    public void replayingOldClaimAfterRevocationDoesNotRestoreStaleProtection() {
        TestContext context = context(PersonalLandRules.allowAll(4));
        LandChunkKey key = new LandChunkKey("lobby", 0, 8, 8);
        PersonalLandActionReceipt claim = context.service.claim("old-claim", "player-a", key);
        context.service.unclaim("revoke-old-claim", "player-a", key, claim.getAfterTitle().getVersion());

        PersonalLandActionReceipt replay = context.service.claim("old-claim", "player-a", key);

        assertEquals(LandActionResult.SUCCESS, replay.getResult());
        assertFalse(context.service.find(key).isPresent());
        assertTrue(context.index.mayModify("player-b", key));
    }

    @Test
    public void reusedRequestIdWithDifferentMeaningIsRejected() {
        TestContext context = context(PersonalLandRules.allowAll(4));

        context.service.claim("shared-request", "player-a", new LandChunkKey("lobby", 0, 1, 1));
        PersonalLandActionReceipt conflict = context.service.claim("shared-request", "player-a",
            new LandChunkKey("lobby", 0, 2, 2));

        assertEquals(LandActionResult.REQUEST_CONFLICT, conflict.getResult());
        assertEquals(1, context.service.listOwned("player-a").size());
    }

    @Test
    public void dimensionReservedChunkAndPerPlayerLimitAreEnforced() {
        LandChunkKey reserved = new LandChunkKey("lobby", 0, 0, 0);
        Set<Integer> blockedDimensions = new HashSet<Integer>();
        blockedDimensions.add(-1);
        Set<LandChunkKey> reservedChunks = new HashSet<LandChunkKey>();
        reservedChunks.add(reserved);
        TestContext context = context(new PersonalLandRules(1, blockedDimensions, reservedChunks));

        assertEquals(LandActionResult.DIMENSION_BLOCKED,
            context.service.claim("blocked", "player-a", new LandChunkKey("lobby", -1, 2, 2)).getResult());
        assertEquals(LandActionResult.RESERVED,
            context.service.claim("reserved", "player-a", reserved).getResult());
        assertEquals(LandActionResult.SUCCESS,
            context.service.claim("allowed", "player-a", new LandChunkKey("lobby", 0, 1, 1)).getResult());
        assertEquals(LandActionResult.LIMIT_REACHED,
            context.service.claim("limited", "player-a", new LandChunkKey("lobby", 0, 2, 2)).getResult());
        assertEquals(LandActionResult.SUCCESS,
            context.service.claim("other-player", "player-b", new LandChunkKey("lobby", 0, 2, 2)).getResult());
    }

    @Test
    public void unclaimRevalidatesOwnerVersionAndUnlockedStatus() {
        TestContext context = context(PersonalLandRules.allowAll(4));
        LandChunkKey key = new LandChunkKey("lobby", 0, 4, 4);
        PersonalLandTitle title = context.service.claim("claim-before-unclaim", "player-a", key).getAfterTitle();

        assertEquals(LandActionResult.NOT_OWNER,
            context.service.unclaim("wrong-owner", "player-b", key, title.getVersion()).getResult());
        assertEquals(LandActionResult.VERSION_CONFLICT,
            context.service.unclaim("wrong-version", "player-a", key, title.getVersion() + 1L).getResult());

        PersonalLandTitle listed = title.withStatus(LandTitleStatus.LISTED);
        context.repository.save(listed);
        context.index.put(listed);
        assertEquals(LandActionResult.TITLE_LOCKED,
            context.service.unclaim("listed", "player-a", key, listed.getVersion()).getResult());

        PersonalLandTitle active = listed.withStatus(LandTitleStatus.ACTIVE);
        context.repository.save(active);
        context.index.put(active);
        assertEquals(LandActionResult.SUCCESS,
            context.service.unclaim("unclaim", "player-a", key, active.getVersion()).getResult());
        assertFalse(context.service.find(key).isPresent());
        assertTrue(context.index.mayModify("player-b", key));

        PersonalLandActionReceipt reclaimed = context.service.claim("reclaim", "player-b", key);
        assertEquals(LandActionResult.SUCCESS, reclaimed.getResult());
        assertEquals("player-b", reclaimed.getAfterTitle().getOwnerPlayerRef());
        assertTrue(reclaimed.getAfterTitle().getTitleId() > title.getTitleId());
    }

    @Test
    public void serverScopedServiceRejectsForeignChunkKeysAndLoadsOnlyLocalSnapshot() {
        InMemoryPersonalLandRepository repository = new InMemoryPersonalLandRepository();
        LandChunkKey local = new LandChunkKey("lobby", 0, 1, 1);
        LandChunkKey foreign = new LandChunkKey("s2", 0, 1, 1);
        repository.save(new PersonalLandTitle(1L, local, "player-a", LandTitleStatus.ACTIVE, 1L));
        repository.save(new PersonalLandTitle(2L, foreign, "player-b", LandTitleStatus.ACTIVE, 1L));
        LandProtectionIndex index = new LandProtectionIndex();
        PersonalLandService service = new PersonalLandService(repository, PersonalLandRules.allowAll(4), index,
            new com.jsirgalaxybase.modules.land.infrastructure.DirectLandTransactionRunner(), "lobby");

        assertEquals(1, index.size());
        assertEquals(LandActionResult.SERVER_MISMATCH,
            service.claim("foreign", "player-a", foreign).getResult());
    }

    @Test
    public void protectionIndexCanBeRebuiltFromRepositorySnapshot() {
        InMemoryPersonalLandRepository repository = new InMemoryPersonalLandRepository();
        LandChunkKey firstKey = new LandChunkKey("lobby", 0, 1, 1);
        LandChunkKey secondKey = new LandChunkKey("s2", 2, -3, 8);
        repository.save(new PersonalLandTitle(1L, firstKey, "player-a", LandTitleStatus.ACTIVE, 1L));
        repository.save(new PersonalLandTitle(2L, secondKey, "player-b", LandTitleStatus.ACTIVE, 1L));

        LandProtectionIndex index = new LandProtectionIndex();
        new PersonalLandService(repository, PersonalLandRules.allowAll(4), index);

        assertEquals(2, index.size());
        assertFalse(index.mayModify("player-b", firstKey));
        assertTrue(index.mayModify("player-b", secondKey));
    }

    private static TestContext context(PersonalLandRules rules) {
        InMemoryPersonalLandRepository repository = new InMemoryPersonalLandRepository();
        LandProtectionIndex index = new LandProtectionIndex();
        return new TestContext(repository, index, new PersonalLandService(repository, rules, index));
    }

    private static final class TestContext {

        private final InMemoryPersonalLandRepository repository;
        private final LandProtectionIndex index;
        private final PersonalLandService service;

        private TestContext(InMemoryPersonalLandRepository repository, LandProtectionIndex index,
            PersonalLandService service) {
            this.repository = repository;
            this.index = index;
            this.service = service;
        }
    }
}
