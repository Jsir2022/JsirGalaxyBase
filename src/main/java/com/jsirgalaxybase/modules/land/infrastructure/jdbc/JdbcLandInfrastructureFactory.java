package com.jsirgalaxybase.modules.land.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.land.infrastructure.LandInfrastructure;

public final class JdbcLandInfrastructureFactory {

    private static final String MIGRATION_COMMAND = "scripts/db-migrate.sh";

    private JdbcLandInfrastructureFactory() {}

    public static LandInfrastructure createShared(JdbcConnectionManager connectionManager) {
        if (connectionManager == null) throw new BankingException("Shared JDBC connection manager is required");
        validateSchema(connectionManager);
        return new LandInfrastructure(new JdbcPersonalLandRepository(connectionManager),
            new JdbcLandTransactionRunner(connectionManager));
    }

    public static void validateSchema(final JdbcConnectionManager connectionManager) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {

            @Override
            public Void doInConnection(Connection connection) throws SQLException {
                try (Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                    if (!resultSet.next() || resultSet.getInt(1) != 1) {
                        throw new SQLException("land PostgreSQL handshake failed");
                    }
                }
                validateTable(connection, "land_title", new String[] {
                    "title_id",
                    "source_server_id",
                    "dimension_id",
                    "chunk_x",
                    "chunk_z",
                    "owner_player_ref",
                    "title_status",
                    "version",
                    "created_at",
                    "updated_at"
                });
                validateTable(connection, "land_operation_log", new String[] {
                    "operation_id",
                    "request_id",
                    "semantics_key",
                    "action_type",
                    "result_code",
                    "operator_player_ref",
                    "source_server_id",
                    "dimension_id",
                    "chunk_x",
                    "chunk_z",
                    "expected_version",
                    "before_title_id",
                    "before_owner_player_ref",
                    "before_title_status",
                    "before_version",
                    "after_title_id",
                    "after_owner_player_ref",
                    "after_title_status",
                    "after_version",
                    "created_at"
                });
                validateIndex(connection, "land_title_active_chunk_unique");
                return null;
            }
        });
    }

    private static void validateTable(Connection connection, String tableName, String[] requiredColumns)
        throws SQLException {
        Set<String> actual = new HashSet<String>();
        String schema = currentSchema(connection);
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(null, schema, tableName, null)) {
            while (columns.next()) actual.add(columns.getString("COLUMN_NAME"));
        }
        if (actual.isEmpty()) throw schemaDrift("missing table " + tableName);
        List<String> missing = new ArrayList<String>();
        for (String column : requiredColumns) {
            if (!actual.contains(column)) missing.add(tableName + "." + column);
        }
        if (!missing.isEmpty()) throw schemaDrift("missing columns " + Arrays.toString(missing.toArray()));
    }

    private static void validateIndex(Connection connection, String indexName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = current_schema() AND indexname = ?)")) {
            statement.setString(1, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || !resultSet.getBoolean(1)) {
                    throw schemaDrift("missing index " + indexName);
                }
            }
        }
    }

    private static String currentSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT current_schema()")) {
            if (!resultSet.next()) throw new SQLException("current_schema() returned no row");
            return resultSet.getString(1);
        }
    }

    private static BankingException schemaDrift(String detail) {
        return new BankingException("Personal land PostgreSQL schema is outdated or drifted: " + detail + ". Run "
            + MIGRATION_COMMAND + " before enabling land protection.");
    }
}
