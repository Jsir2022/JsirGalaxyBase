package com.galaxyfoundation.modules.diagnostics;

import net.minecraftforge.common.MinecraftForge;

import com.galaxyfoundation.GalaxyFoundation;
import com.galaxyfoundation.module.ModModule;
import com.galaxyfoundation.module.ModuleContext;
import com.galaxyfoundation.modules.diagnostics.client.ClientItemDumpController;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;

public class ClientItemDumpModule extends ModModule {

    public ClientItemDumpModule() {
        super("client-item-dump", "Client Item Dump", "diagnostics");
    }

    @Override
    public void postInit(ModuleContext context, FMLPostInitializationEvent event) {
        if (!context.isClient() || !context.getConfiguration().isAutoDumpItemsOnClientStart()) {
            return;
        }

        final ClientItemDumpController controller = new ClientItemDumpController(context.getConfiguration());
        FMLCommonHandler.instance().bus().register(controller);
        MinecraftForge.EVENT_BUS.register(controller);
        GalaxyFoundation.LOG.info("Registered client item dump diagnostics module");
    }
}