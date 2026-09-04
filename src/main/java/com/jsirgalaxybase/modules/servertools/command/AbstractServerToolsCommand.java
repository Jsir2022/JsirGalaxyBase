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
import com.jsirgalaxybase.modules.servertools.application.ServerToolsException;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;

public abstract class AbstractServerToolsCommand extends CommandBase {

    protected final ServerToolsModule module;

    protected AbstractServerToolsCommand(ServerToolsModule module) {
        this.module = module;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    protected EntityPlayerMP requirePlayer(ICommandSender sender) {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new PlayerNotFoundException();
        }
        return (EntityPlayerMP) sender;
    }

    protected PlayerTeleportService requireService(ICommandSender sender) {
        if (!module.isRuntimeAvailable()) {
            module.sendUnavailable(sender);
            return null;
        }
        return module.getPlayerTeleportService();
    }

    protected void send(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }

    protected void sendKey(ICommandSender sender, String key, Object... args) {
        sender.addChatMessage(new ChatComponentTranslation(key, args));
    }

    protected void sendDispatchResult(ICommandSender sender, GatewayDispatchResult result, String localSuccessKey,
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

    protected EntityPlayerMP resolveLiveSubject(TeleportDispatchPlan dispatchPlan) {
        if (!module.getLocalServerId().equals(dispatchPlan.getSourceServerId())) {
            return null;
        }
        return module.findOnlinePlayer(dispatchPlan.getSubjectPlayerName());
    }

    protected List<String> emptyTabList() {
        return new ArrayList<String>();
    }

    protected void handleServiceError(ICommandSender sender, RuntimeException exception) {
        if (exception instanceof ServerToolsException || exception instanceof IllegalArgumentException
            || exception instanceof IllegalStateException) {
            sendKey(sender, "jsirgalaxybase.servertools.error.operation_failed");
            return;
        }
        throw exception;
    }
}
