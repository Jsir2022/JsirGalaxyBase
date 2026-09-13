package com.jsirgalaxybase.terminal.client.ui2;

import com.jsirgalaxybase.ui2.state.UiReducer;

public final class BankUiReducer implements UiReducer<BankUiState, BankUiAction> {
    @Override public BankUiState reduce(BankUiState state, BankUiAction action) {
        if (action == null) return state;
        switch (action.getType()) {
            case FOCUS: return state.withField(action.getField());
            case INPUT: return state.withValue(action.getField(), value(state, action.getField()) + action.getValue());
            case BACKSPACE:
                String value = value(state, action.getField());
                return state.withValue(action.getField(), value.isEmpty() ? value : value.substring(0, value.length() - 1));
            case OPEN_HELP: return state.withHelp(true);
            case CLOSE_HELP: return state.withHelp(false);
            case OPEN_CONFIRM: return state.withConfirm(true);
            case CLOSE_CONFIRM: return state.withConfirm(false);
            default: return state;
        }
    }

    private static String value(BankUiState state, BankUiState.Field field) {
        if (field == BankUiState.Field.TARGET) return state.getTarget();
        if (field == BankUiState.Field.AMOUNT) return state.getAmount();
        if (field == BankUiState.Field.COMMENT) return state.getComment();
        return "";
    }
}
