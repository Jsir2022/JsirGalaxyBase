package com.jsirgalaxybase.module;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public abstract class ModModule {

    private final String id;
    private final String displayName;
    private final String category;

    protected ModModule(String id, String displayName, String category) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
    }

    public final String getId() {
        return id;
    }

    public final String getDisplayName() {
        return displayName;
    }

    public final String getCategory() {
        return category;
    }

    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {}

    public void init(ModuleContext context, FMLInitializationEvent event) {}

    public void postInit(ModuleContext context, FMLPostInitializationEvent event) {}

    public void serverStarting(ModuleContext context, FMLServerStartingEvent event) {}
}