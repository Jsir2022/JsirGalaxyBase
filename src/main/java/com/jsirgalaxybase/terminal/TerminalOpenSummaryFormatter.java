package com.jsirgalaxybase.terminal;

import com.jsirgalaxybase.terminal.ui.TerminalHomeSnapshot;

public final class TerminalOpenSummaryFormatter {

    private TerminalOpenSummaryFormatter() {}

    public static String buildHomeSummary(TerminalHomeSnapshot snapshot) {
        if (snapshot == null) {
            return "首页摘要暂不可用 | 职业: 未知 | 贡献: 0 | 声望: 未知";
        }

        return "首页摘要已加载"
            + " | 职业: " + safe(snapshot.getCareer(), "未知")
            + " | 贡献: " + snapshot.getContribution()
            + " | 声望: " + safe(snapshot.getReputation(), "未知")
            + " | 公共任务: " + safe(snapshot.getPublicTasks(), "暂无")
            + " | 市场: " + safe(snapshot.getMarketSummary(), "暂无");
    }

    public static String buildStatusBandDetail(TerminalHomeSnapshot snapshot) {
        if (snapshot == null) {
            return "职业未知 | 声望未知 | 公共任务待同步";
        }

        return "职业 " + safe(snapshot.getCareer(), "未知")
            + " | 声望 " + safe(snapshot.getReputation(), "未知")
            + " | 公共 " + safe(snapshot.getPublicTasks(), "暂无");
    }

    public static String buildCareerSectionSummary(TerminalHomeSnapshot snapshot) {
        if (snapshot == null) {
            return "职业状态暂不可用";
        }
        return safe(snapshot.getCareer(), "未知") + " / " + safe(snapshot.getReputation(), "未知")
            + " / 贡献 " + snapshot.getContribution();
    }

    public static String buildPublicServiceSectionSummary(TerminalHomeSnapshot snapshot) {
        if (snapshot == null) {
            return "公共任务暂不可用";
        }
        return safe(snapshot.getPublicTasks(), "暂无公共任务");
    }

    public static String buildMarketSectionSummary(TerminalHomeSnapshot snapshot) {
        if (snapshot == null) {
            return "市场摘要暂不可用";
        }
        return safe(snapshot.getMarketSummary(), "暂无市场摘要");
    }

    public static String buildMigrationSectionSummary() {
        return "phase 3 首页壳已接入，后续银行页和市场页会挂到同一宿主上。";
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}