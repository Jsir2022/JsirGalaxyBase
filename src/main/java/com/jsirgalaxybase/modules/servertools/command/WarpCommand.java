package com.jsirgalaxybase.modules.servertools.command;

import java.util.List;

import net.minecraft.command.ICommandSender;

import com.jsirgalaxybase.modules.servertools.ServerToolsModule;

public class WarpCommand extends AbstractServerToolsCommand {

    private final ServerToolsCommandHandler handler;

    public WarpCommand(ServerToolsModule module) {
        super(module);
        this.handler = new ServerToolsCommandHandler(module);
    }

    @Override
    public String getCommandName() {
        return "warp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/warp [list|name]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        handler.processWarpCommand(sender, args);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return handler.addWarpTabCompletionOptions(args);
    }
}
