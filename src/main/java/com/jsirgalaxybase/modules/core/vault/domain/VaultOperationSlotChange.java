package com.jsirgalaxybase.modules.core.vault.domain;

import net.minecraft.item.ItemStack;

/** Immutable before/after evidence for a server-authoritative Vault operation. */
public final class VaultOperationSlotChange {

    private final int slotIndex;
    private final ItemStack before;
    private final ItemStack after;
    private final long beforeVersion;
    private final long afterVersion;

    public VaultOperationSlotChange(int slotIndex, ItemStack before, ItemStack after,
        long beforeVersion, long afterVersion) {
        this.slotIndex = slotIndex;
        this.before = copy(before);
        this.after = copy(after);
        this.beforeVersion = beforeVersion;
        this.afterVersion = afterVersion;
    }

    public int getSlotIndex() { return slotIndex; }
    public ItemStack getBefore() { return copy(before); }
    public ItemStack getAfter() { return copy(after); }
    public long getBeforeVersion() { return beforeVersion; }
    public long getAfterVersion() { return afterVersion; }

    private static ItemStack copy(ItemStack stack) { return stack == null ? null : stack.copy(); }
}
