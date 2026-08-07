package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.terminal.TerminalServerToolsActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalServerToolsSectionModel;

public final class TerminalServerToolsSectionState {

    public enum FocusField {
        NONE,
        WARP
    }

    private String selectedWarpName;
    private boolean hasPendingSelection;
    private FocusField focusedField;
    private boolean confirmTriggered;

    public TerminalServerToolsSectionState() {
        this.selectedWarpName = "";
        this.hasPendingSelection = false;
        this.focusedField = FocusField.NONE;
        this.confirmTriggered = false;
    }

    public void applyModel(TerminalServerToolsSectionModel model) {
        if (model == null) {
            this.selectedWarpName = "";
            this.hasPendingSelection = false;
            this.focusedField = FocusField.NONE;
            this.confirmTriggered = false;
            return;
        }
        setSelectedWarpName(model.getSelectedWarpName());
        this.hasPendingSelection = false;
        this.focusedField = FocusField.NONE;
        this.confirmTriggered = false;
    }

    public String getSelectedWarpName() {
        return selectedWarpName;
    }

    public void setSelectedWarpName(String selectedWarpName) {
        this.selectedWarpName = sanitizeWarpName(selectedWarpName);
    }

    public boolean hasPendingSelection() {
        return hasPendingSelection;
    }

    public void setHasPendingSelection(boolean hasPendingSelection) {
        this.hasPendingSelection = hasPendingSelection;
    }

    public void focus(FocusField focusField) {
        this.focusedField = focusField == null ? FocusField.NONE : focusField;
    }

    public boolean isFocused(FocusField focusField) {
        return focusedField == focusField;
    }

    public boolean isConfirmTriggered() {
        return confirmTriggered;
    }

    public void setConfirmTriggered(boolean confirmTriggered) {
        this.confirmTriggered = confirmTriggered;
    }

    public boolean hasSelectedWarp() {
        return !selectedWarpName.isEmpty();
    }

    public TerminalServerToolsActionPayload toPayload() {
        return TerminalServerToolsActionPayload.forWarp(selectedWarpName);
    }

    public TerminalServerToolsActionPayload toEmptyPayload() {
        return TerminalServerToolsActionPayload.empty();
    }

    static String sanitizeWarpName(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder();
        String trimmed = value.trim();
        for (int i = 0; i < trimmed.length() && sanitized.length() < 64; i++) {
            char current = trimmed.charAt(i);
            if ((current >= 'A' && current <= 'Z') || (current >= 'a' && current <= 'z')
                || (current >= '0' && current <= '9') || current == '_' || current == '-') {
                sanitized.append(current);
            }
        }
        return sanitized.toString();
    }
}
