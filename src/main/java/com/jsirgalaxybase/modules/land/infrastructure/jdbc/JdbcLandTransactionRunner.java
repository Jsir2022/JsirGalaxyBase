package com.jsirgalaxybase.modules.land.infrastructure.jdbc;

import java.util.function.Supplier;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcBankingTransactionRunner;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.land.port.LandTransactionRunner;

public final class JdbcLandTransactionRunner implements LandTransactionRunner {

    private final JdbcBankingTransactionRunner delegate;

    public JdbcLandTransactionRunner(JdbcConnectionManager connectionManager) {
        delegate = new JdbcBankingTransactionRunner(connectionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> callback) {
        return delegate.inTransaction(callback);
    }
}
