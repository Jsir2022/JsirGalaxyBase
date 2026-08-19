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
        colors.put(ThemeColorKey.SCREEN_OVERLAY, 0xAA071017);
        colors.put(ThemeColorKey.PANEL_FILL, 0xF0182733);
        colors.put(ThemeColorKey.PANEL_ACCENT, 0xF0223443);
        colors.put(ThemeColorKey.POPUP_FILL, 0xFF1D2B37);
        colors.put(ThemeColorKey.PANEL_BORDER, 0xFF5F7890);
        colors.put(ThemeColorKey.TEXT_PRIMARY, 0xFFEAF3F8);
        colors.put(ThemeColorKey.TEXT_SECONDARY, 0xFFB8C8D5);
        colors.put(ThemeColorKey.BUTTON_TEXT, 0xFFF7FBFF);
        colors.put(ThemeColorKey.BUTTON_TEXT_DISABLED, 0xFF7D8B97);
        colors.put(ThemeColorKey.BUTTON_FILL, 0xF12A5A83);
        colors.put(ThemeColorKey.BUTTON_FILL_HOVER, 0xF13970A4);
        colors.put(ThemeColorKey.BUTTON_FILL_PRESSED, 0xF1162836);
        colors.put(ThemeColorKey.BUTTON_FILL_DISABLED, 0xF11B2933);

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
