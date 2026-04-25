package com.jsirgalaxybase.client.gui.theme;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.client.gui.framework.ResourceThemeTexture;
import com.jsirgalaxybase.client.gui.framework.ThemeTexture;

public final class TerminalThemeRegistry {

    private static final GuiTheme DEFAULT_THEME = createDefaultTheme();

    private TerminalThemeRegistry() {}

    public static GuiTheme getDefaultTheme() {
        return DEFAULT_THEME;
    }

    private static GuiTheme createDefaultTheme() {
        Map<ThemeColorKey, Integer> colors = new EnumMap<ThemeColorKey, Integer>(ThemeColorKey.class);
        colors.put(ThemeColorKey.SCREEN_OVERLAY, 0xAA0F141A);
        colors.put(ThemeColorKey.PANEL_FILL, 0xF22B3440);
        colors.put(ThemeColorKey.PANEL_ACCENT, 0xF238495A);
        colors.put(ThemeColorKey.POPUP_FILL, 0xF2313C49);
        colors.put(ThemeColorKey.PANEL_BORDER, 0xFF8191A1);
        colors.put(ThemeColorKey.TEXT_PRIMARY, 0xFFE8EEF4);
        colors.put(ThemeColorKey.TEXT_SECONDARY, 0xFFC0CCD8);
        colors.put(ThemeColorKey.BUTTON_TEXT, 0xFFF7FBFF);
        colors.put(ThemeColorKey.BUTTON_TEXT_DISABLED, 0xFF9AA5B1);
        colors.put(ThemeColorKey.BUTTON_FILL, 0xF2465F73);
        colors.put(ThemeColorKey.BUTTON_FILL_HOVER, 0xF25E7B92);
        colors.put(ThemeColorKey.BUTTON_FILL_PRESSED, 0xF2293D4C);
        colors.put(ThemeColorKey.BUTTON_FILL_DISABLED, 0xF228323C);

        ThemeTexture sharedPanel = new ResourceThemeTexture(new ResourceLocation(
            GalaxyBase.MODID,
            "textures/gui/framework/panel_white.png"));
        ThemeTexture sharedButton = new ResourceThemeTexture(new ResourceLocation(
            GalaxyBase.MODID,
            "textures/gui/framework/button_white.png"));
        Map<ThemeTextureKey, ThemeTexture> textures = new EnumMap<ThemeTextureKey, ThemeTexture>(ThemeTextureKey.class);
        textures.put(ThemeTextureKey.PANEL_BACKGROUND, sharedPanel);
        textures.put(ThemeTextureKey.BUTTON_BACKGROUND, sharedButton);
        textures.put(ThemeTextureKey.POPUP_BACKGROUND, sharedPanel);
        return new TerminalGuiTheme("terminal_default", colors, textures);
    }
}