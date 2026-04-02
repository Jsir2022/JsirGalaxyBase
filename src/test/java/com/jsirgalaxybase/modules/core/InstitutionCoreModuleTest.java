package com.jsirgalaxybase.modules.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.jsirgalaxybase.config.ModConfiguration;
import com.jsirgalaxybase.module.ModuleContext;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.market.application.MarketRecoveryService;
import com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketService;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationType;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.infrastructure.MarketInfrastructure;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public class InstitutionCoreModuleTest {

    @Test
    public void serverStartingOnDedicatedServerAssemblesSharedMarketRuntime() throws Exception {
        ModuleContext context = new ModuleContext(false, createServerConfiguration(true, "test-server"));
        BankingInfrastructure bankingInfrastructure = new BankingInfrastructure(null, null, null, null, null, null);
        MarketInfrastructure marketInfrastructure = new MarketInfrastructure(new NoOpOrderBookRepository(),
            new NoOpCustodyRepository(), new NoOpOperationLogRepository(), new NoOpTradeRecordRepository(),
            new DirectMarketTransactionRunner());
        StandardizedSpotMarketService marketService = new StandardizedSpotMarketService(
            marketInfrastructure.getOrderBookRepository(), marketInfrastructure.getCustodyInventoryRepository(),
            marketInfrastructure.getOperationLogRepository(), marketInfrastructure.getTradeRecordRepository(),
            marketInfrastructure.getTransactionRunner(), null);
        MarketRecoveryService recoveryService = new MarketRecoveryService(marketInfrastructure.getOrderBookRepository(),
            marketInfrastructure.getCustodyInventoryRepository(), marketInfrastructure.getOperationLogRepository());
        TestInstitutionCoreModule module = new TestInstitutionCoreModule(bankingInfrastructure, marketInfrastructure,
            marketService, recoveryService);

        module.preInit(context, null);
        module.serverStarting(context, null);

        assertSame(bankingInfrastructure, module.getBankingInfrastructure());
        assertSame(marketInfrastructure, module.getMarketInfrastructure());
        assertSame(marketService, module.getStandardizedSpotMarketService());
        assertSame(recoveryService, module.getMarketRecoveryService());
        assertEquals("test-server", module.getBankingSourceServerId());
        assertTrue(module.managedAccountsInitialized);
        assertTrue(module.startupRecoveryScanTriggered);
    }

    @Test
    public void serverStartingOnNonDedicatedServerSkipsBankingAndMarketRuntime() throws Exception {
        ModuleContext context = new ModuleContext(false, createServerConfiguration(true, "test-server"));
        TestInstitutionCoreModule module = new TestInstitutionCoreModule(new BankingInfrastructure(null, null, null,
            null, null, null), new MarketInfrastructure(new NoOpOrderBookRepository(), new NoOpCustodyRepository(),
                new NoOpOperationLogRepository(), new NoOpTradeRecordRepository(),
                new DirectMarketTransactionRunner()), new StandardizedSpotMarketService(new NoOpOrderBookRepository(),
                    new NoOpCustodyRepository(), new NoOpOperationLogRepository()), new MarketRecoveryService(
                        new NoOpOrderBookRepository(), new NoOpCustodyRepository(), new NoOpOperationLogRepository()));
        module.dedicatedServer = false;

        module.preInit(context, null);
        module.serverStarting(context, null);

        assertNull(module.getBankingInfrastructure());
        assertNull(module.getMarketInfrastructure());
        assertNull(module.getStandardizedSpotMarketService());
        assertNull(module.getMarketRecoveryService());
    }

    private ModConfiguration createServerConfiguration(boolean bankingEnabled, String sourceServerId) throws IOException {
        Path tempDir = Files.createTempDirectory("jgb-institution-core-test");
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        Path clientConfig = configDir.resolve("jsirgalaxybase.cfg");
        Files.write(clientConfig, Collections.singletonList("general {}"), StandardCharsets.UTF_8);
        try {
            Constructor<ModConfiguration> constructor = ModConfiguration.class.getDeclaredConstructor(java.io.File.class,
                boolean.class, String.class, int.class, float.class, float.class, float.class, boolean.class,
                String.class, String.class, String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(tempDir.toFile(), true, "jsirgalaxybase/item_dumps", 0x529BED, 0.90f,
                0.90f, 0.13f, bankingEnabled, "jdbc:postgresql://example.invalid:5432/jsirgalaxybase", "test",
                "test", sourceServerId);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("failed to create test configuration", exception);
        }
    }

    private static final class TestInstitutionCoreModule extends InstitutionCoreModule {

        private final BankingInfrastructure bankingInfrastructureToCreate;
        private final MarketInfrastructure marketInfrastructureToCreate;
        private final StandardizedSpotMarketService marketServiceToCreate;
        private final MarketRecoveryService recoveryServiceToCreate;
        private boolean dedicatedServer = true;
        private boolean managedAccountsInitialized;
        private boolean startupRecoveryScanTriggered;

        private TestInstitutionCoreModule(BankingInfrastructure bankingInfrastructureToCreate,
            MarketInfrastructure marketInfrastructureToCreate, StandardizedSpotMarketService marketServiceToCreate,
            MarketRecoveryService recoveryServiceToCreate) {
            this.bankingInfrastructureToCreate = bankingInfrastructureToCreate;
            this.marketInfrastructureToCreate = marketInfrastructureToCreate;
            this.marketServiceToCreate = marketServiceToCreate;
            this.recoveryServiceToCreate = recoveryServiceToCreate;
        }

        @Override
        protected boolean shouldPrepareDedicatedInfrastructure() {
            return dedicatedServer;
        }

        @Override
        protected BankingInfrastructure createBankingInfrastructure(ModuleContext context) {
            return bankingInfrastructureToCreate;
        }

        @Override
        protected void initializeManagedAccounts(BankingInfrastructure infrastructure) {
            managedAccountsInitialized = true;
        }

        @Override
        protected MarketInfrastructure createMarketInfrastructure(BankingInfrastructure infrastructure) {
            return marketInfrastructureToCreate;
        }

        @Override
        protected StandardizedSpotMarketService createStandardizedSpotMarketService(BankingInfrastructure infrastructure,
            MarketInfrastructure sharedMarketInfrastructure) {
            return marketServiceToCreate;
        }

        @Override
        protected MarketRecoveryService createMarketRecoveryService(BankingInfrastructure infrastructure,
            MarketInfrastructure sharedMarketInfrastructure) {
            return recoveryServiceToCreate;
        }

        @Override
        protected void runStartupRecoveryScanIfNeeded() {
            startupRecoveryScanTriggered = getMarketRecoveryService() != null;
        }
    }

    private static final class NoOpOrderBookRepository implements MarketOrderBookRepository {

        @Override
        public MarketOrder save(MarketOrder order) {
            return order;
        }

        @Override
        public MarketOrder update(MarketOrder order) {
            return order;
        }

        @Override
        public Optional<MarketOrder> findById(long orderId) {
            return Optional.empty();
        }

        @Override
        public MarketOrder lockById(long orderId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MarketOrder> findOpenSellOrdersByProductKey(String productKey) {
            return Collections.emptyList();
        }

        @Override
        public List<MarketOrder> findOpenBuyOrdersByProductKey(String productKey) {
            return Collections.emptyList();
        }

        @Override
        public List<MarketOrder> findMatchingSellOrders(String productKey, long maxUnitPrice) {
            return Collections.emptyList();
        }

        @Override
        public List<MarketOrder> findMatchingBuyOrders(String productKey, long minUnitPrice) {
            return Collections.emptyList();
        }
    }

    private static final class NoOpCustodyRepository implements MarketCustodyInventoryRepository {

        @Override
        public MarketCustodyInventory save(MarketCustodyInventory custodyInventory) {
            return custodyInventory;
        }

        @Override
        public MarketCustodyInventory update(MarketCustodyInventory custodyInventory) {
            return custodyInventory;
        }

        @Override
        public Optional<MarketCustodyInventory> findById(long custodyId) {
            return Optional.empty();
        }

        @Override
        public MarketCustodyInventory lockById(long custodyId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<MarketCustodyInventory> findEscrowSellByOrderId(long orderId) {
            return Optional.empty();
        }

        @Override
        public List<MarketCustodyInventory> findByOwnerAndStatus(String ownerPlayerRef, MarketCustodyStatus status) {
            return Collections.emptyList();
        }
    }

    private static final class NoOpOperationLogRepository implements MarketOperationLogRepository {

        @Override
        public MarketOperationLog save(MarketOperationLog operationLog) {
            return operationLog;
        }

        @Override
        public MarketOperationLog update(MarketOperationLog operationLog) {
            return operationLog;
        }

        @Override
        public Optional<MarketOperationLog> findById(long operationId) {
            return Optional.empty();
        }

        @Override
        public Optional<MarketOperationLog> findByRequestId(String requestId) {
            return Optional.empty();
        }

        @Override
        public List<MarketOperationLog> findByStatuses(List<MarketOperationStatus> statuses, int limit) {
            return Collections.emptyList();
        }
    }

    private static final class NoOpTradeRecordRepository implements MarketTradeRecordRepository {

        @Override
        public com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord save(
            com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord tradeRecord) {
            return tradeRecord;
        }

        @Override
        public List<com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord> findByOrderId(long orderId) {
            return Collections.emptyList();
        }
    }

    private static final class DirectMarketTransactionRunner implements MarketTransactionRunner {

        @Override
        public <T> T inTransaction(java.util.function.Supplier<T> callback) {
            return callback.get();
        }

        @Override
        public void inTransaction(Runnable callback) {
            callback.run();
        }
    }
}