package com.jsirgalaxybase.terminal.client.settings;

import com.jsirgalaxybase.ui2.state.UiReducer;

public final class TerminalNotificationReducer implements UiReducer<TerminalNotificationUiState,TerminalNotificationAction> {
    @Override public TerminalNotificationUiState reduce(TerminalNotificationUiState state,TerminalNotificationAction action){
        if(action==null)return state;
        try{switch(action.getType()){
            case SEVERITY:return state.withSeverity(TerminalNotificationUiState.Severity.valueOf(action.getValue()));
            case PAGE:return state.withPage(action.getPage());
            case OPEN_HELP:return state.withHelp(true);
            case CLOSE_HELP:return state.withHelp(false);
            default:return state;
        }}catch(IllegalArgumentException ignored){return state;}
    }
}
