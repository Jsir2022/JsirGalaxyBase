package com.jsirgalaxybase.terminal.client.settings;

public final class TerminalSettingsState {
    private final TerminalPreferences preferences;private final boolean helpOpen;
    public TerminalSettingsState(TerminalPreferences preferences,boolean helpOpen){this.preferences=preferences==null?TerminalPreferences.defaults():preferences;this.helpOpen=helpOpen;}
    public TerminalPreferences getPreferences(){return preferences;} public boolean isHelpOpen(){return helpOpen;}
    public TerminalSettingsState withPreferences(TerminalPreferences value){return new TerminalSettingsState(value,helpOpen);}
    public TerminalSettingsState withHelp(boolean value){return new TerminalSettingsState(preferences,value);}
}
