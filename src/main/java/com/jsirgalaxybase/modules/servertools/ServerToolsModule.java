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
import com.jsirgalaxybase.modules.servertools.application.ServerToolsAdminService;
import com.jsirgalaxybase.modules.servertools.command.BackCommand;
import com.jsirgalaxybase.modules.servertools.command.DelHomeCommand;
import com.jsirgalaxybase.modules.servertools.command.HomeCommand;
import com.jsirgalaxybase.modules.servertools.command.JgbServerToolsCommand;
import com.jsirgalaxybase.modules.servertools.command.RtpCommand;
import com.jsirgalaxybase.modules.servertools.command.SetHomeCommand;
import com.jsirgalaxybase.modules.servertools.command.SpawnCommand;
import com.jsirgalaxybase.modules.servertools.command.TpaCommand;
import com.jsirgalaxybase.modules.servertools.command.TpaResponseCommand;
import com.jsirgalaxybase.modules.servertools.command.WarpCommand;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;
import com.jsirgalaxybase.modules.servertools.domain.GlobalEntryRules;
import com.jsirgalaxybase.modules.servertools.infrastructure.ServerToolsInfrastructure;
import com.jsirgalaxybase.modules.servertools.infrastructure.ServerToolsRuntimeController;
import com.jsirgalaxybase.modules.servertools.infrastructure.TargetServerRtpArrivalResolver;
import com.jsirgalaxybase.modules.servertools.infrastructure.jdbc.JdbcServerToolsInfrastructureFactory;
import com.jsirgalaxybase.modules.servertools.infrastructure.jdbc.JdbcServerToolsAdminRepository;
import com.jsirgalaxybase.modules.servertools.infrastructure.minecraft.MinecraftRtpCandidateFinder;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class ServerToolsModule extends ModModule {

    private ClusterInfrastructure clusterInfrastructure;
    private ServerToolsInfrastructure infrastructure;
    private PlayerTeleportService playerTeleportService;
    private ServerToolsAdminService serverToolsAdminService;
    private ServerToolsRuntimeController runtimeController;
    private String localServerId;
    private GlobalEntryRules globalEntryRules = GlobalEntryRules.parse("", new String[0]);

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
                globalEntryRules = GlobalEntryRules.parse(context.getConfiguration().getGlobalHubTarget(),
                    context.getConfiguration().getTargetServerRtpProfiles());
                infrastructure = JdbcServerToolsInfrastructureFactory.createShared(
                    clusterInfrastructure.getSharedConnectionManager());
                playerTeleportService = new PlayerTeleportService(infrastructure.getPlayerTeleportRepository(),
                    infrastructure.getPlayerPermissionPolicy(), clusterInfrastructure.getServerDirectory());
                serverToolsAdminService = new ServerToolsAdminService(
                    new JdbcServerToolsAdminRepository(clusterInfrastructure.getSharedConnectionManager()));
                clusterInfrastructure.getPlayerArrivalRestoreService().setArrivalTargetResolver(
                    new TargetServerRtpArrivalResolver(localServerId, globalEntryRules,
                        new MinecraftRtpCandidateFinder(), playerTeleportService));
                GalaxyBase.LOG.info("Server tools runtime prepared for dedicated server {}", localServerId);
            } catch (RuntimeException exception) {
                infrastructure = null;
                playerTeleportService = null;
                serverToolsAdminService = null;
                GalaxyBase.LOG.error("Failed to prepare server tools runtime", exception);
            }
        }

        event.registerServerCommand(new HomeCommand(this));
        event.registerServerCommand(new SetHomeCommand(this));
        event.registerServerCommand(new DelHomeCommand(this));
        event.registerServerCommand(new BackCommand(this));
        event.registerServerCommand(new SpawnCommand(this));
        event.registerServerCommand(new TpaCommand(this));
        event.registerServerCommand(new TpaResponseCommand(this, true));
        event.registerServerCommand(new TpaResponseCommand(this, false));
        event.registerServerCommand(new RtpCommand(this));
        event.registerServerCommand(new WarpCommand(this));
        event.registerServerCommand(new JgbServerToolsCommand(this));
        if (playerTeleportService != null && runtimeController == null) {
            runtimeController = new ServerToolsRuntimeController(this);
            FMLCommonHandler.instance().bus().register(runtimeController);
            GalaxyBase.LOG.info("Server tools TPA runtime controller registered for dedicated server {}", localServerId);
        }
        GalaxyBase.LOG.info("Registered /jgbst server tools command");
        GalaxyBase.LOG.info("Registered /jsirgalaxybase servertools route");
    }

    public PlayerTeleportService getPlayerTeleportService() {
        return playerTeleportService;
    }

    public ServerToolsInfrastructure getInfrastructure() {
        return infrastructure;
    }

    public ServerToolsAdminService getServerToolsAdminService() {
        return serverToolsAdminService;
    }

    public ClusterInfrastructure getClusterInfrastructure() {
        return clusterInfrastructure;
    }

    public String getLocalServerId() {
        return localServerId;
    }

    public GlobalEntryRules getGlobalEntryRules() { return globalEntryRules; }

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
        sender.addChatMessage(new net.minecraft.util.ChatComponentTranslation(
            "jsirgalaxybase.servertools.runtime.unavailable"));
    }

    private ClusterInfrastructure resolveClusterInfrastructure() {
        return clusterInfrastructure;
    }
}
