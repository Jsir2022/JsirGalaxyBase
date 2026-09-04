package com.jsirgalaxybase.terminal.network;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalNotificationCenterModel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TerminalNotificationPacketTest {

    @Test
    public void structuredSourceAndNavigationTargetSurvivePacketRoundTrip() {
        List<TerminalHomeScreenModel.NotificationModel> source = Arrays.asList(
            new TerminalHomeScreenModel.NotificationModel("恢复事项", "交付需要处理", "WARNING",
                "market-recovery", "market_account_center", "C42"));
        ByteBuf buffer = Unpooled.buffer();

        OpenTerminalApprovedMessage.writeNotifications(buffer, source);
        TerminalHomeScreenModel.NotificationModel decoded =
            OpenTerminalApprovedMessage.readNotifications(buffer).get(0);

        assertEquals("market-recovery", decoded.getSourceId());
        assertEquals("market_account_center", decoded.getTargetPageId());
        assertEquals("C42", decoded.getTargetRecordId());
    }

    @Test
    public void centerSnapshotPreservesBoundedEntriesAndOccurrences() {
        TerminalNotificationCenterModel source = new TerminalNotificationCenterModel("保留最近 1 条", Arrays.asList(
            new TerminalNotificationCenterModel.EntryModel("market-recovery", "market_account_center", "C42",
                "恢复事项", "交付需要处理", "WARNING", 3)), 1);
        ByteBuf buffer = Unpooled.buffer();

        OpenTerminalApprovedMessage.writeNotificationCenter(buffer, source);
        TerminalNotificationCenterModel decoded = OpenTerminalApprovedMessage.readNotificationCenter(buffer);

        assertEquals(1, decoded.getEntries().size());
        assertEquals("C42", decoded.getEntries().get(0).getTargetRecordId());
        assertEquals(3, decoded.getEntries().get(0).getOccurrences());
    }
}
