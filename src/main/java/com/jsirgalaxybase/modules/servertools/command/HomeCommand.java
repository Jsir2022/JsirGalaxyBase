package com.jsirgalaxybase.modules.servertools.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.PlayerHome;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;

public class HomeCommand extends AbstractServerToolsCommand {

    public HomeCommand(ServerToolsModule module) {
        super(module);
    }

    @Override
    public String getCommandName() {
        return "home";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/home [name|list|set <name>|delete <name>]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        PlayerTeleportService service = requireService(sender);
        if (service == null) {
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        try {
            if (args.length > 0 && "list".equalsIgnoreCase(args[0])) {
                List<PlayerHome> homes = service.listHomes(player.getUniqueID().toString());
                if (homes.isEmpty()) {
                    sendKey(sender, "jsirgalaxybase.servertools.home.empty");
                    return;
                }
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < homes.size(); i++) {
                    if (i > 0) {
                        builder.append(", ");
                    }
                    builder.append(homes.get(i).getHomeName());
                }
                sendKey(sender, "jsirgalaxybase.servertools.home.list", builder.toString());
                return;
            }
            if (args.length > 0 && "set".equalsIgnoreCase(args[0])) {
                String homeName = args.length > 1 ? args[1] : "home";
                PlayerHome home = service.setHome(module.captureActor(player), homeName);
                sendKey(sender, "jsirgalaxybase.servertools.home.saved", home.getHomeName(),
                    home.getTarget().getServerId(), Integer.valueOf(home.getTarget().getDimensionId()));
                return;
            }
            if (args.length > 0 && ("delete".equalsIgnoreCase(args[0]) || "del".equalsIgnoreCase(args[0]))) {
                String homeName = args.length > 1 ? args[1] : "home";
                if (service.deleteHome(player.getUniqueID().toString(), homeName)) {
                    sendKey(sender, "jsirgalaxybase.servertools.home.deleted", homeName.toLowerCase());
                    return;
                }
                sendKey(sender, "jsirgalaxybase.servertools.home.not_found", homeName);
                return;
            }

            String homeName = args.length > 0 ? args[0] : "home";
            TeleportDispatchPlan dispatchPlan = service.prepareHomeTeleport(module.captureActor(player),
                PlayerTeleportService.newRequestId("home"), homeName);
            GatewayDispatchResult result = module.dispatchTeleport(resolveLiveSubject(dispatchPlan), dispatchPlan);
            sendDispatchResult(sender, result, "jsirgalaxybase.servertools.home.teleported", homeName.toLowerCase());
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        PlayerTeleportService service = module.getPlayerTeleportService();
        if (service == null || !(sender instanceof EntityPlayerMP)) {
            return emptyTabList();
        }
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<String>();
            suggestions.add("list");
            suggestions.add("set");
            suggestions.add("delete");
            for (PlayerHome home : service.listHomes(((EntityPlayerMP) sender).getUniqueID().toString())) {
                suggestions.add(home.getHomeName());
            }
            return getListOfStringsMatchingLastWord(args, suggestions.toArray(new String[suggestions.size()]));
        }
        if (args.length == 2 && ("delete".equalsIgnoreCase(args[0]) || "del".equalsIgnoreCase(args[0]))) {
            List<String> suggestions = new ArrayList<String>();
            for (PlayerHome home : service.listHomes(((EntityPlayerMP) sender).getUniqueID().toString())) {
                suggestions.add(home.getHomeName());
            }
            return getListOfStringsMatchingLastWord(args, suggestions.toArray(new String[suggestions.size()]));
        }
        return emptyTabList();
    }
}
