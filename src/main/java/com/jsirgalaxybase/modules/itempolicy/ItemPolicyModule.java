package com.jsirgalaxybase.modules.itempolicy;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.itempolicy.application.ItemPolicyConfiguration;
import com.jsirgalaxybase.modules.itempolicy.application.ItemPolicyRuntime;
import com.jsirgalaxybase.modules.itempolicy.application.ItemPolicyService;
import com.jsirgalaxybase.modules.itempolicy.infrastructure.jdbc.JdbcItemPolicyInfrastructureFactory;
import com.jsirgalaxybase.modules.itempolicy.infrastructure.jdbc.JdbcItemPolicyAuditQuery;
import com.jsirgalaxybase.modules.itempolicy.port.ItemPolicyAuditQuery;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyRule;

import java.util.Collections;
import java.util.List;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

/** Optional server-only policy module. Enabled mode fails closed at startup. */
public final class ItemPolicyModule extends ModModule {

    private boolean requested;
    private ItemPolicyService service;
    private ItemPolicyAuditQuery auditQuery;
    private List<ItemPolicyRule> configuredRules = Collections.emptyList();

    public ItemPolicyModule() { super("item-policy", "Item Admission Policy", "core"); }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        ItemPolicyRuntime.clear();
        if (context.isClient() || !context.getConfiguration().isItemPolicyEnabled()) {
            GalaxyBase.LOG.info("Item admission policy is disabled on this side");
            return;
        }
        configuredRules = ItemPolicyConfiguration.parseRules(true, context.getConfiguration().getItemPolicyRules());
        requested = true;
    }

    @Override
    public void serverStarting(ModuleContext context, FMLServerStartingEvent event) {
        if (!requested) return;
        InstitutionCoreModule core = context.getModuleManager().findModule(InstitutionCoreModule.class);
        BankingInfrastructure banking = core == null ? null : core.getBankingInfrastructure();
        if (banking == null || banking.getSharedConnectionManager() == null) {
            throw new IllegalStateException("Item policy is enabled but InstitutionCore shared PostgreSQL infrastructure is unavailable");
        }
        service = new ItemPolicyService(
            configuredRules,
            JdbcItemPolicyInfrastructureFactory.createShared(banking.getSharedConnectionManager()),
            context.getConfiguration().getBankingSourceServerId());
        ItemPolicyRuntime.install(service);
        auditQuery = new JdbcItemPolicyAuditQuery(banking.getSharedConnectionManager());
        GalaxyBase.LOG.info("Item admission policy enabled for server={} rules={}",
            context.getConfiguration().getBankingSourceServerId(), context.getConfiguration().getItemPolicyRules().length);
    }

    public boolean isRuntimeAvailable() { return service != null && auditQuery != null; }
    public List<ItemPolicyRule> getConfiguredRules() { return configuredRules; }
    public ItemPolicyAuditQuery getAuditQuery() { return auditQuery; }
}
