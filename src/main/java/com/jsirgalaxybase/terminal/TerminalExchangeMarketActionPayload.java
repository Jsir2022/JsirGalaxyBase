package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TerminalExchangeMarketActionPayload {

    public static final String TARGET_TASK_COIN = "task-coin-formal";

    private final String selectedTargetCode;

    public TerminalExchangeMarketActionPayload(String selectedTargetCode) {
        this.selectedTargetCode = TARGET_TASK_COIN.equals(normalize(selectedTargetCode)) ? TARGET_TASK_COIN : "";
    }

    public static TerminalExchangeMarketActionPayload empty() {
        return new TerminalExchangeMarketActionPayload("");
    }

    public static TerminalExchangeMarketActionPayload decode(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return empty();
        }
        return new TerminalExchangeMarketActionPayload(decodePart(payload));
    }

    public String encode() {
        return encodePart(selectedTargetCode);
    }

    public String getSelectedTargetCode() {
        return selectedTargetCode;
    }

    public boolean hasSelectedTarget() {
        return TARGET_TASK_COIN.equals(selectedTargetCode);
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
