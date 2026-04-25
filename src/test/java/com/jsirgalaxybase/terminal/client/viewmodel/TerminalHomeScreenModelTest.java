package com.jsirgalaxybase.terminal.client.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class TerminalHomeScreenModelTest {

    @Test
    public void constructorNormalizesBlankFieldsToStableFallbacks() {
        TerminalHomeScreenModel model = new TerminalHomeScreenModel("", "", "", null, null, null, null, "");

        assertEquals("home", model.getSelectedPageId());
        assertEquals("银河终端", model.getTerminalTitle());
        assertEquals("客户端终端首页", model.getTerminalSubtitle());
        assertEquals("当前页", model.getStatusBand().getEyebrow());
        assertFalse(model.getNavItems().isEmpty());
        assertFalse(model.getPageSnapshots().isEmpty());
        assertFalse(model.getNotifications().isEmpty());
        assertEquals("terminal-session", model.getSessionToken());
    }

    @Test
    public void placeholderProvidesHostReadyShellStructure() {
        TerminalHomeScreenModel model = TerminalHomeScreenModel.placeholder();

        assertEquals("home", model.getSelectedPageId());
        assertEquals("home", model.getSelectedNavigationPageId());
        assertEquals("home", model.getSelectedSectionPageId());
        assertNotNull(model.getSelectedNavItem());
        assertNotNull(model.getSelectedPageSnapshot());
        assertTrue(model.getSelectedNavItem().isSelected());
        assertTrue(model.getPageSnapshots().size() >= 1);
        assertTrue(model.getSelectedPageSnapshot().getSections().size() >= 1);
        assertTrue(model.getNotifications().size() >= 1);
    }

    @Test
    public void selectedPageIdRemainsSingleSourceWhenTargetingNonHomePage() {
        TerminalHomeScreenModel model = new TerminalHomeScreenModel(
            "career",
            "银河终端",
            "客户端终端首页",
            null,
            Arrays.asList(
                new TerminalHomeScreenModel.NavItemModel("home", "首页", "总览", true, true),
                new TerminalHomeScreenModel.NavItemModel("career", "职业", "职业页", true, false),
                new TerminalHomeScreenModel.NavItemModel("bank", "银行", "银行页", true, false)),
            null,
            null,
            "terminal-session");

        assertEquals("career", model.getSelectedPageId());
        assertEquals("career", model.getSelectedNavigationPageId());
        assertEquals("career", model.getSelectedSectionPageId());
        assertEquals("career", model.getSelectedNavItem().getPageId());
        assertEquals("career", model.getSelectedPageSnapshot().getPageId());
        assertTrue(model.getSelectedNavItem().isSelected());
        assertFalse(model.getNavItems().get(0).isSelected());
        assertTrue(model.getNavItems().get(1).isSelected());
    }

    @Test
    public void modelDerivesTopLevelNavigationAndSectionFromSingleSelectedPageId() {
        TerminalHomeScreenModel model = new TerminalHomeScreenModel(
            "bank_transfer",
            "银河终端",
            "客户端终端首页",
            null,
            Arrays.asList(
                new TerminalHomeScreenModel.NavItemModel("home", "首页", "总览", true, false),
                new TerminalHomeScreenModel.NavItemModel("market", "市场", "总入口", true, false),
                new TerminalHomeScreenModel.NavItemModel("bank", "银行", "银行页", true, false)),
            null,
            null,
            "terminal-session");

        assertEquals("bank_transfer", model.getSelectedPageId());
        assertEquals("bank", model.getSelectedNavigationPageId());
        assertEquals("bank", model.getSelectedSectionPageId());
        assertEquals("bank", model.getSelectedNavItem().getPageId());
        assertEquals("bank", model.getSelectedPageSnapshot().getPageId());
        assertTrue(model.getSelectedNavItem().isSelected());
        assertFalse(model.getNavItems().get(0).isSelected());
        assertFalse(model.getNavItems().get(1).isSelected());
        assertTrue(model.getNavItems().get(2).isSelected());
    }

    @Test
    public void navSelectedFlagIsDerivedFromSelectedPageIdRatherThanTrustedAsInput() {
        TerminalHomeScreenModel model = new TerminalHomeScreenModel(
            "market",
            "银河终端",
            "客户端终端首页",
            null,
            Arrays.asList(
                new TerminalHomeScreenModel.NavItemModel("home", "首页", "总览", true, true),
                new TerminalHomeScreenModel.NavItemModel("market", "市场", "总入口", true, false),
                new TerminalHomeScreenModel.NavItemModel("bank", "银行", "银行页", true, true)),
            null,
            null,
            "terminal-session");

        assertEquals("market", model.getSelectedPageId());
        assertEquals("market", model.getSelectedNavigationPageId());
        assertFalse(model.getNavItems().get(0).isSelected());
        assertTrue(model.getNavItems().get(1).isSelected());
        assertFalse(model.getNavItems().get(2).isSelected());
    }
}
