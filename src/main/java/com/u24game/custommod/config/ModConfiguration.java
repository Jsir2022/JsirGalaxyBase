package com.u24game.custommod.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class ModConfiguration {

    private final File minecraftDirectory;
    private final boolean autoDumpItemsOnClientStart;
    private final String itemDumpDirectory;

    private ModConfiguration(File minecraftDirectory, boolean autoDumpItemsOnClientStart, String itemDumpDirectory) {
        this.minecraftDirectory = minecraftDirectory;
        this.autoDumpItemsOnClientStart = autoDumpItemsOnClientStart;
        this.itemDumpDirectory = itemDumpDirectory;
    }

    public static ModConfiguration load(File configFile) {
        final Configuration configuration = new Configuration(configFile);
        final File minecraftDirectory = configFile.getParentFile().getParentFile();

        final boolean autoDumpItemsOnClientStart = configuration.getBoolean(
            "autoDumpItemsOnClientStart",
            Configuration.CATEGORY_GENERAL,
            true,
            "When true, the client exports registry and NEI item lists after startup.");
        final String itemDumpDirectory = configuration.getString(
            "itemDumpDirectory",
            Configuration.CATEGORY_GENERAL,
            "custommod/item_dumps",
            "Relative path under the Minecraft directory where item dump files are written.");

        if (configuration.hasChanged()) {
            configuration.save();
        }

        return new ModConfiguration(minecraftDirectory, autoDumpItemsOnClientStart, itemDumpDirectory);
    }

    public File getMinecraftDirectory() {
        return minecraftDirectory;
    }

    public boolean isAutoDumpItemsOnClientStart() {
        return autoDumpItemsOnClientStart;
    }

    public String getItemDumpDirectory() {
        return itemDumpDirectory;
    }
}