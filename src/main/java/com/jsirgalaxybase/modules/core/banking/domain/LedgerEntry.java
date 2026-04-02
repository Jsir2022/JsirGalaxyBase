package com.jsirgalaxybase.modules.core.banking.domain;

import java.time.Instant;

public class LedgerEntry {

    private final long entryId;
    private final long transactionId;
    private final long accountId;
    private final LedgerEntrySide entrySide;
    private final long amount;
    private final long balanceBefore;
    private final long balanceAfter;
    private final long frozenBalanceBefore;
    private final long frozenBalanceAfter;
    private final String currencyCode;
    private final short sequenceInTransaction;
    private final Instant createdAt;

    public LedgerEntry(long entryId, long transactionId, long accountId, LedgerEntrySide entrySide, long amount,
        long balanceBefore, long balanceAfter, String currencyCode, short sequenceInTransaction,
        Instant createdAt) {
        this(entryId, transactionId, accountId, entrySide, amount, balanceBefore, balanceAfter, 0L, 0L,
            currencyCode, sequenceInTransaction, createdAt);
    }

    public LedgerEntry(long entryId, long transactionId, long accountId, LedgerEntrySide entrySide, long amount,
        long balanceBefore, long balanceAfter, long frozenBalanceBefore, long frozenBalanceAfter, String currencyCode,
        short sequenceInTransaction, Instant createdAt) {
        this.entryId = entryId;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.entrySide = entrySide;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.frozenBalanceBefore = frozenBalanceBefore;
        this.frozenBalanceAfter = frozenBalanceAfter;
        this.currencyCode = currencyCode;
        this.sequenceInTransaction = sequenceInTransaction;
        this.createdAt = createdAt;
    }

    public long getEntryId() {
        return entryId;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public long getAccountId() {
        return accountId;
    }

    public LedgerEntrySide getEntrySide() {
        return entrySide;
    }

    public long getAmount() {
        return amount;
    }

    public long getBalanceBefore() {
        return balanceBefore;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public long getFrozenBalanceBefore() {
        return frozenBalanceBefore;
    }

    public long getFrozenBalanceAfter() {
        return frozenBalanceAfter;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public short getSequenceInTransaction() {
        return sequenceInTransaction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}