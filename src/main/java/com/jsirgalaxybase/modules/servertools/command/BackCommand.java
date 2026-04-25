package com.jsirgalaxybase.modules.servertools.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;

public class BackCommand extends AbstractServerToolsCommand {

    public BackCommand(ServerToolsModule module) {
        super(module);
    }

    @Override
    public String getCommandName() {
        return "back";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/back";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        PlayerTeleportService service = requireService(sender);
        if (service == null) {
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        try {
            TeleportDispatchPlan dispatchPlan = service.prepareBackTeleport(module.captureActor(player),
                PlayerTeleportService.newRequestId("back"));
            GatewayDispatchResult result = module.dispatchTeleport(resolveLiveSubject(dispatchPlan), dispatchPlan);
            sendDispatchResult(sender, result, "Teleported to your last valid origin.");
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }
}