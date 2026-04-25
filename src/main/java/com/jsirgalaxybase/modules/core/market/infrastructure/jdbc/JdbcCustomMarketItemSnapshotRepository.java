package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.AbstractJdbcRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketItemSnapshot;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketItemSnapshotRepository;

public class JdbcCustomMarketItemSnapshotRepository extends AbstractJdbcRepository
    implements CustomMarketItemSnapshotRepository {

    public JdbcCustomMarketItemSnapshotRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public CustomMarketItemSnapshot save(final CustomMarketItemSnapshot snapshot) {
        return connectionManager.withConnection(new JdbcConnectionCallback<CustomMarketItemSnapshot>() {

            @Override
            public CustomMarketItemSnapshot doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO custom_market_item_snapshot (listing_id, item_id, meta, stack_size, stackable, display_name, nbt_snapshot, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    statement.setLong(1, snapshot.getListingId());
                    statement.setString(2, snapshot.getItemId());
                    statement.setInt(3, snapshot.getMeta());
                    statement.setInt(4, snapshot.getStackSize());
                    statement.setBoolean(5, snapshot.isStackable());
                    statement.setString(6, snapshot.getDisplayName());
                    statement.setString(7, snapshot.getNbtSnapshot());
                    statement.setTimestamp(8, java.sql.Timestamp.from(snapshot.getCreatedAt()));
                    statement.executeUpdate();
                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    try {
                        if (!generatedKeys.next()) {
                            throw new MarketOperationException(
                                "failed to read generated custom_market_item_snapshot key");
                        }
                        return new CustomMarketItemSnapshot(generatedKeys.getLong(1), snapshot.getListingId(),
                            snapshot.getItemId(), snapshot.getMeta(), snapshot.getStackSize(),
                            snapshot.isStackable(), snapshot.getDisplayName(), snapshot.getNbtSnapshot(),
                            snapshot.getCreatedAt());
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
    public Optional<CustomMarketItemSnapshot> findByListingId(final long listingId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<CustomMarketItemSnapshot>>() {

            @Override
            public Optional<CustomMarketItemSnapshot> doInConnection(java.sql.Connection connection)
                throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM custom_market_item_snapshot WHERE listing_id = ?");
                try {
                    statement.setLong(1, listingId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapSnapshot(resultSet))
                            : Optional.<CustomMarketItemSnapshot>empty();
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private CustomMarketItemSnapshot mapSnapshot(ResultSet resultSet) throws SQLException {
        return new CustomMarketItemSnapshot(resultSet.getLong("snapshot_id"), resultSet.getLong("listing_id"),
            resultSet.getString("item_id"), resultSet.getInt("meta"), resultSet.getInt("stack_size"),
            resultSet.getBoolean("stackable"), resultSet.getString("display_name"),
            resultSet.getString("nbt_snapshot"), readInstant(resultSet, "created_at"));
    }
}