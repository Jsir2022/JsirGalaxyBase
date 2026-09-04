package com.jsirgalaxybase.modules.servertools.command;

import java.time.Instant;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequest;

public class TpaResponseCommand extends AbstractServerToolsCommand {

    private final boolean accepting;

    public TpaResponseCommand(ServerToolsModule module, boolean accepting) {
        super(module);
        this.accepting = accepting;
    }

    @Override
    public String getCommandName() {
        return accepting ? "tpaccept" : "tpdeny";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return accepting ? "/tpaccept <playerName>" : "/tpdeny <playerName>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        PlayerTeleportService service = requireService(sender);
        if (service == null) {
            return;
        }
        if (args.length != 1) {
            send(sender, getCommandUsage(sender));
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        try {
            TpaRequest request = accepting ? service.acceptTpa(module.captureActor(player), args[0], Instant.now())
                : service.declineTpa(module.captureActor(player), args[0], Instant.now());
            sendKey(sender, accepting ? "jsirgalaxybase.servertools.tpa.accepted"
                : "jsirgalaxybase.servertools.tpa.declined", request.getRequesterPlayerName());
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }
}
