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
import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderSide;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderStatus;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;

public class JdbcMarketOrderBookRepository extends AbstractJdbcRepository implements MarketOrderBookRepository {

    public JdbcMarketOrderBookRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public MarketOrder save(final MarketOrder order) {
        return connectionManager.withConnection(new JdbcConnectionCallback<MarketOrder>() {

            @Override
            public MarketOrder doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO market_order (order_side, order_status, owner_player_ref, product_key, registry_name, meta, stackable, unit_price, original_quantity, open_quantity, filled_quantity, reserved_funds, custody_id, source_server_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    bindOrder(statement, order);
                    statement.executeUpdate();
                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    try {
                        if (!generatedKeys.next()) {
                            throw new MarketOperationException("failed to read generated market_order key");
                        }
                        return new MarketOrder(generatedKeys.getLong(1), order.getSide(), order.getStatus(),
                            order.getOwnerPlayerRef(), order.getProduct(), order.isStackable(), order.getUnitPrice(),
                            order.getOriginalQuantity(), order.getOpenQuantity(), order.getFilledQuantity(),
                            order.getReservedFunds(), order.getCustodyId(), order.getSourceServerId(),
                            order.getCreatedAt(), order.getUpdatedAt());
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
    public MarketOrder update(final MarketOrder order) {
        return connectionManager.withConnection(new JdbcConnectionCallback<MarketOrder>() {

            @Override
            public MarketOrder doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "UPDATE market_order SET order_status = ?, open_quantity = ?, filled_quantity = ?, reserved_funds = ?, updated_at = ? WHERE order_id = ?");
                try {
                    statement.setString(1, order.getStatus().name());
                    statement.setLong(2, order.getOpenQuantity());
                    statement.setLong(3, order.getFilledQuantity());
                    statement.setLong(4, order.getReservedFunds());
                    statement.setTimestamp(5, java.sql.Timestamp.from(order.getUpdatedAt()));
                    statement.setLong(6, order.getOrderId());
                    if (statement.executeUpdate() != 1) {
                        throw new MarketOperationException("market_order update failed for orderId=" + order.getOrderId());
                    }
                    return order;
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public Optional<MarketOrder> findById(final long orderId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<MarketOrder>>() {

            @Override
            public Optional<MarketOrder> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM market_order WHERE order_id = ?");
                try {
                    statement.setLong(1, orderId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapOrder(resultSet)) : Optional.<MarketOrder>empty();
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
    public MarketOrder lockById(final long orderId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<MarketOrder>() {

            @Override
            public MarketOrder doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_order WHERE order_id = ? FOR UPDATE");
                try {
                    statement.setLong(1, orderId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        if (!resultSet.next()) {
                            throw new MarketOperationException("market order not found: " + orderId);
                        }
                        return mapOrder(resultSet);
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
    public List<MarketOrder> findOpenSellOrdersByProductKey(String productKey) {
        return findOrders(
            "SELECT * FROM market_order WHERE product_key = ? AND order_side = 'SELL' AND order_status = 'OPEN' ORDER BY unit_price ASC, created_at ASC, order_id ASC",
            productKey);
    }

    @Override
    public List<MarketOrder> findOpenBuyOrdersByProductKey(String productKey) {
        return findOrders(
            "SELECT * FROM market_order WHERE product_key = ? AND order_side = 'BUY' AND order_status = 'OPEN' ORDER BY unit_price DESC, created_at ASC, order_id ASC",
            productKey);
    }

    @Override
    public List<MarketOrder> findMatchingSellOrders(final String productKey, final long maxUnitPrice) {
        return findOrdersForMatch(
            "SELECT * FROM market_order WHERE product_key = ? AND order_side = 'SELL' AND order_status IN ('OPEN', 'PARTIALLY_FILLED') AND unit_price <= ? ORDER BY unit_price ASC, created_at ASC, order_id ASC FOR UPDATE",
            productKey, maxUnitPrice);
    }

    @Override
    public List<MarketOrder> findMatchingBuyOrders(final String productKey, final long minUnitPrice) {
        return findOrdersForMatch(
            "SELECT * FROM market_order WHERE product_key = ? AND order_side = 'BUY' AND order_status IN ('OPEN', 'PARTIALLY_FILLED') AND unit_price >= ? ORDER BY unit_price DESC, created_at ASC, order_id ASC FOR UPDATE",
            productKey, minUnitPrice);
    }

    private void bindOrder(PreparedStatement statement, MarketOrder order) throws SQLException {
        statement.setString(1, order.getSide().name());
        statement.setString(2, order.getStatus().name());
        statement.setString(3, order.getOwnerPlayerRef());
        statement.setString(4, order.getProduct().getProductKey());
        statement.setString(5, order.getProduct().getRegistryName());
        statement.setInt(6, order.getProduct().getMeta());
        statement.setBoolean(7, order.isStackable());
        statement.setLong(8, order.getUnitPrice());
        statement.setLong(9, order.getOriginalQuantity());
        statement.setLong(10, order.getOpenQuantity());
        statement.setLong(11, order.getFilledQuantity());
        statement.setLong(12, order.getReservedFunds());
        if (order.getCustodyId() > 0L) {
            statement.setLong(13, order.getCustodyId());
        } else {
            statement.setNull(13, java.sql.Types.BIGINT);
        }
        statement.setString(14, order.getSourceServerId());
        statement.setTimestamp(15, java.sql.Timestamp.from(order.getCreatedAt()));
        statement.setTimestamp(16, java.sql.Timestamp.from(order.getUpdatedAt()));
    }

    private List<MarketOrder> findOrders(final String sql, final String productKey) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<MarketOrder>>() {

            @Override
            public List<MarketOrder> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(sql);
                try {
                    statement.setString(1, productKey);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return mapOrders(resultSet);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private List<MarketOrder> findOrdersForMatch(final String sql, final String productKey, final long price) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<MarketOrder>>() {

            @Override
            public List<MarketOrder> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(sql);
                try {
                    statement.setString(1, productKey);
                    statement.setLong(2, price);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return mapOrders(resultSet);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private List<MarketOrder> mapOrders(ResultSet resultSet) throws SQLException {
        List<MarketOrder> orders = new ArrayList<MarketOrder>();
        while (resultSet.next()) {
            orders.add(mapOrder(resultSet));
        }
        return orders;
    }

    private MarketOrder mapOrder(ResultSet resultSet) throws SQLException {
        return new MarketOrder(resultSet.getLong("order_id"),
            MarketOrderSide.valueOf(resultSet.getString("order_side")),
            MarketOrderStatus.valueOf(resultSet.getString("order_status")), resultSet.getString("owner_player_ref"),
            new StandardizedMarketProduct(resultSet.getString("registry_name"), resultSet.getInt("meta")),
            resultSet.getBoolean("stackable"), resultSet.getLong("unit_price"),
            resultSet.getLong("original_quantity"), resultSet.getLong("open_quantity"),
            resultSet.getLong("filled_quantity"), resultSet.getLong("reserved_funds"), resultSet.getLong("custody_id"),
            resultSet.getString("source_server_id"), readInstant(resultSet, "created_at"),
            readInstant(resultSet, "updated_at"));
    }
}