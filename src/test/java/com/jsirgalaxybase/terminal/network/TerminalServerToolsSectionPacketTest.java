package com.jsirgalaxybase.terminal.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalServerToolsSectionModel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TerminalServerToolsSectionPacketTest {

    @Test
    public void namedHomesAndCrossServerDetailsSurviveRawPacketRoundTrip() {
        TerminalServerToolsSectionModel source = new TerminalServerToolsSectionModel(
            "READY", "lobby", Arrays.asList("lobby | 当前"), Arrays.asList("lobby"),
            Arrays.asList("spawn"), Arrays.asList("spawn"), Arrays.asList("返回出生点"),
            Arrays.asList("可用"), Arrays.asList("暂无记录"),
            Arrays.asList("home | lobby", "mine | s2"), Arrays.asList("home", "mine"),
            Arrays.asList("dim 0 / 0, 70, 0", "dim 2 / 10, 70, -20"),
            Arrays.asList("OUTGOING", "INCOMING"), Arrays.asList("Alice", "Bob"),
            Arrays.asList("s2", "lobby"), Arrays.asList("PENDING", "ACCEPTED"),
            "spawn", "出生点", "当前服务器出生点", "lobby", "dim 0 / 0, 64, 0", "安全地点", true,
            "mine", "s2", "dim 2 / 10, 70, -20", "个人 Home mine", "lobby", "s2", "COMPLETED",
            "08-30 12:00", "恢复完成", new TerminalServerToolsSectionModel.ActionFeedbackModel("Home 已选择", "mine", "INFO"));
        ByteBuf buffer = Unpooled.buffer();

        OpenTerminalApprovedMessage.writeServerToolsSection(buffer, source);
        TerminalServerToolsSectionModel decoded = OpenTerminalApprovedMessage.readServerToolsSection(buffer);

        assertEquals("mine", decoded.getSelectedHomeName());
        assertEquals("s2", decoded.getSelectedHomeTargetServerId());
        assertEquals("dim 2 / 10, 70, -20", decoded.getSelectedHomeTargetLocation());
        assertEquals(2, decoded.getHomeNames().size());
        assertEquals("mine", decoded.getHomeNames().get(1));
        assertTrue(decoded.getHomeSubtitles().get(1).contains("dim 2"));
        assertEquals("OUTGOING", decoded.getTpaDirections().get(0));
        assertEquals("Bob", decoded.getTpaCounterpartyNames().get(1));
        assertEquals("ACCEPTED", decoded.getTpaStatusLabels().get(1));
    }
}
