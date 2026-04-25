package com.jsirgalaxybase.terminal.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TerminalNotificationTest {

    @Test
    public void fromFeedbackUsesStructuredSeverityWithoutGuessingMessage() {
        TerminalNotification notification = TerminalNotification.fromFeedback(
            TerminalActionFeedback.of(TerminalNotificationSeverity.WARNING, "订单提示", "下单已提交", 4500L));

        assertNotNull(notification);
        assertEquals(TerminalNotificationSeverity.WARNING, notification.getSeverity());
        assertEquals("订单提示", notification.getTitle());
        assertEquals("下单已提交", notification.getBody());
        assertEquals(4500L, notification.getAutoCloseMillis());
    }

    @Test
    public void fromBankMessageFallsBackToLegacyMessageClassification() {
        TerminalNotification notification = TerminalNotification.fromBankMessage("§a开户成功: ACC-001", 3000L);

        assertNotNull(notification);
        assertEquals(TerminalNotificationSeverity.SUCCESS, notification.getSeverity());
        assertEquals("开户成功: ACC-001", notification.getBody());
    }

    @Test
    public void fromBankMessageSkipsDefaultIdleMessage() {
        assertNull(TerminalNotification.fromBankMessage("待命 / 可开户 / 可向玩家转账", 3000L));
    }
}