package com.jsirgalaxybase.terminal.client;

import net.minecraft.client.Minecraft;

import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.client.screen.TerminalHomeScreen;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.network.TerminalActionMessage;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;

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
        minecraft.displayGuiScreen(new TerminalHomeScreen(null, model));
        TerminalNetwork.CHANNEL.sendToServer(new TerminalActionMessage(model.getSessionToken(), pageId,
            TerminalActionType.SELECT_PAGE.getId(), "container_route"));
    }

    public static void closeTerminal() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.thePlayer != null) minecraft.thePlayer.closeScreen();
    }
}
