package com.jsirgalaxybase.modules.servertools.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.application.ServerToolsAdminService;
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
        if ("admin".equalsIgnoreCase(args[0])) {
            processAdminCommand(sender, tail(args));
            return;
        }
        sendKey(sender, "jsirgalaxybase.servertools.command.unknown", args[0], usage);
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
            sendDispatchResult(sender, result, "jsirgalaxybase.servertools.warp.teleported", args[0].toLowerCase());
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }

    public List<String> addRootTabCompletionOptions(String[] args) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(args, new String[] { "warp", "admin" });
        }
        if (args.length >= 2 && "warp".equalsIgnoreCase(args[0])) {
            return addWarpTabCompletionOptions(tail(args));
        }
        if (args.length == 2 && "admin".equalsIgnoreCase(args[0])) {
            return CommandBase.getListOfStringsMatchingLastWord(args,
                new String[] { "setwarp", "delwarp", "warp-enabled", "server-enabled" });
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

    public void processAdminCommand(ICommandSender sender, String[] args) {
        if (!sender.canCommandSenderUseCommand(2, "jgbst")) {
            sendKey(sender, "jsirgalaxybase.servertools.admin.denied");
            return;
        }
        ServerToolsAdminService admin = module.getServerToolsAdminService();
        if (admin == null) {
            sendKey(sender, "jsirgalaxybase.servertools.admin.unavailable");
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        if (args.length == 0) {
            sendKey(sender, "jsirgalaxybase.servertools.admin.usage");
            return;
        }
        try {
            String operation = args[0].toLowerCase(java.util.Locale.ROOT);
            if ("setwarp".equals(operation) && args.length >= 2) {
                String display = args.length >= 3 ? args[2] : args[1];
                String description = args.length >= 4 ? join(args, 3) : "";
                com.jsirgalaxybase.modules.servertools.domain.ServerWarp warp = admin.setLocalWarp(module.captureActor(player),
                    PlayerTeleportService.newRequestId("admin-set-warp"), args[1], display, description);
                sendKey(sender, "jsirgalaxybase.servertools.admin.warp_saved", warp.getWarpName());
                return;
            }
            if ("delwarp".equals(operation) && args.length == 2) {
                admin.deleteWarp(module.captureActor(player), PlayerTeleportService.newRequestId("admin-del-warp"), args[1]);
                sendKey(sender, "jsirgalaxybase.servertools.admin.warp_deleted", args[1].toLowerCase());
                return;
            }
            if ("warp-enabled".equals(operation) && args.length == 3) {
                boolean enabled = parseEnabled(args[2]);
                admin.setWarpEnabled(module.captureActor(player), PlayerTeleportService.newRequestId("admin-warp-state"), args[1], enabled);
                sendKey(sender, "jsirgalaxybase.servertools.admin.warp_state", args[1].toLowerCase(), Boolean.valueOf(enabled));
                return;
            }
            if ("server-enabled".equals(operation) && args.length == 3) {
                boolean enabled = parseEnabled(args[2]);
                com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor descriptor = admin.setServerEnabled(
                    module.captureActor(player), PlayerTeleportService.newRequestId("admin-server-state"), args[1], enabled);
                sendKey(sender, "jsirgalaxybase.servertools.admin.server_state", descriptor.getServerId(), Boolean.valueOf(enabled));
                return;
            }
            sendKey(sender, "jsirgalaxybase.servertools.admin.usage");
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
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
            sendKey(sender, "jsirgalaxybase.servertools.warp.empty");
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < warps.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(warps.get(i).getWarpName());
        }
        sendKey(sender, "jsirgalaxybase.servertools.warp.list", builder.toString());
    }

    private void sendDispatchResult(ICommandSender sender, GatewayDispatchResult result, String localSuccessKey,
        Object... localSuccessArgs) {
        if (result.getStatus() == GatewayDispatchResult.Status.COMPLETED_LOCAL) {
            sendKey(sender, localSuccessKey, localSuccessArgs);
            return;
        }
        if (result.getStatus() == GatewayDispatchResult.Status.PENDING_REMOTE) {
            sendKey(sender, "jsirgalaxybase.servertools.transfer.pending");
            return;
        }
        sendKey(sender, "jsirgalaxybase.servertools.transfer.failed");
    }

    private void handleServiceError(ICommandSender sender, RuntimeException exception) {
        if (exception instanceof ServerToolsException || exception instanceof IllegalArgumentException
            || exception instanceof IllegalStateException) {
            sendKey(sender, "jsirgalaxybase.servertools.error.operation_failed");
            return;
        }
        throw exception;
    }

    private String[] tail(String[] args) {
        String[] tail = new String[args.length - 1];
        System.arraycopy(args, 1, tail, 0, tail.length);
        return tail;
    }

    private boolean parseEnabled(String value) {
        if ("true".equalsIgnoreCase(value) || "enable".equalsIgnoreCase(value) || "enabled".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value) || "disable".equalsIgnoreCase(value) || "disabled".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException("enabled must be true or false");
    }

    private String join(String[] values, int start) {
        StringBuilder joined = new StringBuilder();
        for (int i = start; i < values.length; i++) { if (i > start) joined.append(' '); joined.append(values[i]); }
        return joined.toString();
    }

    private void send(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }

    private void sendKey(ICommandSender sender, String key, Object... args) {
        sender.addChatMessage(new ChatComponentTranslation(key, args));
    }

    private List<String> emptyTabList() {
        return new ArrayList<String>();
    }
}
