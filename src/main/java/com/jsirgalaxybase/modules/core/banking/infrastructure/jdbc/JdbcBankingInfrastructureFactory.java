package com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final String MIGRATION_COMMAND = "scripts/db-migrate.sh";

    private static final String[] REQUIRED_BANKING_TABLES = {
        "bank_account",
        "bank_transaction",
        "ledger_entry",
        "coin_exchange_record"
    };

    private static final Map<String, String[]> REQUIRED_BANKING_COLUMNS = createRequiredColumns();

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
                validateRequiredColumns(connection);
                return null;
            }
        });
    }

    private static Map<String, String[]> createRequiredColumns() {
        Map<String, String[]> columns = new LinkedHashMap<String, String[]>();
        columns.put("bank_account", new String[] {
            "account_id",
            "account_no",
            "account_type",
            "owner_type",
            "owner_ref",
            "currency_code",
            "available_balance",
            "frozen_balance",
            "status",
            "version",
            "display_name",
            "metadata_json",
            "created_at",
            "updated_at"
        });
        columns.put("bank_transaction", new String[] {
            "transaction_id",
            "request_id",
            "transaction_type",
            "business_type",
            "business_ref",
            "source_server_id",
            "operator_type",
            "operator_ref",
            "player_ref",
            "comment",
            "extra_json",
            "created_at"
        });
        columns.put("ledger_entry", new String[] {
            "entry_id",
            "transaction_id",
            "account_id",
            "entry_side",
            "amount",
            "balance_before",
            "balance_after",
            "frozen_balance_before",
            "frozen_balance_after",
            "currency_code",
            "sequence_in_tx",
            "created_at"
        });
        columns.put("coin_exchange_record", new String[] {
            "exchange_id",
            "transaction_id",
            "player_ref",
            "coin_family",
            "coin_tier",
            "coin_face_value",
            "coin_quantity",
            "effective_exchange_value",
            "contribution_value",
            "exchange_rule_version",
            "source_server_id",
            "extra_json",
            "created_at"
        });
        return columns;
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
        List<String> missingTables = new ArrayList<String>();
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
                        missingTables.add(tableName);
                    }
                } finally {
                    resultSet.close();
                }
            }
        } finally {
            statement.close();
        }

        if (!missingTables.isEmpty()) {
            throw schemaDrift("missing tables: " + joinValues(missingTables));
        }
    }

    private static void validateRequiredColumns(Connection connection) throws SQLException {
        List<String> missingColumns = new ArrayList<String>();
        for (Map.Entry<String, String[]> entry : REQUIRED_BANKING_COLUMNS.entrySet()) {
            Set<String> actualColumns = loadVisibleTableColumns(connection, entry.getKey());
            for (String columnName : entry.getValue()) {
                if (!actualColumns.contains(columnName)) {
                    missingColumns.add(entry.getKey() + "." + columnName);
                }
            }
        }

        if (!missingColumns.isEmpty()) {
            throw schemaDrift("missing columns: " + joinValues(missingColumns));
        }
    }

    private static Set<String> loadVisibleTableColumns(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String currentSchema = null;
        try (Statement statement = connection.createStatement();
            ResultSet schemas = statement.executeQuery("SELECT current_schema()")) {
            if (schemas.next()) {
                currentSchema = schemas.getString(1);
            }
        }

        ResultSet columns = metadata.getColumns(null, currentSchema, tableName, null);
        try {
            Set<String> names = new LinkedHashSet<String>();
            while (columns.next()) {
                names.add(columns.getString("COLUMN_NAME"));
            }
            return names;
        } finally {
            columns.close();
        }
    }

    private static BankingException schemaDrift(String details) {
        return new BankingException(
            "Banking PostgreSQL schema is outdated or drifted: " + details +
                ". Run " + MIGRATION_COMMAND +
                " against the target database before starting the dedicated server. Startup will not auto-mutate the schema.");
    }

    private static String joinValues(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }
}