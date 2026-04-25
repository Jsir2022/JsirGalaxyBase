package com.jsirgalaxybase.module;

import com.jsirgalaxybase.config.ModConfiguration;

public class ModuleContext {

    private final boolean client;
    private final ModConfiguration configuration;
    private final ModuleManager moduleManager;

    public ModuleContext(boolean client, ModConfiguration configuration, ModuleManager moduleManager) {
        this.client = client;
        this.configuration = configuration;
        this.moduleManager = moduleManager;
    }

    public boolean isClient() {
        return client;
    }

    public ModConfiguration getConfiguration() {
        return configuration;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}