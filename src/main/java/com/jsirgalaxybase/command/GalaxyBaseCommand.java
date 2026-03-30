package com.jsirgalaxybase.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import com.jsirgalaxybase.module.ModModule;
import com.jsirgalaxybase.module.ModuleManager;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.application.command.InternalTransferCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.OpenAccountCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.PlayerTransferCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;
import com.jsirgalaxybase.modules.core.banking.domain.BankBusinessType;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransactionType;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntry;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntrySide;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts.ManagedAccountSpec;

public class GalaxyBaseCommand extends CommandBase {

    private final ModuleManager moduleManager;

    public GalaxyBaseCommand(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public String getCommandName() {
        return "jsirgalaxybase";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/jsirgalaxybase [modules|architecture|bank]";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("gb", "jgbase");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && "bank".equalsIgnoreCase(args[0])) {
            processBankCommand(sender, args);
            return;
        }

        if (args.length == 0 || "modules".equalsIgnoreCase(args[0]) || "architecture".equalsIgnoreCase(args[0])) {
            sender.addChatMessage(new ChatComponentText("JsirGalaxyBase architecture: modular monolith, institution core + capability modules"));
            for (ModModule module : moduleManager.getModules()) {
                sender.addChatMessage(new ChatComponentText("- [" + module.getCategory() + "] " + module.getDisplayName() + " (" + module.getId() + ")"));
            }
            return;
        }

        sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, new String[] { "modules", "architecture", "bank" });
        }
        if (args.length == 2 && "bank".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args,
                new String[] { "help", "open", "balance", "ledger", "system", "public", "tx", "init",
                    "grant", "transfer" });
        }
        if (args.length >= 3 && "bank".equalsIgnoreCase(args[0])) {
            String action = args[1].toLowerCase();
            if ("open".equals(action) || "balance".equals(action) || "grant".equals(action)
                || "ledger".equals(action)) {
                return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
            }
            if ("transfer".equals(action) && (args.length == 3 || args.length == 4)) {
                return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
            }
            if ("system".equals(action) && args.length == 3) {
                return getListOfStringsMatchingLastWord(args, new String[] { "summary", "ledger" });
            }
            if ("public".equals(action) && args.length == 3) {
                return getListOfStringsMatchingLastWord(args, new String[] { "ledger", "all", "ops", "exchange" });
            }
            if ("public".equals(action) && args.length == 4 && "ledger".equalsIgnoreCase(args[2])) {
                return getListOfStringsMatchingLastWord(args, new String[] { "ops", "exchange" });
            }
            if ("init".equals(action) && args.length == 3) {
                return getListOfStringsMatchingLastWord(args, new String[] { "system" });
            }
        }
        return new ArrayList<String>();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    private void processBankCommand(ICommandSender sender, String[] args) {
        if (!sender.canCommandSenderUseCommand(2, getCommandName())) {
            sender.addChatMessage(new ChatComponentText("You must be an operator to use bank admin commands."));
            return;
        }

        if (args.length == 1 || "help".equalsIgnoreCase(args[1])) {
            sendBankUsage(sender);
            return;
        }

        InstitutionCoreModule institutionCoreModule = moduleManager.findModule(InstitutionCoreModule.class);
        if (institutionCoreModule == null || institutionCoreModule.getBankingInfrastructure() == null) {
            sender.addChatMessage(new ChatComponentText("Banking infrastructure is not available. Check PostgreSQL config and server startup logs."));
            return;
        }

        BankingInfrastructure bankingInfrastructure = institutionCoreModule.getBankingInfrastructure();
        BankingApplicationService bankingService = bankingInfrastructure.getBankingApplicationService();

        try {
            String action = args[1].toLowerCase();
            if ("open".equals(action)) {
                handleBankOpen(sender, args, bankingService);
                return;
            }
            if ("balance".equals(action)) {
                handleBankBalance(sender, args, bankingService);
                return;
            }
            if ("ledger".equals(action)) {
                handleBankLedger(sender, args, bankingService);
                return;
            }
            if ("system".equals(action)) {
                handleBankSystem(sender, args, bankingInfrastructure, bankingService);
                return;
            }
            if ("public".equals(action)) {
                handleBankPublic(sender, args, bankingInfrastructure, bankingService);
                return;
            }
            if ("tx".equals(action)) {
                handleBankTransactionDetail(sender, args, bankingInfrastructure);
                return;
            }
            if ("init".equals(action)) {
                handleBankInit(sender, args, bankingInfrastructure);
                return;
            }
            if ("grant".equals(action)) {
                handleBankGrant(sender, args, institutionCoreModule, bankingInfrastructure, bankingService);
                return;
            }
            if ("transfer".equals(action)) {
                handleBankTransfer(sender, args, institutionCoreModule, bankingService);
                return;
            }

            sendBankUsage(sender);
        } catch (PlayerNotFoundException e) {
            sender.addChatMessage(new ChatComponentText("Target player must be online: " + e.getMessage()));
        } catch (NumberFormatException e) {
            sender.addChatMessage(new ChatComponentText("Amount must be a valid integer."));
        } catch (BankingException e) {
            sender.addChatMessage(new ChatComponentText("Banking error: " + e.getMessage()));
        }
    }

    private void handleBankOpen(ICommandSender sender, String[] args, BankingApplicationService bankingService) {
        if (args.length < 3) {
            sender.addChatMessage(new ChatComponentText("Usage: /jsirgalaxybase bank open <player>"));
            return;
        }

        EntityPlayerMP targetPlayer = getPlayer(sender, args[2]);
        BankAccount account = ensurePlayerAccount(bankingService, targetPlayer);
        sender.addChatMessage(new ChatComponentText(
            "Opened or reused account for " + targetPlayer.getCommandSenderName() + ": id=" + account.getAccountId()
                + ", no=" + account.getAccountNo() + ", balance=" + account.getAvailableBalance()));
    }

    private void handleBankBalance(ICommandSender sender, String[] args, BankingApplicationService bankingService) {
        if (args.length < 3) {
            sender.addChatMessage(new ChatComponentText("Usage: /jsirgalaxybase bank balance <player>"));
            return;
        }

        EntityPlayerMP targetPlayer = getPlayer(sender, args[2]);
        Optional<BankAccount> account = bankingService.findAccount(BankingConstants.OWNER_TYPE_PLAYER_UUID,
            targetPlayer.getUniqueID().toString(), BankingConstants.DEFAULT_CURRENCY_CODE);
        if (!account.isPresent()) {
            sender.addChatMessage(new ChatComponentText(
                "No bank account exists yet for " + targetPlayer.getCommandSenderName() + "."));
            return;
        }

        BankAccount bankAccount = account.get();
        sender.addChatMessage(new ChatComponentText(
            "Account " + bankAccount.getAccountNo() + " of " + targetPlayer.getCommandSenderName() + ": balance="
                + bankAccount.getAvailableBalance() + ", status=" + bankAccount.getStatus()));
    }

    private void handleBankLedger(ICommandSender sender, String[] args, BankingApplicationService bankingService) {
        if (args.length < 3) {
            sender.addChatMessage(new ChatComponentText("Usage: /jsirgalaxybase bank ledger <player> [limit]"));
            return;
        }

        EntityPlayerMP targetPlayer = getPlayer(sender, args[2]);
        Optional<BankAccount> account = bankingService.findAccount(BankingConstants.OWNER_TYPE_PLAYER_UUID,
            targetPlayer.getUniqueID().toString(), BankingConstants.DEFAULT_CURRENCY_CODE);
        if (!account.isPresent()) {
            sender.addChatMessage(new ChatComponentText(
                "No bank account exists yet for " + targetPlayer.getCommandSenderName() + "."));
            return;
        }

        int limit = parseLedgerLimit(args, 3);
        List<LedgerEntry> entries = bankingService.getRecentEntries(account.get().getAccountId(), limit);
        sender.addChatMessage(new ChatComponentText(
            "Recent ledger of " + targetPlayer.getCommandSenderName() + " (limit=" + limit + ")"));
        sendLedgerEntries(sender, entries);
    }

    private void handleBankSystem(ICommandSender sender, String[] args, BankingInfrastructure bankingInfrastructure,
        BankingApplicationService bankingService) {
        BankAccount systemAccount = ensureManagedAccount(bankingInfrastructure, ManagedBankAccounts.SYSTEM_OPERATIONS_ACCOUNT);
        if (args.length >= 3 && "ledger".equalsIgnoreCase(args[2])) {
            int limit = parseLedgerLimit(args, 3);
            List<LedgerEntry> entries = bankingService.getRecentEntries(systemAccount.getAccountId(), limit);
            sender.addChatMessage(new ChatComponentText(
                "Recent ledger of system operations account " + systemAccount.getAccountNo() + " (limit=" + limit + ")"));
            sendLedgerEntries(sender, entries);
            return;
        }

        sender.addChatMessage(new ChatComponentText("System operations account summary:"));
        sender.addChatMessage(new ChatComponentText(
            "id=" + systemAccount.getAccountId() + ", no=" + systemAccount.getAccountNo() + ", ownerRef="
                + systemAccount.getOwnerRef()));
        sender.addChatMessage(new ChatComponentText(
            "type=" + systemAccount.getAccountType() + ", status=" + systemAccount.getStatus() + ", balance="
                + systemAccount.getAvailableBalance() + ", frozen=" + systemAccount.getFrozenBalance()));
    }

    private void handleBankPublic(ICommandSender sender, String[] args, BankingInfrastructure bankingInfrastructure,
        BankingApplicationService bankingService) {
        if (args.length >= 3 && "ledger".equalsIgnoreCase(args[2])) {
            if (args.length < 4) {
                sender.addChatMessage(new ChatComponentText(
                    "Usage: /jsirgalaxybase bank public ledger <ops|exchange> [limit]"));
                return;
            }

            ManagedAccountSpec spec = requireManagedAccountSpec(args[3]);
            BankAccount account = ensureManagedAccount(bankingInfrastructure, spec);
            int limit = parseLedgerLimit(args, 4);
            List<LedgerEntry> entries = bankingService.getRecentEntries(account.getAccountId(), limit);
            sender.addChatMessage(new ChatComponentText(
                "Recent ledger of managed account " + spec.getCommandKey() + " (limit=" + limit + ")"));
            sendLedgerEntries(sender, entries);
            return;
        }

        if (args.length == 2 || "all".equalsIgnoreCase(args[2])) {
            sender.addChatMessage(new ChatComponentText("Managed system account summary:"));
            for (ManagedAccountSpec spec : ManagedBankAccounts.getManagedAccounts()) {
                sendManagedAccountSummary(sender, ensureManagedAccount(bankingInfrastructure, spec), spec.getCommandKey());
            }
            return;
        }

        ManagedAccountSpec spec = requireManagedAccountSpec(args[2]);
        sendManagedAccountSummary(sender, ensureManagedAccount(bankingInfrastructure, spec), spec.getCommandKey());
    }

    private void handleBankTransactionDetail(ICommandSender sender, String[] args, BankingInfrastructure bankingInfrastructure) {
        if (args.length < 3) {
            sender.addChatMessage(new ChatComponentText("Usage: /jsirgalaxybase bank tx <transactionId>"));
            return;
        }

        long transactionId = Long.parseLong(args[2]);
        Optional<com.jsirgalaxybase.modules.core.banking.domain.BankTransaction> transaction = bankingInfrastructure
            .getBankTransactionRepository().findById(transactionId);
        if (!transaction.isPresent()) {
            sender.addChatMessage(new ChatComponentText("Transaction not found: " + transactionId));
            return;
        }

        com.jsirgalaxybase.modules.core.banking.domain.BankTransaction tx = transaction.get();
        sender.addChatMessage(new ChatComponentText(
            "tx=" + tx.getTransactionId() + ", type=" + tx.getTransactionType() + ", business="
                + tx.getBusinessType() + ", requestId=" + tx.getRequestId()));
        sender.addChatMessage(new ChatComponentText(
            "sourceServer=" + tx.getSourceServerId() + ", operatorType=" + tx.getOperatorType() + ", operatorRef="
                + safeText(tx.getOperatorRef()) + ", playerRef=" + safeText(tx.getPlayerRef())));
        sender.addChatMessage(new ChatComponentText(
            "businessRef=" + safeText(tx.getBusinessRef()) + ", comment=" + safeText(tx.getComment()) + ", at="
                + tx.getCreatedAt()));

        List<LedgerEntry> entries = bankingInfrastructure.getLedgerEntryRepository().findByTransactionId(transactionId);
        if (entries.isEmpty()) {
            sender.addChatMessage(new ChatComponentText("No ledger entries found for this transaction."));
        } else {
            for (LedgerEntry entry : entries) {
                Optional<BankAccount> account = bankingInfrastructure.getBankAccountRepository().findById(entry.getAccountId());
                String accountInfo = account.isPresent()
                    ? account.get().getAccountNo() + "/" + account.get().getOwnerRef()
                    : "accountId=" + entry.getAccountId();
                sender.addChatMessage(new ChatComponentText(
                    "entry account=" + accountInfo + ", side=" + entry.getEntrySide() + ", amount="
                        + entry.getAmount() + ", before=" + entry.getBalanceBefore() + ", after="
                        + entry.getBalanceAfter()));
            }
        }

        Optional<com.jsirgalaxybase.modules.core.banking.domain.CoinExchangeRecord> exchangeRecord = bankingInfrastructure
            .getCoinExchangeRecordRepository().findByTransactionId(transactionId);
        if (exchangeRecord.isPresent()) {
            sender.addChatMessage(new ChatComponentText(
                "exchange playerRef=" + exchangeRecord.get().getPlayerRef() + ", family="
                    + exchangeRecord.get().getCoinFamily() + ", tier=" + exchangeRecord.get().getCoinTier()
                    + ", effectiveValue=" + exchangeRecord.get().getEffectiveExchangeValue()));
        }
    }

    private void handleBankInit(ICommandSender sender, String[] args, BankingInfrastructure bankingInfrastructure) {
        if (args.length < 3 || !"system".equalsIgnoreCase(args[2])) {
            sender.addChatMessage(new ChatComponentText("Usage: /jsirgalaxybase bank init system"));
            return;
        }

        sender.addChatMessage(new ChatComponentText("Ensuring managed system accounts:"));
        for (ManagedAccountSpec spec : ManagedBankAccounts.getManagedAccounts()) {
            BankAccount account = ensureManagedAccount(bankingInfrastructure, spec);
            sendManagedAccountSummary(sender, account, spec.getCommandKey());
        }
    }

    private void handleBankGrant(ICommandSender sender, String[] args, InstitutionCoreModule institutionCoreModule,
        BankingInfrastructure bankingInfrastructure, BankingApplicationService bankingService) {
        if (args.length < 4) {
            sender.addChatMessage(new ChatComponentText("Usage: /jsirgalaxybase bank grant <player> <amount> [comment]"));
            return;
        }

        EntityPlayerMP targetPlayer = getPlayer(sender, args[2]);
        long amount = Long.parseLong(args[3]);
        String comment = joinTail(args, 4);

        BankAccount targetAccount = ensurePlayerAccount(bankingService, targetPlayer);
        BankAccount systemAccount = ensureManagedAccount(bankingInfrastructure, ManagedBankAccounts.SYSTEM_OPERATIONS_ACCOUNT);
        BankPostingResult result = bankingService.postInternalTransfer(new InternalTransferCommand(
            newRequestId("grant"),
            BankTransactionType.SYSTEM_GRANT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            systemAccount.getAccountId(),
            targetAccount.getAccountId(),
            institutionCoreModule.getBankingSourceServerId(),
            BankingConstants.OPERATOR_TYPE_ADMIN,
            sender.getCommandSenderName(),
            targetPlayer.getUniqueID().toString(),
            amount,
            comment == null ? "ops account grant" : comment,
            targetPlayer.getUniqueID().toString(),
            "{\"command\":\"grant\"}"));

        sender.addChatMessage(new ChatComponentText(
            "Granted " + amount + " to " + targetPlayer.getCommandSenderName() + ". transactionId="
                + result.getTransaction().getTransactionId() + ", newBalance="
                + findAccountBalance(result, targetAccount.getAccountId())));
    }

    private void handleBankTransfer(ICommandSender sender, String[] args, InstitutionCoreModule institutionCoreModule,
        BankingApplicationService bankingService) {
        if (args.length < 5) {
            sender.addChatMessage(new ChatComponentText(
                "Usage: /jsirgalaxybase bank transfer <fromPlayer> <toPlayer> <amount> [comment]"));
            return;
        }

        EntityPlayerMP fromPlayer = getPlayer(sender, args[2]);
        EntityPlayerMP toPlayer = getPlayer(sender, args[3]);
        long amount = Long.parseLong(args[4]);
        String comment = joinTail(args, 5);

        BankAccount fromAccount = ensurePlayerAccount(bankingService, fromPlayer);
        BankAccount toAccount = ensurePlayerAccount(bankingService, toPlayer);
        BankPostingResult result = bankingService.transferBetweenPlayers(new PlayerTransferCommand(
            newRequestId("transfer"),
            fromAccount.getAccountId(),
            toAccount.getAccountId(),
            institutionCoreModule.getBankingSourceServerId(),
            sender.getCommandSenderName(),
            fromPlayer.getUniqueID().toString(),
            amount,
            comment == null ? "admin test transfer" : comment,
            fromPlayer.getUniqueID().toString() + "->" + toPlayer.getUniqueID().toString(),
            "{\"command\":\"transfer\"}"));

        sender.addChatMessage(new ChatComponentText(
            "Transferred " + amount + " from " + fromPlayer.getCommandSenderName() + " to "
                + toPlayer.getCommandSenderName() + ". transactionId=" + result.getTransaction().getTransactionId()));
        sender.addChatMessage(new ChatComponentText(
            fromPlayer.getCommandSenderName() + " balance=" + findAccountBalance(result, fromAccount.getAccountId())
                + ", " + toPlayer.getCommandSenderName() + " balance="
                + findAccountBalance(result, toAccount.getAccountId())));
    }

    private BankAccount ensurePlayerAccount(BankingApplicationService bankingService, EntityPlayerMP player) {
        return bankingService.openAccount(new OpenAccountCommand(
            null,
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            player.getUniqueID().toString(),
            BankingConstants.DEFAULT_CURRENCY_CODE,
            player.getCommandSenderName(),
            "{\"kind\":\"player\"}"));
    }

    private BankAccount ensureManagedAccount(BankingInfrastructure bankingInfrastructure, ManagedAccountSpec spec) {
        return ManagedBankAccounts.ensureManagedAccount(bankingInfrastructure, spec);
    }

    private ManagedAccountSpec requireManagedAccountSpec(String commandKey) {
        return ManagedBankAccounts.requireManagedAccountSpec(commandKey);
    }

    private void sendManagedAccountSummary(ICommandSender sender, BankAccount account, String commandKey) {
        sender.addChatMessage(new ChatComponentText(
            "[" + commandKey + "] id=" + account.getAccountId() + ", no=" + account.getAccountNo() + ", type="
                + account.getAccountType() + ", ownerRef=" + account.getOwnerRef() + ", status="
                + account.getStatus() + ", balance=" + account.getAvailableBalance()));
    }

    private long findAccountBalance(BankPostingResult result, long accountId) {
        for (BankAccount account : result.getAffectedAccounts()) {
            if (account.getAccountId() == accountId) {
                return account.getAvailableBalance();
            }
        }
        return -1L;
    }

    private String newRequestId(String action) {
        return "cmd-" + action + "-" + UUID.randomUUID().toString();
    }

    private int parseLedgerLimit(String[] args, int index) {
        if (args.length <= index) {
            return 5;
        }

        int limit = Integer.parseInt(args[index]);
        if (limit <= 0) {
            throw new BankingException("ledger limit must be positive");
        }
        return Math.min(limit, 20);
    }

    private String safeText(String value) {
        return value == null ? "-" : value;
    }

    private void sendLedgerEntries(ICommandSender sender, List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            sender.addChatMessage(new ChatComponentText("No ledger entries found."));
            return;
        }

        for (LedgerEntry entry : entries) {
            String sign = entry.getEntrySide() == LedgerEntrySide.DEBIT ? "-" : "+";
            sender.addChatMessage(new ChatComponentText(
                "tx=" + entry.getTransactionId() + " " + sign + entry.getAmount() + " "
                    + entry.getCurrencyCode() + " before=" + entry.getBalanceBefore() + " after="
                    + entry.getBalanceAfter() + " side=" + entry.getEntrySide() + " at="
                    + entry.getCreatedAt()));
        }
    }

    private String joinTail(String[] args, int startIndex) {
        if (args.length <= startIndex) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private void sendBankUsage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText("Bank admin commands:"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank open <player>"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank balance <player>"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank ledger <player> [limit]"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank system [summary]"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank system ledger [limit]"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank public [all|ops|exchange]"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank public ledger <ops|exchange> [limit]"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank tx <transactionId>"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank init system"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank grant <player> <amount> [comment]"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank transfer <fromPlayer> <toPlayer> <amount> [comment]"));
    }
}