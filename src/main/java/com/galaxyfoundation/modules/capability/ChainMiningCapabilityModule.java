package com.galaxyfoundation.modules.capability;

import com.galaxyfoundation.GalaxyFoundation;
import com.galaxyfoundation.module.ModModule;
import com.galaxyfoundation.module.ModuleContext;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ChainMiningCapabilityModule extends ModModule {

    public ChainMiningCapabilityModule() {
        super("chain-mining", "Chain Mining Capability", "capability");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        GalaxyFoundation.LOG.info(
            "Chain mining is reserved as an isolated capability module and must stay server authoritative.");
    }
}