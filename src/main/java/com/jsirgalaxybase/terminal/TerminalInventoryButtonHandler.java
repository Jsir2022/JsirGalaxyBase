package com.jsirgalaxybase.terminal;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.client.event.GuiScreenEvent;

import com.jsirgalaxybase.terminal.network.OpenTerminalRequestMessage;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.ReflectionHelper;

@SideOnly(Side.CLIENT)
public class TerminalInventoryButtonHandler {

    @SubscribeEvent
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiInventory) {
            GuiInventory guiInventory = (GuiInventory) event.gui;
            List buttonList = event.buttonList;
            buttonList.add(new TerminalOpenButton(getGuiLeft(guiInventory) + 82, getGuiTop(guiInventory) + 61));
            return;
        }

        if (event.gui instanceof GuiContainerCreative) {
            GuiContainerCreative guiCreative = (GuiContainerCreative) event.gui;
            List buttonList = event.buttonList;
            buttonList.add(new TerminalOpenButton(getGuiLeft(guiCreative) + 173, getGuiTop(guiCreative) + 6));
        }
    }

    private int getGuiLeft(GuiContainer gui) {
        return ReflectionHelper.getPrivateValue(GuiContainer.class, gui, "guiLeft", "field_147003_i");
    }

    private int getGuiTop(GuiContainer gui) {
        return ReflectionHelper.getPrivateValue(GuiContainer.class, gui, "guiTop", "field_147009_r");
    }

    @SideOnly(Side.CLIENT)
    private static final class TerminalOpenButton extends GuiButton {

        private TerminalOpenButton(int x, int y) {
            super(1864201, x, y, 20, 20, "GB");
        }

        @Override
        public boolean mousePressed(net.minecraft.client.Minecraft minecraft, int mouseX, int mouseY) {
            if (!super.mousePressed(minecraft, mouseX, mouseY)) {
                return false;
            }

            TerminalNetwork.CHANNEL.sendToServer(new OpenTerminalRequestMessage());
            return true;
        }
    }
}