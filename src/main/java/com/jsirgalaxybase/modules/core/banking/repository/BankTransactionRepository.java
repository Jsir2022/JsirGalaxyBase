package com.jsirgalaxybase.modules.core.banking.repository;

import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.domain.BankTransaction;

public interface BankTransactionRepository {

    Optional<BankTransaction> findById(long transactionId);

    Optional<BankTransaction> findByRequestId(String requestId);

    BankTransaction save(BankTransaction transaction);
}