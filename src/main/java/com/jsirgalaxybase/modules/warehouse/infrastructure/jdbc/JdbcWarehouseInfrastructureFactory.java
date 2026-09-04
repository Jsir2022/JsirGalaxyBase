package com.jsirgalaxybase.modules.warehouse.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.warehouse.infrastructure.WarehouseInfrastructure;

/** Shared-connection factory; warehouse owns no second JDBC configuration. */
public final class JdbcWarehouseInfrastructureFactory {
    private JdbcWarehouseInfrastructureFactory() {}

    public static WarehouseInfrastructure createShared(JdbcConnectionManager connectionManager) {
        validateSchema(connectionManager);
        return new WarehouseInfrastructure(new JdbcWarehouseDriveRepository(connectionManager),
            new JdbcWarehouseTransactionRunner(connectionManager));
    }

    public static void validateSchema(JdbcConnectionManager connectionManager) {
        if (connectionManager == null) throw outdated("shared JDBC connection is unavailable");
        connectionManager.withConnection(connection -> {
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT 1")) {
                if (!rows.next()) throw new SQLException("warehouse PostgreSQL handshake failed");
            }
            validateTable(connection, "warehouse_drive", new String[] { "drive_id", "source_server_id", "dimension_id",
                "block_x", "block_y", "block_z", "owner_player_ref", "drive_status", "version", "created_at", "updated_at" });
            validateTable(connection, "warehouse_drive_operation", new String[] { "operation_id", "request_id", "semantics_key",
                "operation_type", "result_code", "operator_player_ref", "source_server_id", "dimension_id", "block_x", "block_y",
                "block_z", "before_drive_id", "before_owner_player_ref", "before_status", "before_version", "after_drive_id",
                "after_owner_player_ref", "after_status", "after_version", "detail", "created_at" });
            validateTable(connection, "warehouse_terminal_bay", new String[] { "source_server_id", "owner_player_ref", "cell_nbt", "version", "created_at", "updated_at" });
            validateTable(connection, "warehouse_terminal_bay_operation", new String[] { "operation_id", "request_id", "semantics_key",
                "operation_type", "result_code", "operator_player_ref", "source_server_id", "before_cell_nbt", "before_version",
                "after_cell_nbt", "after_version", "detail", "created_at" });
            validateIndex(connection, "warehouse_drive_active_location_unique");
            return null;
        });
    }

    private static void validateTable(Connection connection, String table, String[] expected) throws SQLException {
        Set<String> columns = new HashSet<String>();
        String schema;
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT current_schema()")) {
            if (!rows.next()) throw new SQLException("no current schema"); schema = rows.getString(1);
        }
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet rows = metadata.getColumns(null, schema, table, null)) { while (rows.next()) columns.add(rows.getString("COLUMN_NAME")); }
        if (columns.isEmpty()) throw outdated("missing table " + table);
        for (String name : expected) if (!columns.contains(name)) throw outdated("missing column " + table + "." + name
            + " expected=" + Arrays.toString(expected));
    }
    private static void validateIndex(Connection connection, String index) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname=current_schema() AND indexname=?)")) {
            statement.setString(1, index); try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next() || !rows.getBoolean(1)) throw outdated("missing index " + index);
            }
        }
    }
    private static BankingException outdated(String detail) {
        return new BankingException("Warehouse PostgreSQL schema is unavailable or outdated: " + detail
            + ". Run scripts/db-migrate.sh before enabling warehouse.");
    }
}
