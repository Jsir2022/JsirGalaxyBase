package com.jsirgalaxybase.modules.land.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.land.domain.LandActionResult;
import com.jsirgalaxybase.modules.land.domain.LandActionType;
import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.LandTitleStatus;
import com.jsirgalaxybase.modules.land.domain.PersonalLandActionReceipt;
import com.jsirgalaxybase.modules.land.domain.PersonalLandTitle;
import com.jsirgalaxybase.modules.land.port.PersonalLandRepository;

public final class JdbcPersonalLandRepository implements PersonalLandRepository {

    private static final int REQUEST_LOCK_NAMESPACE = 0x4A47424C;
    private static final int OWNER_LOCK_NAMESPACE = 0x4C414E44;
    private static final int CHUNK_LOCK_NAMESPACE = 0x43484E4B;

    private static final String TITLE_COLUMNS =
        "title_id, source_server_id, dimension_id, chunk_x, chunk_z, owner_player_ref, title_status, version";

    private final JdbcConnectionManager connectionManager;

    public JdbcPersonalLandRepository(JdbcConnectionManager connectionManager) {
        if (connectionManager == null) throw new IllegalArgumentException("connectionManager must not be null");
        this.connectionManager = connectionManager;
    }

    @Override
    public long nextTitleId() {
        return connectionManager.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT nextval(pg_get_serial_sequence('land_title', 'title_id'))");
                ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("land_title sequence did not return a value");
                return resultSet.getLong(1);
            }
        });
    }

    @Override
    public Optional<PersonalLandTitle> findByChunkKey(LandChunkKey chunkKey) {
        return findByChunkKey(chunkKey, false);
    }

    @Override
    public Optional<PersonalLandTitle> lockByChunkKey(LandChunkKey chunkKey) {
        advisoryLock(CHUNK_LOCK_NAMESPACE, chunkKey.toString());
        return findByChunkKey(chunkKey, true);
    }

    private Optional<PersonalLandTitle> findByChunkKey(final LandChunkKey chunkKey, final boolean forUpdate) {
        return connectionManager.withConnection(connection -> {
            String sql = "SELECT " + TITLE_COLUMNS
                + " FROM land_title WHERE source_server_id = ? AND dimension_id = ? AND chunk_x = ? AND chunk_z = ?"
                + " AND title_status <> 'REVOKED'" + (forUpdate ? " FOR UPDATE" : "");
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindChunk(statement, chunkKey, 1);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(mapTitle(resultSet)) : Optional.<PersonalLandTitle>empty();
                }
            }
        });
    }

    @Override
    public List<PersonalLandTitle> findByOwnerPlayerRef(final String ownerPlayerRef) {
        return connectionManager.withConnection(connection -> {
            String sql = "SELECT " + TITLE_COLUMNS
                + " FROM land_title WHERE owner_player_ref = ? AND title_status <> 'REVOKED'"
                + " ORDER BY source_server_id, dimension_id, chunk_x, chunk_z, title_id";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, ownerPlayerRef);
                return readTitles(statement);
            }
        });
    }

    @Override
    public Collection<PersonalLandTitle> findAll() {
        return connectionManager.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + TITLE_COLUMNS
                    + " FROM land_title WHERE title_status <> 'REVOKED' ORDER BY title_id")) {
                return readTitles(statement);
            }
        });
    }

    @Override
    public Collection<PersonalLandTitle> findAllByServerId(final String serverId) {
        return connectionManager.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + TITLE_COLUMNS
                    + " FROM land_title WHERE source_server_id = ? AND title_status <> 'REVOKED' ORDER BY title_id")) {
                statement.setString(1, serverId);
                return readTitles(statement);
            }
        });
    }

    @Override
    public void save(final PersonalLandTitle title) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {

            @Override
            public Void doInConnection(Connection connection) throws SQLException {
                String sql = "INSERT INTO land_title (title_id, source_server_id, dimension_id, chunk_x, chunk_z,"
                    + " owner_player_ref, title_status, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    + " ON CONFLICT (title_id) DO UPDATE SET owner_player_ref = EXCLUDED.owner_player_ref,"
                    + " title_status = EXCLUDED.title_status, version = EXCLUDED.version, updated_at = CURRENT_TIMESTAMP";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, title.getTitleId());
                    statement.setString(2, title.getChunkKey().getServerId());
                    statement.setInt(3, title.getChunkKey().getDimensionId());
                    statement.setInt(4, title.getChunkKey().getChunkX());
                    statement.setInt(5, title.getChunkKey().getChunkZ());
                    statement.setString(6, title.getOwnerPlayerRef());
                    statement.setString(7, title.getStatus().name());
                    statement.setLong(8, title.getVersion());
                    statement.executeUpdate();
                }
                return null;
            }
        });
    }

    @Override
    public void remove(LandChunkKey chunkKey) {
        throw new UnsupportedOperationException("Persistent land titles must be revoked, not deleted");
    }

    @Override
    public void lockRequest(String requestId) {
        advisoryLock(REQUEST_LOCK_NAMESPACE, requestId);
    }

    @Override
    public void lockOwner(String ownerPlayerRef) {
        advisoryLock(OWNER_LOCK_NAMESPACE, ownerPlayerRef);
    }

    private void advisoryLock(final int namespace, final String value) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {

            @Override
            public Void doInConnection(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(?, hashtext(?))")) {
                    statement.setInt(1, namespace);
                    statement.setString(2, value);
                    statement.executeQuery();
                }
                return null;
            }
        });
    }

    @Override
    public Optional<PersonalLandActionReceipt> findReceiptByRequestId(final String requestId) {
        return connectionManager.withConnection(connection -> {
            String sql = "SELECT * FROM land_operation_log WHERE request_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, requestId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(mapReceipt(resultSet))
                        : Optional.<PersonalLandActionReceipt>empty();
                }
            }
        });
    }

    @Override
    public void saveReceipt(final PersonalLandActionReceipt receipt) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {

            @Override
            public Void doInConnection(Connection connection) throws SQLException {
                ReceiptCoordinates coordinates = ReceiptCoordinates.from(receipt);
                String sql = "INSERT INTO land_operation_log (request_id, semantics_key, action_type, result_code,"
                    + " operator_player_ref, source_server_id, dimension_id, chunk_x, chunk_z, expected_version,"
                    + " before_title_id, before_owner_player_ref, before_title_status, before_version,"
                    + " after_title_id, after_owner_player_ref, after_title_status, after_version)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, receipt.getRequestId());
                    statement.setString(2, receipt.getSemanticsKey());
                    statement.setString(3, receipt.getActionType().name());
                    statement.setString(4, receipt.getResult().name());
                    statement.setString(5, coordinates.operatorPlayerRef);
                    statement.setString(6, coordinates.chunkKey.getServerId());
                    statement.setInt(7, coordinates.chunkKey.getDimensionId());
                    statement.setInt(8, coordinates.chunkKey.getChunkX());
                    statement.setInt(9, coordinates.chunkKey.getChunkZ());
                    if (coordinates.expectedVersion == null) statement.setNull(10, Types.BIGINT);
                    else statement.setLong(10, coordinates.expectedVersion.longValue());
                    bindSnapshot(statement, 11, receipt.getBeforeTitle());
                    bindSnapshot(statement, 15, receipt.getAfterTitle());
                    statement.executeUpdate();
                }
                return null;
            }
        });
    }

    private static void bindSnapshot(PreparedStatement statement, int start, PersonalLandTitle title)
        throws SQLException {
        if (title == null) {
            statement.setNull(start, Types.BIGINT);
            statement.setNull(start + 1, Types.VARCHAR);
            statement.setNull(start + 2, Types.VARCHAR);
            statement.setNull(start + 3, Types.BIGINT);
            return;
        }
        statement.setLong(start, title.getTitleId());
        statement.setString(start + 1, title.getOwnerPlayerRef());
        statement.setString(start + 2, title.getStatus().name());
        statement.setLong(start + 3, title.getVersion());
    }

    private static PersonalLandActionReceipt mapReceipt(ResultSet resultSet) throws SQLException {
        LandChunkKey key = new LandChunkKey(resultSet.getString("source_server_id"),
            resultSet.getInt("dimension_id"), resultSet.getInt("chunk_x"), resultSet.getInt("chunk_z"));
        PersonalLandTitle before = mapSnapshot(resultSet, key, "before_");
        PersonalLandTitle after = mapSnapshot(resultSet, key, "after_");
        return new PersonalLandActionReceipt(resultSet.getString("request_id"), resultSet.getString("semantics_key"),
            LandActionType.valueOf(resultSet.getString("action_type")),
            LandActionResult.valueOf(resultSet.getString("result_code")), before, after);
    }

    private static PersonalLandTitle mapSnapshot(ResultSet resultSet, LandChunkKey key, String prefix)
        throws SQLException {
        long titleId = resultSet.getLong(prefix + "title_id");
        if (resultSet.wasNull()) return null;
        return new PersonalLandTitle(titleId, key, resultSet.getString(prefix + "owner_player_ref"),
            LandTitleStatus.valueOf(resultSet.getString(prefix + "title_status")),
            resultSet.getLong(prefix + "version"));
    }

    private static List<PersonalLandTitle> readTitles(PreparedStatement statement) throws SQLException {
        List<PersonalLandTitle> result = new ArrayList<PersonalLandTitle>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) result.add(mapTitle(resultSet));
        }
        return result;
    }

    private static PersonalLandTitle mapTitle(ResultSet resultSet) throws SQLException {
        LandChunkKey key = new LandChunkKey(resultSet.getString("source_server_id"),
            resultSet.getInt("dimension_id"), resultSet.getInt("chunk_x"), resultSet.getInt("chunk_z"));
        return new PersonalLandTitle(resultSet.getLong("title_id"), key, resultSet.getString("owner_player_ref"),
            LandTitleStatus.valueOf(resultSet.getString("title_status")), resultSet.getLong("version"));
    }

    private static void bindChunk(PreparedStatement statement, LandChunkKey chunkKey, int start) throws SQLException {
        statement.setString(start, chunkKey.getServerId());
        statement.setInt(start + 1, chunkKey.getDimensionId());
        statement.setInt(start + 2, chunkKey.getChunkX());
        statement.setInt(start + 3, chunkKey.getChunkZ());
    }

    private static final class ReceiptCoordinates {

        private final String operatorPlayerRef;
        private final LandChunkKey chunkKey;
        private final Long expectedVersion;

        private ReceiptCoordinates(String operatorPlayerRef, LandChunkKey chunkKey, Long expectedVersion) {
            this.operatorPlayerRef = operatorPlayerRef;
            this.chunkKey = chunkKey;
            this.expectedVersion = expectedVersion;
        }

        private static ReceiptCoordinates from(PersonalLandActionReceipt receipt) {
            PersonalLandTitle snapshot = receipt.getBeforeTitle() == null ? receipt.getAfterTitle()
                : receipt.getBeforeTitle();
            String[] parts = receipt.getSemanticsKey().split("\\|", -1);
            if (parts.length < 3) throw new IllegalArgumentException("Invalid land semantics key");
            String operator = parts[1];
            LandChunkKey key = snapshot == null ? parseChunk(parts[2]) : snapshot.getChunkKey();
            Long expected = parts.length > 3 ? Long.valueOf(parts[3]) : null;
            return new ReceiptCoordinates(operator, key, expected);
        }

        private static LandChunkKey parseChunk(String value) {
            int bracket = value.indexOf(": [");
            if (bracket < 0) bracket = value.indexOf(":[");
            int serverEnd = value.indexOf(":[");
            int at = value.indexOf('@', serverEnd + 2);
            int comma = value.indexOf(',', at + 1);
            int close = value.indexOf(']', comma + 1);
            if (serverEnd <= 0 || at < 0 || comma < 0 || close < 0) {
                throw new IllegalArgumentException("Invalid land chunk semantics: " + value);
            }
            String serverId = value.substring(0, serverEnd);
            int dimension = Integer.parseInt(value.substring(serverEnd + 2, at));
            int chunkX = Integer.parseInt(value.substring(at + 1, comma));
            int chunkZ = Integer.parseInt(value.substring(comma + 1, close));
            return new LandChunkKey(serverId, dimension, chunkX, chunkZ);
        }
    }
}
