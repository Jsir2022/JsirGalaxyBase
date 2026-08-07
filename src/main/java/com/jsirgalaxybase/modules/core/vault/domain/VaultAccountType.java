package com.jsirgalaxybase.modules.core.vault.domain;

/** Ownership boundary for a finite Base Vault. */
public enum VaultAccountType {
    PERSONAL(27),
    ENTERPRISE(54),
    PUBLIC(54);

    private final int defaultSlotCount;

    VaultAccountType(int defaultSlotCount) {
        this.defaultSlotCount = defaultSlotCount;
    }

    public int getDefaultSlotCount() {
        return defaultSlotCount;
    }
}
