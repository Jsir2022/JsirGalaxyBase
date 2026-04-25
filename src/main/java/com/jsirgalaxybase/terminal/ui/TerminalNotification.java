package com.jsirgalaxybase.terminal.ui;

public final class TerminalNotification {

    private static final String DEFAULT_BANK_ACTION_MESSAGE = "待命 / 可开户 / 可向玩家转账";

    private final String title;
    private final String body;
    private final TerminalNotificationSeverity severity;
    private final long autoCloseMillis;
    private final Runnable onClick;
    private final Runnable onClose;

    private TerminalNotification(Builder builder) {
        this.title = builder.title;
        this.body = builder.body;
        this.severity = builder.severity;
        this.autoCloseMillis = builder.autoCloseMillis;
        this.onClick = builder.onClick;
        this.onClose = builder.onClose;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static TerminalNotification fromFeedback(TerminalActionFeedback feedback) {
        if (feedback == null) {
            return null;
        }
        return builder().severity(feedback.getSeverity())
            .title(feedback.getTitle())
            .body(feedback.getBody())
            .autoCloseMillis(feedback.getAutoCloseMillis())
            .build();
    }

    public static TerminalNotification fromBankMessage(String rawMessage, long autoCloseMillis) {
        return fromFeedback(TerminalActionFeedback.fromLegacyBankMessage(rawMessage, autoCloseMillis));
    }

    public static String stripFormatting(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        StringBuilder plain = new StringBuilder(trimmed.length());
        boolean skipNext = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char current = trimmed.charAt(i);
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if (current == '\u00A7') {
                skipNext = true;
                continue;
            }
            plain.append(current);
        }
        return plain.toString().trim();
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public TerminalNotificationSeverity getSeverity() {
        return severity;
    }

    public long getAutoCloseMillis() {
        return autoCloseMillis;
    }

    public Runnable getOnClick() {
        return onClick;
    }

    public Runnable getOnClose() {
        return onClose;
    }

    public static final class Builder {

        private String title = TerminalNotificationSeverity.INFO.getDefaultTitle();
        private String body = "";
        private TerminalNotificationSeverity severity = TerminalNotificationSeverity.INFO;
        private long autoCloseMillis = 3000L;
        private Runnable onClick;
        private Runnable onClose;

        private Builder() {}

        public Builder title(String title) {
            if (title != null && !title.trim().isEmpty()) {
                this.title = title.trim();
            }
            return this;
        }

        public Builder body(String body) {
            this.body = body == null ? "" : body.trim();
            return this;
        }

        public Builder severity(TerminalNotificationSeverity severity) {
            this.severity = severity == null ? TerminalNotificationSeverity.INFO : severity;
            return this;
        }

        public Builder autoCloseMillis(long autoCloseMillis) {
            this.autoCloseMillis = autoCloseMillis <= 0L ? 3000L : autoCloseMillis;
            return this;
        }

        public Builder onClick(Runnable onClick) {
            this.onClick = onClick;
            return this;
        }

        public Builder onClose(Runnable onClose) {
            this.onClose = onClose;
            return this;
        }

        public TerminalNotification build() {
            return new TerminalNotification(this);
        }
    }
}