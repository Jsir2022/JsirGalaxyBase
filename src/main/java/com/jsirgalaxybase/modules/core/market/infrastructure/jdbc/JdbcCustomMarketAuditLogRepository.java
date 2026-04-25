package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.AbstractJdbcRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionCallback;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketAuditLog;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketAuditType;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketAuditLogRepository;

public class JdbcCustomMarketAuditLogRepository extends AbstractJdbcRepository
    implements CustomMarketAuditLogRepository {

    public JdbcCustomMarketAuditLogRepository(JdbcConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public CustomMarketAuditLog save(final CustomMarketAuditLog auditLog) {
        return connectionManager.withConnection(new JdbcConnectionCallback<CustomMarketAuditLog>() {

            @Override
            public CustomMarketAuditLog doInConnection(java.sql.Connection connection) throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO custom_market_audit_log (request_id, audit_type, player_ref, request_semantics_key, listing_id, trade_id, source_server_id, message, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
                try {
                    statement.setString(1, auditLog.getRequestId());
                    statement.setString(2, auditLog.getAuditType().name());
                    statement.setString(3, auditLog.getPlayerRef());
                    statement.setString(4, auditLog.getRequestSemanticsKey());
                    if (auditLog.getListingId() > 0L) {
                        statement.setLong(5, auditLog.getListingId());
                    } else {
                        statement.setNull(5, java.sql.Types.BIGINT);
                    }
                    if (auditLog.getTradeId() > 0L) {
                        statement.setLong(6, auditLog.getTradeId());
                    } else {
                        statement.setNull(6, java.sql.Types.BIGINT);
                    }
                    statement.setString(7, auditLog.getSourceServerId());
                    setNullableText(statement, 8, auditLog.getMessage());
                    statement.setTimestamp(9, java.sql.Timestamp.from(auditLog.getCreatedAt()));
                    statement.setTimestamp(10, java.sql.Timestamp.from(auditLog.getUpdatedAt()));
                    statement.executeUpdate();
                    ResultSet generatedKeys = statement.getGeneratedKeys();
                    try {
                        if (!generatedKeys.next()) {
                            throw new MarketOperationException("failed to read generated custom_market_audit_log key");
                        }
                        return new CustomMarketAuditLog(generatedKeys.getLong(1), auditLog.getRequestId(),
                            auditLog.getAuditType(), auditLog.getPlayerRef(), auditLog.getRequestSemanticsKey(),
                            auditLog.getListingId(), auditLog.getTradeId(), auditLog.getSourceServerId(),
                            auditLog.getMessage(), auditLog.getCreatedAt(), auditLog.getUpdatedAt());
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
    public Optional<CustomMarketAuditLog> findByRequestId(final String requestId) {
        return connectionManager.withConnection(new JdbcConnectionCallback<Optional<CustomMarketAuditLog>>() {

            @Override
            public Optional<CustomMarketAuditLog> doInConnection(java.sql.Connection connection)
                throws SQLException {
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM custom_market_audit_log WHERE request_id = ?");
                try {
                    statement.setString(1, requestId);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        return resultSet.next() ? Optional.of(mapAuditLog(resultSet))
                            : Optional.<CustomMarketAuditLog>empty();
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    statement.close();
                }
            }
        });
    }

    private CustomMarketAuditLog mapAuditLog(ResultSet resultSet) throws SQLException {
        return new CustomMarketAuditLog(resultSet.getLong("audit_id"), resultSet.getString("request_id"),
            CustomMarketAuditType.valueOf(resultSet.getString("audit_type")), resultSet.getString("player_ref"),
            resultSet.getString("request_semantics_key"), resultSet.getLong("listing_id"),
            resultSet.getLong("trade_id"), resultSet.getString("source_server_id"), resultSet.getString("message"),
            readInstant(resultSet, "created_at"), readInstant(resultSet, "updated_at"));
    }
}