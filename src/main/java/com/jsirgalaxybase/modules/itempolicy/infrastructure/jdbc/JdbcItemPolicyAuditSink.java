package com.jsirgalaxybase.modules.itempolicy.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyScope;
import com.jsirgalaxybase.modules.itempolicy.port.ItemPolicyAuditSink;

/** PostgreSQL audit sink. One denied admission produces one immutable audit row. */
public final class JdbcItemPolicyAuditSink implements ItemPolicyAuditSink {

    private final JdbcConnectionManager connectionManager;

    public JdbcItemPolicyAuditSink(JdbcConnectionManager connectionManager) {
        if (connectionManager == null) throw new IllegalArgumentException("shared JDBC connection manager is required");
        this.connectionManager = connectionManager;
    }

    @Override
    public void recordDenied(final String sourceServerId, final String actorRef, final ItemPolicyScope scope,
        final String operation, final String registryName, final int meta, final String ruleId) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {
            @Override
            public Void doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO item_policy_audit (source_server_id, actor_player_ref, policy_scope, operation, item_registry_name, item_meta, decision, rule_id, created_at) VALUES (?, ?, ?, ?, ?, ?, 'DENIED', ?, ?)")) {
                    statement.setString(1, sourceServerId);
                    statement.setString(2, actorRef == null ? "unknown" : actorRef);
                    statement.setString(3, scope.name());
                    statement.setString(4, operation == null ? "unknown" : operation);
                    statement.setString(5, registryName == null ? "unknown" : registryName);
                    statement.setInt(6, meta);
                    statement.setString(7, ruleId);
                    statement.setTimestamp(8, Timestamp.from(Instant.now()));
                    statement.executeUpdate();
                }
                return null;
            }
        });
    }
}
