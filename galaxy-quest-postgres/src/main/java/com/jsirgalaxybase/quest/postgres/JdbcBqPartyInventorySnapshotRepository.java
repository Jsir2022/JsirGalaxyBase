package com.jsirgalaxybase.quest.postgres;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantType;

/** PostgreSQL leases that represent fresh, active cross-server inventory observations. */
public final class JdbcBqPartyInventorySnapshotRepository {
    private static final int MAX_MEMBERS_PER_QUERY = 512;
    private static final Type ATTRIBUTES_TYPE = new TypeToken<Map<String, String>>() { }.getType();

    private final JdbcQuestConnectionManager connections;
    private final Gson gson;

    public JdbcBqPartyInventorySnapshotRepository(JdbcQuestConnectionManager connections) {
        this(connections, new Gson());
    }

    JdbcBqPartyInventorySnapshotRepository(JdbcQuestConnectionManager connections, Gson gson) {
        if (connections == null || gson == null) throw new IllegalArgumentException("dependencies are required");
        this.connections = connections;
        this.gson = gson;
    }

    /** Claims a newly authenticated login session, replacing any older server lease. */
    public void claimSession(final UUID playerId, final String sourceServer, final UUID sessionId,
        final Map<String, String> attributes, final long leaseMillis) {
        requireTransaction();
        validate(playerId, sourceServer, sessionId, attributes, leaseMillis);
        connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Void>() {
            @Override public Void execute(Connection connection) throws SQLException {
                writeLease(connection, playerId, sourceServer, sessionId, attributes, leaseMillis, false);
                return null;
            }
        });
    }

    /** Updates only the currently claimed login session, or claims an absent/expired lease. */
    public boolean publish(final UUID playerId, final String sourceServer, final UUID sessionId,
        final Map<String, String> attributes, final long leaseMillis) {
        requireTransaction();
        validate(playerId, sourceServer, sessionId, attributes, leaseMillis);
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Boolean>() {
            @Override public Boolean execute(Connection connection) throws SQLException {
                return Boolean.valueOf(writeLease(connection, playerId, sourceServer, sessionId, attributes,
                    leaseMillis, true));
            }
        }).booleanValue();
    }

    private boolean writeLease(Connection connection, UUID playerId, String sourceServer, UUID sessionId,
        Map<String, String> attributes, long leaseMillis, boolean fenceExistingSession) throws SQLException {
        String sql = "INSERT INTO galaxy_quest_active_inventory_snapshot(player_id,source_server,session_id,"
            + "attributes_json,observed_at,expires_at) VALUES (?,?,?,?::jsonb,clock_timestamp(),"
            + "clock_timestamp() + (? * INTERVAL '1 millisecond')) ON CONFLICT (player_id) DO UPDATE SET "
            + "source_server=EXCLUDED.source_server,session_id=EXCLUDED.session_id,"
            + "attributes_json=EXCLUDED.attributes_json,observed_at=EXCLUDED.observed_at,"
            + "expires_at=EXCLUDED.expires_at"
            + (fenceExistingSession ? " WHERE galaxy_quest_active_inventory_snapshot.session_id=EXCLUDED.session_id "
                + "OR galaxy_quest_active_inventory_snapshot.expires_at <= clock_timestamp()" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, playerId);
            statement.setString(2, sourceServer.trim());
            statement.setObject(3, sessionId);
            statement.setString(4, gson.toJson(attributes));
            statement.setLong(5, leaseMillis);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean lockActiveParty(final ParticipantId party) {
        requireTransaction();
        if (party == null || party.getType() != ParticipantType.PARTY) {
            throw new IllegalArgumentException("an active PARTY is required");
        }
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Boolean>() {
            @Override public Boolean execute(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM galaxy_quest_participant WHERE participant_type='PARTY' AND participant_id=? "
                        + "AND lifecycle='ACTIVE' FOR UPDATE")) {
                    statement.setObject(1, party.getId());
                    try (ResultSet row = statement.executeQuery()) { return Boolean.valueOf(row.next()); }
                }
            }
        }).booleanValue();
    }

    public Map<UUID, QuestInventorySnapshotLease> findActive(final List<UUID> playerIds) {
        requireTransaction();
        if (playerIds == null) throw new IllegalArgumentException("playerIds are required");
        if (playerIds.isEmpty()) return Collections.emptyMap();
        if (playerIds.size() > MAX_MEMBERS_PER_QUERY) throw new IllegalArgumentException(
            "party has too many members for one active inventory snapshot query");
        final StringBuilder placeholders = new StringBuilder();
        for (int index = 0; index < playerIds.size(); index++) {
            if (playerIds.get(index) == null) throw new IllegalArgumentException("player id must not be null");
            if (index > 0) placeholders.append(',');
            placeholders.append('?');
        }
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Map<UUID, QuestInventorySnapshotLease>>() {
            @Override public Map<UUID, QuestInventorySnapshotLease> execute(Connection connection) throws SQLException {
                Map<UUID, QuestInventorySnapshotLease> result = new LinkedHashMap<UUID, QuestInventorySnapshotLease>();
                String sql = "SELECT player_id,source_server,session_id,attributes_json::text "
                    + "FROM galaxy_quest_active_inventory_snapshot WHERE player_id IN (" + placeholders + ") "
                    + "AND expires_at > clock_timestamp()";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    for (int index = 0; index < playerIds.size(); index++) statement.setObject(index + 1, playerIds.get(index));
                    try (ResultSet rows = statement.executeQuery()) {
                        while (rows.next()) {
                            UUID playerId = (UUID) rows.getObject(1);
                            Map<String, String> attributes = gson.fromJson(rows.getString(4), ATTRIBUTES_TYPE);
                            if (attributes == null) throw new QuestPersistenceException(
                                "active inventory lease contains invalid attributes");
                            result.put(playerId, new QuestInventorySnapshotLease(playerId, rows.getString(2),
                                (UUID) rows.getObject(3), attributes));
                        }
                    }
                }
                return Collections.unmodifiableMap(result);
            }
        });
    }

    /** Logout is session-fenced so an older server connection cannot remove a newer lease. */
    public int deleteSession(final UUID playerId, final String sourceServer, final UUID sessionId) {
        requireTransaction();
        if (playerId == null || sourceServer == null || sourceServer.trim().isEmpty() || sessionId == null) {
            throw new IllegalArgumentException("player session identity is required");
        }
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Integer>() {
            @Override public Integer execute(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM galaxy_quest_active_inventory_snapshot WHERE player_id=? AND source_server=? "
                        + "AND session_id=?")) {
                    statement.setObject(1, playerId);
                    statement.setString(2, sourceServer.trim());
                    statement.setObject(3, sessionId);
                    return Integer.valueOf(statement.executeUpdate());
                }
            }
        }).intValue();
    }

    public int deleteExpired() {
        requireTransaction();
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<Integer>() {
            @Override public Integer execute(Connection connection) throws SQLException {
                try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM galaxy_quest_active_inventory_snapshot WHERE expires_at <= clock_timestamp()")) {
                    return Integer.valueOf(statement.executeUpdate());
                }
            }
        }).intValue();
    }

    private static void validate(UUID playerId, String sourceServer, UUID sessionId,
        Map<String, String> attributes, long leaseMillis) {
        if (playerId == null || sessionId == null || sourceServer == null || sourceServer.trim().isEmpty()
            || sourceServer.trim().length() > 64 || attributes == null || leaseMillis <= 0L) {
            throw new IllegalArgumentException("valid player session, attributes, and lease duration are required");
        }
        if (attributes.size() > 32768) throw new IllegalArgumentException("inventory attributes exceed the safety limit");
    }

    private void requireTransaction() {
        if (connections.current() == null) throw new QuestPersistenceException(
            "active inventory lease operations require an active quest transaction");
    }
}
