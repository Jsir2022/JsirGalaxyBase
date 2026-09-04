package com.jsirgalaxybase.terminal.client.component;

import com.jsirgalaxybase.terminal.TerminalServerToolsActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalServerToolsSectionModel;

public final class TerminalServerToolsSectionState {

    public enum FocusField {
        NONE,
        WARP,
        HOME,
        TPA_PLAYER,
        TPA_SERVER
    }

    private String selectedWarpName;
    private String selectedHomeName;
    private String homeNameDraft;
    private String tpaPlayerNameDraft;
    private String tpaTargetServerIdDraft;
    private boolean hasPendingSelection;
    private FocusField focusedField;
    private boolean confirmTriggered;

    public TerminalServerToolsSectionState() {
        this.selectedWarpName = "";
        this.selectedHomeName = "";
        this.homeNameDraft = "home";
        this.tpaPlayerNameDraft = "";
        this.tpaTargetServerIdDraft = "";
        this.hasPendingSelection = false;
        this.focusedField = FocusField.NONE;
        this.confirmTriggered = false;
    }

    public void applyModel(TerminalServerToolsSectionModel model) {
        if (model == null) {
            this.selectedWarpName = "";
            this.selectedHomeName = "";
            this.homeNameDraft = "home";
            this.tpaPlayerNameDraft = "";
            this.tpaTargetServerIdDraft = "";
            this.hasPendingSelection = false;
            this.focusedField = FocusField.NONE;
            this.confirmTriggered = false;
            return;
        }
        setSelectedWarpName(model.getSelectedWarpName());
        setSelectedHomeName(model.getSelectedHomeName());
        if (homeNameDraft.isEmpty()) homeNameDraft = selectedHomeName.isEmpty() ? "home" : selectedHomeName;
        if (tpaTargetServerIdDraft.isEmpty()) tpaTargetServerIdDraft = model.getCurrentServerId();
        this.hasPendingSelection = false;
        this.focusedField = FocusField.NONE;
        this.confirmTriggered = false;
    }

    public String getSelectedWarpName() {
        return selectedWarpName;
    }

    public void setSelectedWarpName(String selectedWarpName) {
        this.selectedWarpName = sanitizeName(selectedWarpName);
    }

    public String getSelectedHomeName() { return selectedHomeName; }
    public void setSelectedHomeName(String selectedHomeName) {
        this.selectedHomeName = sanitizeName(selectedHomeName);
    }
    public boolean hasSelectedHome() { return !selectedHomeName.isEmpty(); }
    public String getHomeNameDraft() { return homeNameDraft; }
    public void setHomeNameDraft(String homeNameDraft) { this.homeNameDraft = sanitizeName(homeNameDraft); }
    public String getTpaPlayerNameDraft() { return tpaPlayerNameDraft; }
    public void setTpaPlayerNameDraft(String value) { this.tpaPlayerNameDraft = sanitizeName(value); }
    public String getTpaTargetServerIdDraft() { return tpaTargetServerIdDraft; }
    public void setTpaTargetServerIdDraft(String value) { this.tpaTargetServerIdDraft = sanitizeServerId(value); }

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

    public TerminalServerToolsActionPayload toHomePayload() {
        return TerminalServerToolsActionPayload.forHome(selectedHomeName);
    }

    public TerminalServerToolsActionPayload toHomeDraftPayload() {
        return TerminalServerToolsActionPayload.forHome(homeNameDraft);
    }

    public TerminalServerToolsActionPayload toTpaPayload() {
        return TerminalServerToolsActionPayload.forTpa(tpaPlayerNameDraft, tpaTargetServerIdDraft);
    }

    static String sanitizeName(String value) {
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

    static String sanitizeServerId(String value) {
        if (value == null) return "";
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
