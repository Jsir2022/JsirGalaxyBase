package com.jsirgalaxybase.modules.servertools.command;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.application.ServerToolsException;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;

public class RtpCommand extends AbstractServerToolsCommand {

    private static final int MAX_TRIES = 32;
    private static final double MIN_DISTANCE = 256.0D;
    private static final double MAX_DISTANCE = 2048.0D;
    private static final List<Block> UNSAFE_BLOCKS = Arrays.asList(Blocks.cactus, Blocks.fire, Blocks.lava,
        Blocks.water, Blocks.flowing_lava, Blocks.flowing_water);

    public RtpCommand(ServerToolsModule module) {
        super(module);
    }

    @Override
    public String getCommandName() {
        return "rtp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/rtp";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        PlayerTeleportService service = requireService(sender);
        if (service == null) {
            return;
        }
        EntityPlayerMP player = requirePlayer(sender);
        try {
            TeleportTarget target = findSafeRandomTarget(player.worldObj, player);
            TeleportDispatchPlan dispatchPlan = service.prepareRandomTeleport(module.captureActor(player),
                PlayerTeleportService.newRequestId("rtp"), target, Instant.now());
            GatewayDispatchResult result = module.dispatchTeleport(resolveLiveSubject(dispatchPlan), dispatchPlan);
            sendDispatchResult(sender, result, "Random teleport completed.");
        } catch (RuntimeException exception) {
            handleServiceError(sender, exception);
        }
    }

    private TeleportTarget findSafeRandomTarget(World world, EntityPlayerMP player) {
        for (int depth = 0; depth < MAX_TRIES; depth++) {
            double dist = MIN_DISTANCE + world.rand.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            double angle = world.rand.nextDouble() * Math.PI * 2.0D;
            int x = MathHelper.floor_double(player.posX + Math.cos(angle) * dist);
            int z = MathHelper.floor_double(player.posZ + Math.sin(angle) * dist);
            int y = Math.min(world.getActualHeight(), 256);
            while (y > 1) {
                y--;
                Block feet = world.getBlock(x, y, z);
                Block head = world.getBlock(x, y + 1, z);
                Block aboveHead = world.getBlock(x, y + 2, z);
                if (!feet.equals(Blocks.air) && head.equals(Blocks.air) && aboveHead.equals(Blocks.air)
                    && !UNSAFE_BLOCKS.contains(feet)) {
                    return new TeleportTarget(module.getLocalServerId(), world.provider.dimensionId, x + 0.5D,
                        y + 1.1D, z + 0.5D, player.rotationYaw, player.rotationPitch);
                }
            }
        }
        throw new ServerToolsException("Failed to find a safe RTP target in the current world");
    }
}