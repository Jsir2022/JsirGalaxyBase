package com.jsirgalaxybase.terminal.client.settings;

public final class TerminalNotificationAction {
    public enum Type { SEVERITY,PAGE,OPEN_HELP,CLOSE_HELP }
    private final Type type;private final String value;private final int page;
    private TerminalNotificationAction(Type type,String value,int page){this.type=type;this.value=value;this.page=page;}
    public static TerminalNotificationAction severity(TerminalNotificationUiState.Severity value){return new TerminalNotificationAction(Type.SEVERITY,value.name(),0);}
    public static TerminalNotificationAction page(int value){return new TerminalNotificationAction(Type.PAGE,"",value);}
    public static TerminalNotificationAction of(Type type){return new TerminalNotificationAction(type,"",0);}
    public Type getType(){return type;}public String getValue(){return value;}public int getPage(){return page;}
}
