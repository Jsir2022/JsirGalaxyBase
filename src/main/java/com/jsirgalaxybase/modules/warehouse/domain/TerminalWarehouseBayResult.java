package com.jsirgalaxybase.modules.warehouse.domain;

public enum TerminalWarehouseBayResult {
    SUCCESS,
    INVALID_CELL,
    NO_CELL,
    CELL_FULL,
    ITEM_NOT_FOUND,
    VERSION_CONFLICT,
    REQUEST_CONFLICT,
    RUNTIME_UNAVAILABLE
}
