package com.jsirgalaxybase.bootstrap;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.command.GalaxyBaseCommand;
import com.jsirgalaxybase.config.ModConfiguration;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.module.ModuleManager;
import com.jsirgalaxybase.modules.capability.ChainMiningCapabilityModule;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.diagnostics.ClientItemDumpModule;
import com.jsirgalaxybase.modules.terminal.TerminalModule;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class ModBootstrap {

    private final boolean client;
    private ModuleManager moduleManager;
    private ModuleContext moduleContext;
    private ModConfiguration configuration;

    public ModBootstrap(boolean client) {
        this.client = client;
    }

    public void preInit(FMLPreInitializationEvent event) {
        configuration = ModConfiguration.load(event.getSuggestedConfigurationFile(), client);
        moduleManager = new ModuleManager();
        moduleContext = new ModuleContext(client, configuration);

        moduleManager.addModule(new InstitutionCoreModule());
        moduleManager.addModule(new ChainMiningCapabilityModule());
        moduleManager.addModule(new TerminalModule());
        if (client) {
            moduleManager.addModule(new ClientItemDumpModule());
        }

        GalaxyBase.LOG.info("Bootstrapping modular runtime (clientSide={}, modules={})", client, moduleManager.size());
        moduleManager.preInit(moduleContext, event);
    }

    public void init(FMLInitializationEvent event) {
        moduleManager.init(moduleContext, event);
    }

    public void postInit(FMLPostInitializationEvent event) {
        moduleManager.postInit(moduleContext, event);
    }

    public void serverStarting(FMLServerStartingEvent event) {
        moduleManager.serverStarting(moduleContext, event);
        event.registerServerCommand(new GalaxyBaseCommand(moduleManager));
        GalaxyBase.LOG.info("Registered /jsirgalaxybase architecture command");
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ModConfiguration getConfiguration() {
        return configuration;
    }
}