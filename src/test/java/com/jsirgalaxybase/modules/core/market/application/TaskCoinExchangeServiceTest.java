package com.jsirgalaxybase.modules.core.market.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountStatus;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransaction;
import com.jsirgalaxybase.modules.core.banking.domain.CoinExchangeRecord;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntry;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.repository.BankAccountRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankTransactionRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankingTransactionRunner;
import com.jsirgalaxybase.modules.core.banking.repository.CoinExchangeRecordRepository;
import com.jsirgalaxybase.modules.core.banking.repository.LedgerEntryRepository;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketLimitStatus;

public class TaskCoinExchangeServiceTest {

    @Test
    public void previewRegistryQuoteReturnsDisallowedFormalQuoteForUnsupportedTier() {
        TaskCoinExchangeService service = new TaskCoinExchangeService(minimalInfrastructure(), new TaskCoinExchangePlanner(),
            "s1-test");

        TaskCoinExchangeService.PreviewResult previewResult = service.previewRegistryQuote("player-a",
            "dreamcraft:item.CoinMinerV", 2);

        assertEquals(ExchangeMarketLimitStatus.DISALLOWED, previewResult.getFormalQuote().getLimitPolicy().getStatus());
        assertEquals("TASK_COIN_TIER_DISALLOWED", previewResult.getFormalQuote().getLimitPolicy().getReasonCode());
        assertEquals("dreamcraft:item.CoinMinerV", previewResult.getFormalQuote().getInputRegistryName());
        assertEquals("UNRESOLVED", previewResult.getFormalQuote().getInputFamily());
        assertEquals("UNRESOLVED", previewResult.getFormalQuote().getInputTier());
    }

    @Test
    public void requireExecutableRegistryQuoteRejectsDisallowedQuoteWithFormalNote() {
        TaskCoinExchangeService service = new TaskCoinExchangeService(minimalInfrastructure(), new TaskCoinExchangePlanner(),
            "s1-test");

        try {
            service.requireExecutableRegistryQuote("player-a", "dreamcraft:item.CoinMinerV", 2);
            fail("expected disallowed quote to reject execution path");
        } catch (MarketExchangeException expected) {
            assertTrue(expected.getMessage().contains("当前汇率市场 v1 只支持 BASE / I / II / III / IV 任务书硬币"));
            assertTrue(!expected.getMessage().contains("不属于汇率市场支持的任务书硬币资产对"));
        }
    }

    @Test
    public void previewRegistryQuoteStillRejectsNonTaskCoinAsset() {
        TaskCoinExchangeService service = new TaskCoinExchangeService(minimalInfrastructure(), new TaskCoinExchangePlanner(),
            "s1-test");

        try {
            service.previewRegistryQuote("player-a", "minecraft:stick", 1);
            fail("expected non-task-coin asset to be rejected");
        } catch (MarketExchangeException expected) {
            assertEquals("当前手持物品不属于汇率市场支持的任务书硬币资产对", expected.getMessage());
        }
    }

    private BankingInfrastructure minimalInfrastructure() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        accountRepository.addAccount(new BankAccount(10L, "PLY-A", BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID, "player-a", BankingConstants.DEFAULT_CURRENCY_CODE, 0L, 0L,
            BankAccountStatus.ACTIVE, 0L, "Player A", "{}", Instant.now(), Instant.now()));
        BankingApplicationService bankingService = new BankingApplicationService(accountRepository,
            new FakeBankTransactionRepository(), new FakeLedgerEntryRepository(), new FakeCoinExchangeRecordRepository(),
            new DirectTransactionRunner());
        return new BankingInfrastructure(bankingService, accountRepository, new FakeBankTransactionRepository(),
            new FakeLedgerEntryRepository(), new FakeCoinExchangeRecordRepository(), new DirectTransactionRunner());
    }

    private static final class FakeBankAccountRepository implements BankAccountRepository {

        private final Map<Long, BankAccount> accountsById = new LinkedHashMap<Long, BankAccount>();
        private final Map<String, BankAccount> accountsByOwner = new HashMap<String, BankAccount>();
        private long nextId = 100L;

        private BankAccount addAccount(BankAccount account) {
            accountsById.put(Long.valueOf(account.getAccountId()), account);
            accountsByOwner.put(ownerKey(account.getOwnerType(), account.getOwnerRef(), account.getCurrencyCode()), account);
            nextId = Math.max(nextId, account.getAccountId() + 1L);
            return account;
        }

        @Override
        public Optional<BankAccount> findById(long accountId) {
            return Optional.ofNullable(accountsById.get(Long.valueOf(accountId)));
        }

        @Override
        public Optional<BankAccount> findByOwner(String ownerType, String ownerRef, String currencyCode) {
            return Optional.ofNullable(accountsByOwner.get(ownerKey(ownerType, ownerRef, currencyCode)));
        }

        @Override
        public BankAccount save(BankAccount account) {
            BankAccount persisted = new BankAccount(nextId++, account.getAccountNo(), account.getAccountType(),
                account.getOwnerType(), account.getOwnerRef(), account.getCurrencyCode(),
                account.getAvailableBalance(), account.getFrozenBalance(), account.getStatus(), account.getVersion(),
                account.getDisplayName(), account.getMetadataJson(), account.getCreatedAt(), account.getUpdatedAt());
            return addAccount(persisted);
        }

        @Override
        public Optional<BankAccount> saveIfOwnerAbsent(BankAccount account) {
            String key = ownerKey(account.getOwnerType(), account.getOwnerRef(), account.getCurrencyCode());
            if (accountsByOwner.containsKey(key)) {
                return Optional.empty();
            }
            return Optional.of(save(account));
        }

        @Override
        public BankAccount lockById(long accountId) {
            return findById(accountId).get();
        }

        @Override
        public List<BankAccount> lockByIdsInOrder(List<Long> accountIds) {
            List<Long> ordered = new ArrayList<Long>(accountIds);
            Collections.sort(ordered);
            List<BankAccount> accounts = new ArrayList<BankAccount>(ordered.size());
            for (Long accountId : ordered) {
                accounts.add(lockById(accountId.longValue()));
            }
            return accounts;
        }

        @Override
        public void updateBalances(long accountId, long availableBalance, long frozenBalance, long expectedVersion) {
            BankAccount current = lockById(accountId);
            addAccount(new BankAccount(current.getAccountId(), current.getAccountNo(), current.getAccountType(),
                current.getOwnerType(), current.getOwnerRef(), current.getCurrencyCode(), availableBalance,
                frozenBalance, current.getStatus(), expectedVersion + 1L, current.getDisplayName(),
                current.getMetadataJson(), current.getCreatedAt(), Instant.now()));
        }

        private String ownerKey(String ownerType, String ownerRef, String currencyCode) {
            return ownerType + "|" + ownerRef + "|" + currencyCode;
        }
    }

    private static final class FakeBankTransactionRepository implements BankTransactionRepository {

        private final Map<String, BankTransaction> transactionsByRequestId = new HashMap<String, BankTransaction>();
        private long nextId = 1000L;

        @Override
        public Optional<BankTransaction> findById(long transactionId) {
            for (BankTransaction transaction : transactionsByRequestId.values()) {
                if (transaction.getTransactionId() == transactionId) {
                    return Optional.of(transaction);
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<BankTransaction> findByRequestId(String requestId) {
            return Optional.ofNullable(transactionsByRequestId.get(requestId));
        }

        @Override
        public BankTransaction save(BankTransaction transaction) {
            BankTransaction persisted = new BankTransaction(nextId++, transaction.getRequestId(),
                transaction.getTransactionType(), transaction.getBusinessType(), transaction.getBusinessRef(),
                transaction.getSourceServerId(), transaction.getOperatorType(), transaction.getOperatorRef(),
                transaction.getPlayerRef(), transaction.getComment(), transaction.getExtraJson(),
                transaction.getCreatedAt());
            transactionsByRequestId.put(persisted.getRequestId(), persisted);
            return persisted;
        }

        @Override
        public Optional<BankTransaction> saveIfRequestAbsent(BankTransaction transaction) {
            if (transactionsByRequestId.containsKey(transaction.getRequestId())) {
                return Optional.empty();
            }
            return Optional.of(save(transaction));
        }
    }

    private static final class FakeLedgerEntryRepository implements LedgerEntryRepository {

        @Override
        public void appendEntries(List<LedgerEntry> entries) {}

        @Override
        public List<LedgerEntry> findByTransactionId(long transactionId) {
            return Collections.emptyList();
        }

        @Override
        public List<LedgerEntry> findRecentByAccountId(long accountId, int limit) {
            return Collections.emptyList();
        }
    }

    private static final class FakeCoinExchangeRecordRepository implements CoinExchangeRecordRepository {

        @Override
        public CoinExchangeRecord save(CoinExchangeRecord record) {
            return record;
        }

        @Override
        public Optional<CoinExchangeRecord> findByTransactionId(long transactionId) {
            return Optional.empty();
        }

        @Override
        public List<CoinExchangeRecord> findRecentByPlayerRef(String playerRef, int limit) {
            return Collections.emptyList();
        }
    }

    private static final class DirectTransactionRunner implements BankingTransactionRunner {

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