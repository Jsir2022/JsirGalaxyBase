package com.jsirgalaxybase.modules.servertools.command;

import java.time.Instant;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.application.ServerToolsException;
import com.jsirgalaxybase.modules.servertools.domain.TargetServerRtpProfile;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;
import com.jsirgalaxybase.modules.servertools.infrastructure.minecraft.MinecraftRtpCandidateFinder;

public class RtpCommand extends AbstractServerToolsCommand {

    private static final double MIN_DISTANCE = 256.0D;
    private static final double MAX_DISTANCE = 2048.0D;

    public RtpCommand(ServerToolsModule module) {
        super(module);
    }

    @Override
    public String getCommandName() {
        return "rtp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/rtp [targetServerId]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        PlayerTeleportService service = requireService(sender);
        if (service == null) {
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        try {
            String requestedServer = args != null && args.length > 0 ? args[0].trim() : module.getLocalServerId();
            String requestId = PlayerTeleportService.newRequestId("rtp");
            TeleportDispatchPlan dispatchPlan;
            if (requestedServer.isEmpty() || module.getLocalServerId().equals(requestedServer)) {
                TeleportTarget target = findSafeRandomTarget(player.worldObj, player);
                dispatchPlan = service.prepareRandomTeleport(module.captureActor(player), requestId, target, Instant.now());
            } else {
                TargetServerRtpProfile profile = module.getGlobalEntryRules().findRtpProfile(requestedServer).orElse(null);
                if (profile == null) throw new ServerToolsException("Target server RTP is not configured: " + requestedServer);
                dispatchPlan = service.prepareTargetServerRandomTeleport(module.captureActor(player), requestId, profile,
                    Instant.now());
            }
            GatewayDispatchResult result = module.dispatchTeleport(resolveLiveSubject(dispatchPlan), dispatchPlan);
            sendDispatchResult(sender, result, "jsirgalaxybase.servertools.rtp.teleported");
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }

    private TeleportTarget findSafeRandomTarget(World world, EntityPlayerMP player) {
        return new MinecraftRtpCandidateFinder().findSafeTarget(new TargetServerRtpProfile(module.getLocalServerId(),
            world.provider.dimensionId, player.posX, player.posY, player.posZ, MIN_DISTANCE, MAX_DISTANCE), player);
    }
}
