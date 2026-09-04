package com.jsirgalaxybase.modules.servertools.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.AbstractJdbcRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.servertools.application.ServerToolsException;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.port.ServerToolsAdminRepository;

/** JDBC administration path: every state mutation and its audit row share one database transaction. */
public final class JdbcServerToolsAdminRepository extends AbstractJdbcRepository implements ServerToolsAdminRepository {

    public JdbcServerToolsAdminRepository(JdbcConnectionManager connectionManager) { super(connectionManager); }

    @Override public ServerWarp upsertLocalWarp(final TeleportActor actor, final String requestId, final String warpName,
        final String displayName, final String description) {
        return transaction(new Work<ServerWarp>() { @Override public ServerWarp apply(Connection connection) throws SQLException {
            ServerWarp before = findWarp(connection, warpName).orElse(null);
            Instant now = Instant.now(); TeleportTarget target = actor.getCurrentLocation();
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO server_warp (warp_name, display_name, description, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?) "
                    + "ON CONFLICT (warp_name) DO UPDATE SET display_name = EXCLUDED.display_name, description = EXCLUDED.description, server_id = EXCLUDED.server_id, dimension_id = EXCLUDED.dimension_id, target_x = EXCLUDED.target_x, target_y = EXCLUDED.target_y, target_z = EXCLUDED.target_z, target_yaw = EXCLUDED.target_yaw, target_pitch = EXCLUDED.target_pitch, enabled = TRUE, updated_at = EXCLUDED.updated_at RETURNING warp_name, display_name, description, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, enabled, created_at, updated_at")) {
                statement.setString(1, warpName); statement.setString(2, displayName); statement.setString(3, description);
                bindTarget(statement, 4, target); statement.setTimestamp(11, Timestamp.from(now)); statement.setTimestamp(12, Timestamp.from(now));
                try (ResultSet result = statement.executeQuery()) { if (!result.next()) throw new SQLException("warp upsert returned no row"); ServerWarp after = mapWarp(result);
                    audit(connection, actor, requestId, "UPSERT_WARP", "WARP", warpName, warpSnapshot(before), warpSnapshot(after)); return after; }
            }
        }});
    }

    @Override public boolean deleteWarp(final TeleportActor actor, final String requestId, final String warpName) {
        return transaction(new Work<Boolean>() { @Override public Boolean apply(Connection connection) throws SQLException {
            ServerWarp before = findWarp(connection, warpName).orElse(null);
            if (before == null) throw new ServerToolsException("Warp not found: " + warpName);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM server_warp WHERE warp_name = ?")) {
                statement.setString(1, warpName); boolean deleted = statement.executeUpdate() == 1;
                if (!deleted) throw new ServerToolsException("Warp changed before deletion: " + warpName);
                audit(connection, actor, requestId, "DELETE_WARP", "WARP", warpName, warpSnapshot(before), "{}"); return Boolean.TRUE;
            }
        }}).booleanValue();
    }

    @Override public ServerWarp setWarpEnabled(final TeleportActor actor, final String requestId, final String warpName,
        final boolean enabled) {
        return transaction(new Work<ServerWarp>() { @Override public ServerWarp apply(Connection connection) throws SQLException {
            ServerWarp before = findWarp(connection, warpName).orElse(null);
            if (before == null) throw new ServerToolsException("Warp not found: " + warpName);
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE server_warp SET enabled = ?, updated_at = now() WHERE warp_name = ? RETURNING warp_name, display_name, description, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, enabled, created_at, updated_at")) {
                statement.setBoolean(1, enabled); statement.setString(2, warpName);
                try (ResultSet result = statement.executeQuery()) { if (!result.next()) throw new ServerToolsException("Warp changed before update: " + warpName);
                    ServerWarp after = mapWarp(result); audit(connection, actor, requestId, "SET_WARP_ENABLED", "WARP", warpName, warpSnapshot(before), warpSnapshot(after)); return after; }
            }
        }});
    }

    @Override public ServerDescriptor setServerEnabled(final TeleportActor actor, final String requestId, final String serverId,
        final boolean enabled) {
        return transaction(new Work<ServerDescriptor>() { @Override public ServerDescriptor apply(Connection connection) throws SQLException {
            ServerDescriptor before = findServer(connection, serverId).orElse(null);
            if (before == null) throw new ServerToolsException("Unknown server: " + serverId);
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE cluster_server_directory SET enabled = ?, updated_at = now() WHERE server_id = ? RETURNING server_id, display_name, gateway_endpoint, local_server, enabled, created_at, updated_at")) {
                statement.setBoolean(1, enabled); statement.setString(2, serverId);
                try (ResultSet result = statement.executeQuery()) { if (!result.next()) throw new ServerToolsException("Server changed before update: " + serverId);
                    ServerDescriptor after = mapServer(result); audit(connection, actor, requestId, "SET_SERVER_ENABLED", "SERVER", serverId, serverSnapshot(before), serverSnapshot(after)); return after; }
            }
        }});
    }

    @Override public Optional<ServerWarp> findWarp(final String warpName) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<ServerWarp>>() { @Override public Optional<ServerWarp> doInConnection(Connection connection) throws SQLException { return findWarp(connection, warpName); }});
    }

    private <T> T transaction(final Work<T> work) {
        return connectionManager.withConnection(new JdbcConnectionCallback<T>() { @Override public T doInConnection(Connection connection) throws SQLException {
            boolean previous = connection.getAutoCommit(); connection.setAutoCommit(false);
            try { T result = work.apply(connection); connection.commit(); return result; }
            catch (RuntimeException exception) { connection.rollback(); throw exception; }
            catch (SQLException exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(previous); }
        }});
    }
    private Optional<ServerWarp> findWarp(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT warp_name, display_name, description, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, enabled, created_at, updated_at FROM server_warp WHERE warp_name = ?")) {
            statement.setString(1, name); try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(mapWarp(result)) : Optional.<ServerWarp>empty(); }
        }
    }
    private Optional<ServerDescriptor> findServer(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT server_id, display_name, gateway_endpoint, local_server, enabled, created_at, updated_at FROM cluster_server_directory WHERE server_id = ?")) {
            statement.setString(1, id); try (ResultSet result = statement.executeQuery()) { return result.next() ? Optional.of(mapServer(result)) : Optional.<ServerDescriptor>empty(); }
        }
    }
    private void audit(Connection connection, TeleportActor actor, String requestId, String action, String targetType,
        String targetKey, String before, String after) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO servertools_admin_operation (request_id, actor_player_uuid, actor_player_name, source_server_id, action, target_type, target_key, before_snapshot, after_snapshot, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())")) {
            statement.setString(1, requestId); statement.setString(2, actor.getPlayerUuid()); statement.setString(3, actor.getPlayerName()); statement.setString(4, actor.getSourceServerId()); statement.setString(5, action); statement.setString(6, targetType); statement.setString(7, targetKey); statement.setString(8, before); statement.setString(9, after); statement.executeUpdate();
        }
    }
    private static void bindTarget(PreparedStatement statement, int offset, TeleportTarget target) throws SQLException { statement.setString(offset, target.getServerId()); statement.setInt(offset + 1, target.getDimensionId()); statement.setDouble(offset + 2, target.getX()); statement.setDouble(offset + 3, target.getY()); statement.setDouble(offset + 4, target.getZ()); statement.setFloat(offset + 5, target.getYaw()); statement.setFloat(offset + 6, target.getPitch()); }
    private ServerWarp mapWarp(ResultSet result) throws SQLException { return new ServerWarp(result.getString("warp_name"), result.getString("display_name"), result.getString("description"), new TeleportTarget(result.getString("server_id"), result.getInt("dimension_id"), result.getDouble("target_x"), result.getDouble("target_y"), result.getDouble("target_z"), result.getFloat("target_yaw"), result.getFloat("target_pitch")), result.getBoolean("enabled"), result.getTimestamp("created_at").toInstant(), result.getTimestamp("updated_at").toInstant()); }
    private ServerDescriptor mapServer(ResultSet result) throws SQLException { return new ServerDescriptor(result.getString("server_id"), result.getString("display_name"), result.getString("gateway_endpoint"), result.getBoolean("local_server"), result.getBoolean("enabled"), result.getTimestamp("created_at").toInstant(), result.getTimestamp("updated_at").toInstant()); }
    private static String warpSnapshot(ServerWarp value) { return value == null ? "{}" : "{\\\"warp\\\":\\\"" + esc(value.getWarpName()) + "\\\",\\\"server\\\":\\\"" + esc(value.getTarget().getServerId()) + "\\\",\\\"enabled\\\":" + value.isEnabled() + "}"; }
    private static String serverSnapshot(ServerDescriptor value) { return value == null ? "{}" : "{\\\"server\\\":\\\"" + esc(value.getServerId()) + "\\\",\\\"enabled\\\":" + value.isEnabled() + "}"; }
    private static String esc(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private interface Work<T> { T apply(Connection connection) throws SQLException; }
}
