package com.jsirgalaxybase.ui2.theme;

import java.util.LinkedHashMap;
import java.util.Map;

/** Monochrome translucent terminal material with restrained hacker accents. */
public final class GlassTerminalTheme {
    private GlassTerminalTheme() {}

    public static UiTheme create() {
        Map<String, Integer> colors = new LinkedHashMap<String, Integer>();
        colors.put("background", 0x52000000);
        colors.put("window", 0xD20A0C0F);
        colors.put("surface", 0x381E242A);
        colors.put("surfaceRaised", 0x52262D33);
        colors.put("selectedSurface", 0x70333D42);
        colors.put("slot", 0x7203070A);
        colors.put("slotBorder", 0x665BD6C5);
        colors.put("border", 0x3DFFFFFF);
        colors.put("text", 0xFFF2F5F4);
        colors.put("textMuted", 0xFFABB6B5);
        colors.put("accent", 0xFF57E3B4);
        colors.put("success", 0xFF57E3B4);
        colors.put("warning", 0xFFF3C969);
        colors.put("danger", 0xFFFF727B);

        Map<String, Integer> spacing = new LinkedHashMap<String, Integer>();
        spacing.put("xs", 2); spacing.put("sm", 4); spacing.put("md", 7); spacing.put("lg", 10); spacing.put("xl", 14);
        Map<String, Integer> radii = new LinkedHashMap<String, Integer>();
        radii.put("sm", 2); radii.put("md", 3); radii.put("lg", 5);
        Map<String, Integer> typography = new LinkedHashMap<String, Integer>();
        typography.put("caption", 9); typography.put("body", 11); typography.put("section", 13);
        typography.put("title", 17); typography.put("lineBody", 14); typography.put("lineTitle", 21);
        Map<String, Integer> controls = new LinkedHashMap<String, Integer>();
        controls.put("compact", 18); controls.put("normal", 22); controls.put("touch", 28);
        Map<String, Integer> motion = new LinkedHashMap<String, Integer>();
        motion.put("hover", 90); motion.put("overlay", 150); motion.put("toast", 170);
        Map<String, Integer> elevation = new LinkedHashMap<String, Integer>();
        elevation.put("card", 1); elevation.put("popup", 3); elevation.put("modal", 5);
        return new UiTheme(colors, spacing, radii, typography, controls, motion, elevation);
    }
}
