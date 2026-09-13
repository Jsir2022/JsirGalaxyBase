package com.jsirgalaxybase.client.ui2.render;

import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.client.ui2.font.MinecraftFontService;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.text.TextStyle;

final class MinecraftTextBackend {
    private final MinecraftFontService fonts;
    MinecraftTextBackend(MinecraftFontService fonts) { this.fonts = fonts; }
    void draw(String text, UiRect bounds, int color, TextStyle style) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        fonts.render(text,bounds,color,style);
    }
}
