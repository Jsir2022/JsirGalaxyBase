package com.jsirgalaxybase.ui2.theme;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DarkIndustrialTheme {
    private DarkIndustrialTheme() {}

    public static UiTheme create() {
        Map<String, Integer> colors = new LinkedHashMap<String, Integer>();
        colors.put("background", 0xE80B111B);
        colors.put("window", 0xF00B111B);
        colors.put("surface", 0xE8121C29);
        colors.put("surfaceRaised", 0xF0192638);
        colors.put("selectedSurface", 0xF0234163);
        colors.put("slot", 0xFF08131F);
        colors.put("slotBorder", 0xFF466B8C);
        colors.put("border", 0xB0364C66);
        colors.put("text", 0xFFE7EFF8);
        colors.put("textMuted", 0xFF91A5BA);
        colors.put("accent", 0xFF3B82F6);
        colors.put("success", 0xFF42D392);
        colors.put("warning", 0xFFF6B73C);
        colors.put("danger", 0xFFEF5B67);

        Map<String, Integer> spacing = new LinkedHashMap<String, Integer>();
        spacing.put("xs", 2); spacing.put("sm", 4); spacing.put("md", 8); spacing.put("lg", 12); spacing.put("xl", 16);
        Map<String, Integer> radii = new LinkedHashMap<String, Integer>();
        radii.put("sm", 2); radii.put("md", 4); radii.put("lg", 7);
        Map<String, Integer> typography = new LinkedHashMap<String, Integer>();
        typography.put("caption", 9); typography.put("body", 11); typography.put("section", 13);
        typography.put("title", 17); typography.put("lineBody", 14); typography.put("lineTitle", 21);
        Map<String, Integer> controls = new LinkedHashMap<String, Integer>();
        controls.put("compact", 20); controls.put("normal", 26); controls.put("touch", 30);
        Map<String, Integer> motion = new LinkedHashMap<String, Integer>();
        motion.put("hover", 100); motion.put("overlay", 160); motion.put("toast", 180);
        Map<String, Integer> elevation = new LinkedHashMap<String, Integer>();
        elevation.put("card", 1); elevation.put("popup", 3); elevation.put("modal", 5);
        return new UiTheme(colors, spacing, radii, typography, controls, motion, elevation);
    }
}
