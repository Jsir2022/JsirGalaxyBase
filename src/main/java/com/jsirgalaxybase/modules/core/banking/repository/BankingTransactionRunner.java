package com.jsirgalaxybase.modules.core.banking.repository;

import java.util.function.Supplier;

public interface BankingTransactionRunner {

    <T> T inTransaction(Supplier<T> callback);

    void inTransaction(Runnable callback);
}