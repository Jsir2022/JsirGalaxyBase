package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TerminalServerToolsActionPayload {

    private final String warpName;
    private final String quickAction;
    private final String homeName;
    private final String tpaPlayerName;
    private final String tpaTargetServerId;

    public TerminalServerToolsActionPayload(String warpName) {
        this(warpName, "", "");
    }

    public TerminalServerToolsActionPayload(String warpName, String quickAction) {
        this(warpName, quickAction, "");
    }

    public TerminalServerToolsActionPayload(String warpName, String quickAction, String homeName) {
        this(warpName, quickAction, homeName, "", "");
    }

    public TerminalServerToolsActionPayload(String warpName, String quickAction, String homeName,
        String tpaPlayerName, String tpaTargetServerId) {
        this.warpName = normalize(warpName);
        this.quickAction = normalize(quickAction);
        this.homeName = normalize(homeName);
        this.tpaPlayerName = normalize(tpaPlayerName);
        this.tpaTargetServerId = normalize(tpaTargetServerId);
    }

    public static TerminalServerToolsActionPayload empty() {
        return new TerminalServerToolsActionPayload("");
    }

    public static TerminalServerToolsActionPayload forWarp(String warpName) {
        return new TerminalServerToolsActionPayload(warpName);
    }

    public static TerminalServerToolsActionPayload forQuickAction(String quickAction) {
        return new TerminalServerToolsActionPayload("", quickAction);
    }

    public static TerminalServerToolsActionPayload forHome(String homeName) {
        return new TerminalServerToolsActionPayload("", "", homeName);
    }

    /** TPA identity is always inferred by the dedicated server; this only names the other party and target server. */
    public static TerminalServerToolsActionPayload forTpa(String playerName, String targetServerId) {
        return new TerminalServerToolsActionPayload("", "", "", playerName, targetServerId);
    }

    public static TerminalServerToolsActionPayload decode(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return empty();
        }
        String[] parts = payload.split("\\|", -1);
        if (parts.length == 1) {
            return new TerminalServerToolsActionPayload(decodePart(parts[0]));
        }
        if (parts.length == 2) {
            return new TerminalServerToolsActionPayload(decodePart(parts[0]), decodePart(parts[1]));
        }
        if (parts.length == 5) {
            return new TerminalServerToolsActionPayload(decodePart(parts[0]), decodePart(parts[1]),
                decodePart(parts[2]), decodePart(parts[3]), decodePart(parts[4]));
        }
        if (parts.length != 3) {
            return empty();
        }
        return new TerminalServerToolsActionPayload(decodePart(parts[0]), decodePart(parts[1]), decodePart(parts[2]));
    }

    public String encode() {
        if (!tpaPlayerName.isEmpty() || !tpaTargetServerId.isEmpty()) {
            return encodePart(warpName) + "|" + encodePart(quickAction) + "|" + encodePart(homeName) + "|"
                + encodePart(tpaPlayerName) + "|" + encodePart(tpaTargetServerId);
        }
        if (homeName.isEmpty()) {
            return quickAction.isEmpty() ? encodePart(warpName) : encodePart(warpName) + "|" + encodePart(quickAction);
        }
        return encodePart(warpName) + "|" + encodePart(quickAction) + "|" + encodePart(homeName);
    }

    public String getWarpName() {
        return warpName;
    }

    public boolean hasWarpName() {
        return !warpName.isEmpty();
    }

    public String getQuickAction() {
        return quickAction;
    }

    public boolean hasQuickAction() {
        return !quickAction.isEmpty();
    }

    public String getHomeName() {
        return homeName;
    }

    public boolean hasHomeName() {
        return !homeName.isEmpty();
    }

    public String getTpaPlayerName() { return tpaPlayerName; }
    public boolean hasTpaPlayerName() { return !tpaPlayerName.isEmpty(); }
    public String getTpaTargetServerId() { return tpaTargetServerId; }
    public boolean hasTpaTargetServerId() { return !tpaTargetServerId.isEmpty(); }

    private static String encodePart(String value) {
        return Base64.getUrlEncoder().encodeToString(normalize(value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decodePart(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(normalize(value)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
