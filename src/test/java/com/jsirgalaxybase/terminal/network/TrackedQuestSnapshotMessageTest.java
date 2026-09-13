package com.jsirgalaxybase.terminal.network;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;

import com.jsirgalaxybase.terminal.TerminalTrackedQuestSnapshot;
import com.jsirgalaxybase.terminal.client.TrackedQuestHudState;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TrackedQuestSnapshotMessageTest {
    @After public void clear(){TrackedQuestHudState.clear();}
    @Test public void roundTripsBoundedHudProjection(){TerminalTrackedQuestSnapshot snapshot=new TerminalTrackedQuestSnapshot(Arrays.asList(new TerminalTrackedQuestSnapshot.Quest("quest","跨服钢材", "IN_PROGRESS",Arrays.asList(new TerminalTrackedQuestSnapshot.Task("交付钢锭",32,64)))),123L);ByteBuf buffer=Unpooled.buffer();new TrackedQuestSnapshotMessage(snapshot).toBytes(buffer);TrackedQuestSnapshotMessage decoded=new TrackedQuestSnapshotMessage();decoded.fromBytes(buffer);new TrackedQuestSnapshotMessage.Handler().onMessage(decoded,null);assertEquals(123L,TrackedQuestHudState.get().getGeneratedAt());assertEquals("跨服钢材",TrackedQuestHudState.get().getQuests().get(0).getName());assertEquals(64L,TrackedQuestHudState.get().getQuests().get(0).getTasks().get(0).getTarget());}
}
