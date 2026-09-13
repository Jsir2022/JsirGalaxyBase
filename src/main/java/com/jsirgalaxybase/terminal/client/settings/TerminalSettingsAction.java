package com.jsirgalaxybase.terminal.client.settings;

public final class TerminalSettingsAction {
    public enum Type { THEME,WINDOW,TOGGLE_MOTION,RESET,OPEN_HELP,CLOSE_HELP }
    private final Type type;private final String value;
    private TerminalSettingsAction(Type type,String value){this.type=type;this.value=value;}
    public static TerminalSettingsAction of(Type type){return new TerminalSettingsAction(type,"");}
    public static TerminalSettingsAction value(Type type,String value){return new TerminalSettingsAction(type,value==null?"":value);}
    public Type getType(){return type;} public String getValue(){return value;}
    @Override public String toString(){return type+":"+value;}
}
