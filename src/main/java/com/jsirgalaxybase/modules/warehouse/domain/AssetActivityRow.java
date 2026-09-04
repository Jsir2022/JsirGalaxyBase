package com.jsirgalaxybase.modules.warehouse.domain;

import java.time.Instant;

import net.minecraft.item.ItemStack;

public final class AssetActivityRow {
    private final String stableKey;
    private final AssetActivityType type;
    private final ItemStack itemStack;
    private final long quantity, amount, fee;
    private final String reference, status;
    private final Instant createdAt;

    public AssetActivityRow(String stableKey, AssetActivityType type, ItemStack itemStack, long quantity,
        long amount, long fee, String reference, String status, Instant createdAt) {
        this.stableKey = safe(stableKey); this.type = type == null ? AssetActivityType.RECOVERY_REQUIRED : type;
        this.itemStack = itemStack == null ? null : itemStack.copy(); this.quantity = Math.max(0L, quantity);
        this.amount = Math.max(0L, amount); this.fee = Math.max(0L, fee);
        this.reference = safe(reference); this.status = safe(status);
        this.createdAt = createdAt == null ? Instant.EPOCH : createdAt;
    }

    public String getStableKey() { return stableKey; }
    public AssetActivityType getType() { return type; }
    public ItemStack getItemStack() { return itemStack == null ? null : itemStack.copy(); }
    public long getQuantity() { return quantity; }
    public long getAmount() { return amount; }
    public long getFee() { return fee; }
    public String getReference() { return reference; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
