package com.jsirgalaxybase.modules.core.banking.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import com.jsirgalaxybase.modules.core.banking.application.command.InternalTransferCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.FrozenBalanceCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.OpenAccountCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.PlayerTransferCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountStatus;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;
import com.jsirgalaxybase.modules.core.banking.domain.BankBusinessType;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransaction;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransactionType;
import com.jsirgalaxybase.modules.core.banking.domain.CoinExchangeRecord;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntry;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntrySide;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts;
import com.jsirgalaxybase.modules.core.banking.repository.BankAccountRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankTransactionRepository;
import com.jsirgalaxybase.modules.core.banking.repository.BankingTransactionRunner;
import com.jsirgalaxybase.modules.core.banking.repository.CoinExchangeRecordRepository;
import com.jsirgalaxybase.modules.core.banking.repository.LedgerEntryRepository;

public class BankingApplicationServiceTest {

    @Test
    public void openAccountReturnsExistingAccountWhenInsertLosesRace() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount existing = accountRepository.addAccount(playerAccount(11L, "player-a", "Player A", 0L));
        accountRepository.hideOwnerLookup("PLAYER_UUID", "player-a", BankingConstants.DEFAULT_CURRENCY_CODE, 1);

        BankingApplicationService service = createService(accountRepository, new FakeBankTransactionRepository(),
            new FakeLedgerEntryRepository(), new FakeCoinExchangeRecordRepository());

        BankAccount account = service.openAccount(new OpenAccountCommand(
            null,
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            "player-a",
            BankingConstants.DEFAULT_CURRENCY_CODE,
            "Player A",
            "{}"));

        assertSame(existing, account);
    }

    @Test
    public void openAccountKeepsExistingDisplayNameAndMetadataWhenAccountAlreadyExists() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount existing = accountRepository.addAccount(new BankAccount(
            12L,
            "ACCT-12",
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            "player-a",
            BankingConstants.DEFAULT_CURRENCY_CODE,
            0L,
            0L,
            BankAccountStatus.ACTIVE,
            3L,
            "Original Name",
            "{\"source\":\"seed\"}",
            Instant.now(),
            Instant.now()));

        BankingApplicationService service = createService(accountRepository, new FakeBankTransactionRepository(),
            new FakeLedgerEntryRepository(), new FakeCoinExchangeRecordRepository());

        BankAccount account = service.openAccount(new OpenAccountCommand(
            null,
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            "player-a",
            BankingConstants.DEFAULT_CURRENCY_CODE,
            "Updated Name",
            "{\"source\":\"open-command\"}"));

        assertSame(existing, account);
        assertEquals("Original Name", account.getDisplayName());
        assertEquals("{\"source\":\"seed\"}", account.getMetadataJson());
    }

    @Test
    public void ensureManagedAccountReturnsExistingAccountWhenInsertLosesRace() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount existing = accountRepository.addAccount(new BankAccount(
            21L,
            "EXCH-RESERVE",
            BankAccountType.EXCHANGE_RESERVE,
            BankingConstants.OWNER_TYPE_PUBLIC_FUND_CODE,
            "EXCHANGE_RESERVE",
            BankingConstants.DEFAULT_CURRENCY_CODE,
            0L,
            0L,
            BankAccountStatus.ACTIVE,
            0L,
            "Quest Exchange Reserve",
            "{}",
            Instant.now(),
            Instant.now()));
        accountRepository.hideOwnerLookup(
            BankingConstants.OWNER_TYPE_PUBLIC_FUND_CODE,
            "EXCHANGE_RESERVE",
            BankingConstants.DEFAULT_CURRENCY_CODE,
            1);

        BankAccount account = ManagedBankAccounts.ensureManagedAccount(
            new BankingInfrastructure(null, accountRepository, null, null, null, null),
            ManagedBankAccounts.EXCHANGE_RESERVE_ACCOUNT);

        assertSame(existing, account);
    }

    @Test
    public void transferBetweenPlayersRejectsSourceAccountNotOwnedByPlayer() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        accountRepository.addAccount(playerAccount(31L, "other-player", "Other", 500L));
        accountRepository.addAccount(playerAccount(32L, "player-b", "Player B", 200L));

        BankingApplicationService service = createService(accountRepository, new FakeBankTransactionRepository(),
            new FakeLedgerEntryRepository(), new FakeCoinExchangeRecordRepository());

        try {
            service.transferBetweenPlayers(new PlayerTransferCommand(
                "req-ownership",
                31L,
                32L,
                "test-server",
                "Operator",
                "player-a",
                50L,
                "test",
                "p2p:31:32",
                "{}"));
            fail("Expected ownership validation to fail");
        } catch (BankingException exception) {
            assertEquals("fromAccount is not owned by playerRef", exception.getMessage());
        }
    }

    @Test
    public void transferBetweenPlayersReplaysExistingResultWhenRequestInsertLosesRace() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(playerAccount(41L, "player-a", "Player A", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(42L, "player-b", "Player B", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        BankTransaction existingTransaction = transactionRepository.addExisting(new BankTransaction(
            91L,
            "req-replay",
            BankTransactionType.TRANSFER,
            BankBusinessType.PLAYER_TRANSFER,
            "p2p:41:42",
            "test-server",
            BankingConstants.OPERATOR_TYPE_PLAYER,
            "Operator",
            "player-a",
            "existing",
            "{}",
            Instant.now()));
        transactionRepository.hideRequestLookup("req-replay", 1);
        ledgerRepository.seed(existingTransaction.getTransactionId(), Arrays.asList(
            new LedgerEntry(1L, existingTransaction.getTransactionId(), fromAccount.getAccountId(), LedgerEntrySide.DEBIT,
                50L, 500L, 450L, BankingConstants.DEFAULT_CURRENCY_CODE, (short) 1, Instant.now()),
            new LedgerEntry(2L, existingTransaction.getTransactionId(), toAccount.getAccountId(), LedgerEntrySide.CREDIT,
                50L, 300L, 350L, BankingConstants.DEFAULT_CURRENCY_CODE, (short) 2, Instant.now())));

        BankPostingResult result = service.transferBetweenPlayers(new PlayerTransferCommand(
            "req-replay",
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            "test-server",
            "Operator",
            "player-a",
            50L,
            "retry",
            "p2p:41:42",
            "{}"));

        assertEquals(existingTransaction.getTransactionId(), result.getTransaction().getTransactionId());
        assertEquals(0, ledgerRepository.appendCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    public void transferBetweenPlayersReplayUsesHistoricalBalancesFromLedger() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(playerAccount(51L, "player-a", "Player A", 900L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(52L, "player-b", "Player B", 1200L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        BankTransaction existingTransaction = transactionRepository.addExisting(new BankTransaction(
            92L,
            "req-historical-replay",
            BankTransactionType.TRANSFER,
            BankBusinessType.PLAYER_TRANSFER,
            "p2p:51:52",
            "test-server",
            BankingConstants.OPERATOR_TYPE_PLAYER,
            "Operator",
            "player-a",
            "existing",
            "{}",
            Instant.now()));
        ledgerRepository.seed(existingTransaction.getTransactionId(), Arrays.asList(
            new LedgerEntry(11L, existingTransaction.getTransactionId(), fromAccount.getAccountId(), LedgerEntrySide.DEBIT,
                50L, 500L, 450L, BankingConstants.DEFAULT_CURRENCY_CODE, (short) 1, Instant.now()),
            new LedgerEntry(12L, existingTransaction.getTransactionId(), toAccount.getAccountId(), LedgerEntrySide.CREDIT,
                50L, 300L, 350L, BankingConstants.DEFAULT_CURRENCY_CODE, (short) 2, Instant.now())));

        accountRepository.replaceBalances(fromAccount.getAccountId(), 900L, 0L);
        accountRepository.replaceBalances(toAccount.getAccountId(), 1200L, 0L);

        BankPostingResult result = service.transferBetweenPlayers(new PlayerTransferCommand(
            "req-historical-replay",
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            "test-server",
            "Operator",
            "player-a",
            50L,
            "retry",
            "p2p:51:52",
            "{}"));

        assertEquals(450L, affectedBalance(result, fromAccount.getAccountId()));
        assertEquals(350L, affectedBalance(result, toAccount.getAccountId()));
        assertEquals(0, ledgerRepository.appendCalls);
    }

    @Test
    public void transferBetweenPlayersRejectsReplayedRequestWithDifferentAmount() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(playerAccount(61L, "player-a", "Player A", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(62L, "player-b", "Player B", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingPlayerTransfer(transactionRepository, ledgerRepository, fromAccount, toAccount, 93L,
            "req-conflict-amount", 50L, "player-a");

        assertRequestConflict(service, new PlayerTransferCommand(
            "req-conflict-amount",
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            "test-server",
            "Operator",
            "player-a",
            60L,
            "retry",
            "p2p:61:62",
            "{}"), "amount");
    }

    @Test
    public void transferBetweenPlayersRejectsReplayedRequestWithDifferentPlayerRef() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(playerAccount(71L, "player-a", "Player A", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(72L, "player-b", "Player B", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingPlayerTransfer(transactionRepository, ledgerRepository, fromAccount, toAccount, 94L,
            "req-conflict-player", 50L, "player-a");

        assertRequestConflict(service, new PlayerTransferCommand(
            "req-conflict-player",
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            "test-server",
            "Operator",
            "player-c",
            50L,
            "retry",
            "p2p:71:72",
            "{}"), "playerRef");
    }

    @Test
    public void transferBetweenPlayersRejectsReplayedRequestWithDifferentBusinessType() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(playerAccount(81L, "player-a", "Player A", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(82L, "player-b", "Player B", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        BankTransaction existingTransaction = transactionRepository.addExisting(new BankTransaction(
            95L,
            "req-conflict-business",
            BankTransactionType.ADJUSTMENT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            "adjust:81:82",
            "test-server",
            BankingConstants.OPERATOR_TYPE_PLAYER,
            "Operator",
            "player-a",
            "existing",
            "{}",
            Instant.now()));
        ledgerRepository.seed(existingTransaction.getTransactionId(), Arrays.asList(
            new LedgerEntry(21L, existingTransaction.getTransactionId(), fromAccount.getAccountId(), LedgerEntrySide.DEBIT,
                50L, 500L, 450L, BankingConstants.DEFAULT_CURRENCY_CODE, (short) 1, Instant.now()),
            new LedgerEntry(22L, existingTransaction.getTransactionId(), toAccount.getAccountId(), LedgerEntrySide.CREDIT,
                50L, 300L, 350L, BankingConstants.DEFAULT_CURRENCY_CODE, (short) 2, Instant.now())));

        assertRequestConflict(service, new PlayerTransferCommand(
            "req-conflict-business",
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            "test-server",
            "Operator",
            "player-a",
            50L,
            "retry",
            "p2p:81:82",
            "{}"), "businessType");
    }

    @Test
    public void transferBetweenPlayersRejectsReplayedRequestWithDifferentTransactionType() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(playerAccount(83L, "player-a", "Player A", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(84L, "player-b", "Player B", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        BankTransaction existingTransaction = transactionRepository.addExisting(new BankTransaction(
            96L,
            "req-conflict-transaction-type",
            BankTransactionType.ADJUSTMENT,
            BankBusinessType.PLAYER_TRANSFER,
            "p2p:83:84",
            "test-server",
            BankingConstants.OPERATOR_TYPE_PLAYER,
            "Operator",
            "player-a",
            "existing",
            "{}",
            Instant.now()));
        ledgerRepository.seed(existingTransaction.getTransactionId(), Arrays.asList(
            new LedgerEntry(23L, existingTransaction.getTransactionId(), fromAccount.getAccountId(), LedgerEntrySide.DEBIT,
                50L, 500L, 450L, BankingConstants.DEFAULT_CURRENCY_CODE, (short) 1, Instant.now()),
            new LedgerEntry(24L, existingTransaction.getTransactionId(), toAccount.getAccountId(), LedgerEntrySide.CREDIT,
                50L, 300L, 350L, BankingConstants.DEFAULT_CURRENCY_CODE, (short) 2, Instant.now())));

        assertRequestConflict(service, new PlayerTransferCommand(
            "req-conflict-transaction-type",
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            "test-server",
            "Operator",
            "player-a",
            50L,
            "retry",
            "p2p:83:84",
            "{}"), "transactionType");
    }

    @Test
    public void replayReturnsHistoricalBalancesButCurrentNonBalanceFields() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(playerAccount(85L, "player-a", "Player A", 900L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(86L, "player-b", "Player B", 1200L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        Instant replayedAt = Instant.now();
        BankTransaction existingTransaction = transactionRepository.addExisting(new BankTransaction(
            97L,
            "req-replay-shape",
            BankTransactionType.TRANSFER,
            BankBusinessType.PLAYER_TRANSFER,
            "p2p:85:86",
            "test-server",
            BankingConstants.OPERATOR_TYPE_PLAYER,
            "Operator",
            "player-a",
            "existing",
            "{}",
            replayedAt));
        ledgerRepository.seed(existingTransaction.getTransactionId(), Arrays.asList(
            new LedgerEntry(25L, existingTransaction.getTransactionId(), fromAccount.getAccountId(), LedgerEntrySide.DEBIT,
                50L, 500L, 450L, BankingConstants.DEFAULT_CURRENCY_CODE, (short) 1, replayedAt),
            new LedgerEntry(26L, existingTransaction.getTransactionId(), toAccount.getAccountId(), LedgerEntrySide.CREDIT,
                50L, 300L, 350L, BankingConstants.DEFAULT_CURRENCY_CODE, (short) 2, replayedAt)));

        accountRepository.addAccount(new BankAccount(
            fromAccount.getAccountId(),
            fromAccount.getAccountNo(),
            fromAccount.getAccountType(),
            fromAccount.getOwnerType(),
            fromAccount.getOwnerRef(),
            fromAccount.getCurrencyCode(),
            900L,
            25L,
            BankAccountStatus.FROZEN,
            7L,
            "Renamed Player A",
            "{\"source\":\"current-view\"}",
            fromAccount.getCreatedAt(),
            Instant.now()));

        BankPostingResult result = service.transferBetweenPlayers(new PlayerTransferCommand(
            "req-replay-shape",
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            "test-server",
            "Operator",
            "player-a",
            50L,
            "retry",
            "p2p:85:86",
            "{}"));

        BankAccount replayedFrom = affectedAccount(result, fromAccount.getAccountId());
        assertEquals(450L, replayedFrom.getAvailableBalance());
    assertEquals(0L, replayedFrom.getFrozenBalance());
        assertEquals(BankAccountStatus.FROZEN, replayedFrom.getStatus());
        assertEquals(7L, replayedFrom.getVersion());
        assertEquals("Renamed Player A", replayedFrom.getDisplayName());
        assertEquals("{\"source\":\"current-view\"}", replayedFrom.getMetadataJson());
        assertEquals(replayedAt, replayedFrom.getUpdatedAt());
    }

    @Test
    public void postInternalTransferReplayUsesHistoricalBalancesFromLedger() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(systemAccount(91L, "SYSTEM_OPERATIONS", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(92L, "player-a", "Player A", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingInternalTransfer(
            transactionRepository,
            ledgerRepository,
            fromAccount,
            toAccount,
            96L,
            "req-internal-replay",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a");

        accountRepository.replaceBalances(fromAccount.getAccountId(), 430L, 0L);
        accountRepository.replaceBalances(toAccount.getAccountId(), 370L, 0L);

        BankPostingResult result = service.postInternalTransfer(internalTransferCommand(
            "req-internal-replay",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a"));

        assertEquals(450L, affectedBalance(result, fromAccount.getAccountId()));
        assertEquals(350L, affectedBalance(result, toAccount.getAccountId()));
        assertEquals(0, ledgerRepository.appendCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    public void postInternalTransferRejectsReplayedRequestWithDifferentAmount() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(systemAccount(101L, "SYSTEM_OPERATIONS", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(102L, "player-a", "Player A", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingInternalTransfer(
            transactionRepository,
            ledgerRepository,
            fromAccount,
            toAccount,
            97L,
            "req-internal-conflict-amount",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a");

        assertInternalTransferConflict(service, internalTransferCommand(
            "req-internal-conflict-amount",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            60L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a"), "amount");
    }

    @Test
    public void postInternalTransferRejectsReplayedRequestWithDifferentFromAccountId() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(systemAccount(111L, "SYSTEM_OPERATIONS", 450L));
        BankAccount otherFromAccount = accountRepository.addAccount(systemAccount(112L, "SYSTEM_OPERATIONS_ALT", 900L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(113L, "player-a", "Player A", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingInternalTransfer(
            transactionRepository,
            ledgerRepository,
            fromAccount,
            toAccount,
            98L,
            "req-internal-conflict-from",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a");

        assertInternalTransferConflict(service, internalTransferCommand(
            "req-internal-conflict-from",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            otherFromAccount.getAccountId(),
            toAccount.getAccountId(),
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a"), "fromAccountId");
    }

    @Test
    public void postInternalTransferRejectsReplayedRequestWithDifferentToAccountId() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(systemAccount(121L, "SYSTEM_OPERATIONS", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(122L, "player-a", "Player A", 350L));
        BankAccount otherToAccount = accountRepository.addAccount(playerAccount(123L, "player-b", "Player B", 200L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingInternalTransfer(
            transactionRepository,
            ledgerRepository,
            fromAccount,
            toAccount,
            99L,
            "req-internal-conflict-to",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a");

        assertInternalTransferConflict(service, internalTransferCommand(
            "req-internal-conflict-to",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            fromAccount.getAccountId(),
            otherToAccount.getAccountId(),
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a"), "toAccountId");
    }

    @Test
    public void postInternalTransferRejectsReplayedRequestWithDifferentBusinessType() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(systemAccount(131L, "SYSTEM_OPERATIONS", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(132L, "player-a", "Player A", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingInternalTransfer(
            transactionRepository,
            ledgerRepository,
            fromAccount,
            toAccount,
            100L,
            "req-internal-conflict-business",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a");

        assertInternalTransferConflict(service, internalTransferCommand(
            "req-internal-conflict-business",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ACTIVITY_REWARD,
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a"), "businessType");
    }

    @Test
    public void postInternalTransferRejectsReplayedRequestWithDifferentTransactionType() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(systemAccount(133L, "SYSTEM_OPERATIONS", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(134L, "player-a", "Player A", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingInternalTransfer(
            transactionRepository,
            ledgerRepository,
            fromAccount,
            toAccount,
            104L,
            "req-internal-conflict-transaction-type",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a");

        assertInternalTransferConflict(service, internalTransferCommand(
            "req-internal-conflict-transaction-type",
            BankTransactionType.SYSTEM_DEDUCT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a"), "transactionType");
    }

    @Test
    public void postInternalTransferRejectsReplayedRequestWithDifferentOperatorType() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(systemAccount(141L, "SYSTEM_OPERATIONS", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(142L, "player-a", "Player A", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingInternalTransfer(
            transactionRepository,
            ledgerRepository,
            fromAccount,
            toAccount,
            101L,
            "req-internal-conflict-operator-type",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a");

        assertInternalTransferConflict(service, internalTransferCommand(
            "req-internal-conflict-operator-type",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_SYSTEM,
            "Console",
            "player-a"), "operatorType");
    }

    @Test
    public void postInternalTransferRejectsReplayedRequestWithDifferentOperatorRef() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(systemAccount(151L, "SYSTEM_OPERATIONS", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(152L, "player-a", "Player A", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingInternalTransfer(
            transactionRepository,
            ledgerRepository,
            fromAccount,
            toAccount,
            102L,
            "req-internal-conflict-operator-ref",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a");

        assertInternalTransferConflict(service, internalTransferCommand(
            "req-internal-conflict-operator-ref",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "OtherConsole",
            "player-a"), "operatorRef");
    }

    @Test
    public void postInternalTransferRejectsReplayedRequestWithDifferentSourceServerId() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount fromAccount = accountRepository.addAccount(systemAccount(161L, "SYSTEM_OPERATIONS", 450L));
        BankAccount toAccount = accountRepository.addAccount(playerAccount(162L, "player-a", "Player A", 350L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingInternalTransfer(
            transactionRepository,
            ledgerRepository,
            fromAccount,
            toAccount,
            103L,
            "req-internal-conflict-source",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            50L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a");

        assertInternalTransferConflict(service, internalTransferCommand(
            "req-internal-conflict-source",
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            50L,
            "other-server",
            BankingConstants.OPERATOR_TYPE_ADMIN,
            "Console",
            "player-a"), "sourceServerId");
    }

    @Test
    public void ensureManagedTaxAccountCreatesDedicatedTaxPool() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();

        BankAccount account = ManagedBankAccounts.ensureManagedAccount(
            new BankingInfrastructure(null, accountRepository, null, null, null, null),
            ManagedBankAccounts.TAX_ACCOUNT);

        assertEquals("TAX_POOL", account.getOwnerRef());
        assertEquals(BankAccountType.PUBLIC_FUND, account.getAccountType());
        assertEquals(0L, account.getAvailableBalance());
    }

    @Test
    public void freezeFundsMovesAvailableBalanceIntoFrozenBalance() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount playerAccount = accountRepository.addAccount(playerAccount(171L, "player-a", "Player A", 500L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        BankPostingResult result = service.freezeFunds(frozenBalanceCommand(
            "req-freeze",
            BankTransactionType.MARKET_FUNDS_FREEZE,
            BankBusinessType.MARKET_ORDER_FREEZE,
            playerAccount.getAccountId(),
            120L));

        BankAccount updated = affectedAccount(result, playerAccount.getAccountId());
        assertEquals(380L, updated.getAvailableBalance());
        assertEquals(120L, updated.getFrozenBalance());
        assertEquals(1, ledgerRepository.findByTransactionId(result.getTransaction().getTransactionId()).size());
    }

    @Test
    public void releaseFundsMovesFrozenBalanceBackIntoAvailableBalance() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount playerAccount = accountRepository.addAccount(new BankAccount(
            172L,
            "ACCT-172",
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            "player-a",
            BankingConstants.DEFAULT_CURRENCY_CODE,
            320L,
            180L,
            BankAccountStatus.ACTIVE,
            0L,
            "Player A",
            "{}",
            Instant.now(),
            Instant.now()));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        BankPostingResult result = service.releaseFunds(frozenBalanceCommand(
            "req-release",
            BankTransactionType.MARKET_FUNDS_RELEASE,
            BankBusinessType.MARKET_ORDER_CANCEL_RELEASE,
            playerAccount.getAccountId(),
            80L));

        BankAccount updated = affectedAccount(result, playerAccount.getAccountId());
        assertEquals(400L, updated.getAvailableBalance());
        assertEquals(100L, updated.getFrozenBalance());
        assertEquals(1, ledgerRepository.findByTransactionId(result.getTransaction().getTransactionId()).size());
    }

    @Test
    public void settleFrozenTransferMovesFrozenBalanceToTargetAccount() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount buyerAccount = accountRepository.addAccount(new BankAccount(
            173L,
            "ACCT-173",
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            "buyer-player",
            BankingConstants.DEFAULT_CURRENCY_CODE,
            400L,
            220L,
            BankAccountStatus.ACTIVE,
            0L,
            "Buyer",
            "{}",
            Instant.now(),
            Instant.now()));
        BankAccount sellerAccount = accountRepository.addAccount(playerAccount(174L, "seller-player", "Seller", 150L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        BankPostingResult result = service.settleFrozenTransfer(internalTransferCommand(
            "req-settle-frozen",
            BankTransactionType.MARKET_SETTLEMENT_FROM_FROZEN,
            BankBusinessType.MARKET_ORDER_SETTLEMENT,
            buyerAccount.getAccountId(),
            sellerAccount.getAccountId(),
            90L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_SYSTEM,
            "market",
            "buyer-player"));

        BankAccount updatedBuyer = affectedAccount(result, buyerAccount.getAccountId());
        BankAccount updatedSeller = affectedAccount(result, sellerAccount.getAccountId());
        assertEquals(400L, updatedBuyer.getAvailableBalance());
        assertEquals(130L, updatedBuyer.getFrozenBalance());
        assertEquals(240L, updatedSeller.getAvailableBalance());
        assertEquals(2, ledgerRepository.findByTransactionId(result.getTransaction().getTransactionId()).size());
    }

    @Test
    public void freezeFundsReplayUsesHistoricalAvailableAndFrozenBalances() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount playerAccount = accountRepository.addAccount(playerAccount(176L, "player-a", "Player A", 380L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingFrozenBalanceChange(
            transactionRepository,
            ledgerRepository,
            playerAccount,
            105L,
            "req-freeze-replay",
            BankTransactionType.MARKET_FUNDS_FREEZE,
            BankBusinessType.MARKET_ORDER_FREEZE,
            120L,
            500L,
            380L,
            0L,
            120L);

        accountRepository.replaceBalances(playerAccount.getAccountId(), 700L, 30L);

        BankPostingResult result = service.freezeFunds(frozenBalanceCommand(
            "req-freeze-replay",
            BankTransactionType.MARKET_FUNDS_FREEZE,
            BankBusinessType.MARKET_ORDER_FREEZE,
            playerAccount.getAccountId(),
            120L));

        BankAccount replayed = affectedAccount(result, playerAccount.getAccountId());
        assertEquals(380L, replayed.getAvailableBalance());
        assertEquals(120L, replayed.getFrozenBalance());
        assertEquals(0, ledgerRepository.appendCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    public void releaseFundsReplayUsesHistoricalAvailableAndFrozenBalances() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount playerAccount = accountRepository.addAccount(new BankAccount(
            177L,
            "ACCT-177",
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            "player-a",
            BankingConstants.DEFAULT_CURRENCY_CODE,
            400L,
            100L,
            BankAccountStatus.ACTIVE,
            0L,
            "Player A",
            "{}",
            Instant.now(),
            Instant.now()));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingFrozenBalanceChange(
            transactionRepository,
            ledgerRepository,
            playerAccount,
            106L,
            "req-release-replay",
            BankTransactionType.MARKET_FUNDS_RELEASE,
            BankBusinessType.MARKET_ORDER_CANCEL_RELEASE,
            80L,
            320L,
            400L,
            180L,
            100L);

        accountRepository.replaceBalances(playerAccount.getAccountId(), 250L, 20L);

        BankPostingResult result = service.releaseFunds(frozenBalanceCommand(
            "req-release-replay",
            BankTransactionType.MARKET_FUNDS_RELEASE,
            BankBusinessType.MARKET_ORDER_CANCEL_RELEASE,
            playerAccount.getAccountId(),
            80L));

        BankAccount replayed = affectedAccount(result, playerAccount.getAccountId());
        assertEquals(400L, replayed.getAvailableBalance());
        assertEquals(100L, replayed.getFrozenBalance());
        assertEquals(0, ledgerRepository.appendCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    public void settleFrozenTransferReplayUsesHistoricalAvailableAndFrozenBalances() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount buyerAccount = accountRepository.addAccount(new BankAccount(
            178L,
            "ACCT-178",
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            "buyer-player",
            BankingConstants.DEFAULT_CURRENCY_CODE,
            400L,
            130L,
            BankAccountStatus.ACTIVE,
            0L,
            "Buyer",
            "{}",
            Instant.now(),
            Instant.now()));
        BankAccount sellerAccount = accountRepository.addAccount(playerAccount(179L, "seller-player", "Seller", 240L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingInternalTransferWithFrozenBalances(
            transactionRepository,
            ledgerRepository,
            buyerAccount,
            sellerAccount,
            107L,
            "req-settle-frozen-replay",
            BankTransactionType.MARKET_SETTLEMENT_FROM_FROZEN,
            BankBusinessType.MARKET_ORDER_SETTLEMENT,
            90L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_SYSTEM,
            "market",
            "buyer-player",
            400L,
            400L,
            220L,
            130L,
            150L,
            240L,
            0L,
            0L);

        accountRepository.replaceBalances(buyerAccount.getAccountId(), 650L, 15L);
        accountRepository.replaceBalances(sellerAccount.getAccountId(), 500L, 0L);

        BankPostingResult result = service.settleFrozenTransfer(internalTransferCommand(
            "req-settle-frozen-replay",
            BankTransactionType.MARKET_SETTLEMENT_FROM_FROZEN,
            BankBusinessType.MARKET_ORDER_SETTLEMENT,
            buyerAccount.getAccountId(),
            sellerAccount.getAccountId(),
            90L,
            "test-server",
            BankingConstants.OPERATOR_TYPE_SYSTEM,
            "market",
            "buyer-player"));

        BankAccount replayedBuyer = affectedAccount(result, buyerAccount.getAccountId());
        BankAccount replayedSeller = affectedAccount(result, sellerAccount.getAccountId());
        assertEquals(400L, replayedBuyer.getAvailableBalance());
        assertEquals(130L, replayedBuyer.getFrozenBalance());
        assertEquals(240L, replayedSeller.getAvailableBalance());
        assertEquals(0L, replayedSeller.getFrozenBalance());
        assertEquals(0, ledgerRepository.appendCalls);
        assertEquals(0, accountRepository.updateCalls);
    }

    @Test
    public void freezeFundsRejectsReplayedRequestWithDifferentAmount() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount playerAccount = accountRepository.addAccount(playerAccount(180L, "player-a", "Player A", 380L));

        FakeBankTransactionRepository transactionRepository = new FakeBankTransactionRepository();
        FakeLedgerEntryRepository ledgerRepository = new FakeLedgerEntryRepository();
        BankingApplicationService service = createService(accountRepository, transactionRepository, ledgerRepository,
            new FakeCoinExchangeRecordRepository());

        seedExistingFrozenBalanceChange(
            transactionRepository,
            ledgerRepository,
            playerAccount,
            108L,
            "req-freeze-conflict",
            BankTransactionType.MARKET_FUNDS_FREEZE,
            BankBusinessType.MARKET_ORDER_FREEZE,
            120L,
            500L,
            380L,
            0L,
            120L);

        try {
            service.freezeFunds(frozenBalanceCommand(
                "req-freeze-conflict",
                BankTransactionType.MARKET_FUNDS_FREEZE,
                BankBusinessType.MARKET_ORDER_FREEZE,
                playerAccount.getAccountId(),
                130L));
            fail("Expected request conflict for field amount");
        } catch (BankingException exception) {
            assertTrue(exception.getMessage().contains("requestId conflicts with existing transaction"));
            assertTrue(exception.getMessage().contains("amount"));
        }
    }

    @Test
    public void freezeFundsRejectsWhenAvailableBalanceIsInsufficient() {
        FakeBankAccountRepository accountRepository = new FakeBankAccountRepository();
        BankAccount playerAccount = accountRepository.addAccount(playerAccount(175L, "player-a", "Player A", 30L));

        BankingApplicationService service = createService(accountRepository, new FakeBankTransactionRepository(),
            new FakeLedgerEntryRepository(), new FakeCoinExchangeRecordRepository());

        try {
            service.freezeFunds(frozenBalanceCommand(
                "req-freeze-fail",
                BankTransactionType.MARKET_FUNDS_FREEZE,
                BankBusinessType.MARKET_ORDER_FREEZE,
                playerAccount.getAccountId(),
                50L));
            fail("Expected freeze to reject insufficient available balance");
        } catch (BankingException exception) {
            assertEquals("insufficient available balance", exception.getMessage());
        }
    }

    private BankingApplicationService createService(FakeBankAccountRepository accountRepository,
        FakeBankTransactionRepository transactionRepository, FakeLedgerEntryRepository ledgerRepository,
        FakeCoinExchangeRecordRepository exchangeRecordRepository) {
        return new BankingApplicationService(
            accountRepository,
            transactionRepository,
            ledgerRepository,
            exchangeRecordRepository,
            new DirectTransactionRunner());
    }

    private static BankAccount playerAccount(long accountId, String ownerRef, String displayName, long balance) {
        return new BankAccount(
            accountId,
            "ACCT-" + accountId,
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            ownerRef,
            BankingConstants.DEFAULT_CURRENCY_CODE,
            balance,
            0L,
            BankAccountStatus.ACTIVE,
            0L,
            displayName,
            "{}",
            Instant.now(),
            Instant.now());
    }

    private static BankAccount systemAccount(long accountId, String ownerRef, long balance) {
        return new BankAccount(
            accountId,
            "SYS-" + accountId,
            BankAccountType.SYSTEM,
            BankingConstants.OWNER_TYPE_SYSTEM,
            ownerRef,
            BankingConstants.DEFAULT_CURRENCY_CODE,
            balance,
            0L,
            BankAccountStatus.ACTIVE,
            0L,
            ownerRef,
            "{}",
            Instant.now(),
            Instant.now());
    }

    private static InternalTransferCommand internalTransferCommand(String requestId,
        BankTransactionType transactionType, BankBusinessType businessType, long fromAccountId, long toAccountId,
        long amount, String sourceServerId, String operatorType, String operatorRef, String playerRef) {
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
            "internal transfer",
            "internal:" + fromAccountId + ":" + toAccountId,
            "{}");
    }

    private static FrozenBalanceCommand frozenBalanceCommand(String requestId,
        BankTransactionType transactionType, BankBusinessType businessType, long accountId, long amount) {
        return new FrozenBalanceCommand(
            requestId,
            transactionType,
            businessType,
            accountId,
            "test-server",
            BankingConstants.OPERATOR_TYPE_SYSTEM,
            "market",
            "player-a",
            amount,
            "market frozen balance test",
            "market:" + accountId,
            "{}");
    }

    private static void seedExistingPlayerTransfer(FakeBankTransactionRepository transactionRepository,
        FakeLedgerEntryRepository ledgerRepository, BankAccount fromAccount, BankAccount toAccount, long transactionId,
        String requestId, long amount, String playerRef) {
        BankTransaction existingTransaction = transactionRepository.addExisting(new BankTransaction(
            transactionId,
            requestId,
            BankTransactionType.TRANSFER,
            BankBusinessType.PLAYER_TRANSFER,
            "p2p:" + fromAccount.getAccountId() + ":" + toAccount.getAccountId(),
            "test-server",
            BankingConstants.OPERATOR_TYPE_PLAYER,
            "Operator",
            playerRef,
            "existing",
            "{}",
            Instant.now()));
        ledgerRepository.seed(existingTransaction.getTransactionId(), Arrays.asList(
            new LedgerEntry(transactionId * 10L + 1L, existingTransaction.getTransactionId(), fromAccount.getAccountId(),
                LedgerEntrySide.DEBIT, amount, fromAccount.getAvailableBalance() + amount, fromAccount.getAvailableBalance(),
                BankingConstants.DEFAULT_CURRENCY_CODE, (short) 1, Instant.now()),
            new LedgerEntry(transactionId * 10L + 2L, existingTransaction.getTransactionId(), toAccount.getAccountId(),
                LedgerEntrySide.CREDIT, amount, toAccount.getAvailableBalance() - amount, toAccount.getAvailableBalance(),
                BankingConstants.DEFAULT_CURRENCY_CODE, (short) 2, Instant.now())));
    }

    private static void seedExistingInternalTransfer(FakeBankTransactionRepository transactionRepository,
        FakeLedgerEntryRepository ledgerRepository, BankAccount fromAccount, BankAccount toAccount, long transactionId,
        String requestId, BankTransactionType transactionType, BankBusinessType businessType, long amount,
        String sourceServerId, String operatorType, String operatorRef, String playerRef) {
        BankTransaction existingTransaction = transactionRepository.addExisting(new BankTransaction(
            transactionId,
            requestId,
            transactionType,
            businessType,
            "internal:" + fromAccount.getAccountId() + ":" + toAccount.getAccountId(),
            sourceServerId,
            operatorType,
            operatorRef,
            playerRef,
            "existing",
            "{}",
            Instant.now()));
        ledgerRepository.seed(existingTransaction.getTransactionId(), Arrays.asList(
            new LedgerEntry(transactionId * 10L + 1L, existingTransaction.getTransactionId(), fromAccount.getAccountId(),
                LedgerEntrySide.DEBIT, amount, fromAccount.getAvailableBalance() + amount, fromAccount.getAvailableBalance(),
                BankingConstants.DEFAULT_CURRENCY_CODE, (short) 1, Instant.now()),
            new LedgerEntry(transactionId * 10L + 2L, existingTransaction.getTransactionId(), toAccount.getAccountId(),
                LedgerEntrySide.CREDIT, amount, toAccount.getAvailableBalance() - amount, toAccount.getAvailableBalance(),
                BankingConstants.DEFAULT_CURRENCY_CODE, (short) 2, Instant.now())));
    }

    private static void seedExistingFrozenBalanceChange(FakeBankTransactionRepository transactionRepository,
        FakeLedgerEntryRepository ledgerRepository, BankAccount account, long transactionId, String requestId,
        BankTransactionType transactionType, BankBusinessType businessType, long amount, long availableBefore,
        long availableAfter, long frozenBefore, long frozenAfter) {
        BankTransaction existingTransaction = transactionRepository.addExisting(new BankTransaction(
            transactionId,
            requestId,
            transactionType,
            businessType,
            "market:" + account.getAccountId(),
            "test-server",
            BankingConstants.OPERATOR_TYPE_SYSTEM,
            "market",
            account.getOwnerRef(),
            "existing",
            "{}",
            Instant.now()));
        ledgerRepository.seed(existingTransaction.getTransactionId(), Collections.singletonList(
            new LedgerEntry(transactionId * 10L + 1L, existingTransaction.getTransactionId(), account.getAccountId(),
                transactionType == BankTransactionType.MARKET_FUNDS_FREEZE ? LedgerEntrySide.DEBIT : LedgerEntrySide.CREDIT,
                amount, availableBefore, availableAfter, frozenBefore, frozenAfter,
                BankingConstants.DEFAULT_CURRENCY_CODE, (short) 1, Instant.now())));
    }

    private static void seedExistingInternalTransferWithFrozenBalances(FakeBankTransactionRepository transactionRepository,
        FakeLedgerEntryRepository ledgerRepository, BankAccount fromAccount, BankAccount toAccount, long transactionId,
        String requestId, BankTransactionType transactionType, BankBusinessType businessType, long amount,
        String sourceServerId, String operatorType, String operatorRef, String playerRef, long fromAvailableBefore,
        long fromAvailableAfter, long fromFrozenBefore, long fromFrozenAfter, long toAvailableBefore,
        long toAvailableAfter, long toFrozenBefore, long toFrozenAfter) {
        BankTransaction existingTransaction = transactionRepository.addExisting(new BankTransaction(
            transactionId,
            requestId,
            transactionType,
            businessType,
            "internal:" + fromAccount.getAccountId() + ":" + toAccount.getAccountId(),
            sourceServerId,
            operatorType,
            operatorRef,
            playerRef,
            "existing",
            "{}",
            Instant.now()));
        ledgerRepository.seed(existingTransaction.getTransactionId(), Arrays.asList(
            new LedgerEntry(transactionId * 10L + 1L, existingTransaction.getTransactionId(), fromAccount.getAccountId(),
                LedgerEntrySide.DEBIT, amount, fromAvailableBefore, fromAvailableAfter, fromFrozenBefore,
                fromFrozenAfter, BankingConstants.DEFAULT_CURRENCY_CODE, (short) 1, Instant.now()),
            new LedgerEntry(transactionId * 10L + 2L, existingTransaction.getTransactionId(), toAccount.getAccountId(),
                LedgerEntrySide.CREDIT, amount, toAvailableBefore, toAvailableAfter, toFrozenBefore, toFrozenAfter,
                BankingConstants.DEFAULT_CURRENCY_CODE, (short) 2, Instant.now())));
    }

    private static long affectedBalance(BankPostingResult result, long accountId) {
        for (BankAccount account : result.getAffectedAccounts()) {
            if (account.getAccountId() == accountId) {
                return account.getAvailableBalance();
            }
        }
        fail("Expected affected account " + accountId);
        return -1L;
    }

    private static BankAccount affectedAccount(BankPostingResult result, long accountId) {
        for (BankAccount account : result.getAffectedAccounts()) {
            if (account.getAccountId() == accountId) {
                return account;
            }
        }
        fail("Expected affected account " + accountId);
        return null;
    }

    private static void assertRequestConflict(BankingApplicationService service, PlayerTransferCommand command,
        String expectedField) {
        try {
            service.transferBetweenPlayers(command);
            fail("Expected request conflict for field " + expectedField);
        } catch (BankingException exception) {
            assertTrue(exception.getMessage().contains("requestId conflicts with existing transaction"));
            assertTrue(exception.getMessage().contains(expectedField));
        }
    }

    private static void assertInternalTransferConflict(BankingApplicationService service,
        InternalTransferCommand command, String expectedField) {
        try {
            service.postInternalTransfer(command);
            fail("Expected request conflict for field " + expectedField);
        } catch (BankingException exception) {
            assertTrue(exception.getMessage().contains("requestId conflicts with existing transaction"));
            assertTrue(exception.getMessage().contains(expectedField));
        }
    }

    private static final class FakeBankAccountRepository implements BankAccountRepository {

        private final Map<Long, BankAccount> accountsById = new LinkedHashMap<Long, BankAccount>();
        private final Map<String, BankAccount> accountsByOwner = new HashMap<String, BankAccount>();
        private final Map<String, Integer> hiddenOwnerLookups = new HashMap<String, Integer>();
        private long nextId = 100L;
        private int updateCalls;

        private BankAccount addAccount(BankAccount account) {
            accountsById.put(Long.valueOf(account.getAccountId()), account);
            accountsByOwner.put(ownerKey(account.getOwnerType(), account.getOwnerRef(), account.getCurrencyCode()), account);
            nextId = Math.max(nextId, account.getAccountId() + 1L);
            return account;
        }

        private void hideOwnerLookup(String ownerType, String ownerRef, String currencyCode, int times) {
            hiddenOwnerLookups.put(ownerKey(ownerType, ownerRef, currencyCode), Integer.valueOf(times));
        }

        private void replaceBalances(long accountId, long availableBalance, long frozenBalance) {
            BankAccount current = lockById(accountId);
            addAccount(new BankAccount(
                current.getAccountId(),
                current.getAccountNo(),
                current.getAccountType(),
                current.getOwnerType(),
                current.getOwnerRef(),
                current.getCurrencyCode(),
                availableBalance,
                frozenBalance,
                current.getStatus(),
                current.getVersion(),
                current.getDisplayName(),
                current.getMetadataJson(),
                current.getCreatedAt(),
                Instant.now()));
        }

        @Override
        public Optional<BankAccount> findById(long accountId) {
            return Optional.ofNullable(accountsById.get(Long.valueOf(accountId)));
        }

        @Override
        public Optional<BankAccount> findByOwner(String ownerType, String ownerRef, String currencyCode) {
            String key = ownerKey(ownerType, ownerRef, currencyCode);
            Integer hidden = hiddenOwnerLookups.get(key);
            if (hidden != null && hidden.intValue() > 0) {
                hiddenOwnerLookups.put(key, Integer.valueOf(hidden.intValue() - 1));
                return Optional.empty();
            }
            return Optional.ofNullable(accountsByOwner.get(key));
        }

        @Override
        public BankAccount save(BankAccount account) {
            BankAccount persisted = new BankAccount(
                nextId++,
                account.getAccountNo(),
                account.getAccountType(),
                account.getOwnerType(),
                account.getOwnerRef(),
                account.getCurrencyCode(),
                account.getAvailableBalance(),
                account.getFrozenBalance(),
                account.getStatus(),
                account.getVersion(),
                account.getDisplayName(),
                account.getMetadataJson(),
                account.getCreatedAt(),
                account.getUpdatedAt());
            return addAccount(persisted);
        }

        @Override
        public Optional<BankAccount> saveIfOwnerAbsent(BankAccount account) {
            if (accountsByOwner.containsKey(ownerKey(account.getOwnerType(), account.getOwnerRef(), account.getCurrencyCode()))) {
                return Optional.empty();
            }
            return Optional.of(save(account));
        }

        @Override
        public BankAccount lockById(long accountId) {
            BankAccount account = accountsById.get(Long.valueOf(accountId));
            if (account == null) {
                throw new BankingException("account not found: " + accountId);
            }
            return account;
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
            updateCalls++;
            addAccount(new BankAccount(
                current.getAccountId(),
                current.getAccountNo(),
                current.getAccountType(),
                current.getOwnerType(),
                current.getOwnerRef(),
                current.getCurrencyCode(),
                availableBalance,
                frozenBalance,
                current.getStatus(),
                expectedVersion + 1L,
                current.getDisplayName(),
                current.getMetadataJson(),
                current.getCreatedAt(),
                Instant.now()));
        }

        private String ownerKey(String ownerType, String ownerRef, String currencyCode) {
            return ownerType + "|" + ownerRef + "|" + currencyCode;
        }
    }

    private static final class FakeBankTransactionRepository implements BankTransactionRepository {

        private final Map<String, BankTransaction> transactionsByRequestId = new HashMap<String, BankTransaction>();
        private final Map<String, Integer> hiddenRequestLookups = new HashMap<String, Integer>();
        private long nextId = 1000L;

        private BankTransaction addExisting(BankTransaction transaction) {
            transactionsByRequestId.put(transaction.getRequestId(), transaction);
            nextId = Math.max(nextId, transaction.getTransactionId() + 1L);
            return transaction;
        }

        private void hideRequestLookup(String requestId, int times) {
            hiddenRequestLookups.put(requestId, Integer.valueOf(times));
        }

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
            Integer hidden = hiddenRequestLookups.get(requestId);
            if (hidden != null && hidden.intValue() > 0) {
                hiddenRequestLookups.put(requestId, Integer.valueOf(hidden.intValue() - 1));
                return Optional.empty();
            }
            return Optional.ofNullable(transactionsByRequestId.get(requestId));
        }

        @Override
        public BankTransaction save(BankTransaction transaction) {
            BankTransaction persisted = new BankTransaction(
                nextId++,
                transaction.getRequestId(),
                transaction.getTransactionType(),
                transaction.getBusinessType(),
                transaction.getBusinessRef(),
                transaction.getSourceServerId(),
                transaction.getOperatorType(),
                transaction.getOperatorRef(),
                transaction.getPlayerRef(),
                transaction.getComment(),
                transaction.getExtraJson(),
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
        private int appendCalls;

        private void seed(long transactionId, List<LedgerEntry> entries) {
            entriesByTransactionId.put(Long.valueOf(transactionId), new ArrayList<LedgerEntry>(entries));
        }

        @Override
        public void appendEntries(List<LedgerEntry> entries) {
            appendCalls++;
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
            List<LedgerEntry> matches = new ArrayList<LedgerEntry>();
            for (List<LedgerEntry> entries : entriesByTransactionId.values()) {
                for (LedgerEntry entry : entries) {
                    if (entry.getAccountId() == accountId) {
                        matches.add(entry);
                    }
                }
            }
            return matches.size() > limit ? new ArrayList<LedgerEntry>(matches.subList(0, limit)) : matches;
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