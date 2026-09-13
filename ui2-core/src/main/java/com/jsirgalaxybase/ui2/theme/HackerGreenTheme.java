package com.jsirgalaxybase.ui2.theme;

import java.util.LinkedHashMap;
import java.util.Map;

/** High-transparency black terminal with restrained phosphor-green accents. */
public final class HackerGreenTheme {
    private HackerGreenTheme() {}

    public static UiTheme create() {
        UiTheme base=GlassTerminalTheme.create();
        Map<String,Integer> colors=new LinkedHashMap<String,Integer>(base.getColors());
        colors.put("background",0x42000000); colors.put("window",0xD0060907);
        colors.put("surface",0x35101A14); colors.put("surfaceRaised",0x4D16231B);
        colors.put("selectedSurface",0x66304B38); colors.put("slot",0x76010402);
        colors.put("slotBorder",0x7064FF8A); colors.put("border",0x426EFF91);
        colors.put("text",0xFFE8FFF0); colors.put("textMuted",0xFF91B89D);
        colors.put("accent",0xFF59F27C); colors.put("success",0xFF59F27C);
        colors.put("warning",0xFFF0C65A); colors.put("danger",0xFFFF6B77);
        return new UiTheme(colors,base.getSpacing(),base.getRadii(),base.getTypography(),base.getControls(),base.getMotion(),base.getElevation());
    }
}
