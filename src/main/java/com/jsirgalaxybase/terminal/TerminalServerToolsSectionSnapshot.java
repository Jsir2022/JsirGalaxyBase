package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;

public final class TerminalServerToolsSectionSnapshot {

    private final String serviceState;
    private final String currentServerId;
    private final List<String> serverLines;
    private final List<String> serverIds;
    private final List<String> warpLines;
    private final List<String> warpNames;
    private final List<String> warpSubtitles;
    private final List<String> warpStateLabels;
    private final List<String> recentTransferLines;
    private final String selectedWarpName;
    private final String selectedWarpTitle;
    private final String selectedWarpDetail;
    private final String selectedTargetServerId;
    private final String selectedTargetLocation;
    private final String selectedWarpDescription;
    private final boolean selectedWarpEnabled;
    private final String recentSourceServerId;
    private final String recentTargetServerId;
    private final String recentTransferStatus;
    private final String recentTransferTime;
    private final String recentTransferSummary;
    private final ActionFeedback actionFeedback;

    public TerminalServerToolsSectionSnapshot(String serviceState, String currentServerId,
        List<String> serverLines, List<String> serverIds, List<String> warpLines, List<String> warpNames,
        List<String> warpSubtitles, List<String> warpStateLabels,
        List<String> recentTransferLines,
        String selectedWarpName, String selectedWarpTitle, String selectedWarpDetail,
        String selectedTargetServerId, String selectedTargetLocation, String selectedWarpDescription,
        boolean selectedWarpEnabled,
        String recentSourceServerId, String recentTargetServerId, String recentTransferStatus,
        String recentTransferTime, String recentTransferSummary,
        ActionFeedback actionFeedback) {
        this.serviceState = normalize(serviceState, "ServerTools runtime unavailable");
        this.currentServerId = normalize(currentServerId, "unknown");
        this.serverLines = freeze(serverLines, Collections.singletonList("服务器目录不可用。"));
        this.serverIds = freeze(serverIds, Collections.singletonList(""));
        this.warpLines = freeze(warpLines, Collections.singletonList("当前没有可用系统 warp。"));
        this.warpNames = freeze(warpNames, Collections.singletonList(""));
        this.warpSubtitles = freeze(warpSubtitles, Collections.singletonList("当前没有额外说明。"));
        this.warpStateLabels = freeze(warpStateLabels, Collections.singletonList("不可用"));
        this.recentTransferLines = freeze(recentTransferLines, Collections.singletonList("当前没有最近传送记录。"));
        this.selectedWarpName = normalize(selectedWarpName, "");
        this.selectedWarpTitle = normalize(selectedWarpTitle, "未选择 warp");
        this.selectedWarpDetail = normalize(selectedWarpDetail, "选择左侧 warp 后查看目标与说明。");
        this.selectedTargetServerId = normalize(selectedTargetServerId, "--");
        this.selectedTargetLocation = normalize(selectedTargetLocation, "--");
        this.selectedWarpDescription = normalize(selectedWarpDescription, "当前没有额外传送说明。");
        this.selectedWarpEnabled = selectedWarpEnabled;
        this.recentSourceServerId = normalize(recentSourceServerId, "--");
        this.recentTargetServerId = normalize(recentTargetServerId, "--");
        this.recentTransferStatus = normalize(recentTransferStatus, "暂无记录");
        this.recentTransferTime = normalize(recentTransferTime, "--");
        this.recentTransferSummary = normalize(recentTransferSummary, "当前没有最近传送记录。");
        this.actionFeedback = actionFeedback == null ? ActionFeedback.placeholder() : actionFeedback;
    }

    public static TerminalServerToolsSectionSnapshot placeholder() {
        return new TerminalServerToolsSectionSnapshot(
            "ServerTools runtime unavailable",
            "unknown",
            Collections.singletonList("服务器目录不可用。"),
            Collections.singletonList(""),
            Collections.singletonList("当前没有可用系统 warp。"),
            Collections.singletonList(""),
            Collections.singletonList("当前没有额外说明。"),
            Collections.singletonList("不可用"),
            Collections.singletonList("当前没有最近传送记录。"),
            "",
            "未选择 warp",
            "选择左侧 warp 后查看目标与说明。",
            "--",
            "--",
            "当前没有额外传送说明。",
            false,
            "--",
            "--",
            "暂无记录",
            "--",
            "当前没有最近传送记录。",
            ActionFeedback.placeholder());
    }

    public String getServiceState() {
        return serviceState;
    }

    public String getCurrentServerId() {
        return currentServerId;
    }

    public List<String> getServerLines() {
        return serverLines;
    }

    public List<String> getServerIds() {
        return serverIds;
    }

    public List<String> getWarpLines() {
        return warpLines;
    }

    public List<String> getWarpNames() {
        return warpNames;
    }

    public List<String> getWarpSubtitles() {
        return warpSubtitles;
    }

    public List<String> getWarpStateLabels() {
        return warpStateLabels;
    }

    public List<String> getRecentTransferLines() {
        return recentTransferLines;
    }

    public String getSelectedWarpName() {
        return selectedWarpName;
    }

    public String getSelectedWarpTitle() {
        return selectedWarpTitle;
    }

    public String getSelectedWarpDetail() {
        return selectedWarpDetail;
    }

    public String getSelectedTargetServerId() {
        return selectedTargetServerId;
    }

    public String getSelectedTargetLocation() {
        return selectedTargetLocation;
    }

    public String getSelectedWarpDescription() {
        return selectedWarpDescription;
    }

    public boolean isSelectedWarpEnabled() {
        return selectedWarpEnabled;
    }

    public String getRecentSourceServerId() {
        return recentSourceServerId;
    }

    public String getRecentTargetServerId() {
        return recentTargetServerId;
    }

    public String getRecentTransferStatus() {
        return recentTransferStatus;
    }

    public String getRecentTransferTime() {
        return recentTransferTime;
    }

    public String getRecentTransferSummary() {
        return recentTransferSummary;
    }

    public ActionFeedback getActionFeedback() {
        return actionFeedback;
    }

    private static List<String> freeze(List<String> source, List<String> fallback) {
        List<String> resolved = source == null || source.isEmpty() ? fallback : source;
        return Collections.unmodifiableList(new ArrayList<String>(resolved));
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    public static final class ActionFeedback {

        private final String title;
        private final String body;
        private final String severityName;

        public ActionFeedback(String title, String body, String severityName) {
            this.title = normalize(title, "传送动作反馈");
            this.body = normalize(body, "当前没有传送动作反馈。");
            this.severityName = normalize(severityName, TerminalNotificationSeverity.INFO.name());
        }

        public static ActionFeedback placeholder() {
            return new ActionFeedback("传送动作反馈", "当前没有传送动作反馈。", TerminalNotificationSeverity.INFO.name());
        }

        public String getTitle() {
            return title;
        }

        public String getBody() {
            return body;
        }

        public String getSeverityName() {
            return severityName;
        }
    }
}
