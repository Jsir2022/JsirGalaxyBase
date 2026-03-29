package com.jsirgalaxybase.modules.capability;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleContext;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ChainMiningCapabilityModule extends ModModule {

    public ChainMiningCapabilityModule() {
        super("chain-mining", "Chain Mining Capability", "capability");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        GalaxyBase.LOG.info(
            "Chain mining is reserved as an isolated capability module and must stay server authoritative.");
    }
}