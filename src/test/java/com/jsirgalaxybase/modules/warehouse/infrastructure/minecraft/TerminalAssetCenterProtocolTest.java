package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.Instant;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivityRow;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivityType;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentEntry;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;

public class TerminalAssetCenterProtocolTest {
    @Test public void legacyTabsCollapseIntoStorageAndActivity() {
        assertEquals(TerminalAssetCenterTab.STORAGE, TerminalAssetCenterTab.fromCode(99));
        assertEquals(TerminalAssetCenterTab.STORAGE, TerminalAssetCenterTab.fromCode(0));
        assertEquals(TerminalAssetCenterTab.STORAGE, TerminalAssetCenterTab.fromCode(1));
        assertEquals(TerminalAssetCenterTab.STORAGE, TerminalAssetCenterTab.fromCode(2));
        assertEquals(TerminalAssetCenterTab.ACTIVITY, TerminalAssetCenterTab.fromCode(3));
        assertTrue(TerminalAssetCenterTab.STORAGE.exposesInventory());
        assertFalse(TerminalAssetCenterTab.ACTIVITY.exposesInventory());
    }

    @Test public void activitySnapshotRoundTripsWithServerSideBounds() {
        List<AssetActivityRow> source = new ArrayList<AssetActivityRow>();
        for (int index = 0; index < 24; index++) source.add(new AssetActivityRow("record-" + index,
            AssetActivityType.STANDARD_SELL_SETTLED, null, index, index * 10L, index, "T" + index,
            "COMPLETED", Instant.ofEpochMilli(index)));
        TerminalAssetCenterSnapshotMessage encoded = new TerminalAssetCenterSnapshotMessage(new AssetActivitySnapshot(source));
        ByteBuf buffer = Unpooled.buffer(); encoded.toBytes(buffer);
        TerminalAssetCenterSnapshotMessage decoded = new TerminalAssetCenterSnapshotMessage(); decoded.fromBytes(buffer);
        assertEquals(20, decoded.getSnapshot().getRows().size());
        assertEquals("record-0", decoded.getSnapshot().getRows().get(0).getStableKey());
        assertEquals("record-19", decoded.getSnapshot().getRows().get(19).getStableKey());
        assertEquals(190L, decoded.getSnapshot().getRows().get(19).getAmount());
    }

    @Test public void legacyAuditPayloadStillDecodesForOneRelease() {
        ByteBuf buffer = Unpooled.buffer(); buffer.writeByte(2);
        cpw.mods.fml.common.network.ByteBufUtils.writeUTF8String(buffer, "old-1");
        cpw.mods.fml.common.network.ByteBufUtils.writeUTF8String(buffer, "old-2");
        TerminalAssetCenterSnapshotMessage message = new TerminalAssetCenterSnapshotMessage();
        message.fromBytes(buffer);
        assertEquals(2, message.getSnapshot().getRows().size());
        assertEquals("old-2", message.getSnapshot().getRows().get(1).getReference());
    }

    @Test public void cellContentsRoundTripWithLongCountsAndBoundedPage() {
        TerminalCellContentSnapshot cell = new TerminalCellContentSnapshot(true, 9L, 65536L, 2048L,
            123463L, 40L, 63L, 1, 40, 20, "SUCCESS",
            Collections.<TerminalCellContentEntry>emptyList());
        TerminalAssetCenterSnapshotMessage encoded = new TerminalAssetCenterSnapshotMessage(
            AssetActivitySnapshot.empty(), cell);
        ByteBuf buffer = Unpooled.buffer(); encoded.toBytes(buffer);
        TerminalAssetCenterSnapshotMessage decoded = new TerminalAssetCenterSnapshotMessage(); decoded.fromBytes(buffer);
        assertTrue(decoded.getCellSnapshot().isCellPresent());
        assertEquals(9L, decoded.getCellSnapshot().getBayVersion());
        assertEquals(123463L, decoded.getCellSnapshot().getStoredItems());
        assertEquals(40L, decoded.getCellSnapshot().getStoredTypes());
        assertEquals(2, decoded.getCellSnapshot().getTotalPages());
    }

    @Test public void previousActivityOnlySnapshotStillDecodesWithoutCellPanel() {
        ByteBuf buffer = Unpooled.buffer(); buffer.writeByte(0xA2); buffer.writeByte(0);
        TerminalAssetCenterSnapshotMessage decoded = new TerminalAssetCenterSnapshotMessage();
        decoded.fromBytes(buffer);
        assertFalse(decoded.getCellSnapshot().isCellPresent());
    }
}
