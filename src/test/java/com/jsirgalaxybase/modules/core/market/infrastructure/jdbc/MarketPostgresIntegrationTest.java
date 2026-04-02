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
import com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketService;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimMarketAssetCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CancelSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationType;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderStatus;
import com.jsirgalaxybase.modules.core.market.infrastructure.MarketInfrastructure;
import com.jsirgalaxybase.modules.core.market.port.MarketClaimDeliveryPort;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public class MarketPostgresIntegrationTest {

    private PostgresTestConfig config;
    private PostgresTestContext testContext;
    private JdbcConnectionManager connectionManager;
    private MarketInfrastructure marketInfrastructure;
    private StandardizedSpotMarketService marketService;

    @Before
    public void setUp() throws Exception {
        config = PostgresTestConfig.resolve();
        Assume.assumeTrue("PostgreSQL integration config not available", config.isConfigured());
        Assume.assumeTrue("PostgreSQL integration config is not reachable", config.canConnect());

        testContext = PostgresTestContext.create(config);
        connectionManager = new JdbcConnectionManager(testContext.getSchemaDataSource());
        marketInfrastructure = JdbcMarketInfrastructureFactory.createShared(connectionManager);
        marketService = new StandardizedSpotMarketService(marketInfrastructure.getOrderBookRepository(),
            marketInfrastructure.getCustodyInventoryRepository(), marketInfrastructure.getOperationLogRepository(),
            marketInfrastructure.getTradeRecordRepository(), marketInfrastructure.getTransactionRunner(), null);
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

        StandardizedSpotMarketService service = new StandardizedSpotMarketService(infrastructure.getOrderBookRepository(),
            infrastructure.getCustodyInventoryRepository(), infrastructure.getOperationLogRepository(),
            infrastructure.getTradeRecordRepository(), infrastructure.getTransactionRunner(), null);
        StandardizedSpotMarketService.CreateSellOrderResult result = service.createSellOrder(new CreateSellOrderCommand(
            "req-market-factory", "player-factory", "test-server", "minecraft:stone:0", 8L, true, 12L));

        assertTrue(infrastructure.getOrderBookRepository().findById(result.getOrder().getOrderId()).isPresent());
        assertTrue(infrastructure.getCustodyInventoryRepository().findById(result.getCustody().getCustodyId()).isPresent());
    }

    @Test
    public void createSellAndCancelPersistOnPostgres() {
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

        assertTrue(storedOrder.isPresent());
        assertTrue(storedCustody.isPresent());
        assertEquals(MarketOrderStatus.CANCELLED, storedOrder.get().getStatus());
        assertEquals(MarketCustodyStatus.CLAIMABLE, storedCustody.get().getStatus());
        assertEquals(MarketOperationStatus.COMPLETED, cancelled.getOperationLog().getStatus());
        assertEquals(1, claimables.size());
        assertEquals(cancelled.getCustody().getCustodyId(), claimables.get(0).getCustodyId());
    }

    @Test
    public void createSellRejectsRequestIdSemanticConflictOnPostgres() {
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
        StandardizedSpotMarketService recoveryServiceTarget = new StandardizedSpotMarketService(
            marketInfrastructure.getOrderBookRepository(), marketInfrastructure.getCustodyInventoryRepository(),
            marketInfrastructure.getOperationLogRepository(), marketInfrastructure.getTradeRecordRepository(),
            new FailAfterCallbackTransactionRunner(marketInfrastructure.getTransactionRunner()), settlementFacade);

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
        StandardizedSpotMarketService.CreateSellOrderResult created = marketService.createSellOrder(
            new CreateSellOrderCommand("req-market-claim-sell", "player-claim", "test-server",
                "minecraft:stone:0", 5L, true, 14L));
        StandardizedSpotMarketService.CancelSellOrderResult cancelled = marketService.cancelSellOrder(
            new CancelSellOrderCommand("req-market-claim-cancel", "player-claim", "test-server",
                created.getOrder().getOrderId()));

        RecordingClaimDeliveryPort claimDeliveryPort = new RecordingClaimDeliveryPort();
        StandardizedSpotMarketService claimService = new StandardizedSpotMarketService(
            marketInfrastructure.getOrderBookRepository(), marketInfrastructure.getCustodyInventoryRepository(),
            marketInfrastructure.getOperationLogRepository(), marketInfrastructure.getTradeRecordRepository(),
            marketInfrastructure.getTransactionRunner(), null, new com.jsirgalaxybase.modules.core.market.application.StandardizedMarketProductParser(), claimDeliveryPort);

        StandardizedSpotMarketService.ClaimMarketAssetResult claimed = claimService.claimMarketAsset(
            new ClaimMarketAssetCommand("req-market-claim-asset", "player-claim", "test-server",
                cancelled.getCustody().getCustodyId()));

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
        private final List<FrozenBalanceCommand> releaseCommands = new ArrayList<FrozenBalanceCommand>();
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
            return emptyPostingResult();
        }

        @Override
        public BankPostingResult releaseBuyerFunds(FrozenBalanceCommand command) {
            releaseCommands.add(command);
            return emptyPostingResult();
        }

        @Override
        public BankPostingResult settleFromFrozenFunds(InternalTransferCommand command) {
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