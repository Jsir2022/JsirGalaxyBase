package com.jsirgalaxybase.modules.core.banking.domain;

import java.time.Instant;

public class BankAccount {

    private final long accountId;
    private final String accountNo;
    private final BankAccountType accountType;
    private final String ownerType;
    private final String ownerRef;
    private final String currencyCode;
    private final long availableBalance;
    private final long frozenBalance;
    private final BankAccountStatus status;
    private final long version;
    private final String displayName;
    private final String metadataJson;
    private final Instant createdAt;
    private final Instant updatedAt;

    public BankAccount(long accountId, String accountNo, BankAccountType accountType, String ownerType,
        String ownerRef, String currencyCode, long availableBalance, long frozenBalance, BankAccountStatus status,
        long version, String displayName, String metadataJson, Instant createdAt, Instant updatedAt) {
        this.accountId = accountId;
        this.accountNo = accountNo;
        this.accountType = accountType;
        this.ownerType = ownerType;
        this.ownerRef = ownerRef;
        this.currencyCode = currencyCode;
        this.availableBalance = availableBalance;
        this.frozenBalance = frozenBalance;
        this.status = status;
        this.version = version;
        this.displayName = displayName;
        this.metadataJson = metadataJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public BankAccountType getAccountType() {
        return accountType;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public String getOwnerRef() {
        return ownerRef;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public long getAvailableBalance() {
        return availableBalance;
    }

    public long getFrozenBalance() {
        return frozenBalance;
    }

    public BankAccountStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}