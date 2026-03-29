package com.jsirgalaxybase.terminal;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.terminal.ui.TerminalHomeGuiFactory;

public final class TerminalService {

    private TerminalService() {}

    public static void openTerminal(EntityPlayerMP player) {
        if (player == null) {
            return;
        }

        TerminalHomeGuiFactory.INSTANCE.open(player);
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }
    }
}