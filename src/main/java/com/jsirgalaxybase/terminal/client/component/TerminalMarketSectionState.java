package com.jsirgalaxybase.terminal.client.component;

import java.util.List;

import com.jsirgalaxybase.terminal.TerminalMarketActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

public final class TerminalMarketSectionState {

    public enum FocusField {
        NONE,
        PRICE,
        QUANTITY
    }

    private String selectedProductKey = "";
    private String limitBuyPriceText = "";
    private String limitBuyQuantityText = "";
    private String pendingClaimCustodyId = "";
    private FocusField focusedField = FocusField.NONE;
    private final TerminalCustomMarketSectionState customState = new TerminalCustomMarketSectionState();
    private final TerminalExchangeMarketSectionState exchangeState = new TerminalExchangeMarketSectionState();

    public void applyModel(TerminalMarketSectionModel model) {
        if (model == null) {
            selectedProductKey = "";
            limitBuyPriceText = "";
            limitBuyQuantityText = "";
            pendingClaimCustodyId = "";
            focusedField = FocusField.NONE;
            return;
        }
        selectedProductKey = normalize(model.getSelectedProductKey());
        limitBuyPriceText = sanitizeNumber(model.getLimitBuyDraft().getPriceText());
        limitBuyQuantityText = sanitizeNumber(model.getLimitBuyDraft().getQuantityText());
        pendingClaimCustodyId = resolvePendingClaimId(model.getClaimIds(), pendingClaimCustodyId);
    }

    public TerminalCustomMarketSectionState getCustomState() {
        return customState;
    }

    public TerminalExchangeMarketSectionState getExchangeState() {
        return exchangeState;
    }

    public TerminalMarketActionPayload toPayload() {
        return new TerminalMarketActionPayload(selectedProductKey, limitBuyPriceText, limitBuyQuantityText,
            pendingClaimCustodyId);
    }

    public String getSelectedProductKey() {
        return selectedProductKey;
    }

    public void setSelectedProductKey(String selectedProductKey) {
        this.selectedProductKey = normalize(selectedProductKey);
    }

    public String getLimitBuyPriceText() {
        return limitBuyPriceText;
    }

    public void setLimitBuyPriceText(String limitBuyPriceText) {
        this.limitBuyPriceText = sanitizeNumber(limitBuyPriceText);
    }

    public String getLimitBuyQuantityText() {
        return limitBuyQuantityText;
    }

    public void setLimitBuyQuantityText(String limitBuyQuantityText) {
        this.limitBuyQuantityText = sanitizeNumber(limitBuyQuantityText);
    }

    public String getPendingClaimCustodyId() {
        return pendingClaimCustodyId;
    }

    public void setPendingClaimCustodyId(String pendingClaimCustodyId) {
        this.pendingClaimCustodyId = sanitizeNumber(pendingClaimCustodyId);
    }

    public boolean hasCompleteLimitBuyDraft() {
        return !selectedProductKey.isEmpty() && parseLong(limitBuyPriceText) > 0L && parseLong(limitBuyQuantityText) > 0L;
    }

    public boolean hasPendingClaimSelection() {
        return parseLong(pendingClaimCustodyId) > 0L;
    }

    public long parsePendingClaimCustodyId() {
        return parseLong(pendingClaimCustodyId);
    }

    public void focus(FocusField focusField) {
        this.focusedField = focusField == null ? FocusField.NONE : focusField;
    }

    public boolean isFocused(FocusField focusField) {
        return focusedField == focusField;
    }

    private String resolvePendingClaimId(List<String> claimIds, String currentValue) {
        String current = sanitizeNumber(currentValue);
        if (!current.isEmpty()) {
            for (String claimId : claimIds) {
                if (current.equals(sanitizeNumber(claimId))) {
                    return current;
                }
            }
        }
        if (claimIds != null) {
            for (String claimId : claimIds) {
                String sanitized = sanitizeNumber(claimId);
                if (!sanitized.isEmpty()) {
                    return sanitized;
                }
            }
        }
        return "";
    }

    private static String sanitizeNumber(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (current >= '0' && current <= '9') {
                builder.append(current);
            }
        }
        return builder.toString();
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
