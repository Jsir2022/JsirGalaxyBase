package com.jsirgalaxybase.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.GalaxyBase;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class ModuleManager {

    private final List<ModModule> modules = new ArrayList<ModModule>();

    public void addModule(ModModule module) {
        modules.add(module);
    }

    public int size() {
        return modules.size();
    }

    public List<ModModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public <T extends ModModule> T findModule(Class<T> moduleClass) {
        for (ModModule module : modules) {
            if (moduleClass.isInstance(module)) {
                return moduleClass.cast(module);
            }
        }
        return null;
    }

    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        for (ModModule module : modules) {
            GalaxyBase.LOG.info("[Module:{}] preInit", module.getId());
            module.preInit(context, event);
        }
    }

    public void init(ModuleContext context, FMLInitializationEvent event) {
        for (ModModule module : modules) {
            GalaxyBase.LOG.info("[Module:{}] init", module.getId());
            module.init(context, event);
        }
    }

    public void postInit(ModuleContext context, FMLPostInitializationEvent event) {
        for (ModModule module : modules) {
            GalaxyBase.LOG.info("[Module:{}] postInit", module.getId());
            module.postInit(context, event);
        }
    }

    public void serverStarting(ModuleContext context, FMLServerStartingEvent event) {
        for (ModModule module : modules) {
            GalaxyBase.LOG.info("[Module:{}] serverStarting", module.getId());
            module.serverStarting(context, event);
        }
    }
}