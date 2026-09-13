package com.jsirgalaxybase.terminal.client.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;
import com.jsirgalaxybase.ui2.theme.HackerGreenTheme;
import com.jsirgalaxybase.ui2.theme.HighContrastTheme;
import com.jsirgalaxybase.ui2.theme.UiTheme;

/** Safe theme registry. External resource definitions may replace entries, never executable behavior. */
public final class TerminalThemeRegistry {
    public static final class Entry {
        private final String id,label;private final UiTheme theme;private final boolean builtIn;
        private Entry(String id,String label,UiTheme theme,boolean builtIn){this.id=id;this.label=label;this.theme=theme;this.builtIn=builtIn;}
        public String getId(){return id;} public String getLabel(){return label;} public UiTheme getTheme(){return theme;} public boolean isBuiltIn(){return builtIn;}
    }
    private final Map<String,Entry> entries=new LinkedHashMap<String,Entry>();
    public TerminalThemeRegistry(){resetBuiltIns();}
    public synchronized void resetBuiltIns(){entries.clear();register("glass_mono","流动玻璃",GlassTerminalTheme.create(),true);register("hacker_green","黑绿终端",HackerGreenTheme.create(),true);register("high_contrast","高对比",HighContrastTheme.create(),true);}
    public synchronized void registerResource(String id,String label,UiTheme theme){register(id,label,theme,false);}
    private void register(String id,String label,UiTheme theme,boolean builtIn){if(id==null||!id.matches("[a-z0-9_.-]+")||label==null||label.trim().isEmpty()||theme==null)throw new IllegalArgumentException("invalid theme entry");entries.put(id,new Entry(id,label.trim(),theme,builtIn));}
    public synchronized Entry resolveEntry(String id){Entry found=entries.get(id);return found==null?entries.get(TerminalPreferences.DEFAULT_THEME):found;}
    public synchronized UiTheme resolve(TerminalPreferences preferences){TerminalPreferences actual=preferences==null?TerminalPreferences.defaults():preferences;return resolveEntry(actual.getThemeId()).getTheme();}
    public synchronized List<Entry> entries(){return Collections.unmodifiableList(new ArrayList<Entry>(entries.values()));}
}
