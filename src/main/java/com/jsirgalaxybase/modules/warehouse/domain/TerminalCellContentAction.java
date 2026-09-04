package com.jsirgalaxybase.modules.warehouse.domain;

public enum TerminalCellContentAction {
    INJECT_CURSOR,
    EXTRACT_STACK,
    EXTRACT_ONE;

    public static TerminalCellContentAction fromCode(int code) {
        TerminalCellContentAction[] values = values();
        return code < 0 || code >= values.length ? INJECT_CURSOR : values[code];
    }
}
