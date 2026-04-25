package com.jsirgalaxybase.terminal.ui;

public final class TerminalActionFeedback {

    private static final String DEFAULT_LEGACY_BANK_ACTION_MESSAGE = "等待操作";

    private final TerminalNotificationSeverity severity;
    private final String title;
    private final String body;
    private final long autoCloseMillis;

    private TerminalActionFeedback(TerminalNotificationSeverity severity, String title, String body, long autoCloseMillis) {
        this.severity = severity == null ? TerminalNotificationSeverity.INFO : severity;
        this.title = normalizeTitle(title, this.severity);
        this.body = body == null ? "" : body.trim();
        this.autoCloseMillis = autoCloseMillis <= 0L ? 3000L : autoCloseMillis;
    }

    public static TerminalActionFeedback of(TerminalNotificationSeverity severity, String title, String body,
        long autoCloseMillis) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        return new TerminalActionFeedback(severity, title, body, autoCloseMillis);
    }

    public static TerminalActionFeedback info(String title, String body, long autoCloseMillis) {
        return of(TerminalNotificationSeverity.INFO, title, body, autoCloseMillis);
    }

    public static TerminalActionFeedback fromStructured(String severityName, String title, String body,
        long autoCloseMillis) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        return of(TerminalNotificationSeverity.fromName(severityName), title, body, autoCloseMillis);
    }

    public static TerminalActionFeedback fromLegacyBankMessage(String rawMessage, long autoCloseMillis) {
        String message = TerminalNotification.stripFormatting(rawMessage);
        if (message.isEmpty() || DEFAULT_LEGACY_BANK_ACTION_MESSAGE.equals(message)) {
            return null;
        }

        TerminalNotificationSeverity severity = classifyBankSeverity(message);
        return of(severity, severity.getDefaultTitle(), message, autoCloseMillis);
    }

    public static TerminalActionFeedback preferStructured(String severityName, String title, String body,
        long autoCloseMillis, String fallbackMessage) {
        TerminalActionFeedback structured = fromStructured(severityName, title, body, autoCloseMillis);
        return structured != null ? structured : fromLegacyBankMessage(fallbackMessage, autoCloseMillis);
    }

    private static TerminalNotificationSeverity classifyBankSeverity(String message) {
        if (message.contains("成功") || message.contains("已开户") || message.contains("已创建")) {
            return TerminalNotificationSeverity.SUCCESS;
        }
        if (message.contains("已存在") || message.contains("无需重复") || message.contains("已就绪")) {
            return TerminalNotificationSeverity.WARNING;
        }
        if (message.contains("失败") || message.contains("请填写") || message.contains("不能")
            || message.contains("不支持") || message.contains("未启用") || message.contains("不可用")
            || message.contains("必须") || message.contains("在线玩家") || message.contains("不足")
            || message.contains("不存在")) {
            return TerminalNotificationSeverity.ERROR;
        }
        return TerminalNotificationSeverity.INFO;
    }

    private static String normalizeTitle(String title, TerminalNotificationSeverity severity) {
        return title == null || title.trim().isEmpty() ? severity.getDefaultTitle() : title.trim();
    }

    public TerminalNotificationSeverity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public long getAutoCloseMillis() {
        return autoCloseMillis;
    }
}
