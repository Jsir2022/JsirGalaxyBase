package com.jsirgalaxybase.modules.core;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleContext;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class InstitutionCoreModule extends ModModule {

    public InstitutionCoreModule() {
        super("institution-core", "Institution Core", "core");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        GalaxyBase.LOG.info(
            "Institution core reserved for profession, economy, reputation, public orders, and transfer state.");
    }
}