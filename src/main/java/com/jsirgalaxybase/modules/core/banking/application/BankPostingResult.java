package com.jsirgalaxybase.modules.core.banking.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransaction;
import com.jsirgalaxybase.modules.core.banking.domain.CoinExchangeRecord;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntry;

public class BankPostingResult {

    private final BankTransaction transaction;
    private final List<BankAccount> affectedAccounts;
    private final List<LedgerEntry> ledgerEntries;
    private final CoinExchangeRecord exchangeRecord;

    public BankPostingResult(BankTransaction transaction, List<BankAccount> affectedAccounts,
        List<LedgerEntry> ledgerEntries, CoinExchangeRecord exchangeRecord) {
        this.transaction = transaction;
        this.affectedAccounts = Collections.unmodifiableList(new ArrayList<BankAccount>(affectedAccounts));
        this.ledgerEntries = Collections.unmodifiableList(new ArrayList<LedgerEntry>(ledgerEntries));
        this.exchangeRecord = exchangeRecord;
    }

    public BankTransaction getTransaction() {
        return transaction;
    }

    public List<BankAccount> getAffectedAccounts() {
        return affectedAccounts;
    }

    public List<LedgerEntry> getLedgerEntries() {
        return ledgerEntries;
    }

    public CoinExchangeRecord getExchangeRecord() {
        return exchangeRecord;
    }
}