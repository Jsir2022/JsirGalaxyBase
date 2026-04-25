package com.jsirgalaxybase.terminal.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class TerminalNetwork {

    private static final String CHANNEL_NAME = "jgb_terminal";
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
    private static boolean initialized;

    private TerminalNetwork() {}

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        CHANNEL.registerMessage(OpenTerminalRequestMessage.Handler.class, OpenTerminalRequestMessage.class, 0,
            Side.SERVER);
        CHANNEL.registerMessage(OpenTerminalApprovedMessage.Handler.class, OpenTerminalApprovedMessage.class, 1,
            Side.CLIENT);
        CHANNEL.registerMessage(TerminalActionMessage.Handler.class, TerminalActionMessage.class, 2, Side.SERVER);
        CHANNEL.registerMessage(TerminalSnapshotMessage.Handler.class, TerminalSnapshotMessage.class, 3, Side.CLIENT);
    }
}
