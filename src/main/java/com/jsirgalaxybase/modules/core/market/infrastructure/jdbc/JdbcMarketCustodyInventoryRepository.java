package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.AbstractJdbcRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyStatus;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;

public class JdbcMarketCustodyInventoryRepository extends AbstractJdbcRepository
    implements MarketCustodyInventoryRepository {

    public JdbcMarketCustodyInventoryRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public MarketCustodyInventory save(final MarketCustodyInventory custodyInventory) {
        return connectionManager.withConnection(new JdbcConnectionCallback<MarketCustodyInventory>() {

            @Override
            public MarketCustodyInventory doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO market_custody_inventory (owner_player_ref, product_key, registry_name, meta, stackable, quantity, custody_status, related_order_id, related_operation_id, source_server_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    bindCustody(statement, custodyInventory);
                    statement.executeUpdate();
                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    try {
                        if (!generatedKeys.next()) {
                            throw new MarketOperationException("failed to read generated market_custody_inventory key");
                        }
                        return new MarketCustodyInventory(generatedKeys.getLong(1), custodyInventory.getOwnerPlayerRef(),
                            custodyInventory.getProduct(), custodyInventory.isStackable(), custodyInventory.getQuantity(),
                            custodyInventory.getStatus(), custodyInventory.getRelatedOrderId(),
                            custodyInventory.getRelatedOperationId(), custodyInventory.getSourceServerId(),
                            custodyInventory.getCreatedAt(), custodyInventory.getUpdatedAt());
                    } finally {
                        generatedKeys.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public MarketCustodyInventory update(final MarketCustodyInventory custodyInventory) {
        return connectionManager.withConnection(new JdbcConnectionCallback<MarketCustodyInventory>() {

            @Override
            public MarketCustodyInventory doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "UPDATE market_custody_inventory SET quantity = ?, custody_status = ?, related_order_id = ?, related_operation_id = ?, updated_at = ? WHERE custody_id = ?");
                try {
                    statement.setLong(1, custodyInventory.getQuantity());
                    statement.setString(2, custodyInventory.getStatus().name());
                    if (custodyInventory.getRelatedOrderId() > 0L) {
                        statement.setLong(3, custodyInventory.getRelatedOrderId());
                    } else {
                        statement.setNull(3, java.sql.Types.BIGINT);
                    }
                    if (custodyInventory.getRelatedOperationId() > 0L) {
                        statement.setLong(4, custodyInventory.getRelatedOperationId());
                    } else {
                        statement.setNull(4, java.sql.Types.BIGINT);
                    }
                    statement.setTimestamp(5, java.sql.Timestamp.from(custodyInventory.getUpdatedAt()));
                    statement.setLong(6, custodyInventory.getCustodyId());
                    if (statement.executeUpdate() != 1) {
                        throw new MarketOperationException(
                            "market_custody_inventory update failed for custodyId=" + custodyInventory.getCustodyId());
                    }
                    return custodyInventory;
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public Optional<MarketCustodyInventory> findById(final long custodyId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<MarketCustodyInventory>>() {

            @Override
            public Optional<MarketCustodyInventory> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_custody_inventory WHERE custody_id = ?");
                try {
                    statement.setLong(1, custodyId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapCustody(resultSet))
                            : Optional.<MarketCustodyInventory>empty();
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public MarketCustodyInventory lockById(final long custodyId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<MarketCustodyInventory>() {

            @Override
            public MarketCustodyInventory doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_custody_inventory WHERE custody_id = ? FOR UPDATE");
                try {
                    statement.setLong(1, custodyId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        if (!resultSet.next()) {
                            throw new MarketOperationException("market custody not found: " + custodyId);
                        }
                        return mapCustody(resultSet);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public Optional<MarketCustodyInventory> findEscrowSellByOrderId(final long orderId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<MarketCustodyInventory>>() {

            @Override
            public Optional<MarketCustodyInventory> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_custody_inventory WHERE related_order_id = ? AND custody_status = 'ESCROW_SELL' ORDER BY custody_id ASC LIMIT 1");
                try {
                    statement.setLong(1, orderId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapCustody(resultSet))
                            : Optional.<MarketCustodyInventory>empty();
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public List<MarketCustodyInventory> findByOwnerAndStatus(final String ownerPlayerRef, final MarketCustodyStatus status) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<MarketCustodyInventory>>() {

            @Override
            public List<MarketCustodyInventory> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_custody_inventory WHERE owner_player_ref = ? AND custody_status = ? ORDER BY created_at ASC, custody_id ASC");
                try {
                    statement.setString(1, ownerPlayerRef);
                    statement.setString(2, status.name());
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        List<MarketCustodyInventory> results = new ArrayList<MarketCustodyInventory>();
                        while (resultSet.next()) {
                            results.add(mapCustody(resultSet));
                        }
                        return results;
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private void bindCustody(PreparedStatement statement, MarketCustodyInventory custodyInventory) throws SQLException {
        statement.setString(1, custodyInventory.getOwnerPlayerRef());
        statement.setString(2, custodyInventory.getProduct().getProductKey());
        statement.setString(3, custodyInventory.getProduct().getRegistryName());
        statement.setInt(4, custodyInventory.getProduct().getMeta());
        statement.setBoolean(5, custodyInventory.isStackable());
        statement.setLong(6, custodyInventory.getQuantity());
        statement.setString(7, custodyInventory.getStatus().name());
        if (custodyInventory.getRelatedOrderId() > 0L) {
            statement.setLong(8, custodyInventory.getRelatedOrderId());
        } else {
            statement.setNull(8, java.sql.Types.BIGINT);
        }
        if (custodyInventory.getRelatedOperationId() > 0L) {
            statement.setLong(9, custodyInventory.getRelatedOperationId());
        } else {
            statement.setNull(9, java.sql.Types.BIGINT);
        }
        statement.setString(10, custodyInventory.getSourceServerId());
        statement.setTimestamp(11, java.sql.Timestamp.from(custodyInventory.getCreatedAt()));
        statement.setTimestamp(12, java.sql.Timestamp.from(custodyInventory.getUpdatedAt()));
    }

    private MarketCustodyInventory mapCustody(ResultSet resultSet) throws SQLException {
        return new MarketCustodyInventory(resultSet.getLong("custody_id"), resultSet.getString("owner_player_ref"),
            new StandardizedMarketProduct(resultSet.getString("registry_name"), resultSet.getInt("meta")),
            resultSet.getBoolean("stackable"), resultSet.getLong("quantity"),
            MarketCustodyStatus.valueOf(resultSet.getString("custody_status")), resultSet.getLong("related_order_id"),
            resultSet.getLong("related_operation_id"), resultSet.getString("source_server_id"),
            readInstant(resultSet, "created_at"), readInstant(resultSet, "updated_at"));
    }
}