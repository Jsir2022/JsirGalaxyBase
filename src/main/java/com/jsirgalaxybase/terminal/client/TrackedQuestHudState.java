package com.jsirgalaxybase.terminal.client;

import com.jsirgalaxybase.terminal.TerminalTrackedQuestSnapshot;

/** Render-thread visible cache updated only by authenticated server snapshots. */
public final class TrackedQuestHudState {
    private static volatile TerminalTrackedQuestSnapshot snapshot=TerminalTrackedQuestSnapshot.EMPTY;
    private TrackedQuestHudState(){}
    public static TerminalTrackedQuestSnapshot get(){return snapshot;}
    public static void update(TerminalTrackedQuestSnapshot value){snapshot=value==null?TerminalTrackedQuestSnapshot.EMPTY:value;}
    public static void clear(){snapshot=TerminalTrackedQuestSnapshot.EMPTY;}
}
