package com.jsirgalaxybase.modules.core.banking.infrastructure;

import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.repository.BankAccountRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankTransactionRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankingTransactionRunner;
import com.jsirgalaxybase.modules.core.banking.repository.CoinExchangeRecordRepository;
import com.jsirgalaxybase.modules.core.banking.repository.LedgerEntryRepository;

public class BankingInfrastructure {

    private final BankingApplicationService bankingApplicationService;
    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final CoinExchangeRecordRepository coinExchangeRecordRepository;
    private final BankingTransactionRunner bankingTransactionRunner;

    public BankingInfrastructure(BankingApplicationService bankingApplicationService,
        BankAccountRepository bankAccountRepository, BankTransactionRepository bankTransactionRepository,
        LedgerEntryRepository ledgerEntryRepository, CoinExchangeRecordRepository coinExchangeRecordRepository,
        BankingTransactionRunner bankingTransactionRunner) {
        this.bankingApplicationService = bankingApplicationService;
        this.bankAccountRepository = bankAccountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.coinExchangeRecordRepository = coinExchangeRecordRepository;
        this.bankingTransactionRunner = bankingTransactionRunner;
    }

    public BankingApplicationService getBankingApplicationService() {
        return bankingApplicationService;
    }

    public BankAccountRepository getBankAccountRepository() {
        return bankAccountRepository;
    }

    public BankTransactionRepository getBankTransactionRepository() {
        return bankTransactionRepository;
    }

    public LedgerEntryRepository getLedgerEntryRepository() {
        return ledgerEntryRepository;
    }

    public CoinExchangeRecordRepository getCoinExchangeRecordRepository() {
        return coinExchangeRecordRepository;
    }

    public BankingTransactionRunner getBankingTransactionRunner() {
        return bankingTransactionRunner;
    }
}