package com.jsirgalaxybase.modules.core.banking.repository;

import java.util.List;

import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntry;

public interface LedgerEntryRepository {

    void appendEntries(List<LedgerEntry> entries);

    List<LedgerEntry> findByTransactionId(long transactionId);

    List<LedgerEntry> findRecentByAccountId(long accountId, int limit);
}