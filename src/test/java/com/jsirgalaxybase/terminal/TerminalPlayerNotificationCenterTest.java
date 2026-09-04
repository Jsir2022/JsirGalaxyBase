package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class TerminalPlayerNotificationCenterTest {

    @Test
    public void keepsActionableEntriesAndOmitsRoutineInfo() {
        TerminalPlayerNotificationCenter center = new TerminalPlayerNotificationCenter();
        center.recordAndPage(null, Collections.singletonList(
            new TerminalOpenApproval.NotificationEntry("页面已打开", "普通提示", "INFO")));
        assertEquals(0, center.page(null, 0, 12).getEntries().size());

        assertEquals(2, center.recordAndPage(null, Arrays.asList(
            new TerminalOpenApproval.NotificationEntry("传送已提交", "ticket 已创建", "SUCCESS", "transfer",
                "server_tools", ""),
            new TerminalOpenApproval.NotificationEntry("地产保护观察", "SHADOW", "WARNING", "land",
                "property", ""))).size());
    }

    @Test
    public void deduplicatesSameActionAndPreservesTarget() {
        TerminalPlayerNotificationCenter center = new TerminalPlayerNotificationCenter();
        TerminalOpenApproval.NotificationEntry entry = new TerminalOpenApproval.NotificationEntry("交付异常", "等待恢复",
            "WARNING", "market-recovery", "market_account_center", "C-12");
        center.recordAndPage(null, Collections.singletonList(entry));
        assertEquals("market_account_center", center.recordAndPage(null, Collections.singletonList(entry)).get(0)
            .getTargetPageId());
        assertEquals(1, center.page(null, 0, 12).getEntries().size());
        assertEquals(2, center.page(null, 0, 12).getEntries().get(0).getOccurrences());
    }

    @Test
    public void createsPlayerScopedCenterSnapshotWithAllRetainedEntries() {
        TerminalPlayerNotificationCenter center = new TerminalPlayerNotificationCenter();
        for (int i = 0; i < 15; i++) {
            center.recordAndPage(null, Collections.singletonList(new TerminalOpenApproval.NotificationEntry(
                "恢复 " + i, "记录 " + i, "WARNING", "market-recovery", "market_account_center", "C" + i)));
        }
        TerminalNotificationCenterSnapshot snapshot = center.snapshot(null);
        assertEquals(15, snapshot.getRetainedEntries());
        assertEquals(15, snapshot.getEntries().size());
        assertEquals("C14", snapshot.getEntries().get(0).getTargetRecordId());
        assertTrue(snapshot.getEntries().get(0).getOccurrences() >= 1);
    }
}
