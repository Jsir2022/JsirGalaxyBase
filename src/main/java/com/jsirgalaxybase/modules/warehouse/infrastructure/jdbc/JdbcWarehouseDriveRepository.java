package com.jsirgalaxybase.modules.warehouse.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveKey;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveOperation;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveRecord;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveResult;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveStatus;
import com.jsirgalaxybase.modules.warehouse.port.WarehouseDriveRepository;

public final class JdbcWarehouseDriveRepository implements WarehouseDriveRepository {

    private static final int REQUEST_LOCK_NAMESPACE = 0x57445251;
    private static final int DRIVE_LOCK_NAMESPACE = 0x57445256;
    private final JdbcConnectionManager connectionManager;

    public JdbcWarehouseDriveRepository(JdbcConnectionManager connectionManager) {
        if (connectionManager == null) throw new IllegalArgumentException("connectionManager is required");
        this.connectionManager = connectionManager;
    }

    @Override public long nextDriveId() {
        return connectionManager.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT nextval(pg_get_serial_sequence('warehouse_drive', 'drive_id'))"); ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("warehouse_drive sequence did not return a value");
                return rows.getLong(1);
            }
        });
    }

    @Override public void lockRequest(String requestId) { advisoryLock(REQUEST_LOCK_NAMESPACE, requestId); }
    @Override public void lockKey(WarehouseDriveKey key) { advisoryLock(DRIVE_LOCK_NAMESPACE, key.toString()); }

    @Override public Optional<WarehouseDriveRecord> findActiveByKey(WarehouseDriveKey key) {
        return connectionManager.withConnection(connection -> {
            String sql = "SELECT drive_id, source_server_id, dimension_id, block_x, block_y, block_z, owner_player_ref, drive_status, version"
                + " FROM warehouse_drive WHERE source_server_id=? AND dimension_id=? AND block_x=? AND block_y=? AND block_z=?"
                + " AND drive_status='ACTIVE'";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindKey(statement, key, 1);
                try (ResultSet rows = statement.executeQuery()) { return rows.next() ? Optional.of(mapRecord(rows)) : Optional.empty(); }
            }
        });
    }

    @Override public Optional<WarehouseDriveReceipt> findReceipt(String requestId) {
        return connectionManager.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM warehouse_drive_operation WHERE request_id=?")) {
                statement.setString(1, requestId);
                try (ResultSet rows = statement.executeQuery()) { return rows.next() ? Optional.of(mapReceipt(rows)) : Optional.empty(); }
            }
        });
    }

    @Override public List<WarehouseDriveRecord> findActiveByOwner(String owner, String serverId) {
        return connectionManager.withConnection(connection -> {
            String sql = "SELECT drive_id, source_server_id, dimension_id, block_x, block_y, block_z, owner_player_ref, drive_status, version"
                + " FROM warehouse_drive WHERE owner_player_ref=? AND source_server_id=? AND drive_status='ACTIVE'"
                + " ORDER BY dimension_id, block_x, block_y, block_z, drive_id";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, owner); statement.setString(2, serverId); return readRecords(statement);
            }
        });
    }

    @Override public List<WarehouseDriveReceipt> findRecentByOwner(String owner, String serverId, int limit) {
        return connectionManager.withConnection(connection -> {
            String sql = "SELECT * FROM warehouse_drive_operation WHERE operator_player_ref=? AND source_server_id=?"
                + " ORDER BY operation_id DESC LIMIT ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, owner); statement.setString(2, serverId); statement.setInt(3, limit); return readReceipts(statement);
            }
        });
    }

    @Override public void save(final WarehouseDriveRecord record) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {
            @Override public Void doInConnection(Connection connection) throws SQLException {
                String sql = "INSERT INTO warehouse_drive (drive_id, source_server_id, dimension_id, block_x, block_y, block_z, owner_player_ref, drive_status, version)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (drive_id) DO UPDATE SET drive_status=EXCLUDED.drive_status,"
                    + " version=EXCLUDED.version, updated_at=CURRENT_TIMESTAMP";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, record.getDriveId()); bindKey(statement, record.getKey(), 2);
                    statement.setString(7, record.getOwnerPlayerRef()); statement.setString(8, record.getStatus().name()); statement.setLong(9, record.getVersion());
                    statement.executeUpdate();
                }
                return null;
            }
        });
    }

    @Override public void saveReceipt(final WarehouseDriveReceipt receipt, final String actor) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {
            @Override public Void doInConnection(Connection connection) throws SQLException {
                String sql = "INSERT INTO warehouse_drive_operation (request_id, semantics_key, operation_type, result_code, operator_player_ref,"
                    + " source_server_id, dimension_id, block_x, block_y, block_z, before_drive_id, before_owner_player_ref, before_status, before_version,"
                    + " after_drive_id, after_owner_player_ref, after_status, after_version, detail) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, receipt.getRequestId()); statement.setString(2, receipt.getSemanticsKey());
                    statement.setString(3, receipt.getOperation().name()); statement.setString(4, receipt.getResult().name()); statement.setString(5, actor);
                    bindKey(statement, receipt.getKey(), 6); bindSnapshot(statement, 11, receipt.getBefore()); bindSnapshot(statement, 15, receipt.getAfter());
                    statement.setString(19, receipt.getDetail()); statement.executeUpdate();
                }
                return null;
            }
        });
    }

    private void advisoryLock(final int namespace, final String value) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {
            @Override public Void doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?, hashtext(?))")) {
                    statement.setInt(1, namespace); statement.setString(2, value); statement.execute();
                }
                return null;
            }
        });
    }
    private static void bindKey(PreparedStatement statement, WarehouseDriveKey key, int start) throws SQLException {
        statement.setString(start, key.getServerId()); statement.setInt(start + 1, key.getDimensionId()); statement.setInt(start + 2, key.getBlockX());
        statement.setInt(start + 3, key.getBlockY()); statement.setInt(start + 4, key.getBlockZ());
    }
    private static void bindSnapshot(PreparedStatement statement, int start, WarehouseDriveRecord record) throws SQLException {
        if (record == null) {
            statement.setNull(start, java.sql.Types.BIGINT); statement.setNull(start + 1, java.sql.Types.VARCHAR);
            statement.setNull(start + 2, java.sql.Types.VARCHAR); statement.setNull(start + 3, java.sql.Types.BIGINT);
        } else {
            statement.setLong(start, record.getDriveId()); statement.setString(start + 1, record.getOwnerPlayerRef());
            statement.setString(start + 2, record.getStatus().name()); statement.setLong(start + 3, record.getVersion());
        }
    }
    private static WarehouseDriveRecord mapRecord(ResultSet rows) throws SQLException {
        return new WarehouseDriveRecord(rows.getLong("drive_id"), new WarehouseDriveKey(rows.getString("source_server_id"), rows.getInt("dimension_id"),
            rows.getInt("block_x"), rows.getInt("block_y"), rows.getInt("block_z")), rows.getString("owner_player_ref"),
            WarehouseDriveStatus.valueOf(rows.getString("drive_status")), rows.getLong("version"));
    }
    private static WarehouseDriveReceipt mapReceipt(ResultSet rows) throws SQLException {
        WarehouseDriveKey key = new WarehouseDriveKey(rows.getString("source_server_id"), rows.getInt("dimension_id"), rows.getInt("block_x"), rows.getInt("block_y"), rows.getInt("block_z"));
        WarehouseDriveRecord before = mapSnapshot(rows, key, "before_"); WarehouseDriveRecord after = mapSnapshot(rows, key, "after_");
        return new WarehouseDriveReceipt(rows.getString("request_id"), rows.getString("semantics_key"), WarehouseDriveOperation.valueOf(rows.getString("operation_type")),
            WarehouseDriveResult.valueOf(rows.getString("result_code")), key, before, after, rows.getString("detail"));
    }
    private static WarehouseDriveRecord mapSnapshot(ResultSet rows, WarehouseDriveKey key, String prefix) throws SQLException {
        long id = rows.getLong(prefix + "drive_id"); if (rows.wasNull()) return null;
        return new WarehouseDriveRecord(id, key, rows.getString(prefix + "owner_player_ref"),
            WarehouseDriveStatus.valueOf(rows.getString(prefix + "status")), rows.getLong(prefix + "version"));
    }
    private static List<WarehouseDriveRecord> readRecords(PreparedStatement statement) throws SQLException { List<WarehouseDriveRecord> result = new ArrayList<WarehouseDriveRecord>(); try (ResultSet rows = statement.executeQuery()) { while (rows.next()) result.add(mapRecord(rows)); } return result; }
    private static List<WarehouseDriveReceipt> readReceipts(PreparedStatement statement) throws SQLException { List<WarehouseDriveReceipt> result = new ArrayList<WarehouseDriveReceipt>(); try (ResultSet rows = statement.executeQuery()) { while (rows.next()) result.add(mapReceipt(rows)); } return result; }
}
