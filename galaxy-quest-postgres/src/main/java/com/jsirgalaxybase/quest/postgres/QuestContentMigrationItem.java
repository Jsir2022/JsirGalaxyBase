package com.jsirgalaxybase.quest.postgres;

import java.util.UUID;

public final class QuestContentMigrationItem {
    public enum Kind { QUEST, CHAPTER }
    public enum Action { CREATED, VERSIONED, UNCHANGED }
    private final Kind kind; private final UUID id; private final int version; private final Action action; private final String hash;
    public QuestContentMigrationItem(Kind kind, UUID id, int version, Action action, String hash) {
        if (kind == null || id == null || action == null || hash == null) throw new IllegalArgumentException("migration item fields are required");
        if (version < 1) throw new IllegalArgumentException("version must be positive");
        this.kind=kind;this.id=id;this.version=version;this.action=action;this.hash=hash;
    }
    public Kind getKind(){return kind;} public UUID getId(){return id;} public int getVersion(){return version;}
    public Action getAction(){return action;} public String getHash(){return hash;}
}
