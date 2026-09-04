package com.jsirgalaxybase.modules.warehouse.infrastructure.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.vault.application.VaultItemStackCodec;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBay;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBayReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBayResult;
import com.jsirgalaxybase.modules.warehouse.port.TerminalWarehouseBayRepository;

import net.minecraft.item.ItemStack;

/** JDBC persistence for the virtual terminal Bay. All calls run inside the shared transaction runner. */
public final class JdbcTerminalWarehouseBayRepository implements TerminalWarehouseBayRepository {
    private static final int OWNER_LOCK_NAMESPACE = 0x57425459;
    private final JdbcConnectionManager connectionManager;
    public JdbcTerminalWarehouseBayRepository(JdbcConnectionManager connectionManager) { if (connectionManager == null) throw new IllegalArgumentException("connectionManager is required"); this.connectionManager = connectionManager; }

    @Override public void lockOwner(String serverId, String ownerPlayerRef) {
        connectionManager.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?, hashtext(?))")) {
                statement.setInt(1, OWNER_LOCK_NAMESPACE); statement.setString(2, serverId + "|" + ownerPlayerRef); statement.execute(); return null;
            }
        });
    }
    @Override public Optional<TerminalWarehouseBay> find(String serverId, String ownerPlayerRef) {
        return connectionManager.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT source_server_id, owner_player_ref, cell_nbt, version FROM warehouse_terminal_bay WHERE source_server_id=? AND owner_player_ref=?")) {
                statement.setString(1, serverId); statement.setString(2, ownerPlayerRef);
                try (ResultSet rows = statement.executeQuery()) { return rows.next() ? Optional.of(mapBay(rows)) : Optional.<TerminalWarehouseBay>empty(); }
            }
        });
    }
    @Override public void save(TerminalWarehouseBay bay) {
        connectionManager.withConnection(connection -> {
            String sql = "INSERT INTO warehouse_terminal_bay (source_server_id, owner_player_ref, cell_nbt, version) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (source_server_id, owner_player_ref) DO UPDATE SET cell_nbt=EXCLUDED.cell_nbt, version=EXCLUDED.version, updated_at=CURRENT_TIMESTAMP";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, bay.getServerId()); statement.setString(2, bay.getOwnerPlayerRef()); bindCell(statement, 3, bay.getCell()); statement.setLong(4, bay.getVersion()); statement.executeUpdate(); return null;
            }
        });
    }
    @Override public Optional<TerminalWarehouseBayReceipt> findReceipt(String requestId) {
        return connectionManager.withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM warehouse_terminal_bay_operation WHERE request_id=?")) {
                statement.setString(1, requestId); try (ResultSet rows = statement.executeQuery()) { return rows.next() ? Optional.of(mapReceipt(rows)) : Optional.<TerminalWarehouseBayReceipt>empty(); }
            }
        });
    }
    @Override public void saveReceipt(TerminalWarehouseBayReceipt receipt, String actorPlayerRef) {
        connectionManager.withConnection(connection -> {
            TerminalWarehouseBay identity = receipt.getAfter() == null ? receipt.getBefore() : receipt.getAfter();
            String sql = "INSERT INTO warehouse_terminal_bay_operation (request_id, semantics_key, operation_type, result_code, operator_player_ref, source_server_id, before_cell_nbt, before_version, after_cell_nbt, after_version, detail) VALUES (?, ?, 'CONTAINER_MUTATION', ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, receipt.getRequestId()); statement.setString(2, receipt.getSemanticsKey()); statement.setString(3, receipt.getResult().name()); statement.setString(4, actorPlayerRef);
                statement.setString(5, identity.getServerId()); bindCell(statement, 6, receipt.getBefore() == null ? null : receipt.getBefore().getCell()); statement.setLong(7, receipt.getBefore() == null ? 0L : receipt.getBefore().getVersion());
                bindCell(statement, 8, receipt.getAfter() == null ? null : receipt.getAfter().getCell()); statement.setLong(9, receipt.getAfter() == null ? 0L : receipt.getAfter().getVersion()); statement.setString(10, receipt.getDetail()); statement.executeUpdate(); return null;
            }
        });
    }
    @Override public List<TerminalWarehouseBayReceipt> findRecent(String serverId, String ownerPlayerRef, int limit) {
        return connectionManager.withConnection(connection -> {
            String sql = "SELECT * FROM warehouse_terminal_bay_operation WHERE source_server_id=? AND operator_player_ref=? ORDER BY operation_id DESC LIMIT ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, serverId); statement.setString(2, ownerPlayerRef); statement.setInt(3, limit); List<TerminalWarehouseBayReceipt> result = new ArrayList<TerminalWarehouseBayReceipt>();
                try (ResultSet rows = statement.executeQuery()) { while (rows.next()) result.add(mapReceipt(rows)); } return result;
            }
        });
    }
    private static TerminalWarehouseBay mapBay(ResultSet rows) throws SQLException { return new TerminalWarehouseBay(rows.getString("source_server_id"), rows.getString("owner_player_ref"), decodeCell(rows.getString("cell_nbt")), rows.getLong("version")); }
    private static TerminalWarehouseBayReceipt mapReceipt(ResultSet rows) throws SQLException {
        String server = rows.getString("source_server_id"); String owner = rows.getString("operator_player_ref");
        TerminalWarehouseBay before = new TerminalWarehouseBay(server, owner, decodeCell(rows.getString("before_cell_nbt")), rows.getLong("before_version"));
        TerminalWarehouseBay after = new TerminalWarehouseBay(server, owner, decodeCell(rows.getString("after_cell_nbt")), rows.getLong("after_version"));
        return new TerminalWarehouseBayReceipt(rows.getString("request_id"), rows.getString("semantics_key"), TerminalWarehouseBayResult.valueOf(rows.getString("result_code")), before, after, rows.getString("detail"));
    }
    private static void bindCell(PreparedStatement statement, int index, ItemStack cell) throws SQLException { if (cell == null || cell.stackSize <= 0) statement.setNull(index, java.sql.Types.LONGVARCHAR); else statement.setString(index, VaultItemStackCodec.encode(cell)); }
    private static ItemStack decodeCell(String encoded) { return encoded == null || encoded.trim().isEmpty() ? null : VaultItemStackCodec.decode(encoded); }
}
