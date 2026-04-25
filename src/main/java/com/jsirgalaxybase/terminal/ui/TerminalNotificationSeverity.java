package com.jsirgalaxybase.terminal.ui;

public enum TerminalNotificationSeverity {

    SUCCESS("操作成功", 0xFF3A7C4B, 0xFF68C07A),
    INFO("终端通知", 0xFF284866, 0xFF529BED),
    WARNING("账户提示", 0xFF6F5321, 0xFFD1A64C),
    ERROR("操作失败", 0xFF6C2A2F, 0xFFE06C75);

    private final String defaultTitle;
    private final int backgroundColor;
    private final int accentColor;

    TerminalNotificationSeverity(String defaultTitle, int backgroundColor, int accentColor) {
        this.defaultTitle = defaultTitle;
        this.backgroundColor = backgroundColor;
        this.accentColor = accentColor;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public static TerminalNotificationSeverity fromName(String name) {
        if (name != null) {
            for (TerminalNotificationSeverity severity : values()) {
                if (severity.name().equalsIgnoreCase(name.trim())) {
                    return severity;
                }
            }
        }
        return INFO;
    }
}