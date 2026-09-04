package com.jsirgalaxybase.modules.warehouse.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveKey;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveResult;
import com.jsirgalaxybase.modules.warehouse.infrastructure.DirectWarehouseTransactionRunner;
import com.jsirgalaxybase.modules.warehouse.infrastructure.InMemoryWarehouseDriveRepository;

public class WarehouseDriveServiceTest {

    private final WarehouseDriveKey key = new WarehouseDriveKey("lobby", 0, -17, 64, 33);

    @Test
    public void ownerLifecycleIsAuditedAndRemovedDriveCanBeReregistered() {
        WarehouseDriveService service = service();
        assertEquals(WarehouseDriveResult.SUCCESS, service.register("r1", "owner", false, key).getResult());
        assertEquals(WarehouseDriveResult.SUCCESS, service.authorizeInstall("r2", "owner", false, key, true, false).getResult());
        assertEquals(WarehouseDriveResult.CELL_NOT_EMPTY,
            service.authorizeRemoveCell("r3", "owner", false, key, true, false).getResult());
        assertEquals(WarehouseDriveResult.SUCCESS,
            service.authorizeRemoveCell("r4", "owner", false, key, true, true).getResult());
        assertEquals(WarehouseDriveResult.SUCCESS, service.authorizeBreak("r5", "owner", false, key, false, true).getResult());
        assertFalse(service.findActive(key).isPresent());
        assertEquals(WarehouseDriveResult.SUCCESS, service.register("r6", "owner", false, key).getResult());
        assertTrue(service.findActive(key).isPresent());
        assertEquals(6, service.listRecent("owner", 20).size());
    }

    @Test
    public void foreignAndFakeActorsNeverGainCellOrBreakAuthority() {
        WarehouseDriveService service = service();
        service.register("r1", "owner", false, key);
        assertEquals(WarehouseDriveResult.NOT_OWNER,
            service.authorizeInstall("r2", "other", false, key, true, false).getResult());
        assertEquals(WarehouseDriveResult.NOT_OWNER,
            service.authorizeBreak("r3", "other", false, key, false, true).getResult());
        assertEquals(WarehouseDriveResult.FAKE_PLAYER_DENIED,
            service.authorizeInstall("r4", "owner", true, key, true, false).getResult());
        assertTrue(service.findActive(key).isPresent());
    }

    @Test
    public void requestReplayIsIdempotentButDifferentSemanticsConflict() {
        WarehouseDriveService service = service();
        assertEquals(WarehouseDriveResult.SUCCESS, service.register("replay", "owner", false, key).getResult());
        assertEquals(WarehouseDriveResult.SUCCESS, service.register("replay", "owner", false, key).getResult());
        assertEquals(WarehouseDriveResult.REQUEST_CONFLICT,
            service.authorizeInstall("replay", "owner", false, key, true, false).getResult());
        assertEquals(1, service.listOwned("owner").size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void foreignServerCannotBeAddressedByThisRuntime() {
        service().register("r1", "owner", false, new WarehouseDriveKey("s2", 0, 1, 2, 3));
    }

    private static WarehouseDriveService service() {
        return new WarehouseDriveService(new InMemoryWarehouseDriveRepository(), new DirectWarehouseTransactionRunner(), "lobby");
    }
}
