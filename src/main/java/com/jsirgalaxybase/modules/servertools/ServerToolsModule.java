package com.jsirgalaxybase.modules.servertools;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.modules.cluster.ClusterModule;
import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.infrastructure.ClusterInfrastructure;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.command.BackCommand;
import com.jsirgalaxybase.modules.servertools.command.HomeCommand;
import com.jsirgalaxybase.modules.servertools.command.RtpCommand;
import com.jsirgalaxybase.modules.servertools.command.SpawnCommand;
import com.jsirgalaxybase.modules.servertools.command.TpaCommand;
import com.jsirgalaxybase.modules.servertools.command.WarpCommand;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;
import com.jsirgalaxybase.modules.servertools.infrastructure.ServerToolsInfrastructure;
import com.jsirgalaxybase.modules.servertools.infrastructure.jdbc.JdbcServerToolsInfrastructureFactory;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class ServerToolsModule extends ModModule {

    private ClusterInfrastructure clusterInfrastructure;
    private ServerToolsInfrastructure infrastructure;
    private PlayerTeleportService playerTeleportService;
    private String localServerId;

    public ServerToolsModule() {
        super("server-tools", "Server Tools", "servertools");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        if (!context.isClient() && context.getConfiguration().isBankingPostgresEnabled()) {
            localServerId = context.getConfiguration().getBankingSourceServerId();
            GalaxyBase.LOG.info(
                "Server tools module reserved for server {} and will prepare database-backed teleport services during dedicated server start",
                localServerId);
        }
    }

    @Override
    public void serverStarting(ModuleContext context, FMLServerStartingEvent event) {
        ClusterModule clusterModule = context.getModuleManager() == null ? null
            : context.getModuleManager().findModule(ClusterModule.class);
        clusterInfrastructure = clusterModule == null ? null : clusterModule.getClusterInfrastructure();
        if (clusterInfrastructure != null && infrastructure == null) {
            try {
                infrastructure = JdbcServerToolsInfrastructureFactory.createShared(
                    clusterInfrastructure.getSharedConnectionManager());
                playerTeleportService = new PlayerTeleportService(infrastructure.getPlayerTeleportRepository(),
                    infrastructure.getPlayerPermissionPolicy(), clusterInfrastructure.getServerDirectory());
                GalaxyBase.LOG.info("Server tools runtime prepared for dedicated server {}", localServerId);
            } catch (RuntimeException exception) {
                infrastructure = null;
                playerTeleportService = null;
                GalaxyBase.LOG.error("Failed to prepare server tools runtime", exception);
            }
        }

        event.registerServerCommand(new HomeCommand(this));
        event.registerServerCommand(new BackCommand(this));
        event.registerServerCommand(new SpawnCommand(this));
        event.registerServerCommand(new TpaCommand(this));
        event.registerServerCommand(new RtpCommand(this));
        event.registerServerCommand(new WarpCommand(this));
    }

    public PlayerTeleportService getPlayerTeleportService() {
        return playerTeleportService;
    }

    public ServerToolsInfrastructure getInfrastructure() {
        return infrastructure;
    }

    public String getLocalServerId() {
        return localServerId;
    }

    public boolean isRuntimeAvailable() {
        return playerTeleportService != null;
    }

    public TeleportActor captureActor(EntityPlayerMP player) {
        return new TeleportActor(player.getUniqueID().toString(), player.getCommandSenderName(), localServerId,
            com.jsirgalaxybase.modules.cluster.domain.TeleportTarget.fromPlayer(player, localServerId));
    }

    public GatewayDispatchResult dispatchTeleport(EntityPlayerMP livePlayer, TeleportDispatchPlan dispatchPlan) {
        ClusterInfrastructure clusterInfrastructure = resolveClusterInfrastructure();
        if (clusterInfrastructure == null) {
            throw new IllegalStateException("Cluster runtime is not available");
        }
        return clusterInfrastructure.getClusterTeleportService().dispatchTeleport(livePlayer,
            dispatchPlan.getRequestId(), dispatchPlan.getSubjectPlayerUuid(), dispatchPlan.getSubjectPlayerName(),
            dispatchPlan.getSourceServerId(), dispatchPlan.getTeleportKind().name(), dispatchPlan.getTarget());
    }

    public EntityPlayerMP findOnlinePlayer(String playerName) {
        MinecraftServer server = MinecraftServer.getServer();
        return server == null ? null : server.getConfigurationManager().func_152612_a(playerName);
    }

    public void sendUnavailable(ICommandSender sender) {
        sender.addChatMessage(new net.minecraft.util.ChatComponentText(
            "Server tools runtime is not available. Check dedicated server startup logs and PostgreSQL configuration."));
    }

    private ClusterInfrastructure resolveClusterInfrastructure() {
        return clusterInfrastructure;
    }
}