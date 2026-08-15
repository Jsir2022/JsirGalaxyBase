package com.jsirgalaxybase.terminal.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TerminalResponseSequenceGateTest {

    @Test
    public void legacyAndInitialSnapshotsRemainCompatible() {
        TerminalResponseSequenceGate gate = new TerminalResponseSequenceGate();

        assertTrue(gate.shouldAccept(0L));
        gate.markApplied(0L);
        assertEquals(0L, gate.getLatestApplied());
    }

    @Test
    public void olderResponseCannotOverwriteNewestIssuedRequest() {
        TerminalResponseSequenceGate gate = new TerminalResponseSequenceGate();
        long first = gate.issueNext();
        long second = gate.issueNext();

        assertFalse(gate.shouldAccept(first));
        assertTrue(gate.shouldAccept(second));
        gate.markApplied(second);
        assertFalse(gate.shouldAccept(first));
        assertEquals(second, gate.getLatestApplied());
    }

    @Test
    public void nextResponseCanAdvanceAfterLatestSnapshot() {
        TerminalResponseSequenceGate gate = new TerminalResponseSequenceGate();
        long first = gate.issueNext();
        gate.markApplied(first);
        long second = gate.issueNext();

        assertTrue(gate.shouldAccept(second));
        gate.markApplied(second);
        assertEquals(second, gate.getLatestApplied());
    }
}
