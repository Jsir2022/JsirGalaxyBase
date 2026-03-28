package com.u24game.custommod.modules.capability;

import com.u24game.custommod.CustomMod;
import com.u24game.custommod.module.ModModule;
import com.u24game.custommod.module.ModuleContext;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ChainMiningCapabilityModule extends ModModule {

    public ChainMiningCapabilityModule() {
        super("chain-mining", "Chain Mining Capability", "capability");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        CustomMod.LOG.info(
            "Chain mining is reserved as an isolated capability module and must stay server authoritative.");
    }
}