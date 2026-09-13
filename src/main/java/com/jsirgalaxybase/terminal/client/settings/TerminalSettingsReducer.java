package com.jsirgalaxybase.terminal.client.settings;

import com.jsirgalaxybase.ui2.state.UiReducer;

public final class TerminalSettingsReducer implements UiReducer<TerminalSettingsState,TerminalSettingsAction> {
    @Override public TerminalSettingsState reduce(TerminalSettingsState state,TerminalSettingsAction action){
        if(action==null)return state;TerminalPreferences preferences=state.getPreferences();
        try{
            switch(action.getType()){
                case THEME:return state.withPreferences(preferences.withTheme(action.getValue()));
                case WINDOW:return state.withPreferences(preferences.withWindowSize(TerminalWindowSize.valueOf(action.getValue())));
                case TOGGLE_MOTION:return state.withPreferences(preferences.withReducedMotion(!preferences.isReducedMotion()));
                case RESET:return state.withPreferences(TerminalPreferences.defaults());
                case OPEN_HELP:return state.withHelp(true);
                case CLOSE_HELP:return state.withHelp(false);
                default:return state;
            }
        }catch(IllegalArgumentException ignored){return state;}
    }
}
