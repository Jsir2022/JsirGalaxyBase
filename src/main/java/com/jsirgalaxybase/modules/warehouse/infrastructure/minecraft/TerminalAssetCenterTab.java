package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

/** Local presentation state for the unified personal asset container. */
public enum TerminalAssetCenterTab {
    STORAGE(0),
    ACTIVITY(3);

    private final int code;

    TerminalAssetCenterTab(int code) { this.code = code; }

    public int getCode() { return code; }

    public static TerminalAssetCenterTab fromCode(int code) {
        return code == 3 ? ACTIVITY : STORAGE;
    }

    public boolean exposesInventory() { return this == STORAGE; }
}
