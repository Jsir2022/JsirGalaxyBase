package com.jsirgalaxybase.modules.warehouse.domain;

public enum WarehouseDriveResult {
    SUCCESS,
    NOT_REGISTERED,
    NOT_OWNER,
    FAKE_PLAYER_DENIED,
    BAY_OCCUPIED,
    INVALID_CELL,
    NO_CELL,
    CELL_NOT_EMPTY,
    ALREADY_REGISTERED,
    REQUEST_CONFLICT,
    RUNTIME_UNAVAILABLE
}
