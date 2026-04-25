package com.jsirgalaxybase.client.gui.theme;

import com.jsirgalaxybase.client.gui.framework.ThemeTexture;

public interface GuiTheme {

    String getId();

    int color(ThemeColorKey key);

    ThemeTexture texture(ThemeTextureKey key);
}