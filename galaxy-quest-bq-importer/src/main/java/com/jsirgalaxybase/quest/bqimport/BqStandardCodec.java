package com.jsirgalaxybase.quest.bqimport;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class BqStandardCodec {
    static Set<String> supportedTaskTypes() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
            "bq_standard:block_break", "bq_standard:checkbox", "bq_standard:crafting", "bq_standard:fluid",
            "bq_standard:hunt", "bq_standard:interact_entity", "bq_standard:interact_item",
            "bq_standard:location", "bq_standard:meeting", "bq_standard:optional_retrieval",
            "bq_standard:retrieval", "bq_standard:scoreboard", "bq_standard:xp")));
    }

    static Set<String> supportedRewardTypes() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
            "bq_standard:choice", "bq_standard:command", "bq_standard:item", "bq_standard:questcompletion",
            "bq_standard:scoreboard", "bq_standard:xp")));
    }

    private final Gson gson;

    BqStandardCodec(Gson gson) { this.gson = gson; }

    BqStandardTaskSpec task(JsonObject value, int fallbackIndex) {
        int index = (int) BqTypedJson.number(value, "index", fallbackIndex);
        String type = BqTypedJson.string(value, "taskID", "betterquesting:unknown");
        Map<String, String> options = scalarOptions(value, "taskID", "index");
        return new BqStandardTaskSpec("task-" + index, type, options, items(value, "requiredItems"),
            fluids(value, "requiredFluids"), blocks(value, "blocks"));
    }

    BqStandardRewardSpec reward(JsonObject value, int fallbackIndex) {
        int index = (int) BqTypedJson.number(value, "index", fallbackIndex);
        String type = BqTypedJson.string(value, "rewardID", "betterquesting:unknown");
        Map<String, String> options = scalarOptions(value, "rewardID", "index");
        String compactQuestId = BqTypedJson.string(value, "questID", "");
        if (!compactQuestId.isEmpty()) options.put("questUuid", decodeCompactUuid(compactQuestId).toString());
        String itemField = "bq_standard:choice".equals(type) ? "choices" : "rewards";
        return new BqStandardRewardSpec("reward-" + index, type, options, items(value, itemField));
    }

    Map<String, String> taskParameters(BqStandardTaskSpec spec, String rawJson) {
        Map<String, String> result = new LinkedHashMap<String, String>(spec.getOptions());
        result.put("bq.raw", rawJson);
        String type = spec.getTypeId();
        if ("bq_standard:hunt".equals(type) || "bq_standard:meeting".equals(type)) {
            String subject = result.remove("target");
            if (subject != null) result.put("subject", subject);
            JsonObject raw = gson.fromJson(rawJson, JsonObject.class);
            result.put("targetNBT.portable", BqPortableNbtCodec.encodeCompound(BqTypedJson.object(raw, "targetNBT")));
        }
        if ("bq_standard:hunt".equals(type)) {
            result.put("progressMode", "scalar");
            result.put("factType", "galaxy:entity_killed");
            result.put("target", valueOrDefault(result, "required", "1"));
        } else if ("bq_standard:retrieval".equals(type) || "bq_standard:optional_retrieval".equals(type)
            || "bq_standard:crafting".equals(type)) {
            result.put("progressMode", "item-vector");
            if ("bq_standard:crafting".equals(type)) result.put("factType", "galaxy:item_produced");
            else result.put("factType", "galaxy:inventory_observed");
            appendItems(result, spec.getItems());
            result.put("target", Long.toString(sumItemTargets(spec.getItems())));
        } else if ("bq_standard:fluid".equals(type)) {
            result.put("progressMode", "fluid-vector");
            result.put("factType", "galaxy:fluid_inventory_observed");
            appendFluids(result, spec.getFluids());
            result.put("target", Long.toString(sumFluidTargets(spec.getFluids())));
        } else if ("bq_standard:block_break".equals(type)) {
            result.put("progressMode", "block-vector");
            result.put("factType", "galaxy:block_broken");
            appendBlocks(result, spec.getBlocks());
            result.put("target", Long.toString(sumBlockTargets(spec.getBlocks())));
        } else if ("bq_standard:interact_entity".equals(type)) {
            result.put("progressMode", "scalar");
            result.put("factType", "galaxy:entity_interacted");
            result.put("target", valueOrDefault(result, "requiredUses", "1"));
            rename(result, "targetID", "subject");
            JsonObject raw = gson.fromJson(rawJson, JsonObject.class);
            result.put("targetNBT.portable", BqPortableNbtCodec.encodeCompound(BqTypedJson.object(raw, "targetNBT")));
            appendInteractionItem(result, BqTypedJson.object(raw, "item"));
        } else if ("bq_standard:interact_item".equals(type)) {
            result.put("progressMode", "scalar");
            result.put("factType", "galaxy:item_interacted");
            result.put("target", valueOrDefault(result, "requiredUses", "1"));
            JsonObject raw = gson.fromJson(rawJson, JsonObject.class);
            appendInteractionItem(result, BqTypedJson.object(raw, "item"));
            appendInteractionBlock(result, BqTypedJson.object(raw, "block"));
        } else if ("bq_standard:scoreboard".equals(type)) {
            result.put("progressMode", "numeric-observation");
            result.put("factType", "galaxy:score_observed");
            result.put("subject", valueOrDefault(result, "scoreName", "Score").replace(' ', '_'));
            result.put("scoreTarget", valueOrDefault(result, "target", "1"));
            result.put("target", "1");
        } else if ("bq_standard:xp".equals(type)) {
            result.put("progressMode", "numeric-observation");
            result.put("factType", "galaxy:xp_observed");
            result.put("xpUnit", "1".equals(result.get("isLevels")) ? "levels" : "points");
            result.put("target", Long.toString("levels".equals(result.get("xpUnit"))
                ? levelXp((int) parseLong(valueOrDefault(result, "amount", "30"), 30L))
                : parseLong(valueOrDefault(result, "amount", "30"), 30L)));
        } else {
            result.put("progressMode", "boolean");
            if ("bq_standard:checkbox".equals(type)) result.put("factType", "galaxy:task_confirmed");
            if ("bq_standard:location".equals(type)) result.put("factType", "galaxy:location_observed");
            if ("bq_standard:meeting".equals(type)) result.put("factType", "galaxy:nearby_entities_observed");
            result.put("target", "1");
        }
        return result;
    }

    Map<String, String> rewardParameters(BqStandardRewardSpec spec, String rawJson) {
        Map<String, String> result = new LinkedHashMap<String, String>(spec.getOptions());
        result.put("bq.raw", rawJson);
        appendItems(result, spec.getItems());
        if ("bq_standard:choice".equals(spec.getTypeId())) result.put("selectionMode", "player-required");
        if ("bq_standard:xp".equals(spec.getTypeId())) {
            result.put("xpUnit", "1".equals(result.get("isLevels")) ? "levels" : "points");
        }
        return result;
    }

    private static String valueOrDefault(Map<String, String> values, String key, String fallback) {
        String value = values.get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static void rename(Map<String, String> values, String from, String to) {
        String value = values.remove(from);
        if (value != null) values.put(to, value);
    }

    private static long parseLong(String value, long fallback) {
        try { return Long.parseLong(value); } catch (RuntimeException invalid) { return fallback; }
    }

    private static long levelXp(int level) {
        if (level <= 0) return 0L;
        if (level < 16) return level * 17L;
        if (level < 31) return (long) (1.5D * level * level - 29.5D * level + 360D);
        return (long) (3.5D * level * level - 151.5D * level + 2220D);
    }

    private void appendInteractionItem(Map<String, String> result, JsonObject item) {
        String id = BqTypedJson.string(item, "id", "");
        if (id.isEmpty() || "minecraft:air".equals(id)) return;
        result.put("heldItem.registryName", id);
        result.put("heldItem.meta", Long.toString(BqTypedJson.number(item, "Damage", 0)));
        result.put("heldItem.oreDictionary", BqTypedJson.string(item, "OreDict", ""));
        result.put("heldItem.nbt", gson.toJson(BqTypedJson.object(item, "tag")));
        result.put("heldItem.nbtPortable", BqPortableNbtCodec.encodeCompound(BqTypedJson.object(item, "tag")));
    }

    private void appendInteractionBlock(Map<String, String> result, JsonObject block) {
        String id = BqTypedJson.string(block, "blockID", "");
        if (id.isEmpty() || "minecraft:air".equals(id)) return;
        result.put("block.registryName", id);
        result.put("block.meta", Long.toString(BqTypedJson.number(block, "meta", 0)));
        result.put("block.oreDictionary", BqTypedJson.string(block, "oreDict", ""));
        result.put("block.nbt", gson.toJson(BqTypedJson.object(block, "nbt")));
        result.put("block.nbtPortable", BqPortableNbtCodec.encodeCompound(BqTypedJson.object(block, "nbt")));
    }

    private static void appendItems(Map<String, String> result, List<BqItemStackSpec> items) {
        result.put("item.count", Integer.toString(items.size()));
        for (int i = 0; i < items.size(); i++) {
            BqItemStackSpec item = items.get(i);
            String prefix = "item." + i + ".";
            result.put(prefix + "registryName", item.getRegistryName());
            result.put(prefix + "meta", Integer.toString(item.getDamage()));
            result.put(prefix + "amount", Integer.toString(item.getCount()));
            result.put(prefix + "oreDictionary", item.getOreDictionary());
            result.put(prefix + "nbt", item.getTagJson());
            result.put(prefix + "nbtPortable", BqPortableNbtCodec.encodeJson(item.getTagJson()));
        }
    }

    private static void appendFluids(Map<String, String> result, List<BqFluidStackSpec> fluids) {
        result.put("fluid.count", Integer.toString(fluids.size()));
        for (int i = 0; i < fluids.size(); i++) {
            BqFluidStackSpec fluid = fluids.get(i);
            String prefix = "fluid." + i + ".";
            result.put(prefix + "name", fluid.getFluidName());
            result.put(prefix + "amount", Integer.toString(fluid.getAmount()));
            result.put(prefix + "nbt", fluid.getTagJson());
            result.put(prefix + "nbtPortable", BqPortableNbtCodec.encodeJson(fluid.getTagJson()));
        }
    }

    private static void appendBlocks(Map<String, String> result, List<BqBlockSpec> blocks) {
        result.put("block.count", Integer.toString(blocks.size()));
        for (int i = 0; i < blocks.size(); i++) {
            BqBlockSpec block = blocks.get(i);
            String prefix = "block." + i + ".";
            result.put(prefix + "registryName", block.getRegistryName());
            result.put(prefix + "meta", Integer.toString(block.getMeta()));
            result.put(prefix + "amount", Integer.toString(block.getAmount()));
            result.put(prefix + "oreDictionary", block.getOreDictionary());
            result.put(prefix + "nbt", block.getNbtJson());
            result.put(prefix + "nbtPortable", BqPortableNbtCodec.encodeJson(block.getNbtJson()));
        }
    }

    private static long sumItemTargets(List<BqItemStackSpec> values) {
        long result = 0L;
        for (BqItemStackSpec value : values) result = saturatingAdd(result, Math.max(0, value.getCount()));
        return Math.max(1L, result);
    }

    private static long sumFluidTargets(List<BqFluidStackSpec> values) {
        long result = 0L;
        for (BqFluidStackSpec value : values) result = saturatingAdd(result, Math.max(0, value.getAmount()));
        return Math.max(1L, result);
    }

    private static long sumBlockTargets(List<BqBlockSpec> values) {
        long result = 0L;
        for (BqBlockSpec value : values) result = saturatingAdd(result, Math.max(0, value.getAmount()));
        return Math.max(1L, result);
    }

    private static long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private Map<String, String> scalarOptions(JsonObject value, String... excluded) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        outer: for (Map.Entry<String, JsonElement> entry : value.entrySet()) {
            String logical = logicalName(entry.getKey());
            for (String name : excluded) if (name.equals(logical)) continue outer;
            if (entry.getValue().isJsonPrimitive()) result.put(logical, entry.getValue().getAsString());
            else result.put(logical + ".raw", gson.toJson(entry.getValue()));
        }
        return result;
    }

    private List<BqItemStackSpec> items(JsonObject value, String field) {
        List<BqItemStackSpec> result = new ArrayList<BqItemStackSpec>();
        for (JsonObject item : BqTypedJson.indexedObjects(value, field)) {
            result.add(new BqItemStackSpec(BqTypedJson.string(item, "id", "minecraft:air"),
                (int) BqTypedJson.number(item, "Damage", 0), (int) BqTypedJson.number(item, "Count", 0),
                BqTypedJson.string(item, "OreDict", ""), gson.toJson(BqTypedJson.object(item, "tag"))));
        }
        return result;
    }

    private List<BqFluidStackSpec> fluids(JsonObject value, String field) {
        List<BqFluidStackSpec> result = new ArrayList<BqFluidStackSpec>();
        for (JsonObject fluid : BqTypedJson.indexedObjects(value, field)) {
            result.add(new BqFluidStackSpec(BqTypedJson.string(fluid, "FluidName", ""),
                (int) BqTypedJson.number(fluid, "Amount", 0), gson.toJson(BqTypedJson.object(fluid, "Tag"))));
        }
        return result;
    }

    private List<BqBlockSpec> blocks(JsonObject value, String field) {
        List<BqBlockSpec> result = new ArrayList<BqBlockSpec>();
        for (JsonObject block : BqTypedJson.indexedObjects(value, field)) {
            result.add(new BqBlockSpec(BqTypedJson.string(block, "blockID", "minecraft:air"),
                (int) BqTypedJson.number(block, "meta", 0), (int) BqTypedJson.number(block, "amount", 0),
                BqTypedJson.string(block, "oreDict", ""), gson.toJson(BqTypedJson.object(block, "nbt"))));
        }
        return result;
    }

    private static String logicalName(String key) {
        int separator = key.lastIndexOf(':');
        return separator < 0 ? key : key.substring(0, separator);
    }

    static UUID decodeCompactUuid(String value) {
        String padded = value;
        while ((padded.length() & 3) != 0) padded += "=";
        byte[] bytes = Base64.getUrlDecoder().decode(padded);
        if (bytes.length != 16) throw new IllegalArgumentException("Invalid BetterQuesting UUID: " + value);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
