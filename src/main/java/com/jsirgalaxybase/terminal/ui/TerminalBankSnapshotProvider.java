package com.jsirgalaxybase.terminal.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModuleManager;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntry;
import com.jsirgalaxybase.modules.core.banking.domain.LedgerEntrySide;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts;

public final class TerminalBankSnapshotProvider {

    public static final TerminalBankSnapshotProvider INSTANCE = new TerminalBankSnapshotProvider();

    private static final int PLAYER_LEDGER_LIMIT = 4;
    private static final int EXCHANGE_LEDGER_LIMIT = 4;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm")
        .withZone(ZoneId.systemDefault());

    private TerminalBankSnapshotProvider() {}

    public TerminalBankSnapshot create(EntityPlayer player) {
        InstitutionCoreModule institutionModule = resolveInstitutionCoreModule();
        if (institutionModule == null || institutionModule.getBankingInfrastructure() == null) {
            return createUnavailableSnapshot();
        }

        try {
            BankingInfrastructure bankingInfrastructure = institutionModule.getBankingInfrastructure();
            Optional<BankAccount> playerAccount = resolvePlayerAccount(player, bankingInfrastructure);
            Optional<BankAccount> exchangeAccount = bankingInfrastructure.getBankAccountRepository().findByOwner(
                ManagedBankAccounts.EXCHANGE_RESERVE_ACCOUNT.getOwnerType(),
                ManagedBankAccounts.EXCHANGE_RESERVE_ACCOUNT.getOwnerRef(),
                BankingConstants.DEFAULT_CURRENCY_CODE);
            return new TerminalBankSnapshot(
                "银行服务在线",
                playerAccount.map(this::formatBalance).orElse("未开户"),
                playerAccount.map(this::formatPlayerStatus).orElse("未开户 / 按需开户 / 余额 0"),
                playerAccount.map(BankAccount::getAccountNo).orElse("未分配"),
                playerAccount.map(BankAccount::getUpdatedAt).map(this::formatTime).orElse("无更新记录"),
                playerAccount.isPresent() ? "可查看余额与流水，并可向已开户玩家转账；离线目标需服务端可识别"
                    : "当前可在终端内为自己开户，开户后即可向已开户玩家转账",
                playerAccount.map(account -> formatLedgerLines(
                    bankingInfrastructure.getLedgerEntryRepository().findRecentByAccountId(
                        account.getAccountId(),
                        PLAYER_LEDGER_LIMIT),
                    PLAYER_LEDGER_LIMIT,
                    "当前没有个人流水记录"))
                    .orElse(createDefaultLines(PLAYER_LEDGER_LIMIT, "尚未开户，暂无个人流水")),
                exchangeAccount.map(this::formatBalance).orElse("储备账户缺失"),
                exchangeAccount.map(this::formatExchangeStatus).orElse("公开账户未就绪"),
                exchangeAccount.map(BankAccount::getAccountNo).orElse("未分配"),
                exchangeAccount.map(BankAccount::getUpdatedAt).map(this::formatTime).orElse("无更新记录"),
                exchangeAccount.map(account -> formatLedgerLines(
                    bankingInfrastructure.getLedgerEntryRepository().findRecentByAccountId(
                        account.getAccountId(),
                        EXCHANGE_LEDGER_LIMIT),
                    EXCHANGE_LEDGER_LIMIT,
                    "当前没有公开 exchange 流水"))
                    .orElse(createDefaultLines(EXCHANGE_LEDGER_LIMIT, "公开 exchange 流水暂不可用")));
        } catch (RuntimeException exception) {
            GalaxyBase.LOG.warn("Failed to build terminal bank snapshot", exception);
            return createErrorSnapshot();
        }
    }

    private InstitutionCoreModule resolveInstitutionCoreModule() {
        if (GalaxyBase.proxy == null) {
            return null;
        }
        ModuleManager moduleManager = GalaxyBase.proxy.getModuleManager();
        if (moduleManager == null) {
            return null;
        }
        return moduleManager.findModule(InstitutionCoreModule.class);
    }

    private Optional<BankAccount> resolvePlayerAccount(EntityPlayer player, BankingInfrastructure bankingInfrastructure) {
        if (player == null) {
            return Optional.empty();
        }
        return bankingInfrastructure.getBankingApplicationService().findAccount(
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            player.getUniqueID().toString(),
            BankingConstants.DEFAULT_CURRENCY_CODE);
    }

    private TerminalBankSnapshot createUnavailableSnapshot() {
        MinecraftServer server = MinecraftServer.getServer();
        boolean dedicatedServer = server != null && server.isDedicatedServer();
        String unavailableMessage = dedicatedServer ? TerminalBankingService.describeInfrastructureUnavailable()
            : "单人游戏与局域网内开当前不启用银行基础设施";
        return new TerminalBankSnapshot(
            dedicatedServer ? "银行未接入 PostgreSQL" : "银行仅限独立服务端",
            "不可用",
            unavailableMessage,
            "未分配",
            "无更新记录",
            dedicatedServer ? "请检查服务端 jsirgalaxybase-server.cfg、JDBC 配置和启动日志" : "请连接独立测试服后再使用开户与转账功能",
            createDefaultLines(PLAYER_LEDGER_LIMIT, "银行服务离线，无法读取个人流水"),
            "不可用",
            dedicatedServer ? "exchange 公开页当前不可用" : "独立服务端未连接，exchange 公开页不可用",
            "未分配",
            "无更新记录",
            createDefaultLines(EXCHANGE_LEDGER_LIMIT, "银行服务离线，无法读取 exchange 流水"));
    }

    private TerminalBankSnapshot createErrorSnapshot() {
        return new TerminalBankSnapshot(
            "银行快照读取失败",
            "读取失败",
            "请查看服务端日志中的 banking snapshot 异常",
            "读取失败",
            "读取失败",
            "银行快照异常，当前不建议继续执行开户或转账操作",
            createDefaultLines(PLAYER_LEDGER_LIMIT, "个人流水读取失败"),
            "读取失败",
            "exchange 公开页读取失败",
            "读取失败",
            "读取失败",
            createDefaultLines(EXCHANGE_LEDGER_LIMIT, "exchange 流水读取失败"));
    }

    private String formatBalance(BankAccount account) {
        return formatAmount(account.getAvailableBalance()) + " / " + account.getCurrencyCode();
    }

    private String formatPlayerStatus(BankAccount account) {
        return account.getStatus() + " / 按需开户 / 冻结 " + formatAmount(account.getFrozenBalance());
    }

    private String formatExchangeStatus(BankAccount account) {
        return account.getStatus() + " / 公开透明 / 兑换储备";
    }

    private String[] formatLedgerLines(List<LedgerEntry> entries, int limit, String emptyLine) {
        String[] lines = createDefaultLines(limit, emptyLine);
        for (int i = 0; i < entries.size() && i < limit; i++) {
            LedgerEntry entry = entries.get(i);
            lines[i] = formatLedgerLine(entry);
        }
        return lines;
    }

    private String[] createDefaultLines(int limit, String firstLine) {
        String[] lines = new String[limit];
        for (int i = 0; i < limit; i++) {
            lines[i] = i == 0 ? firstLine : "";
        }
        return lines;
    }

    private String formatLedgerLine(LedgerEntry entry) {
        String direction = entry.getEntrySide() == LedgerEntrySide.CREDIT ? "+" : "-";
        String sideText = entry.getEntrySide() == LedgerEntrySide.CREDIT ? "入账" : "出账";
        return formatTime(entry.getCreatedAt())
            + " | "
            + sideText
            + " "
            + direction
            + formatAmount(entry.getAmount())
            + " | 结余 "
            + formatAmount(entry.getBalanceAfter());
    }

    private String formatAmount(long amount) {
        return String.format("%,d", Long.valueOf(amount));
    }

    private String formatTime(Instant instant) {
        return instant == null ? "无记录" : TIME_FORMATTER.format(instant);
    }
}