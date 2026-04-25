package com.jsirgalaxybase.terminal.client;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import com.jsirgalaxybase.terminal.client.screen.TerminalFrameworkTestScreen;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TerminalFrameworkDebugKeyHandler {

    private static final KeyBinding OPEN_FRAMEWORK_TEST_KEY = new KeyBinding(
        "key.jsirgalaxybase.terminalFrameworkTest",
        Keyboard.KEY_F8,
        "key.categories.jsirgalaxybase");

    private static boolean registered;

    public TerminalFrameworkDebugKeyHandler() {
        if (!registered) {
            registered = true;
            ClientRegistry.registerKeyBinding(OPEN_FRAMEWORK_TEST_KEY);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!OPEN_FRAMEWORK_TEST_KEY.isPressed()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null) {
            return;
        }

        minecraft.displayGuiScreen(new TerminalFrameworkTestScreen(minecraft.currentScreen));
    }
}