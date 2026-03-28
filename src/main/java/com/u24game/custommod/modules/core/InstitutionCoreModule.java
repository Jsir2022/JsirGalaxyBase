package com.u24game.custommod.modules.core;

import com.u24game.custommod.CustomMod;
import com.u24game.custommod.module.ModModule;
import com.u24game.custommod.module.ModuleContext;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class InstitutionCoreModule extends ModModule {

    public InstitutionCoreModule() {
        super("institution-core", "Institution Core", "core");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        CustomMod.LOG.info(
            "Institution core reserved for profession, economy, reputation, public orders, and transfer state.");
    }
}