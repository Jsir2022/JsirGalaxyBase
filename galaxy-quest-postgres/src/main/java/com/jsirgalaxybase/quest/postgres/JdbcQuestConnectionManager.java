package com.jsirgalaxybase.quest.postgres;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public final class JdbcQuestConnectionManager {
    private final DataSource dataSource;
    private final ThreadLocal<Connection> transaction = new ThreadLocal<Connection>();

    public JdbcQuestConnectionManager(DataSource dataSource) {
        if (dataSource == null) throw new IllegalArgumentException("dataSource must not be null");
        this.dataSource = dataSource;
    }

    Connection current() { return transaction.get(); }
    void bind(Connection connection) { transaction.set(connection); }
    void clear() { transaction.remove(); }

    <T> T withConnection(SqlWork<T> work) {
        Connection existing = current();
        if (existing != null) return execute(existing, work);
        try (Connection connection = dataSource.getConnection()) {
            return execute(connection, work);
        } catch (SQLException exception) {
            throw new QuestPersistenceException("quest database connection failed", exception);
        }
    }

    private static <T> T execute(Connection connection, SqlWork<T> work) {
        try {
            return work.execute(connection);
        } catch (SQLException exception) {
            throw new QuestPersistenceException("quest database operation failed", exception);
        }
    }

    interface SqlWork<T> { T execute(Connection connection) throws SQLException; }
}
