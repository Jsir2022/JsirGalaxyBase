package com.jsirgalaxybase.quest.bqimport;

public final class BqMigrationIssue implements Comparable<BqMigrationIssue> {
    public enum Severity { WARNING, ERROR }
    private final Severity severity;
    private final String code;
    private final String subject;
    private final String message;

    public BqMigrationIssue(Severity severity, String code, String subject, String message) {
        if (severity == null || code == null || subject == null || message == null) {
            throw new IllegalArgumentException("migration issue fields are required");
        }
        this.severity = severity; this.code = code; this.subject = subject; this.message = message;
    }
    public Severity getSeverity() { return severity; }
    public String getCode() { return code; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    @Override public int compareTo(BqMigrationIssue other) {
        int value = severity.compareTo(other.severity);
        if (value == 0) value = code.compareTo(other.code);
        if (value == 0) value = subject.compareTo(other.subject);
        return value == 0 ? message.compareTo(other.message) : value;
    }
}
