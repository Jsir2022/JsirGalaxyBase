package com.galaxyfoundation.bootstrap;

import com.galaxyfoundation.GalaxyFoundation;
import com.galaxyfoundation.command.GalaxyFoundationCommand;
import com.galaxyfoundation.config.ModConfiguration;
import com.galaxyfoundation.module.ModuleContext;
import com.galaxyfoundation.module.ModuleManager;
import com.galaxyfoundation.modules.capability.ChainMiningCapabilityModule;
import com.galaxyfoundation.modules.core.InstitutionCoreModule;
import com.galaxyfoundation.modules.diagnostics.ClientItemDumpModule;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class ModBootstrap {

    private final boolean client;
    private ModuleManager moduleManager;
    private ModuleContext moduleContext;

    public ModBootstrap(boolean client) {
        this.client = client;
    }

    public void preInit(FMLPreInitializationEvent event) {
        final ModConfiguration configuration = ModConfiguration.load(event.getSuggestedConfigurationFile());
        moduleManager = new ModuleManager();
        moduleContext = new ModuleContext(client, configuration);

        moduleManager.addModule(new InstitutionCoreModule());
        moduleManager.addModule(new ChainMiningCapabilityModule());
        if (client) {
            moduleManager.addModule(new ClientItemDumpModule());
        }

        GalaxyFoundation.LOG.info("Bootstrapping modular runtime (clientSide={}, modules={})", client, moduleManager.size());
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
        event.registerServerCommand(new GalaxyFoundationCommand(moduleManager));
        GalaxyFoundation.LOG.info("Registered /galaxyfoundation architecture command");
    }
}