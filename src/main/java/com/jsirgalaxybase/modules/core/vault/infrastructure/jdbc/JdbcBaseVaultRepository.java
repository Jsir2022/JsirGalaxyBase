package com.jsirgalaxybase.modules.core.vault.infrastructure.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.AbstractJdbcRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.vault.application.VaultException;
import com.jsirgalaxybase.modules.core.vault.application.VaultItemStackCodec;
import com.jsirgalaxybase.modules.core.vault.domain.VaultAccount;
import com.jsirgalaxybase.modules.core.vault.domain.VaultAccountType;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperation;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperationHistoryPage;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperationSlotChange;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperationStatus;
import com.jsirgalaxybase.modules.core.vault.domain.VaultSlot;
import com.jsirgalaxybase.modules.core.vault.port.BaseVaultRepository;

import net.minecraft.item.ItemStack;

public final class JdbcBaseVaultRepository extends AbstractJdbcRepository implements BaseVaultRepository {

    public JdbcBaseVaultRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    public static void validateSchema(final JdbcConnectionManager connectionManager) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {
            @Override
            public Void doInConnection(Connection connection) throws SQLException {
                String[] tables = { "warehouse_account", "warehouse_slot", "warehouse_operation_log",
                    "warehouse_operation_slot_change" };
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c WHERE c.relkind IN ('r', 'p') "
                        + "AND c.relname = ? AND pg_catalog.pg_table_is_visible(c.oid))");
                try {
                    for (String table : tables) {
                        statement.setString(1, table);
                        ResultSet row = statement.executeQuery();
                        try {
                            if (!row.next() || !row.getBoolean(1)) {
                                throw new VaultException("Base Vault PostgreSQL schema is missing " + table
                                    + "; run scripts/db-migrate.sh before starting the server");
                            }
                        } finally {
                            row.close();
                        }
                    }
                } finally {
                    statement.close();
                }
                return null;
            }
        });
    }

    @Override
    public VaultAccount ensureAccount(final VaultAccountType type, final String ref) {
        return connectionManager.withConnection(new JdbcConnectionCallback<VaultAccount>() {
            @Override
            public VaultAccount doInConnection(Connection connection) throws SQLException {
                PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO warehouse_account (account_type, account_ref, base_slot_count, vault_status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', ?, ?) ON CONFLICT (account_type, account_ref) DO NOTHING");
                try {
                    Instant now = Instant.now();
                    insert.setString(1, type.name());
                    insert.setString(2, ref);
                    insert.setInt(3, type.getDefaultSlotCount());
                    insert.setTimestamp(4, Timestamp.from(now));
                    insert.setTimestamp(5, Timestamp.from(now));
                    insert.executeUpdate();
                } finally {
                    insert.close();
                }
                return findAccount(connection, type, ref, false);
            }
        });
    }

    @Override
    public VaultAccount lockAccount(final VaultAccountType type, final String ref) {
        ensureAccount(type, ref);
        return connectionManager.withConnection(new JdbcConnectionCallback<VaultAccount>() {
            @Override
            public VaultAccount doInConnection(Connection connection) throws SQLException {
                return findAccount(connection, type, ref, true);
            }
        });
    }

    @Override
    public List<VaultSlot> findSlots(final long accountId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<VaultSlot>>() {
            @Override
            public List<VaultSlot> doInConnection(Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT slot_index, stack_nbt, version FROM warehouse_slot WHERE account_id = ? AND stack_nbt IS NOT NULL ORDER BY slot_index ASC");
                try {
                    statement.setLong(1, accountId);
                    ResultSet rows = statement.executeQuery();
                    try {
                        List<VaultSlot> result = new ArrayList<VaultSlot>();
                        while (rows.next()) {
                            String encoded = rows.getString("stack_nbt");
                            ItemStack stack;
                            try {
                                stack = VaultItemStackCodec.decode(encoded);
                            } catch (RuntimeException exception) {
                                throw new VaultException("warehouse_slot contains unreadable item NBT at slot "
                                    + rows.getInt("slot_index"), exception);
                            }
                            result.add(new VaultSlot(rows.getInt("slot_index"), stack, rows.getLong("version")));
                        }
                        return result;
                    } finally {
                        rows.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public void saveSlot(final long accountId, final VaultSlot slot) {
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {
            @Override
            public Void doInConnection(Connection connection) throws SQLException {
                ItemStack stack = slot == null ? null : slot.getStack();
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO warehouse_slot (account_id, slot_index, stack_nbt, item_id, item_meta, stack_size, display_name, version, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT (account_id, slot_index) DO UPDATE SET stack_nbt = EXCLUDED.stack_nbt, item_id = EXCLUDED.item_id, item_meta = EXCLUDED.item_meta, stack_size = EXCLUDED.stack_size, display_name = EXCLUDED.display_name, version = EXCLUDED.version, updated_at = EXCLUDED.updated_at");
                try {
                    statement.setLong(1, accountId);
                    statement.setInt(2, slot.getSlotIndex());
                    if (stack == null || stack.stackSize <= 0) {
                        statement.setNull(3, java.sql.Types.LONGVARCHAR);
                        statement.setNull(4, java.sql.Types.VARCHAR);
                        statement.setNull(5, java.sql.Types.INTEGER);
                        statement.setNull(6, java.sql.Types.INTEGER);
                        statement.setNull(7, java.sql.Types.VARCHAR);
                    } else {
                        statement.setString(3, VaultItemStackCodec.encode(stack));
                        Object id = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem());
                        statement.setString(4, id == null ? stack.getItem().getUnlocalizedName() : String.valueOf(id));
                        statement.setInt(5, stack.getItemDamage());
                        statement.setInt(6, stack.stackSize);
                        statement.setString(7, stack.getDisplayName());
                    }
                    statement.setLong(8, slot.getVersion());
                    statement.setTimestamp(9, Timestamp.from(Instant.now()));
                    statement.executeUpdate();
                    return null;
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public Optional<VaultOperation> findOperationByRequestId(final String requestId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<VaultOperation>>() {
            @Override
            public Optional<VaultOperation> doInConnection(Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM warehouse_operation_log WHERE request_id = ?");
                try {
                    statement.setString(1, requestId);
                    ResultSet rows = statement.executeQuery();
                    try {
                        return rows.next() ? Optional.of(mapOperation(rows)) : Optional.<VaultOperation>empty();
                    } finally {
                        rows.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public VaultOperation saveOperation(final VaultOperation operation) {
        return connectionManager.withConnection(new JdbcConnectionCallback<VaultOperation>() {
            @Override
            public VaultOperation doInConnection(Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO warehouse_operation_log (request_id, account_id, operation_type, source_domain, target_domain, item_snapshot, quantity, operation_status, message, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    bindOperation(statement, operation, false);
                    statement.executeUpdate();
                    ResultSet keys = statement.getGeneratedKeys();
                    try {
                        if (!keys.next()) {
                            throw new VaultException("failed to read warehouse operation key");
                        }
                        return new VaultOperation(keys.getLong(1), operation.getRequestId(), operation.getAccountId(),
                            operation.getOperationType(), operation.getSourceDomain(), operation.getTargetDomain(),
                            operation.getItemSnapshot(), operation.getQuantity(), operation.getStatus(), operation.getMessage(),
                            operation.getCreatedAt(), operation.getUpdatedAt());
                    } finally {
                        keys.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public VaultOperation updateOperation(final VaultOperation operation) {
        return connectionManager.withConnection(new JdbcConnectionCallback<VaultOperation>() {
            @Override
            public VaultOperation doInConnection(Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "UPDATE warehouse_operation_log SET operation_status = ?, message = ?, updated_at = ? WHERE operation_id = ?");
                try {
                    statement.setString(1, operation.getStatus().name());
                    statement.setString(2, operation.getMessage());
                    statement.setTimestamp(3, Timestamp.from(operation.getUpdatedAt()));
                    statement.setLong(4, operation.getOperationId());
                    if (statement.executeUpdate() != 1) {
                        throw new VaultException("warehouse operation update failed: " + operation.getOperationId());
                    }
                    return operation;
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public void saveOperationSlotChanges(final long operationId, final List<VaultOperationSlotChange> changes) {
        if (changes == null || changes.isEmpty()) return;
        connectionManager.withConnection(new JdbcConnectionCallback<Void>() {
            @Override
            public Void doInConnection(Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO warehouse_operation_slot_change (operation_id, slot_index, before_snapshot, after_snapshot, before_version, after_version) VALUES (?, ?, ?, ?, ?, ?)");
                try {
                    for (VaultOperationSlotChange change : changes) {
                        statement.setLong(1, operationId);
                        statement.setInt(2, change.getSlotIndex());
                        bindSnapshot(statement, 3, change.getBefore());
                        bindSnapshot(statement, 4, change.getAfter());
                        statement.setLong(5, change.getBeforeVersion());
                        statement.setLong(6, change.getAfterVersion());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                    return null;
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public VaultOperationHistoryPage findExceptionalOperations(final long accountId, final String searchText,
        final VaultOperationStatus status, final Instant createdAfter, final int pageIndex, final int pageSize) {
        return connectionManager.withConnection(new JdbcConnectionCallback<VaultOperationHistoryPage>() {
            @Override
            public VaultOperationHistoryPage doInConnection(Connection connection) throws SQLException {
                StringBuilder where = new StringBuilder(
                    " WHERE account_id = ? AND operation_status IN ('FAILED', 'RECOVERY_REQUIRED')");
                List<Object> values = new ArrayList<Object>();
                values.add(Long.valueOf(accountId));
                if (status == VaultOperationStatus.FAILED || status == VaultOperationStatus.RECOVERY_REQUIRED) {
                    where.append(" AND operation_status = ?"); values.add(status.name());
                }
                String search = searchText == null ? "" : searchText.trim().toLowerCase(java.util.Locale.ROOT);
                if (!search.isEmpty()) {
                    where.append(" AND (LOWER(operation_type) LIKE ? OR LOWER(message) LIKE ? OR LOWER(request_id) LIKE ?"
                        + " OR CAST(operation_id AS TEXT) LIKE ?)");
                    String pattern = "%" + search + "%";
                    values.add(pattern); values.add(pattern); values.add(pattern); values.add(pattern);
                }
                if (createdAfter != null) { where.append(" AND created_at >= ?"); values.add(Timestamp.from(createdAfter)); }
                int size = Math.max(1, Math.min(50, pageSize));
                PreparedStatement count = connection.prepareStatement(
                    "SELECT COUNT(*) FROM warehouse_operation_log" + where.toString());
                int total;
                try {
                    bindValues(count, values);
                    ResultSet row = count.executeQuery();
                    try { row.next(); total = row.getInt(1); } finally { row.close(); }
                } finally { count.close(); }
                int page = total <= 0 ? 0 : Math.min(Math.max(0, pageIndex), (total - 1) / size);
                PreparedStatement select = connection.prepareStatement(
                    "SELECT * FROM warehouse_operation_log" + where.toString()
                        + " ORDER BY updated_at DESC, operation_id DESC LIMIT ? OFFSET ?");
                try {
                    bindValues(select, values);
                    select.setInt(values.size() + 1, size);
                    select.setInt(values.size() + 2, page * size);
                    ResultSet rows = select.executeQuery();
                    try {
                        List<VaultOperation> operations = new ArrayList<VaultOperation>();
                        while (rows.next()) operations.add(mapOperation(rows));
                        return new VaultOperationHistoryPage(operations, total, page, size);
                    } finally { rows.close(); }
                } finally { select.close(); }
            }
        });
    }

    private static void bindValues(PreparedStatement statement, List<Object> values) throws SQLException {
        for (int index = 0; index < values.size(); index++) {
            Object value = values.get(index);
            if (value instanceof Long) statement.setLong(index + 1, ((Long) value).longValue());
            else if (value instanceof Timestamp) statement.setTimestamp(index + 1, (Timestamp) value);
            else statement.setString(index + 1, String.valueOf(value));
        }
    }

    private static void bindSnapshot(PreparedStatement statement, int parameter, ItemStack stack) throws SQLException {
        if (stack == null || stack.stackSize <= 0) {
            statement.setNull(parameter, java.sql.Types.LONGVARCHAR);
        } else {
            statement.setString(parameter, VaultItemStackCodec.encode(stack));
        }
    }

    private VaultAccount findAccount(Connection connection, VaultAccountType type, String ref, boolean lock)
        throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "SELECT * FROM warehouse_account WHERE account_type = ? AND account_ref = ?" + (lock ? " FOR UPDATE" : ""));
        try {
            statement.setString(1, type.name());
            statement.setString(2, ref);
            ResultSet rows = statement.executeQuery();
            try {
                if (!rows.next()) {
                    throw new VaultException("warehouse account was not created");
                }
                return new VaultAccount(rows.getLong("account_id"), VaultAccountType.valueOf(rows.getString("account_type")),
                    rows.getString("account_ref"), rows.getInt("base_slot_count"), rows.getString("vault_status"),
                    readInstant(rows, "created_at"), readInstant(rows, "updated_at"));
            } finally {
                rows.close();
            }
        } finally {
            statement.close();
        }
    }

    private VaultOperation mapOperation(ResultSet row) throws SQLException {
        return new VaultOperation(row.getLong("operation_id"), row.getString("request_id"), row.getLong("account_id"),
            row.getString("operation_type"), row.getString("source_domain"), row.getString("target_domain"),
            row.getString("item_snapshot"), row.getInt("quantity"),
            VaultOperationStatus.valueOf(row.getString("operation_status")), row.getString("message"),
            readInstant(row, "created_at"), readInstant(row, "updated_at"));
    }

    private void bindOperation(PreparedStatement statement, VaultOperation operation, boolean update) throws SQLException {
        statement.setString(1, operation.getRequestId());
        statement.setLong(2, operation.getAccountId());
        statement.setString(3, operation.getOperationType());
        statement.setString(4, operation.getSourceDomain());
        statement.setString(5, operation.getTargetDomain());
        statement.setString(6, operation.getItemSnapshot());
        statement.setInt(7, operation.getQuantity());
        statement.setString(8, operation.getStatus().name());
        statement.setString(9, operation.getMessage());
        statement.setTimestamp(10, Timestamp.from(operation.getCreatedAt()));
        statement.setTimestamp(11, Timestamp.from(operation.getUpdatedAt()));
    }
}
