package com.jsirgalaxybase.modules.servertools.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;

public class WarpCommand extends AbstractServerToolsCommand {

    public WarpCommand(ServerToolsModule module) {
        super(module);
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
        PlayerTeleportService service = requireService(sender);
        if (service == null) {
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        try {
            if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
                List<ServerWarp> warps = service.listWarps();
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
                return;
            }

            TeleportDispatchPlan dispatchPlan = service.prepareWarpTeleport(module.captureActor(player),
                PlayerTeleportService.newRequestId("warp"), args[0]);
            GatewayDispatchResult result = module.dispatchTeleport(resolveLiveSubject(dispatchPlan), dispatchPlan);
            sendDispatchResult(sender, result, "Teleported to warp: " + args[0].toLowerCase());
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (module.getPlayerTeleportService() == null || args.length != 1) {
            return emptyTabList();
        }
        List<String> suggestions = new ArrayList<String>();
        suggestions.add("list");
        for (ServerWarp warp : module.getPlayerTeleportService().listWarps()) {
            suggestions.add(warp.getWarpName());
        }
        return getListOfStringsMatchingLastWord(args, suggestions.toArray(new String[suggestions.size()]));
    }
}