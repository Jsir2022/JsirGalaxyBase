package com.jsirgalaxybase.terminal.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalLandSectionModel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TerminalLandSectionPacketTest {

    @Test
    public void propertySnapshotRoundTripsStructuredCoordinatesAndVersions() {
        TerminalLandSectionModel land = new TerminalLandSectionModel("READY", "lobby", "SHADOW", -1,
            -20, 30, -19, 31, 2, 4, "MINE", -18, 33, "FAR",
            Arrays.asList(new TerminalLandSectionModel.MapCellModel(-19, 31, "OWNED", 11L, 7L)),
            Arrays.asList(Long.valueOf(11L)), Arrays.asList(Integer.valueOf(-19)),
            Arrays.asList(Integer.valueOf(31)), Arrays.asList(Long.valueOf(7L)),
            0, 1, 1, 11L, 7L, "OWNED", false, true, "SUCCESS");
        TerminalHomeScreenModel.PageSnapshotModel page = new TerminalHomeScreenModel.PageSnapshotModel(
            "property", "个人地产", "附近地皮", Collections.<TerminalHomeScreenModel.SectionModel>emptyList(),
            null, null, null, null, null, land);
        ByteBuf buffer = Unpooled.buffer();

        OpenTerminalApprovedMessage.writePageSnapshots(buffer, Collections.singletonList(page));
        TerminalHomeScreenModel.PageSnapshotModel decoded = OpenTerminalApprovedMessage.readPageSnapshots(buffer).get(0);

        assertTrue(decoded.hasLandSectionModel());
        assertEquals(-19, decoded.getLandSectionModel().getSelectedChunkX());
        assertEquals(7L, decoded.getLandSectionModel().getSelectedVersion());
        assertEquals("SUCCESS", decoded.getLandSectionModel().getFeedbackCode());
        assertEquals(-18, decoded.getLandSectionModel().getViewportChunkX());
        assertEquals("FAR", decoded.getLandSectionModel().getZoom());
        assertEquals(1, decoded.getLandSectionModel().getMapCells().size());
    }
}
