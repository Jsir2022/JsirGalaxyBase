package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.terminal.TerminalBankActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel;

public final class TerminalBankSectionState {

    public enum FocusField {
        NONE,
        TARGET,
        AMOUNT,
        COMMENT
    }

    private String targetPlayerName = "";
    private String amountText = "";
    private String comment = "";
    private FocusField focusedField = FocusField.NONE;

    public void applyModel(TerminalBankSectionModel.TransferFormModel model) {
        if (model == null) {
            this.targetPlayerName = "";
            this.amountText = "";
            this.comment = "";
            this.focusedField = FocusField.NONE;
            return;
        }
        setTargetPlayerName(model.getTargetPlayerName());
        setAmountText(model.getAmountText());
        setComment(model.getComment());
        this.focusedField = FocusField.NONE;
    }

    public String getTargetPlayerName() {
        return targetPlayerName;
    }

    public void setTargetPlayerName(String targetPlayerName) {
        this.targetPlayerName = sanitizePlayerName(targetPlayerName);
    }

    public String getAmountText() {
        return amountText;
    }

    public void setAmountText(String amountText) {
        this.amountText = sanitizeAmount(amountText);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = sanitizeComment(comment);
    }

    public void focus(FocusField focusField) {
        this.focusedField = focusField == null ? FocusField.NONE : focusField;
    }

    public boolean isFocused(FocusField focusField) {
        return focusedField == focusField;
    }

    public boolean hasCompleteTransferDraft() {
        return !targetPlayerName.isEmpty() && parseAmount() > 0L;
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

    public TerminalBankActionPayload toPayload() {
        return new TerminalBankActionPayload(targetPlayerName, amountText, comment);
    }

    static String sanitizePlayerName(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder();
        String trimmed = value.trim();
        for (int i = 0; i < trimmed.length() && sanitized.length() < 16; i++) {
            char current = trimmed.charAt(i);
            if ((current >= 'A' && current <= 'Z') || (current >= 'a' && current <= 'z')
                || (current >= '0' && current <= '9') || current == '_') {
                sanitized.append(current);
            }
        }
        return sanitized.toString();
    }

    static String sanitizeAmount(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder();
        String trimmed = value.trim();
        for (int i = 0; i < trimmed.length(); i++) {
            char current = trimmed.charAt(i);
            if (current >= '0' && current <= '9') {
                sanitized.append(current);
            }
        }
        return sanitized.toString();
    }

    static String sanitizeComment(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() > 96 ? trimmed.substring(0, 96) : trimmed;
    }
}