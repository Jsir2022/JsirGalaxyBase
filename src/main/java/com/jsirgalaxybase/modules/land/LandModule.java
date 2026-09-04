package com.jsirgalaxybase.modules.land;

import net.minecraftforge.common.MinecraftForge;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.land.application.LandProtectionIndex;
import com.jsirgalaxybase.modules.land.application.LandProtectionPolicy;
import com.jsirgalaxybase.modules.land.application.LandProtectionRuntime;
import com.jsirgalaxybase.modules.land.application.PersonalLandRules;
import com.jsirgalaxybase.modules.land.application.PersonalLandRulesFactory;
import com.jsirgalaxybase.modules.land.application.PersonalLandService;
import com.jsirgalaxybase.modules.land.domain.LandProtectionMode;
import com.jsirgalaxybase.modules.land.infrastructure.LandInfrastructure;
import com.jsirgalaxybase.modules.land.infrastructure.jdbc.JdbcLandInfrastructureFactory;
import com.jsirgalaxybase.modules.land.infrastructure.minecraft.LandProtectionEventHandler;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public final class LandModule extends ModModule {

    private PersonalLandService personalLandService;
    private LandInfrastructure infrastructure;
    private LandProtectionRuntime protectionRuntime;
    private LandProtectionEventHandler eventHandler;
    private boolean requested;
    private String localServerId;
    private int maxClaimsPerPlayer;

    public LandModule() {
        super("land", "Personal Land", "core");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        if (context.isClient() || !context.getConfiguration().isLandEnabled()) {
            GalaxyBase.LOG.info("Personal land runtime is disabled on this side");
            return;
        }
        requested = true;
        localServerId = requireText(context.getConfiguration().getBankingSourceServerId(), "bankingSourceServerId");
        maxClaimsPerPlayer = context.getConfiguration().getLandMaxClaimsPerPlayer();
        LandProtectionMode.parseStrict(context.getConfiguration().getLandProtectionMode());
        PersonalLandRulesFactory.fromConfiguration(context.getConfiguration(), localServerId);
        GalaxyBase.LOG.info("Personal land runtime requested for server {} mode={}", localServerId,
            context.getConfiguration().getLandProtectionMode());
    }

    @Override
    public void serverStarting(ModuleContext context, FMLServerStartingEvent event) {
        if (!requested) return;
        InstitutionCoreModule core = context.getModuleManager() == null ? null
            : context.getModuleManager().findModule(InstitutionCoreModule.class);
        BankingInfrastructure banking = core == null ? null : core.getBankingInfrastructure();
        if (banking == null || banking.getSharedConnectionManager() == null) {
            throw new IllegalStateException(
                "Personal land is enabled but InstitutionCore shared PostgreSQL infrastructure is unavailable");
        }

        infrastructure = JdbcLandInfrastructureFactory.createShared(banking.getSharedConnectionManager());
        PersonalLandRules rules = PersonalLandRulesFactory.fromConfiguration(context.getConfiguration(), localServerId);
        LandProtectionIndex index = new LandProtectionIndex();
        personalLandService = new PersonalLandService(infrastructure.getRepository(), rules, index,
            infrastructure.getTransactionRunner(), localServerId);
        LandProtectionMode mode = LandProtectionMode.parseStrict(context.getConfiguration().getLandProtectionMode());
        protectionRuntime = new LandProtectionRuntime(
            new LandProtectionPolicy(index, context.getConfiguration().isLandAllowFakePlayers()), mode);
        eventHandler = new LandProtectionEventHandler(localServerId, protectionRuntime);
        MinecraftForge.EVENT_BUS.register(eventHandler);
        GalaxyBase.LOG.info("Personal land runtime ready server={} mode={} activeTitles={} fakePlayersAllowed={}",
            localServerId, mode, index.size(), context.getConfiguration().isLandAllowFakePlayers());
    }

    public PersonalLandService getPersonalLandService() {
        return personalLandService;
    }

    public LandInfrastructure getInfrastructure() {
        return infrastructure;
    }

    public LandProtectionRuntime getProtectionRuntime() {
        return protectionRuntime;
    }

    public boolean isRuntimeAvailable() {
        return personalLandService != null && protectionRuntime != null;
    }

    public String getLocalServerId() {
        return localServerId;
    }

    public int getMaxClaimsPerPlayer() {
        return maxClaimsPerPlayer;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
