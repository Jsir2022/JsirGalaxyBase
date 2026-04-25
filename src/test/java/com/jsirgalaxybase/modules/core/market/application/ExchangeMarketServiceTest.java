package com.jsirgalaxybase.modules.core.market.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.application.command.OpenAccountCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountStatus;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransaction;
import com.jsirgalaxybase.modules.core.banking.domain.CoinExchangeRecord;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntry;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts;
import com.jsirgalaxybase.modules.core.banking.repository.BankAccountRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankTransactionRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankingTransactionRunner;
import com.jsirgalaxybase.modules.core.banking.repository.CoinExchangeRecordRepository;
import com.jsirgalaxybase.modules.core.banking.repository.LedgerEntryRepository;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketExecutionRequest;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketExecutionResult;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketLimitStatus;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketQuoteRequest;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketQuoteResult;

public class ExchangeMarketServiceTest {

    @Test
    public void quoteTaskCoinToStarcoinBuildsFormalRuleFields() {
        ExchangeMarketService service = new ExchangeMarketService(minimalInfrastructure(), new TaskCoinExchangePlanner(),
            "s1-test");

        ExchangeMarketQuoteResult quote = service.quoteTaskCoinToStarcoin(new ExchangeMarketQuoteRequest("req-quote-1",
            "player-a", "s1-test", "dreamcraft:item.CoinChemistII", 3L)).get();

        assertEquals("task-coin-to-starcoin", quote.getPairDefinition().getPairCode());
        assertEquals(TaskCoinExchangePlanner.RULE_VERSION, quote.getRuleVersion().getRuleKey());
        assertEquals(ExchangeMarketLimitStatus.ALLOWED, quote.getLimitPolicy().getStatus());
        assertEquals("TASK_COIN_RULE_APPLIED", quote.getLimitPolicy().getReasonCode());
        assertEquals("player-a", quote.getPlayerRef());
        assertEquals("s1-test", quote.getSourceServerId());
        assertEquals("dreamcraft:item.CoinChemistII", quote.getInputRegistryName());
        assertEquals("Chemist", quote.getInputFamily());
        assertEquals("II", quote.getInputTier());
        assertEquals(100L, quote.getInputUnitFaceValue());
        assertEquals(3L, quote.getInputQuantity());
        assertEquals(300L, quote.getInputTotalFaceValue());
        assertEquals(300L, quote.getEffectiveExchangeValue());
        assertEquals(300L, quote.getContributionValue());
        assertEquals(300L, quote.getExchangeRateNumerator());
        assertEquals(3L, quote.getExchangeRateDenominator());
        assertEquals(0, quote.getDiscountBasisPoints());
        assertTrue(quote.getNotes().contains("汇率市场 v1"));
    }

    @Test
    public void quoteTaskCoinToStarcoinReturnsDisallowedResultForUnsupportedTier() {
        ExchangeMarketService service = new ExchangeMarketService(minimalInfrastructure(), new TaskCoinExchangePlanner(),
            "s1-test");

        ExchangeMarketQuoteResult quote = service.quoteTaskCoinToStarcoin(new ExchangeMarketQuoteRequest("req-quote-2",
            "player-a", "s1-test", "dreamcraft:item.CoinMinerV", 1L)).get();

        assertEquals(ExchangeMarketLimitStatus.DISALLOWED, quote.getLimitPolicy().getStatus());
        assertEquals("TASK_COIN_TIER_DISALLOWED", quote.getLimitPolicy().getReasonCode());
        assertEquals("dreamcraft:item.CoinMinerV", quote.getInputRegistryName());
        assertEquals("UNRESOLVED", quote.getInputFamily());
        assertEquals("UNRESOLVED", quote.getInputTier());
        assertEquals(10000, quote.getDiscountBasisPoints());
    }

    @Test
    public void quoteTaskCoinToStarcoinReturnsFormalDisallowedResultForUnsupportedAsset() {
        ExchangeMarketService service = new ExchangeMarketService(minimalInfrastructure(), new TaskCoinExchangePlanner(),
            "s1-test");

        ExchangeMarketQuoteResult quote = service.quoteTaskCoinToStarcoin(new ExchangeMarketQuoteRequest("req-quote-3",
            "player-a", "s1-test", "minecraft:stone", 1L)).get();

        assertEquals(ExchangeMarketLimitStatus.DISALLOWED, quote.getLimitPolicy().getStatus());
        assertEquals("TASK_COIN_ASSET_UNSUPPORTED", quote.getLimitPolicy().getReasonCode());
        assertEquals("minecraft:stone", quote.getInputRegistryName());
        assertEquals("UNRESOLVED", quote.getInputFamily());
        assertEquals("UNRESOLVED", quote.getInputTier());
        assertTrue(quote.getNotes().contains("不属于汇率市场支持的任务书硬币资产对"));
    }

    @Test
    public void executeTaskCoinToStarcoinPropagatesRuleAndAuditFieldsIntoSettlement() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        accountRepository.addAccount(new BankAccount(10L, "PLY-A", BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID, "player-a", BankingConstants.DEFAULT_CURRENCY_CODE, 0L, 0L,
            BankAccountStatus.ACTIVE, 0L, "Player A", "{}", Instant.now(), Instant.now()));
        accountRepository.addAccount(new BankAccount(20L, "EXCH-RESERVE", BankAccountType.EXCHANGE_RESERVE,
            BankingConstants.OWNER_TYPE_PUBLIC_FUND_CODE, "EXCHANGE_RESERVE", BankingConstants.DEFAULT_CURRENCY_CODE,
            5000L, 0L, BankAccountStatus.ACTIVE, 0L, "Exchange Reserve", "{}", Instant.now(), Instant.now()));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerEntryRepository = new FakeLedgerEntryRepository();
        FakeCoinExchangeRecordRepository coinExchangeRecordRepository = new FakeCoinExchangeRecordRepository();
        BankingApplicationService bankingService = new BankingApplicationService(accountRepository, transactionRepository,
            ledgerEntryRepository, coinExchangeRecordRepository, new DirectTransactionRunner());
        BankingInfrastructure infrastructure = new BankingInfrastructure(bankingService, accountRepository,
            transactionRepository, ledgerEntryRepository, coinExchangeRecordRepository, new DirectTransactionRunner());
        ExchangeMarketService service = new ExchangeMarketService(infrastructure, new TaskCoinExchangePlanner(),
            "s1-test");

        ExchangeMarketExecutionResult result = service.executeTaskCoinToStarcoin(new ExchangeMarketExecutionRequest(
            "req-exec-1", "player-a", "s1-test", "Operator", "dreamcraft:item.CoinSurvivorIII", 2L));

        assertEquals(2000L, result.getQuoteResult().getEffectiveExchangeValue());
        assertEquals(TaskCoinExchangePlanner.RULE_VERSION, result.getQuoteResult().getRuleVersion().getRuleKey());

        BankPostingResult postingResult = result.getPostingResult();
        assertEquals("req-exec-1", postingResult.getTransaction().getRequestId());
        assertNotNull(postingResult.getExchangeRecord());
        assertEquals("Survivor", postingResult.getExchangeRecord().getCoinFamily());
        assertEquals("III", postingResult.getExchangeRecord().getCoinTier());
        assertEquals(TaskCoinExchangePlanner.RULE_VERSION, postingResult.getExchangeRecord().getRuleVersion());
        assertEquals("s1-test", postingResult.getExchangeRecord().getSourceServerId());
        assertTrue(postingResult.getExchangeRecord().getExtraJson().contains("\"marketType\":\"exchange\""));
        assertTrue(postingResult.getExchangeRecord().getExtraJson().contains("\"pairCode\":\"task-coin-to-starcoin\""));
        assertTrue(postingResult.getExchangeRecord().getExtraJson().contains("\"limitStatus\":\"ALLOWED\""));
        assertTrue(postingResult.getExchangeRecord().getExtraJson().contains("\"reasonCode\":\"TASK_COIN_RULE_APPLIED\""));
    }

    private BankingInfrastructure minimalInfrastructure() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
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

        private final Map<Long, List<LedgerEntry>> entriesByTransactionId = new HashMap<Long, List<LedgerEntry>>();

        @Override
        public void appendEntries(List<LedgerEntry> entries) {
            for (LedgerEntry entry : entries) {
                List<LedgerEntry> existing = entriesByTransactionId.get(Long.valueOf(entry.getTransactionId()));
                if (existing == null) {
                    existing = new ArrayList<LedgerEntry>();
                    entriesByTransactionId.put(Long.valueOf(entry.getTransactionId()), existing);
                }
                existing.add(entry);
            }
        }

        @Override
        public List<LedgerEntry> findByTransactionId(long transactionId) {
            List<LedgerEntry> entries = entriesByTransactionId.get(Long.valueOf(transactionId));
            return entries == null ? Collections.<LedgerEntry>emptyList() : new ArrayList<LedgerEntry>(entries);
        }

        @Override
        public List<LedgerEntry> findRecentByAccountId(long accountId, int limit) {
            return Collections.emptyList();
        }
    }

    private static final class FakeCoinExchangeRecordRepository implements CoinExchangeRecordRepository {

        private final Map<Long, CoinExchangeRecord> recordsByTransactionId = new HashMap<Long, CoinExchangeRecord>();

        @Override
        public CoinExchangeRecord save(CoinExchangeRecord record) {
            CoinExchangeRecord persisted = new CoinExchangeRecord(recordsByTransactionId.size() + 1L,
                record.getTransactionId(), record.getPlayerRef(), record.getCoinFamily(), record.getCoinTier(),
                record.getCoinFaceValue(), record.getCoinQuantity(), record.getEffectiveExchangeValue(),
                record.getContributionValue(), record.getRuleVersion(), record.getSourceServerId(),
                record.getExtraJson(), record.getCreatedAt());
            recordsByTransactionId.put(Long.valueOf(persisted.getTransactionId()), persisted);
            return persisted;
        }

        @Override
        public Optional<CoinExchangeRecord> findByTransactionId(long transactionId) {
            return Optional.ofNullable(recordsByTransactionId.get(Long.valueOf(transactionId)));
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