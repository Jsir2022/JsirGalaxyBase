package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.AbstractJdbcRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationType;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;

public class JdbcMarketOperationLogRepository extends AbstractJdbcRepository implements MarketOperationLogRepository {

    public JdbcMarketOperationLogRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public MarketOperationLog save(final MarketOperationLog operationLog) {
        return connectionManager.withConnection(new JdbcConnectionCallback<MarketOperationLog>() {

            @Override
            public MarketOperationLog doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO market_operation_log (request_id, operation_type, operation_status, source_server_id, player_ref, request_semantics_key, recovery_metadata_key, related_order_id, related_custody_id, related_trade_id, message, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    bindOperation(statement, operationLog);
                    statement.executeUpdate();
                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    try {
                        if (!generatedKeys.next()) {
                            throw new MarketOperationException("failed to read generated market_operation_log key");
                        }
                        return new MarketOperationLog(generatedKeys.getLong(1), operationLog.getRequestId(),
                            operationLog.getOperationType(), operationLog.getStatus(), operationLog.getSourceServerId(),
                            operationLog.getPlayerRef(), operationLog.getRequestSemanticsKey(),
                            operationLog.getRecoveryMetadataKey(),
                            operationLog.getRelatedOrderId(), operationLog.getRelatedCustodyId(),
                            operationLog.getRelatedTradeId(), operationLog.getMessage(), operationLog.getCreatedAt(),
                            operationLog.getUpdatedAt());
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
    public MarketOperationLog update(final MarketOperationLog operationLog) {
        return connectionManager.withConnection(new JdbcConnectionCallback<MarketOperationLog>() {

            @Override
            public MarketOperationLog doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "UPDATE market_operation_log SET operation_status = ?, recovery_metadata_key = ?, related_order_id = ?, related_custody_id = ?, related_trade_id = ?, message = ?, updated_at = ? WHERE operation_id = ?");
                try {
                    statement.setString(1, operationLog.getStatus().name());
                    setNullableText(statement, 2, operationLog.getRecoveryMetadataKey());
                    if (operationLog.getRelatedOrderId() > 0L) {
                        statement.setLong(3, operationLog.getRelatedOrderId());
                    } else {
                        statement.setNull(3, java.sql.Types.BIGINT);
                    }
                    if (operationLog.getRelatedCustodyId() > 0L) {
                        statement.setLong(4, operationLog.getRelatedCustodyId());
                    } else {
                        statement.setNull(4, java.sql.Types.BIGINT);
                    }
                    if (operationLog.getRelatedTradeId() > 0L) {
                        statement.setLong(5, operationLog.getRelatedTradeId());
                    } else {
                        statement.setNull(5, java.sql.Types.BIGINT);
                    }
                    setNullableText(statement, 6, operationLog.getMessage());
                    statement.setTimestamp(7, java.sql.Timestamp.from(operationLog.getUpdatedAt()));
                    statement.setLong(8, operationLog.getOperationId());
                    if (statement.executeUpdate() != 1) {
                        throw new MarketOperationException(
                            "market_operation_log update failed for operationId=" + operationLog.getOperationId());
                    }
                    return operationLog;
                } finally {
                    statement.close();
                }
            }
        });
    }

    @Override
    public Optional<MarketOperationLog> findById(final long operationId) {
        return findOne("SELECT * FROM market_operation_log WHERE operation_id = ?", operationId, null);
    }

    @Override
    public Optional<MarketOperationLog> findByRequestId(final String requestId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<MarketOperationLog>>() {

            @Override
            public Optional<MarketOperationLog> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_operation_log WHERE request_id = ?");
                try {
                    statement.setString(1, requestId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapOperation(resultSet))
                            : Optional.<MarketOperationLog>empty();
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
    public List<MarketOperationLog> findByStatuses(final List<MarketOperationStatus> statuses, final int limit) {
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }

        return connectionManager.withConnection(new JdbcConnectionCallback<List<MarketOperationLog>>() {

            @Override
            public List<MarketOperationLog> doInConnection(java.sql.Connection connection) throws SQLException {
                StringBuilder placeholders = new StringBuilder();
                for (int i = 0; i < statuses.size(); i++) {
                    if (i > 0) {
                        placeholders.append(", ");
                    }
                    placeholders.append('?');
                }

                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM market_operation_log WHERE operation_status IN (" + placeholders.toString()
                        + ") ORDER BY created_at ASC, operation_id ASC LIMIT ?");
                try {
                    int index = 1;
                    for (MarketOperationStatus status : statuses) {
                        statement.setString(index++, status.name());
                    }
                    statement.setInt(index, limit);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        List<MarketOperationLog> operations = new ArrayList<MarketOperationLog>();
                        while (resultSet.next()) {
                            operations.add(mapOperation(resultSet));
                        }
                        return operations;
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private Optional<MarketOperationLog> findOne(final String sql, final long id, final String requestId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<MarketOperationLog>>() {

            @Override
            public Optional<MarketOperationLog> doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(sql);
                try {
                    if (requestId != null) {
                        statement.setString(1, requestId);
                    } else {
                        statement.setLong(1, id);
                    }
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapOperation(resultSet))
                            : Optional.<MarketOperationLog>empty();
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private void bindOperation(PreparedStatement statement, MarketOperationLog operationLog) throws SQLException {
        statement.setString(1, operationLog.getRequestId());
        statement.setString(2, operationLog.getOperationType().name());
        statement.setString(3, operationLog.getStatus().name());
        statement.setString(4, operationLog.getSourceServerId());
        statement.setString(5, operationLog.getPlayerRef());
        statement.setString(6, operationLog.getRequestSemanticsKey());
        setNullableText(statement, 7, operationLog.getRecoveryMetadataKey());
        if (operationLog.getRelatedOrderId() > 0L) {
            statement.setLong(8, operationLog.getRelatedOrderId());
        } else {
            statement.setNull(8, java.sql.Types.BIGINT);
        }
        if (operationLog.getRelatedCustodyId() > 0L) {
            statement.setLong(9, operationLog.getRelatedCustodyId());
        } else {
            statement.setNull(9, java.sql.Types.BIGINT);
        }
        if (operationLog.getRelatedTradeId() > 0L) {
            statement.setLong(10, operationLog.getRelatedTradeId());
        } else {
            statement.setNull(10, java.sql.Types.BIGINT);
        }
        setNullableText(statement, 11, operationLog.getMessage());
        statement.setTimestamp(12, java.sql.Timestamp.from(operationLog.getCreatedAt()));
        statement.setTimestamp(13, java.sql.Timestamp.from(operationLog.getUpdatedAt()));
    }

    private MarketOperationLog mapOperation(ResultSet resultSet) throws SQLException {
        return new MarketOperationLog(resultSet.getLong("operation_id"), resultSet.getString("request_id"),
            MarketOperationType.valueOf(resultSet.getString("operation_type")),
            MarketOperationStatus.valueOf(resultSet.getString("operation_status")),
            resultSet.getString("source_server_id"), resultSet.getString("player_ref"),
            resultSet.getString("request_semantics_key"), resultSet.getString("recovery_metadata_key"),
            resultSet.getLong("related_order_id"),
            resultSet.getLong("related_custody_id"), resultSet.getLong("related_trade_id"),
            resultSet.getString("message"), readInstant(resultSet, "created_at"), readInstant(resultSet, "updated_at"));
    }
}