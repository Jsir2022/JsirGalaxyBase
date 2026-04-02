package com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public abstract class AbstractJdbcRepository {

    protected final JdbcConnectionManager connectionManager;

    protected AbstractJdbcRepository(JdbcConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    protected Instant readInstant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    protected void setNullableText(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
            return;
        }
        statement.setString(index, value);
    }
}