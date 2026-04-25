package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

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

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.DriverManagerDataSource;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.infrastructure.MarketInfrastructure;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketAuditLogRepository;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketItemSnapshotRepository;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketListingRepository;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public final class JdbcMarketInfrastructureFactory {

    private static final String MIGRATION_COMMAND = "scripts/db-migrate.sh";

    private static final String[] REQUIRED_MARKET_TABLES = {
        "market_order",
        "market_custody_inventory",
        "market_operation_log",
        "market_trade_record",
        "custom_market_listing",
        "custom_market_item_snapshot",
        "custom_market_trade_record",
        "custom_market_audit_log"
    };

    private static final Map<String, String[]> REQUIRED_MARKET_COLUMNS = createRequiredColumns();

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
        CustomMarketListingRepository customMarketListingRepository = new JdbcCustomMarketListingRepository(connectionManager);
        CustomMarketItemSnapshotRepository customMarketItemSnapshotRepository = new JdbcCustomMarketItemSnapshotRepository(connectionManager);
        CustomMarketTradeRecordRepository customMarketTradeRecordRepository = new JdbcCustomMarketTradeRecordRepository(connectionManager);
        CustomMarketAuditLogRepository customMarketAuditLogRepository = new JdbcCustomMarketAuditLogRepository(connectionManager);
        MarketTransactionRunner transactionRunner = new JdbcMarketTransactionRunner(connectionManager);
        return new MarketInfrastructure(orderRepository, custodyRepository, operationLogRepository,
            tradeRecordRepository, customMarketListingRepository, customMarketItemSnapshotRepository,
            customMarketTradeRecordRepository, customMarketAuditLogRepository, transactionRunner);
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
        columns.put("market_order", new String[] {
            "order_id",
            "order_side",
            "order_status",
            "owner_player_ref",
            "product_key",
            "registry_name",
            "meta",
            "stackable",
            "unit_price",
            "original_quantity",
            "open_quantity",
            "filled_quantity",
            "reserved_funds",
            "custody_id",
            "source_server_id",
            "created_at",
            "updated_at"
        });
        columns.put("market_custody_inventory", new String[] {
            "custody_id",
            "owner_player_ref",
            "product_key",
            "registry_name",
            "meta",
            "stackable",
            "quantity",
            "custody_status",
            "related_order_id",
            "related_operation_id",
            "source_server_id",
            "created_at",
            "updated_at"
        });
        columns.put("market_operation_log", new String[] {
            "operation_id",
            "request_id",
            "operation_type",
            "operation_status",
            "source_server_id",
            "player_ref",
            "request_semantics_key",
            "recovery_metadata_key",
            "related_order_id",
            "related_custody_id",
            "related_trade_id",
            "message",
            "created_at",
            "updated_at"
        });
        columns.put("market_trade_record", new String[] {
            "trade_id",
            "buyer_player_ref",
            "seller_player_ref",
            "product_key",
            "registry_name",
            "meta",
            "stackable",
            "unit_price",
            "quantity",
            "fee_amount",
            "buy_order_id",
            "sell_order_id",
            "operation_id",
            "created_at"
        });
        columns.put("custom_market_listing", new String[] {
            "listing_id",
            "seller_player_ref",
            "buyer_player_ref",
            "asking_price",
            "currency_code",
            "listing_status",
            "delivery_status",
            "source_server_id",
            "created_at",
            "updated_at"
        });
        columns.put("custom_market_item_snapshot", new String[] {
            "snapshot_id",
            "listing_id",
            "item_id",
            "meta",
            "stack_size",
            "stackable",
            "display_name",
            "nbt_snapshot",
            "created_at"
        });
        columns.put("custom_market_trade_record", new String[] {
            "trade_id",
            "listing_id",
            "seller_player_ref",
            "buyer_player_ref",
            "settled_amount",
            "currency_code",
            "delivery_status",
            "created_at"
        });
        columns.put("custom_market_audit_log", new String[] {
            "audit_id",
            "request_id",
            "audit_type",
            "player_ref",
            "request_semantics_key",
            "listing_id",
            "trade_id",
            "source_server_id",
            "message",
            "created_at",
            "updated_at"
        });
        return columns;
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
        List<String> missingTables = new ArrayList<String>();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c WHERE c.relkind IN ('r', 'p') AND c.relname = ? AND pg_catalog.pg_table_is_visible(c.oid))");
        try {
            for (String tableName : REQUIRED_MARKET_TABLES) {
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
        for (Map.Entry<String, String[]> entry : REQUIRED_MARKET_COLUMNS.entrySet()) {
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

    private static MarketOperationException schemaDrift(String details) {
        return new MarketOperationException(
            "Market PostgreSQL schema is outdated or drifted: " + details
                + ". Run " + MIGRATION_COMMAND
                + " against the target database before starting the dedicated server. Startup will not auto-mutate the schema.");
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