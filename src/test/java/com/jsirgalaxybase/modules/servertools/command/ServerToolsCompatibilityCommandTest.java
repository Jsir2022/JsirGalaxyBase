package com.jsirgalaxybase.modules.servertools.command;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.jsirgalaxybase.modules.servertools.ServerToolsModule;

public class ServerToolsCompatibilityCommandTest {

    @Test
    public void exposesServerUtilitiesCompatibleCommandNames() {
        ServerToolsModule module = new ServerToolsModule();

        assertEquals("sethome", new SetHomeCommand(module).getCommandName());
        assertEquals("delhome", new DelHomeCommand(module).getCommandName());
        assertEquals("tpaccept", new TpaResponseCommand(module, true).getCommandName());
        assertEquals("tpdeny", new TpaResponseCommand(module, false).getCommandName());
    }

    @Test
    public void tpaUsageDocumentsExplicitCrossServerTargetAndLifecycleActions() {
        String usage = new TpaCommand(new ServerToolsModule()).getCommandUsage(null);

        org.junit.Assert.assertTrue(usage.contains("targetServerId"));
        org.junit.Assert.assertTrue(usage.contains("cancel"));
        org.junit.Assert.assertTrue(usage.contains("status"));
    }
}
