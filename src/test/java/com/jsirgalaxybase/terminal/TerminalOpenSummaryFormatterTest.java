package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.terminal.ui.TerminalHomeSnapshot;

public class TerminalOpenSummaryFormatterTest {

    @Test
    public void buildHomeSummaryIncludesStructuredSnapshotFields() {
        TerminalHomeSnapshot snapshot = new TerminalHomeSnapshot(
            "后勤见习",
            1280,
            "友善",
            "钢材、焦煤、盘点",
            "钢紧、红稳、建材补 | DevA");

        String summary = TerminalOpenSummaryFormatter.buildHomeSummary(snapshot);

        assertTrue(summary.contains("职业: 后勤见习"));
        assertTrue(summary.contains("贡献: 1280"));
        assertTrue(summary.contains("声望: 友善"));
        assertTrue(summary.contains("公共任务: 钢材、焦煤、盘点"));
        assertTrue(summary.contains("市场: 钢紧、红稳、建材补 | DevA"));
    }

    @Test
    public void buildHomeSummaryFallsBackWhenSnapshotIsMissing() {
        String summary = TerminalOpenSummaryFormatter.buildHomeSummary(null);

        assertTrue(summary.contains("首页摘要暂不可用"));
        assertTrue(summary.contains("职业: 未知"));
    }

    @Test
    public void buildStatusBandDetailIncludesKeySnapshotFields() {
        TerminalHomeSnapshot snapshot = new TerminalHomeSnapshot(
            "后勤见习",
            1280,
            "友善",
            "钢材、焦煤、盘点",
            "钢紧、红稳、建材补 | DevA");

        String detail = TerminalOpenSummaryFormatter.buildStatusBandDetail(snapshot);

        assertTrue(detail.contains("职业 后勤见习"));
        assertTrue(detail.contains("声望 友善"));
        assertTrue(detail.contains("公共 钢材、焦煤、盘点"));
    }
}