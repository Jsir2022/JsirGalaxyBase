package com.jsirgalaxybase.modules.cluster.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.Optional;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicketStatus;
import com.jsirgalaxybase.modules.cluster.port.TeleportTicketRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.AbstractJdbcRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;

public class JdbcTeleportTicketRepository extends AbstractJdbcRepository implements TeleportTicketRepository {

    public static final String EXPIRED_STATUS_MESSAGE = "Cluster transfer ticket expired before target restore completed";

    public JdbcTeleportTicketRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public TransferTicket save(final TransferTicket ticket) {
        return connectionManager.withConnection(new JdbcConnectionCallback<TransferTicket>() {

            @Override
            public TransferTicket doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO cluster_transfer_ticket (ticket_id, request_id, player_uuid, player_name, teleport_kind, source_server_id, target_server_id, target_dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, status, status_message, created_at, expires_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    bindTicket(statement, ticket);
                    statement.executeUpdate();
                }
                return ticket;
            }
        });
    }

    @Override
    public TransferTicket update(final TransferTicket ticket) {
        return connectionManager.withConnection(new JdbcConnectionCallback<TransferTicket>() {

            @Override
            public TransferTicket doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE cluster_transfer_ticket SET status = ?, status_message = ?, expires_at = ?, updated_at = ? WHERE ticket_id = ?")) {
                    statement.setString(1, ticket.getStatus().name());
                    setNullableText(statement, 2, ticket.getStatusMessage());
                    statement.setTimestamp(3, java.sql.Timestamp.from(ticket.getExpiresAt()));
                    statement.setTimestamp(4, java.sql.Timestamp.from(ticket.getUpdatedAt()));
                    statement.setString(5, ticket.getTicketId());
                    statement.executeUpdate();
                }
                return ticket;
            }
        });
    }

    @Override
    public Optional<TransferTicket> findByRequestId(final String requestId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<TransferTicket>>() {

            @Override
            public Optional<TransferTicket> doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT ticket_id, request_id, player_uuid, player_name, teleport_kind, source_server_id, target_server_id, target_dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, status, status_message, created_at, expires_at, updated_at FROM cluster_transfer_ticket WHERE request_id = ?")) {
                    statement.setString(1, requestId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? Optional.of(mapTicket(resultSet)) : Optional.<TransferTicket>empty();
                    }
                }
            }
        });
    }

    @Override
    public Optional<TransferTicket> findActiveForTargetPlayer(final String targetServerId, final String playerUuid,
        final Instant now) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<TransferTicket>>() {

            @Override
            public Optional<TransferTicket> doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT ticket_id, request_id, player_uuid, player_name, teleport_kind, source_server_id, target_server_id, target_dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, status, status_message, created_at, expires_at, updated_at FROM cluster_transfer_ticket WHERE target_server_id = ? AND player_uuid = ? AND status = ? AND expires_at > ? ORDER BY created_at DESC LIMIT 1")) {
                    statement.setString(1, targetServerId);
                    statement.setString(2, playerUuid);
                    statement.setString(3, TransferTicketStatus.DISPATCHED.name());
                    statement.setTimestamp(4, java.sql.Timestamp.from(now));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? Optional.of(mapTicket(resultSet)) : Optional.<TransferTicket>empty();
                    }
                }
            }
        });
    }

    @Override
    public int expireActiveTickets(final Instant now) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Integer>() {

            @Override
            public Integer doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE cluster_transfer_ticket SET status = ?, status_message = ?, updated_at = ? WHERE status IN (?, ?) AND expires_at <= ?")) {
                    statement.setString(1, TransferTicketStatus.EXPIRED.name());
                    statement.setString(2, EXPIRED_STATUS_MESSAGE);
                    statement.setTimestamp(3, java.sql.Timestamp.from(now));
                    statement.setString(4, TransferTicketStatus.PENDING_GATEWAY.name());
                    statement.setString(5, TransferTicketStatus.DISPATCHED.name());
                    statement.setTimestamp(6, java.sql.Timestamp.from(now));
                    return statement.executeUpdate();
                }
            }
        });
    }

    private void bindTicket(PreparedStatement statement, TransferTicket ticket) throws SQLException {
        statement.setString(1, ticket.getTicketId());
        statement.setString(2, ticket.getRequestId());
        statement.setString(3, ticket.getPlayerUuid());
        statement.setString(4, ticket.getPlayerName());
        statement.setString(5, ticket.getTeleportKind());
        statement.setString(6, ticket.getSourceServerId());
        statement.setString(7, ticket.getTarget().getServerId());
        statement.setInt(8, ticket.getTarget().getDimensionId());
        statement.setDouble(9, ticket.getTarget().getX());
        statement.setDouble(10, ticket.getTarget().getY());
        statement.setDouble(11, ticket.getTarget().getZ());
        statement.setFloat(12, ticket.getTarget().getYaw());
        statement.setFloat(13, ticket.getTarget().getPitch());
        statement.setString(14, ticket.getStatus().name());
        if (ticket.getStatusMessage() == null) {
            statement.setNull(15, Types.VARCHAR);
        } else {
            statement.setString(15, ticket.getStatusMessage());
        }
        statement.setTimestamp(16, java.sql.Timestamp.from(ticket.getCreatedAt()));
        statement.setTimestamp(17, java.sql.Timestamp.from(ticket.getExpiresAt()));
        statement.setTimestamp(18, java.sql.Timestamp.from(ticket.getUpdatedAt()));
    }

    private TransferTicket mapTicket(ResultSet resultSet) throws SQLException {
        TeleportTarget target = new TeleportTarget(resultSet.getString("target_server_id"),
            resultSet.getInt("target_dimension_id"), resultSet.getDouble("target_x"),
            resultSet.getDouble("target_y"), resultSet.getDouble("target_z"),
            resultSet.getFloat("target_yaw"), resultSet.getFloat("target_pitch"));
        Instant createdAt = readInstant(resultSet, "created_at");
        Instant expiresAt = readInstant(resultSet, "expires_at");
        Instant updatedAt = readInstant(resultSet, "updated_at");
        return new TransferTicket(resultSet.getString("ticket_id"), resultSet.getString("request_id"),
            resultSet.getString("player_uuid"), resultSet.getString("player_name"),
            resultSet.getString("teleport_kind"), resultSet.getString("source_server_id"), target,
            TransferTicketStatus.valueOf(resultSet.getString("status")), resultSet.getString("status_message"),
            createdAt, expiresAt, updatedAt);
    }
}