package com.jsirgalaxybase.client.gui.framework;

import net.minecraft.client.gui.Gui;

public final class RoundedRectPainter {

    private static final float DEFAULT_RADIUS_RATIO = 0.16F;
    private static final int DEFAULT_MAX_RADIUS = 8;

    private RoundedRectPainter() {}

    public static void draw(GuiRect bounds, int borderColor, int fillColor) {
        if (bounds == null) {
            return;
        }
        draw(bounds.getX(), bounds.getY(), bounds.getRight(), bounds.getBottom(), borderColor, fillColor);
    }

    public static void draw(int x, int y, int right, int bottom, int borderColor, int fillColor) {
        int radius = radiusFor(right - x, bottom - y);
        drawSolid(x, y, right, bottom, radius, borderColor);
        drawSolid(x + 1, y + 1, right - 1, bottom - 1, Math.max(0, radius - 1), fillColor);
    }

    public static void drawSolid(int x, int y, int right, int bottom, int color) {
        drawSolid(x, y, right, bottom, radiusFor(right - x, bottom - y), color);
    }

    public static void drawSolid(int x, int y, int right, int bottom, int radius, int color) {
        if (right <= x || bottom <= y) {
            return;
        }
        int width = right - x;
        int height = bottom - y;
        int safeRadius = Math.min(Math.max(0, radius), Math.min(width, height) / 2);
        if (safeRadius < 2 || width < 6 || height < 6) {
            Gui.drawRect(x, y, right, bottom, color);
            return;
        }
        for (int row = 0; row < height; row++) {
            int inset = roundedInset(safeRadius, row, height);
            Gui.drawRect(x + inset, y + row, right - inset, y + row + 1, color);
        }
    }

    public static int radiusFor(GuiRect bounds) {
        return bounds == null ? 0 : radiusFor(bounds.getWidth(), bounds.getHeight());
    }

    public static int radiusFor(int width, int height) {
        int minSide = Math.max(0, Math.min(width, height));
        if (minSide < 6) {
            return 0;
        }
        int ratioRadius = Math.round(minSide * DEFAULT_RADIUS_RATIO);
        return Math.max(2, Math.min(Math.min(DEFAULT_MAX_RADIUS, minSide / 2), ratioRadius));
    }

    private static int roundedInset(int radius, int row, int height) {
        if (row >= radius && row < height - radius) {
            return 0;
        }
        int localRow = row < radius ? row : height - 1 - row;
        double center = radius - 0.5D;
        double dy = center - localRow;
        double inside = Math.max(0.0D, radius * radius - dy * dy);
        int inset = (int) Math.floor(radius - Math.sqrt(inside));
        return Math.max(0, Math.min(radius, inset));
    }
}
