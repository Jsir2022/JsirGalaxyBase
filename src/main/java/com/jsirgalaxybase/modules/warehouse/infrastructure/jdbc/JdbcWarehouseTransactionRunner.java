package com.jsirgalaxybase.modules.warehouse.infrastructure.jdbc;

import java.util.function.Supplier;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcBankingTransactionRunner;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.warehouse.port.WarehouseTransactionRunner;

public final class JdbcWarehouseTransactionRunner implements WarehouseTransactionRunner {
    private final JdbcBankingTransactionRunner delegate;
    public JdbcWarehouseTransactionRunner(JdbcConnectionManager connectionManager) {
        delegate = new JdbcBankingTransactionRunner(connectionManager);
    }
    @Override
    public <T> T inTransaction(Supplier<T> callback) { return delegate.inTransaction(callback); }
}
