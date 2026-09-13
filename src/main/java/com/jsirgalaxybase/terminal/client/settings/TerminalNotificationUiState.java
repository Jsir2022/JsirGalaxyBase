package com.jsirgalaxybase.terminal.client.settings;

public final class TerminalNotificationUiState {
    public enum Severity { ALL,ERROR,WARNING,SUCCESS,INFO }
    private final Severity severity;private final int page;private final boolean helpOpen;
    public TerminalNotificationUiState(Severity severity,int page,boolean helpOpen){this.severity=severity==null?Severity.ALL:severity;this.page=Math.max(0,page);this.helpOpen=helpOpen;}
    public static TerminalNotificationUiState initial(){return new TerminalNotificationUiState(Severity.ALL,0,false);}
    public Severity getSeverity(){return severity;}public int getPage(){return page;}public boolean isHelpOpen(){return helpOpen;}
    public TerminalNotificationUiState withSeverity(Severity value){return new TerminalNotificationUiState(value,0,helpOpen);}
    public TerminalNotificationUiState withPage(int value){return new TerminalNotificationUiState(severity,value,helpOpen);}
    public TerminalNotificationUiState withHelp(boolean value){return new TerminalNotificationUiState(severity,page,value);}
}
