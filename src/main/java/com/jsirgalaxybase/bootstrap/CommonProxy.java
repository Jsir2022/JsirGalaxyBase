package com.jsirgalaxybase.bootstrap;

import com.jsirgalaxybase.config.ModConfiguration;
import com.jsirgalaxybase.module.ModuleManager;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    protected final ModBootstrap bootstrap;

    public CommonProxy() {
        this(false);
    }

    protected CommonProxy(boolean client) {
        bootstrap = new ModBootstrap(client);
    }

    public void preInit(FMLPreInitializationEvent event) {
        bootstrap.preInit(event);
    }

    public void init(FMLInitializationEvent event) {
        bootstrap.init(event);
    }

    public void postInit(FMLPostInitializationEvent event) {
        bootstrap.postInit(event);
    }

    public void serverStarting(FMLServerStartingEvent event) {
        bootstrap.serverStarting(event);
    }

    public ModuleManager getModuleManager() {
        return bootstrap.getModuleManager();
    }

    public ModConfiguration getConfiguration() {
        return bootstrap.getConfiguration();
    }
}