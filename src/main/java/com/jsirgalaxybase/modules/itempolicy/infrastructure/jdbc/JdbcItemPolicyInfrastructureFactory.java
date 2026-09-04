package com.jsirgalaxybase.modules.itempolicy.infrastructure.jdbc;

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
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;

/** Explicit fail-fast schema gate for the enabled server policy runtime. */
public final class JdbcItemPolicyInfrastructureFactory {

    private JdbcItemPolicyInfrastructureFactory() {}

    public static JdbcItemPolicyAuditSink createShared(JdbcConnectionManager connectionManager) {
        validateSchema(connectionManager);
        return new JdbcItemPolicyAuditSink(connectionManager);
    }

    public static void validateSchema(final JdbcConnectionManager connectionManager) {
        if (connectionManager == null) throw new BankingException("shared JDBC connection manager is required for item policy");
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {
            @Override
            public Void doInConnection(Connection connection) throws SQLException {
                Set<String> columns = new HashSet<String>();
                String schema;
                try (Statement statement = connection.createStatement(); ResultSet row = statement.executeQuery("SELECT current_schema()")) {
                    if (!row.next()) throw new SQLException("current_schema() returned no row");
                    schema = row.getString(1);
                }
                DatabaseMetaData metadata = connection.getMetaData();
                try (ResultSet rows = metadata.getColumns(null, schema, "item_policy_audit", null)) {
                    while (rows.next()) columns.add(rows.getString("COLUMN_NAME"));
                }
                String[] required = { "audit_id", "source_server_id", "actor_player_ref", "policy_scope", "operation",
                    "item_registry_name", "item_meta", "decision", "rule_id", "created_at" };
                if (!columns.containsAll(Arrays.asList(required))) {
                    throw schemaDrift("missing item_policy_audit or required columns; run scripts/db-migrate.sh before startup");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = current_schema() AND indexname = 'item_policy_audit_actor_created_idx')")) {
                    try (ResultSet row = statement.executeQuery()) {
                        if (!row.next() || !row.getBoolean(1)) throw schemaDrift("missing index item_policy_audit_actor_created_idx");
                    }
                }
                return null;
            }
        });
    }

    private static BankingException schemaDrift(String detail) { return new BankingException("Item policy PostgreSQL schema drift: " + detail); }
}
