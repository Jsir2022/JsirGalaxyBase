package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.AbstractJdbcRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketDeliveryStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketTradeRecordRepository;

public class JdbcCustomMarketTradeRecordRepository extends AbstractJdbcRepository
    implements CustomMarketTradeRecordRepository {

    public JdbcCustomMarketTradeRecordRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public CustomMarketTradeRecord save(final CustomMarketTradeRecord tradeRecord) {
        return connectionManager.withConnection(new JdbcConnectionCallback<CustomMarketTradeRecord>() {

            @Override
            public CustomMarketTradeRecord doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO custom_market_trade_record (listing_id, seller_player_ref, buyer_player_ref, settled_amount, currency_code, delivery_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    statement.setLong(1, tradeRecord.getListingId());
                    statement.setString(2, tradeRecord.getSellerPlayerRef());
                    statement.setString(3, tradeRecord.getBuyerPlayerRef());
                    statement.setLong(4, tradeRecord.getSettledAmount());
                    statement.setString(5, tradeRecord.getCurrencyCode());
                    statement.setString(6, tradeRecord.getDeliveryStatus().name());
                    statement.setTimestamp(7, java.sql.Timestamp.from(tradeRecord.getCreatedAt()));
                    statement.executeUpdate();
                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    try {
                        if (!generatedKeys.next()) {
                            throw new MarketOperationException(
                                "failed to read generated custom_market_trade_record key");
                        }
                        return new CustomMarketTradeRecord(generatedKeys.getLong(1), tradeRecord.getListingId(),
                            tradeRecord.getSellerPlayerRef(), tradeRecord.getBuyerPlayerRef(),
                            tradeRecord.getSettledAmount(), tradeRecord.getCurrencyCode(),
                            tradeRecord.getDeliveryStatus(), tradeRecord.getCreatedAt());
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
    public CustomMarketTradeRecord update(final CustomMarketTradeRecord tradeRecord) {
        return connectionManager.withConnection(new JdbcConnectionCallback<CustomMarketTradeRecord>() {

            @Override
            public CustomMarketTradeRecord doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "UPDATE custom_market_trade_record SET delivery_status = ? WHERE trade_id = ?");
                try {
                    statement.setString(1, tradeRecord.getDeliveryStatus().name());
                    statement.setLong(2, tradeRecord.getTradeId());
                    if (statement.executeUpdate() != 1) {
                        throw new MarketOperationException(
                            "custom_market_trade_record update failed for tradeId=" + tradeRecord.getTradeId());
                    }
                    return tradeRecord;
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public Optional<CustomMarketTradeRecord> findByListingId(final long listingId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<CustomMarketTradeRecord>>() {

            @Override
            public Optional<CustomMarketTradeRecord> doInConnection(java.sql.Connection connection)
                throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM custom_market_trade_record WHERE listing_id = ? ORDER BY trade_id DESC LIMIT 1");
                try {
                    statement.setLong(1, listingId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapTradeRecord(resultSet))
                            : Optional.<CustomMarketTradeRecord>empty();
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private CustomMarketTradeRecord mapTradeRecord(ResultSet resultSet) throws SQLException {
        return new CustomMarketTradeRecord(resultSet.getLong("trade_id"), resultSet.getLong("listing_id"),
            resultSet.getString("seller_player_ref"), resultSet.getString("buyer_player_ref"),
            resultSet.getLong("settled_amount"), resultSet.getString("currency_code"),
            CustomMarketDeliveryStatus.valueOf(resultSet.getString("delivery_status")),
            readInstant(resultSet, "created_at"));
    }
}