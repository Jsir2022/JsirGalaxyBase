package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jsirgalaxybase.terminal.ui.TerminalNotification;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

public class TerminalHudNotificationManagerTest {

    @Before
    public void setUp() {
        TerminalHudNotificationManager.clearForTest();
    }

    @After
    public void tearDown() {
        TerminalHudNotificationManager.clearForTest();
    }

    @Test
    public void pushDeduplicatesSameNotificationBody() {
        TerminalNotification first = notification("账户提示", "已开户", 3000L);
        TerminalNotification second = notification("账户提示", "已开户", 3000L);

        TerminalHudNotificationManager.push(first);
        TerminalHudNotificationManager.push(second);

        List<TerminalHudNotificationManager.NotificationView> visible = TerminalHudNotificationManager.pollVisible(10L, 5);
        assertEquals(1, visible.size());
        assertEquals("已开户", visible.get(0).getNotification().getBody());
    }

    @Test
    public void pushRespectsQueueUpperBound() {
        for (int i = 0; i < 8; i++) {
            TerminalHudNotificationManager.push(notification("N" + i, "B" + i, 3000L));
        }

        List<TerminalHudNotificationManager.NotificationView> visible = TerminalHudNotificationManager.pollVisible(20L, 10);
        assertEquals(6, visible.size());
        assertEquals("B2", visible.get(0).getNotification().getBody());
        assertEquals("B7", visible.get(5).getNotification().getBody());
    }

    @Test
    public void pollVisibleRemovesExpiredNotifications() {
        TerminalHudNotificationManager.push(notification("短通知", "很快过期", 50L));

        List<TerminalHudNotificationManager.NotificationView> initial = TerminalHudNotificationManager.pollVisible(100L, 5);
        assertEquals(1, initial.size());
        List<TerminalHudNotificationManager.NotificationView> expired = TerminalHudNotificationManager.pollVisible(151L, 5);
        assertTrue(expired.isEmpty());
    }

    private TerminalNotification notification(String title, String body, long autoCloseMillis) {
        return TerminalNotification.builder()
            .severity(TerminalNotificationSeverity.INFO)
            .title(title)
            .body(body)
            .autoCloseMillis(autoCloseMillis)
            .build();
    }
}