package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.nio.charset.Charset;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jsirgalaxybase.quest.core.ConsumptionApplyResult;
import com.jsirgalaxybase.quest.core.ConsumptionPort;
import com.jsirgalaxybase.quest.core.ConsumptionRequest;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTBase.NBTPrimitive;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Server-thread inventory deduction for consuming retrieval tasks.
 *
 * The stable submission marker is written beside the mutated inventory in player NBT. A complete allocation is
 * calculated before mutation, and a failed synchronous save restores both inventory and marker.
 */
public final class MinecraftPlayerConsumptionPort implements ConsumptionPort {
    private static final String MARKER_ROOT = "GalaxyQuestConsumption";
    private static final Field TAG_LIST = tagListField();
    private final QuestPlayerResolver players;
    private final QuestPlayerDataFlusher flusher;
    private final QuestFluidContainerAdapter fluids;

    public MinecraftPlayerConsumptionPort(QuestPlayerResolver players, QuestPlayerDataFlusher flusher) {
        this(players, flusher, new ForgeQuestFluidContainerAdapter());
    }

    MinecraftPlayerConsumptionPort(QuestPlayerResolver players, QuestPlayerDataFlusher flusher,
        QuestFluidContainerAdapter fluids) {
        if (players == null || flusher == null || fluids == null) throw new IllegalArgumentException(
            "players, flusher and fluids are required");
        this.players = players;
        this.flusher = flusher;
        this.fluids = fluids;
    }

    @Override
    public ConsumptionApplyResult apply(ConsumptionRequest request) {
        if (!"item".equals(request.getKind()) && !"fluid".equals(request.getKind()) && !"xp".equals(request.getKind())) {
            return ConsumptionApplyResult.insufficient("unsupported-kind:" + request.getKind());
        }
        EntityPlayerMP player = players.findOnline(request.getPlayerId());
        if (player == null) return ConsumptionApplyResult.insufficient("player-offline");
        String marker = marker(request.getSubmissionKey());
        NBTTagCompound persisted = persisted(player);
        NBTTagCompound markers = persisted.getCompoundTag(MARKER_ROOT);
        if (markers.hasKey(marker)) return ConsumptionApplyResult.applied("already-applied:" + marker,
            markerAmounts(markers.getTag(marker), request));

        if ("xp".equals(request.getKind())) return applyXp(player, request, markers, marker, persisted);

        List<ItemStack> before = snapshot(player);
        List<ItemStack> after;
        try {
            after = "item".equals(request.getKind()) ? planItems(before, request) : planFluids(before, request);
        } catch (IllegalArgumentException invalid) {
            return ConsumptionApplyResult.insufficient("invalid-request:" + invalid.getMessage());
        }
        if (after == null) return ConsumptionApplyResult.insufficient("insufficient-" + request.getKind());

        restore(player, after);
        markers.setTag(marker, markerValue(request.getAmounts()));
        persisted.setTag(MARKER_ROOT, markers);
        try {
            flusher.flush(player);
        } catch (RuntimeException failure) {
            restore(player, before);
            markers.removeTag(marker);
            persisted.setTag(MARKER_ROOT, markers);
            throw failure;
        }
        return ConsumptionApplyResult.applied("inventory:" + marker, request.getAmounts());
    }

    private ConsumptionApplyResult applyXp(EntityPlayerMP player, ConsumptionRequest request, NBTTagCompound markers,
        String marker, NBTTagCompound persisted) {
        if (request.getAmounts().size() != 1) return ConsumptionApplyResult.insufficient("invalid-request:xp vector width");
        long available = MinecraftXpRewardDeliveryHandler.playerXp(player);
        long applied = xpDeduction(request.getAmounts().get(0), available);
        if (applied <= 0) return ConsumptionApplyResult.insufficient("insufficient-xp");
        int oldTotal = player.experienceTotal;
        int oldLevel = player.experienceLevel;
        float oldProgress = player.experience;
        List<Long> amounts = java.util.Collections.singletonList(applied);
        MinecraftXpRewardDeliveryHandler.addPoints(player, -applied);
        markers.setTag(marker, markerValue(amounts));
        persisted.setTag(MARKER_ROOT, markers);
        try { flusher.flush(player); }
        catch (RuntimeException failure) {
            player.experienceTotal = oldTotal;
            player.experienceLevel = oldLevel;
            player.experience = oldProgress;
            markers.removeTag(marker);
            persisted.setTag(MARKER_ROOT, markers);
            throw failure;
        }
        if (player.playerNetServerHandler != null) player.playerNetServerHandler.sendPacket(
            new S1FPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel));
        return ConsumptionApplyResult.applied("xp:" + marker, amounts);
    }

    static long xpDeduction(long requested, long available) {
        return Math.max(0L, Math.min(Math.max(0L, requested), Math.max(0L, available)));
    }

    private NBTTagCompound markerValue(List<Long> amounts) {
        NBTTagCompound value = new NBTTagCompound();
        value.setInteger("count", amounts.size());
        for (int i = 0; i < amounts.size(); i++) value.setLong("value" + i, amounts.get(i));
        return value;
    }

    private List<Long> markerAmounts(NBTBase marker, ConsumptionRequest request) {
        if (!(marker instanceof NBTTagCompound)) return request.getAmounts();
        NBTTagCompound value = (NBTTagCompound) marker;
        int count = value.getInteger("count");
        if (count != request.getAmounts().size()) throw new IllegalStateException("consumption marker vector mismatch");
        List<Long> result = new ArrayList<Long>(count);
        for (int i = 0; i < count; i++) result.add(value.getLong("value" + i));
        return result;
    }

    private List<ItemStack> planItems(List<ItemStack> before, ConsumptionRequest request) {
        Map<String, String> parameters = request.getResourceParameters();
        int count = integer(parameters.get("item.count"), -1);
        if (count < 0 || count != request.getAmounts().size()) throw new IllegalArgumentException("item vector width");
        List<ItemStack> working = copy(before);
        int size = working.size();
        int[] available = new int[size];
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = working.get(slot);
            available[slot] = stack == null ? 0 : Math.max(0, stack.stackSize);
        }
        boolean partial = bool(parameters.get("partialMatch"), true);
        boolean ignoreNbt = bool(parameters.get("ignoreNBT"), true);
        for (int requirement = 0; requirement < count; requirement++) {
            long remaining = request.getAmounts().get(requirement);
            for (int slot = 0; slot < size && remaining > 0; slot++) {
                ItemStack stack = working.get(slot);
                if (available[slot] <= 0 || !matches(stack, parameters, requirement, ignoreNbt, partial)) continue;
                int take = (int) Math.min(remaining, available[slot]);
                available[slot] -= take;
                stack.stackSize -= take;
                if (stack.stackSize <= 0) working.set(slot, null);
                remaining -= take;
            }
            if (remaining > 0) return null;
        }
        return working;
    }

    List<ItemStack> planFluids(List<ItemStack> before, ConsumptionRequest request) {
        Map<String, String> parameters = request.getResourceParameters();
        int count = integer(parameters.get("fluid.count"), -1);
        if (count < 0 || count != request.getAmounts().size()) throw new IllegalArgumentException("fluid vector width");
        boolean ignoreNbt = bool(parameters.get("ignoreNBT"), true);
        List<ItemStack> working = copy(before);
        for (int requirement = 0; requirement < count; requirement++) {
            long remaining = request.getAmounts().get(requirement);
            String prefix = "fluid." + requirement + ".";
            String requiredName = parameters.get(prefix + "name");
            NBTTagCompound requiredNbt = BqTypedNbtCodec.compound(parameters.get(prefix + "nbt"));
            for (int slot = 0; slot < working.size() && remaining > 0; slot++) {
                while (remaining > 0) {
                    ItemStack source = working.get(slot);
                    if (source == null || source.stackSize <= 0 || source.getItem() == null) break;
                    ItemStack one = source.copy();
                    one.stackSize = 1;
                    QuestFluidContainerAdapter.FluidView available = fluids.inspect(one);
                    if (!fluidMatches(available, requiredName, requiredNbt, ignoreNbt)) break;
                    QuestFluidContainerAdapter.FluidDrain result = fluids.drain(one,
                        (int) Math.min(Integer.MAX_VALUE, remaining));
                    QuestFluidContainerAdapter.FluidView drained = result == null ? null : result.getDrained();
                    ItemStack returned = result == null ? null : result.getReturnedContainer();
                    if (!fluidMatches(drained, requiredName, requiredNbt, ignoreNbt)) break;
                    removeOne(working, slot);
                    if (returned != null && returned.getItem() != null && returned.stackSize > 0
                        && !insert(working, returned)) return null;
                    remaining = Math.max(0L, remaining - drained.getAmount());
                }
            }
            if (remaining > 0) return null;
        }
        return working;
    }

    private boolean fluidMatches(QuestFluidContainerAdapter.FluidView actual, String requiredName,
        NBTTagCompound requiredNbt,
        boolean ignoreNbt) {
        if (actual == null || actual.getAmount() <= 0 || requiredName == null
            || !requiredName.equals(actual.getName())) return false;
        return ignoreNbt || compareNbt(requiredNbt, actual.getTag(), false);
    }

    private void removeOne(List<ItemStack> inventory, int slot) {
        ItemStack stack = inventory.get(slot);
        stack.stackSize--;
        if (stack.stackSize <= 0) inventory.set(slot, null);
    }

    private boolean insert(List<ItemStack> inventory, ItemStack input) {
        ItemStack remaining = input.copy();
        for (int slot = 0; slot < inventory.size() && remaining.stackSize > 0; slot++) {
            ItemStack existing = inventory.get(slot);
            if (existing == null || existing.getItem() != remaining.getItem()
                || existing.getItemDamage() != remaining.getItemDamage()
                || !ItemStack.areItemStackTagsEqual(existing, remaining)) continue;
            int limit = Math.min(existing.getMaxStackSize(), 64);
            int moved = Math.min(remaining.stackSize, Math.max(0, limit - existing.stackSize));
            existing.stackSize += moved;
            remaining.stackSize -= moved;
        }
        for (int slot = 0; slot < inventory.size() && remaining.stackSize > 0; slot++) {
            if (inventory.get(slot) != null) continue;
            int moved = Math.min(remaining.stackSize, Math.min(remaining.getMaxStackSize(), 64));
            ItemStack placed = remaining.copy();
            placed.stackSize = moved;
            inventory.set(slot, placed);
            remaining.stackSize -= moved;
        }
        return remaining.stackSize <= 0;
    }

    private boolean matches(ItemStack stack, Map<String, String> values, int index, boolean ignoreNbt,
        boolean partial) {
        if (stack == null || stack.getItem() == null) return false;
        String prefix = "item." + index + ".";
        Object actualName = Item.itemRegistry.getNameForObject(stack.getItem());
        boolean direct = actualName != null && actualName.toString().equals(values.get(prefix + "registryName"));
        int requiredMeta = integer(values.get(prefix + "meta"), 0);
        direct &= requiredMeta == OreDictionary.WILDCARD_VALUE || stack.getItem().isDamageable()
            || requiredMeta == stack.getItemDamage();
        boolean ore = containsOre(stack, values.get(prefix + "oreDictionary"));
        if (!direct && !ore) return false;
        if (ignoreNbt) return true;
        NBTBase required = BqTypedNbtCodec.compound(values.get(prefix + "nbt"));
        return compareNbt(required, stack.getTagCompound(), partial);
    }

    private boolean containsOre(ItemStack stack, String expected) {
        if (expected == null || expected.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(stack)) if (expected.equals(OreDictionary.getOreName(id))) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    static boolean compareNbt(NBTBase required, NBTBase actual, boolean partial) {
        if (empty(required) || empty(actual)) return empty(required) == empty(actual);
        if (!(required instanceof net.minecraft.nbt.NBTBase.NBTPrimitive
            && actual instanceof net.minecraft.nbt.NBTBase.NBTPrimitive) && required.getId() != actual.getId()) return false;
        if (required instanceof NBTTagCompound) {
            NBTTagCompound left = (NBTTagCompound) required;
            NBTTagCompound right = (NBTTagCompound) actual;
            if (!partial && left.func_150296_c().size() != right.func_150296_c().size()) return false;
            for (String key : (java.util.Set<String>) left.func_150296_c()) {
                if (!right.hasKey(key) || !compareNbt(left.getTag(key), right.getTag(key), partial)) return false;
            }
            return true;
        }
        if (required instanceof NBTTagList) {
            NBTTagList left = (NBTTagList) required;
            NBTTagList right = (NBTTagList) actual;
            if (left.tagCount() > right.tagCount() || (!partial && left.tagCount() != right.tagCount())) return false;
            boolean[] used = new boolean[right.tagCount()];
            List<NBTBase> leftValues = listValues(left);
            List<NBTBase> rightValues = listValues(right);
            for (int i = 0; i < left.tagCount(); i++) {
                boolean found = false;
                for (int j = 0; j < right.tagCount(); j++) if (!used[j]
                    && compareNbt(leftValues.get(i), rightValues.get(j), partial)) {
                    used[j] = true;
                    found = true;
                    break;
                }
                if (!found) return false;
            }
            return true;
        }
        if (required instanceof NBTTagIntArray && actual instanceof NBTTagIntArray) {
            int[] left = ((NBTTagIntArray) required).func_150302_c();
            int[] right = ((NBTTagIntArray) actual).func_150302_c();
            return unorderedSubset(left, right, partial);
        }
        if (required instanceof NBTTagByteArray && actual instanceof NBTTagByteArray) {
            byte[] left = ((NBTTagByteArray) required).func_150292_c();
            byte[] right = ((NBTTagByteArray) actual).func_150292_c();
            return unorderedSubset(left, right, partial);
        }
        if (required instanceof NBTPrimitive && actual instanceof NBTPrimitive) {
            if (required instanceof NBTTagFloat || required instanceof NBTTagDouble
                || actual instanceof NBTTagFloat || actual instanceof NBTTagDouble) {
                return ((NBTPrimitive) required).func_150286_g() == ((NBTPrimitive) actual).func_150286_g();
            }
            return ((NBTPrimitive) required).func_150291_c() == ((NBTPrimitive) actual).func_150291_c();
        }
        return required.equals(actual);
    }

    private static boolean unorderedSubset(int[] required, int[] actual, boolean partial) {
        if (required.length > actual.length || !partial && required.length != actual.length) return false;
        boolean[] used = new boolean[actual.length];
        for (int value : required) {
            boolean found = false;
            for (int i = 0; i < actual.length; i++) if (!used[i] && value == actual[i]) {
                used[i] = true;
                found = true;
                break;
            }
            if (!found) return false;
        }
        return true;
    }

    private static boolean unorderedSubset(byte[] required, byte[] actual, boolean partial) {
        if (required.length > actual.length || !partial && required.length != actual.length) return false;
        boolean[] used = new boolean[actual.length];
        for (byte value : required) {
            boolean found = false;
            for (int i = 0; i < actual.length; i++) if (!used[i] && value == actual[i]) {
                used[i] = true;
                found = true;
                break;
            }
            if (!found) return false;
        }
        return true;
    }

    private static boolean empty(NBTBase tag) {
        return tag == null || tag instanceof NBTTagCompound && ((NBTTagCompound) tag).hasNoTags()
            || tag instanceof NBTTagList && ((NBTTagList) tag).tagCount() == 0;
    }

    @SuppressWarnings("unchecked")
    private static List<NBTBase> listValues(NBTTagList list) {
        try {
            return (List<NBTBase>) TAG_LIST.get(list);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("cannot inspect NBT list", failure);
        }
    }

    private static Field tagListField() {
        for (String name : new String[] { "tagList", "field_74747_a" }) {
            try {
                Field field = NBTTagList.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new ExceptionInInitializerError("NBTTagList backing field not found");
    }

    private NBTTagCompound persisted(EntityPlayer player) {
        NBTTagCompound entity = player.getEntityData();
        if (!entity.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) entity.setTag(EntityPlayer.PERSISTED_NBT_TAG,
            new NBTTagCompound());
        return entity.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    private List<ItemStack> snapshot(EntityPlayerMP player) {
        List<ItemStack> result = new ArrayList<ItemStack>(player.inventory.getSizeInventory());
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            result.add(stack == null ? null : stack.copy());
        }
        return result;
    }

    private List<ItemStack> copy(List<ItemStack> values) {
        List<ItemStack> result = new ArrayList<ItemStack>(values.size());
        for (ItemStack value : values) result.add(value == null ? null : value.copy());
        return result;
    }

    private void restore(EntityPlayerMP player, List<ItemStack> values) {
        for (int i = 0; i < values.size(); i++) player.inventory.setInventorySlotContents(i,
            values.get(i) == null ? null : values.get(i).copy());
        player.inventory.markDirty();
    }

    private String marker(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(Charset.forName("UTF-8")));
            StringBuilder result = new StringBuilder(64);
            for (byte b : digest) result.append(String.format("%02x", b & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private int integer(String value, int fallback) { return value == null ? fallback : Integer.parseInt(value); }
    private boolean bool(String value, boolean fallback) { return value == null ? fallback : Boolean.parseBoolean(value); }
}
