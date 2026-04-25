package com.jsirgalaxybase.terminal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TerminalMarketActionPayload {

    private final String selectedProductKey;
    private final String priceText;
    private final String quantityText;
    private final String custodyIdText;

    public TerminalMarketActionPayload(String selectedProductKey, String priceText, String quantityText,
        String custodyIdText) {
        this.selectedProductKey = normalize(selectedProductKey);
        this.priceText = normalize(priceText);
        this.quantityText = normalize(quantityText);
        this.custodyIdText = normalize(custodyIdText);
    }

    public static TerminalMarketActionPayload empty() {
        return new TerminalMarketActionPayload("", "", "", "");
    }

    public static TerminalMarketActionPayload decode(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return empty();
        }
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 4) {
            return empty();
        }
        return new TerminalMarketActionPayload(
            decodePart(parts[0]),
            decodePart(parts[1]),
            decodePart(parts[2]),
            decodePart(parts[3]));
    }

    public String encode() {
        return encodePart(selectedProductKey) + "|"
            + encodePart(priceText) + "|"
            + encodePart(quantityText) + "|"
            + encodePart(custodyIdText);
    }

    public String getSelectedProductKey() {
        return selectedProductKey;
    }

    public String getPriceText() {
        return priceText;
    }

    public String getQuantityText() {
        return quantityText;
    }

    public String getCustodyIdText() {
        return custodyIdText;
    }

    public long parsePrice() {
        return parseLong(priceText);
    }

    public long parseQuantity() {
        return parseLong(quantityText);
    }

    public long parseCustodyId() {
        return parseLong(custodyIdText);
    }

    public TerminalMarketActionPayload clearedAfterLimitBuySuccess() {
        return new TerminalMarketActionPayload(selectedProductKey, priceText, "", custodyIdText);
    }

    public TerminalMarketActionPayload clearedAfterClaimSuccess() {
        return new TerminalMarketActionPayload(selectedProductKey, priceText, quantityText, "");
    }

    private static long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
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