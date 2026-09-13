package com.jsirgalaxybase.terminal.client.ui2;

public final class BankUiAction {
    public enum Type { FOCUS, INPUT, BACKSPACE, OPEN_HELP, CLOSE_HELP, OPEN_CONFIRM, CLOSE_CONFIRM }
    private final Type type;
    private final BankUiState.Field field;
    private final String value;

    private BankUiAction(Type type, BankUiState.Field field, String value) {
        this.type = type; this.field = field; this.value = value == null ? "" : value;
    }
    public static BankUiAction of(Type type) { return new BankUiAction(type, BankUiState.Field.NONE, ""); }
    public static BankUiAction focus(BankUiState.Field field) { return new BankUiAction(Type.FOCUS, field, ""); }
    public static BankUiAction input(BankUiState.Field field, char value) {
        return new BankUiAction(Type.INPUT, field, String.valueOf(value));
    }
    public static BankUiAction backspace(BankUiState.Field field) {
        return new BankUiAction(Type.BACKSPACE, field, "");
    }
    public Type getType() { return type; }
    public BankUiState.Field getField() { return field; }
    public String getValue() { return value; }
}
