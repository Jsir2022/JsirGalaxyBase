package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ConsumptionRequest;
import com.jsirgalaxybase.quest.core.ParticipantId;

import org.junit.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class MinecraftPlayerConsumptionPortTest {
    @Test public void xpConsumptionUsesAvailablePartialAmountWithoutOverdrawing() {
        assertEquals(40L, MinecraftPlayerConsumptionPort.xpDeduction(100L, 40L));
        assertEquals(100L, MinecraftPlayerConsumptionPort.xpDeduction(100L, 140L));
        assertEquals(0L, MinecraftPlayerConsumptionPort.xpDeduction(100L, -1L));
    }
    @Test
    public void strictCompoundRequiresDeclaredTagsButAllowsAdditionalActualTagsLikeBq() {
        NBTTagCompound required = new NBTTagCompound();
        required.setInteger("grade", 2);
        NBTTagCompound actual = new NBTTagCompound();
        actual.setInteger("grade", 2);
        actual.setString("owner", "player");

        assertTrue(MinecraftPlayerConsumptionPort.compareNbt(required, actual, true));
        assertTrue(MinecraftPlayerConsumptionPort.compareNbt(required, actual, false));
    }

    @Test
    public void itemNbtMatchUsesBqOreDictionaryArgumentDirection() {
        NBTTagCompound subset = new NBTTagCompound();
        subset.setInteger("grade", 2);
        NBTTagCompound required = (NBTTagCompound) subset.copy();
        required.setString("extra", "required");
        NBTTagCompound superset = (NBTTagCompound) subset.copy();
        superset.setString("extra", "required");
        superset.setString("unexpected", "value");

        assertTrue(MinecraftPlayerConsumptionPort.matchesItemNbt(required, subset, true, true));
        assertFalse(MinecraftPlayerConsumptionPort.matchesItemNbt(required, superset, true, true));
        assertTrue(MinecraftPlayerConsumptionPort.matchesItemNbt(required, superset, true, false));
        assertFalse(MinecraftPlayerConsumptionPort.matchesItemNbt(new NBTTagCompound(), subset, true, false));
    }

    @Test
    public void partialCompoundListCanReuseOneActualEntryLikeBetterQuesting() {
        NBTTagList required = new NBTTagList();
        required.appendTag(compoundValue(7));
        required.appendTag(compoundValue(7));
        NBTTagList actual = new NBTTagList();
        actual.appendTag(compoundValue(7));
        actual.appendTag(compoundValue(8));

        assertTrue(MinecraftPlayerConsumptionPort.compareNbt(required, actual, true));
    }

    @Test
    public void stringListElementComparisonUsesBqCompoundAccessorBehavior() {
        NBTTagList required = new NBTTagList();
        required.appendTag(new net.minecraft.nbt.NBTTagString("required"));
        NBTTagList actual = new NBTTagList();
        actual.appendTag(new net.minecraft.nbt.NBTTagString("different"));

        assertTrue(MinecraftPlayerConsumptionPort.compareNbt(required, actual, false));
    }

    @Test
    public void strictListStillRequiresEqualElementCountLikeBetterQuesting() {
        NBTTagList required = new NBTTagList();
        required.appendTag(compoundValue(7));
        NBTTagList actual = new NBTTagList();
        actual.appendTag(compoundValue(7));
        actual.appendTag(compoundValue(8));

        assertTrue(MinecraftPlayerConsumptionPort.compareNbt(required, actual, true));
        assertFalse(MinecraftPlayerConsumptionPort.compareNbt(required, actual, false));
    }

    @Test
    public void byteAndIntArraysStillRequireDistinctMatchesLikeBetterQuesting() {
        assertFalse(MinecraftPlayerConsumptionPort.compareNbt(new NBTTagByteArray(new byte[] { 7, 7 }),
            new NBTTagByteArray(new byte[] { 7, 8 }), true));
        assertFalse(MinecraftPlayerConsumptionPort.compareNbt(new NBTTagIntArray(new int[] { 7, 7 }),
            new NBTTagIntArray(new int[] { 7, 8 }), true));
    }

    private NBTTagCompound compoundValue(int value) {
        NBTTagCompound result = new NBTTagCompound();
        result.setInteger("value", value);
        return result;
    }

    @Test
    public void numericTagsMatchAcrossIntegralTypesLikeBetterQuesting() {
        assertTrue(MinecraftPlayerConsumptionPort.compareNbt(new NBTTagInt(12), new NBTTagShort((short) 12), false));
    }

    @Test
    public void itemPlanConsumesAvailablePartialAmountPerRequirementWithoutMutatingInput() {
        ItemStack input = new ItemStack(new Item(), 64);
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(input));
        MinecraftPlayerConsumptionPort.ItemPlan plan = port().planItems(before, Arrays.asList(40L, 40L),
            (stack, requirement) -> true);

        assertNotNull(plan);
        assertEquals(Arrays.asList(40L, 24L), plan.appliedAmounts);
        assertNull(plan.inventory.get(0));
        assertEquals(64, input.stackSize);
    }

    @Test
    public void itemPlanHonorsSelectedStackAndLeavesOtherMatchingStacksUntouched() {
        ItemStack unselected = new ItemStack(new Item(), 32);
        ItemStack selected = new ItemStack(new Item(), 64);
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(unselected, selected));

        MinecraftPlayerConsumptionPort.ItemPlan plan = port().planItems(before, Arrays.asList(40L),
            (stack, requirement) -> true, 1);

        assertNotNull(plan);
        assertEquals(Collections.singletonList(40L), plan.appliedAmounts);
        assertEquals(32, plan.inventory.get(0).stackSize);
        assertEquals(24, plan.inventory.get(1).stackSize);
        assertEquals(32, unselected.stackSize);
        assertEquals(64, selected.stackSize);
    }

    @Test
    public void selectedStackUsesBetterQuestingSubmitStationSixtyFourItemLimit() {
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(new ItemStack(new Item(), 128)));

        MinecraftPlayerConsumptionPort.ItemPlan plan = port().planItems(before, Arrays.asList(80L),
            (stack, requirement) -> true, 0);

        assertNotNull(plan);
        assertEquals(Collections.singletonList(64L), plan.appliedAmounts);
        assertEquals(64, plan.inventory.get(0).stackSize);
    }

    @Test
    public void selectedStackRequestIsRejectedAfterTheCapturedSlotChanges() {
        ItemStack captured = new ItemStack(new Item(), 8);
        ItemStack changed = new ItemStack(captured.getItem(), 7);
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "1");
        parameters.put("item.0.registryName", "test:item");
        parameters.put("item.0.meta", "0");
        parameters.put(ConsumptionRequest.SELECTION_SLOT_PARAMETER, "0");
        parameters.put(ConsumptionRequest.SELECTION_FINGERPRINT_PARAMETER,
            MinecraftPlayerConsumptionPort.fingerprint(captured));
        UUID player = new UUID(5L, 6L);
        ConsumptionRequest request = new ConsumptionRequest("selected-stack", "s2", player,
            ParticipantId.player(player), new UUID(7L, 8L), 1, "task", "item", Arrays.asList(4L), parameters, 9L);

        assertNull(port().planItems(Arrays.asList(changed), request));
    }

    @Test
    public void fluidPlanDrainsVariableContainersWithoutMutatingInput() {
        TestTank tank = new TestTank();
        ItemStack input = tank(2, tank, 1000);
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(input, null, null));

        List<ItemStack> after = port().planFluids(before, fluidRequest(1500));

        assertNotNull(after);
        assertEquals(2000, totalFluid(before));
        assertEquals(500, totalFluid(after));
        assertEquals(2, totalItems(after));
    }

    @Test
    public void fluidPlanRecordsAvailablePartialAmountInsteadOfRejectingWholeSubmission() {
        TestTank tank = new TestTank();
        ItemStack input = tank(1, tank, 1000);
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(input, null));

        MinecraftPlayerConsumptionPort.FluidPlan plan = port().planFluidsDetailed(before, fluidRequest(1500));

        assertNotNull(plan);
        assertEquals(Collections.singletonList(1000L), plan.appliedAmounts);
        assertEquals(0, totalFluid(plan.inventory));
        assertEquals(1000, totalFluid(before));
    }

    @Test
    public void fluidPlanReportsWholeAmountDrainedFromAnIndivisibleContainer() {
        TestTank tank = new TestTank();
        ItemStack input = tank(1, tank, 750);
        input.getTagCompound().setBoolean("indivisible", true);
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(input, null));

        MinecraftPlayerConsumptionPort.FluidPlan plan = port().planFluidsDetailed(before, fluidRequest(600));

        assertNotNull(plan);
        assertEquals(Collections.singletonList(750L), plan.appliedAmounts);
        assertEquals(0, totalFluid(plan.inventory));
        assertEquals(750, totalFluid(before));
    }

    @Test
    public void heldFluidSubmissionConsumesAtMostOneContainerFromSelectedStack() {
        TestTank tank = new TestTank();
        ItemStack selected = tank(2, tank, 1000);
        ItemStack unselected = tank(1, tank, 1000);
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(selected, unselected, null, null));
        ConsumptionRequest request = fluidRequest(2500, true, 0,
            MinecraftPlayerConsumptionPort.fingerprint(selected), false);

        MinecraftPlayerConsumptionPort.FluidPlan plan = port().planFluidsDetailed(before, request);

        assertNotNull(plan);
        assertEquals(Collections.singletonList(1000L), plan.appliedAmounts);
        assertEquals(2000, totalFluid(plan.inventory));
        assertEquals(3, totalItems(plan.inventory));
        assertEquals(2, selected.stackSize);
    }

    @Test
    public void heldFluidSubmissionRejectsChangedSelectedStackFingerprint() {
        TestTank tank = new TestTank();
        ItemStack captured = tank(2, tank, 1000);
        ItemStack changed = tank(1, tank, 1000);
        ConsumptionRequest request = fluidRequest(1000, true, 0,
            MinecraftPlayerConsumptionPort.fingerprint(captured), false);

        assertNull(port().planFluidsDetailed(Arrays.asList(changed, null), request));
        assertEquals(1, changed.stackSize);
        assertEquals(1000, totalFluid(Arrays.asList(changed)));
    }

    @Test
    public void fluidConsumptionMatchesNbtTagPresenceExactlyWhenNbtIsNotIgnored() {
        TestTank tank = new TestTank();
        ItemStack input = tank(1, tank, 1000);
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(input, null));
        ConsumptionRequest noTag = fluidRequest(1000, false, -1, null, false);
        ConsumptionRequest emptyTag = fluidRequest(1000, false, -1, null, true);

        assertNotNull(port().planFluidsDetailed(before, noTag));
        assertNull(port().planFluidsDetailed(before, emptyTag));
        assertEquals(1000, totalFluid(before));
    }

    @Test
    public void fluidConsumptionRequiresExactFluidNameWhenNbtMatchingIsIgnored() {
        TestTank tank = new TestTank();
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(tank(1, tank, 1000), null));

        assertNull(port().planFluidsDetailed(before, fluidRequest(1000, "another_test_fluid")));
        assertEquals("a different registered fluid must not be drained", 1000, totalFluid(before));
    }

    @Test
    public void fluidPlanRejectsWhenReturnedContainerHasNoInventorySpace() {
        TestTank tank = new TestTank();
        List<ItemStack> full = new ArrayList<ItemStack>(Arrays.asList(tank(2, tank, 1000)));
        assertNull(port().planFluids(full, fluidRequest(1000, "galaxy_test_fluid")));
        assertEquals(2000, totalFluid(full));
    }

    private MinecraftPlayerConsumptionPort port() {
        return new MinecraftPlayerConsumptionPort(playerId -> null, player -> {}, new FakeFluidAdapter());
    }

    private ConsumptionRequest fluidRequest(long amount) {
        return fluidRequest(amount, "galaxy_test_fluid");
    }

    private ConsumptionRequest fluidRequest(long amount, String name) {
        return fluidRequest(amount, name, true, -1, null, false);
    }

    private ConsumptionRequest fluidRequest(long amount, boolean ignoreNbt, int slot, String fingerprint,
        boolean requiredTagPresent) {
        return fluidRequest(amount, "galaxy_test_fluid", ignoreNbt, slot, fingerprint, requiredTagPresent);
    }

    private ConsumptionRequest fluidRequest(long amount, String name, boolean ignoreNbt, int slot,
        String fingerprint, boolean requiredTagPresent) {
        UUID player = new UUID(1L, 2L);
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("fluid.count", "1");
        parameters.put("fluid.0.name", name);
        parameters.put("fluid.0.nbt", "{}");
        parameters.put("fluid.0.nbtPresent", Boolean.toString(requiredTagPresent));
        parameters.put("ignoreNBT", Boolean.toString(ignoreNbt));
        if (slot >= 0) {
            parameters.put(ConsumptionRequest.SELECTION_SLOT_PARAMETER, Integer.toString(slot));
            parameters.put(ConsumptionRequest.SELECTION_FINGERPRINT_PARAMETER, fingerprint);
        }
        return new ConsumptionRequest("submission", "test", player, ParticipantId.player(player),
            new UUID(3L, 4L), 1, "task", "fluid", Arrays.asList(amount), parameters, 5L);
    }

    private ItemStack tank(int count, TestTank item, int amount) {
        ItemStack stack = new ItemStack(item, count);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("amount", amount);
        stack.setTagCompound(tag);
        return stack;
    }

    private int totalFluid(List<ItemStack> values) {
        int total = 0;
        for (ItemStack value : values) {
            int amount = value == null || !value.hasTagCompound() ? 0 : value.getTagCompound().getInteger("amount");
            total += amount * (value == null ? 0 : value.stackSize);
        }
        return total;
    }

    private int totalItems(List<ItemStack> values) {
        int total = 0;
        for (ItemStack value : values) if (value != null) total += value.stackSize;
        return total;
    }

    private static final class TestTank extends Item {
        private TestTank() { setMaxStackSize(16); }
    }

    private static final class FakeFluidAdapter implements QuestFluidContainerAdapter {
        @Override public FluidView inspect(ItemStack container) {
            int amount = container.hasTagCompound() ? container.getTagCompound().getInteger("amount") : 0;
            return amount <= 0 ? null : new FluidView("galaxy_test_fluid", amount, null);
        }
        @Override public FluidDrain drain(ItemStack container, int maximumAmount) {
            FluidView stored = inspect(container);
            if (stored == null || maximumAmount <= 0) return null;
            int drained = container.getTagCompound().getBoolean("indivisible")
                ? stored.getAmount() : Math.min(stored.getAmount(), maximumAmount);
            container.getTagCompound().setInteger("amount", stored.getAmount() - drained);
            return new FluidDrain(new FluidView(stored.getName(), drained, null), container);
        }
    }
}
