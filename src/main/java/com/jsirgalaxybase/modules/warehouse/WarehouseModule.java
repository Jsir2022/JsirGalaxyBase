package com.jsirgalaxybase.modules.warehouse;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.common.MinecraftForge;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.warehouse.application.WarehouseDriveService;
import com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseBayService;
import com.jsirgalaxybase.modules.warehouse.infrastructure.WarehouseInfrastructure;
import com.jsirgalaxybase.modules.warehouse.infrastructure.jdbc.JdbcWarehouseInfrastructureFactory;
import com.jsirgalaxybase.modules.warehouse.infrastructure.jdbc.JdbcTerminalWarehouseBayRepository;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.WarehouseBlockRegistry;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.WarehouseDriveEventHandler;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.WarehouseDriveRuntime;

/** Personal-only AE2 Drive module. Port, organisation storage and remote AE remain outside this module. */
public final class WarehouseModule extends ModModule {
    private boolean requested;
    private String localServerId;
    private WarehouseInfrastructure infrastructure;
    private WarehouseDriveService driveService;
    private TerminalWarehouseBayService terminalBayService;

    public WarehouseModule() { super("warehouse", "AE2 Galactic Warehouse", "core"); }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        WarehouseBlockRegistry.registerIfAe2Present();
        if (context.isClient() || !context.getConfiguration().isWarehouseEnabled()) {
            GalaxyBase.LOG.info("Personal Warehouse Drive runtime is disabled on this side");
            return;
        }
        if (!Loader.isModLoaded("appliedenergistics2")) {
            throw new IllegalStateException("warehouseEnabled=true requires Applied Energistics 2");
        }
        localServerId = required(context.getConfiguration().getBankingSourceServerId());
        requested = true;
    }

    @Override
    public void serverStarting(ModuleContext context, FMLServerStartingEvent event) {
        if (!requested) return;
        InstitutionCoreModule core = context.getModuleManager().findModule(InstitutionCoreModule.class);
        BankingInfrastructure banking = core == null ? null : core.getBankingInfrastructure();
        if (banking == null || banking.getSharedConnectionManager() == null) {
            throw new IllegalStateException("warehouseEnabled=true requires InstitutionCore shared PostgreSQL infrastructure");
        }
        infrastructure = JdbcWarehouseInfrastructureFactory.createShared(banking.getSharedConnectionManager());
        driveService = new WarehouseDriveService(infrastructure.getRepository(), infrastructure.getTransactionRunner(), localServerId);
        terminalBayService = new TerminalWarehouseBayService(new JdbcTerminalWarehouseBayRepository(
            banking.getSharedConnectionManager()), infrastructure.getTransactionRunner(), localServerId);
        WarehouseDriveRuntime.install(driveService, localServerId);
        MinecraftForge.EVENT_BUS.register(new WarehouseDriveEventHandler());
        GalaxyBase.LOG.info("Personal AE2 Warehouse Drive runtime ready for server={}", localServerId);
    }

    public boolean isRuntimeAvailable() { return driveService != null; }
    public WarehouseDriveService getDriveService() { return driveService; }
    public TerminalWarehouseBayService getTerminalBayService() { return terminalBayService; }
    public String getLocalServerId() { return localServerId; }

    private static String required(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("bankingSourceServerId is required");
        return value.trim();
    }
}
