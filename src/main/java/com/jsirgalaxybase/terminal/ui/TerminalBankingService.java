package com.jsirgalaxybase.terminal.ui;

import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumChatFormatting;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.config.ModConfiguration;
import com.jsirgalaxybase.module.ModuleManager;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.application.BankingException;
import com.jsirgalaxybase.modules.core.banking.application.command.OpenAccountCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.PlayerTransferCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;

public final class TerminalBankingService {

    public static final TerminalBankingService INSTANCE = new TerminalBankingService();

    private TerminalBankingService() {}

    public ActionResult openOwnAccount(EntityPlayer player) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return ActionResult.error("当前客户端上下文不能直接提交开户请求");
        }

        BankingContext context = resolveContext();
        if (!context.isReady()) {
            return ActionResult.error(context.getUnavailableMessage());
        }

        try {
            Optional<BankAccount> existingAccount = context.bankingService.findAccount(
                BankingConstants.OWNER_TYPE_PLAYER_UUID,
                serverPlayer.getUniqueID().toString(),
                BankingConstants.DEFAULT_CURRENCY_CODE);
            if (existingAccount.isPresent()) {
                BankAccount account = existingAccount.get();
                return ActionResult.info(
                    "账户已存在: "
                        + account.getAccountNo()
                        + " | 余额 "
                        + formatAmount(account.getAvailableBalance())
                        + " "
                        + account.getCurrencyCode()
                        + " | 无需重复开户");
            }

            BankAccount account = ensurePlayerAccount(context.bankingService, serverPlayer);
            return ActionResult.success(
                "开户成功: "
                    + account.getAccountNo()
                    + " | 余额 "
                    + formatAmount(account.getAvailableBalance())
                    + " "
                    + account.getCurrencyCode());
        } catch (BankingException exception) {
            GalaxyBase.LOG.warn("Terminal bank open action rejected: {}", exception.getMessage(), exception);
            return ActionResult.error(toClientSafeMessage(exception));
        } catch (RuntimeException exception) {
            GalaxyBase.LOG.warn("Terminal bank open action failed", exception);
            return ActionResult.error("开户失败，请查看服务端日志");
        }
    }

    public ActionResult transferToPlayer(EntityPlayer player, String targetPlayerName, long amount, String comment) {
        EntityPlayerMP fromPlayer = requireServerPlayer(player);
        if (fromPlayer == null) {
            return ActionResult.error("当前客户端上下文不能直接提交转账请求");
        }
        if (amount <= 0L) {
            return ActionResult.error("转账金额必须大于 0");
        }

        String normalizedTarget = normalizeTargetName(targetPlayerName);
        if (normalizedTarget == null) {
            return ActionResult.error("请填写收款玩家名");
        }

        EntityPlayerMP toPlayer = resolveOnlinePlayer(normalizedTarget);
        if (toPlayer != null && fromPlayer.getUniqueID().equals(toPlayer.getUniqueID())) {
            return ActionResult.error("不能给自己转账");
        }

        BankingContext context = resolveContext();
        if (!context.isReady()) {
            return ActionResult.error(context.getUnavailableMessage());
        }

        try {
            Optional<BankAccount> existingFrom = findExistingPlayerAccount(
                context.bankingService,
                fromPlayer.getUniqueID().toString());
            if (!existingFrom.isPresent()) {
                return ActionResult.error("你尚未开户，请先开户后再转账");
            }

            ResolvedPlayerIdentity targetIdentity = resolveTargetIdentity(normalizedTarget, toPlayer);
            if (targetIdentity == null) {
                return ActionResult.error("收款玩家不存在，或服务端当前无法解析其身份: " + normalizedTarget);
            }
            if (fromPlayer.getUniqueID().toString().equals(targetIdentity.playerRef)) {
                return ActionResult.error("不能给自己转账");
            }

            Optional<BankAccount> existingTarget = findExistingPlayerAccount(context.bankingService, targetIdentity.playerRef);
            if (!existingTarget.isPresent()) {
                return ActionResult.error("收款账户不存在: " + targetIdentity.displayName + "，请先让对方开户");
            }

            BankAccount fromAccount = existingFrom.get();
            BankAccount toAccount = existingTarget.get();
            String businessRef = buildPlayerTransferBusinessRef(fromAccount.getAccountId(), toAccount.getAccountId());
            if (fromAccount.getAccountId() == toAccount.getAccountId()) {
                return ActionResult.error("不能给自己转账");
            }
            if (fromAccount.getAvailableBalance() < amount) {
                return ActionResult.error(
                    "余额不足: 当前可用 "
                        + formatAmount(fromAccount.getAvailableBalance())
                        + " STARCOIN，无法转出 "
                        + formatAmount(amount)
                        + " STARCOIN");
            }
            String normalizedComment = normalizeComment(comment);
            BankPostingResult result = context.bankingService.transferBetweenPlayers(new PlayerTransferCommand(
                newRequestId("terminal-transfer"),
                fromAccount.getAccountId(),
                toAccount.getAccountId(),
                context.sourceServerId,
                fromPlayer.getCommandSenderName(),
                fromPlayer.getUniqueID().toString(),
                amount,
                normalizedComment,
                businessRef,
                "{\"source\":\"terminal_gui\"}"));

            return ActionResult.success(
                "转账成功: 向 "
                    + targetIdentity.displayName
                    + " 支付 "
                    + formatAmount(amount)
                    + " STARCOIN | 你的余额 "
                    + formatAmount(findAccountBalance(result, fromAccount.getAccountId())));
        } catch (BankingException exception) {
            GalaxyBase.LOG.warn(
                "Terminal bank transfer action rejected: fromPlayer={}, target={}, amount={}, reason={}",
                fromPlayer.getCommandSenderName(),
                normalizedTarget,
                Long.valueOf(amount),
                exception.getMessage(),
                exception);
            return ActionResult.error(toClientSafeMessage(exception));
        } catch (RuntimeException exception) {
            GalaxyBase.LOG.warn("Terminal bank transfer action failed", exception);
            return ActionResult.error("转账失败，请查看服务端日志");
        }
    }

    private BankingContext resolveContext() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && !server.isDedicatedServer()) {
            return BankingContext.unavailable("银行功能仅在独立服务端启用");
        }

        if (GalaxyBase.proxy == null) {
            return BankingContext.unavailable(describeInfrastructureUnavailable());
        }

        ModuleManager moduleManager = GalaxyBase.proxy.getModuleManager();
        if (moduleManager == null) {
            return BankingContext.unavailable(describeInfrastructureUnavailable());
        }

        InstitutionCoreModule institutionCoreModule = moduleManager.findModule(InstitutionCoreModule.class);
        if (institutionCoreModule == null) {
            return BankingContext.unavailable(describeInfrastructureUnavailable());
        }

        BankingInfrastructure bankingInfrastructure = institutionCoreModule.getBankingInfrastructure();
        if (bankingInfrastructure == null) {
            return BankingContext.unavailable(describeInfrastructureUnavailable());
        }

        return new BankingContext(
            bankingInfrastructure.getBankingApplicationService(),
            safeSourceServerId(institutionCoreModule.getBankingSourceServerId()),
            null);
    }

    private EntityPlayerMP requireServerPlayer(EntityPlayer player) {
        return player instanceof EntityPlayerMP ? (EntityPlayerMP) player : null;
    }

    private EntityPlayerMP resolveOnlinePlayer(String name) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return null;
        }
        return server.getConfigurationManager().func_152612_a(name);
    }

    private ResolvedPlayerIdentity resolveTargetIdentity(String requestedName, EntityPlayerMP onlinePlayer) {
        if (onlinePlayer != null) {
            return new ResolvedPlayerIdentity(
                onlinePlayer.getUniqueID().toString(),
                onlinePlayer.getCommandSenderName());
        }

        GameProfile cachedProfile = resolveCachedProfile(requestedName);
        if (cachedProfile == null || cachedProfile.getId() == null) {
            return null;
        }
        return new ResolvedPlayerIdentity(
            cachedProfile.getId().toString(),
            isBlank(cachedProfile.getName()) ? requestedName : cachedProfile.getName());
    }

    private GameProfile resolveCachedProfile(String playerName) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.func_152358_ax() == null) {
            return null;
        }
        return server.func_152358_ax().func_152655_a(playerName);
    }

    private Optional<BankAccount> findExistingPlayerAccount(BankingApplicationService bankingService, String playerRef) {
        return bankingService.findAccount(
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            playerRef,
            BankingConstants.DEFAULT_CURRENCY_CODE);
    }

    private BankAccount ensurePlayerAccount(BankingApplicationService bankingService, EntityPlayerMP player) {
        return bankingService.openAccount(new OpenAccountCommand(
            null,
            BankAccountType.PLAYER,
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            player.getUniqueID().toString(),
            BankingConstants.DEFAULT_CURRENCY_CODE,
            player.getCommandSenderName(),
            "{\"kind\":\"player\",\"source\":\"terminal_gui\"}"));
    }

    private long findAccountBalance(BankPostingResult result, long accountId) {
        if (result == null || result.getAffectedAccounts() == null) {
            return 0L;
        }
        for (BankAccount account : result.getAffectedAccounts()) {
            if (account.getAccountId() == accountId) {
                return account.getAvailableBalance();
            }
        }
        return 0L;
    }

    private String normalizeTargetName(String targetPlayerName) {
        if (targetPlayerName == null) {
            return null;
        }
        String normalized = targetPlayerName.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return "terminal transfer";
        }
        String normalized = comment.trim();
        if (normalized.isEmpty()) {
            return "terminal transfer";
        }
        return normalized.length() > 96 ? normalized.substring(0, 96) : normalized;
    }

    private String safeSourceServerId(String sourceServerId) {
        return sourceServerId == null || sourceServerId.trim().isEmpty() ? "unknown-server" : sourceServerId.trim();
    }

    private String newRequestId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString();
    }

    private String buildPlayerTransferBusinessRef(long fromAccountId, long toAccountId) {
        return "p2p:" + fromAccountId + ":" + toAccountId;
    }

    private String formatAmount(long amount) {
        return String.format("%,d", Long.valueOf(amount));
    }

    private String toClientSafeMessage(BankingException exception) {
        String message = exception == null ? null : exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "银行服务暂时不可用";
        }

        String normalized = message.trim();
        if (normalized.startsWith("database access failed") || normalized.startsWith("PostgreSQL ")) {
            return "银行服务暂时不可用: 数据库访问失败";
        }
        if ("insufficient available balance".equals(normalized)) {
            return "余额不足";
        }
        if (normalized.startsWith("account is not active")) {
            return "账户当前不可用";
        }
        if ("currency mismatch between accounts".equals(normalized)) {
            return "账户币种不一致";
        }
        if (normalized.startsWith("account not found")) {
            return "账户不存在或尚未完成初始化";
        }
        if (normalized.startsWith("Required banking table is missing")) {
            return "银行服务暂未完成初始化";
        }
        return "银行操作未完成: " + normalized;
    }

    static String describeInfrastructureUnavailable() {
        if (GalaxyBase.proxy == null) {
            return "当前世界未启用 PostgreSQL 银行基础设施";
        }

        ModConfiguration configuration = GalaxyBase.proxy.getConfiguration();
        ModuleManager moduleManager = GalaxyBase.proxy.getModuleManager();
        InstitutionCoreModule institutionCoreModule = moduleManager == null ? null
            : moduleManager.findModule(InstitutionCoreModule.class);
        return describeInfrastructureUnavailable(configuration, institutionCoreModule);
    }

    private static String describeInfrastructureUnavailable(ModConfiguration configuration,
        InstitutionCoreModule institutionCoreModule) {
        if (configuration == null) {
            return "当前世界未启用 PostgreSQL 银行基础设施";
        }
        if (!configuration.isBankingPostgresEnabled()) {
            return "服务端 jsirgalaxybase-server.cfg 已关闭 bankingPostgresEnabled";
        }
        if (isBlank(configuration.getBankingJdbcUrl()) || configuration.getBankingJdbcUrl().contains("db-host")) {
            return "服务端银行 JDBC 地址未配置完成";
        }
        if (isBlank(configuration.getBankingJdbcUsername())) {
            return "服务端银行 JDBC 用户名未配置";
        }
        if (isBlank(configuration.getBankingJdbcPassword())) {
            return "服务端银行 JDBC 密码未配置";
        }
        if (institutionCoreModule == null || institutionCoreModule.getBankingInfrastructure() == null) {
            return "银行 PostgreSQL 初始化失败，请查看服务端启动日志";
        }
        return "当前世界未启用 PostgreSQL 银行基础设施";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class ActionResult {

        private final boolean success;
        private final String message;
        private final TerminalNotificationSeverity severity;

        private ActionResult(boolean success, String message, TerminalNotificationSeverity severity) {
            this.success = success;
            this.message = message;
            this.severity = severity;
        }

        public static ActionResult success(String message) {
            return new ActionResult(true, EnumChatFormatting.GREEN + message, TerminalNotificationSeverity.SUCCESS);
        }

        public static ActionResult error(String message) {
            return new ActionResult(false, EnumChatFormatting.RED + message, TerminalNotificationSeverity.ERROR);
        }

        public static ActionResult info(String message) {
            return new ActionResult(true, EnumChatFormatting.GOLD + message, TerminalNotificationSeverity.WARNING);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public TerminalNotificationSeverity getSeverity() {
            return severity;
        }

        public TerminalActionFeedback toFeedback(long autoCloseMillis) {
            return TerminalActionFeedback.of(
                severity,
                severity.getDefaultTitle(),
                TerminalNotification.stripFormatting(message),
                autoCloseMillis);
        }
    }

    private static final class BankingContext {

        private final BankingApplicationService bankingService;
        private final String sourceServerId;
        private final String unavailableMessage;

        private BankingContext(BankingApplicationService bankingService, String sourceServerId, String unavailableMessage) {
            this.bankingService = bankingService;
            this.sourceServerId = sourceServerId;
            this.unavailableMessage = unavailableMessage;
        }

        private static BankingContext unavailable(String unavailableMessage) {
            return new BankingContext(null, "unknown-server", unavailableMessage);
        }

        private boolean isReady() {
            return bankingService != null;
        }

        private String getUnavailableMessage() {
            return unavailableMessage == null || unavailableMessage.trim().isEmpty() ? "银行服务暂时不可用" : unavailableMessage;
        }
    }

    private static final class ResolvedPlayerIdentity {

        private final String playerRef;
        private final String displayName;

        private ResolvedPlayerIdentity(String playerRef, String displayName) {
            this.playerRef = playerRef;
            this.displayName = displayName;
        }
    }
}