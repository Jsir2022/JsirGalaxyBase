package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TerminalExchangeMarketActionPayload {

    public static final String TARGET_TASK_COIN = "task-coin-formal";

    private final String selectedTargetCode;
    private final String selectedCoinCode;
    private final String query;
    private final int pageIndex;
    private final int selectedVaultSlot;

    public TerminalExchangeMarketActionPayload(String selectedTargetCode) {
        this(selectedTargetCode, "", "", 0, -1);
    }

    public TerminalExchangeMarketActionPayload(String selectedTargetCode, String selectedCoinCode, String query,
        int pageIndex) {
        this(selectedTargetCode, selectedCoinCode, query, pageIndex, -1);
    }

    public TerminalExchangeMarketActionPayload(String selectedTargetCode, String selectedCoinCode, String query,
        int pageIndex, int selectedVaultSlot) {
        this.selectedTargetCode = TARGET_TASK_COIN.equals(normalize(selectedTargetCode)) ? TARGET_TASK_COIN : "";
        this.selectedCoinCode = normalize(selectedCoinCode);
        this.query = normalize(query);
        this.pageIndex = Math.max(0, pageIndex);
        this.selectedVaultSlot = selectedVaultSlot < 0 ? -1 : selectedVaultSlot;
    }

    public static TerminalExchangeMarketActionPayload empty() {
        return new TerminalExchangeMarketActionPayload("");
    }

    public static TerminalExchangeMarketActionPayload decode(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return empty();
        }
        String[] parts = payload.split("\\|", -1);
        if (parts.length == 1) { return new TerminalExchangeMarketActionPayload(decodePart(parts[0])); }
        if (parts.length != 4 && parts.length != 5) { return empty(); }
        return new TerminalExchangeMarketActionPayload(decodePart(parts[0]), decodePart(parts[1]), decodePart(parts[2]),
            parsePage(decodePart(parts[3])), parts.length == 5 ? parseSlot(decodePart(parts[4])) : -1);
    }

    public String encode() {
        return encodePart(selectedTargetCode) + "|" + encodePart(selectedCoinCode) + "|" + encodePart(query)
            + "|" + encodePart(String.valueOf(pageIndex)) + "|" + encodePart(String.valueOf(selectedVaultSlot));
    }

    public String getSelectedTargetCode() {
        return selectedTargetCode;
    }

    public boolean hasSelectedTarget() {
        return TARGET_TASK_COIN.equals(selectedTargetCode);
    }

    public String getSelectedCoinCode() { return selectedCoinCode; }
    public String getQuery() { return query; }
    public int getPageIndex() { return pageIndex; }
    public int getSelectedVaultSlot() { return selectedVaultSlot; }

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

    private static int parsePage(String value) {
        try { return Math.max(0, Integer.parseInt(normalize(value))); } catch (NumberFormatException ignored) { return 0; }
    }

    private static int parseSlot(String value) {
        try { return Math.max(-1, Integer.parseInt(normalize(value))); } catch (NumberFormatException ignored) { return -1; }
    }
}
