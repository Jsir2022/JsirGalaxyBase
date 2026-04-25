package com.jsirgalaxybase.modules.servertools.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;

public class SpawnCommand extends AbstractServerToolsCommand {

    public SpawnCommand(ServerToolsModule module) {
        super(module);
    }

    @Override
    public String getCommandName() {
        return "spawn";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/spawn";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        PlayerTeleportService service = requireService(sender);
        if (service == null) {
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        try {
            TeleportTarget spawnTarget = resolveSpawnTarget(player.worldObj);
            TeleportDispatchPlan dispatchPlan = service.prepareSpawnTeleport(module.captureActor(player),
                PlayerTeleportService.newRequestId("spawn"), spawnTarget);
            GatewayDispatchResult result = module.dispatchTeleport(resolveLiveSubject(dispatchPlan), dispatchPlan);
            sendDispatchResult(sender, result, "Teleported to world spawn.");
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }

    private TeleportTarget resolveSpawnTarget(World world) {
        ChunkCoordinates spawn = world.getSpawnPoint();
        int y = spawn.posY;
        while (world.getBlock(spawn.posX, y, spawn.posZ).isNormalCube()) {
            y += 2;
        }
        return new TeleportTarget(module.getLocalServerId(), world.provider.dimensionId, spawn.posX + 0.5D,
            y + 0.1D, spawn.posZ + 0.5D, 0.0F, 0.0F);
    }
}