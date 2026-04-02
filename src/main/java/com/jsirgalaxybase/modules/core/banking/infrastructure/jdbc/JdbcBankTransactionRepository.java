package com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.domain.BankBusinessType;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransaction;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransactionType;
import com.jsirgalaxybase.modules.core.banking.repository.BankTransactionRepository;

public class JdbcBankTransactionRepository extends AbstractJdbcRepository implements BankTransactionRepository {

    public JdbcBankTransactionRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public Optional<BankTransaction> findById(final long transactionId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<BankTransaction>>() {

            @Override
            public Optional<BankTransaction> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection
                    .prepareStatement("SELECT * FROM bank_transaction WHERE transaction_id = ?");
                try {
                    statement.setLong(1, transactionId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapTransaction(resultSet))
                            : Optional.<BankTransaction>empty();
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
    public Optional<BankTransaction> findByRequestId(final String requestId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<BankTransaction>>() {

            @Override
            public Optional<BankTransaction> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection
                    .prepareStatement("SELECT * FROM bank_transaction WHERE request_id = ?");
                try {
                    statement.setString(1, requestId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapTransaction(resultSet))
                            : Optional.<BankTransaction>empty();
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
    public BankTransaction save(final BankTransaction transaction) {
        return connectionManager.withConnection(new JdbcConnectionCallback<BankTransaction>() {

            @Override
            public BankTransaction doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO bank_transaction (request_id, transaction_type, business_type, business_ref, source_server_id, operator_type, operator_ref, player_ref, comment, extra_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    statement.setString(1, transaction.getRequestId());
                    statement.setString(2, transaction.getTransactionType().name());
                    statement.setString(3, transaction.getBusinessType().name());
                    setNullableText(statement, 4, transaction.getBusinessRef());
                    statement.setString(5, transaction.getSourceServerId());
                    statement.setString(6, transaction.getOperatorType());
                    setNullableText(statement, 7, transaction.getOperatorRef());
                    setNullableText(statement, 8, transaction.getPlayerRef());
                    setNullableText(statement, 9, transaction.getComment());
                    statement.setString(10, transaction.getExtraJson());
                    statement.setTimestamp(11, java.sql.Timestamp.from(transaction.getCreatedAt()));
                    statement.executeUpdate();

                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    try {
                        if (!generatedKeys.next()) {
                            throw new BankingException("failed to read generated bank_transaction key");
                        }
                        return new BankTransaction(generatedKeys.getLong(1), transaction.getRequestId(),
                            transaction.getTransactionType(), transaction.getBusinessType(), transaction.getBusinessRef(),
                            transaction.getSourceServerId(), transaction.getOperatorType(), transaction.getOperatorRef(),
                            transaction.getPlayerRef(), transaction.getComment(), transaction.getExtraJson(),
                            transaction.getCreatedAt());
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
    public Optional<BankTransaction> saveIfRequestAbsent(final BankTransaction transaction) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<BankTransaction>>() {

            @Override
            public Optional<BankTransaction> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO bank_transaction (request_id, transaction_type, business_type, business_ref, source_server_id, operator_type, operator_ref, player_ref, comment, extra_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?) ON CONFLICT (request_id) DO NOTHING RETURNING *");
                try {
                    statement.setString(1, transaction.getRequestId());
                    statement.setString(2, transaction.getTransactionType().name());
                    statement.setString(3, transaction.getBusinessType().name());
                    setNullableText(statement, 4, transaction.getBusinessRef());
                    statement.setString(5, transaction.getSourceServerId());
                    statement.setString(6, transaction.getOperatorType());
                    setNullableText(statement, 7, transaction.getOperatorRef());
                    setNullableText(statement, 8, transaction.getPlayerRef());
                    setNullableText(statement, 9, transaction.getComment());
                    statement.setString(10, transaction.getExtraJson());
                    statement.setTimestamp(11, java.sql.Timestamp.from(transaction.getCreatedAt()));

                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapTransaction(resultSet)) : Optional.<BankTransaction>empty();
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private BankTransaction mapTransaction(ResultSet resultSet) throws SQLException {
        return new BankTransaction(resultSet.getLong("transaction_id"), resultSet.getString("request_id"),
            BankTransactionType.valueOf(resultSet.getString("transaction_type")),
            BankBusinessType.valueOf(resultSet.getString("business_type")), resultSet.getString("business_ref"),
            resultSet.getString("source_server_id"), resultSet.getString("operator_type"),
            resultSet.getString("operator_ref"), resultSet.getString("player_ref"), resultSet.getString("comment"),
            resultSet.getString("extra_json"), readInstant(resultSet, "created_at"));
    }
}