package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.UUID;

public final class ParticipantId {
    private final ParticipantType type;
    private final UUID id;

    public ParticipantId(ParticipantType type, UUID id) {
        this.type = Objects.requireNonNull(type, "type");
        this.id = Objects.requireNonNull(id, "id");
    }

    public static ParticipantId player(UUID id) { return new ParticipantId(ParticipantType.PLAYER, id); }
    public static ParticipantId party(UUID id) { return new ParticipantId(ParticipantType.PARTY, id); }
    public static ParticipantId team(UUID id) { return new ParticipantId(ParticipantType.TEAM, id); }
    public static ParticipantId publicGroup(UUID id) { return new ParticipantId(ParticipantType.PUBLIC, id); }

    public ParticipantType getType() { return type; }
    public UUID getId() { return id; }
    public String asStableKey() { return type.name().toLowerCase() + ":" + id; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ParticipantId)) return false;
        ParticipantId that = (ParticipantId) other;
        return type == that.type && id.equals(that.id);
    }

    @Override
    public int hashCode() { return 31 * type.hashCode() + id.hashCode(); }

    @Override
    public String toString() { return asStableKey(); }
}
