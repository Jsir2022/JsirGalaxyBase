package com.jsirgalaxybase.modules.servertools.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;
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
        return "/tpa <playerName> [targetServerId] | /tpa accept <playerName>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        PlayerTeleportService service = requireService(sender);
        if (service == null) {
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        try {
            if (args.length >= 2 && "accept".equalsIgnoreCase(args[0])) {
                TeleportDispatchPlan dispatchPlan = service.acceptTpa(module.captureActor(player),
                    PlayerTeleportService.newRequestId("tpa-accept"), args[1], Instant.now());
                GatewayDispatchResult result = module.dispatchTeleport(resolveLiveSubject(dispatchPlan), dispatchPlan);
                sendDispatchResult(sender, result, "Accepted TPA request from " + args[1] + ".");
                return;
            }
            if (args.length == 0) {
                send(sender, getCommandUsage(sender));
                return;
            }
            String targetServerId = args.length > 1 ? args[1] : module.getLocalServerId();
            TpaRequest request = service.createTpaRequest(module.captureActor(player),
                PlayerTeleportService.newRequestId("tpa"), args[0], targetServerId, Instant.now());
            send(sender, "TPA request recorded for " + request.getTargetPlayerName() + " on server "
                + request.getTargetServerId() + ". The target can accept with /tpa accept "
                + request.getRequesterPlayerName());
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<String>();
            suggestions.add("accept");
            suggestions.addAll(java.util.Arrays.asList(net.minecraft.server.MinecraftServer.getServer().getAllUsernames()));
            return getListOfStringsMatchingLastWord(args, suggestions.toArray(new String[suggestions.size()]));
        }
        if (args.length == 2 && "accept".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args,
                net.minecraft.server.MinecraftServer.getServer().getAllUsernames());
        }
        if (args.length == 2) {
            List<String> serverIds = new ArrayList<String>();
            serverIds.add(module.getLocalServerId());
            return getListOfStringsMatchingLastWord(args, serverIds.toArray(new String[serverIds.size()]));
        }
        return emptyTabList();
    }
}