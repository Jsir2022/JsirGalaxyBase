package com.jsirgalaxybase.modules.core.vault.client;

import com.jsirgalaxybase.terminal.network.OpenTerminalRequestMessage;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;
import com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft.BaseVaultSortRequestMessage;

import java.util.Collections;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Client skin for the server-owned personal Base Vault container. */
public final class GuiBaseVault extends GuiContainer {

    private static final int PANEL = 0xEE142330;
    private static final int PANEL_DARK = 0xEE0B151F;
    private static final int BORDER = 0xFF52728A;
    private static final int TEXT = 0xFFE1EDF5;
    private static final int MUTED = 0xFF9CB5C7;
    private static final int RETURN_BUTTON_ID = 41;
    private static final int SORT_BUTTON_ID = 42;

    public GuiBaseVault(InventoryPlayer ignored) {
        super(createClientContainer(ignored));
        xSize = 212;
        ySize = 190;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new VaultSortButton(SORT_BUTTON_ID, guiLeft + 127, guiTop + 5, 18, 17));
        buttonList.add(new TerminalReturnButton(RETURN_BUTTON_ID, guiLeft + xSize - 29, guiTop + 5, 21, 17));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        GuiButton sortButton = null;
        for (Object value : buttonList) {
            GuiButton button = (GuiButton) value;
            if (button.id == SORT_BUTTON_ID) {
                sortButton = button;
                break;
            }
        }
        if (sortButton != null && mouseX >= sortButton.xPosition && mouseY >= sortButton.yPosition
            && mouseX < sortButton.xPosition + sortButton.width && mouseY < sortButton.yPosition + sortButton.height) {
            drawHoveringText(Collections.singletonList("整理保险箱"), mouseX, mouseY, fontRendererObj);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == SORT_BUTTON_ID) {
            TerminalNetwork.CHANNEL.sendToServer(new BaseVaultSortRequestMessage());
        } else if (button.id == RETURN_BUTTON_ID) {
            mc.thePlayer.closeScreen();
            TerminalNetwork.CHANNEL.sendToServer(new OpenTerminalRequestMessage());
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        drawRect(guiLeft - 3, guiTop - 3, guiLeft + xSize + 3, guiTop + ySize + 3, BORDER);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, PANEL);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + 27, PANEL_DARK);
        drawRect(guiLeft + 8, guiTop + 29, guiLeft + xSize - 8, guiTop + 88, 0xCC101C27);
        drawRect(guiLeft + 8, guiTop + 94, guiLeft + xSize - 8, guiTop + 181, 0xCC101C27);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slotFrame(18 + col * 18, 32 + row * 18);
                slotFrame(18 + col * 18, 98 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) slotFrame(18 + col * 18, 156);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString("银河终端", 10, 9, TEXT);
        fontRendererObj.drawString("27 格", 151, 9, MUTED);
    }

    private void slotFrame(int x, int y) {
        drawRect(guiLeft + x - 1, guiTop + y - 1, guiLeft + x + 17, guiTop + y + 17, 0xFF355066);
        drawRect(guiLeft + x, guiTop + y, guiLeft + x + 16, guiTop + y + 16, 0xFF0C141C);
    }

    private static Container createClientContainer(InventoryPlayer playerInventory) {
        return new ClientContainer(playerInventory);
    }

    private static final class TerminalReturnButton extends GuiButton {
        private TerminalReturnButton(int id, int x, int y, int width, int height) {
            super(id, x, y, width, height, "<");
        }

        @Override
        public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
            if (!visible) return;
            boolean hovered = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
            int border = hovered ? 0xFF74A4C3 : BORDER;
            int fill = hovered ? 0xFF27475E : PANEL_DARK;
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, border);
            drawRect(xPosition + 2, yPosition + 2, xPosition + width - 2, yPosition + height - 2, fill);
            drawCenteredString(minecraft.fontRenderer, displayString, xPosition + width / 2, yPosition + 4,
                hovered ? TEXT : MUTED);
        }
    }

    private static final class VaultSortButton extends GuiButton {
        private VaultSortButton(int id, int x, int y, int width, int height) {
            super(id, x, y, width, height, "");
        }

        @Override
        public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
            if (!visible) return;
            boolean hovered = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
            int border = hovered ? 0xFF74A4C3 : BORDER;
            int fill = hovered ? 0xFF27475E : PANEL_DARK;
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, border);
            drawRect(xPosition + 2, yPosition + 2, xPosition + width - 2, yPosition + height - 2, fill);
            int icon = hovered ? TEXT : MUTED;
            // Three rows converge toward the lower right: a compact sort/stack icon.
            drawRect(xPosition + 4, yPosition + 4, xPosition + 13, yPosition + 6, icon);
            drawRect(xPosition + 5, yPosition + 8, xPosition + 12, yPosition + 10, icon);
            drawRect(xPosition + 6, yPosition + 12, xPosition + 11, yPosition + 14, icon);
        }
    }

    private static final class ClientContainer extends Container {
        private ClientContainer(InventoryPlayer playerInventory) {
            InventoryBasic vault = new InventoryBasic("Base Vault", true, 27);
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    addSlotToContainer(new Slot(vault, column + row * 9, 18 + column * 18, 32 + row * 18));
                }
            }
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9, 18 + column * 18, 98 + row * 18));
                }
            }
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(playerInventory, column, 18 + column * 18, 156));
            }
        }

        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) { return true; }

        @Override
        public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer player) {
            // Keep client prediction aligned with the authoritative container:
            // a persistent Vault does not allow creative middle-click cloning.
            return mode == 3 ? null : super.slotClick(slotId, clickedButton, mode, player);
        }

        /**
         * Mirror the server container's Shift-click path. Vanilla containers
         * perform this prediction locally before the authoritative slot updates
         * arrive; leaving the client implementation as Container's no-op makes
         * the two sides disagree for the duration of a quick move.
         */
        @Override
        public ItemStack transferStackInSlot(EntityPlayer player, int containerSlot) {
            Slot slot = containerSlot < 0 || containerSlot >= inventorySlots.size()
                ? null : (Slot) inventorySlots.get(containerSlot);
            if (slot == null || !slot.getHasStack()) return null;

            ItemStack source = slot.getStack();
            ItemStack original = source.copy();
            if (containerSlot < 27) {
                if (!mergeItemStack(source, 27, inventorySlots.size(), true)) return null;
            } else if (!mergeItemStack(source, 0, 27, false)) {
                return null;
            }
            if (source.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }
            if (source.stackSize == original.stackSize) return null;
            slot.onPickupFromSlot(player, source);
            return original;
        }
    }
}
