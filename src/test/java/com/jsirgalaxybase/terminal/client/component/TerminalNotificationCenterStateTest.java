package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalNotificationCenterModel;

public class TerminalNotificationCenterStateTest {

    @Test
    public void filtersSourceAndSeverityWithoutChangingServerSnapshot() {
        TerminalNotificationCenterModel model = new TerminalNotificationCenterModel("ready", Arrays.asList(
            new TerminalNotificationCenterModel.EntryModel("market-recovery", "market_account_center", "C1", "市场", "a", "WARNING", 1),
            new TerminalNotificationCenterModel.EntryModel("server-tools", "server_tools", "", "传送", "b", "SUCCESS", 1),
            new TerminalNotificationCenterModel.EntryModel("land-shadow", "property", "", "地产", "c", "WARNING", 1)), 3);
        TerminalNotificationCenterState state = new TerminalNotificationCenterState();
        state.setSource(TerminalNotificationCenterState.SourceFilter.MARKET);
        assertEquals(1, state.filtered(model).size());
        state.setSource(TerminalNotificationCenterState.SourceFilter.ALL);
        state.setSeverity(TerminalNotificationCenterState.SeverityFilter.WARNING);
        assertEquals(2, state.filtered(model).size());
        assertEquals(3, model.getEntries().size());
    }
}
