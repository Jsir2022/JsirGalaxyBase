package com.jsirgalaxybase.terminal.client.component;

import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

/** Shared compact text measurements for dense market charts and statistics. */
final class MarketCompactText {

    static final float CONTENT_SCALE = 0.78F;
    static final float AXIS_SCALE = 0.68F;

    private MarketCompactText() {}

    static void draw(FontRenderer font, String text, int x, int y, int color, float scale) {
        if (font == null || text == null || text.isEmpty()) { return; }
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);
        GL11.glScalef(scale, scale, 1.0F);
        font.drawStringWithShadow(text, 0, 0, color);
        GL11.glPopMatrix();
    }

    static int width(FontRenderer font, String text, float scale) {
        return font == null || text == null ? 0 : Math.round(font.getStringWidth(text) * scale);
    }

    static String trim(FontRenderer font, String text, int scaledWidth, float scale) {
        if (font == null || text == null) { return ""; }
        int sourceWidth = Math.max(1, Math.round(scaledWidth / Math.max(0.01F, scale)));
        return font.trimStringToWidth(text, sourceWidth);
    }
}
