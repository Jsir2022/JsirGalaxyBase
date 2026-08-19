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
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderHistoryPage;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderHistoryQuery;
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
            "SELECT * FROM market_order WHERE product_key = ? AND order_side = 'SELL' AND order_status IN ('OPEN', 'PARTIALLY_FILLED') ORDER BY unit_price ASC, created_at ASC, order_id ASC",
            productKey);
    }

    @Override
    public List<MarketOrder> findOpenBuyOrdersByProductKey(String productKey) {
        return findOrders(
            "SELECT * FROM market_order WHERE product_key = ? AND order_side = 'BUY' AND order_status IN ('OPEN', 'PARTIALLY_FILLED') ORDER BY unit_price DESC, created_at ASC, order_id ASC",
            productKey);
    }

    @Override
    public List<MarketOrder> findOpenOrdersByProductKeys(final List<String> productKeys) {
        if (productKeys == null || productKeys.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        final List<String> keys = new ArrayList<String>();
        for (String key : productKeys) {
            if (key != null && !key.trim().isEmpty() && !keys.contains(key.trim())) keys.add(key.trim());
        }
        if (keys.isEmpty()) return java.util.Collections.emptyList();
        return connectionManager.withConnection(new JdbcConnectionCallback<List<MarketOrder>>() {
            @Override public List<MarketOrder> doInConnection(java.sql.Connection connection) throws SQLException {
                StringBuilder placeholders = new StringBuilder();
                for (int index = 0; index < keys.size(); index++) {
                    if (index > 0) placeholders.append(',');
                    placeholders.append('?');
                }
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_order WHERE product_key IN (" + placeholders
                        + ") AND order_status IN ('OPEN', 'PARTIALLY_FILLED') "
                        + "ORDER BY product_key, order_side, unit_price, created_at, order_id");
                try {
                    for (int index = 0; index < keys.size(); index++) statement.setString(index + 1, keys.get(index));
                    ResultSet resultSet = statement.executeQuery();
                    try { return mapOrders(resultSet); } finally { resultSet.close(); }
                } finally { statement.close(); }
            }
        });
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

    @Override
    public List<MarketOrder> findOrdersByOwnerAndProductKey(final String ownerPlayerRef, final String productKey,
        final int limit) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<MarketOrder>>() {

            @Override
            public List<MarketOrder> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_order WHERE owner_player_ref = ? AND product_key = ? ORDER BY created_at DESC, order_id DESC LIMIT ?");
                try {
                    statement.setString(1, ownerPlayerRef);
                    statement.setString(2, productKey);
                    statement.setInt(3, sanitizeLimit(limit));
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

    @Override
    public List<MarketOrder> findOrdersByOwner(final String ownerPlayerRef, final int limit) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<MarketOrder>>() {

            @Override
            public List<MarketOrder> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_order WHERE owner_player_ref = ? ORDER BY created_at DESC, order_id DESC LIMIT ?");
                try {
                    statement.setString(1, ownerPlayerRef);
                    statement.setInt(2, sanitizeLimit(limit));
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

    @Override
    public MarketOrderHistoryPage findOrderHistory(final String ownerPlayerRef, final MarketOrderHistoryQuery query) {
        return connectionManager.withConnection(new JdbcConnectionCallback<MarketOrderHistoryPage>() {
            @Override public MarketOrderHistoryPage doInConnection(java.sql.Connection connection) throws SQLException {
                String where = historyWhere(query);
                int total;
                PreparedStatement count = connection.prepareStatement("SELECT COUNT(*) FROM market_order " + where);
                try {
                    bindHistory(count, ownerPlayerRef, query, false);
                    ResultSet rows = count.executeQuery();
                    try { rows.next(); total = rows.getInt(1); } finally { rows.close(); }
                } finally { count.close(); }
                int pages = Math.max(1, (total + query.getPageSize() - 1) / query.getPageSize());
                int page = Math.min(query.getPageIndex(), pages - 1);
                PreparedStatement select = connection.prepareStatement("SELECT * FROM market_order " + where
                    + " ORDER BY created_at DESC, order_id DESC LIMIT ? OFFSET ?");
                try {
                    int next = bindHistory(select, ownerPlayerRef, query, false);
                    select.setInt(next++, query.getPageSize());
                    select.setInt(next, page * query.getPageSize());
                    ResultSet rows = select.executeQuery();
                    try { return new MarketOrderHistoryPage(mapOrders(rows), total, page, query.getPageSize()); }
                    finally { rows.close(); }
                } finally { select.close(); }
            }
        });
    }

    private String historyWhere(MarketOrderHistoryQuery query) {
        StringBuilder sql = new StringBuilder("WHERE owner_player_ref = ?");
        if (!query.getProductKey().isEmpty()) sql.append(" AND product_key = ?");
        if (!query.getSearchText().isEmpty()) {
            sql.append(" AND (LOWER(product_key) LIKE ? ESCAPE '!' OR CAST(order_id AS TEXT) LIKE ? ESCAPE '!' OR EXISTS (")
                .append("SELECT 1 FROM standardized_market_catalog catalog ")
                .append("WHERE catalog.product_key = market_order.product_key ")
                .append("AND LOWER(catalog.display_name) LIKE ? ESCAPE '!'))");
        }
        if (query.getSide() != null) sql.append(" AND order_side = ?");
        switch (query.getStatus()) {
            case OPEN: sql.append(" AND order_status IN ('OPEN','PARTIALLY_FILLED')"); break;
            case FILLED: sql.append(" AND order_status = 'FILLED'"); break;
            case CLOSED: sql.append(" AND order_status IN ('CANCELLED','EXCEPTION')"); break;
            case HISTORICAL: sql.append(" AND order_status IN ('FILLED','CANCELLED','EXCEPTION')"); break;
            default: break;
        }
        if (query.getCreatedAfter() != null) sql.append(" AND created_at >= ?");
        return sql.toString();
    }

    private int bindHistory(PreparedStatement statement, String owner, MarketOrderHistoryQuery query,
        boolean ignored) throws SQLException {
        int index = 1;
        statement.setString(index++, owner);
        if (!query.getProductKey().isEmpty()) statement.setString(index++, query.getProductKey());
        if (!query.getSearchText().isEmpty()) {
            String pattern = "%" + escapeLike(query.getSearchText().toLowerCase(java.util.Locale.ROOT)) + "%";
            statement.setString(index++, pattern);
            statement.setString(index++, pattern);
            statement.setString(index++, pattern);
        }
        if (query.getSide() != null) statement.setString(index++, query.getSide().name());
        if (query.getCreatedAfter() != null) statement.setTimestamp(index++, java.sql.Timestamp.from(query.getCreatedAfter()));
        return index;
    }

    static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    @Override
    public List<String> findActiveProductKeys(final int limit) {
        return findProductKeys(
            "SELECT product_key FROM market_order WHERE order_status IN ('OPEN', 'PARTIALLY_FILLED') GROUP BY product_key ORDER BY MAX(updated_at) DESC LIMIT ?",
            sanitizeLimit(limit));
    }

    @Override
    public List<String> findDistinctProductKeysByOwner(final String ownerPlayerRef, final int limit) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<String>>() {

            @Override
            public List<String> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT product_key FROM market_order WHERE owner_player_ref = ? GROUP BY product_key ORDER BY MAX(updated_at) DESC LIMIT ?");
                try {
                    statement.setString(1, ownerPlayerRef);
                    statement.setInt(2, sanitizeLimit(limit));
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return mapProductKeys(resultSet);
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
    public int countActiveOrdersByOwner(final String ownerPlayerRef) {
        return (int) Math.min(Integer.MAX_VALUE, ownerAggregate(ownerPlayerRef, "COUNT(*)"));
    }

    @Override
    public long sumReservedFundsByOwner(final String ownerPlayerRef) {
        return ownerAggregate(ownerPlayerRef, "COALESCE(SUM(reserved_funds), 0)");
    }

    private long ownerAggregate(final String ownerPlayerRef, final String expression) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Long>() {
            @Override public Long doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement("SELECT " + expression
                    + " FROM market_order WHERE owner_player_ref = ? AND order_status IN ('OPEN','PARTIALLY_FILLED')"
                    + " AND open_quantity > 0");
                try {
                    statement.setString(1, ownerPlayerRef);
                    ResultSet rows = statement.executeQuery();
                    try { rows.next(); return Long.valueOf(rows.getLong(1)); } finally { rows.close(); }
                } finally { statement.close(); }
            }
        }).longValue();
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

    private List<String> findProductKeys(final String sql, final int limit) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<String>>() {

            @Override
            public List<String> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(sql);
                try {
                    statement.setInt(1, limit);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return mapProductKeys(resultSet);
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

    private List<String> mapProductKeys(ResultSet resultSet) throws SQLException {
        List<String> productKeys = new ArrayList<String>();
        while (resultSet.next()) {
            productKeys.add(resultSet.getString("product_key"));
        }
        return productKeys;
    }

    private int sanitizeLimit(int limit) {
        return Math.max(1, limit);
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
