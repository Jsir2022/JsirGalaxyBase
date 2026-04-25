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
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketDeliveryStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListing;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListingStatus;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketListingRepository;

public class JdbcCustomMarketListingRepository extends AbstractJdbcRepository implements CustomMarketListingRepository {

    public JdbcCustomMarketListingRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public CustomMarketListing save(final CustomMarketListing listing) {
        return connectionManager.withConnection(new JdbcConnectionCallback<CustomMarketListing>() {

            @Override
            public CustomMarketListing doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO custom_market_listing (seller_player_ref, buyer_player_ref, asking_price, currency_code, listing_status, delivery_status, source_server_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    bindListing(statement, listing);
                    statement.executeUpdate();
                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    try {
                        if (!generatedKeys.next()) {
                            throw new MarketOperationException("failed to read generated custom_market_listing key");
                        }
                        return new CustomMarketListing(generatedKeys.getLong(1), listing.getSellerPlayerRef(),
                            listing.getBuyerPlayerRef(), listing.getAskingPrice(), listing.getCurrencyCode(),
                            listing.getListingStatus(), listing.getDeliveryStatus(), listing.getSourceServerId(),
                            listing.getCreatedAt(), listing.getUpdatedAt());
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
    public CustomMarketListing update(final CustomMarketListing listing) {
        return connectionManager.withConnection(new JdbcConnectionCallback<CustomMarketListing>() {

            @Override
            public CustomMarketListing doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "UPDATE custom_market_listing SET buyer_player_ref = ?, listing_status = ?, delivery_status = ?, updated_at = ? WHERE listing_id = ?");
                try {
                    setNullableText(statement, 1, listing.getBuyerPlayerRef());
                    statement.setString(2, listing.getListingStatus().name());
                    statement.setString(3, listing.getDeliveryStatus().name());
                    statement.setTimestamp(4, java.sql.Timestamp.from(listing.getUpdatedAt()));
                    statement.setLong(5, listing.getListingId());
                    if (statement.executeUpdate() != 1) {
                        throw new MarketOperationException(
                            "custom_market_listing update failed for listingId=" + listing.getListingId());
                    }
                    return listing;
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public Optional<CustomMarketListing> findById(final long listingId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<CustomMarketListing>>() {

            @Override
            public Optional<CustomMarketListing> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM custom_market_listing WHERE listing_id = ?");
                try {
                    statement.setLong(1, listingId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapListing(resultSet))
                            : Optional.<CustomMarketListing>empty();
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
    public CustomMarketListing lockById(final long listingId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<CustomMarketListing>() {

            @Override
            public CustomMarketListing doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM custom_market_listing WHERE listing_id = ? FOR UPDATE");
                try {
                    statement.setLong(1, listingId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        if (!resultSet.next()) {
                            throw new MarketOperationException("custom market listing not found: " + listingId);
                        }
                        return mapListing(resultSet);
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
    public List<CustomMarketListing> findByStatus(final CustomMarketListingStatus status, final int limit) {
        return findListings(
            "SELECT * FROM custom_market_listing WHERE listing_status = ? ORDER BY created_at DESC, listing_id DESC LIMIT ?",
            status.name(), sanitizeLimit(limit));
    }

    @Override
    public List<CustomMarketListing> findBySellerAndDeliveryStatus(final String sellerPlayerRef,
        final CustomMarketDeliveryStatus deliveryStatus, final int limit) {
        return findListingsByPlayer(
            "SELECT * FROM custom_market_listing WHERE seller_player_ref = ? AND delivery_status = ? ORDER BY updated_at DESC, listing_id DESC LIMIT ?",
            sellerPlayerRef, deliveryStatus.name(), sanitizeLimit(limit));
    }

    @Override
    public List<CustomMarketListing> findByBuyerAndDeliveryStatus(final String buyerPlayerRef,
        final CustomMarketDeliveryStatus deliveryStatus, final int limit) {
        return findListingsByPlayer(
            "SELECT * FROM custom_market_listing WHERE buyer_player_ref = ? AND delivery_status = ? ORDER BY updated_at DESC, listing_id DESC LIMIT ?",
            buyerPlayerRef, deliveryStatus.name(), sanitizeLimit(limit));
    }

    private void bindListing(PreparedStatement statement, CustomMarketListing listing) throws SQLException {
        statement.setString(1, listing.getSellerPlayerRef());
        setNullableText(statement, 2, listing.getBuyerPlayerRef());
        statement.setLong(3, listing.getAskingPrice());
        statement.setString(4, listing.getCurrencyCode());
        statement.setString(5, listing.getListingStatus().name());
        statement.setString(6, listing.getDeliveryStatus().name());
        statement.setString(7, listing.getSourceServerId());
        statement.setTimestamp(8, java.sql.Timestamp.from(listing.getCreatedAt()));
        statement.setTimestamp(9, java.sql.Timestamp.from(listing.getUpdatedAt()));
    }

    private List<CustomMarketListing> findListings(final String sql, final String status, final int limit) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<CustomMarketListing>>() {

            @Override
            public List<CustomMarketListing> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(sql);
                try {
                    statement.setString(1, status);
                    statement.setInt(2, limit);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return mapListings(resultSet);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private List<CustomMarketListing> findListingsByPlayer(final String sql, final String playerRef,
        final String deliveryStatus, final int limit) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<CustomMarketListing>>() {

            @Override
            public List<CustomMarketListing> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(sql);
                try {
                    statement.setString(1, playerRef);
                    statement.setString(2, deliveryStatus);
                    statement.setInt(3, limit);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return mapListings(resultSet);
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private List<CustomMarketListing> mapListings(ResultSet resultSet) throws SQLException {
        List<CustomMarketListing> listings = new ArrayList<CustomMarketListing>();
        while (resultSet.next()) {
            listings.add(mapListing(resultSet));
        }
        return listings;
    }

    private CustomMarketListing mapListing(ResultSet resultSet) throws SQLException {
        return new CustomMarketListing(resultSet.getLong("listing_id"), resultSet.getString("seller_player_ref"),
            resultSet.getString("buyer_player_ref"), resultSet.getLong("asking_price"),
            resultSet.getString("currency_code"),
            CustomMarketListingStatus.valueOf(resultSet.getString("listing_status")),
            CustomMarketDeliveryStatus.valueOf(resultSet.getString("delivery_status")),
            resultSet.getString("source_server_id"), readInstant(resultSet, "created_at"),
            readInstant(resultSet, "updated_at"));
    }

    private int sanitizeLimit(int limit) {
        return limit <= 0 ? 20 : Math.min(limit, 50);
    }
}