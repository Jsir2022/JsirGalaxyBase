package com.jsirgalaxybase.modules.servertools.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;

public class DelHomeCommand extends AbstractServerToolsCommand {

    public DelHomeCommand(ServerToolsModule module) {
        super(module);
    }

    @Override
    public String getCommandName() {
        return "delhome";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/delhome [name]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        PlayerTeleportService service = requireService(sender);
        if (service == null) {
            return;
        }
        if (args.length > 1) {
            send(sender, getCommandUsage(sender));
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        String homeName = args.length == 0 ? "home" : args[0];
        try {
            if (service.deleteHome(player.getUniqueID().toString(), homeName)) {
                sendKey(sender, "jsirgalaxybase.servertools.home.deleted", homeName.toLowerCase());
            } else {
                sendKey(sender, "jsirgalaxybase.servertools.home.not_found", homeName);
            }
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }
}
