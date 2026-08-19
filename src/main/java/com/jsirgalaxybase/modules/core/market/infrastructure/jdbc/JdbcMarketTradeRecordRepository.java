package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

import java.time.Instant;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.AbstractJdbcRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeHistoryPage;
import com.jsirgalaxybase.modules.core.market.domain.MarketAccountCenterQuery;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderSide;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;

public class JdbcMarketTradeRecordRepository extends AbstractJdbcRepository implements MarketTradeRecordRepository {

    public JdbcMarketTradeRecordRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public MarketTradeRecord save(final MarketTradeRecord tradeRecord) {
        return connectionManager.withConnection(new JdbcConnectionCallback<MarketTradeRecord>() {

            @Override
            public MarketTradeRecord doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO market_trade_record (buyer_player_ref, seller_player_ref, product_key, registry_name, meta, stackable, unit_price, quantity, fee_amount, buy_order_id, sell_order_id, operation_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    statement.setString(1, tradeRecord.getBuyerPlayerRef());
                    statement.setString(2, tradeRecord.getSellerPlayerRef());
                    statement.setString(3, tradeRecord.getProduct().getProductKey());
                    statement.setString(4, tradeRecord.getProduct().getRegistryName());
                    statement.setInt(5, tradeRecord.getProduct().getMeta());
                    statement.setBoolean(6, tradeRecord.isStackable());
                    statement.setLong(7, tradeRecord.getUnitPrice());
                    statement.setLong(8, tradeRecord.getQuantity());
                    statement.setLong(9, tradeRecord.getFeeAmount());
                    if (tradeRecord.getBuyOrderId() > 0L) {
                        statement.setLong(10, tradeRecord.getBuyOrderId());
                    } else {
                        statement.setNull(10, java.sql.Types.BIGINT);
                    }
                    if (tradeRecord.getSellOrderId() > 0L) {
                        statement.setLong(11, tradeRecord.getSellOrderId());
                    } else {
                        statement.setNull(11, java.sql.Types.BIGINT);
                    }
                    if (tradeRecord.getOperationId() > 0L) {
                        statement.setLong(12, tradeRecord.getOperationId());
                    } else {
                        statement.setNull(12, java.sql.Types.BIGINT);
                    }
                    statement.setTimestamp(13, java.sql.Timestamp.from(tradeRecord.getCreatedAt()));
                    statement.executeUpdate();
                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    try {
                        if (!generatedKeys.next()) {
                            throw new MarketOperationException("failed to read generated market_trade_record key");
                        }
                        return new MarketTradeRecord(generatedKeys.getLong(1), tradeRecord.getBuyerPlayerRef(),
                            tradeRecord.getSellerPlayerRef(), tradeRecord.getProduct(), tradeRecord.isStackable(),
                            tradeRecord.getUnitPrice(), tradeRecord.getQuantity(), tradeRecord.getFeeAmount(),
                            tradeRecord.getBuyOrderId(), tradeRecord.getSellOrderId(), tradeRecord.getOperationId(),
                            tradeRecord.getCreatedAt());
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
    public List<MarketTradeRecord> findByOrderId(final long orderId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<MarketTradeRecord>>() {

            @Override
            public List<MarketTradeRecord> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_trade_record WHERE buy_order_id = ? OR sell_order_id = ? ORDER BY created_at ASC, trade_id ASC");
                try {
                    statement.setLong(1, orderId);
                    statement.setLong(2, orderId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        List<MarketTradeRecord> trades = new ArrayList<MarketTradeRecord>();
                        while (resultSet.next()) {
                            trades.add(new MarketTradeRecord(resultSet.getLong("trade_id"),
                                resultSet.getString("buyer_player_ref"), resultSet.getString("seller_player_ref"),
                                new StandardizedMarketProduct(resultSet.getString("registry_name"),
                                    resultSet.getInt("meta")),
                                resultSet.getBoolean("stackable"), resultSet.getLong("unit_price"),
                                resultSet.getLong("quantity"), resultSet.getLong("fee_amount"),
                                resultSet.getLong("buy_order_id"), resultSet.getLong("sell_order_id"),
                                resultSet.getLong("operation_id"), readInstant(resultSet, "created_at")));
                        }
                        return trades;
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
    public List<MarketTradeRecord> findByProductKey(final String productKey, final int limit) {
        return findTrades(
            "SELECT * FROM market_trade_record WHERE product_key = ? ORDER BY created_at DESC, trade_id DESC LIMIT ?",
            productKey, null, sanitizeLimit(limit));
    }

    @Override
    public List<MarketTradeRecord> findByProductKeySince(final String productKey, final Instant since, final int limit) {
        return findTrades(
            "SELECT * FROM market_trade_record WHERE product_key = ? AND created_at >= ? ORDER BY created_at DESC, trade_id DESC LIMIT ?",
            productKey, since, sanitizeLimit(limit));
    }

    @Override
    public MarketTradeRecord findLatestByProductKeyBefore(final String productKey, final Instant before) {
        List<MarketTradeRecord> records = findTrades(
            "SELECT * FROM market_trade_record WHERE product_key = ? AND created_at < ? ORDER BY created_at DESC, trade_id DESC LIMIT ?",
            productKey, before, 1);
        return records.isEmpty() ? null : records.get(0);
    }

    @Override
    public List<MarketTradeRecord> findByProductKeysSince(final List<String> productKeys, final Instant since,
        final int limit) {
        if (productKeys == null || productKeys.isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> keys = new ArrayList<String>();
        for (String key : productKeys) {
            if (key != null && !key.trim().isEmpty() && !keys.contains(key.trim())) {
                keys.add(key.trim());
            }
        }
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        return connectionManager.withConnection(new JdbcConnectionCallback<List<MarketTradeRecord>>() {
            @Override
            public List<MarketTradeRecord> doInConnection(java.sql.Connection connection) throws SQLException {
                StringBuilder placeholders = new StringBuilder();
                for (int index = 0; index < keys.size(); index++) {
                    if (index > 0) { placeholders.append(','); }
                    placeholders.append('?');
                }
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_trade_record WHERE product_key IN (" + placeholders
                        + ") AND created_at >= ? ORDER BY created_at DESC, trade_id DESC LIMIT ?");
                try {
                    int parameter = 1;
                    for (String key : keys) { statement.setString(parameter++, key); }
                    statement.setTimestamp(parameter++, java.sql.Timestamp.from(since == null ? Instant.EPOCH : since));
                    statement.setInt(parameter, sanitizeLimit(limit));
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        List<MarketTradeRecord> trades = new ArrayList<MarketTradeRecord>();
                        while (resultSet.next()) { trades.add(mapTrade(resultSet)); }
                        return trades;
                    } finally { resultSet.close(); }
                } finally { statement.close(); }
            }
        });
    }

    @Override
    public List<String> findDistinctProductKeys(final int limit) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<String>>() {

            @Override
            public List<String> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT product_key FROM market_trade_record GROUP BY product_key ORDER BY MAX(created_at) DESC LIMIT ?");
                try {
                    statement.setInt(1, sanitizeLimit(limit));
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        List<String> productKeys = new ArrayList<String>();
                        while (resultSet.next()) {
                            productKeys.add(resultSet.getString("product_key"));
                        }
                        return productKeys;
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
    public MarketTradeHistoryPage findPersonalTradeHistory(final String playerRef,
        final MarketAccountCenterQuery query) {
        return connectionManager.withConnection(new JdbcConnectionCallback<MarketTradeHistoryPage>() {
            @Override public MarketTradeHistoryPage doInConnection(java.sql.Connection connection) throws SQLException {
                String where = personalTradeWhere(query);
                PreparedStatement count = connection.prepareStatement(
                    "SELECT COUNT(*) FROM market_trade_record WHERE " + where);
                int total;
                try {
                    bindPersonalTrade(count, playerRef, query, false);
                    ResultSet rows = count.executeQuery();
                    try { rows.next(); total = rows.getInt(1); } finally { rows.close(); }
                } finally { count.close(); }
                int page = total <= 0 ? 0 : Math.min(query.getPageIndex(), (total - 1) / query.getPageSize());
                PreparedStatement select = connection.prepareStatement(
                    "SELECT * FROM market_trade_record WHERE " + where
                        + " ORDER BY created_at DESC, trade_id DESC LIMIT ? OFFSET ?");
                try {
                    int parameter = bindPersonalTrade(select, playerRef, query, true);
                    select.setInt(parameter++, query.getPageSize());
                    select.setInt(parameter, page * query.getPageSize());
                    ResultSet rows = select.executeQuery();
                    try {
                        List<MarketTradeRecord> trades = new ArrayList<MarketTradeRecord>();
                        while (rows.next()) trades.add(mapTrade(rows));
                        return new MarketTradeHistoryPage(trades, total, page, query.getPageSize());
                    } finally { rows.close(); }
                } finally { select.close(); }
            }
        });
    }

    private String personalTradeWhere(MarketAccountCenterQuery query) {
        StringBuilder where = new StringBuilder();
        if (query.getSide() == MarketOrderSide.BUY) where.append("buyer_player_ref = ?");
        else if (query.getSide() == MarketOrderSide.SELL) where.append("seller_player_ref = ?");
        else where.append("(buyer_player_ref = ? OR seller_player_ref = ?)");
        if (!query.getProductKey().isEmpty()) where.append(" AND product_key = ?");
        if (!query.getSearchText().isEmpty()) {
            where.append(" AND (product_key ILIKE ? ESCAPE '!' OR CAST(trade_id AS TEXT) LIKE ? ESCAPE '!'"
                + " OR CAST(buy_order_id AS TEXT) LIKE ? ESCAPE '!' OR CAST(sell_order_id AS TEXT) LIKE ? ESCAPE '!')");
        }
        if (query.getCreatedAfter() != null) where.append(" AND created_at >= ?");
        return where.toString();
    }

    private int bindPersonalTrade(PreparedStatement statement, String playerRef, MarketAccountCenterQuery query,
        boolean select) throws SQLException {
        int parameter = 1;
        statement.setString(parameter++, playerRef);
        if (query.getSide() == null) statement.setString(parameter++, playerRef);
        if (!query.getProductKey().isEmpty()) statement.setString(parameter++, query.getProductKey());
        if (!query.getSearchText().isEmpty()) {
            String pattern = "%" + escapeLike(query.getSearchText()) + "%";
            statement.setString(parameter++, pattern); statement.setString(parameter++, pattern);
            statement.setString(parameter++, pattern); statement.setString(parameter++, pattern);
        }
        if (query.getCreatedAfter() != null) statement.setTimestamp(parameter++, java.sql.Timestamp.from(query.getCreatedAfter()));
        return parameter;
    }

    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private List<MarketTradeRecord> findTrades(final String sql, final String productKey, final Instant since,
        final int limit) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<MarketTradeRecord>>() {

            @Override
            public List<MarketTradeRecord> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(sql);
                try {
                    statement.setString(1, productKey);
                    if (since == null) {
                        statement.setInt(2, limit);
                    } else {
                        statement.setTimestamp(2, java.sql.Timestamp.from(since));
                        statement.setInt(3, limit);
                    }
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        List<MarketTradeRecord> trades = new ArrayList<MarketTradeRecord>();
                        while (resultSet.next()) {
                            trades.add(mapTrade(resultSet));
                        }
                        return trades;
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private MarketTradeRecord mapTrade(ResultSet resultSet) throws SQLException {
        return new MarketTradeRecord(resultSet.getLong("trade_id"), resultSet.getString("buyer_player_ref"),
            resultSet.getString("seller_player_ref"),
            new StandardizedMarketProduct(resultSet.getString("registry_name"), resultSet.getInt("meta")),
            resultSet.getBoolean("stackable"), resultSet.getLong("unit_price"), resultSet.getLong("quantity"),
            resultSet.getLong("fee_amount"), resultSet.getLong("buy_order_id"), resultSet.getLong("sell_order_id"),
            resultSet.getLong("operation_id"), readInstant(resultSet, "created_at"));
    }

    private int sanitizeLimit(int limit) {
        return Math.max(1, limit);
    }
}
