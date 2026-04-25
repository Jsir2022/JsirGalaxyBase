package com.jsirgalaxybase.modules.terminal;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.terminal.TerminalClientBootstrap;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class TerminalModule extends ModModule {

    public TerminalModule() {
        super("terminal", "Galaxy Terminal Entry", "interface");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        TerminalNetwork.init();
        GalaxyBase.LOG.info("Initialized terminal network channel");
    }

    @Override
    public void init(ModuleContext context, FMLInitializationEvent event) {
        if (context.isClient()) {
            TerminalClientBootstrap.init();
            GalaxyBase.LOG.info("Registered terminal client entry handlers");
        }
    }
}
