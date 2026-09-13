package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Editor schema for the BetterQuesting Standard semantics internalized by Base. */
public final class StandardQuestEditorTypeRegistry {
    private StandardQuestEditorTypeRegistry() {}

    public static QuestEditorTypeRegistry create() {
        List<QuestElementTypeDescriptor> result = new ArrayList<QuestElementTypeDescriptor>();
        result.add(task("bq_standard:block_break", "Break blocks", list("block", "Blocks", QuestEditorFieldType.BLOCK_LIST), bool("ignoreNBT", "Ignore NBT")));
        result.add(task("bq_standard:checkbox", "Manual checkbox"));
        result.add(task("bq_standard:crafting", "Craft items", list("item", "Crafted items", QuestEditorFieldType.ITEM_LIST)));
        result.add(task("bq_standard:fluid", "Retrieve fluids", list("fluid", "Fluids", QuestEditorFieldType.FLUID_LIST), bool("consume", "Consume fluids")));
        result.add(task("bq_standard:hunt", "Hunt entities", text("subject", "Entity"), positive("target", "Required kills"), bool("ignoreNBT", "Ignore NBT"), bool("subtypes", "Include subtypes"), optional("targetNBT.portable", "Entity NBT", QuestEditorFieldType.NBT)));
        result.add(task("bq_standard:interact_entity", "Interact with entity", text("subject", "Entity"), positive("target", "Required interactions"), optional("targetNBT.portable", "Entity NBT", QuestEditorFieldType.NBT), optional("heldItem.registryName", "Held item", QuestEditorFieldType.TEXT)));
        result.add(task("bq_standard:interact_item", "Use item or block", positive("target", "Required interactions"), optional("heldItem.registryName", "Held item", QuestEditorFieldType.TEXT), optional("block.registryName", "Block", QuestEditorFieldType.TEXT)));
        result.add(task("bq_standard:location", "Visit location", integer("dimension", "Dimension"), integer("posX", "X"), integer("posY", "Y"), integer("posZ", "Z"), positive("range", "Range"), bool("invert", "Invert area"), bool("taxiCabDist", "Use taxicab distance")));
        result.add(task("bq_standard:meeting", "Meet entities", text("subject", "Entity"), positive("amount", "Required entities"), positive("range", "Range"), optional("targetNBT.portable", "Entity NBT", QuestEditorFieldType.NBT)));
        result.add(task("bq_standard:optional_retrieval", "Optional item retrieval", list("item", "Items", QuestEditorFieldType.ITEM_LIST), bool("consume", "Consume items")));
        result.add(task("bq_standard:retrieval", "Retrieve items", list("item", "Items", QuestEditorFieldType.ITEM_LIST), bool("consume", "Consume items")));
        result.add(task("bq_standard:scoreboard", "Scoreboard value", text("subject", "Objective"), integer("scoreTarget", "Target score"), enumeration("operation", "Comparison", "EQUAL", "NOT_EQUAL", "GREATER", "GREATER_OR_EQUAL", "LESS", "LESS_OR_EQUAL")));
        result.add(task("bq_standard:xp", "Experience", positive("amount", "Amount"), enumeration("xpUnit", "Unit", "points", "levels"), bool("consume", "Consume experience")));
        result.add(reward("bq_standard:choice", "Choice of items", list("item", "Choices", QuestEditorFieldType.ITEM_LIST)));
        result.add(reward("bq_standard:command", "Run command", required("command", "Command", QuestEditorFieldType.COMMAND_LIST), bool("viaPlayer", "Run as player")));
        result.add(reward("bq_standard:item", "Give items", list("item", "Items", QuestEditorFieldType.ITEM_LIST)));
        result.add(reward("bq_standard:questcompletion", "Complete quest", required("questUuid", "Quest", QuestEditorFieldType.QUEST_REFERENCE)));
        result.add(reward("bq_standard:scoreboard", "Set scoreboard", text("scoreName", "Objective"), integer("score", "Score")));
        result.add(reward("bq_standard:xp", "Give experience", positive("amount", "Amount"), enumeration("xpUnit", "Unit", "points", "levels")));
        return new QuestEditorTypeRegistry(result);
    }

    private static QuestElementTypeDescriptor task(String id, String name, QuestEditorFieldDescriptor... fields) { return descriptor(id, QuestElementKind.TASK, name, fields); }
    private static QuestElementTypeDescriptor reward(String id, String name, QuestEditorFieldDescriptor... fields) { return descriptor(id, QuestElementKind.REWARD, name, fields); }
    private static QuestElementTypeDescriptor descriptor(String id, QuestElementKind kind, String name, QuestEditorFieldDescriptor... fields) { return new QuestElementTypeDescriptor(id, kind, name, Arrays.asList(fields), null); }
    private static QuestEditorFieldDescriptor text(String key, String label) { return required(key, label, QuestEditorFieldType.TEXT); }
    private static QuestEditorFieldDescriptor list(String key, String label, QuestEditorFieldType type) { return required(key, label, type); }
    private static QuestEditorFieldDescriptor required(String key, String label, QuestEditorFieldType type) { return QuestEditorFieldDescriptor.builder(key, label, type).required().build(); }
    private static QuestEditorFieldDescriptor optional(String key, String label, QuestEditorFieldType type) { return QuestEditorFieldDescriptor.builder(key, label, type).build(); }
    private static QuestEditorFieldDescriptor bool(String key, String label) { return optional(key, label, QuestEditorFieldType.BOOLEAN); }
    private static QuestEditorFieldDescriptor integer(String key, String label) { return optional(key, label, QuestEditorFieldType.LONG); }
    private static QuestEditorFieldDescriptor positive(String key, String label) { return QuestEditorFieldDescriptor.builder(key, label, QuestEditorFieldType.LONG).required().minimum(1L).build(); }
    private static QuestEditorFieldDescriptor enumeration(String key, String label, String... values) { return QuestEditorFieldDescriptor.builder(key, label, QuestEditorFieldType.ENUM).required().options(values).build(); }
}
