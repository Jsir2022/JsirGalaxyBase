package com.jsirgalaxybase.terminal;

import net.minecraftforge.common.MinecraftForge;

import com.jsirgalaxybase.terminal.client.TerminalClientScreenController;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class TerminalClientBootstrap {

    private static boolean initialized;

    private TerminalClientBootstrap() {}

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        FMLCommonHandler.instance().bus().register(new TerminalKeyHandler());
        FMLCommonHandler.instance().bus().register(TerminalClientScreenController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new TerminalInventoryButtonHandler());
        MinecraftForge.EVENT_BUS.register(TerminalHudOverlayHandler.INSTANCE);
    }
}