package com.jsirgalaxybase.terminal.client;

import net.minecraft.client.Minecraft;

import com.jsirgalaxybase.terminal.client.screen.TerminalHomeScreen;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TerminalClientScreenController {

    public static final TerminalClientScreenController INSTANCE = new TerminalClientScreenController();

    private TerminalHomeScreenModel pendingHomeScreen;

    private TerminalClientScreenController() {}

    public synchronized void queueHomeScreen(TerminalHomeScreenModel model) {
        if (model != null) {
            pendingHomeScreen = model;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        TerminalHomeScreenModel model = drainPendingHomeScreen();
        if (model == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null) {
            queueHomeScreen(model);
            return;
        }

        if (minecraft.currentScreen instanceof TerminalHomeScreen) {
            ((TerminalHomeScreen) minecraft.currentScreen).applyModel(model);
            return;
        }

        minecraft.displayGuiScreen(new TerminalHomeScreen(minecraft.currentScreen, model));
    }

    private synchronized TerminalHomeScreenModel drainPendingHomeScreen() {
        TerminalHomeScreenModel model = pendingHomeScreen;
        pendingHomeScreen = null;
        return model;
    }
}
