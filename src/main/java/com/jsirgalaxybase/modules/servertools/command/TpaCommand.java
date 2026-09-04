package com.jsirgalaxybase.modules.servertools.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequest;

public class TpaCommand extends AbstractServerToolsCommand {

    public TpaCommand(ServerToolsModule module) {
        super(module);
    }

    @Override
    public String getCommandName() {
        return "tpa";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/tpa <playerName> [targetServerId] | /tpa accept|deny <playerName> | /tpa cancel <playerName> [targetServerId] | /tpa status";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        PlayerTeleportService service = requireService(sender);
        if (service == null) {
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        try {
            if (args.length == 0) {
                send(sender, getCommandUsage(sender));
                return;
            }
            if ("accept".equalsIgnoreCase(args[0])) {
                respond(sender, service, player, args, true);
                return;
            }
            if ("deny".equalsIgnoreCase(args[0]) || "decline".equalsIgnoreCase(args[0])) {
                respond(sender, service, player, args, false);
                return;
            }
            if ("cancel".equalsIgnoreCase(args[0])) {
                cancel(sender, service, player, args);
                return;
            }
            if ("status".equalsIgnoreCase(args[0])) {
                status(sender, service, player);
                return;
            }
            String targetServerId = args.length > 1 ? args[1] : module.getLocalServerId();
            TpaRequest request = service.createTpaRequest(module.captureActor(player),
                PlayerTeleportService.newRequestId("tpa"), args[0], targetServerId, Instant.now());
            sendKey(sender, "jsirgalaxybase.servertools.tpa.created", request.getTargetPlayerName(),
                request.getTargetServerId());
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }

    private void respond(ICommandSender sender, PlayerTeleportService service, EntityPlayerMP player, String[] args,
        boolean accepting) {
        if (args.length != 2) {
            send(sender, getCommandUsage(sender));
            return;
        }
        TpaRequest request = accepting ? service.acceptTpa(module.captureActor(player), args[1], Instant.now())
            : service.declineTpa(module.captureActor(player), args[1], Instant.now());
        sendKey(sender, accepting ? "jsirgalaxybase.servertools.tpa.accepted"
            : "jsirgalaxybase.servertools.tpa.declined", request.getRequesterPlayerName());
    }

    private void cancel(ICommandSender sender, PlayerTeleportService service, EntityPlayerMP player, String[] args) {
        if (args.length < 2 || args.length > 3) {
            send(sender, getCommandUsage(sender));
            return;
        }
        String targetServerId = args.length == 3 ? args[2] : module.getLocalServerId();
        TpaRequest request = service.cancelTpa(module.captureActor(player), args[1], targetServerId, Instant.now());
        sendKey(sender, "jsirgalaxybase.servertools.tpa.cancelled", request.getTargetPlayerName(),
            request.getTargetServerId());
    }

    private void status(ICommandSender sender, PlayerTeleportService service, EntityPlayerMP player) {
        TeleportActor actor = module.captureActor(player);
        List<TpaRequest> outgoing = service.listRecentTpaRequestsForRequester(actor.getSourceServerId(),
            actor.getPlayerUuid(), 10);
        List<TpaRequest> incoming = service.listRecentTpaRequestsForTarget(actor.getSourceServerId(),
            actor.getPlayerName(), 10);
        if (outgoing.isEmpty() && incoming.isEmpty()) {
            sendKey(sender, "jsirgalaxybase.servertools.tpa.status.empty");
            return;
        }
        for (TpaRequest request : outgoing) {
            sendKey(sender, "jsirgalaxybase.servertools.tpa.status.outgoing", request.getTargetPlayerName(),
                request.getTargetServerId(), request.getStatus().name());
        }
        for (TpaRequest request : incoming) {
            sendKey(sender, "jsirgalaxybase.servertools.tpa.status.incoming", request.getRequesterPlayerName(),
                request.getRequesterServerId(), request.getStatus().name());
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<String>();
            suggestions.add("accept");
            suggestions.add("deny");
            suggestions.add("cancel");
            suggestions.add("status");
            suggestions.addAll(java.util.Arrays.asList(net.minecraft.server.MinecraftServer.getServer().getAllUsernames()));
            return getListOfStringsMatchingLastWord(args, suggestions.toArray(new String[suggestions.size()]));
        }
        if (args.length == 2 && ("accept".equalsIgnoreCase(args[0]) || "deny".equalsIgnoreCase(args[0])
            || "cancel".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args,
                net.minecraft.server.MinecraftServer.getServer().getAllUsernames());
        }
        if ((args.length == 2 && !"status".equalsIgnoreCase(args[0]))
            || (args.length == 3 && "cancel".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, module.getLocalServerId());
        }
        return emptyTabList();
    }
}
