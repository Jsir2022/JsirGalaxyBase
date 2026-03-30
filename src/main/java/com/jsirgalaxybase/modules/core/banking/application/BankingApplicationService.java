package com.jsirgalaxybase.modules.core.banking.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.jsirgalaxybase.modules.core.banking.application.command.CoinExchangeSettlementCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.InternalTransferCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.OpenAccountCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.PlayerTransferCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountStatus;
import com.jsirgalaxybase.modules.core.banking.domain.BankBusinessType;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransaction;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransactionType;
import com.jsirgalaxybase.modules.core.banking.domain.CoinExchangeRecord;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntry;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntrySide;
import com.jsirgalaxybase.modules.core.banking.repository.BankAccountRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankTransactionRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankingTransactionRunner;
import com.jsirgalaxybase.modules.core.banking.repository.CoinExchangeRecordRepository;
import com.jsirgalaxybase.modules.core.banking.repository.LedgerEntryRepository;

public class BankingApplicationService {

    private static final short DEBIT_SEQUENCE = 1;
    private static final short CREDIT_SEQUENCE = 2;

    private final BankAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final CoinExchangeRecordRepository coinExchangeRecordRepository;
    private final BankingTransactionRunner transactionRunner;

    public BankingApplicationService(BankAccountRepository accountRepository,
        BankTransactionRepository transactionRepository, LedgerEntryRepository ledgerEntryRepository,
        CoinExchangeRecordRepository coinExchangeRecordRepository, BankingTransactionRunner transactionRunner) {
        this.accountRepository = requireNonNull(accountRepository, "accountRepository");
        this.transactionRepository = requireNonNull(transactionRepository, "transactionRepository");
        this.ledgerEntryRepository = requireNonNull(ledgerEntryRepository, "ledgerEntryRepository");
        this.coinExchangeRecordRepository = requireNonNull(coinExchangeRecordRepository, "coinExchangeRecordRepository");
        this.transactionRunner = requireNonNull(transactionRunner, "transactionRunner");
    }

    public BankAccount openAccount(OpenAccountCommand command) {
        requireNonNull(command, "command");
        String ownerType = requireText(command.getOwnerType(), "ownerType");
        String ownerRef = requireText(command.getOwnerRef(), "ownerRef");
        String currencyCode = normalizeCurrencyCode(command.getCurrencyCode());
        requireNonNull(command.getAccountType(), "accountType");

        Optional<BankAccount> existing = accountRepository.findByOwner(ownerType, ownerRef, currencyCode);
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant now = Instant.now();
        BankAccount account = new BankAccount(0L, normalizeAccountNo(command.getAccountNo()), command.getAccountType(),
            ownerType, ownerRef, currencyCode, 0L, 0L, BankAccountStatus.ACTIVE, 0L,
            requireText(command.getDisplayName(), "displayName"), defaultJson(command.getMetadataJson()), now, now);
        return accountRepository.save(account);
    }

    public Optional<BankAccount> findAccount(String ownerType, String ownerRef, String currencyCode) {
        return accountRepository.findByOwner(requireText(ownerType, "ownerType"), requireText(ownerRef, "ownerRef"),
            normalizeCurrencyCode(currencyCode));
    }

    public List<LedgerEntry> getRecentEntries(long accountId, int limit) {
        if (accountId <= 0L) {
            throw new BankingException("accountId must be positive");
        }
        if (limit <= 0) {
            throw new BankingException("limit must be positive");
        }
        return ledgerEntryRepository.findRecentByAccountId(accountId, limit);
    }

    public BankPostingResult transferBetweenPlayers(PlayerTransferCommand command) {
        requireNonNull(command, "command");
        validateTransferIds(command.getFromAccountId(), command.getToAccountId());
        return transferFunds(requireText(command.getRequestId(), "requestId"), BankTransactionType.TRANSFER,
            BankBusinessType.PLAYER_TRANSFER, command.getFromAccountId(), command.getToAccountId(),
            requireText(command.getSourceServerId(), "sourceServerId"), BankingConstants.OPERATOR_TYPE_PLAYER,
            requireText(command.getOperatorRef(), "operatorRef"), requireText(command.getPlayerRef(), "playerRef"),
            command.getAmount(), command.getBusinessRef(), command.getComment(), command.getExtraJson(), null);
    }

    public BankPostingResult postInternalTransfer(InternalTransferCommand command) {
        requireNonNull(command, "command");
        validateTransferIds(command.getFromAccountId(), command.getToAccountId());
        requireNonNull(command.getTransactionType(), "transactionType");
        requireNonNull(command.getBusinessType(), "businessType");
        return transferFunds(requireText(command.getRequestId(), "requestId"), command.getTransactionType(),
            command.getBusinessType(), command.getFromAccountId(), command.getToAccountId(),
            requireText(command.getSourceServerId(), "sourceServerId"),
            requireText(command.getOperatorType(), "operatorType"), blankToNull(command.getOperatorRef()),
            blankToNull(command.getPlayerRef()), command.getAmount(), command.getBusinessRef(), command.getComment(),
            command.getExtraJson(), null);
    }

    public BankPostingResult settleCoinExchange(CoinExchangeSettlementCommand command) {
        requireNonNull(command, "command");
        validateTransferIds(command.getReserveAccountId(), command.getPlayerAccountId());
        requireText(command.getCoinFamily(), "coinFamily");
        requireText(command.getCoinTier(), "coinTier");
        requireText(command.getExchangeRuleVersion(), "exchangeRuleVersion");
        requirePositive(command.getCoinFaceValue(), "coinFaceValue");
        requirePositive(command.getCoinQuantity(), "coinQuantity");
        if (command.getContributionValue() < 0L) {
            throw new BankingException("contributionValue must not be negative");
        }

        return transferFunds(requireText(command.getRequestId(), "requestId"), BankTransactionType.EXCHANGE,
            BankBusinessType.TASK_COIN_EXCHANGE, command.getReserveAccountId(), command.getPlayerAccountId(),
            requireText(command.getSourceServerId(), "sourceServerId"), BankingConstants.OPERATOR_TYPE_SYSTEM,
            blankToNull(command.getOperatorRef()), requireText(command.getPlayerRef(), "playerRef"),
            command.getEffectiveExchangeValue(), command.getBusinessRef(), command.getComment(), command.getExtraJson(),
            command);
    }

    private BankPostingResult transferFunds(final String requestId, final BankTransactionType transactionType,
        final BankBusinessType businessType, final long fromAccountId, final long toAccountId,
        final String sourceServerId, final String operatorType, final String operatorRef, final String playerRef,
        final long amount, final String businessRef, final String comment, final String extraJson,
        final CoinExchangeSettlementCommand exchangeCommand) {
        requirePositive(amount, "amount");

        return transactionRunner.inTransaction(new java.util.function.Supplier<BankPostingResult>() {

            @Override
            public BankPostingResult get() {
                Optional<BankTransaction> existing = transactionRepository.findByRequestId(requestId);
                if (existing.isPresent()) {
                    return loadExistingResult(existing.get());
                }

                List<BankAccount> lockedAccounts = accountRepository
                    .lockByIdsInOrder(Arrays.asList(Long.valueOf(fromAccountId), Long.valueOf(toAccountId)));
                Map<Long, BankAccount> accountById = mapAccountsById(lockedAccounts);
                BankAccount fromAccount = requireLockedAccount(accountById, fromAccountId);
                BankAccount toAccount = requireLockedAccount(accountById, toAccountId);

                ensureAccountReady(fromAccount);
                ensureAccountReady(toAccount);
                ensureSameCurrency(fromAccount, toAccount);
                ensureSufficientBalance(fromAccount, amount);

                BankTransaction persistedTransaction = transactionRepository.save(new BankTransaction(0L, requestId,
                    transactionType, businessType, blankToNull(businessRef), sourceServerId, operatorType, operatorRef,
                    playerRef, blankToNull(comment), defaultJson(extraJson), Instant.now()));

                BankAccount updatedFromAccount = withBalances(fromAccount, fromAccount.getAvailableBalance() - amount,
                    fromAccount.getFrozenBalance());
                BankAccount updatedToAccount = withBalances(toAccount, toAccount.getAvailableBalance() + amount,
                    toAccount.getFrozenBalance());

                List<LedgerEntry> entries = buildLedgerEntries(persistedTransaction.getTransactionId(), fromAccount,
                    updatedFromAccount, toAccount, updatedToAccount, amount);
                ledgerEntryRepository.appendEntries(entries);
                accountRepository.updateBalances(fromAccount.getAccountId(), updatedFromAccount.getAvailableBalance(),
                    updatedFromAccount.getFrozenBalance(), fromAccount.getVersion());
                accountRepository.updateBalances(toAccount.getAccountId(), updatedToAccount.getAvailableBalance(),
                    updatedToAccount.getFrozenBalance(), toAccount.getVersion());

                CoinExchangeRecord exchangeRecord = null;
                if (exchangeCommand != null) {
                    exchangeRecord = coinExchangeRecordRepository.save(new CoinExchangeRecord(0L,
                        persistedTransaction.getTransactionId(), exchangeCommand.getPlayerRef(),
                        exchangeCommand.getCoinFamily(), exchangeCommand.getCoinTier(),
                        exchangeCommand.getCoinFaceValue(), exchangeCommand.getCoinQuantity(),
                        exchangeCommand.getEffectiveExchangeValue(), exchangeCommand.getContributionValue(),
                        exchangeCommand.getExchangeRuleVersion(), exchangeCommand.getSourceServerId(),
                        defaultJson(exchangeCommand.getExtraJson()), Instant.now()));
                }

                return new BankPostingResult(persistedTransaction,
                    Arrays.asList(updatedFromAccount, updatedToAccount), entries, exchangeRecord);
            }
        });
    }

    private BankPostingResult loadExistingResult(BankTransaction transaction) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(transaction.getTransactionId());
        List<BankAccount> accounts = loadAccountsForEntries(entries);
        CoinExchangeRecord exchangeRecord = coinExchangeRecordRepository.findByTransactionId(transaction.getTransactionId())
            .orElse(null);
        return new BankPostingResult(transaction, accounts, entries, exchangeRecord);
    }

    private List<BankAccount> loadAccountsForEntries(List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, BankAccount> accounts = new LinkedHashMap<Long, BankAccount>();
        for (LedgerEntry entry : entries) {
            if (!accounts.containsKey(Long.valueOf(entry.getAccountId()))) {
                BankAccount account = accountRepository.findById(entry.getAccountId())
                    .orElseThrow(new java.util.function.Supplier<BankingException>() {

                        @Override
                        public BankingException get() {
                            return new BankingException("account not found for historical ledger result");
                        }
                    });
                accounts.put(Long.valueOf(account.getAccountId()), account);
            }
        }
        return new ArrayList<BankAccount>(accounts.values());
    }

    private Map<Long, BankAccount> mapAccountsById(List<BankAccount> accounts) {
        Map<Long, BankAccount> mapped = new LinkedHashMap<Long, BankAccount>();
        for (BankAccount account : accounts) {
            mapped.put(Long.valueOf(account.getAccountId()), account);
        }
        return mapped;
    }

    private BankAccount requireLockedAccount(Map<Long, BankAccount> accountById, long accountId) {
        BankAccount account = accountById.get(Long.valueOf(accountId));
        if (account == null) {
            throw new BankingException("account not found: " + accountId);
        }
        return account;
    }

    private void ensureAccountReady(BankAccount account) {
        if (account.getStatus() != BankAccountStatus.ACTIVE) {
            throw new BankingException("account is not active: " + account.getAccountId());
        }
    }

    private void ensureSameCurrency(BankAccount left, BankAccount right) {
        if (!left.getCurrencyCode().equals(right.getCurrencyCode())) {
            throw new BankingException("currency mismatch between accounts");
        }
    }

    private void ensureSufficientBalance(BankAccount account, long amount) {
        if (account.getAvailableBalance() < amount) {
            throw new BankingException("insufficient available balance");
        }
    }

    private List<LedgerEntry> buildLedgerEntries(long transactionId, BankAccount fromAccount,
        BankAccount updatedFromAccount, BankAccount toAccount, BankAccount updatedToAccount, long amount) {
        Instant now = Instant.now();
        List<LedgerEntry> entries = new ArrayList<LedgerEntry>(2);
        entries.add(new LedgerEntry(0L, transactionId, fromAccount.getAccountId(), LedgerEntrySide.DEBIT, amount,
            fromAccount.getAvailableBalance(), updatedFromAccount.getAvailableBalance(), fromAccount.getCurrencyCode(),
            DEBIT_SEQUENCE, now));
        entries.add(new LedgerEntry(0L, transactionId, toAccount.getAccountId(), LedgerEntrySide.CREDIT, amount,
            toAccount.getAvailableBalance(), updatedToAccount.getAvailableBalance(), toAccount.getCurrencyCode(),
            CREDIT_SEQUENCE, now));
        return entries;
    }

    private BankAccount withBalances(BankAccount account, long availableBalance, long frozenBalance) {
        return new BankAccount(account.getAccountId(), account.getAccountNo(), account.getAccountType(),
            account.getOwnerType(), account.getOwnerRef(), account.getCurrencyCode(), availableBalance, frozenBalance,
            account.getStatus(), account.getVersion() + 1, account.getDisplayName(), account.getMetadataJson(),
            account.getCreatedAt(), Instant.now());
    }

    private void validateTransferIds(long fromAccountId, long toAccountId) {
        if (fromAccountId <= 0L || toAccountId <= 0L) {
            throw new BankingException("account ids must be positive");
        }
        if (fromAccountId == toAccountId) {
            throw new BankingException("fromAccountId and toAccountId must differ");
        }
    }

    private String normalizeCurrencyCode(String currencyCode) {
        String normalized = requireText(currencyCode, "currencyCode").toUpperCase();
        if (normalized.length() > 16) {
            throw new BankingException("currencyCode is too long");
        }
        return normalized;
    }

    private String normalizeAccountNo(String accountNo) {
        String value = blankToNull(accountNo);
        if (value == null) {
            value = "ACCT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
        if (value.length() > 32) {
            throw new BankingException("accountNo is too long");
        }
        return value;
    }

    private long requirePositive(long value, String fieldName) {
        if (value <= 0L) {
            throw new BankingException(fieldName + " must be positive");
        }
        return value;
    }

    private <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new BankingException(fieldName + " must not be null");
        }
        return value;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            throw new BankingException(fieldName + " must not be blank");
        }
        return trimmed;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultJson(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? "{}" : trimmed;
    }
}