package com.jsirgalaxybase.client.gui.theme;

import java.util.EnumMap;
import java.util.Map;

import com.jsirgalaxybase.client.gui.framework.ThemeTexture;

public class TerminalGuiTheme implements GuiTheme {

    private final String id;
    private final Map<ThemeColorKey, Integer> colors;
    private final Map<ThemeTextureKey, ThemeTexture> textures;

    public TerminalGuiTheme(String id, Map<ThemeColorKey, Integer> colors, Map<ThemeTextureKey, ThemeTexture> textures) {
        this.id = id;
        this.colors = new EnumMap<ThemeColorKey, Integer>(colors);
        this.textures = new EnumMap<ThemeTextureKey, ThemeTexture>(textures);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int color(ThemeColorKey key) {
        Integer color = colors.get(key);
        return color == null ? 0xFFFFFFFF : color.intValue();
    }

    @Override
    public ThemeTexture texture(ThemeTextureKey key) {
        return textures.get(key);
    }
}