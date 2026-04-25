package com.jsirgalaxybase.modules.cluster;

import cpw.mods.fml.common.FMLCommonHandler;
import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.modules.cluster.infrastructure.ClusterInfrastructure;
import com.jsirgalaxybase.modules.cluster.infrastructure.ClusterRuntimeController;
import com.jsirgalaxybase.modules.cluster.infrastructure.jdbc.JdbcClusterInfrastructureFactory;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.server.MinecraftServer;

public class ClusterModule extends ModModule {

    private ClusterInfrastructure clusterInfrastructure;
    private ClusterRuntimeController runtimeController;
    private boolean clusterRequested;
    private String localServerId;

    public ClusterModule() {
        super("cluster", "Cluster", "cluster");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        if (!context.isClient() && context.getConfiguration().isBankingPostgresEnabled()) {
            clusterRequested = true;
            localServerId = context.getConfiguration().getBankingSourceServerId();
            GalaxyBase.LOG.info(
                "Cluster module reserved for server {} and will prepare shared JDBC-backed routing during dedicated server start",
                localServerId);
            return;
        }

        GalaxyBase.LOG.info("Cluster module waiting for server-side PostgreSQL configuration.");
    }

    @Override
    public void serverStarting(ModuleContext context, FMLServerStartingEvent event) {
        if (!shouldPrepareDedicatedInfrastructure()) {
            if (clusterRequested) {
                GalaxyBase.LOG.warn("Cluster runtime is disabled because the current server is not dedicated");
            }
            return;
        }

        if (!clusterRequested || clusterInfrastructure != null) {
            return;
        }

        InstitutionCoreModule institutionCoreModule = context.getModuleManager() == null ? null
            : context.getModuleManager().findModule(InstitutionCoreModule.class);
        BankingInfrastructure bankingInfrastructure = institutionCoreModule == null ? null
            : institutionCoreModule.getBankingInfrastructure();
        if (bankingInfrastructure == null || bankingInfrastructure.getSharedConnectionManager() == null) {
            GalaxyBase.LOG.warn(
                "Cluster runtime requires InstitutionCoreModule to expose a shared JDBC connection manager; startup will continue without cluster routing");
            return;
        }

        try {
            clusterInfrastructure = JdbcClusterInfrastructureFactory.createShared(
                bankingInfrastructure.getSharedConnectionManager(),
                localServerId);
            runtimeController = new ClusterRuntimeController(clusterInfrastructure.getPlayerArrivalRestoreService());
            FMLCommonHandler.instance().bus().register(runtimeController);
            GalaxyBase.LOG.info("Cluster runtime prepared for dedicated server {}", localServerId);
        } catch (RuntimeException exception) {
            clusterInfrastructure = null;
            runtimeController = null;
            GalaxyBase.LOG.error("Failed to prepare cluster runtime", exception);
        }
    }

    protected boolean shouldPrepareDedicatedInfrastructure() {
        MinecraftServer server = MinecraftServer.getServer();
        return server != null && server.isDedicatedServer();
    }

    public ClusterInfrastructure getClusterInfrastructure() {
        return clusterInfrastructure;
    }

    public String getLocalServerId() {
        return localServerId;
    }
}