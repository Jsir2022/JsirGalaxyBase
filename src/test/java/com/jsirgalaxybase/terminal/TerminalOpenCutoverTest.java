package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.network.OpenTerminalApprovedMessage;

public class TerminalOpenCutoverTest {

    @Test
    public void officialApprovalSerializesToNewTerminalHomeScreenModel() {
        TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
            null,
            "home",
            "session-phase9-open",
            TerminalActionType.OPEN_SHELL,
            "");

        TerminalHomeScreenModel model = new OpenTerminalApprovedMessage(approval).toScreenModel();

        assertEquals("home", model.getSelectedPageId());
        assertEquals("home", model.getSelectedSectionPageId());
        assertEquals("session-phase9-open", model.getSessionToken());
        assertTrue(model.getTerminalSubtitle().contains("phase 9"));
        assertEquals("home", model.getPageSnapshots().get(0).getPageId());
        assertNotNull(model.getPageSnapshot("bank").getBankSectionModel());
        assertNotNull(model.getPageSnapshot("market").getMarketSectionModel());
    }

    @Test
    public void approvalStillBuildsAllFormalBusinessPagesForNewShell() {
        TerminalHomeScreenModel bank = buildModel("bank");
        TerminalHomeScreenModel standardized = buildModel("market_standardized");
        TerminalHomeScreenModel custom = buildModel("market_custom");
        TerminalHomeScreenModel exchange = buildModel("market_exchange");

        assertEquals("bank", bank.getSelectedPageId());
        assertNotNull(bank.getSelectedPageSnapshot().getBankSectionModel());
        assertEquals("market_standardized", standardized.getSelectedPageId());
        assertEquals("market_standardized", standardized.getSelectedPageSnapshot().getMarketSectionModel().getRoutePageId());
        assertEquals("market_custom", custom.getSelectedPageId());
        assertNotNull(custom.getSelectedPageSnapshot().getCustomMarketSectionModel());
        assertEquals("market_exchange", exchange.getSelectedPageId());
        assertNotNull(exchange.getSelectedPageSnapshot().getExchangeMarketSectionModel());
    }

    @Test
    public void keyAndInventoryButtonOfficialEntriesSendNewRequestPacketOnly() throws IOException {
        assertOfficialEntrySource("src/main/java/com/jsirgalaxybase/terminal/TerminalKeyHandler.java");
        assertOfficialEntrySource("src/main/java/com/jsirgalaxybase/terminal/TerminalInventoryButtonHandler.java");
    }

    @Test
    public void terminalNetworkOnlyRegistersNewOpenAndSnapshotPackets() throws IOException {
        String source = readSource("src/main/java/com/jsirgalaxybase/terminal/network/TerminalNetwork.java");

        assertTrue(source.contains("OpenTerminalRequestMessage.Handler.class"));
        assertTrue(source.contains("OpenTerminalApprovedMessage.Handler.class"));
        assertTrue(source.contains("TerminalActionMessage.Handler.class"));
        assertTrue(source.contains("TerminalSnapshotMessage.Handler.class"));
        assertFalse(source.contains(legacyOpenPacketName()));
        assertFalse(source.contains("Legacy ModularUI fallback packet"));
    }

    @Test
    public void legacyTerminalFallbackIsRemovedFromProductionEntryPoints() throws IOException {
        String serviceSource = readSource("src/main/java/com/jsirgalaxybase/terminal/TerminalService.java");
        String moduleSource = readSource("src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java");

        assertFalse(serviceSource.contains("openLegacy" + "Terminal"));
        assertFalse(serviceSource.contains("open" + "Terminal("));
        assertFalse(serviceSource.contains(legacyFactoryName()));
        assertFalse(moduleSource.contains("GuiManager." + "registerFactory"));
        assertFalse(moduleSource.contains(legacyFactoryName()));
    }

    private static TerminalHomeScreenModel buildModel(String pageId) {
        TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
            null,
            pageId,
            "session-" + pageId,
            TerminalActionType.SELECT_PAGE,
            "nav_click");
        return new OpenTerminalApprovedMessage(approval).toScreenModel();
    }

    private static void assertOfficialEntrySource(String path) throws IOException {
        String source = readSource(path);

        assertTrue(source.contains("OpenTerminalRequestMessage"));
        assertFalse(source.contains(legacyOpenPacketName()));
        assertFalse(source.contains(legacyFactoryName()));
        assertFalse(source.contains("TerminalService.open" + "Terminal"));
    }

    private static String readSource(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private static String legacyOpenPacketName() {
        return "OpenTerminal" + "Message";
    }

    private static String legacyFactoryName() {
        return "TerminalHomeGui" + "Factory";
    }
}
