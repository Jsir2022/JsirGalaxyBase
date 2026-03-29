package com.jsirgalaxybase.terminal.network;

import com.jsirgalaxybase.GalaxyBase;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class TerminalNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(GalaxyBase.MODID + ".terminal");
    private static boolean initialized;

    private TerminalNetwork() {}

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        CHANNEL.registerMessage(OpenTerminalMessage.Handler.class, OpenTerminalMessage.class, 0, Side.SERVER);
    }
}