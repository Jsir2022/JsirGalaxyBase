package com.jsirgalaxybase.terminal.client.settings;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/** Dedicated client configuration; appearance choices never enter the server configuration contract. */
final class TerminalPreferencesRepository {
    private static final String CATEGORY="terminal_ui2";
    private final File file;
    TerminalPreferencesRepository(File minecraftDirectory){
        if(minecraftDirectory==null)throw new IllegalArgumentException("minecraft directory is required");
        file=new File(new File(minecraftDirectory,"config"),"jsirgalaxybase-terminal-ui.cfg");
    }
    TerminalPreferences load(){
        Configuration config=new Configuration(file);config.load();
        TerminalPreferences defaults=TerminalPreferences.defaults();
        String theme=config.getString("theme",CATEGORY,defaults.getThemeId(),"Selected Galaxy terminal theme id.");
        TerminalWindowSize window=parse(TerminalWindowSize.class,config.getString("windowSize",CATEGORY,defaults.getWindowSize().name(),"COMPACT, STANDARD or LARGE."),defaults.getWindowSize());
        boolean reduced=config.getBoolean("reducedMotion",CATEGORY,defaults.isReducedMotion(),"Disable non-essential terminal motion.");
        int tooltip=config.getInt("tooltipDelayMillis",CATEGORY,defaults.getTooltipDelayMillis(),0,2000,"Delay before terminal tooltips appear.");
        config.getCategory(CATEGORY).remove("density");
        config.getCategory(CATEGORY).remove("fontSource");
        if(config.hasChanged())config.save();
        return new TerminalPreferences(theme,window,reduced,tooltip);
    }
    void save(TerminalPreferences value){
        TerminalPreferences actual=value==null?TerminalPreferences.defaults():value;
        Configuration config=new Configuration(file);config.load();
        config.get(CATEGORY,"theme",TerminalPreferences.DEFAULT_THEME).set(actual.getThemeId());
        config.getCategory(CATEGORY).remove("density");
        config.getCategory(CATEGORY).remove("fontSource");
        config.get(CATEGORY,"windowSize",TerminalWindowSize.STANDARD.name()).set(actual.getWindowSize().name());
        config.get(CATEGORY,"reducedMotion",false).set(actual.isReducedMotion());
        config.get(CATEGORY,"tooltipDelayMillis",350).set(actual.getTooltipDelayMillis());
        config.save();
    }
    private static <E extends Enum<E>> E parse(Class<E> type,String value,E fallback){
        try{return Enum.valueOf(type,value==null?"":value.trim().toUpperCase(java.util.Locale.ROOT));}
        catch(IllegalArgumentException ignored){return fallback;}
    }
}
