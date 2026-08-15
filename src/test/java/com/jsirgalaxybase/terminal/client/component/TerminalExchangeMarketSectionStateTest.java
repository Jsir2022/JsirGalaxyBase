package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;

public class TerminalExchangeMarketSectionStateTest {

    @Test
    public void lateDetailSnapshotCannotOpenOrReplaceNewerSelection() {
        TerminalExchangeMarketSectionState state = new TerminalExchangeMarketSectionState();
        state.requestDetail("coin-a");
        state.requestDetail("coin-b");

        TerminalExchangeMarketSectionModel coinA = model("coin-a");
        TerminalExchangeMarketSectionModel coinB = model("coin-b");

        assertFalse(state.acceptsModel(coinA));
        assertFalse(state.isDetailView());
        assertTrue(state.acceptsModel(coinB));
        state.applyModel(coinB);
        assertTrue(state.isDetailView());

        assertFalse(state.acceptsModel(coinA));
        assertTrue(state.acceptsModel(coinB));
    }

    @Test
    public void browseModeAcceptsOrdinaryRefreshSnapshots() {
        TerminalExchangeMarketSectionState state = new TerminalExchangeMarketSectionState();
        assertTrue(state.acceptsModel(model("coin-a")));
        state.requestDetail("coin-a");
        state.returnToBrowse();
        assertTrue(state.acceptsModel(model("coin-b")));
    }

    @Test
    public void lateBrowseSnapshotCannotReplaceNewerExchangeQueryOrPage() {
        TerminalExchangeMarketSectionState state = new TerminalExchangeMarketSectionState();
        state.setBrowserQuery("wizard");
        state.setBrowserPage(2);

        assertFalse(state.acceptsModel(model("", "old", 2)));
        assertFalse(state.acceptsModel(model("", "wizard", 1)));
        assertTrue(state.acceptsModel(model("", "wizard", 2)));
        assertEquals("wizard", state.getBrowserQuery());
        assertEquals(2, state.getBrowserPage());
    }

    @Test
    public void exchangeLimitStatusUsesPlayerFacingLabels() {
        assertEquals("暂不可兑换", modelWithStatus("UNAVAILABLE").getLimitStatusDisplay());
        assertEquals("可兑换", modelWithStatus("ACTIVE").getLimitStatusDisplay());
        assertEquals("报价已过期", modelWithStatus("EXPIRED").getLimitStatusDisplay());
        assertEquals("超出兑换限额", modelWithStatus("LIMIT_EXCEEDED").getLimitStatusDisplay());
        assertEquals("状态待确认", modelWithStatus("INTERNAL_PENDING").getLimitStatusDisplay());
    }

    private static TerminalExchangeMarketSectionModel model(String selectedCoinCode) {
        return model(selectedCoinCode, "", 0);
    }

    private static TerminalExchangeMarketSectionModel model(String selectedCoinCode, String query, int page) {
        return new TerminalExchangeMarketSectionModel("在线", "", Arrays.asList("TASK_COIN"),
            Arrays.asList("任务书硬币"), "TASK_COIN", selectedCoinCode, "任务书硬币", "", "", "", "--", "--",
            "--", "UNAVAILABLE", "--", "", "0", "0", "0", "0", "", "--", "", "", false, null)
            .withBrowsePage(Collections.emptyList(), query, page, 12, 36, page > 0, page < 2);
    }

    private static TerminalExchangeMarketSectionModel modelWithStatus(String status) {
        return new TerminalExchangeMarketSectionModel("在线", "", Arrays.asList("TASK_COIN"),
            Arrays.asList("任务书硬币"), "TASK_COIN", "coin-a", "任务书硬币", "", "", "", "--", "--",
            "--", "--", status, "--", "", "0", "0", "0", "0", "", "--", "", false, null);
    }
}
