package com.jsirgalaxybase.terminal.client.ui2;

public final class ServerToolsUiAction {
    public enum Type { TAB,FOCUS,INPUT,BACKSPACE,SELECT_WARP,SELECT_HOME,OPEN_HELP,CLOSE_HELP,OPEN_CONFIRM,CLOSE_CONFIRM }
    private final Type type;private final ServerToolsUiState.Tab tab;private final ServerToolsUiState.Field field;
    private final String value;private final ServerToolsUiState.Confirmation confirmation;
    private ServerToolsUiAction(Type type,ServerToolsUiState.Tab tab,ServerToolsUiState.Field field,String value,ServerToolsUiState.Confirmation confirmation){this.type=type;this.tab=tab;this.field=field;this.value=value==null?"":value;this.confirmation=confirmation;}
    public static ServerToolsUiAction of(Type type){return new ServerToolsUiAction(type,null,null,"",null);}
    public static ServerToolsUiAction tab(ServerToolsUiState.Tab value){return new ServerToolsUiAction(Type.TAB,value,null,"",null);}
    public static ServerToolsUiAction focus(ServerToolsUiState.Field value){return new ServerToolsUiAction(Type.FOCUS,null,value,"",null);}
    public static ServerToolsUiAction input(ServerToolsUiState.Field field,char value){return new ServerToolsUiAction(Type.INPUT,null,field,String.valueOf(value),null);}
    public static ServerToolsUiAction backspace(ServerToolsUiState.Field field){return new ServerToolsUiAction(Type.BACKSPACE,null,field,"",null);}
    public static ServerToolsUiAction select(Type type,String value){return new ServerToolsUiAction(type,null,null,value,null);}
    public static ServerToolsUiAction confirm(ServerToolsUiState.Confirmation value){return new ServerToolsUiAction(Type.OPEN_CONFIRM,null,null,"",value);}
    public Type getType(){return type;}public ServerToolsUiState.Tab getTab(){return tab;}public ServerToolsUiState.Field getField(){return field;}public String getValue(){return value;}public ServerToolsUiState.Confirmation getConfirmation(){return confirmation;}
}
