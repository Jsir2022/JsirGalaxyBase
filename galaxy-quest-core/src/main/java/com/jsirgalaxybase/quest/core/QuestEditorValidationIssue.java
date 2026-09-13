package com.jsirgalaxybase.quest.core;

public final class QuestEditorValidationIssue {
    private final String fieldKey;
    private final String code;
    private final String message;

    public QuestEditorValidationIssue(String fieldKey, String code, String message) {
        this.fieldKey = fieldKey == null ? "" : fieldKey;
        this.code = code == null ? "invalid" : code;
        this.message = message == null ? "" : message;
    }

    public String getFieldKey() { return fieldKey; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
