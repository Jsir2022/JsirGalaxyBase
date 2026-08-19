package com.jsirgalaxybase.client.gui.theme;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TerminalThemeRegistryTest {

    @Test
    public void popupBodyIsFullyOpaqueWhileScreenDimmerRemainsSeparate() {
        GuiTheme theme = TerminalThemeRegistry.getDefaultTheme();

        assertEquals(255, theme.color(ThemeColorKey.POPUP_FILL) >>> 24);
        assertEquals(170, theme.color(ThemeColorKey.SCREEN_OVERLAY) >>> 24);
    }
}
