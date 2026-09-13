package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TerminalLandDocumentTest {
    @Test public void allNonContainerTerminalRoutesUseUi2Registry() {
        String[] routes = {"home","career","public_service","market","market_standardized","market_custom",
            "market_exchange","market_account_center","server_tools","property","bank","bank_account",
            "bank_transfer","bank_exchange","bank_ledger","notifications","item_policy"};
        for (String route : routes) assertTrue("route " + route, TerminalPageRegistry.supports(route));
    }

    @Test public void mineTabAndContentUseDifferentStableKeys() {
        assertTrue(!TerminalLandLayoutKeys.TAB_MINE.equals(TerminalLandLayoutKeys.CONTENT_MINE));
    }

}
