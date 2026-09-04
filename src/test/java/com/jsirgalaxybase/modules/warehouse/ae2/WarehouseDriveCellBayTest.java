package com.jsirgalaxybase.modules.warehouse.ae2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WarehouseDriveCellBayTest {

    @Test
    public void oneBayOnlyAcceptsOneValidCellAndBlocksNonEmptyRemoval() {
        WarehouseDriveCellBay bay = new WarehouseDriveCellBay();
        assertEquals(WarehouseDriveProbeResult.Code.INVALID_CELL, bay.install(false).getCode());
        assertEquals(WarehouseDriveProbeResult.Code.SUCCESS, bay.install(true).getCode());
        assertTrue(bay.isInstalled());
        assertEquals(WarehouseDriveProbeResult.Code.BAY_OCCUPIED, bay.install(true).getCode());
        bay.updateStoredItems(1L);
        assertEquals(WarehouseDriveProbeResult.Code.CELL_NOT_EMPTY, bay.remove().getCode());
        bay.updateStoredItems(0L);
        assertTrue(bay.remove().isSuccess());
        assertFalse(bay.isInstalled());
    }

    @Test
    public void powerAndChannelAreRequiredAndSimulationNeverChangesStoredCount() {
        WarehouseDriveCellBay bay = new WarehouseDriveCellBay();
        bay.install(true);
        bay.updateStoredItems(23L);
        assertEquals(WarehouseDriveProbeResult.Code.NO_CHANNEL_OR_POWER, bay.evaluateTransfer(false, 64L, 64L).getCode());
        WarehouseDriveProbeResult partial = bay.evaluateTransfer(true, 64L, 16L);
        assertEquals(WarehouseDriveProbeResult.Code.PARTIAL, partial.getCode());
        assertEquals(64L, partial.getRequested());
        assertEquals(16L, partial.getAccepted());
        assertEquals(23L, bay.getStoredItems());
    }
}
