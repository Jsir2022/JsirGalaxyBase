package com.jsirgalaxybase.client.ui2.lab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

public class Ui2LabLocalContainerTest {
    @Test
    public void shiftClickMovesOnlyInsideDisposableLabInventories() {
        Ui2LabLocalContainer container = new Ui2LabLocalContainer();
        assertEquals(14, container.inventorySlots.size());
        for (Object value : container.inventorySlots) ((Slot) value).putStack(null);
        ((Slot) container.inventorySlots.get(0)).putStack(new ItemStack(new Item(), 64));

        ItemStack moved = container.transferStackInSlot(null, 0);

        assertNotNull(moved);
        assertFalse(((Slot) container.inventorySlots.get(0)).getHasStack());
        assertEquals(64, ((Slot) container.inventorySlots.get(9)).getStack().stackSize);

        container.transferStackInSlot(null, 9);
        assertEquals(64, ((Slot) container.inventorySlots.get(0)).getStack().stackSize);
    }
}
