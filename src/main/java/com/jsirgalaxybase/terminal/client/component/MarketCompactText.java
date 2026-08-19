package com.jsirgalaxybase.terminal.client.component;

import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.terminal.client.TerminalNumberFormat;

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

    /** Keeps exact prices and folds only quantities in the narrow depth panel. */
    static String compactOrderBookLine(String line) {
        if (line == null) { return "--"; }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("[买卖]价\\s+([0-9,]+)\\s*\\|\\s*剩余\\s+([0-9,]+).*")
            .matcher(line);
        if (!matcher.matches()) { return line; }
        try {
            long quantity = Long.parseLong(matcher.group(2).replace(",", ""));
            return matcher.group(1) + "x" + TerminalNumberFormat.compactQuantity(quantity);
        } catch (NumberFormatException ignored) {
            return line;
        }
    }

    /** Reads the untouched server row used for an actionable order-book click. */
    static long[] exactOrderBookLevel(String line) {
        if (line == null) { return null; }
        java.util.regex.Matcher verbose = java.util.regex.Pattern
            .compile("[买卖]价\\s+([0-9,]+)\\s*\\|\\s*剩余\\s+([0-9,]+).*")
            .matcher(line);
        java.util.regex.Matcher compact = java.util.regex.Pattern
            .compile("([0-9,]+)\\s*[xX]\\s*([0-9,]+)")
            .matcher(line);
        java.util.regex.Matcher selected = verbose.matches() ? verbose : compact.find() ? compact : null;
        if (selected == null) { return null; }
        try {
            long price = Long.parseLong(selected.group(1).replace(",", ""));
            long quantity = Long.parseLong(selected.group(2).replace(",", ""));
            return price > 0L && quantity > 0L ? new long[] { price, quantity } : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
