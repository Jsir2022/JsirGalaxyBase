package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;

public class TerminalCustomMarketSectionStateTest {

    @Test
    public void compactSellingScopeLabelRetainsTheServerSellingScope() {
        TerminalCustomMarketSectionState state = new TerminalCustomMarketSectionState();
        state.applyModel(modelForScope("出售"));

        assertEquals("selling", state.toPayload().getSelectedScope());
    }

    @Test
    public void compactPendingScopeLabelRetainsTheServerPendingScope() {
        TerminalCustomMarketSectionState state = new TerminalCustomMarketSectionState();
        state.applyModel(modelForScope("待领"));

        assertEquals("pending", state.toPayload().getSelectedScope());
    }

    @Test
    public void lateCustomSnapshotsCannotReplaceNewerScopeBrowseOrDetailContext() {
        TerminalCustomMarketSectionState state = new TerminalCustomMarketSectionState();
        state.setSelectedScope("selling");
        state.setBrowserQuery("pump");
        state.setBrowserPage(1);

        assertFalse(state.acceptsModel(modelForScope("全部挂牌").withBrowsePage(
            Collections.emptyList(), "pump", 1, 12, 24, true, false)));
        assertFalse(state.acceptsModel(modelForScope("出售").withBrowsePage(
            Collections.emptyList(), "old", 1, 12, 24, true, false)));
        assertTrue(state.acceptsModel(modelForScope("出售").withBrowsePage(
            Collections.emptyList(), "pump", 1, 12, 24, true, false)));

        state.requestDetail("22");
        assertFalse(state.acceptsModel(modelForSelection("21")));
        assertTrue(state.acceptsModel(modelForSelection("22")));
        state.applyModel(modelForSelection("22"));
        assertTrue(state.isDetailView());
        assertFalse(state.acceptsModel(modelForSelection("21")));
    }

    private static TerminalCustomMarketSectionModel modelForScope(String scope) {
        return new TerminalCustomMarketSectionModel(
            "online", "browse", scope,
            Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
            Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
            Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
            "", "未选中挂牌", "--", "--", "--", "--", "--", "--",
            false, false, false, TerminalCustomMarketSectionModel.ActionFeedbackModel.placeholder());
    }

    private static TerminalCustomMarketSectionModel modelForSelection(String listingId) {
        return new TerminalCustomMarketSectionModel(
            "online", "detail", "全部挂牌",
            Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
            Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
            Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
            listingId, "Listing", "100", "ACTIVE", "seller", "item", "trade", "hint",
            true, false, false, TerminalCustomMarketSectionModel.ActionFeedbackModel.placeholder());
    }
}
