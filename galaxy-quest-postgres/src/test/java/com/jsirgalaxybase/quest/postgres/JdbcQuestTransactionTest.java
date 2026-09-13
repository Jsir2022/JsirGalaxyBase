package com.jsirgalaxybase.quest.postgres;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.Test;

public class JdbcQuestTransactionTest {
    @Test
    public void commitsAndRestoresConnectionState() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.getAutoCommit()).thenReturn(true);
        DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenReturn(connection);
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(source);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);

        assertEquals("ok", transaction.inTransaction(() -> "ok"));

        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
        verify(connection).close();
    }

    @Test
    public void rollsBackRuntimeFailureAndNestedWorkUsesSameBoundary() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.getAutoCommit()).thenReturn(true);
        DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenReturn(connection);
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(source);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);

        try {
            transaction.inTransaction(() -> transaction.inTransaction(() -> { throw new IllegalStateException("boom"); }));
            fail("expected failure");
        } catch (IllegalStateException expected) {
            assertEquals("boom", expected.getMessage());
        }
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection, times(2)).setAutoCommit(anyBoolean());
    }
}
