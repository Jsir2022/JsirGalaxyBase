package com.jsirgalaxybase.modules.cluster.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.port.ServerDirectory;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.AbstractJdbcRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;

public class JdbcServerDirectoryRepository extends AbstractJdbcRepository implements ServerDirectory {

    public JdbcServerDirectoryRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public Optional<ServerDescriptor> findById(final String serverId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<ServerDescriptor>>() {

            @Override
            public Optional<ServerDescriptor> doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT server_id, display_name, gateway_endpoint, local_server, enabled, created_at, updated_at FROM cluster_server_directory WHERE server_id = ?")) {
                    statement.setString(1, serverId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? Optional.of(mapServer(resultSet)) : Optional.<ServerDescriptor>empty();
                    }
                }
            }
        });
    }

    @Override
    public List<ServerDescriptor> listAll() {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<ServerDescriptor>>() {

            @Override
            public List<ServerDescriptor> doInConnection(Connection connection) throws SQLException {
                List<ServerDescriptor> servers = new ArrayList<ServerDescriptor>();
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT server_id, display_name, gateway_endpoint, local_server, enabled, created_at, updated_at FROM cluster_server_directory ORDER BY server_id ASC");
                    ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        servers.add(mapServer(resultSet));
                    }
                }
                return servers;
            }
        });
    }

    @Override
    public ServerDescriptor upsertLocalServer(final String serverId, final String displayName) {
        return connectionManager.withConnection(new JdbcConnectionCallback<ServerDescriptor>() {

            @Override
            public ServerDescriptor doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO cluster_server_directory (server_id, display_name, gateway_endpoint, local_server, enabled, created_at, updated_at) VALUES (?, ?, NULL, TRUE, TRUE, now(), now()) "
                        + "ON CONFLICT (server_id) DO UPDATE SET display_name = EXCLUDED.display_name, local_server = TRUE, enabled = TRUE, updated_at = now() "
                        + "RETURNING server_id, display_name, gateway_endpoint, local_server, enabled, created_at, updated_at")) {
                    statement.setString(1, serverId);
                    statement.setString(2, displayName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        return mapServer(resultSet);
                    }
                }
            }
        });
    }

    private ServerDescriptor mapServer(ResultSet resultSet) throws SQLException {
        Instant createdAt = readInstant(resultSet, "created_at");
        Instant updatedAt = readInstant(resultSet, "updated_at");
        return new ServerDescriptor(resultSet.getString("server_id"), resultSet.getString("display_name"),
            resultSet.getString("gateway_endpoint"), resultSet.getBoolean("local_server"),
            resultSet.getBoolean("enabled"), createdAt, updatedAt);
    }
}