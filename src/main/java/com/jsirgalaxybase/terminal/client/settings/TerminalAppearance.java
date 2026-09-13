package com.jsirgalaxybase.terminal.client.settings;

import java.io.File;

import net.minecraft.client.resources.IResourceManager;

import com.jsirgalaxybase.ui2.theme.UiTheme;

/** Client-local source of truth for terminal theme, font, window and motion preferences. */
public enum TerminalAppearance {
    INSTANCE;

    private final TerminalThemeRegistry themes=new TerminalThemeRegistry();
    private TerminalPreferences preferences=TerminalPreferences.defaults();
    private TerminalPreferencesRepository repository;

    public synchronized void initialize(File minecraftDirectory){
        repository=new TerminalPreferencesRepository(minecraftDirectory);
        preferences=repository.load();
        if(!contains(preferences.getThemeId()))preferences=preferences.withTheme(TerminalPreferences.DEFAULT_THEME);
    }
    public synchronized TerminalPreferences preferences(){return preferences;}
    public synchronized TerminalThemeRegistry themes(){return themes;}
    public synchronized UiTheme theme(){return themes.resolve(preferences);}
    public synchronized void apply(TerminalPreferences value){
        TerminalPreferences next=value==null?TerminalPreferences.defaults():value;
        if(!contains(next.getThemeId()))next=next.withTheme(TerminalPreferences.DEFAULT_THEME);
        preferences=next;if(repository!=null)repository.save(next);
    }
    public synchronized void reloadThemes(IResourceManager resources){
        TerminalThemeResourceLoader.reload(resources,themes);
        if(!contains(preferences.getThemeId()))preferences=preferences.withTheme(TerminalPreferences.DEFAULT_THEME);
    }
    private boolean contains(String id){for(TerminalThemeRegistry.Entry entry:themes.entries())if(entry.getId().equals(id))return true;return false;}
}
