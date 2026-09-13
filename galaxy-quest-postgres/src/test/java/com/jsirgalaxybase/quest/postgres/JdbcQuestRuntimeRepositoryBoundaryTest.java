package com.jsirgalaxybase.quest.postgres;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.GameplayFact;

public class JdbcQuestRuntimeRepositoryBoundaryTest {
    @Test
    public void rejectsFactMutationOutsideAtomicTransaction() {
        JdbcQuestRuntimeRepository repository = new JdbcQuestRuntimeRepository(
            new JdbcQuestConnectionManager(mock(DataSource.class)));
        try {
            repository.recordFactIfAbsent(new GameplayFact("s2", "event", UUID.randomUUID(), "kill", 1L,
                Collections.<String, String>emptyMap()));
            fail("expected transaction boundary failure");
        } catch (QuestPersistenceException expected) {
            assertTrue(expected.getMessage().contains("active quest transaction"));
        }
    }
}
