package com.jsirgalaxybase.modules.servertools.infrastructure.minecraft;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.servertools.application.ServerToolsException;
import com.jsirgalaxybase.modules.servertools.domain.TargetServerRtpProfile;
import com.jsirgalaxybase.modules.servertools.port.RtpCandidateFinder;

/** Target-server-only safe-block sampling for a configured RTP area. */
public final class MinecraftRtpCandidateFinder implements RtpCandidateFinder {

    private static final int MAX_TRIES = 32;
    private static final List<Block> UNSAFE_BLOCKS = Arrays.asList(Blocks.cactus, Blocks.fire, Blocks.lava,
        Blocks.water, Blocks.flowing_lava, Blocks.flowing_water);

    @Override
    public TeleportTarget findSafeTarget(TargetServerRtpProfile profile, EntityPlayerMP player) {
        MinecraftServer server = MinecraftServer.getServer();
        World world = server == null ? null : server.worldServerForDimension(profile.getDimensionId());
        if (world == null) throw new ServerToolsException("Configured target RTP dimension is unavailable");
        for (int depth = 0; depth < MAX_TRIES; depth++) {
            double distance = profile.getMinDistance() + world.rand.nextDouble()
                * (profile.getMaxDistance() - profile.getMinDistance());
            double angle = world.rand.nextDouble() * Math.PI * 2.0D;
            int x = MathHelper.floor_double(profile.getCenterX() + Math.cos(angle) * distance);
            int z = MathHelper.floor_double(profile.getCenterZ() + Math.sin(angle) * distance);
            int y = Math.min(world.getActualHeight(), 256);
            while (y > 1) {
                y--;
                Block feet = world.getBlock(x, y, z);
                Block head = world.getBlock(x, y + 1, z);
                Block aboveHead = world.getBlock(x, y + 2, z);
                if (!feet.equals(Blocks.air) && head.equals(Blocks.air) && aboveHead.equals(Blocks.air)
                    && !UNSAFE_BLOCKS.contains(feet)) {
                    float yaw = player == null ? 0.0F : player.rotationYaw;
                    float pitch = player == null ? 0.0F : player.rotationPitch;
                    return new TeleportTarget(profile.getServerId(), profile.getDimensionId(), x + 0.5D, y + 1.1D,
                        z + 0.5D, yaw, pitch);
                }
            }
        }
        throw new ServerToolsException("Failed to find a safe RTP target in configured target-server area");
    }
}
