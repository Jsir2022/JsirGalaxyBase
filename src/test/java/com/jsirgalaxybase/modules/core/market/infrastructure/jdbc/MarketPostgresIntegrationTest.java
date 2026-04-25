package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.command.FrozenBalanceCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.InternalTransferCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountStatus;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.DriverManagerDataSource;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;
import com.jsirgalaxybase.modules.core.market.application.MarketRecoveryMetadata;
import com.jsirgalaxybase.modules.core.market.application.MarketRecoveryService;
import com.jsirgalaxybase.modules.core.market.application.MarketSettlementFacade;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.application.CustomMarketService;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketAdmissionDecision;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketAdmissionReason;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogEntry;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogVersion;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketProductCatalog;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketProductParser;
import com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketService;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimMarketAssetCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CancelSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CancelCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.DepositMarketInventoryCommand;
import com.jsirgalaxybase.modules.core.market.application.command.PublishCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.PurchaseCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationType;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketDeliveryStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListingStatus;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.infrastructure.MarketInfrastructure;
import com.jsirgalaxybase.modules.core.market.port.MarketClaimDeliveryPort;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class MarketPostgresIntegrationTest {

    private PostgresTestConfig config;
    private PostgresTestContext testContext;
    private JdbcConnectionManager connectionManager;
    private MarketInfrastructure marketInfrastructure;
    private StandardizedSpotMarketService marketService;
    private CustomMarketService customMarketService;

    @Before
    public void setUp() throws Exception {
        config = PostgresTestConfig.resolve();
        Assume.assumeTrue("PostgreSQL integration config not available", config.isConfigured());
        Assume.assumeTrue("PostgreSQL integration config is not reachable", config.canConnect());

        testContext = PostgresTestContext.create(config);
        connectionManager = new JdbcConnectionManager(testContext.getSchemaDataSource());
        marketInfrastructure = JdbcMarketInfrastructureFactory.createShared(connectionManager);
        marketService = createService(marketInfrastructure, marketInfrastructure.getTransactionRunner(), null, null);
        customMarketService = new CustomMarketService(marketInfrastructure.getCustomMarketListingRepository(),
            marketInfrastructure.getCustomMarketItemSnapshotRepository(),
            marketInfrastructure.getCustomMarketTradeRecordRepository(),
            marketInfrastructure.getCustomMarketAuditLogRepository(), marketInfrastructure.getTransactionRunner(), null);
    }

    @After
    public void tearDown() {
        if (testContext != null) {
            testContext.close();
        }
    }

    @Test
    public void factoryCreateUsesCurrentSchemaForInitializationValidation() {
        MarketInfrastructure infrastructure = JdbcMarketInfrastructureFactory.create(testContext.getSchemaJdbcUrl(),
            config.username, config.password);

        StandardizedSpotMarketService service = createService(infrastructure, infrastructure.getTransactionRunner(), null,
            null);
        service.depositInventory(new DepositMarketInventoryCommand("req-market-factory-deposit", "player-factory",
            "test-server", "minecraft:stone:0", 8L, true));
        StandardizedSpotMarketService.CreateSellOrderResult result = service.createSellOrder(new CreateSellOrderCommand(
            "req-market-factory", "player-factory", "test-server", "minecraft:stone:0", 8L, true, 12L));

        assertTrue(infrastructure.getOrderBookRepository().findById(result.getOrder().getOrderId()).isPresent());
        assertTrue(infrastructure.getCustodyInventoryRepository().findById(result.getCustody().getCustodyId()).isPresent());
    }

    @Test
    public void factoryRejectsOutdatedMarketSchemaAndGuidesOperatorToRunMigration() throws Exception {
        try (Connection connection = testContext.getSchemaDataSource().getConnection();
            Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO \"" + testContext.schemaName + "\", public");
            statement.execute("ALTER TABLE market_order DROP COLUMN filled_quantity");
            statement.execute("ALTER TABLE custom_market_trade_record DROP COLUMN delivery_status");
        }

        try {
            JdbcMarketInfrastructureFactory.create(testContext.getSchemaJdbcUrl(), config.username, config.password);
            fail("Expected outdated market schema to be rejected");
        } catch (MarketOperationException exception) {
            assertTrue(exception.getMessage().contains("market_order.filled_quantity"));
            assertTrue(exception.getMessage().contains("custom_market_trade_record.delivery_status"));
            assertTrue(exception.getMessage().contains("scripts/db-migrate.sh"));
        }
    }

    @Test
    public void depositInventoryPersistsAvailableCustodyAndOperationOnPostgres() {
        StandardizedSpotMarketService.DepositInventoryResult result = marketService.depositInventory(
            new DepositMarketInventoryCommand("req-market-deposit", "player-a", "test-server", "minecraft:stone:0",
                12L, true));

        Optional<MarketCustodyInventory> storedCustody = marketInfrastructure.getCustodyInventoryRepository()
            .findById(result.getCustody().getCustodyId());
        Optional<MarketOperationLog> storedOperation = marketInfrastructure.getOperationLogRepository()
            .findByRequestId("req-market-deposit");

        assertTrue(storedCustody.isPresent());
        assertTrue(storedOperation.isPresent());
        assertEquals(MarketCustodyStatus.AVAILABLE, storedCustody.get().getStatus());
        assertEquals(MarketOperationType.INVENTORY_DEPOSIT, storedOperation.get().getOperationType());
        assertEquals(MarketOperationStatus.COMPLETED, storedOperation.get().getStatus());
    }

    @Test
    public void createSellAndCancelPersistOnPostgres() {
        marketService.depositInventory(new DepositMarketInventoryCommand("req-market-sell-deposit", "player-a",
            "test-server", "minecraft:stone:0", 32L, true));
        StandardizedSpotMarketService.CreateSellOrderResult created = marketService.createSellOrder(
            new CreateSellOrderCommand("req-market-sell-create", "player-a", "test-server", "minecraft:stone:0",
                32L, true, 15L));
        StandardizedSpotMarketService.CancelSellOrderResult cancelled = marketService.cancelSellOrder(
            new CancelSellOrderCommand("req-market-sell-cancel", "player-a", "test-server",
                created.getOrder().getOrderId()));

        Optional<MarketOrder> storedOrder = marketInfrastructure.getOrderBookRepository().findById(created.getOrder().getOrderId());
        Optional<MarketCustodyInventory> storedCustody = marketInfrastructure.getCustodyInventoryRepository()
            .findById(cancelled.getCustody().getCustodyId());
        List<MarketCustodyInventory> claimables = marketService.listClaimableAssets("player-a");
        List<MarketCustodyInventory> available = marketService.listAvailableAssets("player-a", "minecraft:stone:0");

        assertTrue(storedOrder.isPresent());
        assertTrue(storedCustody.isPresent());
        assertEquals(MarketOrderStatus.CANCELLED, storedOrder.get().getStatus());
        assertEquals(MarketCustodyStatus.AVAILABLE, storedCustody.get().getStatus());
        assertEquals(MarketOperationStatus.COMPLETED, cancelled.getOperationLog().getStatus());
        assertTrue(claimables.isEmpty());
        assertEquals(1, available.size());
        assertEquals(cancelled.getCustody().getCustodyId(), available.get(0).getCustodyId());
    }

    @Test
    public void createSellRejectsRequestIdSemanticConflictOnPostgres() {
        marketService.depositInventory(new DepositMarketInventoryCommand("req-market-conflict-deposit", "player-a",
            "test-server", "minecraft:stone:0", 16L, true));
        marketService.createSellOrder(new CreateSellOrderCommand("req-market-conflict", "player-a", "test-server",
            "minecraft:stone:0", 16L, true, 10L));

        try {
            marketService.createSellOrder(new CreateSellOrderCommand("req-market-conflict", "player-a", "test-server",
                "minecraft:stone:0", 16L, true, 11L));
            fail("Expected request semantics conflict");
        } catch (MarketOperationException exception) {
            assertTrue(exception.getMessage().contains("unitPrice"));
        }
    }

    @Test
    public void recoveryRequiredScanFindsProcessingOperationOnPostgres() {
        marketService.depositInventory(new DepositMarketInventoryCommand("req-market-recovery-create-deposit", "player-a",
            "test-server", "minecraft:stone:0", 4L, true));
        StandardizedSpotMarketService.CreateSellOrderResult created = marketService.createSellOrder(
            new CreateSellOrderCommand("req-market-recovery-create", "player-a", "test-server", "minecraft:stone:0",
                4L, true, 9L));
        marketInfrastructure.getOperationLogRepository().save(new com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog(
            0L, "req-market-recovery-stuck",
            com.jsirgalaxybase.modules.core.market.domain.MarketOperationType.SELL_ORDER_CANCEL,
            MarketOperationStatus.PROCESSING, "test-server", "player-a", "playerRef=player-a|orderId="
                + created.getOrder().getOrderId(),
            created.getOrder().getOrderId(), created.getCustody().getCustodyId(), 0L, "stuck", Instant.now(),
            Instant.now()));

        com.jsirgalaxybase.modules.core.market.application.MarketRecoveryService recoveryService =
            new com.jsirgalaxybase.modules.core.market.application.MarketRecoveryService(
                marketInfrastructure.getOrderBookRepository(), marketInfrastructure.getCustodyInventoryRepository(),
                marketInfrastructure.getOperationLogRepository());
        List<com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog> escalated =
            recoveryService.scanAndEscalateIncompleteOperations(10);

        assertEquals(1, escalated.size());
        assertEquals(MarketOperationStatus.RECOVERY_REQUIRED, escalated.get(0).getStatus());
        assertEquals(MarketOrderStatus.EXCEPTION,
            marketInfrastructure.getOrderBookRepository().findById(created.getOrder().getOrderId()).get().getStatus());
    }

    @Test
    public void buyRecoveryReleasesFrozenFundsOnPostgres() {
        RecordingSettlementFacade settlementFacade = new RecordingSettlementFacade();
        settlementFacade.registerPlayer("buyer");
        StandardizedSpotMarketService recoveryServiceTarget = createService(marketInfrastructure,
            new FailAfterCallbackTransactionRunner(marketInfrastructure.getTransactionRunner()), settlementFacade, null);

        try {
            recoveryServiceTarget.createBuyOrder(new CreateBuyOrderCommand("req-market-buy-recovery", "buyer",
                "test-server", "minecraft:stone:0", 6L, true, 80L));
            fail("expected simulated failure");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("post-commit"));
        }

        MarketRecoveryService recoveryService = new MarketRecoveryService(marketInfrastructure.getOrderBookRepository(),
            marketInfrastructure.getCustodyInventoryRepository(), marketInfrastructure.getOperationLogRepository(),
            marketInfrastructure.getTransactionRunner(), settlementFacade);
        List<MarketOperationLog> recovered = recoveryService.scanAndEscalateIncompleteOperations(10);

        assertEquals(1, recovered.size());
        assertEquals(MarketOperationStatus.COMPLETED, recovered.get(0).getStatus());
        assertEquals(1, settlementFacade.releaseCommands.size());
        assertEquals("req-market-buy-recovery:recovery-release", settlementFacade.releaseCommands.get(0).getRequestId());
        MarketOrder storedOrder = marketInfrastructure.getOrderBookRepository().findById(recovered.get(0).getRelatedOrderId()).get();
        assertEquals(MarketOrderStatus.CANCELLED, storedOrder.getStatus());
        assertEquals(0L, storedOrder.getReservedFunds());
    }

    @Test
    public void claimMarketAssetPersistsClaimedCustodyOnPostgres() {
        RecordingSettlementFacade settlementFacade = new RecordingSettlementFacade();
        settlementFacade.registerPlayer("buyer");
        settlementFacade.registerPlayer("player-claim");
        StandardizedSpotMarketService matchingService = createService(marketInfrastructure,
            marketInfrastructure.getTransactionRunner(), settlementFacade, null);

        matchingService.depositInventory(new DepositMarketInventoryCommand("req-market-claim-sell-deposit",
            "player-claim", "test-server", "minecraft:stone:0", 5L, true));
        matchingService.createBuyOrder(new CreateBuyOrderCommand("req-market-claim-buy", "buyer", "test-server",
            "minecraft:stone:0", 5L, true, 14L));
        StandardizedSpotMarketService.CreateSellOrderResult created = matchingService.createSellOrder(
            new CreateSellOrderCommand("req-market-claim-sell", "player-claim", "test-server",
                "minecraft:stone:0", 5L, true, 14L));

        RecordingClaimDeliveryPort claimDeliveryPort = new RecordingClaimDeliveryPort();
        StandardizedSpotMarketService claimService = createService(marketInfrastructure,
            marketInfrastructure.getTransactionRunner(), null, claimDeliveryPort);

        StandardizedSpotMarketService.ClaimMarketAssetResult claimed = claimService.claimMarketAsset(
            new ClaimMarketAssetCommand("req-market-claim-asset", "buyer", "test-server",
                created.getClaimableAssets().get(0).getCustodyId()));

        Optional<MarketCustodyInventory> storedCustody = marketInfrastructure.getCustodyInventoryRepository()
            .findById(claimed.getCustody().getCustodyId());
        Optional<MarketOperationLog> storedOperation = marketInfrastructure.getOperationLogRepository()
            .findByRequestId("req-market-claim-asset");

        assertTrue(storedCustody.isPresent());
        assertTrue(storedOperation.isPresent());
        assertEquals(MarketCustodyStatus.CLAIMED, storedCustody.get().getStatus());
        assertEquals(MarketOperationStatus.COMPLETED, storedOperation.get().getStatus());
        assertEquals(1, claimDeliveryPort.deliveries.size());
    }

    @Test
    public void customPublishPersistsSnapshotAndAuditOnPostgres() {
        CustomMarketService.PublishListingResult result = customMarketService.publishListing(
            new PublishCustomMarketListingCommand("req-custom-market-pg-publish", "seller-a", "test-server",
                1800L, "STARCOIN", namedCustomStack("Relic Stone", 1, "seller-a")));

        assertTrue(marketInfrastructure.getCustomMarketListingRepository().findById(result.getListing().getListingId())
            .isPresent());
        assertTrue(marketInfrastructure.getCustomMarketItemSnapshotRepository().findByListingId(
            result.getListing().getListingId()).isPresent());
        assertTrue(marketInfrastructure.getCustomMarketAuditLogRepository().findByRequestId(
            "req-custom-market-pg-publish").isPresent());
        assertEquals(CustomMarketListingStatus.ACTIVE, result.getListing().getListingStatus());
        assertEquals(CustomMarketDeliveryStatus.ESCROW_HELD, result.getListing().getDeliveryStatus());
    }

    @Test
    public void customPublishRejectsStackedListingOnPostgres() {
        try {
            customMarketService.publishListing(new PublishCustomMarketListingCommand("req-custom-market-pg-stacked",
                "seller-a", "test-server", 1800L, "STARCOIN", namedCustomStack("Relic Stone", 4, "seller-a")));
            fail("Expected stacked custom listing to be rejected");
        } catch (MarketOperationException exception) {
            assertTrue(exception.getMessage().contains("exactly one item"));
        }
    }

    @Test
    public void customPurchasePersistsPendingClaimStateOnPostgres() {
        RecordingSettlementFacade settlementFacade = new RecordingSettlementFacade();
        settlementFacade.registerPlayer("seller-a");
        settlementFacade.registerPlayer("buyer-b");
        CustomMarketService purchasingService = new CustomMarketService(
            marketInfrastructure.getCustomMarketListingRepository(),
            marketInfrastructure.getCustomMarketItemSnapshotRepository(),
            marketInfrastructure.getCustomMarketTradeRecordRepository(),
            marketInfrastructure.getCustomMarketAuditLogRepository(), marketInfrastructure.getTransactionRunner(),
            settlementFacade);

        CustomMarketService.PublishListingResult publishResult = purchasingService.publishListing(
            new PublishCustomMarketListingCommand("req-custom-market-pg-buy-publish", "seller-a", "test-server",
                2200L, "STARCOIN", namedCustomStack("Ancient Gear", 1, "seller-a")));
        CustomMarketService.PurchaseListingResult purchaseResult = purchasingService.purchaseListing(
            new PurchaseCustomMarketListingCommand("req-custom-market-pg-buy", "buyer-b", "test-server",
                publishResult.getListing().getListingId()));

        assertEquals(CustomMarketListingStatus.SOLD, purchaseResult.getListing().getListingStatus());
        assertEquals(CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM,
            purchaseResult.getListing().getDeliveryStatus());
        assertEquals(1, settlementFacade.freezeCommands.size());
        assertEquals(1, settlementFacade.settleCommands.size());
        assertTrue(marketInfrastructure.getCustomMarketTradeRecordRepository().findByListingId(
            publishResult.getListing().getListingId()).isPresent());
        assertEquals(1, purchasingService.listBuyerPendingClaims("buyer-b").size());
        assertEquals(1, purchasingService.listSellerPendingDeliveries("seller-a").size());
    }

    @Test
    public void customClaimCompletesDeliveryAndClearsPendingViewsOnPostgres() {
        RecordingSettlementFacade settlementFacade = new RecordingSettlementFacade();
        settlementFacade.registerPlayer("seller-a");
        settlementFacade.registerPlayer("buyer-b");
        CustomMarketService purchasingService = new CustomMarketService(
            marketInfrastructure.getCustomMarketListingRepository(),
            marketInfrastructure.getCustomMarketItemSnapshotRepository(),
            marketInfrastructure.getCustomMarketTradeRecordRepository(),
            marketInfrastructure.getCustomMarketAuditLogRepository(), marketInfrastructure.getTransactionRunner(),
            settlementFacade);

        CustomMarketService.PublishListingResult publishResult = purchasingService.publishListing(
            new PublishCustomMarketListingCommand("req-custom-market-pg-claim-publish", "seller-a", "test-server",
                2200L, "STARCOIN", namedCustomStack("Ancient Gear", 1, "seller-a")));
        purchasingService.purchaseListing(new PurchaseCustomMarketListingCommand("req-custom-market-pg-claim-buy",
            "buyer-b", "test-server", publishResult.getListing().getListingId()));

        CustomMarketService.ClaimListingResult claimResult = purchasingService.claimPurchasedListing(
            new ClaimCustomMarketListingCommand("req-custom-market-pg-claim", "buyer-b", "test-server",
                publishResult.getListing().getListingId()));

        assertEquals(CustomMarketDeliveryStatus.COMPLETED, claimResult.getListing().getDeliveryStatus());
        assertEquals(CustomMarketDeliveryStatus.COMPLETED, claimResult.getTradeRecord().getDeliveryStatus());
        assertTrue(marketInfrastructure.getCustomMarketAuditLogRepository().findByRequestId(
            "req-custom-market-pg-claim").isPresent());
        assertEquals(0, purchasingService.listBuyerPendingClaims("buyer-b").size());
        assertEquals(0, purchasingService.listSellerPendingDeliveries("seller-a").size());
    }

    @Test
    public void depositRecoveryCompletesAvailableCustodyOnPostgres() {
        StandardizedSpotMarketService recoveryServiceTarget = createService(marketInfrastructure,
            new FailAfterCallbackTransactionRunner(marketInfrastructure.getTransactionRunner()), null, null);

        try {
            recoveryServiceTarget.depositInventory(new DepositMarketInventoryCommand("req-market-deposit-recovery",
                "player-a", "test-server", "minecraft:stone:0", 7L, true));
            fail("expected simulated failure");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("post-commit"));
        }

        MarketOperationLog failedOperation = marketInfrastructure.getOperationLogRepository()
            .findByRequestId("req-market-deposit-recovery").get();
        assertEquals(MarketOperationStatus.RECOVERY_REQUIRED, failedOperation.getStatus());
        assertEquals(MarketCustodyStatus.AVAILABLE,
            marketInfrastructure.getCustodyInventoryRepository().findById(failedOperation.getRelatedCustodyId()).get()
                .getStatus());

        MarketRecoveryService recoveryService = new MarketRecoveryService(marketInfrastructure.getOrderBookRepository(),
            marketInfrastructure.getCustodyInventoryRepository(), marketInfrastructure.getOperationLogRepository(),
            marketInfrastructure.getTransactionRunner(), null);
        List<MarketOperationLog> recovered = recoveryService.scanAndEscalateIncompleteOperations(10);

        assertEquals(1, recovered.size());
        assertEquals(MarketOperationStatus.COMPLETED, recovered.get(0).getStatus());
        assertEquals(MarketCustodyStatus.AVAILABLE,
            marketInfrastructure.getCustodyInventoryRepository().findById(failedOperation.getRelatedCustodyId()).get()
                .getStatus());
    }

    private StandardizedSpotMarketService createService(MarketInfrastructure infrastructure,
        MarketTransactionRunner transactionRunner, MarketSettlementFacade settlementFacade,
        MarketClaimDeliveryPort claimDeliveryPort) {
        return new StandardizedSpotMarketService(infrastructure.getOrderBookRepository(),
            infrastructure.getCustodyInventoryRepository(), infrastructure.getOperationLogRepository(),
            infrastructure.getTradeRecordRepository(), transactionRunner, settlementFacade,
            new StandardizedMarketProductParser(), new PostgresTestProductCatalog(), claimDeliveryPort);
    }

    private static final class PostgresTestProductCatalog implements StandardizedMarketProductCatalog {

        private final StandardizedMarketProductParser parser = new StandardizedMarketProductParser();

        @Override
        public StandardizedMarketCatalogVersion getCatalogVersion() {
            return new StandardizedMarketCatalogVersion("postgres-test-catalog-v1", "Postgres 测试目录 v1");
        }

        @Override
        public String getCatalogSourceKey() {
            return "postgres-test-source";
        }

        @Override
        public String getCatalogSourceDescription() {
            return "Postgres Test Source";
        }

        @Override
        public StandardizedMarketAdmissionDecision evaluateProduct(String productKey) {
            StandardizedMarketProduct product = parser.parse(productKey);
            return new StandardizedMarketAdmissionDecision(getCatalogVersion(),
                new StandardizedMarketCatalogEntry(product, "postgres-test", "统一定义、统一计量、统一托管", "postgres"),
                true, StandardizedMarketAdmissionReason.CATALOG_ADMITTED, "admitted", getCatalogSourceKey(),
                getCatalogSourceDescription());
        }

        @Override
        public StandardizedMarketAdmissionDecision evaluateStack(net.minecraft.item.ItemStack stack) {
            return evaluateProduct(cpw.mods.fml.common.registry.GameRegistry.findUniqueIdentifierFor(stack.getItem()) + ":"
                + stack.getItemDamage());
        }

        @Override
        public StandardizedMarketProduct requireTradableProduct(String productKey) {
            return evaluateProduct(productKey).requireProduct();
        }

        @Override
        public StandardizedMarketProduct requireTradableStack(net.minecraft.item.ItemStack stack) {
            return evaluateStack(stack).requireProduct();
        }
    }

    private static final class PostgresTestContext {

        private static final String MARKET_DDL = loadMarketDdl();

        private final DriverManagerDataSource baseDataSource;
        private final DriverManagerDataSource schemaDataSource;
        private final String schemaName;
        private final String schemaJdbcUrl;

        private PostgresTestContext(DriverManagerDataSource baseDataSource, DriverManagerDataSource schemaDataSource,
            String schemaName, String schemaJdbcUrl) {
            this.baseDataSource = baseDataSource;
            this.schemaDataSource = schemaDataSource;
            this.schemaName = schemaName;
            this.schemaJdbcUrl = schemaJdbcUrl;
        }

        private static PostgresTestContext create(PostgresTestConfig config) throws SQLException {
            DriverManagerDataSource baseDataSource = new DriverManagerDataSource(config.jdbcUrl, config.username,
                config.password);
            String schemaName = "market_it_" + UUID.randomUUID().toString().replace("-", "");
            createSchema(baseDataSource, schemaName);
            String schemaJdbcUrl = withCurrentSchema(config.jdbcUrl, schemaName);
            DriverManagerDataSource schemaDataSource = new DriverManagerDataSource(schemaJdbcUrl, config.username,
                config.password);
            applyDdl(schemaDataSource, schemaName);
            return new PostgresTestContext(baseDataSource, schemaDataSource, schemaName, schemaJdbcUrl);
        }

        private DriverManagerDataSource getSchemaDataSource() {
            return schemaDataSource;
        }

        private String getSchemaJdbcUrl() {
            return schemaJdbcUrl;
        }

        private void close() {
            try (Connection connection = baseDataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            } catch (SQLException ignored) {
                // cleanup best effort only
            }
        }

        private static void createSchema(DriverManagerDataSource baseDataSource, String schemaName) throws SQLException {
            try (Connection connection = baseDataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA \"" + schemaName + "\"");
            }
        }

        private static void applyDdl(DriverManagerDataSource schemaDataSource, String schemaName) throws SQLException {
            try (Connection connection = schemaDataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO \"" + schemaName + "\", public");
                statement.execute(MARKET_DDL);
            }
        }

        private static String withCurrentSchema(String jdbcUrl, String schemaName) {
            return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "currentSchema=" + schemaName;
        }

        private static String loadMarketDdl() {
            Path ddlPath = Paths.get("docs", "market-postgresql-ddl.sql");
            try {
                return new String(Files.readAllBytes(ddlPath), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to load market-postgresql-ddl.sql", exception);
            }
        }
    }

    private static final class RecordingClaimDeliveryPort implements MarketClaimDeliveryPort {

        private final List<String> deliveries = new ArrayList<String>();

        @Override
        public void deliver(String deliveryRequestId, String playerRef, String sourceServerId,
            com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct product, boolean stackable,
            long quantity) {
            deliveries.add(deliveryRequestId + "|" + playerRef + "|" + product.getProductKey() + "|" + quantity);
        }
    }

    private ItemStack namedCustomStack(String displayName, int stackSize, String owner) {
        ItemStack stack = new ItemStack(Blocks.stone, stackSize, 0);
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", displayName);
        tag.setTag("display", display);
        tag.setString("owner", owner);
        stack.setTagCompound(tag);
        return stack;
    }

    private static final class FailAfterCallbackTransactionRunner implements MarketTransactionRunner {

        private final MarketTransactionRunner delegate;

        private FailAfterCallbackTransactionRunner(MarketTransactionRunner delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> T inTransaction(final java.util.function.Supplier<T> callback) {
            delegate.inTransaction(callback);
            throw new RuntimeException("simulated post-commit failure");
        }

        @Override
        public void inTransaction(final Runnable callback) {
            delegate.inTransaction(callback);
            throw new RuntimeException("simulated post-commit failure");
        }
    }

    private static final class RecordingSettlementFacade extends MarketSettlementFacade {

        private final java.util.Map<String, BankAccount> playerAccounts = new java.util.HashMap<String, BankAccount>();
        private final BankAccount taxAccount = newAccount(999L, "tax");
        private final List<FrozenBalanceCommand> freezeCommands = new ArrayList<FrozenBalanceCommand>();
        private final List<FrozenBalanceCommand> releaseCommands = new ArrayList<FrozenBalanceCommand>();
        private final List<InternalTransferCommand> settleCommands = new ArrayList<InternalTransferCommand>();
        private long nextAccountId = 1L;

        private void registerPlayer(String playerRef) {
            playerAccounts.put(playerRef, newAccount(nextAccountId++, playerRef));
        }

        @Override
        public BankAccount requirePlayerAccount(String playerRef) {
            BankAccount account = playerAccounts.get(playerRef);
            if (account == null) {
                throw new MarketOperationException("player bank account not found: " + playerRef);
            }
            return account;
        }

        @Override
        public BankAccount ensureTaxAccount() {
            return taxAccount;
        }

        @Override
        public BankPostingResult freezeBuyerFunds(FrozenBalanceCommand command) {
            freezeCommands.add(command);
            return emptyPostingResult();
        }

        @Override
        public BankPostingResult releaseBuyerFunds(FrozenBalanceCommand command) {
            releaseCommands.add(command);
            return emptyPostingResult();
        }

        @Override
        public BankPostingResult settleFromFrozenFunds(InternalTransferCommand command) {
            settleCommands.add(command);
            return emptyPostingResult();
        }

        @Override
        public BankPostingResult collectTax(InternalTransferCommand command) {
            return emptyPostingResult();
        }

        private BankAccount newAccount(long accountId, String ownerRef) {
            return new BankAccount(accountId, "ACC-" + accountId, BankAccountType.PLAYER, "player", ownerRef,
                "STARCOIN", 1_000_000L, 0L, BankAccountStatus.ACTIVE, 0L, ownerRef, "{}", Instant.now(),
                Instant.now());
        }

        private BankPostingResult emptyPostingResult() {
            return new BankPostingResult(null, java.util.Collections.<BankAccount>emptyList(),
                java.util.Collections.emptyList(), null);
        }
    }

    private static final class PostgresTestConfig {

        private final String jdbcUrl;
        private final String username;
        private final String password;

        private PostgresTestConfig(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }

        private static PostgresTestConfig resolve() {
            String jdbcUrl = firstNonBlank(System.getProperty("jgb.market.it.jdbcUrl"),
                System.getenv("JGB_MARKET_IT_JDBC_URL"), readConfigValue("bankingJdbcUrl"));
            String username = firstNonBlank(System.getProperty("jgb.market.it.username"),
                System.getenv("JGB_MARKET_IT_JDBC_USERNAME"), readConfigValue("bankingJdbcUsername"));
            String password = firstNonBlank(System.getProperty("jgb.market.it.password"),
                System.getenv("JGB_MARKET_IT_JDBC_PASSWORD"), readConfigValue("bankingJdbcPassword"));
            return new PostgresTestConfig(jdbcUrl, username, password);
        }

        private boolean isConfigured() {
            return !isBlank(jdbcUrl) && !isBlank(username) && !isBlank(password);
        }

        private boolean canConnect() {
            if (!isConfigured()) {
                return false;
            }
            try {
                Class.forName("org.postgresql.Driver");
                DriverManagerDataSource dataSource = new DriverManagerDataSource(jdbcUrl, username, password);
                try (Connection ignored = dataSource.getConnection()) {
                    return true;
                }
            } catch (ClassNotFoundException exception) {
                return false;
            } catch (SQLException exception) {
                return false;
            }
        }

        private static String readConfigValue(String key) {
            Path configPath = Paths.get("run", "server", "config", "jsirgalaxybase-server.cfg");
            if (!Files.exists(configPath)) {
                return null;
            }
            try {
                for (String line : Files.readAllLines(configPath, StandardCharsets.UTF_8)) {
                    String trimmedLine = line.trim();
                    String prefix = "S:" + key + "=";
                    if (trimmedLine.startsWith(prefix)) {
                        String value = trimmedLine.substring(prefix.length()).trim();
                        return value.isEmpty() ? null : value;
                    }
                }
            } catch (IOException exception) {
                return null;
            }
            return null;
        }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                if (!isBlank(value)) {
                    return value;
                }
            }
            return null;
        }

        private static boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}