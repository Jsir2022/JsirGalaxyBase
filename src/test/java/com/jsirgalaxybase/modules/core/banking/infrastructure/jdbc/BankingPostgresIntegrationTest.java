package com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.application.command.FrozenBalanceCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.InternalTransferCommand;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountStatus;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;
import com.jsirgalaxybase.modules.core.banking.domain.BankBusinessType;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransaction;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransactionType;
import com.jsirgalaxybase.modules.core.banking.repository.BankAccountRepository;

public class BankingPostgresIntegrationTest {

    private PostgresTestConfig config;
    private PostgresTestContext testContext;
    private JdbcConnectionManager connectionManager;
    private JdbcBankAccountRepository accountRepository;
    private JdbcBankTransactionRepository transactionRepository;
    private JdbcLedgerEntryRepository ledgerEntryRepository;
    private JdbcCoinExchangeRecordRepository coinExchangeRecordRepository;
    private JdbcBankingTransactionRunner transactionRunner;
    private BankingApplicationService bankingService;

    @Before
    public void setUp() throws Exception {
        config = PostgresTestConfig.resolve();
        Assume.assumeTrue("PostgreSQL integration config not available", config.isConfigured());
        Assume.assumeTrue("PostgreSQL integration config is not reachable", config.canConnect());

        testContext = PostgresTestContext.create(config);
        connectionManager = new JdbcConnectionManager(testContext.getSchemaDataSource());
        accountRepository = new JdbcBankAccountRepository(connectionManager);
        transactionRepository = new JdbcBankTransactionRepository(connectionManager);
        ledgerEntryRepository = new JdbcLedgerEntryRepository(connectionManager);
        coinExchangeRecordRepository = new JdbcCoinExchangeRecordRepository(connectionManager);
        transactionRunner = new JdbcBankingTransactionRunner(connectionManager);
        bankingService = new BankingApplicationService(
            accountRepository,
            transactionRepository,
            ledgerEntryRepository,
            coinExchangeRecordRepository,
            transactionRunner);
    }

    @After
    public void tearDown() {
        if (testContext != null) {
            testContext.close();
        }
    }

    @Test
    public void saveIfOwnerAbsentIsAtomicOnPostgres() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Optional<BankAccount>> first = executor.submit(awaitAndRun(ready, start, new Callable<Optional<BankAccount>>() {

                @Override
                public Optional<BankAccount> call() {
                    return accountRepository.saveIfOwnerAbsent(playerAccountDraft(
                        "ACCT-PG-1",
                        "player-concurrent",
                        "Concurrent Player A",
                        "{\"source\":\"first\"}"));
                }
            }));
            Future<Optional<BankAccount>> second = executor.submit(awaitAndRun(ready, start, new Callable<Optional<BankAccount>>() {

                @Override
                public Optional<BankAccount> call() {
                    return accountRepository.saveIfOwnerAbsent(playerAccountDraft(
                        "ACCT-PG-2",
                        "player-concurrent",
                        "Concurrent Player B",
                        "{\"source\":\"second\"}"));
                }
            }));

            assertTrue("Concurrent account inserts did not become ready in time", ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int inserted = countPresent(first.get()) + countPresent(second.get());
            assertEquals(1, inserted);
            assertEquals(1L, countRows("bank_account"));

            BankAccount stored = accountRepository.findByOwner(
                BankingConstants.OWNER_TYPE_PLAYER_UUID,
                "player-concurrent",
                BankingConstants.DEFAULT_CURRENCY_CODE).get();
            assertTrue(Arrays.asList("ACCT-PG-1", "ACCT-PG-2").contains(stored.getAccountNo()));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void saveIfRequestAbsentIsAtomicOnPostgres() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Optional<BankTransaction>> first = executor.submit(awaitAndRun(ready, start, new Callable<Optional<BankTransaction>>() {

                @Override
                public Optional<BankTransaction> call() {
                    return transactionRepository.saveIfRequestAbsent(transactionDraft(
                        "req-jdbc-atomic",
                        BankTransactionType.SYSTEM_GRANT,
                        BankBusinessType.ADMIN_ADJUSTMENT,
                        "grant:1:2",
                        "test-server",
                        BankingConstants.OPERATOR_TYPE_ADMIN,
                        "Console",
                        "player-a"));
                }
            }));
            Future<Optional<BankTransaction>> second = executor.submit(awaitAndRun(ready, start, new Callable<Optional<BankTransaction>>() {

                @Override
                public Optional<BankTransaction> call() {
                    return transactionRepository.saveIfRequestAbsent(transactionDraft(
                        "req-jdbc-atomic",
                        BankTransactionType.SYSTEM_GRANT,
                        BankBusinessType.ADMIN_ADJUSTMENT,
                        "grant:1:2:duplicate",
                        "test-server",
                        BankingConstants.OPERATOR_TYPE_ADMIN,
                        "OtherConsole",
                        "player-a"));
                }
            }));

            assertTrue("Concurrent request inserts did not become ready in time", ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int inserted = countPresent(first.get()) + countPresent(second.get());
            assertEquals(1, inserted);
            assertEquals(1L, countRows("bank_transaction"));
            assertTrue(transactionRepository.findByRequestId("req-jdbc-atomic").isPresent());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void factoryCreateUsesCurrentSchemaForInitializationValidation() {
        BankingInfrastructure infrastructure = JdbcBankingInfrastructureFactory.create(
            testContext.getSchemaJdbcUrl(),
            config.username,
            config.password);

        BankAccount created = infrastructure.getBankAccountRepository().save(
            playerAccountDraft("ACCT-FACTORY-1", "player-factory", "Factory Player", "{}", 120L));

        assertTrue(infrastructure.getBankAccountRepository().findByOwner(
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            "player-factory",
            BankingConstants.DEFAULT_CURRENCY_CODE).isPresent());
        assertEquals(created.getAccountId(), infrastructure.getBankAccountRepository().findById(created.getAccountId()).get()
            .getAccountId());
    }

    @Test
    public void factoryCreateRejectsMissingTablesInCurrentSchema() throws Exception {
        PostgresTestContext emptyContext = PostgresTestContext.createEmpty(config);
        try {
            JdbcBankingInfrastructureFactory.create(emptyContext.getSchemaJdbcUrl(), config.username, config.password);
            fail("Expected missing-table validation failure");
        } catch (BankingException exception) {
            assertTrue(exception.getMessage().contains("Required banking table is missing"));
        } finally {
            emptyContext.close();
        }
    }

    @Test
    public void freezeFundsUpdatesAvailableAndFrozenBalancesOnPostgres() {
        BankAccount playerAccount = accountRepository.save(playerAccountDraft(
            "ACCT-FREEZE-1",
            "player-freeze",
            "Freeze Player",
            "{}",
            500L,
            0L));

        BankPostingResult result = bankingService.freezeFunds(frozenBalanceCommand(
            "req-jdbc-freeze",
            BankTransactionType.MARKET_FUNDS_FREEZE,
            BankBusinessType.MARKET_ORDER_FREEZE,
            playerAccount.getAccountId(),
            120L,
            "player-freeze"));

        assertEquals(380L, affectedBalance(result, playerAccount.getAccountId()));
        assertEquals(120L, affectedFrozenBalance(result, playerAccount.getAccountId()));
        assertEquals(380L, accountRepository.findById(playerAccount.getAccountId()).get().getAvailableBalance());
        assertEquals(120L, accountRepository.findById(playerAccount.getAccountId()).get().getFrozenBalance());
        assertEquals(1L, countRows("bank_transaction"));
        assertEquals(1L, countRows("ledger_entry"));
    }

    @Test
    public void freezeFundsReplayUsesHistoricalAvailableAndFrozenBalancesOnPostgres() {
        BankAccount playerAccount = accountRepository.save(playerAccountDraft(
            "ACCT-FREEZE-REPLAY",
            "player-freeze-replay",
            "Freeze Replay",
            "{}",
            500L,
            0L));

        FrozenBalanceCommand original = frozenBalanceCommand(
            "req-jdbc-freeze-replay",
            BankTransactionType.MARKET_FUNDS_FREEZE,
            BankBusinessType.MARKET_ORDER_FREEZE,
            playerAccount.getAccountId(),
            120L,
            "player-freeze-replay");
        bankingService.freezeFunds(original);

        BankAccount afterOriginal = accountRepository.findById(playerAccount.getAccountId()).get();
        accountRepository.updateBalances(playerAccount.getAccountId(), 310L, 10L, afterOriginal.getVersion());

        BankPostingResult replay = bankingService.freezeFunds(original);

        assertEquals(380L, affectedBalance(replay, playerAccount.getAccountId()));
        assertEquals(120L, affectedFrozenBalance(replay, playerAccount.getAccountId()));
        assertEquals(310L, accountRepository.findById(playerAccount.getAccountId()).get().getAvailableBalance());
        assertEquals(10L, accountRepository.findById(playerAccount.getAccountId()).get().getFrozenBalance());
        assertEquals(1L, countRows("bank_transaction"));
        assertEquals(1L, countRows("ledger_entry"));
    }

    @Test
    public void freezeFundsRejectsSemanticConflictOnPostgres() {
        BankAccount playerAccount = accountRepository.save(playerAccountDraft(
            "ACCT-FREEZE-CONFLICT",
            "player-freeze-conflict",
            "Freeze Conflict",
            "{}",
            500L,
            0L));

        bankingService.freezeFunds(frozenBalanceCommand(
            "req-jdbc-freeze-conflict",
            BankTransactionType.MARKET_FUNDS_FREEZE,
            BankBusinessType.MARKET_ORDER_FREEZE,
            playerAccount.getAccountId(),
            120L,
            "player-freeze-conflict"));

        try {
            bankingService.freezeFunds(frozenBalanceCommand(
                "req-jdbc-freeze-conflict",
                BankTransactionType.MARKET_FUNDS_FREEZE,
                BankBusinessType.MARKET_ORDER_FREEZE,
                playerAccount.getAccountId(),
                130L,
                "player-freeze-conflict"));
            fail("Expected freeze request conflict to be rejected");
        } catch (BankingException exception) {
            assertTrue(exception.getMessage().contains("requestId conflicts with existing transaction"));
            assertTrue(exception.getMessage().contains("amount"));
        }
    }

    @Test
    public void releaseFundsUpdatesAvailableAndFrozenBalancesOnPostgres() {
        BankAccount playerAccount = accountRepository.save(playerAccountDraft(
            "ACCT-RELEASE-1",
            "player-release",
            "Release Player",
            "{}",
            320L,
            180L));

        BankPostingResult result = bankingService.releaseFunds(frozenBalanceCommand(
            "req-jdbc-release",
            BankTransactionType.MARKET_FUNDS_RELEASE,
            BankBusinessType.MARKET_ORDER_CANCEL_RELEASE,
            playerAccount.getAccountId(),
            80L,
            "player-release"));

        assertEquals(400L, affectedBalance(result, playerAccount.getAccountId()));
        assertEquals(100L, affectedFrozenBalance(result, playerAccount.getAccountId()));
        assertEquals(400L, accountRepository.findById(playerAccount.getAccountId()).get().getAvailableBalance());
        assertEquals(100L, accountRepository.findById(playerAccount.getAccountId()).get().getFrozenBalance());
        assertEquals(1L, countRows("bank_transaction"));
        assertEquals(1L, countRows("ledger_entry"));
    }

    @Test
    public void releaseFundsReplayUsesHistoricalAvailableAndFrozenBalancesOnPostgres() {
        BankAccount playerAccount = accountRepository.save(playerAccountDraft(
            "ACCT-RELEASE-REPLAY",
            "player-release-replay",
            "Release Replay",
            "{}",
            320L,
            180L));

        FrozenBalanceCommand original = frozenBalanceCommand(
            "req-jdbc-release-replay",
            BankTransactionType.MARKET_FUNDS_RELEASE,
            BankBusinessType.MARKET_ORDER_CANCEL_RELEASE,
            playerAccount.getAccountId(),
            80L,
            "player-release-replay");
        bankingService.releaseFunds(original);

        BankAccount afterOriginal = accountRepository.findById(playerAccount.getAccountId()).get();
        accountRepository.updateBalances(playerAccount.getAccountId(), 260L, 15L, afterOriginal.getVersion());

        BankPostingResult replay = bankingService.releaseFunds(original);

        assertEquals(400L, affectedBalance(replay, playerAccount.getAccountId()));
        assertEquals(100L, affectedFrozenBalance(replay, playerAccount.getAccountId()));
        assertEquals(260L, accountRepository.findById(playerAccount.getAccountId()).get().getAvailableBalance());
        assertEquals(15L, accountRepository.findById(playerAccount.getAccountId()).get().getFrozenBalance());
        assertEquals(1L, countRows("bank_transaction"));
        assertEquals(1L, countRows("ledger_entry"));
    }

    @Test
    public void settleFrozenTransferMovesFundsFromFrozenBalanceOnPostgres() {
        BankAccount buyerAccount = accountRepository.save(playerAccountDraft(
            "ACCT-BUYER-1",
            "buyer-a",
            "Buyer A",
            "{}",
            400L,
            220L));
        BankAccount sellerAccount = accountRepository.save(playerAccountDraft(
            "ACCT-SELLER-1",
            "seller-a",
            "Seller A",
            "{}",
            150L,
            0L));

        BankPostingResult result = bankingService.settleFrozenTransfer(internalTransferCommand(
            "req-jdbc-settle-frozen",
            buyerAccount.getAccountId(),
            sellerAccount.getAccountId(),
            90L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_SYSTEM,
            "market",
            "buyer-a",
            BankTransactionType.MARKET_SETTLEMENT_FROM_FROZEN,
            BankBusinessType.MARKET_ORDER_SETTLEMENT));

        assertEquals(400L, affectedBalance(result, buyerAccount.getAccountId()));
        assertEquals(130L, affectedFrozenBalance(result, buyerAccount.getAccountId()));
        assertEquals(240L, affectedBalance(result, sellerAccount.getAccountId()));
        assertEquals(0L, affectedFrozenBalance(result, sellerAccount.getAccountId()));
        assertEquals(400L, accountRepository.findById(buyerAccount.getAccountId()).get().getAvailableBalance());
        assertEquals(130L, accountRepository.findById(buyerAccount.getAccountId()).get().getFrozenBalance());
        assertEquals(240L, accountRepository.findById(sellerAccount.getAccountId()).get().getAvailableBalance());
        assertEquals(0L, accountRepository.findById(sellerAccount.getAccountId()).get().getFrozenBalance());
        assertEquals(1L, countRows("bank_transaction"));
        assertEquals(2L, countRows("ledger_entry"));
    }

    @Test
    public void settleFrozenTransferReplayUsesHistoricalAvailableAndFrozenBalancesOnPostgres() {
        BankAccount buyerAccount = accountRepository.save(playerAccountDraft(
            "ACCT-BUYER-REPLAY",
            "buyer-replay",
            "Buyer Replay",
            "{}",
            400L,
            220L));
        BankAccount sellerAccount = accountRepository.save(playerAccountDraft(
            "ACCT-SELLER-REPLAY",
            "seller-replay",
            "Seller Replay",
            "{}",
            150L,
            0L));

        InternalTransferCommand original = internalTransferCommand(
            "req-jdbc-settle-replay",
            buyerAccount.getAccountId(),
            sellerAccount.getAccountId(),
            90L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_SYSTEM,
            "market",
            "buyer-replay",
            BankTransactionType.MARKET_SETTLEMENT_FROM_FROZEN,
            BankBusinessType.MARKET_ORDER_SETTLEMENT);
        bankingService.settleFrozenTransfer(original);

        BankAccount buyerAfterOriginal = accountRepository.findById(buyerAccount.getAccountId()).get();
        BankAccount sellerAfterOriginal = accountRepository.findById(sellerAccount.getAccountId()).get();
        accountRepository.updateBalances(buyerAccount.getAccountId(), 250L, 20L, buyerAfterOriginal.getVersion());
        accountRepository.updateBalances(sellerAccount.getAccountId(), 500L, 0L, sellerAfterOriginal.getVersion());

        BankPostingResult replay = bankingService.settleFrozenTransfer(original);

        assertEquals(400L, affectedBalance(replay, buyerAccount.getAccountId()));
        assertEquals(130L, affectedFrozenBalance(replay, buyerAccount.getAccountId()));
        assertEquals(240L, affectedBalance(replay, sellerAccount.getAccountId()));
        assertEquals(0L, affectedFrozenBalance(replay, sellerAccount.getAccountId()));
        assertEquals(250L, accountRepository.findById(buyerAccount.getAccountId()).get().getAvailableBalance());
        assertEquals(20L, accountRepository.findById(buyerAccount.getAccountId()).get().getFrozenBalance());
        assertEquals(500L, accountRepository.findById(sellerAccount.getAccountId()).get().getAvailableBalance());
        assertEquals(0L, accountRepository.findById(sellerAccount.getAccountId()).get().getFrozenBalance());
        assertEquals(1L, countRows("bank_transaction"));
        assertEquals(2L, countRows("ledger_entry"));
    }

    @Test
    public void postInternalTransferReplayUsesHistoricalLedgerBalancesOnPostgres() {
        BankAccount systemAccount = accountRepository.save(systemAccountDraft("SYS-OPS-1", "SYSTEM_OPERATIONS", 500L));
        BankAccount playerAccount = accountRepository.save(playerAccountDraft("ACCT-PLAYER-1", "player-a", "Player A", "{}", 300L));

        InternalTransferCommand original = internalTransferCommand(
            "req-jdbc-replay",
            systemAccount.getAccountId(),
            playerAccount.getAccountId(),
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT);
        bankingService.postInternalTransfer(original);
        bankingService.postInternalTransfer(internalTransferCommand(
            "req-jdbc-followup",
            systemAccount.getAccountId(),
            playerAccount.getAccountId(),
            20L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT));

        BankPostingResult replay = bankingService.postInternalTransfer(original);

        assertEquals(450L, affectedBalance(replay, systemAccount.getAccountId()));
        assertEquals(350L, affectedBalance(replay, playerAccount.getAccountId()));
        assertEquals(430L, accountRepository.findById(systemAccount.getAccountId()).get().getAvailableBalance());
        assertEquals(370L, accountRepository.findById(playerAccount.getAccountId()).get().getAvailableBalance());
    }

    @Test
    public void postInternalTransferRejectsSemanticConflictOnPostgres() {
        BankAccount systemAccount = accountRepository.save(systemAccountDraft("SYS-OPS-2", "SYSTEM_OPERATIONS", 500L));
        BankAccount playerAccount = accountRepository.save(playerAccountDraft("ACCT-PLAYER-2", "player-a", "Player A", "{}", 300L));

        InternalTransferCommand original = internalTransferCommand(
            "req-jdbc-conflict",
            systemAccount.getAccountId(),
            playerAccount.getAccountId(),
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT);
        bankingService.postInternalTransfer(original);

        try {
            bankingService.postInternalTransfer(internalTransferCommand(
                "req-jdbc-conflict",
                systemAccount.getAccountId(),
                playerAccount.getAccountId(),
                60L,
                "test-server",
                BankingConstants.OPERATOR_TYPE_ADMIN,
                "Console",
                "player-a",
                BankTransactionType.SYSTEM_GRANT,
                BankBusinessType.ADMIN_ADJUSTMENT));
            fail("Expected request conflict to be rejected");
        } catch (BankingException exception) {
            assertTrue(exception.getMessage().contains("requestId conflicts with existing transaction"));
            assertTrue(exception.getMessage().contains("amount"));
        }
    }

    @Test
    public void postInternalTransferRejectsTransactionTypeConflictOnPostgres() {
        BankAccount systemAccount = accountRepository.save(systemAccountDraft("SYS-OPS-3", "SYSTEM_OPERATIONS", 500L));
        BankAccount playerAccount = accountRepository.save(playerAccountDraft("ACCT-PLAYER-3", "player-a", "Player A", "{}", 300L));

        InternalTransferCommand original = internalTransferCommand(
            "req-jdbc-conflict-transaction-type",
            systemAccount.getAccountId(),
            playerAccount.getAccountId(),
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT);
        bankingService.postInternalTransfer(original);

        try {
            bankingService.postInternalTransfer(internalTransferCommand(
                "req-jdbc-conflict-transaction-type",
                systemAccount.getAccountId(),
                playerAccount.getAccountId(),
                50L,
                "test-server",
                BankingConstants.OPERATOR_TYPE_ADMIN,
                "Console",
                "player-a",
                BankTransactionType.SYSTEM_DEDUCT,
                BankBusinessType.ADMIN_ADJUSTMENT));
            fail("Expected transactionType conflict to be rejected");
        } catch (BankingException exception) {
            assertTrue(exception.getMessage().contains("requestId conflicts with existing transaction"));
            assertTrue(exception.getMessage().contains("transactionType"));
        }
    }

    @Test
    public void transactionRollbackPreventsHalfCommittedJdbcWrites() {
        JdbcBankAccountRepository realAccountRepository = new JdbcBankAccountRepository(connectionManager);
        ThrowingAfterSecondUpdateAccountRepository failingAccountRepository = new ThrowingAfterSecondUpdateAccountRepository(
            realAccountRepository);
        BankingApplicationService failingService = new BankingApplicationService(
            failingAccountRepository,
            transactionRepository,
            ledgerEntryRepository,
            coinExchangeRecordRepository,
            transactionRunner);

        BankAccount systemAccount = realAccountRepository.save(systemAccountDraft("SYS-OPS-ROLLBACK", "SYSTEM_OPERATIONS", 500L));
        BankAccount playerAccount = realAccountRepository.save(playerAccountDraft("ACCT-ROLLBACK", "player-a", "Player A", "{}", 300L));

        try {
            failingService.postInternalTransfer(internalTransferCommand(
                "req-jdbc-rollback",
                systemAccount.getAccountId(),
                playerAccount.getAccountId(),
                50L,
                "test-server",
                BankingConstants.OPERATOR_TYPE_ADMIN,
                "Console",
                "player-a",
                BankTransactionType.SYSTEM_GRANT,
                BankBusinessType.ADMIN_ADJUSTMENT));
            fail("Expected injected update failure");
        } catch (BankingException exception) {
            assertTrue(exception.getMessage().contains("forced failure after jdbc updates"));
        }

        assertFalse(transactionRepository.findByRequestId("req-jdbc-rollback").isPresent());
        assertEquals(0L, countRows("bank_transaction"));
        assertEquals(0L, countRows("ledger_entry"));
        assertEquals(500L, realAccountRepository.findById(systemAccount.getAccountId()).get().getAvailableBalance());
        assertEquals(300L, realAccountRepository.findById(playerAccount.getAccountId()).get().getAvailableBalance());
    }

    @Test
    public void freezeFundsRollbackPreventsHalfCommittedJdbcWrites() {
        JdbcBankAccountRepository realAccountRepository = new JdbcBankAccountRepository(connectionManager);
        ThrowingAfterFirstUpdateAccountRepository failingAccountRepository = new ThrowingAfterFirstUpdateAccountRepository(
            realAccountRepository);
        BankingApplicationService failingService = new BankingApplicationService(
            failingAccountRepository,
            transactionRepository,
            ledgerEntryRepository,
            coinExchangeRecordRepository,
            transactionRunner);

        BankAccount playerAccount = realAccountRepository.save(playerAccountDraft(
            "ACCT-FREEZE-ROLLBACK",
            "player-freeze-rollback",
            "Freeze Rollback",
            "{}",
            500L,
            0L));

        try {
            failingService.freezeFunds(frozenBalanceCommand(
                "req-jdbc-freeze-rollback",
                BankTransactionType.MARKET_FUNDS_FREEZE,
                BankBusinessType.MARKET_ORDER_FREEZE,
                playerAccount.getAccountId(),
                120L,
                "player-freeze-rollback"));
            fail("Expected injected update failure");
        } catch (BankingException exception) {
            assertTrue(exception.getMessage().contains("forced failure after jdbc updates"));
        }

        assertFalse(transactionRepository.findByRequestId("req-jdbc-freeze-rollback").isPresent());
        assertEquals(0L, countRows("bank_transaction"));
        assertEquals(0L, countRows("ledger_entry"));
        assertEquals(500L, realAccountRepository.findById(playerAccount.getAccountId()).get().getAvailableBalance());
        assertEquals(0L, realAccountRepository.findById(playerAccount.getAccountId()).get().getFrozenBalance());
    }

    private <T> Callable<T> awaitAndRun(final CountDownLatch ready, final CountDownLatch start, final Callable<T> task) {
        return new Callable<T>() {

            @Override
            public T call() throws Exception {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting for concurrent start");
                }
                return task.call();
            }
        };
    }

    private int countPresent(Optional<?> value) {
        return value.isPresent() ? 1 : 0;
    }

    private long affectedBalance(BankPostingResult result, long accountId) {
        for (BankAccount account : result.getAffectedAccounts()) {
            if (account.getAccountId() == accountId) {
                return account.getAvailableBalance();
            }
        }
        fail("Expected affected account " + accountId);
        return -1L;
    }

    private long affectedFrozenBalance(BankPostingResult result, long accountId) {
        for (BankAccount account : result.getAffectedAccounts()) {
            if (account.getAccountId() == accountId) {
                return account.getFrozenBalance();
            }
        }
        fail("Expected affected account " + accountId);
        return -1L;
    }

    private long countRows(String tableName) {
        return queryForLong("SELECT COUNT(*) FROM " + tableName);
    }

    private long queryForLong(String sql) {
        try (Connection connection = testContext.getSchemaDataSource().getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException exception) {
            throw new AssertionError("Failed to run SQL: " + sql, exception);
        }
    }

    private InternalTransferCommand internalTransferCommand(String requestId, long fromAccountId, long toAccountId,
        long amount, String sourceServerId, String operatorType, String operatorRef, String playerRef,
        BankTransactionType transactionType, BankBusinessType businessType) {
        return new InternalTransferCommand(
            requestId,
            transactionType,
            businessType,
            fromAccountId,
            toAccountId,
            sourceServerId,
            operatorType,
            operatorRef,
            playerRef,
            amount,
            "jdbc integration",
            "internal:" + fromAccountId + ":" + toAccountId,
            "{}");
    }

    private FrozenBalanceCommand frozenBalanceCommand(String requestId, BankTransactionType transactionType,
        BankBusinessType businessType, long accountId, long amount, String playerRef) {
        return new FrozenBalanceCommand(
            requestId,
            transactionType,
            businessType,
            accountId,
            "test-server",
            BankingConstants.OPERATOR_TYPE_SYSTEM,
            "market",
            playerRef,
            amount,
            "jdbc integration",
            "market:" + accountId,
            "{}");
    }

    private BankAccount playerAccountDraft(String accountNo, String ownerRef, String displayName, String metadataJson) {
        return playerAccountDraft(accountNo, ownerRef, displayName, metadataJson, 0L);
    }

    private BankAccount playerAccountDraft(String accountNo, String ownerRef, String displayName, String metadataJson,
        long availableBalance) {
        return playerAccountDraft(accountNo, ownerRef, displayName, metadataJson, availableBalance, 0L);
    }

    private BankAccount playerAccountDraft(String accountNo, String ownerRef, String displayName, String metadataJson,
        long availableBalance, long frozenBalance) {
        Instant now = Instant.now();
        return new BankAccount(
            0L,
            accountNo,
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            ownerRef,
            BankingConstants.DEFAULT_CURRENCY_CODE,
            availableBalance,
            frozenBalance,
            BankAccountStatus.ACTIVE,
            0L,
            displayName,
            metadataJson,
            now,
            now);
    }

    private BankAccount systemAccountDraft(String accountNo, String ownerRef, long availableBalance) {
        Instant now = Instant.now();
        return new BankAccount(
            0L,
            accountNo,
            BankAccountType.SYSTEM,
            BankingConstants.OWNER_TYPE_SYSTEM,
            ownerRef,
            BankingConstants.DEFAULT_CURRENCY_CODE,
            availableBalance,
            0L,
            BankAccountStatus.ACTIVE,
            0L,
            ownerRef,
            "{}",
            now,
            now);
    }

    private BankTransaction transactionDraft(String requestId, BankTransactionType transactionType,
        BankBusinessType businessType, String businessRef, String sourceServerId, String operatorType,
        String operatorRef, String playerRef) {
        return new BankTransaction(
            0L,
            requestId,
            transactionType,
            businessType,
            businessRef,
            sourceServerId,
            operatorType,
            operatorRef,
            playerRef,
            "jdbc integration",
            "{}",
            Instant.now());
    }

    private static final class ThrowingAfterSecondUpdateAccountRepository implements BankAccountRepository {

        private final JdbcBankAccountRepository delegate;
        private int updateCount;

        private ThrowingAfterSecondUpdateAccountRepository(JdbcBankAccountRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<BankAccount> findById(long accountId) {
            return delegate.findById(accountId);
        }

        @Override
        public Optional<BankAccount> findByOwner(String ownerType, String ownerRef, String currencyCode) {
            return delegate.findByOwner(ownerType, ownerRef, currencyCode);
        }

        @Override
        public BankAccount save(BankAccount account) {
            return delegate.save(account);
        }

        @Override
        public Optional<BankAccount> saveIfOwnerAbsent(BankAccount account) {
            return delegate.saveIfOwnerAbsent(account);
        }

        @Override
        public BankAccount lockById(long accountId) {
            return delegate.lockById(accountId);
        }

        @Override
        public java.util.List<BankAccount> lockByIdsInOrder(java.util.List<Long> accountIds) {
            return delegate.lockByIdsInOrder(accountIds);
        }

        @Override
        public void updateBalances(long accountId, long availableBalance, long frozenBalance, long expectedVersion) {
            delegate.updateBalances(accountId, availableBalance, frozenBalance, expectedVersion);
            updateCount++;
            if (updateCount == 2) {
                throw new BankingException("forced failure after jdbc updates");
            }
        }
    }

    private static final class ThrowingAfterFirstUpdateAccountRepository implements BankAccountRepository {

        private final JdbcBankAccountRepository delegate;
        private int updateCount;

        private ThrowingAfterFirstUpdateAccountRepository(JdbcBankAccountRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<BankAccount> findById(long accountId) {
            return delegate.findById(accountId);
        }

        @Override
        public Optional<BankAccount> findByOwner(String ownerType, String ownerRef, String currencyCode) {
            return delegate.findByOwner(ownerType, ownerRef, currencyCode);
        }

        @Override
        public BankAccount save(BankAccount account) {
            return delegate.save(account);
        }

        @Override
        public Optional<BankAccount> saveIfOwnerAbsent(BankAccount account) {
            return delegate.saveIfOwnerAbsent(account);
        }

        @Override
        public BankAccount lockById(long accountId) {
            return delegate.lockById(accountId);
        }

        @Override
        public java.util.List<BankAccount> lockByIdsInOrder(java.util.List<Long> accountIds) {
            return delegate.lockByIdsInOrder(accountIds);
        }

        @Override
        public void updateBalances(long accountId, long availableBalance, long frozenBalance, long expectedVersion) {
            delegate.updateBalances(accountId, availableBalance, frozenBalance, expectedVersion);
            updateCount++;
            if (updateCount == 1) {
                throw new BankingException("forced failure after jdbc updates");
            }
        }
    }

    private static final class PostgresTestContext {

        private static final String BANKING_DDL = loadBankingDdl();

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
            return create(config, true);
        }

        private static PostgresTestContext createEmpty(PostgresTestConfig config) throws SQLException {
            return create(config, false);
        }

        private static PostgresTestContext create(PostgresTestConfig config, boolean applyDdl) throws SQLException {
            DriverManagerDataSource baseDataSource = new DriverManagerDataSource(
                config.jdbcUrl,
                config.username,
                config.password);
            String schemaName = "banking_it_" + UUID.randomUUID().toString().replace("-", "");
            createSchema(baseDataSource, schemaName);
            String schemaJdbcUrl = withCurrentSchema(config.jdbcUrl, schemaName);
            DriverManagerDataSource schemaDataSource = new DriverManagerDataSource(
                schemaJdbcUrl,
                config.username,
                config.password);
            if (applyDdl) {
                applyDdl(schemaDataSource, schemaName);
            }
            return new PostgresTestContext(baseDataSource, schemaDataSource, schemaName, schemaJdbcUrl);
        }

        private DriverManagerDataSource getSchemaDataSource() {
            return schemaDataSource;
        }

        private String getSchemaJdbcUrl() {
            return schemaJdbcUrl;
        }

        private void close() {
            try (Connection connection = baseDataSource.getConnection();
                Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            } catch (SQLException ignored) {
                // cleanup best effort only
            }
        }

        private static void createSchema(DriverManagerDataSource baseDataSource, String schemaName) throws SQLException {
            try (Connection connection = baseDataSource.getConnection();
                Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA \"" + schemaName + "\"");
            }
        }

        private static void applyDdl(DriverManagerDataSource schemaDataSource, String schemaName) throws SQLException {
            try (Connection connection = schemaDataSource.getConnection();
                Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO \"" + schemaName + "\", public");
                statement.execute(BANKING_DDL);
            }
        }

        private static String withCurrentSchema(String jdbcUrl, String schemaName) {
            return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "currentSchema=" + schemaName;
        }

        private static String loadBankingDdl() {
            Path ddlPath = Paths.get("docs", "banking-postgresql-ddl.sql");
            try {
                return new String(Files.readAllBytes(ddlPath), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to load banking-postgresql-ddl.sql", exception);
            }
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
            String jdbcUrl = firstNonBlank(
                System.getProperty("jgb.banking.it.jdbcUrl"),
                System.getenv("JGB_BANKING_IT_JDBC_URL"),
                readConfigValue("bankingJdbcUrl"));
            String username = firstNonBlank(
                System.getProperty("jgb.banking.it.username"),
                System.getenv("JGB_BANKING_IT_JDBC_USERNAME"),
                readConfigValue("bankingJdbcUsername"));
            String password = firstNonBlank(
                System.getProperty("jgb.banking.it.password"),
                System.getenv("JGB_BANKING_IT_JDBC_PASSWORD"),
                readConfigValue("bankingJdbcPassword"));
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