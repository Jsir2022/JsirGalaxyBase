package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

import java.util.function.Supplier;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcBankingTransactionRunner;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public class JdbcMarketTransactionRunner implements MarketTransactionRunner {

    private final JdbcBankingTransactionRunner delegate;

    public JdbcMarketTransactionRunner(JdbcConnectionManager connectionManager) {
        if (connectionManager == null) {
            throw new MarketOperationException("connectionManager must not be null");
        }
        this.delegate = new JdbcBankingTransactionRunner(connectionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> callback) {
        return delegate.inTransaction(callback);
    }

    @Override
    public void inTransaction(Runnable callback) {
        delegate.inTransaction(callback);
    }
}