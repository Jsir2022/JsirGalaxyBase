package com.jsirgalaxybase.modules.core;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts.ManagedAccountSpec;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcBankingInfrastructureFactory;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.application.MarketRecoveryService;
import com.jsirgalaxybase.modules.core.market.application.MarketSettlementFacade;
import com.jsirgalaxybase.modules.core.market.application.CustomMarketService;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketAdmissionDecision;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogService;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogVersion;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketProductCatalog;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketProductParser;
import com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketService;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.infrastructure.MarketInfrastructure;
import com.jsirgalaxybase.modules.core.market.infrastructure.jdbc.JdbcMarketInfrastructureFactory;
import com.jsirgalaxybase.modules.core.market.infrastructure.jdbc.JdbcStandardizedMarketCatalogSource;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.infrastructure.VaultMarketClaimDeliveryPort;
import com.jsirgalaxybase.modules.core.vault.infrastructure.BaseVaultAccountInventoryResolver;
import com.jsirgalaxybase.modules.core.vault.infrastructure.jdbc.JdbcBaseVaultRepository;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

public class InstitutionCoreModule extends ModModule {

    private static final int STARTUP_MARKET_RECOVERY_LIMIT = 25;
    private static final int STARTUP_MARKET_SMOKE_LIMIT = 12;

    private BankingInfrastructure bankingInfrastructure;
    private MarketInfrastructure marketInfrastructure;
    private StandardizedSpotMarketService standardizedSpotMarketService;
    private CustomMarketService customMarketService;
    private MarketRecoveryService marketRecoveryService;
    private BaseVaultService baseVaultService;
    private BaseVaultAccountInventoryResolver accountInventoryResolver;
    private String bankingSourceServerId;
    private boolean bankingRequested;

    public InstitutionCoreModule() {
        super("institution-core", "Institution Core", "core");
    }

    @Override
    public void preInit(ModuleContext context, FMLPreInitializationEvent event) {
        if (!context.isClient() && context.getConfiguration().isBankingPostgresEnabled()) {
            bankingSourceServerId = context.getConfiguration().getBankingSourceServerId();
            bankingRequested = true;
            GalaxyBase.LOG.info(
                "Banking PostgreSQL infrastructure is configured for server {} and will be prepared during dedicated server start",
                bankingSourceServerId);
            return;
        }

        GalaxyBase.LOG.info(
            "Institution core reserved for profession, economy, reputation, public orders, and transfer state.");
    }

    @Override
    public void serverStarting(ModuleContext context, FMLServerStartingEvent event) {
        if (!shouldPrepareDedicatedInfrastructure()) {
            if (bankingRequested) {
                GalaxyBase.LOG.warn("Banking PostgreSQL infrastructure is disabled because the current server is not dedicated");
            }
            return;
        }

        if (bankingRequested && bankingInfrastructure == null) {
            bankingInfrastructure = createBankingInfrastructure(context);
            GalaxyBase.LOG.info(
                "Banking PostgreSQL infrastructure prepared and validated for dedicated server {} using server-only JDBC configuration",
                bankingSourceServerId);
        }

        if (bankingInfrastructure == null) {
            return;
        }

        initializeManagedAccounts(bankingInfrastructure);
        initializeMarketRuntimeIfNeeded();
        runStartupRecoveryScanIfNeeded();
    }

    protected boolean shouldPrepareDedicatedInfrastructure() {
        MinecraftServer server = MinecraftServer.getServer();
        return server != null && server.isDedicatedServer();
    }

    protected BankingInfrastructure createBankingInfrastructure(ModuleContext context) {
        return JdbcBankingInfrastructureFactory.create(context.getConfiguration());
    }

    protected void initializeManagedAccounts(BankingInfrastructure infrastructure) {
        for (ManagedAccountSpec spec : ManagedBankAccounts.getManagedAccounts()) {
            BankAccount account = ManagedBankAccounts.ensureManagedAccount(infrastructure, spec);
            GalaxyBase.LOG.info(
                "Managed banking account ensured: {} -> {} ({}) balance={}",
                spec.getCommandKey(),
                account.getAccountNo(),
                account.getAccountType(),
                account.getAvailableBalance());
        }
    }

    protected MarketInfrastructure createMarketInfrastructure(BankingInfrastructure infrastructure) {
        if (infrastructure == null || infrastructure.getSharedConnectionManager() == null) {
            throw new MarketOperationException("shared banking JDBC connection manager is required for market runtime");
        }
        return JdbcMarketInfrastructureFactory.createShared(infrastructure.getSharedConnectionManager());
    }

    protected StandardizedSpotMarketService createStandardizedSpotMarketService(BankingInfrastructure infrastructure,
        MarketInfrastructure sharedMarketInfrastructure) {
        MarketSettlementFacade settlementFacade = new MarketSettlementFacade(infrastructure);
        return new StandardizedSpotMarketService(sharedMarketInfrastructure.getOrderBookRepository(),
            sharedMarketInfrastructure.getCustodyInventoryRepository(),
            sharedMarketInfrastructure.getOperationLogRepository(),
            sharedMarketInfrastructure.getTradeRecordRepository(), sharedMarketInfrastructure.getTransactionRunner(),
            settlementFacade, new StandardizedMarketProductParser(),
            new StandardizedMarketCatalogService(
                new StandardizedMarketCatalogVersion("standardized-market-catalog-db-v1", "标准商品正式目录 v1"),
                new JdbcStandardizedMarketCatalogSource(infrastructure.getSharedConnectionManager())),
            new VaultMarketClaimDeliveryPort(baseVaultService));
    }

    protected MarketRecoveryService createMarketRecoveryService(BankingInfrastructure infrastructure,
        MarketInfrastructure sharedMarketInfrastructure) {
        return new MarketRecoveryService(sharedMarketInfrastructure.getOrderBookRepository(),
            sharedMarketInfrastructure.getCustodyInventoryRepository(),
            sharedMarketInfrastructure.getOperationLogRepository(), sharedMarketInfrastructure.getTransactionRunner(),
            new MarketSettlementFacade(infrastructure));
    }

    protected CustomMarketService createCustomMarketService(BankingInfrastructure infrastructure,
        MarketInfrastructure sharedMarketInfrastructure) {
        return new CustomMarketService(sharedMarketInfrastructure.getCustomMarketListingRepository(),
            sharedMarketInfrastructure.getCustomMarketItemSnapshotRepository(),
            sharedMarketInfrastructure.getCustomMarketTradeRecordRepository(),
            sharedMarketInfrastructure.getCustomMarketAuditLogRepository(),
            sharedMarketInfrastructure.getTransactionRunner(), new MarketSettlementFacade(infrastructure));
    }

    protected void runStartupRecoveryScanIfNeeded() {
        if (marketRecoveryService == null) {
            return;
        }
        try {
            java.util.List<MarketOperationLog> recovered = scanMarketRecovery(STARTUP_MARKET_RECOVERY_LIMIT);
            if (recovered.isEmpty()) {
                GalaxyBase.LOG.info("Market startup recovery scan finished with no pending operations");
                return;
            }
            GalaxyBase.LOG.info("Market startup recovery scan processed {} pending operations", recovered.size());
            for (MarketOperationLog operation : recovered) {
                GalaxyBase.LOG.info(
                    "Recovered market operation {} requestId={} status={} orderId={} custodyId={} message={}",
                    operation.getOperationId(),
                    operation.getRequestId(),
                    operation.getStatus(),
                    operation.getRelatedOrderId(),
                    operation.getRelatedCustodyId(),
                    operation.getMessage());
            }
        } catch (RuntimeException exception) {
            GalaxyBase.LOG.warn("Market startup recovery scan failed", exception);
        }
    }

    private void initializeMarketRuntimeIfNeeded() {
        if (marketInfrastructure != null && standardizedSpotMarketService != null && customMarketService != null
            && marketRecoveryService != null && baseVaultService != null) {
            return;
        }
        try {
            marketInfrastructure = createMarketInfrastructure(bankingInfrastructure);
            if (bankingInfrastructure.getSharedConnectionManager() != null) {
                JdbcBaseVaultRepository.validateSchema(bankingInfrastructure.getSharedConnectionManager());
            }
            baseVaultService = new BaseVaultService(
                new JdbcBaseVaultRepository(bankingInfrastructure.getSharedConnectionManager()),
                marketInfrastructure.getTransactionRunner());
            accountInventoryResolver = new BaseVaultAccountInventoryResolver(baseVaultService);
            standardizedSpotMarketService = createStandardizedSpotMarketService(bankingInfrastructure, marketInfrastructure);
            customMarketService = createCustomMarketService(bankingInfrastructure, marketInfrastructure);
            marketRecoveryService = createMarketRecoveryService(bankingInfrastructure, marketInfrastructure);
            GalaxyBase.LOG.info(
                "Market runtime prepared for dedicated server {} using shared banking JDBC schema",
                bankingSourceServerId);
            runStartupMarketCatalogSmokeIfNeeded();
        } catch (RuntimeException exception) {
            marketInfrastructure = null;
            standardizedSpotMarketService = null;
            customMarketService = null;
            marketRecoveryService = null;
            baseVaultService = null;
            accountInventoryResolver = null;
            GalaxyBase.LOG.error("Failed to prepare market runtime", exception);
        }
    }

    protected void runStartupMarketCatalogSmokeIfNeeded() {
        if (marketInfrastructure == null || standardizedSpotMarketService == null) {
            return;
        }
        try {
            java.util.LinkedHashSet<String> candidateKeys = new java.util.LinkedHashSet<String>();
            candidateKeys.addAll(marketInfrastructure.getOrderBookRepository()
                .findActiveProductKeys(STARTUP_MARKET_SMOKE_LIMIT));
            if (candidateKeys.size() < STARTUP_MARKET_SMOKE_LIMIT) {
                candidateKeys.addAll(marketInfrastructure.getTradeRecordRepository()
                    .findDistinctProductKeys(STARTUP_MARKET_SMOKE_LIMIT - candidateKeys.size()));
            }
            if (candidateKeys.isEmpty()) {
                GalaxyBase.LOG.info("[market-smoke] Standardized market startup smoke found no DB product keys.");
                return;
            }

            java.util.List<String> admitted = new java.util.ArrayList<String>();
            java.util.List<String> rejected = new java.util.ArrayList<String>();
            for (String productKey : candidateKeys) {
                if (productKey == null || productKey.trim().isEmpty()) {
                    continue;
                }
                try {
                    StandardizedMarketAdmissionDecision decision = standardizedSpotMarketService
                        .inspectCatalogProduct(productKey);
                    if (decision.isAdmitted()) {
                        admitted.add(productKey);
                    } else {
                        rejected.add(formatStartupCatalogRejection(productKey, decision.getDetailMessage()));
                    }
                } catch (RuntimeException exception) {
                    rejected.add(formatStartupCatalogRejection(productKey, exception.getMessage()));
                }
            }

            if (admitted.isEmpty()) {
                GalaxyBase.LOG.warn(
                    "[market-smoke] Standardized market startup smoke rejected all {} DB product keys. First rejected: {}",
                    candidateKeys.size(),
                    rejected);
                return;
            }
            GalaxyBase.LOG.info(
                "[market-smoke] Standardized market startup smoke admitted {}/{} DB product keys. First admitted: {}. First rejected: {}",
                admitted.size(),
                candidateKeys.size(),
                admitted.subList(0, Math.min(admitted.size(), STARTUP_MARKET_SMOKE_LIMIT)),
                rejected.subList(0, Math.min(rejected.size(), STARTUP_MARKET_SMOKE_LIMIT)));
        } catch (RuntimeException exception) {
            GalaxyBase.LOG.warn("[market-smoke] Standardized market startup smoke failed", exception);
        }
    }

    private String formatStartupCatalogRejection(String productKey, String detailMessage) {
        if (detailMessage == null || detailMessage.trim().isEmpty()) {
            return productKey + " -> rejected without detail";
        }
        return productKey + " -> " + detailMessage;
    }

    public BankingInfrastructure getBankingInfrastructure() {
        return bankingInfrastructure;
    }

    public MarketInfrastructure getMarketInfrastructure() {
        return marketInfrastructure;
    }

    public StandardizedSpotMarketService getStandardizedSpotMarketService() {
        return standardizedSpotMarketService;
    }

    public CustomMarketService getCustomMarketService() {
        return customMarketService;
    }

    public BaseVaultService getBaseVaultService() {
        return baseVaultService;
    }

    public BaseVaultAccountInventoryResolver getAccountInventoryResolver() {
        return accountInventoryResolver;
    }

    public StandardizedMarketProductCatalog getStandardizedMarketProductCatalog() {
        StandardizedSpotMarketService spotMarketService = getStandardizedSpotMarketService();
        return spotMarketService == null ? null : spotMarketService.getProductCatalog();
    }

    public StandardizedMarketAdmissionDecision inspectStandardizedCatalogProduct(String productKey) {
        StandardizedSpotMarketService spotMarketService = getStandardizedSpotMarketService();
        if (spotMarketService == null) {
            throw new MarketOperationException("standardized spot market runtime is not available");
        }
        return spotMarketService.inspectCatalogProduct(productKey);
    }

    public StandardizedMarketAdmissionDecision inspectStandardizedCatalogStack(ItemStack stack) {
        StandardizedSpotMarketService spotMarketService = getStandardizedSpotMarketService();
        if (spotMarketService == null) {
            throw new MarketOperationException("standardized spot market runtime is not available");
        }
        return spotMarketService.inspectCatalogStack(stack);
    }

    public MarketRecoveryService getMarketRecoveryService() {
        return marketRecoveryService;
    }

    public String getBankingSourceServerId() {
        return bankingSourceServerId;
    }

    public java.util.List<MarketOperationLog> scanMarketRecovery(int limit) {
        if (marketRecoveryService == null) {
            throw new MarketOperationException("market recovery service is not available");
        }
        int normalizedLimit = limit <= 0 ? STARTUP_MARKET_RECOVERY_LIMIT : Math.min(limit, 100);
        return marketRecoveryService.scanAndEscalateIncompleteOperations(normalizedLimit);
    }
}
