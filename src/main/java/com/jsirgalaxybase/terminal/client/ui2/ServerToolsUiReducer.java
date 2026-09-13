package com.jsirgalaxybase.terminal.client.ui2;

import com.jsirgalaxybase.ui2.state.UiReducer;

public final class ServerToolsUiReducer implements UiReducer<ServerToolsUiState,ServerToolsUiAction>{
    @Override public ServerToolsUiState reduce(ServerToolsUiState state,ServerToolsUiAction action){if(action==null)return state;switch(action.getType()){
        case TAB:return state.withTab(action.getTab());case FOCUS:return state.withField(action.getField());
        case INPUT:return state.withValue(action.getField(),value(state,action.getField())+action.getValue());
        case BACKSPACE:String current=value(state,action.getField());return state.withValue(action.getField(),current.isEmpty()?current:current.substring(0,current.length()-1));
        case SELECT_WARP:return state.withWarp(action.getValue());case SELECT_HOME:return state.withHome(action.getValue());
        case OPEN_HELP:return state.withHelp(true);case CLOSE_HELP:return state.withHelp(false);
        case OPEN_CONFIRM:return state.withConfirmation(action.getConfirmation());case CLOSE_CONFIRM:return state.withConfirmation(null);
        default:return state;}}
    private static String value(ServerToolsUiState state,ServerToolsUiState.Field field){if(field==ServerToolsUiState.Field.HOME)return state.getHomeDraft();if(field==ServerToolsUiState.Field.TPA_PLAYER)return state.getPlayerDraft();if(field==ServerToolsUiState.Field.TPA_SERVER)return state.getServerDraft();return "";}
}
