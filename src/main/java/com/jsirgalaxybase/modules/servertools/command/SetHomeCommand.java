package com.jsirgalaxybase.modules.servertools.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.PlayerHome;

public class SetHomeCommand extends AbstractServerToolsCommand {

    public SetHomeCommand(ServerToolsModule module) {
        super(module);
    }

    @Override
    public String getCommandName() {
        return "sethome";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/sethome [name]";
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
        try {
            PlayerHome home = service.setHome(module.captureActor(player), args.length == 0 ? "home" : args[0]);
            sendKey(sender, "jsirgalaxybase.servertools.home.saved", home.getHomeName(),
                home.getTarget().getServerId(), Integer.valueOf(home.getTarget().getDimensionId()));
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }
}
