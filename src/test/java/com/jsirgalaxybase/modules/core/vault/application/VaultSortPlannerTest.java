package com.jsirgalaxybase.modules.core.vault.application;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class VaultSortPlannerTest {

    private static final Item ALPHA = new Item().setUnlocalizedName("vault_sort_alpha");
    private static final Item ZULU = new Item().setUnlocalizedName("vault_sort_zulu");

    @Test
    public void sortsByStableIdentityAndMergesOnlyCompatibleStacks() {
        List<ItemStack> source = new ArrayList<ItemStack>();
        source.add(new ItemStack(ZULU, 4));
        source.add(new ItemStack(ALPHA, 2));
        source.add(new ItemStack(ALPHA, 3));

        List<ItemStack> sorted = VaultSortPlanner.sort(source, 4);

        assertEquals(ALPHA, sorted.get(0).getItem());
        assertEquals(5, sorted.get(0).stackSize);
        assertEquals(ZULU, sorted.get(1).getItem());
        assertEquals(4, sorted.get(1).stackSize);
        assertEquals(null, sorted.get(2));
        assertEquals(null, sorted.get(3));
    }
}
