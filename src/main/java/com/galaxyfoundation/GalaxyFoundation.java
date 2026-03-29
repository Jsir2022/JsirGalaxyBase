package com.galaxyfoundation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.galaxyfoundation.bootstrap.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = GalaxyFoundation.MODID, version = Tags.VERSION, name = "Galaxy Foundation", acceptedMinecraftVersions = "[1.7.10]")
public class GalaxyFoundation {

    public static final String MODID = "galaxyfoundation";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(
        clientSide = "com.galaxyfoundation.bootstrap.ClientProxy",
        serverSide = "com.galaxyfoundation.bootstrap.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
