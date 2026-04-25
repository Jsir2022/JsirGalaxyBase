package com.jsirgalaxybase.modules.servertools.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.servertools.infrastructure.AllowAllPlayerPermissionPolicy;
import com.jsirgalaxybase.modules.servertools.infrastructure.ServerToolsInfrastructure;
import com.jsirgalaxybase.modules.servertools.port.PlayerPermissionPolicy;
import com.jsirgalaxybase.modules.servertools.port.PlayerTeleportRepository;

public final class JdbcServerToolsInfrastructureFactory {

    private static final String MIGRATION_COMMAND = "scripts/db-migrate.sh";

    private static final String[] REQUIRED_TABLES = {
        "player_home",
        "player_back_record",
        "server_warp",
        "player_tpa_request",
        "player_rtp_record"
    };

    private static final Map<String, String[]> REQUIRED_COLUMNS = createRequiredColumns();

    private JdbcServerToolsInfrastructureFactory() {}

    public static ServerToolsInfrastructure createShared(JdbcConnectionManager connectionManager) {
        validateConnection(connectionManager);
        PlayerTeleportRepository repository = new JdbcPlayerTeleportRepository(connectionManager);
        PlayerPermissionPolicy permissionPolicy = new AllowAllPlayerPermissionPolicy();
        return new ServerToolsInfrastructure(repository, permissionPolicy);
    }

    private static Map<String, String[]> createRequiredColumns() {
        Map<String, String[]> columns = new LinkedHashMap<String, String[]>();
        columns.put("player_home", new String[] {
            "player_uuid",
            "home_name",
            "server_id",
            "dimension_id",
            "target_x",
            "target_y",
            "target_z",
            "target_yaw",
            "target_pitch",
            "created_at",
            "updated_at"
        });
        columns.put("player_back_record", new String[] {
            "player_uuid",
            "teleport_kind",
            "server_id",
            "dimension_id",
            "target_x",
            "target_y",
            "target_z",
            "target_yaw",
            "target_pitch",
            "recorded_at"
        });
        columns.put("server_warp", new String[] {
            "warp_name",
            "display_name",
            "description",
            "server_id",
            "dimension_id",
            "target_x",
            "target_y",
            "target_z",
            "target_yaw",
            "target_pitch",
            "enabled",
            "created_at",
            "updated_at"
        });
        columns.put("player_tpa_request", new String[] {
            "request_id",
            "requester_player_uuid",
            "requester_player_name",
            "requester_server_id",
            "requester_origin_server_id",
            "requester_origin_dimension_id",
            "requester_origin_x",
            "requester_origin_y",
            "requester_origin_z",
            "requester_origin_yaw",
            "requester_origin_pitch",
            "target_player_name",
            "target_server_id",
            "status",
            "created_at",
            "expires_at",
            "updated_at"
        });
        columns.put("player_rtp_record", new String[] {
            "record_id",
            "player_uuid",
            "source_server_id",
            "dimension_id",
            "target_x",
            "target_y",
            "target_z",
            "target_yaw",
            "target_pitch",
            "created_at"
        });
        return columns;
    }

    private static void validateConnection(final JdbcConnectionManager connectionManager) {
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

    private static void validateSelectOne(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            if (!resultSet.next() || resultSet.getInt(1) != 1) {
                throw new BankingException("Server tools PostgreSQL handshake validation failed");
            }
        }
    }

    private static void validateRequiredTables(Connection connection) throws SQLException {
        List<String> missingTables = new ArrayList<String>();
        try (java.sql.PreparedStatement statement = connection.prepareStatement(
            "SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c WHERE c.relkind IN ('r', 'p') AND c.relname = ? AND pg_catalog.pg_table_is_visible(c.oid))")) {
            for (String tableName : REQUIRED_TABLES) {
                statement.setString(1, tableName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next() || !resultSet.getBoolean(1)) {
                        missingTables.add(tableName);
                    }
                }
            }
        }
        if (!missingTables.isEmpty()) {
            throw schemaDrift("missing tables: " + joinValues(missingTables));
        }
    }

    private static void validateRequiredColumns(Connection connection) throws SQLException {
        List<String> missingColumns = new ArrayList<String>();
        for (Map.Entry<String, String[]> entry : REQUIRED_COLUMNS.entrySet()) {
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
        try (ResultSet columns = metadata.getColumns(null, currentSchema, tableName, null)) {
            Set<String> names = new LinkedHashSet<String>();
            while (columns.next()) {
                names.add(columns.getString("COLUMN_NAME"));
            }
            return names;
        }
    }

    private static BankingException schemaDrift(String details) {
        return new BankingException("Server tools PostgreSQL schema is outdated or drifted: " + details + ". Run "
            + MIGRATION_COMMAND
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