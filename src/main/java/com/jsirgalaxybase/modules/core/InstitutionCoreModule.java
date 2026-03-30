package com.jsirgalaxybase.modules.core;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts.ManagedAccountSpec;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcBankingInfrastructureFactory;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class InstitutionCoreModule extends ModModule {

    private BankingInfrastructure bankingInfrastructure;
    private String bankingSourceServerId;

    public InstitutionCoreModule() {
        super("institution-core", "Institution Core", "core");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        if (!context.isClient() && context.getConfiguration().isBankingPostgresEnabled()) {
            bankingSourceServerId = context.getConfiguration().getBankingSourceServerId();
            bankingInfrastructure = JdbcBankingInfrastructureFactory.create(context.getConfiguration());
            GalaxyBase.LOG.info(
                "Banking PostgreSQL infrastructure prepared and validated for server {} using {}",
                bankingSourceServerId,
                context.getConfiguration().getBankingJdbcUrl());
            return;
        }

        GalaxyBase.LOG.info(
            "Institution core reserved for profession, economy, reputation, public orders, and transfer state.");
    }

    @Override
    public void serverStarting(ModuleContext context, FMLServerStartingEvent event) {
        if (bankingInfrastructure == null) {
            return;
        }

        for (ManagedAccountSpec spec : ManagedBankAccounts.getManagedAccounts()) {
            BankAccount account = ManagedBankAccounts.ensureManagedAccount(bankingInfrastructure, spec);
            GalaxyBase.LOG.info(
                "Managed banking account ensured: {} -> {} ({}) balance={}",
                spec.getCommandKey(),
                account.getAccountNo(),
                account.getAccountType(),
                account.getAvailableBalance());
        }
    }

    public BankingInfrastructure getBankingInfrastructure() {
        return bankingInfrastructure;
    }

    public String getBankingSourceServerId() {
        return bankingSourceServerId;
    }
}