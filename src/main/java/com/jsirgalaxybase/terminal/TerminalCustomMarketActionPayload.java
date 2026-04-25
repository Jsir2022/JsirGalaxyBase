package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TerminalCustomMarketActionPayload {

    private final String selectedScope;
    private final String selectedListingId;

    public TerminalCustomMarketActionPayload(String selectedScope, String selectedListingId) {
        this.selectedScope = normalize(selectedScope);
        this.selectedListingId = sanitizeNumber(selectedListingId);
    }

    public static TerminalCustomMarketActionPayload empty() {
        return new TerminalCustomMarketActionPayload("active", "");
    }

    public static TerminalCustomMarketActionPayload decode(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return empty();
        }
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 2) {
            return empty();
        }
        return new TerminalCustomMarketActionPayload(decodePart(parts[0]), decodePart(parts[1]));
    }

    public String encode() {
        return encodePart(selectedScope) + "|" + encodePart(selectedListingId);
    }

    public String getSelectedScope() {
        return selectedScope;
    }

    public String getSelectedListingId() {
        return selectedListingId;
    }

    public long parseSelectedListingId() {
        if (selectedListingId.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(selectedListingId);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public TerminalCustomMarketActionPayload clearedAfterSuccess() {
        return new TerminalCustomMarketActionPayload(selectedScope, selectedListingId);
    }

    private static String sanitizeNumber(String value) {
        String normalized = normalize(value);
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current >= '0' && current <= '9') {
                builder.append(current);
            }
        }
        return builder.toString();
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
