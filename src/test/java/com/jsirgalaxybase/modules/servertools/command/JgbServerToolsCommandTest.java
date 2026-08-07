package com.jsirgalaxybase.modules.servertools.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.modules.servertools.ServerToolsModule;

public class JgbServerToolsCommandTest {

    @Test
    public void rootCommandUsesDedicatedServerToolsNamespace() {
        JgbServerToolsCommand command = new JgbServerToolsCommand(new ServerToolsModule());

        assertEquals("jgbst", command.getCommandName());
        assertTrue(command.getCommandAliases().contains("jst"));
        assertTrue(command.getCommandAliases().contains("jsirst"));
    }

    @Test
    public void rootTabCompletionExposesWarpSubcommand() {
        ServerToolsCommandHandler handler = new ServerToolsCommandHandler(new ServerToolsModule());

        List<String> suggestions = handler.addRootTabCompletionOptions(new String[] { "w" });

        assertEquals(1, suggestions.size());
        assertEquals("warp", suggestions.get(0));
    }
}
