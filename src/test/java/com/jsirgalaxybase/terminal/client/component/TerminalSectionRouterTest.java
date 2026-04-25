package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;

public class TerminalSectionRouterTest {

    @Test
    public void resolvesBankSubpageToBankSectionHost() {
        assertEquals("bank", TerminalSectionRouter.resolveSectionPageId("bank_transfer"));
    }

    @Test
    public void resolvesSelectedSnapshotFromModelUsingTopLevelSection() {
        TerminalHomeScreenModel model = TerminalHomeScreenModel.placeholder().withSelectedPageId("market_custom");

        assertEquals("market", TerminalSectionRouter.resolveSnapshot(model).getPageId());
    }
}
