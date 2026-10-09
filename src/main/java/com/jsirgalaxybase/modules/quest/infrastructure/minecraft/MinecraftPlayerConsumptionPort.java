package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.nio.charset.Charset;
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
        List<Long> appliedAmounts = request.getAmounts();
        try {
            if ("item".equals(request.getKind())) {
                ItemPlan plan = planItems(before, request);
                after = plan == null ? null : plan.inventory;
                if (plan != null) appliedAmounts = plan.appliedAmounts;
            } else {
                FluidPlan plan = planFluidsDetailed(before, request);
                after = plan == null ? null : plan.inventory;
                if (plan != null) appliedAmounts = plan.appliedAmounts;
            }
        } catch (IllegalArgumentException invalid) {
            return ConsumptionApplyResult.insufficient("invalid-request:" + invalid.getMessage());
        }
        if (after == null) return ConsumptionApplyResult.insufficient("insufficient-" + request.getKind());

        restore(player, after);
        markers.setTag(marker, markerValue(appliedAmounts));
        persisted.setTag(MARKER_ROOT, markers);
        try {
            flusher.flush(player);
        } catch (RuntimeException failure) {
            restore(player, before);
            markers.removeTag(marker);
            persisted.setTag(MARKER_ROOT, markers);
            throw failure;
        }
        return ConsumptionApplyResult.applied("inventory:" + marker, appliedAmounts);
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

    ItemPlan planItems(List<ItemStack> before, ConsumptionRequest request) {
        Map<String, String> parameters = request.getResourceParameters();
        int count = integer(parameters.get("item.count"), -1);
        if (count < 0 || count != request.getAmounts().size()) throw new IllegalArgumentException("item vector width");
        final boolean ignoreNbt = bool(parameters.get("ignoreNBT"), true);
        final boolean partial = bool(parameters.get("partialMatch"), true);
        int selectedSlot = -1;
        if (parameters.containsKey(ConsumptionRequest.SELECTION_SLOT_PARAMETER)) {
            selectedSlot = integer(parameters.get(ConsumptionRequest.SELECTION_SLOT_PARAMETER), -1);
            String expectedFingerprint = parameters.get(ConsumptionRequest.SELECTION_FINGERPRINT_PARAMETER);
            if (selectedSlot < 0 || selectedSlot >= before.size() || expectedFingerprint == null
                || expectedFingerprint.trim().isEmpty()
                || !expectedFingerprint.equals(fingerprint(before.get(selectedSlot)))) return null;
        }
        return planItems(before, request.getAmounts(), new ItemRequirementMatcher() {
            @Override public boolean matches(ItemStack stack, int requirement) {
                return MinecraftPlayerConsumptionPort.this.matches(stack, parameters, requirement, ignoreNbt, partial);
            }
        }, selectedSlot);
    }

    ItemPlan planItems(List<ItemStack> before, List<Long> amounts, ItemRequirementMatcher matcher) {
        return planItems(before, amounts, matcher, -1);
    }

    ItemPlan planItems(List<ItemStack> before, List<Long> amounts, ItemRequirementMatcher matcher, int selectedSlot) {
        if (before == null || amounts == null || amounts.isEmpty() || matcher == null) throw new IllegalArgumentException(
            "inventory, amounts and matcher are required");
        if (selectedSlot < -1 || selectedSlot >= before.size()) throw new IllegalArgumentException(
            "selected inventory slot is outside the inventory");
        List<ItemStack> working = copy(before);
        int size = working.size();
        int[] available = new int[size];
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = working.get(slot);
            available[slot] = stack == null || selectedSlot >= 0 && selectedSlot != slot
                ? 0 : Math.max(0, stack.stackSize);
            if (selectedSlot == slot) available[slot] = Math.min(64, available[slot]);
        }
        List<Long> appliedAmounts = new ArrayList<Long>(amounts.size());
        long appliedTotal = 0L;
        for (int requirement = 0; requirement < amounts.size(); requirement++) {
            long requested = amounts.get(requirement);
            if (requested < 0L) throw new IllegalArgumentException("negative item amount");
            long remaining = requested;
            for (int slot = 0; slot < size && remaining > 0; slot++) {
                ItemStack stack = working.get(slot);
                if (available[slot] <= 0 || !matcher.matches(stack, requirement)) continue;
                int take = (int) Math.min(remaining, available[slot]);
                available[slot] -= take;
                stack.stackSize -= take;
                if (stack.stackSize <= 0) working.set(slot, null);
                remaining -= take;
            }
            long applied = requested - remaining;
            appliedAmounts.add(applied);
            appliedTotal += applied;
        }
        return appliedTotal <= 0L ? null : new ItemPlan(working, appliedAmounts);
    }

    static String fingerprint(ItemStack stack) {
        if (stack == null) return "";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(stack.writeToNBT(new NBTTagCompound()).toString().getBytes(Charset.forName("UTF-8")));
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    interface ItemRequirementMatcher {
        boolean matches(ItemStack stack, int requirementIndex);
    }

    static final class ItemPlan {
        final List<ItemStack> inventory;
        final List<Long> appliedAmounts;
        ItemPlan(List<ItemStack> inventory, List<Long> appliedAmounts) {
            this.inventory = inventory;
            this.appliedAmounts = appliedAmounts;
        }
    }

    List<ItemStack> planFluids(List<ItemStack> before, ConsumptionRequest request) {
        FluidPlan plan = planFluidsDetailed(before, request);
        return plan == null ? null : plan.inventory;
    }

    FluidPlan planFluidsDetailed(List<ItemStack> before, ConsumptionRequest request) {
        if (before == null || request == null) throw new IllegalArgumentException("inventory and request are required");
        Map<String, String> parameters = request.getResourceParameters();
        int count = integer(parameters.get("fluid.count"), -1);
        if (count < 0 || count != request.getAmounts().size()) throw new IllegalArgumentException("fluid vector width");
        boolean ignoreNbt = bool(parameters.get("ignoreNBT"), true);
        int selectedSlot = -1;
        if (parameters.containsKey(ConsumptionRequest.SELECTION_SLOT_PARAMETER)) {
            selectedSlot = integer(parameters.get(ConsumptionRequest.SELECTION_SLOT_PARAMETER), -1);
            String expectedFingerprint = parameters.get(ConsumptionRequest.SELECTION_FINGERPRINT_PARAMETER);
            if (selectedSlot < 0 || selectedSlot >= before.size() || expectedFingerprint == null
                || expectedFingerprint.trim().isEmpty()
                || !expectedFingerprint.equals(fingerprint(before.get(selectedSlot)))) return null;
        }
        List<ItemStack> working = copy(before);
        List<Long> remaining = new ArrayList<Long>(request.getAmounts());
        List<Long> applied = new ArrayList<Long>(count);
        for (int requirement = 0; requirement < count; requirement++) applied.add(0L);
        boolean anyApplied = false;
        for (int slot = 0; slot < working.size(); slot++) {
            if (selectedSlot >= 0 && selectedSlot != slot) continue;
            ItemStack initial = working.get(slot);
            if (initial == null || initial.stackSize <= 0 || initial.getItem() == null) continue;
            // BQ SubmitStation accepts one item, even when the selected hotbar stack contains more.
            int containersRemaining = selectedSlot == slot ? 1 : initial.stackSize;
            for (int requirement = 0; requirement < count && containersRemaining > 0; requirement++) {
                if (remaining.get(requirement) <= 0L) continue;
                String prefix = "fluid." + requirement + ".";
                String requiredName = parameters.get(prefix + "name");
                if (requiredName == null || requiredName.trim().isEmpty()) throw new IllegalArgumentException(
                    "fluid name is required");
                boolean requiredTagPresent = fluidTagPresent(parameters, prefix);
                NBTTagCompound requiredNbt = requiredTagPresent
                    ? BqTypedNbtCodec.compound(parameters.get(prefix + "nbt")) : null;
                while (remaining.get(requirement) > 0L && containersRemaining > 0) {
                    ItemStack source = working.get(slot);
                    if (source == null || source.stackSize <= 0 || source.getItem() == null) break;
                    ItemStack one = source.copy();
                    one.stackSize = 1;
                    QuestFluidContainerAdapter.FluidView available = fluids.inspect(one);
                    if (!fluidMatches(available, requiredName, requiredNbt, requiredTagPresent, ignoreNbt)) break;
                    QuestFluidContainerAdapter.FluidDrain result = fluids.drain(one,
                        (int) Math.min(Integer.MAX_VALUE, remaining.get(requirement)));
                    QuestFluidContainerAdapter.FluidView drained = result == null ? null : result.getDrained();
                    ItemStack returned = result == null ? null : result.getReturnedContainer();
                    if (!fluidMatches(drained, requiredName, requiredNbt, requiredTagPresent, ignoreNbt)) break;
                    long drainedAmount = drained.getAmount();
                    if (drainedAmount <= 0L) break;
                    removeOne(working, slot);
                    containersRemaining--;
                    if (returned != null && returned.getItem() != null && returned.stackSize > 0
                        && !insert(working, returned)) return null;
                    // Some Forge registered containers are indivisible; BQ consumes the whole container but
                    // task progress is bounded by the remaining requirement.
                    long credited = Math.min(remaining.get(requirement), drainedAmount);
                    remaining.set(requirement, remaining.get(requirement) - credited);
                    applied.set(requirement, applied.get(requirement) + credited);
                    anyApplied |= credited > 0L;
                }
            }
        }
        return !anyApplied ? null : new FluidPlan(working, applied);
    }

    private boolean fluidTagPresent(Map<String, String> parameters, String prefix) {
        String explicit = parameters.get(prefix + "nbtPresent");
        if (explicit != null) return bool(explicit, false);
        String raw = parameters.get(prefix + "nbt");
        if (raw != null && !raw.trim().isEmpty() && !"{}".equals(raw.trim())) return true;
        return !value(parameters, prefix + "nbtPortable").isEmpty();
    }

    private String value(Map<String, String> parameters, String key) {
        String value = parameters.get(key);
        return value == null ? "" : value;
    }

    private boolean fluidMatches(QuestFluidContainerAdapter.FluidView actual, String requiredName,
        NBTTagCompound requiredNbt, boolean requiredTagPresent, boolean ignoreNbt) {
        if (actual == null || actual.getAmount() <= 0 || requiredName == null
            || !requiredName.equals(actual.getName())) return false;
        if (ignoreNbt) return true;
        NBTTagCompound actualNbt = actual.getTag();
        if (requiredTagPresent != (actualNbt != null)) return false;
        return !requiredTagPresent || requiredNbt.equals(actualNbt);
    }

    static final class FluidPlan {
        final List<ItemStack> inventory;
        final List<Long> appliedAmounts;
        FluidPlan(List<ItemStack> inventory, List<Long> appliedAmounts) {
            this.inventory = inventory;
            this.appliedAmounts = appliedAmounts;
        }
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
        NBTBase actual = stack.getTagCompound();
        return (direct && matchesItemNbt(required, actual, partial, false))
            || (ore && matchesItemNbt(required, actual, partial, true));
    }

    static boolean matchesItemNbt(NBTBase required, NBTBase actual, boolean partial, boolean oreDictionaryMatch) {
        // BetterQuesting's OreDictionaryMatch reverses CompareNBTTagCompound's arguments
        // compared with StackMatch; match that source behavior rather than normalizing it.
        return oreDictionaryMatch ? compareNbt(actual, required, partial) : compareNbt(required, actual, partial);
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
            for (String key : (java.util.Set<String>) left.func_150296_c()) {
                if (!right.hasKey(key) || !compareNbt(left.getTag(key), right.getTag(key), partial)) return false;
            }
            return true;
        }
        if (required instanceof NBTTagList) {
            NBTTagList left = (NBTTagList) required;
            NBTTagList right = (NBTTagList) actual;
            if (left.tagCount() > right.tagCount() || (!partial && left.tagCount() != right.tagCount())) return false;
            for (int i = 0; i < left.tagCount(); i++) {
                boolean found = false;
                // BetterQuesting searches the complete NBTTagList independently for every
                // requested entry via getCompoundTagAt. Non-compound entries therefore become
                // empty compounds; unlike byte/int arrays, matched list indices are reusable.
                for (int j = 0; j < right.tagCount(); j++) {
                    if (compareNbt(left.getCompoundTagAt(i), right.getCompoundTagAt(j), partial)) {
                        found = true;
                        break;
                    }
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
    private boolean bool(String value, boolean fallback) {
        return com.jsirgalaxybase.quest.core.QuestBooleanParameter.parse(value, fallback);
    }
}
