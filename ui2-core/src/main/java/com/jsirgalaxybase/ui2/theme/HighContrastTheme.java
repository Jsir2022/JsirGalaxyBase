package com.jsirgalaxybase.ui2.theme;

import java.util.LinkedHashMap;
import java.util.Map;

/** Opaque accessibility theme with explicit borders and cyan focus color. */
public final class HighContrastTheme {
    private HighContrastTheme() {}

    public static UiTheme create() {
        UiTheme base=GlassTerminalTheme.create();
        Map<String,Integer> colors=new LinkedHashMap<String,Integer>(base.getColors());
        colors.put("background",0xB8000000); colors.put("window",0xFF050607);
        colors.put("surface",0xFF0B0E10); colors.put("surfaceRaised",0xFF151A1E);
        colors.put("selectedSurface",0xFF26343A); colors.put("slot",0xFF000000);
        colors.put("slotBorder",0xFFFFFFFF); colors.put("border",0xFFC9D2D6);
        colors.put("text",0xFFFFFFFF); colors.put("textMuted",0xFFD6DEE1);
        colors.put("accent",0xFF43F5D0); colors.put("success",0xFF65FF8A);
        colors.put("warning",0xFFFFD447); colors.put("danger",0xFFFF6673);
        Map<String,Integer> radii=new LinkedHashMap<String,Integer>(base.getRadii());
        radii.put("sm",1); radii.put("md",2); radii.put("lg",3);
        return new UiTheme(colors,base.getSpacing(),radii,base.getTypography(),base.getControls(),base.getMotion(),base.getElevation());
    }
}
