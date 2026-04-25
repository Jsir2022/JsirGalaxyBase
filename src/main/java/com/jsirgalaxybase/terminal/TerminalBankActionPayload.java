package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TerminalBankActionPayload {

    private final String targetPlayerName;
    private final String amountText;
    private final String comment;

    public TerminalBankActionPayload(String targetPlayerName, String amountText, String comment) {
        this.targetPlayerName = normalize(targetPlayerName);
        this.amountText = normalize(amountText);
        this.comment = normalize(comment);
    }

    public static TerminalBankActionPayload empty() {
        return new TerminalBankActionPayload("", "", "");
    }

    public static TerminalBankActionPayload decode(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return empty();
        }
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 3) {
            return empty();
        }
        return new TerminalBankActionPayload(decodePart(parts[0]), decodePart(parts[1]), decodePart(parts[2]));
    }

    public String encode() {
        return encodePart(targetPlayerName) + "|" + encodePart(amountText) + "|" + encodePart(comment);
    }

    public String getTargetPlayerName() {
        return targetPlayerName;
    }

    public String getAmountText() {
        return amountText;
    }

    public String getComment() {
        return comment;
    }

    public long parseAmount() {
        if (amountText.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(amountText);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public TerminalBankActionPayload clearedAfterTransferSuccess() {
        return new TerminalBankActionPayload(targetPlayerName, "", "");
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