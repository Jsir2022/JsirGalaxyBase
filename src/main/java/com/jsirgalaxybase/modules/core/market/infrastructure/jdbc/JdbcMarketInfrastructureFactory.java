package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.DriverManagerDataSource;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.infrastructure.MarketInfrastructure;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public final class JdbcMarketInfrastructureFactory {

    private static final String[] REQUIRED_MARKET_TABLES = {
        "market_order",
        "market_custody_inventory",
        "market_operation_log",
        "market_trade_record"
    };

    private JdbcMarketInfrastructureFactory() {}

    public static MarketInfrastructure create(String jdbcUrl, String jdbcUsername, String jdbcPassword) {
        DataSource dataSource = new DriverManagerDataSource(jdbcUrl, jdbcUsername, jdbcPassword);
        return createShared(new JdbcConnectionManager(dataSource));
    }

    public static MarketInfrastructure createShared(JdbcConnectionManager connectionManager) {
        validateConnection(connectionManager);
        MarketOrderBookRepository orderRepository = new JdbcMarketOrderBookRepository(connectionManager);
        MarketCustodyInventoryRepository custodyRepository = new JdbcMarketCustodyInventoryRepository(connectionManager);
        MarketOperationLogRepository operationLogRepository = new JdbcMarketOperationLogRepository(connectionManager);
        MarketTradeRecordRepository tradeRecordRepository = new JdbcMarketTradeRecordRepository(connectionManager);
        MarketTransactionRunner transactionRunner = new JdbcMarketTransactionRunner(connectionManager);
        return new MarketInfrastructure(orderRepository, custodyRepository, operationLogRepository, tradeRecordRepository,
            transactionRunner);
    }

    private static void validateConnection(JdbcConnectionManager connectionManager) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {

            @Override
            public Void doInConnection(Connection connection) throws SQLException {
                validateSelectOne(connection);
                validateRequiredTables(connection);
                return null;
            }
        });
    }

    private static void validateSelectOne(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT 1");
        try {
            ResultSet resultSet = statement.executeQuery();
            try {
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new MarketOperationException("PostgreSQL handshake validation failed");
                }
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    private static void validateRequiredTables(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c WHERE c.relkind IN ('r', 'p') AND c.relname = ? AND pg_catalog.pg_table_is_visible(c.oid))");
        try {
            for (String tableName : REQUIRED_MARKET_TABLES) {
                statement.setString(1, tableName);
                ResultSet resultSet = statement.executeQuery();
                try {
                    if (!resultSet.next() || !resultSet.getBoolean(1)) {
                        throw new MarketOperationException("Required market table is missing: " + tableName);
                    }
                } finally {
                    resultSet.close();
                }
            }
        } finally {
            statement.close();
        }
    }
}