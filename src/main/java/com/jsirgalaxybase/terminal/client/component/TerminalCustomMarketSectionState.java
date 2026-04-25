package com.jsirgalaxybase.terminal.client.component;

import java.util.List;

import com.jsirgalaxybase.terminal.TerminalCustomMarketActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;

public final class TerminalCustomMarketSectionState {

    private String selectedScope = "active";
    private String selectedListingId = "";

    public void applyModel(TerminalCustomMarketSectionModel model) {
        if (model == null) {
            selectedScope = "active";
            selectedListingId = "";
            return;
        }
        selectedScope = scopeFromLabel(model.getScopeLabel());
        selectedListingId = resolveListingId(model);
    }

    public TerminalCustomMarketActionPayload toPayload() {
        return new TerminalCustomMarketActionPayload(selectedScope, selectedListingId);
    }

    public String getSelectedScope() {
        return selectedScope;
    }

    public void setSelectedScope(String selectedScope) {
        this.selectedScope = normalizeScope(selectedScope);
    }

    public String getSelectedListingId() {
        return selectedListingId;
    }

    public void setSelectedListingId(String selectedListingId) {
        this.selectedListingId = sanitizeNumber(selectedListingId);
    }

    public boolean hasSelectedListing() {
        return parseSelectedListingId() > 0L;
    }

    public long parseSelectedListingId() {
        try {
            return selectedListingId.isEmpty() ? 0L : Long.parseLong(selectedListingId);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private String resolveListingId(TerminalCustomMarketSectionModel model) {
        String selected = sanitizeNumber(model.getSelectedListingId());
        if (!selected.isEmpty()) {
            return selected;
        }
        List<String> ids = "selling".equals(selectedScope) ? model.getSellingListingIds()
            : "pending".equals(selectedScope) ? model.getPendingListingIds() : model.getActiveListingIds();
        for (String id : ids) {
            String value = sanitizeNumber(id);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private String scopeFromLabel(String label) {
        if ("我的出售".equals(label)) {
            return "selling";
        }
        if ("我的待领取".equals(label) || "我的待处理".equals(label)) {
            return "pending";
        }
        return "active";
    }

    private String normalizeScope(String value) {
        if ("selling".equalsIgnoreCase(value)) {
            return "selling";
        }
        if ("pending".equalsIgnoreCase(value)) {
            return "pending";
        }
        return "active";
    }

    private String sanitizeNumber(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current >= '0' && current <= '9') {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}
