package com.jsirgalaxybase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jsirgalaxybase.bootstrap.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = GalaxyBase.MODID, version = Tags.VERSION, name = "JsirGalaxyBase", acceptedMinecraftVersions = "[1.7.10]")
public class GalaxyBase {

    public static final String MODID = "jsirgalaxybase";
    public static final Logger LOG = LogManager.getLogger(MODID);
    private static final Set<String> IGNORED_DEV_MAPPINGS = new HashSet<String>(Arrays.asList(
        "modularui2:test_block",
        "modularui2:test_item"));

    @Instance(MODID)
    public static GalaxyBase instance;

    @SidedProxy(
        clientSide = "com.jsirgalaxybase.bootstrap.ClientProxy",
        serverSide = "com.jsirgalaxybase.bootstrap.CommonProxy")
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

    @Mod.EventHandler
    public void missingMappings(FMLMissingMappingsEvent event) {
        if (event == null || event.getAll() == null) {
            return;
        }

        for (FMLMissingMappingsEvent.MissingMapping mapping : event.getAll()) {
            if (mapping == null || mapping.name == null || !IGNORED_DEV_MAPPINGS.contains(mapping.name)) {
                continue;
            }

            LOG.warn("Ignoring transient dev mapping {} during Forge missing-mapping reconciliation", mapping.name);
            mapping.ignore();
        }
    }
}
