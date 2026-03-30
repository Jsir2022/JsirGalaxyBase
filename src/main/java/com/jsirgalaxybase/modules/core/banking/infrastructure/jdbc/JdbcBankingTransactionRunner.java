package com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.repository.BankingTransactionRunner;

public class JdbcBankingTransactionRunner implements BankingTransactionRunner {

    private final JdbcConnectionManager connectionManager;

    public JdbcBankingTransactionRunner(JdbcConnectionManager connectionManager) {
        if (connectionManager == null) {
            throw new BankingException("connectionManager must not be null");
        }
        this.connectionManager = connectionManager;
    }

    @Override
    public <T> T inTransaction(Supplier<T> callback) {
        if (callback == null) {
            throw new BankingException("callback must not be null");
        }
        if (connectionManager.hasTransactionalConnection()) {
            return callback.get();
        }

        try (Connection connection = connectionManager.openConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            connectionManager.bindTransactionalConnection(connection);
            try {
                T result = callback.get();
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                rollbackQuietly(connection);
                throw e;
            } catch (Error e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                connectionManager.clearTransactionalConnection();
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new BankingException("transaction failed: " + e.getMessage());
        }
    }

    @Override
    public void inTransaction(Runnable callback) {
        inTransaction(new Supplier<Void>() {

            @Override
            public Void get() {
                callback.run();
                return null;
            }
        });
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // rollback best effort only
        }
    }
}