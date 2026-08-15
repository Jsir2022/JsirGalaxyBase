package com.jsirgalaxybase.terminal.client;

/**
 * Orders terminal request/response snapshots without depending on a live GUI.
 * Sequence zero is reserved for legacy packets and initial screen opens.
 */
public final class TerminalResponseSequenceGate {

    private long latestIssued;
    private long latestApplied;

    public long issueNext() {
        return ++latestIssued;
    }

    public boolean shouldAccept(long sequence) {
        return sequence <= 0L || sequence >= Math.max(latestIssued, latestApplied);
    }

    public void markApplied(long sequence) {
        if (sequence > 0L) {
            latestApplied = Math.max(latestApplied, sequence);
        }
    }

    public long getLatestIssued() {
        return latestIssued;
    }

    public long getLatestApplied() {
        return latestApplied;
    }
}
