package com.jsirgalaxybase.client.ui2.render;

import org.lwjgl.opengl.GL11;

import com.jsirgalaxybase.client.ui2.render.RoundedRectGeometry.Point;
import com.jsirgalaxybase.ui2.geometry.UiRect;

/** Smooth fixed-function shapes with a one-physical-pixel alpha coverage fringe. */
final class MinecraftShapeBackend {
    private final float density;
    private final float fringe;
    private final float halfFringe;

    MinecraftShapeBackend(float density) {
        this.density = Math.max(1F, density);
        this.fringe = RoundedRectGeometry.hairline(this.density);
        this.halfFringe = RoundedRectGeometry.coverageInset(this.density);
    }

    void surface(UiRect bounds, int top, int bottom, int radius) {
        if (!usable(bounds)) return;
        float r = RoundedRectGeometry.clampRadius(bounds.getWidth(), bounds.getHeight(), radius);
        int segments = RoundedRectGeometry.segments(r, density);
        Point[] inner = RoundedRectGeometry.outline(bounds.getX() + halfFringe,
            bounds.getY() + halfFringe, bounds.getRight() - halfFringe,
            bounds.getBottom() - halfFringe, Math.max(0F, r - halfFringe), segments);
        Point[] outer = RoundedRectGeometry.outline(bounds.getX() - halfFringe,
            bounds.getY() - halfFringe, bounds.getRight() + halfFringe,
            bounds.getBottom() + halfFringe, r + halfFringe, segments);
        prepare();
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        color(mix(top, bottom, 0.5F));
        GL11.glVertex2f(bounds.getX() + bounds.getWidth() / 2F,
            bounds.getY() + bounds.getHeight() / 2F);
        for (Point point : inner) vertex(point, vertical(top, bottom, bounds, point.getY()));
        vertex(inner[0], vertical(top, bottom, bounds, inner[0].getY()));
        GL11.glEnd();
        fringe(inner, outer, top, bottom, bounds, false);
    }

    void border(UiRect bounds, int argb, int radius) {
        if (!usable(bounds)) return;
        float r = RoundedRectGeometry.clampRadius(bounds.getWidth(), bounds.getHeight(), radius);
        int segments = RoundedRectGeometry.segments(r, density);
        float width = fringe;
        if (bounds.getWidth() <= width * 2F || bounds.getHeight() <= width * 2F) {
            surface(bounds, argb, argb, radius);
            return;
        }
        Point[] outer = RoundedRectGeometry.outline(bounds.getX(), bounds.getY(), bounds.getRight(),
            bounds.getBottom(), r, segments);
        Point[] inner = RoundedRectGeometry.outline(bounds.getX() + width, bounds.getY() + width,
            bounds.getRight() - width, bounds.getBottom() - width, Math.max(0F, r - width), segments);
        Point[] outside = RoundedRectGeometry.outline(bounds.getX() - halfFringe,
            bounds.getY() - halfFringe, bounds.getRight() + halfFringe,
            bounds.getBottom() + halfFringe, r + halfFringe, segments);
        Point[] inside = RoundedRectGeometry.outline(bounds.getX() + width + halfFringe,
            bounds.getY() + width + halfFringe, bounds.getRight() - width - halfFringe,
            bounds.getBottom() - width - halfFringe,
            Math.max(0F, r - width - halfFringe), segments);
        prepare();
        ring(outer, inner, argb, argb);
        ring(outside, outer, transparent(argb), argb);
        ring(inner, inside, argb, transparent(argb));
    }

    void shadow(UiRect bounds, int argb, int radius, int elevation) {
        int layers = Math.max(1, Math.min(6, elevation));
        for (int i = layers; i >= 1; i--) {
            int alpha = ((argb >>> 24) & 255) * (layers - i + 1) / (layers * 3);
            UiRect expanded = new UiRect(bounds.getX() - i, bounds.getY() + i / 2,
                bounds.getWidth() + i * 2, bounds.getHeight() + i * 2);
            surface(expanded, withAlpha(argb, alpha), withAlpha(argb, alpha), radius + i);
        }
    }

    void line(UiRect bounds, int argb) { surface(bounds, argb, argb, 0); }

    void chartLine(String encoded, int argb) {
        prepare(); color(argb); GL11.glLineWidth(Math.max(1F, density)); GL11.glBegin(GL11.GL_LINE_STRIP);
        for(String value:encoded.split(";")){String[] pair=value.split(",");if(pair.length!=2)continue;try{GL11.glVertex2f(Float.parseFloat(pair[0]),Float.parseFloat(pair[1]));}catch(NumberFormatException ignored){}}
        GL11.glEnd(); GL11.glLineWidth(1F);
    }

    void chartVolume(String encoded, int argb) {
        for(String value:encoded.split(";")){String[] p=value.split(",");if(p.length!=4)continue;try{surface(new UiRect(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Math.max(1,Integer.parseInt(p[2])),Math.max(1,Integer.parseInt(p[3]))),argb,argb,0);}catch(NumberFormatException ignored){}}
    }

    private void fringe(Point[] inner, Point[] outer, int top, int bottom, UiRect bounds,
        boolean ignored) {
        prepare();
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (int i = 0; i < inner.length; i++) {
            int next = (i + 1) % inner.length;
            vertex(inner[i], vertical(top, bottom, bounds, inner[i].getY()));
            vertex(inner[next], vertical(top, bottom, bounds, inner[next].getY()));
            vertex(outer[next], transparent(vertical(top, bottom, bounds, outer[next].getY())));
            vertex(inner[i], vertical(top, bottom, bounds, inner[i].getY()));
            vertex(outer[i], transparent(vertical(top, bottom, bounds, outer[i].getY())));
            vertex(outer[next], transparent(vertical(top, bottom, bounds, outer[next].getY())));
        }
        GL11.glEnd();
    }

    private static void ring(Point[] outer, Point[] inner, int outerColor, int innerColor) {
        int count = Math.min(outer.length, inner.length);
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            vertex(outer[i], outerColor); vertex(outer[next], outerColor);
            vertex(inner[next], innerColor);
            vertex(outer[i], outerColor); vertex(inner[next], innerColor);
            vertex(inner[i], innerColor);
        }
        GL11.glEnd();
    }

    private static void prepare() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
    }
    private static boolean usable(UiRect bounds) {
        return bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0;
    }
    private static void vertex(Point point, int argb) {
        color(argb); GL11.glVertex2f(point.getX(), point.getY());
    }
    private static int vertical(int top, int bottom, UiRect bounds, float y) {
        return mix(top, bottom, (y - bounds.getY()) / Math.max(1F, bounds.getHeight()));
    }
    private static int transparent(int argb) { return argb & 0x00FFFFFF; }
    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xFFFFFF);
    }
    private static int mix(int a, int b, float ratio) {
        float r = Math.max(0F, Math.min(1F, ratio));
        int aa = Math.round(((a >>> 24) & 255) * (1F - r) + ((b >>> 24) & 255) * r);
        int rr = Math.round(((a >>> 16) & 255) * (1F - r) + ((b >>> 16) & 255) * r);
        int gg = Math.round(((a >>> 8) & 255) * (1F - r) + ((b >>> 8) & 255) * r);
        int bb = Math.round((a & 255) * (1F - r) + (b & 255) * r);
        return aa << 24 | rr << 16 | gg << 8 | bb;
    }
    static void color(int argb) {
        GL11.glColor4f(((argb >>> 16) & 255) / 255F, ((argb >>> 8) & 255) / 255F,
            (argb & 255) / 255F, ((argb >>> 24) & 255) / 255F);
    }
}
