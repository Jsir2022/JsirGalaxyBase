package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ConsumptionRequest;
import com.jsirgalaxybase.quest.core.ParticipantId;

import org.junit.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class MinecraftPlayerConsumptionPortTest {
    @Test public void xpConsumptionUsesAvailablePartialAmountWithoutOverdrawing() {
        assertEquals(40L, MinecraftPlayerConsumptionPort.xpDeduction(100L, 40L));
        assertEquals(100L, MinecraftPlayerConsumptionPort.xpDeduction(100L, 140L));
        assertEquals(0L, MinecraftPlayerConsumptionPort.xpDeduction(100L, -1L));
    }
    @Test
    public void partialCompoundRequiresOnlyDeclaredTags() {
        NBTTagCompound required = new NBTTagCompound();
        required.setInteger("grade", 2);
        NBTTagCompound actual = new NBTTagCompound();
        actual.setInteger("grade", 2);
        actual.setString("owner", "player");

        assertTrue(MinecraftPlayerConsumptionPort.compareNbt(required, actual, true));
        assertFalse(MinecraftPlayerConsumptionPort.compareNbt(required, actual, false));
    }

    @Test
    public void partialListDoesNotReuseOneActualEntry() {
        NBTTagList required = new NBTTagList();
        required.appendTag(new NBTTagInt(7));
        required.appendTag(new NBTTagInt(7));
        NBTTagList actual = new NBTTagList();
        actual.appendTag(new NBTTagInt(7));
        actual.appendTag(new NBTTagInt(8));

        assertFalse(MinecraftPlayerConsumptionPort.compareNbt(required, actual, true));
    }

    @Test
    public void numericTagsMatchAcrossIntegralTypesLikeBetterQuesting() {
        assertTrue(MinecraftPlayerConsumptionPort.compareNbt(new NBTTagInt(12), new NBTTagShort((short) 12), false));
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
        UUID player = new UUID(1L, 2L);
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("fluid.count", "1");
        parameters.put("fluid.0.name", name);
        parameters.put("fluid.0.nbt", "{}");
        parameters.put("ignoreNBT", "true");
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
            int drained = Math.min(stored.getAmount(), maximumAmount);
            container.getTagCompound().setInteger("amount", stored.getAmount() - drained);
            return new FluidDrain(new FluidView(stored.getName(), drained, null), container);
        }
    }
}
