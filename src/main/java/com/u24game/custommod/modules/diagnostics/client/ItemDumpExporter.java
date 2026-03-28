package com.u24game.custommod.modules.diagnostics.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.gson.stream.JsonWriter;
import com.u24game.custommod.CustomMod;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;

public final class ItemDumpExporter {

    private ItemDumpExporter() {}

    public static boolean isNeiItemListReady() {
        try {
            final Class<?> itemListClass = Class.forName("codechicken.nei.ItemList");
            final boolean loadFinished = itemListClass.getField("loadFinished").getBoolean(null);
            final Object itemList = itemListClass.getField("items").get(null);
            return loadFinished && itemList instanceof List && !((List<?>) itemList).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static File exportAll(File minecraftDirectory, String relativeDumpDirectory, boolean includeNei)
        throws IOException {
        if (minecraftDirectory == null) {
            throw new IOException("Minecraft directory is not configured");
        }

        final File rootDirectory = new File(minecraftDirectory, relativeDumpDirectory);
        if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
            throw new IOException("Failed to create dump directory " + rootDirectory.getAbsolutePath());
        }

        final File sessionDirectory = new File(
            rootDirectory,
            new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date()));
        if (!sessionDirectory.exists() && !sessionDirectory.mkdirs()) {
            throw new IOException("Failed to create dump session directory " + sessionDirectory.getAbsolutePath());
        }

        final Map<String, String> modNames = buildModNameMap();
        final List<DumpEntry> registryEntries = collectRegistryEntries(modNames);
        writeCsv(new File(sessionDirectory, "registry_items.csv"), registryEntries);
        writeJson(new File(sessionDirectory, "registry_items.json"), registryEntries);

        int neiCount = 0;
        if (includeNei) {
            final List<DumpEntry> neiEntries = collectNeiEntries(modNames);
            neiCount = neiEntries.size();
            writeCsv(new File(sessionDirectory, "nei_items.csv"), neiEntries);
            writeJson(new File(sessionDirectory, "nei_items.json"), neiEntries);
        }

        writeSummary(new File(sessionDirectory, "summary.json"), registryEntries.size(), neiCount, includeNei);
        CustomMod.LOG.info(
            "Item dump exported to {} (registry={}, nei={})",
            sessionDirectory.getAbsolutePath(),
            registryEntries.size(),
            neiCount);
        return sessionDirectory;
    }

    private static Map<String, String> buildModNameMap() {
        final Map<String, String> modNames = new TreeMap<String, String>();
        for (ModContainer modContainer : Loader.instance().getModList()) {
            modNames.put(modContainer.getModId(), modContainer.getName());
        }
        return modNames;
    }

    @SuppressWarnings("unchecked")
    private static List<DumpEntry> collectRegistryEntries(Map<String, String> modNames) {
        final List<DumpEntry> entries = new ArrayList<DumpEntry>();
        final Set<String> keys = (Set<String>) GameData.getItemRegistry().getKeys();
        for (String key : keys) {
            final Item item = (Item) GameData.getItemRegistry().getObject(key);
            if (item == null) {
                continue;
            }

            final GameRegistry.UniqueIdentifier uniqueIdentifier = GameRegistry.findUniqueIdentifierFor(item);
            final String registryName = uniqueIdentifier == null ? key : uniqueIdentifier.modId + ":" + uniqueIdentifier.name;
            final String modId = uniqueIdentifier == null ? extractModId(key) : uniqueIdentifier.modId;
            final String modName = modNames.containsKey(modId) ? modNames.get(modId) : "";

            final DumpEntry entry = new DumpEntry();
            entry.source = "registry";
            entry.registryName = registryName;
            entry.modId = modId;
            entry.modName = modName;
            entry.numericId = Item.getIdFromItem(item);
            entry.meta = 0;
            entry.hasNbt = false;
            entry.hidden = false;
            entry.displayName = safeDisplayName(new ItemStack(item, 1, 0));
            entry.unlocalizedName = safeUnlocalizedName(new ItemStack(item, 1, 0));
            entry.oreDict = "";
            entry.itemClass = item.getClass().getName();
            entry.maxStackSize = item.getItemStackLimit(new ItemStack(item, 1, 0));
            entry.maxDamage = item.getMaxDamage();
            entry.hasSubtypes = item.getHasSubtypes();
            entries.add(entry);
        }
        Collections.sort(entries);
        return entries;
    }

    @SuppressWarnings("unchecked")
    private static List<DumpEntry> collectNeiEntries(Map<String, String> modNames) {
        final Map<String, DumpEntry> uniqueEntries = new LinkedHashMap<String, DumpEntry>();

        try {
            final Class<?> itemListClass = Class.forName("codechicken.nei.ItemList");
            final List<ItemStack> neiItems = (List<ItemStack>) itemListClass.getField("items").get(null);
            if (neiItems == null) {
                return new ArrayList<DumpEntry>();
            }

            for (ItemStack stack : neiItems) {
                if (stack == null || stack.getItem() == null) {
                    continue;
                }

                final DumpEntry entry = createEntryFromStack(stack, modNames, "nei");
                final String key = buildEntryKey(entry);
                if (!uniqueEntries.containsKey(key)) {
                    uniqueEntries.put(key, entry);
                }
            }
        } catch (Exception e) {
            CustomMod.LOG.warn("Failed to read NEI item list, falling back to empty export", e);
        }

        final List<DumpEntry> entries = new ArrayList<DumpEntry>(uniqueEntries.values());
        Collections.sort(entries);
        return entries;
    }

    private static DumpEntry createEntryFromStack(ItemStack stack, Map<String, String> modNames, String source) {
        final Item item = stack.getItem();
        final GameRegistry.UniqueIdentifier uniqueIdentifier = GameRegistry.findUniqueIdentifierFor(item);
        final String registryName;
        final String modId;
        if (uniqueIdentifier == null) {
            registryName = String.valueOf(GameData.getItemRegistry().getNameForObject(item));
            modId = extractModId(registryName);
        } else {
            registryName = uniqueIdentifier.modId + ":" + uniqueIdentifier.name;
            modId = uniqueIdentifier.modId;
        }

        final DumpEntry entry = new DumpEntry();
        entry.source = source;
        entry.registryName = registryName;
        entry.modId = modId;
        entry.modName = modNames.containsKey(modId) ? modNames.get(modId) : "";
        entry.numericId = Item.getIdFromItem(item);
        entry.meta = normalizeMeta(stack);
        entry.hasNbt = stack.stackTagCompound != null;
        entry.nbt = stack.stackTagCompound == null ? "" : stack.stackTagCompound.toString();
        entry.hidden = isNeiHidden(stack);
        entry.displayName = safeDisplayName(stack);
        entry.unlocalizedName = safeUnlocalizedName(stack);
        entry.oreDict = collectOreDictNames(stack);
        entry.itemClass = item.getClass().getName();
        entry.maxStackSize = item.getItemStackLimit(stack);
        entry.maxDamage = item.getMaxDamage();
        entry.hasSubtypes = item.getHasSubtypes();
        return entry;
    }

    private static void writeSummary(File file, int registryCount, int neiCount, boolean includeNei) throws IOException {
        try (JsonWriter writer = new JsonWriter(
            new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)))) {
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("generated_at").value(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()));
            writer.name("registry_count").value(registryCount);
            writer.name("nei_enabled").value(includeNei);
            writer.name("nei_count").value(neiCount);
            writer.endObject();
        }
    }

    private static void writeJson(File file, List<DumpEntry> entries) throws IOException {
        try (JsonWriter writer = new JsonWriter(
            new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)))) {
            writer.setIndent("  ");
            writer.beginArray();
            for (DumpEntry entry : entries) {
                writer.beginObject();
                writer.name("source").value(entry.source);
                writer.name("registry_name").value(entry.registryName);
                writer.name("mod_id").value(entry.modId);
                writer.name("mod_name").value(entry.modName);
                writer.name("numeric_id").value(entry.numericId);
                writer.name("meta").value(entry.meta);
                writer.name("has_nbt").value(entry.hasNbt);
                writer.name("hidden").value(entry.hidden);
                writer.name("display_name").value(entry.displayName);
                writer.name("unlocalized_name").value(entry.unlocalizedName);
                writer.name("ore_dict").value(entry.oreDict);
                writer.name("item_class").value(entry.itemClass);
                writer.name("max_stack_size").value(entry.maxStackSize);
                writer.name("max_damage").value(entry.maxDamage);
                writer.name("has_subtypes").value(entry.hasSubtypes);
                writer.name("nbt").value(entry.nbt);
                writer.endObject();
            }
            writer.endArray();
        }
    }

    private static void writeCsv(File file, List<DumpEntry> entries) throws IOException {
        try (Writer writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(
                "source,registry_name,mod_id,mod_name,numeric_id,meta,has_nbt,hidden,display_name,unlocalized_name,ore_dict,item_class,max_stack_size,max_damage,has_subtypes\n");
            for (DumpEntry entry : entries) {
                writer.write(csv(entry.source));
                writer.write(',');
                writer.write(csv(entry.registryName));
                writer.write(',');
                writer.write(csv(entry.modId));
                writer.write(',');
                writer.write(csv(entry.modName));
                writer.write(',');
                writer.write(Integer.toString(entry.numericId));
                writer.write(',');
                writer.write(Integer.toString(entry.meta));
                writer.write(',');
                writer.write(Boolean.toString(entry.hasNbt));
                writer.write(',');
                writer.write(Boolean.toString(entry.hidden));
                writer.write(',');
                writer.write(csv(entry.displayName));
                writer.write(',');
                writer.write(csv(entry.unlocalizedName));
                writer.write(',');
                writer.write(csv(entry.oreDict));
                writer.write(',');
                writer.write(csv(entry.itemClass));
                writer.write(',');
                writer.write(Integer.toString(entry.maxStackSize));
                writer.write(',');
                writer.write(Integer.toString(entry.maxDamage));
                writer.write(',');
                writer.write(Boolean.toString(entry.hasSubtypes));
                writer.write('\n');
            }
        }
    }

    private static String collectOreDictNames(ItemStack stack) {
        try {
            final int[] ids = OreDictionary.getOreIDs(stack);
            if (ids == null || ids.length == 0) {
                return "";
            }

            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < ids.length; i++) {
                if (i > 0) {
                    builder.append('|');
                }
                builder.append(OreDictionary.getOreName(ids[i]));
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static int normalizeMeta(ItemStack stack) {
        final int meta = stack.getItemDamage();
        return meta < 0 ? 0 : meta;
    }

    private static boolean isNeiHidden(ItemStack stack) {
        try {
            final Class<?> apiClass = Class.forName("codechicken.nei.api.API");
            final Method method = apiClass.getMethod("isItemHidden", ItemStack.class);
            final Object result = method.invoke(null, stack);
            return result instanceof Boolean && ((Boolean) result).booleanValue();
        } catch (Exception e) {
            return false;
        }
    }

    private static String safeDisplayName(ItemStack stack) {
        try {
            return stack.getDisplayName();
        } catch (Exception e) {
            return "<error>";
        }
    }

    private static String safeUnlocalizedName(ItemStack stack) {
        try {
            return stack.getUnlocalizedName();
        } catch (Exception e) {
            return "<error>";
        }
    }

    private static String extractModId(String registryName) {
        final int separator = registryName.indexOf(':');
        return separator <= 0 ? "unknown" : registryName.substring(0, separator);
    }

    private static String buildEntryKey(DumpEntry entry) {
        return entry.registryName + "#" + entry.meta + "#" + entry.nbt;
    }

    private static String csv(String value) {
        final String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

    private static final class DumpEntry implements Comparable<DumpEntry> {

        private String source;
        private String registryName;
        private String modId;
        private String modName;
        private int numericId;
        private int meta;
        private boolean hasNbt;
        private String nbt = "";
        private boolean hidden;
        private String displayName;
        private String unlocalizedName;
        private String oreDict;
        private String itemClass;
        private int maxStackSize;
        private int maxDamage;
        private boolean hasSubtypes;

        @Override
        public int compareTo(DumpEntry other) {
            int compare = registryName.compareTo(other.registryName);
            if (compare != 0) {
                return compare;
            }
            compare = Integer.valueOf(meta).compareTo(Integer.valueOf(other.meta));
            if (compare != 0) {
                return compare;
            }
            return nbt.compareTo(other.nbt);
        }
    }
}