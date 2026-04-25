package com.jsirgalaxybase.client.gui.framework;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class ResourceThemeTexture implements ThemeTexture {

    private final ResourceLocation location;

    public ResourceThemeTexture(ResourceLocation location) {
        this.location = location;
    }

    @Override
    public void draw(int x, int y, int width, int height, int tint) {
        if (width <= 0 || height <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) {
            return;
        }

        float alpha = (float) (tint >> 24 & 255) / 255.0F;
        float red = (float) (tint >> 16 & 255) / 255.0F;
        float green = (float) (tint >> 8 & 255) / 255.0F;
        float blue = (float) (tint & 255) / 255.0F;
        Tessellator tessellator = Tessellator.instance;

        minecraft.getTextureManager().bindTexture(location);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(red, green, blue, alpha);
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(x + width, y + height, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(x + width, y, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(x, y, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}