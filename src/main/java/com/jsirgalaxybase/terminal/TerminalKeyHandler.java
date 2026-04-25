package com.jsirgalaxybase.terminal;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;

import com.jsirgalaxybase.terminal.network.OpenTerminalRequestMessage;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TerminalKeyHandler {

    private static final KeyBinding OPEN_TERMINAL_KEY = new KeyBinding(
        TerminalConstants.OPEN_TERMINAL_KEY_DESCRIPTION,
        Keyboard.KEY_G,
        TerminalConstants.OPEN_TERMINAL_KEY_CATEGORY);

    private static boolean registered;

    public TerminalKeyHandler() {
        if (!registered) {
            registered = true;
            ClientRegistry.registerKeyBinding(OPEN_TERMINAL_KEY);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (OPEN_TERMINAL_KEY.isPressed()) {
            TerminalNetwork.CHANNEL.sendToServer(new OpenTerminalRequestMessage());
        }
    }
}