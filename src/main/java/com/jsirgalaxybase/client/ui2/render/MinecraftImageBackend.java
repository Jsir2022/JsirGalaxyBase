package com.jsirgalaxybase.client.ui2.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.ui2.geometry.UiRect;

final class MinecraftImageBackend {
    void draw(UiRect bounds, int color, String resource) {
        if (bounds == null || resource == null) return;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(resource));
        MinecraftShapeBackend.color(color);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(bounds.getX(), bounds.getBottom(), 0, 0, 1);
        tessellator.addVertexWithUV(bounds.getRight(), bounds.getBottom(), 0, 1, 1);
        tessellator.addVertexWithUV(bounds.getRight(), bounds.getY(), 0, 1, 0);
        tessellator.addVertexWithUV(bounds.getX(), bounds.getY(), 0, 0, 0);
        tessellator.draw();
    }
}
