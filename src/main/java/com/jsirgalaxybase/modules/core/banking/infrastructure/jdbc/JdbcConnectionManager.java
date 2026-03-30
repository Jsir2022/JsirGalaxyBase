package com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.jsirgalaxybase.modules.core.banking.application.BankingException;

public class JdbcConnectionManager {

    private final DataSource dataSource;
    private final ThreadLocal<Connection> transactionalConnection = new ThreadLocal<Connection>();

    public JdbcConnectionManager(DataSource dataSource) {
        if (dataSource == null) {
            throw new BankingException("dataSource must not be null");
        }
        this.dataSource = dataSource;
    }

    public boolean hasTransactionalConnection() {
        return transactionalConnection.get() != null;
    }

    public Connection getTransactionalConnection() {
        return transactionalConnection.get();
    }

    public void bindTransactionalConnection(Connection connection) {
        transactionalConnection.set(connection);
    }

    public void clearTransactionalConnection() {
        transactionalConnection.remove();
    }

    public Connection openConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public <T> T withConnection(JdbcConnectionCallback<T> callback) {
        Connection existing = transactionalConnection.get();
        if (existing != null) {
            return execute(callback, existing);
        }

        try (Connection connection = openConnection()) {
            return execute(callback, connection);
        } catch (SQLException e) {
            throw new BankingException("database access failed: " + e.getMessage());
        }
    }

    private <T> T execute(JdbcConnectionCallback<T> callback, Connection connection) {
        try {
            return callback.doInConnection(connection);
        } catch (SQLException e) {
            throw new BankingException("database access failed: " + e.getMessage());
        }
    }
}