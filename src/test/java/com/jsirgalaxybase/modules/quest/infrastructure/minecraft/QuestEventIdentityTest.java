package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.*;

import org.junit.Test;

public class QuestEventIdentityTest {
    @Test
    public void producesDeterministicProcessScopedMonotonicIds() {
        QuestEventIdentity identity = new QuestEventIdentity("boot");
        assertEquals("boot:kill:1", identity.next("kill"));
        assertEquals("boot:craft:2", identity.next("craft"));
        assertNotEquals(identity.next("kill"), identity.next("kill"));
    }
}
