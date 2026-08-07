package com.jsirgalaxybase.modules.core.vault.domain;

import java.time.Instant;

public final class VaultAccount {

    private final long accountId;
    private final VaultAccountType accountType;
    private final String accountRef;
    private final int slotCount;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public VaultAccount(long accountId, VaultAccountType accountType, String accountRef, int slotCount, String status,
        Instant createdAt, Instant updatedAt) {
        this.accountId = accountId;
        this.accountType = accountType;
        this.accountRef = accountRef;
        this.slotCount = slotCount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getAccountId() { return accountId; }
    public VaultAccountType getAccountType() { return accountType; }
    public String getAccountRef() { return accountRef; }
    public int getSlotCount() { return slotCount; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
