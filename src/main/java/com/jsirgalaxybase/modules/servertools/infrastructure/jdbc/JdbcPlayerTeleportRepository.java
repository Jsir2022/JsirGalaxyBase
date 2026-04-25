package com.jsirgalaxybase.modules.servertools.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.AbstractJdbcRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.servertools.domain.BackRecord;
import com.jsirgalaxybase.modules.servertools.domain.PlayerHome;
import com.jsirgalaxybase.modules.servertools.domain.RandomTeleportRecord;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportKind;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequest;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequestStatus;
import com.jsirgalaxybase.modules.servertools.port.PlayerTeleportRepository;

public class JdbcPlayerTeleportRepository extends AbstractJdbcRepository implements PlayerTeleportRepository {

    public JdbcPlayerTeleportRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public List<PlayerHome> listHomes(final String playerUuid) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<PlayerHome>>() {

            @Override
            public List<PlayerHome> doInConnection(Connection connection) throws SQLException {
                List<PlayerHome> homes = new ArrayList<PlayerHome>();
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT player_uuid, home_name, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, created_at, updated_at FROM player_home WHERE player_uuid = ? ORDER BY home_name ASC")) {
                    statement.setString(1, playerUuid);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            homes.add(mapHome(resultSet));
                        }
                    }
                }
                return homes;
            }
        });
    }

    @Override
    public Optional<PlayerHome> findHome(final String playerUuid, final String homeName) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<PlayerHome>>() {

            @Override
            public Optional<PlayerHome> doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT player_uuid, home_name, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, created_at, updated_at FROM player_home WHERE player_uuid = ? AND home_name = ?")) {
                    statement.setString(1, playerUuid);
                    statement.setString(2, homeName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? Optional.of(mapHome(resultSet)) : Optional.<PlayerHome>empty();
                    }
                }
            }
        });
    }

    @Override
    public PlayerHome saveHome(final PlayerHome playerHome) {
        return connectionManager.withConnection(new JdbcConnectionCallback<PlayerHome>() {

            @Override
            public PlayerHome doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO player_home (player_uuid, home_name, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT (player_uuid, home_name) DO UPDATE SET server_id = EXCLUDED.server_id, dimension_id = EXCLUDED.dimension_id, target_x = EXCLUDED.target_x, target_y = EXCLUDED.target_y, target_z = EXCLUDED.target_z, target_yaw = EXCLUDED.target_yaw, target_pitch = EXCLUDED.target_pitch, updated_at = EXCLUDED.updated_at RETURNING player_uuid, home_name, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, created_at, updated_at")) {
                    bindTarget(statement, 3, playerHome.getTarget());
                    statement.setString(1, playerHome.getPlayerUuid());
                    statement.setString(2, playerHome.getHomeName());
                    statement.setTimestamp(10, Timestamp.from(playerHome.getCreatedAt()));
                    statement.setTimestamp(11, Timestamp.from(playerHome.getUpdatedAt()));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        return mapHome(resultSet);
                    }
                }
            }
        });
    }

    @Override
    public boolean deleteHome(final String playerUuid, final String homeName) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Boolean>() {

            @Override
            public Boolean doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM player_home WHERE player_uuid = ? AND home_name = ?")) {
                    statement.setString(1, playerUuid);
                    statement.setString(2, homeName);
                    return statement.executeUpdate() > 0;
                }
            }
        });
    }

    @Override
    public Optional<BackRecord> findBackRecord(final String playerUuid) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<BackRecord>>() {

            @Override
            public Optional<BackRecord> doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT player_uuid, teleport_kind, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, recorded_at FROM player_back_record WHERE player_uuid = ?")) {
                    statement.setString(1, playerUuid);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? Optional.of(mapBackRecord(resultSet)) : Optional.<BackRecord>empty();
                    }
                }
            }
        });
    }

    @Override
    public BackRecord saveBackRecord(final BackRecord backRecord) {
        return connectionManager.withConnection(new JdbcConnectionCallback<BackRecord>() {

            @Override
            public BackRecord doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO player_back_record (player_uuid, teleport_kind, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, recorded_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT (player_uuid) DO UPDATE SET teleport_kind = EXCLUDED.teleport_kind, server_id = EXCLUDED.server_id, dimension_id = EXCLUDED.dimension_id, target_x = EXCLUDED.target_x, target_y = EXCLUDED.target_y, target_z = EXCLUDED.target_z, target_yaw = EXCLUDED.target_yaw, target_pitch = EXCLUDED.target_pitch, recorded_at = EXCLUDED.recorded_at RETURNING player_uuid, teleport_kind, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, recorded_at")) {
                    statement.setString(1, backRecord.getPlayerUuid());
                    statement.setString(2, backRecord.getTeleportKind().name());
                    bindTarget(statement, 3, backRecord.getTarget());
                    statement.setTimestamp(10, Timestamp.from(backRecord.getRecordedAt()));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        return mapBackRecord(resultSet);
                    }
                }
            }
        });
    }

    @Override
    public List<ServerWarp> listWarps() {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<ServerWarp>>() {

            @Override
            public List<ServerWarp> doInConnection(Connection connection) throws SQLException {
                List<ServerWarp> warps = new ArrayList<ServerWarp>();
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT warp_name, display_name, description, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, enabled, created_at, updated_at FROM server_warp ORDER BY warp_name ASC");
                    ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        warps.add(mapWarp(resultSet));
                    }
                }
                return warps;
            }
        });
    }

    @Override
    public Optional<ServerWarp> findWarp(final String warpName) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<ServerWarp>>() {

            @Override
            public Optional<ServerWarp> doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT warp_name, display_name, description, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, enabled, created_at, updated_at FROM server_warp WHERE warp_name = ?")) {
                    statement.setString(1, warpName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? Optional.of(mapWarp(resultSet)) : Optional.<ServerWarp>empty();
                    }
                }
            }
        });
    }

    @Override
    public ServerWarp saveWarp(final ServerWarp serverWarp) {
        return connectionManager.withConnection(new JdbcConnectionCallback<ServerWarp>() {

            @Override
            public ServerWarp doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO server_warp (warp_name, display_name, description, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT (warp_name) DO UPDATE SET display_name = EXCLUDED.display_name, description = EXCLUDED.description, server_id = EXCLUDED.server_id, dimension_id = EXCLUDED.dimension_id, target_x = EXCLUDED.target_x, target_y = EXCLUDED.target_y, target_z = EXCLUDED.target_z, target_yaw = EXCLUDED.target_yaw, target_pitch = EXCLUDED.target_pitch, enabled = EXCLUDED.enabled, updated_at = EXCLUDED.updated_at RETURNING warp_name, display_name, description, server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, enabled, created_at, updated_at")) {
                    statement.setString(1, serverWarp.getWarpName());
                    statement.setString(2, serverWarp.getDisplayName());
                    setNullableText(statement, 3, serverWarp.getDescription());
                    bindTarget(statement, 4, serverWarp.getTarget());
                    statement.setBoolean(11, serverWarp.isEnabled());
                    statement.setTimestamp(12, Timestamp.from(serverWarp.getCreatedAt()));
                    statement.setTimestamp(13, Timestamp.from(serverWarp.getUpdatedAt()));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        return mapWarp(resultSet);
                    }
                }
            }
        });
    }

    @Override
    public TpaRequest saveTpaRequest(final TpaRequest tpaRequest) {
        return connectionManager.withConnection(new JdbcConnectionCallback<TpaRequest>() {

            @Override
            public TpaRequest doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO player_tpa_request (request_id, requester_player_uuid, requester_player_name, requester_server_id, requester_origin_server_id, requester_origin_dimension_id, requester_origin_x, requester_origin_y, requester_origin_z, requester_origin_yaw, requester_origin_pitch, target_player_name, target_server_id, status, created_at, expires_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    statement.setString(1, tpaRequest.getRequestId());
                    statement.setString(2, tpaRequest.getRequesterPlayerUuid());
                    statement.setString(3, tpaRequest.getRequesterPlayerName());
                    statement.setString(4, tpaRequest.getRequesterServerId());
                    bindTarget(statement, 5, tpaRequest.getRequesterOrigin());
                    statement.setString(12, tpaRequest.getTargetPlayerName());
                    statement.setString(13, tpaRequest.getTargetServerId());
                    statement.setString(14, tpaRequest.getStatus().name());
                    statement.setTimestamp(15, Timestamp.from(tpaRequest.getCreatedAt()));
                    statement.setTimestamp(16, Timestamp.from(tpaRequest.getExpiresAt()));
                    statement.setTimestamp(17, Timestamp.from(tpaRequest.getUpdatedAt()));
                    statement.executeUpdate();
                }
                return tpaRequest;
            }
        });
    }

    @Override
    public TpaRequest updateTpaRequest(final TpaRequest tpaRequest) {
        return connectionManager.withConnection(new JdbcConnectionCallback<TpaRequest>() {

            @Override
            public TpaRequest doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE player_tpa_request SET status = ?, updated_at = ? WHERE request_id = ?")) {
                    statement.setString(1, tpaRequest.getStatus().name());
                    statement.setTimestamp(2, Timestamp.from(tpaRequest.getUpdatedAt()));
                    statement.setString(3, tpaRequest.getRequestId());
                    statement.executeUpdate();
                }
                return tpaRequest;
            }
        });
    }

    @Override
    public Optional<TpaRequest> findPendingTpaRequest(final String requesterPlayerName, final String targetPlayerName,
        final String targetServerId, final Instant now) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<TpaRequest>>() {

            @Override
            public Optional<TpaRequest> doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT request_id, requester_player_uuid, requester_player_name, requester_server_id, requester_origin_server_id, requester_origin_dimension_id, requester_origin_x, requester_origin_y, requester_origin_z, requester_origin_yaw, requester_origin_pitch, target_player_name, target_server_id, status, created_at, expires_at, updated_at FROM player_tpa_request WHERE status = 'PENDING' AND LOWER(requester_player_name) = LOWER(?) AND LOWER(target_player_name) = LOWER(?) AND target_server_id = ? AND expires_at > ? ORDER BY created_at DESC LIMIT 1")) {
                    statement.setString(1, requesterPlayerName);
                    statement.setString(2, targetPlayerName);
                    statement.setString(3, targetServerId);
                    statement.setTimestamp(4, Timestamp.from(now));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        return resultSet.next() ? Optional.of(mapTpaRequest(resultSet)) : Optional.<TpaRequest>empty();
                    }
                }
            }
        });
    }

    @Override
    public int expirePendingTpaRequests(final Instant now) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Integer>() {

            @Override
            public Integer doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE player_tpa_request SET status = 'EXPIRED', updated_at = ? WHERE status = 'PENDING' AND expires_at <= ?")) {
                    statement.setTimestamp(1, Timestamp.from(now));
                    statement.setTimestamp(2, Timestamp.from(now));
                    return statement.executeUpdate();
                }
            }
        });
    }

    @Override
    public RandomTeleportRecord saveRandomTeleportRecord(final RandomTeleportRecord randomTeleportRecord) {
        return connectionManager.withConnection(new JdbcConnectionCallback<RandomTeleportRecord>() {

            @Override
            public RandomTeleportRecord doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO player_rtp_record (player_uuid, source_server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING record_id, player_uuid, source_server_id, dimension_id, target_x, target_y, target_z, target_yaw, target_pitch, created_at")) {
                    statement.setString(1, randomTeleportRecord.getPlayerUuid());
                    statement.setString(2, randomTeleportRecord.getSourceServerId());
                    statement.setInt(3, randomTeleportRecord.getTarget().getDimensionId());
                    statement.setDouble(4, randomTeleportRecord.getTarget().getX());
                    statement.setDouble(5, randomTeleportRecord.getTarget().getY());
                    statement.setDouble(6, randomTeleportRecord.getTarget().getZ());
                    statement.setFloat(7, randomTeleportRecord.getTarget().getYaw());
                    statement.setFloat(8, randomTeleportRecord.getTarget().getPitch());
                    statement.setTimestamp(9, Timestamp.from(randomTeleportRecord.getCreatedAt()));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        return mapRandomTeleportRecord(resultSet);
                    }
                }
            }
        });
    }

    private void bindTarget(PreparedStatement statement, int baseIndex, TeleportTarget target) throws SQLException {
        statement.setString(baseIndex, target.getServerId());
        statement.setInt(baseIndex + 1, target.getDimensionId());
        statement.setDouble(baseIndex + 2, target.getX());
        statement.setDouble(baseIndex + 3, target.getY());
        statement.setDouble(baseIndex + 4, target.getZ());
        statement.setFloat(baseIndex + 5, target.getYaw());
        statement.setFloat(baseIndex + 6, target.getPitch());
    }

    private PlayerHome mapHome(ResultSet resultSet) throws SQLException {
        return new PlayerHome(resultSet.getString("player_uuid"), resultSet.getString("home_name"),
            mapTarget(resultSet, "server_id", "dimension_id", "target_x", "target_y", "target_z", "target_yaw",
                "target_pitch"),
            readInstant(resultSet, "created_at"), readInstant(resultSet, "updated_at"));
    }

    private BackRecord mapBackRecord(ResultSet resultSet) throws SQLException {
        return new BackRecord(resultSet.getString("player_uuid"),
            TeleportKind.valueOf(resultSet.getString("teleport_kind")),
            mapTarget(resultSet, "server_id", "dimension_id", "target_x", "target_y", "target_z", "target_yaw",
                "target_pitch"),
            readInstant(resultSet, "recorded_at"));
    }

    private ServerWarp mapWarp(ResultSet resultSet) throws SQLException {
        return new ServerWarp(resultSet.getString("warp_name"), resultSet.getString("display_name"),
            resultSet.getString("description"),
            mapTarget(resultSet, "server_id", "dimension_id", "target_x", "target_y", "target_z", "target_yaw",
                "target_pitch"),
            resultSet.getBoolean("enabled"), readInstant(resultSet, "created_at"), readInstant(resultSet, "updated_at"));
    }

    private TpaRequest mapTpaRequest(ResultSet resultSet) throws SQLException {
        TeleportTarget origin = mapTarget(resultSet, "requester_origin_server_id", "requester_origin_dimension_id",
            "requester_origin_x", "requester_origin_y", "requester_origin_z", "requester_origin_yaw",
            "requester_origin_pitch");
        return new TpaRequest(resultSet.getString("request_id"), resultSet.getString("requester_player_uuid"),
            resultSet.getString("requester_player_name"), resultSet.getString("requester_server_id"), origin,
            resultSet.getString("target_player_name"), resultSet.getString("target_server_id"),
            TpaRequestStatus.valueOf(resultSet.getString("status")), readInstant(resultSet, "created_at"),
            readInstant(resultSet, "expires_at"), readInstant(resultSet, "updated_at"));
    }

    private RandomTeleportRecord mapRandomTeleportRecord(ResultSet resultSet) throws SQLException {
        TeleportTarget target = mapTarget(resultSet, "source_server_id", "dimension_id", "target_x", "target_y",
            "target_z", "target_yaw", "target_pitch");
        return new RandomTeleportRecord(resultSet.getLong("record_id"), resultSet.getString("player_uuid"),
            resultSet.getString("source_server_id"), target, readInstant(resultSet, "created_at"));
    }

    private TeleportTarget mapTarget(ResultSet resultSet, String serverColumn, String dimensionColumn, String xColumn,
        String yColumn, String zColumn, String yawColumn, String pitchColumn) throws SQLException {
        return new TeleportTarget(resultSet.getString(serverColumn), resultSet.getInt(dimensionColumn),
            resultSet.getDouble(xColumn), resultSet.getDouble(yColumn), resultSet.getDouble(zColumn),
            resultSet.getFloat(yawColumn), resultSet.getFloat(pitchColumn));
    }
}