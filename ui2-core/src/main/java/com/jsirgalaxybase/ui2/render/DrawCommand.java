package com.jsirgalaxybase.ui2.render;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.text.TextStyle;

/** Immutable, renderer-neutral paint instruction. */
public final class DrawCommand {
    public enum Kind {
        SURFACE, GRADIENT_SURFACE, SHADOW, BORDER, LINE, CHART_LINE, CHART_VOLUME, TEXT, ICON, IMAGE, EXTERNAL_REGION,
        CLIP_PUSH, CLIP_POP, TRANSFORM_PUSH, TRANSFORM_POP
    }

    private final Kind kind;
    private final int layer;
    private final UiRect bounds;
    private final int color;
    private final int secondaryColor;
    private final String text;
    private final int radius;
    private final int elevation;
    private final TextStyle textStyle;
    private final String resourceId;
    private final String externalId;

    private DrawCommand(Kind kind, int layer, UiRect bounds, int color, int secondaryColor, String text,
        int radius, int elevation, TextStyle textStyle, String resourceId, String externalId) {
        if (kind == null) throw new IllegalArgumentException("kind is required");
        this.kind = kind; this.layer = layer; this.bounds = bounds; this.color = color;
        this.secondaryColor = secondaryColor; this.text = text; this.radius = Math.max(0, radius);
        this.elevation = Math.max(0, elevation); this.textStyle = textStyle;
        this.resourceId = resourceId; this.externalId = externalId;
    }

    private static DrawCommand command(Kind kind, int layer, UiRect bounds, int color, int second,
        String text, int radius, int elevation, TextStyle style, String resource, String external) {
        return new DrawCommand(kind, layer, bounds, color, second, text, radius, elevation, style, resource, external);
    }

    public static DrawCommand surface(int layer, UiRect bounds, int color, int radius) {
        return command(Kind.SURFACE, layer, bounds, color, color, null, radius, 0, null, null, null);
    }
    public static DrawCommand gradientSurface(int layer, UiRect bounds, int topColor, int bottomColor, int radius) {
        return command(Kind.GRADIENT_SURFACE, layer, bounds, topColor, bottomColor, null, radius, 0, null, null, null);
    }
    public static DrawCommand shadow(int layer, UiRect bounds, int color, int radius, int elevation) {
        return command(Kind.SHADOW, layer, bounds, color, color, null, radius, elevation, null, null, null);
    }
    public static DrawCommand border(int layer, UiRect bounds, int color, int radius) {
        return command(Kind.BORDER, layer, bounds, color, color, null, radius, 0, null, null, null);
    }
    public static DrawCommand line(int layer, UiRect bounds, int color) {
        return command(Kind.LINE, layer, bounds, color, color, null, 0, 0, null, null, null);
    }
    public static DrawCommand chartLine(int layer, UiRect bounds, int color, String points) {
        return command(Kind.CHART_LINE, layer, bounds, color, color, points == null ? "" : points, 0, 0, null, null, null);
    }
    public static DrawCommand chartVolume(int layer, UiRect bounds, int color, String bars) {
        return command(Kind.CHART_VOLUME, layer, bounds, color, color, bars == null ? "" : bars, 0, 0, null, null, null);
    }
    public static DrawCommand text(int layer, UiRect bounds, int color, String text) {
        return text(layer, bounds, color, text, TextStyle.BODY);
    }
    public static DrawCommand text(int layer, UiRect bounds, int color, String text, TextStyle style) {
        return command(Kind.TEXT, layer, bounds, color, color, text == null ? "" : text, 0, 0,
            style == null ? TextStyle.BODY : style, null, null);
    }
    public static DrawCommand icon(int layer, UiRect bounds, int color, String icon) {
        return command(Kind.ICON, layer, bounds, color, color, icon == null ? "" : icon, 0, 0, null, null, null);
    }
    public static DrawCommand image(int layer, UiRect bounds, int color, String resourceId) {
        return command(Kind.IMAGE, layer, bounds, color, color, null, 0, 0, null, requireId(resourceId, "resourceId"), null);
    }
    public static DrawCommand externalRegion(int layer, UiRect bounds, String externalId) {
        return command(Kind.EXTERNAL_REGION, layer, bounds, 0, 0, null, 0, 0, null, null,
            requireId(externalId, "externalId"));
    }
    public static DrawCommand clipPush(int layer, UiRect bounds) {
        return command(Kind.CLIP_PUSH, layer, bounds, 0, 0, null, 0, 0, null, null, null);
    }
    public static DrawCommand clipPop(int layer) { return command(Kind.CLIP_POP, layer, null, 0, 0, null, 0, 0, null, null, null); }
    public static DrawCommand transformPush(int layer, UiRect translation) {
        return command(Kind.TRANSFORM_PUSH, layer, translation, 0, 0, null, 0, 0, null, null, null);
    }
    public static DrawCommand transformPop(int layer) { return command(Kind.TRANSFORM_POP, layer, null, 0, 0, null, 0, 0, null, null, null); }

    private static String requireId(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    public Kind getKind() { return kind; }
    public int getLayer() { return layer; }
    public UiRect getBounds() { return bounds; }
    public int getColor() { return color; }
    public int getSecondaryColor() { return secondaryColor; }
    public String getText() { return text; }
    public int getRadius() { return radius; }
    public int getElevation() { return elevation; }
    public TextStyle getTextStyle() { return textStyle; }
    public String getResourceId() { return resourceId; }
    public String getExternalId() { return externalId; }

    public String snapshot() {
        String style = textStyle == null ? "-" : textStyle.getToken() + ":" + textStyle.getSize() + ":"
            + textStyle.isBold() + ":" + textStyle.getLineHeight() + ":" + textStyle.getAlign();
        return kind + "|" + layer + "|" + (bounds == null ? "-" : bounds.toString()) + "|"
            + String.format("%08X", color) + "|" + String.format("%08X", secondaryColor) + "|"
            + radius + "|" + elevation + "|" + style + "|" + safe(resourceId) + "|" + safe(externalId)
            + "|" + safe(text);
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", "\\n");
    }
}
