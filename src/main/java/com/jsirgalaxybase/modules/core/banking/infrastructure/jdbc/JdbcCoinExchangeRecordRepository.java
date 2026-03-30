package com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.domain.CoinExchangeRecord;
import com.jsirgalaxybase.modules.core.banking.repository.CoinExchangeRecordRepository;

public class JdbcCoinExchangeRecordRepository extends AbstractJdbcRepository implements CoinExchangeRecordRepository {

    public JdbcCoinExchangeRecordRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public CoinExchangeRecord save(final CoinExchangeRecord record) {
        return connectionManager.withConnection(new JdbcConnectionCallback<CoinExchangeRecord>() {

            @Override
            public CoinExchangeRecord doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO coin_exchange_record (transaction_id, player_ref, coin_family, coin_tier, coin_face_value, coin_quantity, effective_exchange_value, contribution_value, exchange_rule_version, source_server_id, extra_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    statement.setLong(1, record.getTransactionId());
                    statement.setString(2, record.getPlayerRef());
                    statement.setString(3, record.getCoinFamily());
                    statement.setString(4, record.getCoinTier());
                    statement.setLong(5, record.getCoinFaceValue());
                    statement.setLong(6, record.getCoinQuantity());
                    statement.setLong(7, record.getEffectiveExchangeValue());
                    statement.setLong(8, record.getContributionValue());
                    statement.setString(9, record.getRuleVersion());
                    statement.setString(10, record.getSourceServerId());
                    statement.setString(11, record.getExtraJson());
                    statement.setTimestamp(12, java.sql.Timestamp.from(record.getCreatedAt()));
                    statement.executeUpdate();

                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    try {
                        if (!generatedKeys.next()) {
                            throw new BankingException("failed to read generated coin_exchange_record key");
                        }
                        return new CoinExchangeRecord(generatedKeys.getLong(1), record.getTransactionId(),
                            record.getPlayerRef(), record.getCoinFamily(), record.getCoinTier(),
                            record.getCoinFaceValue(), record.getCoinQuantity(), record.getEffectiveExchangeValue(),
                            record.getContributionValue(), record.getRuleVersion(), record.getSourceServerId(),
                            record.getExtraJson(), record.getCreatedAt());
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
    public Optional<CoinExchangeRecord> findByTransactionId(final long transactionId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<CoinExchangeRecord>>() {

            @Override
            public Optional<CoinExchangeRecord> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM coin_exchange_record WHERE transaction_id = ?");
                try {
                    statement.setLong(1, transactionId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapRecord(resultSet))
                            : Optional.<CoinExchangeRecord>empty();
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
    public List<CoinExchangeRecord> findRecentByPlayerRef(final String playerRef, final int limit) {
        return connectionManager.withConnection(new JdbcConnectionCallback<List<CoinExchangeRecord>>() {

            @Override
            public List<CoinExchangeRecord> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM coin_exchange_record WHERE player_ref = ? ORDER BY exchange_id DESC LIMIT ?");
                try {
                    statement.setString(1, playerRef);
                    statement.setInt(2, limit);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        List<CoinExchangeRecord> records = new ArrayList<CoinExchangeRecord>();
                        while (resultSet.next()) {
                            records.add(mapRecord(resultSet));
                        }
                        return records;
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private CoinExchangeRecord mapRecord(ResultSet resultSet) throws SQLException {
        return new CoinExchangeRecord(resultSet.getLong("exchange_id"), resultSet.getLong("transaction_id"),
            resultSet.getString("player_ref"), resultSet.getString("coin_family"), resultSet.getString("coin_tier"),
            resultSet.getLong("coin_face_value"), resultSet.getLong("coin_quantity"),
            resultSet.getLong("effective_exchange_value"), resultSet.getLong("contribution_value"),
            resultSet.getString("exchange_rule_version"), resultSet.getString("source_server_id"),
            resultSet.getString("extra_json"), readInstant(resultSet, "created_at"));
    }
}