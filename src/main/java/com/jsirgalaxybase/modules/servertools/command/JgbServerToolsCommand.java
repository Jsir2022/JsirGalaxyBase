package com.jsirgalaxybase.modules.servertools.command;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.ICommandSender;

import com.jsirgalaxybase.modules.servertools.ServerToolsModule;

public class JgbServerToolsCommand extends AbstractServerToolsCommand {

    private final ServerToolsCommandHandler handler;

    public JgbServerToolsCommand(ServerToolsModule module) {
        super(module);
        this.handler = new ServerToolsCommandHandler(module);
    }

    @Override
    public String getCommandName() {
        return "jgbst";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/jgbst warp [list|name]";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("jst", "jsirst");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        handler.processRootCommand(sender, args, getCommandUsage(sender));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return handler.addRootTabCompletionOptions(args);
    }
}
