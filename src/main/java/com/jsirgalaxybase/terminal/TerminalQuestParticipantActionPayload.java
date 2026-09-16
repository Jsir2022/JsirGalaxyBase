package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Bounded participant-management intent. The authenticated actor is deliberately absent and is always supplied by
 * the receiving server session.
 */
public final class TerminalQuestParticipantActionPayload {
    private final String participantType;
    private final String participantId;
    private final String displayName;
    private final String targetPlayer;
    private final String targetRole;
    private final long expectedRevision;

    public TerminalQuestParticipantActionPayload(String participantType, String participantId, String displayName,
        String targetPlayer, String targetRole, long expectedRevision) {
        this.participantType = limit(participantType, 16);
        this.participantId = limit(participantId, 64);
        this.displayName = limit(displayName, 128);
        this.targetPlayer = limit(targetPlayer, 64);
        this.targetRole = limit(targetRole, 16);
        this.expectedRevision = Math.max(0L, expectedRevision);
    }

    public static TerminalQuestParticipantActionPayload empty() {
        return new TerminalQuestParticipantActionPayload("", "", "", "", "", 0L);
    }

    public String encode() {
        return part(participantType) + '|' + part(participantId) + '|' + part(displayName) + '|'
            + part(targetPlayer) + '|' + part(targetRole) + '|' + expectedRevision;
    }

    public static TerminalQuestParticipantActionPayload decode(String encoded) {
        if (encoded == null) return empty();
        String[] values = encoded.split("\\|", -1);
        if (values.length != 6) return empty();
        try {
            return new TerminalQuestParticipantActionPayload(unpart(values[0]), unpart(values[1]),
                unpart(values[2]), unpart(values[3]), unpart(values[4]), Long.parseLong(values[5]));
        } catch (RuntimeException invalid) {
            return empty();
        }
    }

    public String getParticipantType() { return participantType; }
    public String getParticipantId() { return participantId; }
    public String getDisplayName() { return displayName; }
    public String getTargetPlayer() { return targetPlayer; }
    public String getTargetRole() { return targetRole; }
    public long getExpectedRevision() { return expectedRevision; }

    private static String part(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String unpart(String value) {
        return limit(new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8), 128);
    }

    private static String limit(String value, int maximum) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= maximum ? normalized : normalized.substring(0, maximum);
    }
}
