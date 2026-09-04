package com.jsirgalaxybase.modules.itempolicy.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyAuditRecord;
import com.jsirgalaxybase.modules.itempolicy.port.ItemPolicyAuditQuery;

public final class JdbcItemPolicyAuditQuery implements ItemPolicyAuditQuery {
    private final JdbcConnectionManager connectionManager;
    public JdbcItemPolicyAuditQuery(JdbcConnectionManager connectionManager) { this.connectionManager = connectionManager; }
    @Override public List<ItemPolicyAuditRecord> listRecentForActor(final String server, final String actor, final int limit) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<ItemPolicyAuditRecord>>() {
            @Override public List<ItemPolicyAuditRecord> doInConnection(Connection connection) throws SQLException {
                List<ItemPolicyAuditRecord> result = new ArrayList<ItemPolicyAuditRecord>();
                try (PreparedStatement statement = connection.prepareStatement("SELECT policy_scope, operation, item_registry_name, item_meta, rule_id, created_at FROM item_policy_audit WHERE source_server_id = ? AND actor_player_ref = ? ORDER BY created_at DESC LIMIT ?")) {
                    statement.setString(1, server); statement.setString(2, actor); statement.setInt(3, Math.max(1, Math.min(20, limit)));
                    try (ResultSet rows = statement.executeQuery()) { while (rows.next()) result.add(new ItemPolicyAuditRecord(rows.getString(1), rows.getString(2), rows.getString(3), rows.getInt(4), rows.getString(5), rows.getTimestamp(6).toInstant())); }
                }
                return result;
            }
        });
    }
}
