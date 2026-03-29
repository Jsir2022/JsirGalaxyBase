package com.galaxyfoundation.modules.core;

import com.galaxyfoundation.GalaxyFoundation;
import com.galaxyfoundation.module.ModModule;
import com.galaxyfoundation.module.ModuleContext;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class InstitutionCoreModule extends ModModule {

    public InstitutionCoreModule() {
        super("institution-core", "Institution Core", "core");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        GalaxyFoundation.LOG.info(
            "Institution core reserved for profession, economy, reputation, public orders, and transfer state.");
    }
}