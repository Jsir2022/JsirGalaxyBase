package com.jsirgalaxybase.terminal.client.component;

import java.util.List;

import com.jsirgalaxybase.terminal.TerminalCustomMarketActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;

public final class TerminalCustomMarketSectionState {

    private String selectedScope = "active";
    private String selectedListingId = "";
    private String publishPriceText = "";
    private boolean publishPriceFocused;
    private boolean browserQueryFocused;
    private int selectedVaultSlot = -1;
    private final MarketBrowseDetailController browser = new MarketBrowseDetailController();
    private String pendingDetailListingId = "";

    public void applyModel(TerminalCustomMarketSectionModel model) {
        if (model == null) {
            selectedScope = "active";
            selectedListingId = "";
            publishPriceText = "";
            publishPriceFocused = false;
            browserQueryFocused = false;
            selectedVaultSlot = -1;
            browser.reset();
            pendingDetailListingId = "";
            return;
        }
        selectedScope = scopeFromLabel(model.getScopeLabel());
        // A browse refresh must not silently promote the first result into a selected listing.
        // Details are entered only after the player clicks a real grid item.
        selectedListingId = sanitizeNumber(model.getSelectedListingId());
        if (!pendingDetailListingId.isEmpty() && pendingDetailListingId.equals(selectedListingId)) {
            browser.openDetail(selectedListingId);
            pendingDetailListingId = "";
        }
    }

    /** Rejects snapshots produced for an older scope, browse context, or listing selection. */
    public boolean acceptsModel(TerminalCustomMarketSectionModel model) {
        if (model == null) {
            return true;
        }
        String responseListingId = sanitizeNumber(model.getSelectedListingId());
        if (!pendingDetailListingId.isEmpty()) {
            return pendingDetailListingId.equals(responseListingId);
        }
        if (browser.isDetail()) {
            return browser.getSelectedKey().equals(responseListingId);
        }
        return selectedScope.equals(scopeFromLabel(model.getScopeLabel()))
            && browser.getQuery().equals(normalize(model.getBrowseQuery()))
            && browser.getPageIndex() == Math.max(0, model.getBrowsePageIndex());
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
        pendingDetailListingId = this.selectedListingId;
    }

    public boolean isDetailView() { return browser.isDetail(); }
    public void returnToBrowse() { browser.openBrowse(); pendingDetailListingId = ""; publishPriceFocused = false; browserQueryFocused = false; }
    public String getBrowserQuery() { return browser.getQuery(); }
    public void setBrowserQuery(String value) { browser.setQuery(value); }
    public boolean isBrowserQueryFocused() { return browserQueryFocused; }
    public void focusBrowserQuery() { browserQueryFocused = true; }
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

    private String scopeFromLabel(String label) {
        if ("我的出售".equals(label) || "出售".equals(label)) {
            return "selling";
        }
        if ("我的待领取".equals(label) || "我的待处理".equals(label) || "待领".equals(label)) {
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

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
