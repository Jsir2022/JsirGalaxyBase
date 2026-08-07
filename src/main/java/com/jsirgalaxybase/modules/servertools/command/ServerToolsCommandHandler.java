package com.jsirgalaxybase.modules.servertools.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.application.ServerToolsException;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;

public class ServerToolsCommandHandler {

    private final ServerToolsModule module;

    public ServerToolsCommandHandler(ServerToolsModule module) {
        this.module = module;
    }

    public void processRootCommand(ICommandSender sender, String[] args, String usage) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            send(sender, usage);
            return;
        }
        if ("warp".equalsIgnoreCase(args[0])) {
            processWarpCommand(sender, tail(args));
            return;
        }
        send(sender, "Unknown server tools command: " + args[0] + ". Usage: " + usage);
    }

    public void processWarpCommand(ICommandSender sender, String[] args) {
        PlayerTeleportService service = requireService(sender);
        if (service == null) {
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        try {
            if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
                sendWarpList(sender, service.listWarps());
                return;
            }

            TeleportDispatchPlan dispatchPlan = service.prepareWarpTeleport(module.captureActor(player),
                PlayerTeleportService.newRequestId("warp"), args[0]);
            GatewayDispatchResult result = module.dispatchTeleport(resolveLiveSubject(dispatchPlan), dispatchPlan);
            sendDispatchResult(sender, result, "Local teleport completed: warp " + args[0].toLowerCase());
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }

    public List<String> addRootTabCompletionOptions(String[] args) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(args, new String[] { "warp" });
        }
        if (args.length >= 2 && "warp".equalsIgnoreCase(args[0])) {
            return addWarpTabCompletionOptions(tail(args));
        }
        return emptyTabList();
    }

    public List<String> addWarpTabCompletionOptions(String[] args) {
        if (module.getPlayerTeleportService() == null || args.length != 1) {
            return emptyTabList();
        }
        List<String> suggestions = new ArrayList<String>();
        suggestions.add("list");
        for (ServerWarp warp : module.getPlayerTeleportService().listWarps()) {
            suggestions.add(warp.getWarpName());
        }
        return CommandBase.getListOfStringsMatchingLastWord(args,
            suggestions.toArray(new String[suggestions.size()]));
    }

    private PlayerTeleportService requireService(ICommandSender sender) {
        if (!module.isRuntimeAvailable()) {
            module.sendUnavailable(sender);
            return null;
        }
        return module.getPlayerTeleportService();
    }

    private EntityPlayerMP requirePlayer(ICommandSender sender) {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new PlayerNotFoundException();
        }
        return (EntityPlayerMP) sender;
    }

    private EntityPlayerMP resolveLiveSubject(TeleportDispatchPlan dispatchPlan) {
        if (!module.getLocalServerId().equals(dispatchPlan.getSourceServerId())) {
            return null;
        }
        return module.findOnlinePlayer(dispatchPlan.getSubjectPlayerName());
    }

    private void sendWarpList(ICommandSender sender, List<ServerWarp> warps) {
        if (warps.isEmpty()) {
            send(sender, "No warps are configured.");
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < warps.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(warps.get(i).getWarpName());
        }
        send(sender, "Warps: " + builder.toString());
    }

    private void sendDispatchResult(ICommandSender sender, GatewayDispatchResult result, String localSuccessText) {
        if (result.getStatus() == GatewayDispatchResult.Status.COMPLETED_LOCAL) {
            send(sender, localSuccessText);
            return;
        }
        if (result.getStatus() == GatewayDispatchResult.Status.PENDING_REMOTE) {
            send(sender, "Transfer ticket created / pending remote: " + result.getMessage());
            return;
        }
        send(sender, result.getMessage() == null ? "Teleport failed" : result.getMessage());
    }

    private void handleServiceError(ICommandSender sender, RuntimeException exception) {
        if (exception instanceof ServerToolsException || exception instanceof IllegalArgumentException
            || exception instanceof IllegalStateException) {
            send(sender, exception.getMessage());
            return;
        }
        throw exception;
    }

    private String[] tail(String[] args) {
        String[] tail = new String[args.length - 1];
        System.arraycopy(args, 1, tail, 0, tail.length);
        return tail;
    }

    private void send(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }

    private List<String> emptyTabList() {
        return new ArrayList<String>();
    }
}
