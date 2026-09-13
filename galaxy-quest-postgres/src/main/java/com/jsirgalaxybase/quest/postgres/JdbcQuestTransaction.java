package com.jsirgalaxybase.quest.postgres;

import java.sql.Connection;
import java.sql.SQLException;

import com.jsirgalaxybase.quest.core.QuestTransaction;

public final class JdbcQuestTransaction implements QuestTransaction {
    private final JdbcQuestConnectionManager connections;

    public JdbcQuestTransaction(JdbcQuestConnectionManager connections) {
        if (connections == null) throw new IllegalArgumentException("connections must not be null");
        this.connections = connections;
    }

    @Override
    public <T> T inTransaction(final Work<T> work) {
        if (connections.current() != null) return work.execute();
        return connections.withConnection(new JdbcQuestConnectionManager.SqlWork<T>() {
            @Override
            public T execute(Connection connection) throws SQLException {
                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                connections.bind(connection);
                try {
                    T result = work.execute();
                    connection.commit();
                    return result;
                } catch (RuntimeException exception) {
                    rollback(connection, exception);
                    throw exception;
                } catch (Error error) {
                    rollback(connection, error);
                    throw error;
                } finally {
                    connections.clear();
                    connection.setAutoCommit(autoCommit);
                }
            }
        });
    }

    private static void rollback(Connection connection, Throwable original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
