package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.FluidObservationCanonicalizer;
import com.jsirgalaxybase.quest.core.FluidObservationEntry;
import com.jsirgalaxybase.quest.core.InventoryObservationEntry;
import com.jsirgalaxybase.quest.core.InventoryObservationCanonicalizer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.ScoreDummyCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

public final class MinecraftQuestFactFactory {
    private final String sourceServer;
    private final QuestEventIdentity identity;

    public MinecraftQuestFactFactory(String sourceServer) { this(sourceServer, new QuestEventIdentity()); }

    MinecraftQuestFactFactory(String sourceServer, QuestEventIdentity identity) {
        if (sourceServer == null || sourceServer.trim().isEmpty()) throw new IllegalArgumentException(
            "sourceServer must not be blank");
        this.sourceServer = sourceServer.trim();
        this.identity = identity;
    }

    public GameplayFact itemProduced(EntityPlayer player, ItemStack stack, int amount, String channel) {
        if (player == null || stack == null || stack.getItem() == null) throw new IllegalArgumentException(
            "player and produced item are required");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        attributes.put("registryName", registryName == null ? "minecraft:air" : registryName.toString());
        attributes.put("meta", Integer.toString(stack.getItemDamage()));
        attributes.put("amount", Integer.toString(Math.max(0, amount)));
        attributes.put("channel", channel);
        attributes.put("oreDictionary", oreNames(stack));
        if (stack.hasTagCompound()) appendNbt(attributes, "nbt", stack.getTagCompound());
        return fact(player, "galaxy:item_produced", channel, attributes);
    }

    public GameplayFact craftingStatisticObserved(EntityPlayer player, String registryName, int meta) {
        if (player == null || registryName == null || registryName.trim().isEmpty()) {
            throw new IllegalArgumentException("player and registryName are required");
        }
        Item item = (Item) Item.itemRegistry.getObject(registryName);
        int amount = 0;
        if (item != null && player instanceof EntityPlayerMP) {
            int id = Item.getIdFromItem(item);
            if (id >= 0 && id < net.minecraft.stats.StatList.objectCraftStats.length
                && net.minecraft.stats.StatList.objectCraftStats[id] != null) {
                amount = Math.max(0, ((EntityPlayerMP) player).func_147099_x().writeStat(
                    net.minecraft.stats.StatList.objectCraftStats[id]));
            }
        }
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("registryName", registryName.trim());
        attributes.put("meta", Integer.toString(meta));
        attributes.put("amount", Integer.toString(amount));
        attributes.put("channel", "craft-statistics");
        return fact(player, "galaxy:item_produced", "craft-statistics", attributes);
    }

    public GameplayFact entityKilled(EntityPlayer player, Entity entity, String damageType) {
        if (player == null || entity == null) throw new IllegalArgumentException("player and entity are required");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        String subject = EntityList.getEntityString(entity);
        attributes.put("subject", subject == null ? entity.getClass().getName() : subject);
        attributes.put("subjectAliases", entityAliases(entity.getClass()));
        attributes.put("damageType", damageType == null ? "" : damageType);
        attributes.put("amount", "1");
        NBTTagCompound nbt = new NBTTagCompound();
        if (entity.writeToNBTOptional(nbt)) appendNbt(attributes, "entity.nbt", nbt);
        return fact(player, "galaxy:entity_killed", "kill", attributes);
    }

    public GameplayFact entityInteracted(EntityPlayer player, Entity entity, ItemStack held, boolean hit) {
        if (player == null || entity == null) throw new IllegalArgumentException("player and entity are required");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        String subject = EntityList.getEntityString(entity);
        attributes.put("subject", subject == null ? entity.getClass().getName() : subject);
        attributes.put("subjectAliases", entityAliases(entity.getClass()));
        attributes.put("hit", Boolean.toString(hit));
        appendHeldItem(attributes, held);
        NBTTagCompound nbt = new NBTTagCompound();
        if (entity.writeToNBTOptional(nbt)) appendNbt(attributes, "entity.nbt", nbt);
        return fact(player, "galaxy:entity_interacted", hit ? "entity-hit" : "entity-interact", attributes);
    }

    public GameplayFact itemInteracted(EntityPlayer player, ItemStack held, Block block, int meta, int x, int y,
        int z, boolean hit) {
        if (player == null || block == null) throw new IllegalArgumentException("player and block are required");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("hit", Boolean.toString(hit));
        appendHeldItem(attributes, held);
        Object name = Block.blockRegistry.getNameForObject(block);
        attributes.put("block.registryName", name == null ? "minecraft:air" : name.toString());
        attributes.put("block.meta", Integer.toString(meta));
        attributes.put("block.oreDictionary", oreNames(new ItemStack(block, 1, meta)));
        TileEntity tile = player.worldObj.getTileEntity(x, y, z);
        if (tile != null) {
            NBTTagCompound nbt = new NBTTagCompound();
            tile.writeToNBT(nbt);
            appendNbt(attributes, "block.nbt", nbt);
        }
        return fact(player, "galaxy:item_interacted", hit ? "block-hit" : "block-interact", attributes);
    }

    public GameplayFact blockBroken(EntityPlayer player, Block block, int meta, int x, int y, int z) {
        if (player == null || block == null) throw new IllegalArgumentException("player and block are required");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        Object registryName = Block.blockRegistry.getNameForObject(block);
        attributes.put("registryName", registryName == null ? "minecraft:air" : registryName.toString());
        attributes.put("meta", Integer.toString(meta));
        attributes.put("oreDictionary", oreNames(new ItemStack(block, 1, meta)));
        attributes.put("x", Integer.toString(x));
        attributes.put("y", Integer.toString(y));
        attributes.put("z", Integer.toString(z));
        attributes.put("dimension", Integer.toString(player.dimension));
        TileEntity tile = player.worldObj.getTileEntity(x, y, z);
        if (tile != null) {
            NBTTagCompound nbt = new NBTTagCompound();
            tile.writeToNBT(nbt);
            appendNbt(attributes, "nbt", nbt);
        }
        return fact(player, "galaxy:block_broken", "block-break", attributes);
    }

    public GameplayFact inventoryObserved(EntityPlayer player, IInventory inventory) {
        if (player == null || inventory == null) throw new IllegalArgumentException("player and inventory are required");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        List<InventoryObservationEntry> raw = new ArrayList<InventoryObservationEntry>();
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
            Object name = Item.itemRegistry.getNameForObject(stack.getItem());
            String registryName = name == null ? "minecraft:air" : name.toString();
            String ore = oreNames(stack);
            String portable = stack.hasTagCompound()
                ? MinecraftPortableNbtCodec.encode(stack.getTagCompound()) : "";
            raw.add(new InventoryObservationEntry(registryName, stack.getItemDamage(), stack.stackSize, ore,
                stack.hasTagCompound() ? stack.getTagCompound().toString() : "", portable));
        }
        int entry = 0;
        for (InventoryObservationEntry observation : new InventoryObservationCanonicalizer().canonicalize(raw)) {
            String prefix = "inventory." + entry + ".";
            attributes.put(prefix + "registryName", observation.getRegistryName());
            attributes.put(prefix + "meta", Integer.toString(observation.getMeta()));
            attributes.put(prefix + "amount", Long.toString(observation.getAmount()));
            attributes.put(prefix + "oreDictionary", observation.getOreDictionary());
            if (!observation.getNbt().isEmpty()) attributes.put(prefix + "nbt", observation.getNbt());
            if (!observation.getNbtPortable().isEmpty()) attributes.put(prefix + "nbtPortable", observation.getNbtPortable());
            entry++;
        }
        attributes.put("inventory.count", Integer.toString(entry));
        return fact(player, "galaxy:inventory_observed", "inventory", attributes);
    }

    public GameplayFact fluidInventoryObserved(EntityPlayer player, IInventory inventory) {
        if (player == null || inventory == null) throw new IllegalArgumentException("player and inventory are required");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        List<FluidObservationEntry> raw = new ArrayList<FluidObservationEntry>();
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
            FluidStack fluid;
            if (stack.getItem() instanceof IFluidContainerItem) {
                ItemStack one = stack.copy();
                one.stackSize = 1;
                fluid = ((IFluidContainerItem) stack.getItem()).getFluid(one);
            } else {
                fluid = FluidContainerRegistry.getFluidForFilledItem(stack);
            }
            if (fluid == null || fluid.getFluid() == null || fluid.amount <= 0) continue;
            raw.add(new FluidObservationEntry(fluid.getFluid().getName(),
                saturatingMultiply(fluid.amount, stack.stackSize), fluid.tag == null ? "" : fluid.tag.toString(),
                fluid.tag == null ? "" : MinecraftPortableNbtCodec.encode(fluid.tag)));
        }
        int entry = 0;
        for (FluidObservationEntry observation : new FluidObservationCanonicalizer().canonicalize(raw)) {
            String prefix = "fluidInventory." + entry + ".";
            attributes.put(prefix + "name", observation.getName());
            attributes.put(prefix + "amount", Long.toString(observation.getAmount()));
            if (!observation.getNbt().isEmpty()) attributes.put(prefix + "nbt", observation.getNbt());
            if (!observation.getNbtPortable().isEmpty()) attributes.put(prefix + "nbtPortable",
                observation.getNbtPortable());
            entry++;
        }
        attributes.put("fluidInventory.count", Integer.toString(entry));
        return fact(player, "galaxy:fluid_inventory_observed", "fluid-inventory", attributes);
    }

    public GameplayFact locationObserved(EntityPlayer player) {
        if (player == null || player.worldObj == null) throw new IllegalArgumentException("player and world are required");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("dimension", Integer.toString(player.dimension));
        attributes.put("x", Double.toString(player.posX));
        attributes.put("y", Double.toString(player.posY));
        attributes.put("z", Double.toString(player.posZ));
        attributes.put("biome", Integer.toString(player.worldObj.getBiomeGenForCoords(
            net.minecraft.util.MathHelper.floor_double(player.posX),
            net.minecraft.util.MathHelper.floor_double(player.posZ)).biomeID));
        return fact(player, "galaxy:location_observed", "location", attributes);
    }

    public GameplayFact xpObserved(EntityPlayer player) {
        if (player == null) throw new IllegalArgumentException("player is required");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        long points = MinecraftXpRewardDeliveryHandler.levelXp(player.experienceLevel)
            + (long) (MinecraftXpRewardDeliveryHandler.barCap(player.experienceLevel)
                * Math.max(0D, player.experience));
        attributes.put("value", Long.toString(points));
        return fact(player, "galaxy:xp_observed", "xp", attributes);
    }

    public GameplayFact scoreObserved(EntityPlayer player, String objectiveName, String displayName,
        String criteriaName) {
        if (player == null || player.worldObj == null) throw new IllegalArgumentException("player and world are required");
        Scoreboard board = player.getWorldScoreboard();
        ScoreObjective objective = board.getObjective(objectiveName);
        if (objective == null) {
            IScoreObjectiveCriteria criteria = (IScoreObjectiveCriteria) IScoreObjectiveCriteria.field_96643_a
                .get(criteriaName);
            objective = board.addScoreObjective(objectiveName,
                criteria == null ? new ScoreDummyCriteria(objectiveName) : criteria);
            objective.setDisplayName(displayName);
        }
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("subject", objectiveName);
        attributes.put("value", Integer.toString(board.func_96529_a(player.getCommandSenderName(), objective)
            .getScorePoints()));
        return fact(player, "galaxy:score_observed", "score", attributes);
    }

    @SuppressWarnings("unchecked")
    public GameplayFact nearbyEntitiesObserved(EntityPlayer player, int radius) {
        if (player == null || player.worldObj == null) throw new IllegalArgumentException("player and world are required");
        int safeRadius = Math.max(0, radius);
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        List<Entity> entities = player.worldObj.getEntitiesWithinAABBExcludingEntity(player,
            player.boundingBox.expand(safeRadius, safeRadius, safeRadius));
        int entry = 0;
        for (Entity entity : entities) {
            String subject = EntityList.getEntityString(entity);
            if (subject == null) continue;
            String prefix = "nearbyEntity." + entry + ".";
            attributes.put(prefix + "subject", subject);
            attributes.put(prefix + "subjectAliases", entityAliases(entity.getClass()));
            NBTTagCompound nbt = new NBTTagCompound();
            if (entity.writeToNBTOptional(nbt)) appendNbt(attributes, prefix + "nbt", nbt);
            entry++;
        }
        attributes.put("nearbyEntity.count", Integer.toString(entry));
        attributes.put("observedRadius", Integer.toString(safeRadius));
        return fact(player, "galaxy:nearby_entities_observed", "nearby-entities", attributes);
    }

    private GameplayFact fact(EntityPlayer player, String factType, String eventType, Map<String, String> attributes) {
        return new GameplayFact(sourceServer, identity.next(eventType), player.getUniqueID(), factType,
            System.currentTimeMillis(), attributes);
    }

    private void appendHeldItem(Map<String, String> attributes, ItemStack stack) {
        if (stack == null || stack.getItem() == null) return;
        Object name = Item.itemRegistry.getNameForObject(stack.getItem());
        attributes.put("heldItem.registryName", name == null ? "minecraft:air" : name.toString());
        attributes.put("heldItem.meta", Integer.toString(stack.getItemDamage()));
        attributes.put("heldItem.oreDictionary", oreNames(stack));
        if (stack.hasTagCompound()) appendNbt(attributes, "heldItem.nbt", stack.getTagCompound());
    }

    private void appendNbt(Map<String, String> attributes, String key, NBTBase tag) {
        attributes.put(key, tag.toString());
        attributes.put(key + "Portable", MinecraftPortableNbtCodec.encode(tag));
    }

    private String oreNames(ItemStack stack) {
        List<String> names = new ArrayList<String>();
        for (int id : OreDictionary.getOreIDs(stack)) names.add(OreDictionary.getOreName(id));
        Collections.sort(names);
        return join(names);
    }

    @SuppressWarnings("unchecked")
    private String entityAliases(Class<? extends Entity> subject) {
        List<String> names = new ArrayList<String>();
        for (Map.Entry<String, Class<? extends Entity>> entry
            : ((Map<String, Class<? extends Entity>>) (Map<?, ?>) EntityList.stringToClassMapping).entrySet()) {
            if (entry.getValue().isAssignableFrom(subject)) names.add(entry.getKey());
        }
        Collections.sort(names);
        return join(names);
    }

    private String join(List<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append('|');
            result.append(value);
        }
        return result.toString();
    }

    private long saturatingMultiply(long left, long right) {
        if (left <= 0 || right <= 0) return 0L;
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

}
