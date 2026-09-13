package com.jsirgalaxybase.terminal.client;

import net.minecraft.client.Minecraft;

import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.client.screen.TerminalApplicationScreen;
import com.jsirgalaxybase.terminal.client.screen.TerminalSettingsScreen;
import com.jsirgalaxybase.terminal.client.ui2.TerminalAppShell;
import com.jsirgalaxybase.terminal.client.ui2.TerminalPageRegistry;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.network.TerminalActionMessage;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

/** Same-tick handoff between a native Container and the retained terminal shell. */
public final class TerminalRouteCoordinator {

    private TerminalRouteCoordinator() {}

    public static void openTerminalPage(String pageId) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null) return;
        TerminalHomeScreenModel model = TerminalClientScreenController.INSTANCE.getLastHomeScreen(pageId);
        // Close the native window on the server, then replace it immediately in the same client tick.
        // No game-world frame is rendered between these calls.
        if (minecraft.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
            minecraft.thePlayer.closeScreen();
        }
        if (TerminalAppShell.SETTINGS_PAGE_ID.equals(pageId)) {
            minecraft.displayGuiScreen(new TerminalSettingsScreen(null,
                TerminalClientScreenController.INSTANCE.getLastHomeScreen(TerminalPage.HOME.getId())));
            return;
        }
        TerminalHomeScreenModel target = TerminalPageRegistry.supports(model.getSelectedPageId())
            ? model : model.withSelectedPageId(TerminalPage.HOME.getId());
        minecraft.displayGuiScreen(new TerminalApplicationScreen(null, target));
        TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(model.getSessionToken(), pageId,
            TerminalActionType.SELECT_PAGE.getId(), "container_route"));
    }

    public static void closeTerminal() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.thePlayer != null) minecraft.thePlayer.closeScreen();
    }
}
