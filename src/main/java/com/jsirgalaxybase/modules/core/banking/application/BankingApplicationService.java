package com.jsirgalaxybase.modules.core.banking.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.jsirgalaxybase.modules.core.banking.application.command.CoinExchangeSettlementCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.FrozenBalanceCommand;
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

        Optional<BankAccount> created = accountRepository.saveIfOwnerAbsent(
            createAccount(command, ownerType, ownerRef, currencyCode));
        if (created.isPresent()) {
            return created.get();
        }

        return accountRepository.findByOwner(ownerType, ownerRef, currencyCode)
            .orElseThrow(new java.util.function.Supplier<BankingException>() {

                @Override
                public BankingException get() {
                    return new BankingException("account create lost race but existing account could not be reloaded");
                }
            });
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
            command.getAmount(), command.getBusinessRef(), command.getComment(), command.getExtraJson(), null, false);
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
            command.getExtraJson(), null, false);
    }

    public BankPostingResult freezeFunds(FrozenBalanceCommand command) {
        requireNonNull(command, "command");
        if (command.getAccountId() <= 0L) {
            throw new BankingException("accountId must be positive");
        }
        requireNonNull(command.getTransactionType(), "transactionType");
        requireNonNull(command.getBusinessType(), "businessType");
        return postSingleAccountBalanceChange(requireText(command.getRequestId(), "requestId"),
            command.getTransactionType(), command.getBusinessType(), command.getAccountId(),
            requireText(command.getSourceServerId(), "sourceServerId"),
            requireText(command.getOperatorType(), "operatorType"), blankToNull(command.getOperatorRef()),
            blankToNull(command.getPlayerRef()), command.getAmount(), command.getBusinessRef(), command.getComment(),
            command.getExtraJson(), true);
    }

    public BankPostingResult releaseFunds(FrozenBalanceCommand command) {
        requireNonNull(command, "command");
        if (command.getAccountId() <= 0L) {
            throw new BankingException("accountId must be positive");
        }
        requireNonNull(command.getTransactionType(), "transactionType");
        requireNonNull(command.getBusinessType(), "businessType");
        return postSingleAccountBalanceChange(requireText(command.getRequestId(), "requestId"),
            command.getTransactionType(), command.getBusinessType(), command.getAccountId(),
            requireText(command.getSourceServerId(), "sourceServerId"),
            requireText(command.getOperatorType(), "operatorType"), blankToNull(command.getOperatorRef()),
            blankToNull(command.getPlayerRef()), command.getAmount(), command.getBusinessRef(), command.getComment(),
            command.getExtraJson(), false);
    }

    public BankPostingResult settleFrozenTransfer(InternalTransferCommand command) {
        requireNonNull(command, "command");
        validateTransferIds(command.getFromAccountId(), command.getToAccountId());
        requireNonNull(command.getTransactionType(), "transactionType");
        requireNonNull(command.getBusinessType(), "businessType");
        return transferFunds(requireText(command.getRequestId(), "requestId"), command.getTransactionType(),
            command.getBusinessType(), command.getFromAccountId(), command.getToAccountId(),
            requireText(command.getSourceServerId(), "sourceServerId"),
            requireText(command.getOperatorType(), "operatorType"), blankToNull(command.getOperatorRef()),
            blankToNull(command.getPlayerRef()), command.getAmount(), command.getBusinessRef(), command.getComment(),
            command.getExtraJson(), null, true);
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
            command, false);
    }

    private BankPostingResult transferFunds(final String requestId, final BankTransactionType transactionType,
        final BankBusinessType businessType, final long fromAccountId, final long toAccountId,
        final String sourceServerId, final String operatorType, final String operatorRef, final String playerRef,
        final long amount, final String businessRef, final String comment, final String extraJson,
        final CoinExchangeSettlementCommand exchangeCommand, final boolean debitFrozenBalance) {
        requirePositive(amount, "amount");

        return transactionRunner.inTransaction(new java.util.function.Supplier<BankPostingResult>() {

            @Override
            public BankPostingResult get() {
                RequestSemantics requestedSemantics = new RequestSemantics(
                    transactionType,
                    businessType,
                    fromAccountId,
                    toAccountId,
                    amount,
                    sourceServerId,
                    operatorType,
                    blankToNull(operatorRef),
                    blankToNull(playerRef));
                Optional<BankTransaction> existing = transactionRepository.findByRequestId(requestId);
                if (existing.isPresent()) {
                    ExistingPostingResult replay = loadExistingResult(existing.get());
                    ensureRequestSemanticsMatch(requestId, requestedSemantics, replay.getSemantics());
                    return replay.getPostingResult();
                }

                List<BankAccount> lockedAccounts = accountRepository
                    .lockByIdsInOrder(Arrays.asList(Long.valueOf(fromAccountId), Long.valueOf(toAccountId)));
                Map<Long, BankAccount> accountById = mapAccountsById(lockedAccounts);
                BankAccount fromAccount = requireLockedAccount(accountById, fromAccountId);
                BankAccount toAccount = requireLockedAccount(accountById, toAccountId);

                ensureAccountReady(fromAccount);
                ensureAccountReady(toAccount);
                ensureSameCurrency(fromAccount, toAccount);
                ensurePlayerTransferOwnership(businessType, fromAccount, toAccount, playerRef);
                if (debitFrozenBalance) {
                    ensureSufficientFrozenBalance(fromAccount, amount);
                } else {
                    ensureSufficientBalance(fromAccount, amount);
                }

                Optional<BankTransaction> persisted = transactionRepository.saveIfRequestAbsent(new BankTransaction(0L, requestId,
                    transactionType, businessType, blankToNull(businessRef), sourceServerId, operatorType, operatorRef,
                    playerRef, blankToNull(comment), defaultJson(extraJson), Instant.now()));
                if (!persisted.isPresent()) {
                    BankTransaction existingTransaction = transactionRepository.findByRequestId(requestId)
                        .orElseThrow(new java.util.function.Supplier<BankingException>() {

                            @Override
                            public BankingException get() {
                                return new BankingException("transaction create lost race but existing transaction could not be reloaded");
                            }
                        });
                    ExistingPostingResult replay = loadExistingResult(existingTransaction);
                    ensureRequestSemanticsMatch(requestId, requestedSemantics, replay.getSemantics());
                    return replay.getPostingResult();
                }
                BankTransaction persistedTransaction = persisted.get();

                BankAccount updatedFromAccount = debitFrozenBalance
                    ? withBalances(fromAccount, fromAccount.getAvailableBalance(), fromAccount.getFrozenBalance() - amount)
                    : withBalances(fromAccount, fromAccount.getAvailableBalance() - amount, fromAccount.getFrozenBalance());
                BankAccount updatedToAccount = withBalances(toAccount, toAccount.getAvailableBalance() + amount,
                    toAccount.getFrozenBalance());

                List<LedgerEntry> entries = buildLedgerEntries(persistedTransaction.getTransactionId(), fromAccount,
                    updatedFromAccount, toAccount, updatedToAccount, amount, debitFrozenBalance);
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

    private BankPostingResult postSingleAccountBalanceChange(final String requestId,
        final BankTransactionType transactionType, final BankBusinessType businessType, final long accountId,
        final String sourceServerId, final String operatorType, final String operatorRef, final String playerRef,
        final long amount, final String businessRef, final String comment, final String extraJson,
        final boolean freezeBalance) {
        requirePositive(amount, "amount");

        return transactionRunner.inTransaction(new java.util.function.Supplier<BankPostingResult>() {

            @Override
            public BankPostingResult get() {
                RequestSemantics requestedSemantics = new RequestSemantics(transactionType, businessType, accountId,
                    accountId, amount, sourceServerId, operatorType, blankToNull(operatorRef),
                    blankToNull(playerRef));
                Optional<BankTransaction> existing = transactionRepository.findByRequestId(requestId);
                if (existing.isPresent()) {
                    ExistingPostingResult replay = loadExistingResult(existing.get());
                    ensureRequestSemanticsMatch(requestId, requestedSemantics, replay.getSemantics());
                    return replay.getPostingResult();
                }

                BankAccount account = accountRepository.lockById(accountId);
                ensureAccountReady(account);
                if (freezeBalance) {
                    ensureSufficientBalance(account, amount);
                } else {
                    ensureSufficientFrozenBalance(account, amount);
                }

                Optional<BankTransaction> persisted = transactionRepository.saveIfRequestAbsent(new BankTransaction(0L,
                    requestId, transactionType, businessType, blankToNull(businessRef), sourceServerId, operatorType,
                    operatorRef, playerRef, blankToNull(comment), defaultJson(extraJson), Instant.now()));
                if (!persisted.isPresent()) {
                    BankTransaction existingTransaction = transactionRepository.findByRequestId(requestId)
                        .orElseThrow(new java.util.function.Supplier<BankingException>() {

                            @Override
                            public BankingException get() {
                                return new BankingException("transaction create lost race but existing transaction could not be reloaded");
                            }
                        });
                    ExistingPostingResult replay = loadExistingResult(existingTransaction);
                    ensureRequestSemanticsMatch(requestId, requestedSemantics, replay.getSemantics());
                    return replay.getPostingResult();
                }
                BankTransaction persistedTransaction = persisted.get();

                BankAccount updatedAccount = freezeBalance
                    ? withBalances(account, account.getAvailableBalance() - amount, account.getFrozenBalance() + amount)
                    : withBalances(account, account.getAvailableBalance() + amount, account.getFrozenBalance() - amount);
                List<LedgerEntry> entries = Collections.singletonList(buildSingleLedgerEntry(
                    persistedTransaction.getTransactionId(), account, updatedAccount, amount,
                    freezeBalance ? LedgerEntrySide.DEBIT : LedgerEntrySide.CREDIT));

                ledgerEntryRepository.appendEntries(entries);
                accountRepository.updateBalances(account.getAccountId(), updatedAccount.getAvailableBalance(),
                    updatedAccount.getFrozenBalance(), account.getVersion());
                return new BankPostingResult(persistedTransaction, Collections.singletonList(updatedAccount), entries,
                    null);
            }
        });
    }

    private ExistingPostingResult loadExistingResult(BankTransaction transaction) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(transaction.getTransactionId());
        RequestSemantics semantics = buildRecordedSemantics(transaction, entries);
        List<BankAccount> accounts = loadHistoricalAccountsForEntries(entries);
        CoinExchangeRecord exchangeRecord = coinExchangeRecordRepository.findByTransactionId(transaction.getTransactionId())
            .orElse(null);
        return new ExistingPostingResult(new BankPostingResult(transaction, accounts, entries, exchangeRecord), semantics);
    }

    private List<BankAccount> loadHistoricalAccountsForEntries(List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, BankAccount> historicalAccounts = new LinkedHashMap<Long, BankAccount>();
        for (LedgerEntry entry : entries) {
            BankAccount account = accountRepository.findById(entry.getAccountId())
                .orElseThrow(new java.util.function.Supplier<BankingException>() {

                    @Override
                    public BankingException get() {
                        return new BankingException("account not found for historical ledger result");
                    }
                });
            historicalAccounts.put(Long.valueOf(account.getAccountId()), withHistoricalBalances(account, entry));
        }
        return new ArrayList<BankAccount>(historicalAccounts.values());
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

    private void ensurePlayerTransferOwnership(BankBusinessType businessType, BankAccount fromAccount,
        BankAccount toAccount, String playerRef) {
        if (businessType != BankBusinessType.PLAYER_TRANSFER) {
            return;
        }
        if (!BankingConstants.OWNER_TYPE_PLAYER_UUID.equals(fromAccount.getOwnerType())
            || !playerRef.equals(fromAccount.getOwnerRef())) {
            throw new BankingException("fromAccount is not owned by playerRef");
        }
        if (!BankingConstants.OWNER_TYPE_PLAYER_UUID.equals(toAccount.getOwnerType())) {
            throw new BankingException("toAccount is not a player account");
        }
    }

    private void ensureRequestSemanticsMatch(String requestId, RequestSemantics requested,
        RequestSemantics recorded) {
        List<String> mismatches = new ArrayList<String>();
        if (recorded.getTransactionType() != requested.getTransactionType()) {
            mismatches.add("transactionType");
        }
        if (recorded.getBusinessType() != requested.getBusinessType()) {
            mismatches.add("businessType");
        }
        if (recorded.getFromAccountId() != requested.getFromAccountId()) {
            mismatches.add("fromAccountId");
        }
        if (recorded.getToAccountId() != requested.getToAccountId()) {
            mismatches.add("toAccountId");
        }
        if (recorded.getAmount() != requested.getAmount()) {
            mismatches.add("amount");
        }
        if (!recorded.getSourceServerId().equals(requested.getSourceServerId())) {
            mismatches.add("sourceServerId");
        }
        if (!recorded.getOperatorType().equals(requested.getOperatorType())) {
            mismatches.add("operatorType");
        }
        if (!safeEquals(recorded.getOperatorRef(), requested.getOperatorRef())) {
            mismatches.add("operatorRef");
        }
        if (!safeEquals(recorded.getPlayerRef(), requested.getPlayerRef())) {
            mismatches.add("playerRef");
        }

        if (!mismatches.isEmpty()) {
            throw new BankingException(
                "requestId conflicts with existing transaction: " + requestId + " fields=" + String.join(", ", mismatches));
        }
    }

    private RequestSemantics buildRecordedSemantics(BankTransaction transaction, List<LedgerEntry> entries) {
        if (entries.size() == 1) {
            LedgerEntry entry = entries.get(0);
            return new RequestSemantics(transaction.getTransactionType(), transaction.getBusinessType(),
                entry.getAccountId(), entry.getAccountId(), entry.getAmount(), transaction.getSourceServerId(),
                transaction.getOperatorType(), blankToNull(transaction.getOperatorRef()),
                blankToNull(transaction.getPlayerRef()));
        }
        LedgerPair pair = requireLedgerPair(transaction, entries);
        return new RequestSemantics(
            transaction.getTransactionType(),
            transaction.getBusinessType(),
            pair.getDebitEntry().getAccountId(),
            pair.getCreditEntry().getAccountId(),
            pair.getDebitEntry().getAmount(),
            transaction.getSourceServerId(),
            transaction.getOperatorType(),
            blankToNull(transaction.getOperatorRef()),
            blankToNull(transaction.getPlayerRef()));
    }

    private LedgerPair requireLedgerPair(BankTransaction transaction, List<LedgerEntry> entries) {
        LedgerEntry debitEntry = null;
        LedgerEntry creditEntry = null;
        for (LedgerEntry entry : entries) {
            if (entry.getEntrySide() == LedgerEntrySide.DEBIT && debitEntry == null) {
                debitEntry = entry;
            }
            if (entry.getEntrySide() == LedgerEntrySide.CREDIT && creditEntry == null) {
                creditEntry = entry;
            }
        }

        if (debitEntry == null || creditEntry == null) {
            throw new BankingException(
                "historical ledger entries are incomplete for transactionId=" + transaction.getTransactionId());
        }
        if (debitEntry.getAmount() != creditEntry.getAmount()) {
            throw new BankingException(
                "historical ledger entries disagree on amount for transactionId=" + transaction.getTransactionId());
        }
        return new LedgerPair(debitEntry, creditEntry);
    }

    private BankAccount withHistoricalBalances(BankAccount account, LedgerEntry entry) {
        return new BankAccount(account.getAccountId(), account.getAccountNo(), account.getAccountType(),
            account.getOwnerType(), account.getOwnerRef(), account.getCurrencyCode(), entry.getBalanceAfter(),
            entry.getFrozenBalanceAfter(), account.getStatus(), account.getVersion(), account.getDisplayName(),
            account.getMetadataJson(), account.getCreatedAt(), entry.getCreatedAt());
    }

    private void ensureSufficientBalance(BankAccount account, long amount) {
        if (account.getAvailableBalance() < amount) {
            throw new BankingException("insufficient available balance");
        }
    }

    private void ensureSufficientFrozenBalance(BankAccount account, long amount) {
        if (account.getFrozenBalance() < amount) {
            throw new BankingException("insufficient frozen balance");
        }
    }

    private List<LedgerEntry> buildLedgerEntries(long transactionId, BankAccount fromAccount,
        BankAccount updatedFromAccount, BankAccount toAccount, BankAccount updatedToAccount, long amount,
        boolean debitFrozenBalance) {
        Instant now = Instant.now();
        List<LedgerEntry> entries = new ArrayList<LedgerEntry>(2);
        entries.add(new LedgerEntry(0L, transactionId, fromAccount.getAccountId(), LedgerEntrySide.DEBIT, amount,
            fromAccount.getAvailableBalance(), updatedFromAccount.getAvailableBalance(), fromAccount.getFrozenBalance(),
            updatedFromAccount.getFrozenBalance(), fromAccount.getCurrencyCode(), DEBIT_SEQUENCE, now));
        entries.add(new LedgerEntry(0L, transactionId, toAccount.getAccountId(), LedgerEntrySide.CREDIT, amount,
            toAccount.getAvailableBalance(), updatedToAccount.getAvailableBalance(), toAccount.getFrozenBalance(),
            updatedToAccount.getFrozenBalance(), toAccount.getCurrencyCode(), CREDIT_SEQUENCE, now));
        return entries;
    }

    private LedgerEntry buildSingleLedgerEntry(long transactionId, BankAccount account, BankAccount updatedAccount,
        long amount, LedgerEntrySide entrySide) {
        return new LedgerEntry(0L, transactionId, account.getAccountId(), entrySide, amount,
            account.getAvailableBalance(), updatedAccount.getAvailableBalance(), account.getFrozenBalance(),
            updatedAccount.getFrozenBalance(), account.getCurrencyCode(),
            entrySide == LedgerEntrySide.DEBIT ? DEBIT_SEQUENCE : CREDIT_SEQUENCE, Instant.now());
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

    private BankAccount createAccount(OpenAccountCommand command, String ownerType, String ownerRef,
        String currencyCode) {
        Instant now = Instant.now();
        return new BankAccount(0L, normalizeAccountNo(command.getAccountNo()), command.getAccountType(), ownerType,
            ownerRef, currencyCode, 0L, 0L, BankAccountStatus.ACTIVE, 0L,
            requireText(command.getDisplayName(), "displayName"), defaultJson(command.getMetadataJson()), now, now);
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

    private boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static final class ExistingPostingResult {

        private final BankPostingResult postingResult;
        private final RequestSemantics semantics;

        private ExistingPostingResult(BankPostingResult postingResult, RequestSemantics semantics) {
            this.postingResult = postingResult;
            this.semantics = semantics;
        }

        private BankPostingResult getPostingResult() {
            return postingResult;
        }

        private RequestSemantics getSemantics() {
            return semantics;
        }
    }

    private static final class RequestSemantics {

        private final BankTransactionType transactionType;
        private final BankBusinessType businessType;
        private final long fromAccountId;
        private final long toAccountId;
        private final long amount;
        private final String sourceServerId;
        private final String operatorType;
        private final String operatorRef;
        private final String playerRef;

        private RequestSemantics(BankTransactionType transactionType, BankBusinessType businessType, long fromAccountId,
            long toAccountId, long amount, String sourceServerId, String operatorType, String operatorRef,
            String playerRef) {
            this.transactionType = transactionType;
            this.businessType = businessType;
            this.fromAccountId = fromAccountId;
            this.toAccountId = toAccountId;
            this.amount = amount;
            this.sourceServerId = sourceServerId;
            this.operatorType = operatorType;
            this.operatorRef = operatorRef;
            this.playerRef = playerRef;
        }

        private BankTransactionType getTransactionType() {
            return transactionType;
        }

        private BankBusinessType getBusinessType() {
            return businessType;
        }

        private long getFromAccountId() {
            return fromAccountId;
        }

        private long getToAccountId() {
            return toAccountId;
        }

        private long getAmount() {
            return amount;
        }

        private String getSourceServerId() {
            return sourceServerId;
        }

        private String getOperatorType() {
            return operatorType;
        }

        private String getOperatorRef() {
            return operatorRef;
        }

        private String getPlayerRef() {
            return playerRef;
        }
    }

    private static final class LedgerPair {

        private final LedgerEntry debitEntry;
        private final LedgerEntry creditEntry;

        private LedgerPair(LedgerEntry debitEntry, LedgerEntry creditEntry) {
            this.debitEntry = debitEntry;
            this.creditEntry = creditEntry;
        }

        private LedgerEntry getDebitEntry() {
            return debitEntry;
        }

        private LedgerEntry getCreditEntry() {
            return creditEntry;
        }
    }
}