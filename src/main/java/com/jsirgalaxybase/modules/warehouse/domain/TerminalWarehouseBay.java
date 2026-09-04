package com.jsirgalaxybase.modules.warehouse.domain;

import net.minecraft.item.ItemStack;

/** One personal, server-hosted AE2 Cell slot. It is never an AE grid node. */
public final class TerminalWarehouseBay {
    private final String serverId;
    private final String ownerPlayerRef;
    private final ItemStack cell;
    private final long version;

    public TerminalWarehouseBay(String serverId, String ownerPlayerRef, ItemStack cell, long version) {
        this.serverId = required(serverId, "serverId");
        this.ownerPlayerRef = required(ownerPlayerRef, "ownerPlayerRef");
        this.cell = copy(cell);
        this.version = Math.max(0L, version);
    }

    public String getServerId() { return serverId; }
    public String getOwnerPlayerRef() { return ownerPlayerRef; }
    public ItemStack getCell() { return copy(cell); }
    public long getVersion() { return version; }
    public boolean hasCell() { return cell != null && cell.stackSize > 0; }

    private static ItemStack copy(ItemStack stack) { return stack == null ? null : stack.copy(); }
    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
