package com.jsirgalaxybase.terminal.client.component;

import java.util.List;

import com.jsirgalaxybase.terminal.TerminalCustomMarketActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;

public final class TerminalCustomMarketSectionState {

    private String selectedScope = "active";
    private String selectedListingId = "";
    private String publishPriceText = "";
    private boolean publishPriceFocused;
    private int selectedVaultSlot = -1;
    private final MarketBrowseDetailController browser = new MarketBrowseDetailController();
    private boolean pendingDetail;

    public void applyModel(TerminalCustomMarketSectionModel model) {
        if (model == null) {
            selectedScope = "active";
            selectedListingId = "";
            publishPriceText = "";
            publishPriceFocused = false;
            selectedVaultSlot = -1;
            browser.reset();
            pendingDetail = false;
            return;
        }
        selectedScope = scopeFromLabel(model.getScopeLabel());
        selectedListingId = resolveListingId(model);
        if (pendingDetail && !selectedListingId.isEmpty()) {
            browser.openDetail(selectedListingId);
            pendingDetail = false;
        }
    }

    public TerminalCustomMarketActionPayload toPayload() {
        return new TerminalCustomMarketActionPayload(selectedScope, selectedListingId, publishPriceText,
            browser.getQuery(), browser.getPageIndex(), selectedVaultSlot);
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

    public void requestDetail(String listingId) {
        setSelectedListingId(listingId);
        browser.setSelectedKey(this.selectedListingId);
        pendingDetail = !this.selectedListingId.isEmpty();
    }

    public boolean isDetailView() { return browser.isDetail(); }
    public void returnToBrowse() { browser.openBrowse(); pendingDetail = false; publishPriceFocused = false; }
    public String getBrowserQuery() { return browser.getQuery(); }
    public void setBrowserQuery(String value) { browser.setQuery(value); }
    public int getBrowserPage() { return browser.getPageIndex(); }
    public void setBrowserPage(int value) { browser.setPageIndex(value); }
    public int getBrowserGridScrollOffset() { return browser.getGridScrollOffset(); }
    public void setBrowserGridScrollOffset(int value) { browser.setGridScrollOffset(value); }

    public boolean hasSelectedListing() {
        return parseSelectedListingId() > 0L;
    }

    public String getPublishPriceText() {
        return publishPriceText;
    }

    public void setPublishPriceText(String value) {
        publishPriceText = sanitizeNumber(value);
    }

    public boolean hasPublishPrice() {
        try {
            return !publishPriceText.isEmpty() && Long.parseLong(publishPriceText) > 0L;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public boolean isPublishPriceFocused() {
        return publishPriceFocused;
    }

    public void focusPublishPrice() {
        publishPriceFocused = true;
    }

    public int getSelectedVaultSlot() { return selectedVaultSlot; }
    public void setSelectedVaultSlot(int slotIndex) { selectedVaultSlot = slotIndex < 0 ? -1 : slotIndex; }

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
