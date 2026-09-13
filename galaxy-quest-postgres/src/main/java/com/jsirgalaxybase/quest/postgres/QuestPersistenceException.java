package com.jsirgalaxybase.quest.postgres;

public final class QuestPersistenceException extends RuntimeException {
    public QuestPersistenceException(String message, Throwable cause) { super(message, cause); }
    public QuestPersistenceException(String message) { super(message); }
}
