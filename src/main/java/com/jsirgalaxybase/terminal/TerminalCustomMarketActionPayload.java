package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TerminalCustomMarketActionPayload {

    private final String selectedScope;
    private final String selectedListingId;
    private final String publishPriceText;
    private final String query;
    private final int pageIndex;
    private final int selectedVaultSlot;

    public TerminalCustomMarketActionPayload(String selectedScope, String selectedListingId) {
        this(selectedScope, selectedListingId, "");
    }

    public TerminalCustomMarketActionPayload(String selectedScope, String selectedListingId, String publishPriceText) {
        this(selectedScope, selectedListingId, publishPriceText, "", 0, -1);
    }

    public TerminalCustomMarketActionPayload(String selectedScope, String selectedListingId, String publishPriceText,
        String query, int pageIndex) {
        this(selectedScope, selectedListingId, publishPriceText, query, pageIndex, -1);
    }

    public TerminalCustomMarketActionPayload(String selectedScope, String selectedListingId, String publishPriceText,
        String query, int pageIndex, int selectedVaultSlot) {
        this.selectedScope = normalize(selectedScope);
        this.selectedListingId = sanitizeNumber(selectedListingId);
        this.publishPriceText = sanitizeNumber(publishPriceText);
        this.query = normalize(query);
        this.pageIndex = Math.max(0, pageIndex);
        this.selectedVaultSlot = selectedVaultSlot < 0 ? -1 : selectedVaultSlot;
    }

    public static TerminalCustomMarketActionPayload empty() {
        return new TerminalCustomMarketActionPayload("active", "");
    }

    public static TerminalCustomMarketActionPayload decode(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return empty();
        }
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 2 && parts.length != 3 && parts.length != 5 && parts.length != 6) {
            return empty();
        }
        return new TerminalCustomMarketActionPayload(decodePart(parts[0]), decodePart(parts[1]),
            parts.length >= 3 ? decodePart(parts[2]) : "", parts.length >= 5 ? decodePart(parts[3]) : "",
            parts.length >= 5 ? parsePage(decodePart(parts[4])) : 0,
            parts.length == 6 ? parseSlot(decodePart(parts[5])) : -1);
    }

    public String encode() {
        return encodePart(selectedScope) + "|" + encodePart(selectedListingId) + "|" + encodePart(publishPriceText)
            + "|" + encodePart(query) + "|" + encodePart(String.valueOf(pageIndex))
            + "|" + encodePart(String.valueOf(selectedVaultSlot));
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

    public String getPublishPriceText() {
        return publishPriceText;
    }

    public long parsePublishPrice() {
        if (publishPriceText.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(publishPriceText);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public String getQuery() { return query; }
    public int getPageIndex() { return pageIndex; }
    public int getSelectedVaultSlot() { return selectedVaultSlot; }

    public TerminalCustomMarketActionPayload clearedAfterSuccess() {
        return new TerminalCustomMarketActionPayload(selectedScope, selectedListingId, publishPriceText, query, pageIndex,
            selectedVaultSlot);
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

    private static int parsePage(String value) {
        try { return Math.max(0, Integer.parseInt(normalize(value))); } catch (NumberFormatException ignored) { return 0; }
    }

    private static int parseSlot(String value) {
        try { return Math.max(-1, Integer.parseInt(normalize(value))); } catch (NumberFormatException ignored) { return -1; }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
