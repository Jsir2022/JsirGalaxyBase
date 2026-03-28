package com.u24game.custommod.bootstrap;

import com.u24game.custommod.CustomMod;
import com.u24game.custommod.command.CustomModCommand;
import com.u24game.custommod.config.ModConfiguration;
import com.u24game.custommod.module.ModuleContext;
import com.u24game.custommod.module.ModuleManager;
import com.u24game.custommod.modules.capability.ChainMiningCapabilityModule;
import com.u24game.custommod.modules.core.InstitutionCoreModule;
import com.u24game.custommod.modules.diagnostics.ClientItemDumpModule;

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

        CustomMod.LOG.info("Bootstrapping modular runtime (clientSide={}, modules={})", client, moduleManager.size());
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
        event.registerServerCommand(new CustomModCommand(moduleManager));
        CustomMod.LOG.info("Registered /custommod architecture command");
    }
}