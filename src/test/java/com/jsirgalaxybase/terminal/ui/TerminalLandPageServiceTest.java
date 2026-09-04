package com.jsirgalaxybase.terminal.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.jsirgalaxybase.modules.land.application.LandProtectionIndex;
import com.jsirgalaxybase.modules.land.application.PersonalLandRules;
import com.jsirgalaxybase.modules.land.application.PersonalLandService;
import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.infrastructure.DirectLandTransactionRunner;
import com.jsirgalaxybase.modules.land.infrastructure.InMemoryPersonalLandRepository;
import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.TerminalLandActionPayload;
import com.jsirgalaxybase.terminal.TerminalLandSectionSnapshot;

public class TerminalLandPageServiceTest {

    private PersonalLandService land;
    private TerminalLandPageService page;

    @Before
    public void setUp() {
        land = new PersonalLandService(new InMemoryPersonalLandRepository(), PersonalLandRules.allowAll(20),
            new LandProtectionIndex(), new DirectLandTransactionRunner(), "lobby");
        page = new TerminalLandPageService();
    }

    @Test
    public void viewportUsesSparseCellsAndClassifiesOwnership() {
        land.claim("own", "player-a", new LandChunkKey("lobby", 0, -2, -3));
        land.claim("other", "player-b", new LandChunkKey("lobby", 0, -1, -3));

        TerminalLandSectionSnapshot snapshot = snapshot("player-a", -2, -3, TerminalActionType.SELECT_PAGE,
            new TerminalLandActionPayload(TerminalLandActionPayload.Tab.NEARBY, -2, -3, 0, "", 0L));

        assertEquals(2, snapshot.getMapCells().size());
        assertEquals("OWNED", snapshot.getMapCells().get(0).getState());
        assertEquals("OTHER", snapshot.getMapCells().get(1).getState());
        assertEquals("OWNED", snapshot.getSelectedState());
        assertTrue(snapshot.isCanUnclaim());
    }

    @Test
    public void viewportAndZoomAreBoundedAroundServerPlayer() {
        TerminalLandSectionSnapshot snapshot = snapshot("player-a", Integer.MAX_VALUE - 2, Integer.MIN_VALUE + 2,
            TerminalActionType.LAND_VIEWPORT, new TerminalLandActionPayload(TerminalLandActionPayload.Tab.NEARBY,
                Integer.MAX_VALUE - 2, Integer.MIN_VALUE + 2, 0, "", 0L, Integer.MIN_VALUE, Integer.MAX_VALUE,
                TerminalLandActionPayload.Zoom.FAR));
        assertEquals(Integer.MAX_VALUE - 18, snapshot.getViewportChunkX());
        assertEquals(Integer.MIN_VALUE + 18, snapshot.getViewportChunkZ());
        assertEquals("FAR", snapshot.getZoom());
    }

    @Test
    public void outOfRangeClaimIsRejectedWithoutClaimingCurrentChunk() {
        TerminalLandActionPayload payload = new TerminalLandActionPayload(TerminalLandActionPayload.Tab.NEARBY,
            20, 20, 0, "out-of-range", 0L, 16, 16, TerminalLandActionPayload.Zoom.FAR);
        TerminalLandSectionSnapshot snapshot = snapshot("player-a", 0, 0, TerminalActionType.LAND_CLAIM, payload);
        assertEquals("OUT_OF_RANGE", snapshot.getFeedbackCode());
        assertFalse(land.find(new LandChunkKey("lobby", 0, 0, 0)).isPresent());
        assertFalse(land.find(new LandChunkKey("lobby", 0, 20, 20)).isPresent());
    }

    @Test
    public void reservedChunkIsPublishedAsSparseOverlayAndCannotBeClaimed() {
        LandChunkKey reserved = new LandChunkKey("lobby", 0, 1, -1);
        land = new PersonalLandService(new InMemoryPersonalLandRepository(),
            new PersonalLandRules(20, Collections.<Integer>emptySet(), Collections.singleton(reserved)),
            new LandProtectionIndex(), new DirectLandTransactionRunner(), "lobby");
        TerminalLandSectionSnapshot snapshot = snapshot("player-a", 0, 0, TerminalActionType.LAND_SELECT,
            new TerminalLandActionPayload(TerminalLandActionPayload.Tab.NEARBY, 1, -1, 0, "", 0L));
        assertEquals("RESERVED", snapshot.getSelectedState());
        assertEquals(1, snapshot.getMapCells().size());
        assertEquals("RESERVED", snapshot.getMapCells().get(0).getState());
        assertFalse(snapshot.isCanClaim());
    }

    @Test
    public void selectedChunkOutsideGridIsReplacedByServerCurrentChunk() {
        TerminalLandSectionSnapshot snapshot = snapshot("player-a", 100, -100, TerminalActionType.LAND_SELECT,
            new TerminalLandActionPayload(TerminalLandActionPayload.Tab.NEARBY, Integer.MAX_VALUE,
                Integer.MIN_VALUE, 0, "", 0L));

        assertEquals(100, snapshot.getSelectedChunkX());
        assertEquals(-100, snapshot.getSelectedChunkZ());
    }

    @Test
    public void initialPropertyRouteAlwaysSelectsServerCurrentChunk() {
        TerminalLandSectionSnapshot snapshot = snapshot("player-a", 2, 3, TerminalActionType.SELECT_PAGE,
            TerminalLandActionPayload.empty());
        assertEquals(2, snapshot.getSelectedChunkX());
        assertEquals(3, snapshot.getSelectedChunkZ());
    }

    @Test
    public void myLandMaySelectOwnedChunkOutsideNearbyGridButNotArbitraryChunk() {
        land.claim("distant", "player-a", new LandChunkKey("lobby", 0, 500, 600));
        TerminalLandSectionSnapshot owned = snapshot("player-a", 0, 0, TerminalActionType.LAND_SELECT,
            new TerminalLandActionPayload(TerminalLandActionPayload.Tab.MINE, 500, 600, 0, "", 0L));
        assertEquals(500, owned.getSelectedChunkX());
        assertTrue(owned.isCanUnclaim());

        TerminalLandSectionSnapshot arbitrary = snapshot("player-a", 0, 0, TerminalActionType.LAND_SELECT,
            new TerminalLandActionPayload(TerminalLandActionPayload.Tab.MINE, 700, 800, 0, "", 0L));
        assertEquals(0, arbitrary.getSelectedChunkX());
        assertEquals(0, arbitrary.getSelectedChunkZ());
    }

    @Test
    public void claimAndUnclaimUseInjectedPlayerAndVersion() {
        TerminalLandActionPayload claim = new TerminalLandActionPayload(TerminalLandActionPayload.Tab.NEARBY,
            3, 4, 0, "claim-ui", 0L);
        TerminalLandSectionSnapshot claimed = snapshot("player-a", 3, 4, TerminalActionType.LAND_CLAIM, claim);
        assertEquals("SUCCESS", claimed.getFeedbackCode());
        assertEquals("OWNED", claimed.getSelectedState());

        TerminalLandActionPayload stale = new TerminalLandActionPayload(TerminalLandActionPayload.Tab.NEARBY,
            3, 4, 0, "unclaim-stale", claimed.getSelectedVersion() + 1L);
        assertEquals("VERSION_CONFLICT",
            snapshot("player-a", 3, 4, TerminalActionType.LAND_UNCLAIM, stale).getFeedbackCode());

        TerminalLandActionPayload revoke = new TerminalLandActionPayload(TerminalLandActionPayload.Tab.NEARBY,
            3, 4, 0, "unclaim-ok", claimed.getSelectedVersion());
        TerminalLandSectionSnapshot unclaimed = snapshot("player-a", 3, 4, TerminalActionType.LAND_UNCLAIM, revoke);
        assertEquals("SUCCESS", unclaimed.getFeedbackCode());
        assertEquals("UNCLAIMED", unclaimed.getSelectedState());
    }

    @Test
    public void ownedListUsesTruePageCountAndClampsPage() {
        for (int i = 0; i < 11; i++) {
            land.claim("claim-" + i, "player-a", new LandChunkKey("lobby", 0, i, 0));
        }
        TerminalLandSectionSnapshot snapshot = snapshot("player-a", 0, 0, TerminalActionType.LAND_CHANGE_PAGE,
            new TerminalLandActionPayload(TerminalLandActionPayload.Tab.MINE, 0, 0, 99, "", 0L));

        assertEquals(11, snapshot.getTotalEntries());
        assertEquals(3, snapshot.getTotalPages());
        assertEquals(2, snapshot.getPageIndex());
        assertEquals(3, snapshot.getOwnedTitleIds().size());
    }

    @Test
    public void missingRequestIdNeverWrites() {
        TerminalLandSectionSnapshot snapshot = snapshot("player-a", 1, 1, TerminalActionType.LAND_CLAIM,
            TerminalLandActionPayload.empty());
        assertEquals("INVALID_REQUEST", snapshot.getFeedbackCode());
        assertFalse(land.find(new LandChunkKey("lobby", 0, 1, 1)).isPresent());
    }

    private TerminalLandSectionSnapshot snapshot(String player, int centerX, int centerZ,
        TerminalActionType action, TerminalLandActionPayload payload) {
        return page.createSnapshot(land, "lobby", "SHADOW", 20, player, 0, centerX, centerZ, action, payload);
    }
}
