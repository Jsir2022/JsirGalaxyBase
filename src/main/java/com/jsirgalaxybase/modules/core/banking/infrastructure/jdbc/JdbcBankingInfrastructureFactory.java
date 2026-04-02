package com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.jsirgalaxybase.config.ModConfiguration;
import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.repository.BankAccountRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankTransactionRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankingTransactionRunner;
import com.jsirgalaxybase.modules.core.banking.repository.CoinExchangeRecordRepository;
import com.jsirgalaxybase.modules.core.banking.repository.LedgerEntryRepository;

public final class JdbcBankingInfrastructureFactory {

    private static final String[] REQUIRED_BANKING_TABLES = {
        "bank_account",
        "bank_transaction",
        "ledger_entry",
        "coin_exchange_record"
    };

    private JdbcBankingInfrastructureFactory() {}

    public static BankingInfrastructure create(ModConfiguration configuration) {
        return create(configuration.getBankingJdbcUrl(), configuration.getBankingJdbcUsername(),
            configuration.getBankingJdbcPassword());
    }

    static BankingInfrastructure create(String jdbcUrl, String jdbcUsername, String jdbcPassword) {
        loadDriver();
        DataSource dataSource = new DriverManagerDataSource(jdbcUrl, jdbcUsername, jdbcPassword);
        JdbcConnectionManager connectionManager = new JdbcConnectionManager(dataSource);
        validateConnection(connectionManager);
        BankAccountRepository accountRepository = new JdbcBankAccountRepository(connectionManager);
        BankTransactionRepository transactionRepository = new JdbcBankTransactionRepository(connectionManager);
        LedgerEntryRepository ledgerEntryRepository = new JdbcLedgerEntryRepository(connectionManager);
        CoinExchangeRecordRepository coinExchangeRecordRepository = new JdbcCoinExchangeRecordRepository(
            connectionManager);
        BankingTransactionRunner transactionRunner = new JdbcBankingTransactionRunner(connectionManager);
        BankingApplicationService bankingApplicationService = new BankingApplicationService(accountRepository,
            transactionRepository, ledgerEntryRepository, coinExchangeRecordRepository, transactionRunner);
        return new BankingInfrastructure(bankingApplicationService, accountRepository, transactionRepository,
            ledgerEntryRepository, coinExchangeRecordRepository, transactionRunner, connectionManager);
    }

    private static void loadDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new BankingException("PostgreSQL JDBC driver not found on classpath");
        }
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
                    throw new BankingException("PostgreSQL handshake validation failed");
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
            "SELECT EXISTS (" +
                "SELECT 1 FROM pg_catalog.pg_class c " +
                "WHERE c.relkind IN ('r', 'p') AND c.relname = ? AND pg_catalog.pg_table_is_visible(c.oid))");
        try {
            for (String tableName : REQUIRED_BANKING_TABLES) {
                statement.setString(1, tableName);
                ResultSet resultSet = statement.executeQuery();
                try {
                    if (!resultSet.next() || !resultSet.getBoolean(1)) {
                        throw new BankingException("Required banking table is missing: " + tableName);
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