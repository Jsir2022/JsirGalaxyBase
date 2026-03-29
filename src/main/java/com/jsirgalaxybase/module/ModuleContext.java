package com.jsirgalaxybase.module;

import com.jsirgalaxybase.config.ModConfiguration;

public class ModuleContext {

    private final boolean client;
    private final ModConfiguration configuration;

    public ModuleContext(boolean client, ModConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
    }

    public boolean isClient() {
        return client;
    }

    public ModConfiguration getConfiguration() {
        return configuration;
    }
}