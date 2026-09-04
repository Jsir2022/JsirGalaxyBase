package com.jsirgalaxybase.modules.warehouse.client;

import java.util.Collections;

import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.terminal.network.OpenTerminalRequestMessage;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Pixel-native single Cell Bay view opened from the 银河仓储 terminal page. */
public final class GuiTerminalWarehouseBay extends GuiContainer {
    private static final int PANEL = 0xEE142330, DARK = 0xEE0B151F, BORDER = 0xFF52728A, TEXT = 0xFFE1EDF5, MUTED = 0xFF9CB5C7;
    private static final int RETURN_BUTTON_ID = 43;
    public GuiTerminalWarehouseBay(InventoryPlayer inventory) { super(new ClientContainer(inventory)); xSize = 212; ySize = 168; }
    @Override public void initGui() { super.initGui(); buttonList.add(new GuiButton(RETURN_BUTTON_ID, guiLeft + xSize - 28, guiTop + 5, 20, 17, "<")); }
    @Override protected void actionPerformed(GuiButton button) { if (button.id == RETURN_BUTTON_ID) { mc.thePlayer.closeScreen(); TerminalNetwork.CHANNEL.sendToServer(new OpenTerminalRequestMessage()); } }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) { super.drawScreen(mouseX, mouseY, partialTicks); if (mouseX >= guiLeft + 80 && mouseX < guiLeft + 132 && mouseY >= guiTop + 24 && mouseY < guiTop + 72) drawHoveringText(Collections.singletonList("仅接受一枚真实 AE2 存储单元"), mouseX, mouseY, fontRendererObj); }
    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F); drawRect(guiLeft - 3, guiTop - 3, guiLeft + xSize + 3, guiTop + ySize + 3, BORDER); drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, PANEL); drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + 25, DARK);
        drawRect(guiLeft + 8, guiTop + 27, guiLeft + xSize - 8, guiTop + 69, 0xCC101C27); drawRect(guiLeft + 79, guiTop + 24, guiLeft + 133, guiTop + 78, 0xFF355066); drawRect(guiLeft + 81, guiTop + 26, guiLeft + 131, guiTop + 76, 0xFF0C141C);
        drawRect(guiLeft + 8, guiTop + 80, guiLeft + xSize - 8, guiTop + 159, 0xCC101C27);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) slotFrame(18 + col * 18, 84 + row * 18);
        for (int col = 0; col < 9; col++) slotFrame(18 + col * 18, 142);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) { fontRendererObj.drawString("银河仓储 / 存储单元", 10, 8, TEXT); fontRendererObj.drawString("1 / 1", 155, 8, MUTED); fontRendererObj.drawString("从背包拖入一枚 AE2 存储单元", 17, 32, MUTED); fontRendererObj.drawString("不接入外部 AE 网络", 17, 48, MUTED); }
    private void slotFrame(int x, int y) { drawRect(guiLeft + x - 1, guiTop + y - 1, guiLeft + x + 17, guiTop + y + 17, 0xFF355066); drawRect(guiLeft + x, guiTop + y, guiLeft + x + 16, guiTop + y + 16, 0xFF0C141C); }
    private static final class ClientContainer extends Container {
        private ClientContainer(InventoryPlayer inventory) {
            InventoryBasic bay = new InventoryBasic("银河仓储存储单元槽", true, 1); addSlotToContainer(new Slot(bay, 0, 98, 28) { @Override public int getSlotStackLimit() { return 1; } });
            for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlotToContainer(new Slot(inventory, column + row * 9 + 9, 18 + column * 18, 84 + row * 18));
            for (int column = 0; column < 9; column++) addSlotToContainer(new Slot(inventory, column, 18 + column * 18, 142));
        }
        @Override public boolean canInteractWith(EntityPlayer player) { return true; }
        @Override public ItemStack transferStackInSlot(EntityPlayer player, int containerSlot) {
            Slot slot = containerSlot < 0 || containerSlot >= inventorySlots.size() ? null : (Slot) inventorySlots.get(containerSlot); if (slot == null || !slot.getHasStack()) return null;
            ItemStack source = slot.getStack(); ItemStack original = source.copy(); if (containerSlot == 0) { if (!mergeItemStack(source, 1, inventorySlots.size(), true)) return null; } else if (!mergeItemStack(source, 0, 1, false)) return null;
            if (source.stackSize == 0) slot.putStack(null); else slot.onSlotChanged(); if (source.stackSize == original.stackSize) return null; slot.onPickupFromSlot(player, source); return original;
        }
    }
}
