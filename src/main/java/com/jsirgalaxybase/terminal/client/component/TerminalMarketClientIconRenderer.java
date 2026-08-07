package com.jsirgalaxybase.terminal.client.component;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

@SideOnly(Side.CLIENT)
final class TerminalMarketClientIconRenderer {

    private static final RenderItem ITEM_RENDER = new RenderItem();

    private TerminalMarketClientIconRenderer() {}

    static boolean drawItemIcon(int x, int y, int size, ItemStack stack) {
        if (stack == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.fontRenderer == null || minecraft.getTextureManager() == null) {
            return false;
        }
        Gui.drawRect(x, y, x + size, y + size, 0xFF0C131B);
        Gui.drawRect(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF25303D);
        GL11.glPushMatrix();
        boolean lightingEnabled = false;
        try {
            float scale = size <= 18 ? 1.0F : (size - 4) / 16.0F;
            int renderX = size <= 18 ? x + Math.max(0, (size - 16) / 2) : 0;
            int renderY = size <= 18 ? y + Math.max(0, (size - 16) / 2) : 0;
            if (size > 18) {
                GL11.glTranslatef(x + 2, y + 2, 0.0F);
                GL11.glScalef(scale, scale, 1.0F);
            }
            RenderHelper.enableGUIStandardItemLighting();
            lightingEnabled = true;
            ITEM_RENDER.renderItemAndEffectIntoGUI(
                minecraft.fontRenderer,
                minecraft.getTextureManager(),
                stack,
                size > 18 ? 0 : renderX,
                size > 18 ? 0 : renderY);
            RenderHelper.disableStandardItemLighting();
            lightingEnabled = false;
            return true;
        } catch (RuntimeException ignored) {
            return false;
        } finally {
            if (lightingEnabled) {
                RenderHelper.disableStandardItemLighting();
            }
            GL11.glPopMatrix();
            // Do not leak the item renderer's depth state into panels or a later modal layer.
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
