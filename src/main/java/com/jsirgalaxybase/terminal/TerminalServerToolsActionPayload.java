package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TerminalServerToolsActionPayload {

    private final String warpName;

    public TerminalServerToolsActionPayload(String warpName) {
        this.warpName = normalize(warpName);
    }

    public static TerminalServerToolsActionPayload empty() {
        return new TerminalServerToolsActionPayload("");
    }

    public static TerminalServerToolsActionPayload forWarp(String warpName) {
        return new TerminalServerToolsActionPayload(warpName);
    }

    public static TerminalServerToolsActionPayload decode(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return empty();
        }
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 1) {
            return empty();
        }
        return new TerminalServerToolsActionPayload(decodePart(parts[0]));
    }

    public String encode() {
        return encodePart(warpName);
    }

    public String getWarpName() {
        return warpName;
    }

    public boolean hasWarpName() {
        return !warpName.isEmpty();
    }

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
