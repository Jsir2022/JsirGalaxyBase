package com.jsirgalaxybase.terminal.client.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.jsirgalaxybase.ui2.theme.UiTheme;

public class TerminalThemeRegistryTest {
    @Test public void exposesThreeSafeBuiltInChoicesAndFallsBack(){
        TerminalThemeRegistry registry=new TerminalThemeRegistry();
        assertEquals(3,registry.entries().size());
        assertEquals("glass_mono",registry.resolveEntry("missing").getId());
    }
    @Test public void themeTypographyAndControlsAreFixedByTheme(){
        TerminalThemeRegistry registry=new TerminalThemeRegistry();
        UiTheme standard=registry.resolve(TerminalPreferences.defaults());
        assertSame(standard,registry.resolve(TerminalPreferences.defaults()));
        assertEquals(standard.control("normal"),registry.resolve(TerminalPreferences.defaults().withWindowSize(TerminalWindowSize.LARGE)).control("normal"));
    }
    @Test public void preferencesClampTooltipDelay(){
        assertEquals(2000,TerminalPreferences.defaults().withTooltipDelay(5000).getTooltipDelayMillis());
        assertEquals(0,TerminalPreferences.defaults().withTooltipDelay(-1).getTooltipDelayMillis());
    }
}
