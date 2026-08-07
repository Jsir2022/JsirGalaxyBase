package com.jsirgalaxybase.modules.core.market.application;

import java.util.Optional;
import java.time.Instant;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketExecutionRequest;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketExecutionResult;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketQuoteRequest;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketQuoteResult;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinDescriptor;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinExchangeQuote;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationType;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;

public class TaskCoinExchangeService {

    private final BankingApplicationService bankingService;
    private final ExchangeMarketService exchangeMarketService;
    private final String sourceServerId;
    private final MarketOperationLogRepository operationLogRepository;

    public TaskCoinExchangeService(BankingInfrastructure bankingInfrastructure, String sourceServerId) {
        this(bankingInfrastructure, new TaskCoinExchangePlanner(), sourceServerId);
    }

    public TaskCoinExchangeService(BankingInfrastructure bankingInfrastructure, TaskCoinExchangePlanner planner,
        String sourceServerId) {
        this(bankingInfrastructure, planner, sourceServerId, null);
    }

    public TaskCoinExchangeService(BankingInfrastructure bankingInfrastructure, TaskCoinExchangePlanner planner,
        String sourceServerId, MarketOperationLogRepository operationLogRepository) {
        this.bankingService = bankingInfrastructure.getBankingApplicationService();
        this.exchangeMarketService = new ExchangeMarketService(bankingInfrastructure, planner, sourceServerId);
        this.sourceServerId = sourceServerId == null || sourceServerId.trim().isEmpty() ? "unknown-server"
            : sourceServerId.trim();
        this.operationLogRepository = operationLogRepository;
    }

    public TaskCoinExchangeQuote previewHeldCoin(EntityPlayerMP player) {
        return previewHeldCoinFormal(player).getLegacyQuote();
    }

    public PreviewResult previewHeldCoinFormal(EntityPlayerMP player) {
        HeldCoinQuote quote = resolveHeldQuote(player);
        return quote.previewResult;
    }

    public TaskCoinExchangeExecutionResult exchangeHeldCoin(EntityPlayerMP player) {
        return exchangeHeldCoinFormal(player).toLegacyResult();
    }

    public ExchangeMarketExecutionCompatibilityResult exchangeHeldCoinFormal(EntityPlayerMP player) {
        HeldCoinSelection selection = resolveExecutableHeldSelection(player);
        Optional<BankAccount> playerAccount = bankingService.findAccount(BankingConstants.OWNER_TYPE_PLAYER_UUID,
            player.getUniqueID().toString(), BankingConstants.DEFAULT_CURRENCY_CODE);
        if (!playerAccount.isPresent()) {
            throw new MarketExchangeException("请先开户后再兑换任务书硬币");
        }

        String requestId = newRequestId();
        MarketOperationLog operation = createExchangeOperation(requestId, player, selection);
        ItemStack snapshot = selection.stack.copy();
        player.inventory.setInventorySlotContents(selection.slotIndex, null);
        player.inventory.markDirty();
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }

        try {
            ExchangeMarketExecutionResult result = exchangeMarketService.executeTaskCoinToStarcoin(
                new ExchangeMarketExecutionRequest(requestId, player.getUniqueID().toString(), sourceServerId,
                    player.getCommandSenderName(), selection.stackRegistryName, selection.stack.stackSize));
            completeExchangeOperation(operation, result);
            return new ExchangeMarketExecutionCompatibilityResult(result, selection.legacyQuote);
        } catch (RuntimeException exception) {
            player.inventory.setInventorySlotContents(selection.slotIndex, snapshot);
            player.inventory.markDirty();
            if (player.openContainer != null) {
                player.openContainer.detectAndSendChanges();
            }
            failExchangeOperation(operation, exception);
            throw exception;
        }
    }

    /**
     * Vault-facing exchange entry point. The caller has already removed the
     * exact item through an audited Base Vault transfer; this service only owns
     * quote validation and bank settlement, never a Minecraft inventory.
     */
    public PreviewResult previewVaultCoinFormal(String playerRef, ItemStack stack) {
        ItemStack source = requireCoinStack(stack);
        return previewRegistryQuote(playerRef, resolveRegistryName(source.getItem()), source.stackSize);
    }

    public ExchangeMarketExecutionCompatibilityResult exchangeVaultCoinFormal(String requestId, String playerRef,
        ItemStack stack) {
        ItemStack source = requireCoinStack(stack);
        PreviewResult quote = requireExecutableRegistryQuote(playerRef, resolveRegistryName(source.getItem()),
            source.stackSize);
        ExchangeMarketExecutionResult result = exchangeMarketService.executeTaskCoinToStarcoin(
            new ExchangeMarketExecutionRequest(requireText(requestId), playerRef, sourceServerId,
                "base-vault", resolveRegistryName(source.getItem()), source.stackSize));
        return new ExchangeMarketExecutionCompatibilityResult(result, quote.getLegacyQuote());
    }

    PreviewResult previewRegistryQuote(String playerRef, String registryName, int quantity) {
        ExchangeMarketQuoteRequest request = new ExchangeMarketQuoteRequest(newRequestId(), playerRef, sourceServerId,
            registryName, quantity);
        ExchangeMarketQuoteResult formalQuote = exchangeMarketService.quoteTaskCoinToStarcoin(request)
            .orElseThrow(new java.util.function.Supplier<MarketExchangeException>() {

                @Override
                public MarketExchangeException get() {
                    return new MarketExchangeException("当前手持物品不属于汇率市场支持的任务书硬币资产对");
                }
            });
        if ("TASK_COIN_ASSET_UNSUPPORTED".equals(formalQuote.getLimitPolicy().getReasonCode())) {
            throw new MarketExchangeException("当前手持物品不属于汇率市场支持的任务书硬币资产对");
        }
        return new PreviewResult(formalQuote, toLegacyQuote(formalQuote));
    }

    PreviewResult requireExecutableRegistryQuote(String playerRef, String registryName, int quantity) {
        PreviewResult previewResult = previewRegistryQuote(playerRef, registryName, quantity);
        if (!previewResult.getFormalQuote().getLimitPolicy().isExecutable()) {
            throw new MarketExchangeException(previewResult.getFormalQuote().getLimitPolicy().getNote());
        }
        return previewResult;
    }

    private HeldCoinQuote resolveHeldQuote(EntityPlayerMP player) {
        if (player == null) {
            throw new MarketExchangeException("当前上下文不能直接提交市场兑换请求");
        }

        int currentSlot = player.inventory.currentItem;
        ItemStack heldStack = player.inventory.getCurrentItem();
        if (heldStack == null || heldStack.getItem() == null || heldStack.stackSize <= 0) {
            throw new MarketExchangeException("请先把任务书硬币拿在手上，再执行兑换");
        }

        String registryName = resolveRegistryName(heldStack.getItem());
        PreviewResult previewResult = previewRegistryQuote(player.getUniqueID().toString(), registryName,
            heldStack.stackSize);
        return new HeldCoinQuote(currentSlot, heldStack, registryName, previewResult);
    }

    private HeldCoinSelection resolveExecutableHeldSelection(EntityPlayerMP player) {
        HeldCoinQuote quote = resolveHeldQuote(player);
        if (!quote.previewResult.getFormalQuote().getLimitPolicy().isExecutable()) {
            throw new MarketExchangeException(quote.previewResult.getFormalQuote().getLimitPolicy().getNote());
        }
        return new HeldCoinSelection(quote.slotIndex, quote.stack, quote.stackRegistryName,
            quote.previewResult.getLegacyQuote());
    }

    private TaskCoinExchangeQuote toLegacyQuote(ExchangeMarketQuoteResult formalQuote) {
        return new TaskCoinExchangeQuote(new TaskCoinDescriptor(formalQuote.getInputRegistryName(),
            formalQuote.getInputFamily(), formalQuote.getInputTier(), formalQuote.getInputUnitFaceValue()),
            (int) formalQuote.getInputQuantity(), formalQuote.getInputTotalFaceValue(),
            formalQuote.getEffectiveExchangeValue(), formalQuote.getContributionValue(),
            formalQuote.getRuleVersion().getRuleKey());
    }

    private String resolveRegistryName(Item item) {
        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(item);
        if (identifier != null) {
            return identifier.modId + ":" + identifier.name;
        }
        Object fallback = GameData.getItemRegistry().getNameForObject(item);
        return fallback == null ? "" : String.valueOf(fallback);
    }

    private ItemStack requireCoinStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            throw new MarketExchangeException("请选择 Base Vault 中可兑换的任务书硬币");
        }
        return stack.copy();
    }

    private String requireText(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new MarketExchangeException("exchange request id is required");
        }
        return value.trim();
    }

    private String newRequestId() {
        return exchangeMarketService.newRequestId();
    }

    private MarketOperationLog createExchangeOperation(String requestId, EntityPlayerMP player,
        HeldCoinSelection selection) {
        if (operationLogRepository == null) {
            return null;
        }
        Instant now = Instant.now();
        String playerRef = player.getUniqueID().toString();
        MarketRecoveryMetadata metadata = MarketRecoveryMetadata.builder()
            .put("mode", "exchange-execution")
            .put("inputRegistryName", selection.stackRegistryName)
            .putLong("inputMeta", selection.stack.getItemDamage())
            .putLong("inputQuantity", selection.stack.stackSize)
            .putLong("inventorySlot", selection.slotIndex)
            .put("physicalInputState", "PENDING_REMOVAL")
            .build();
        MarketOperationLog created = operationLogRepository.save(new MarketOperationLog(0L, requestId,
            MarketOperationType.EXCHANGE_EXECUTION, MarketOperationStatus.CREATED, sourceServerId, playerRef,
            "exchange-execution|registry=" + selection.stackRegistryName + "|meta=" + selection.stack.getItemDamage()
                + "|quantity=" + selection.stack.stackSize,
            metadata.toKey(), 0L, 0L, 0L, "exchange input accepted; physical removal is pending", now, now));
        return operationLogRepository.update(created.withState(MarketOperationStatus.PROCESSING, 0L, 0L, 0L,
            "exchange input removed; waiting for idempotent bank settlement", metadata.toKey(), Instant.now()));
    }

    private void completeExchangeOperation(MarketOperationLog operation, ExchangeMarketExecutionResult result) {
        if (operation == null || operationLogRepository == null) {
            return;
        }
        long transactionId = result.getPostingResult().getTransaction().getTransactionId();
        MarketRecoveryMetadata metadata = MarketRecoveryMetadata.parse(operation.getRecoveryMetadataKey()).toBuilder()
            .put("physicalInputState", "REMOVED")
            .putLong("bankTransactionId", transactionId)
            .putLong("exchangeRecordId", result.getPostingResult().getExchangeRecord() == null ? 0L
                : result.getPostingResult().getExchangeRecord().getExchangeId())
            .build();
        operationLogRepository.update(operation.withState(MarketOperationStatus.COMPLETED, 0L, 0L, transactionId,
            "exchange input removed and formal bank settlement completed", metadata.toKey(), Instant.now()));
    }

    private void failExchangeOperation(MarketOperationLog operation, RuntimeException exception) {
        if (operation == null || operationLogRepository == null) {
            return;
        }
        try {
            MarketRecoveryMetadata metadata = MarketRecoveryMetadata.parse(operation.getRecoveryMetadataKey()).toBuilder()
                .put("physicalInputState", "RESTORED_AFTER_FAILURE")
                .build();
            operationLogRepository.update(operation.withState(MarketOperationStatus.FAILED, 0L, 0L, 0L,
                "exchange settlement failed; held input restored: " + safeMessage(exception), metadata.toKey(),
                Instant.now()));
        } catch (RuntimeException ignored) {
            // Preserve the original settlement failure; startup recovery will surface the incomplete operation.
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception == null ? null : exception.getMessage();
        return message == null || message.trim().isEmpty() ? exception == null ? "unknown" : exception.getClass().getSimpleName()
            : message.trim();
    }

    public static final class TaskCoinExchangeExecutionResult {

        private final TaskCoinExchangeQuote quote;
        private final BankPostingResult postingResult;

        public TaskCoinExchangeExecutionResult(TaskCoinExchangeQuote quote, BankPostingResult postingResult) {
            this.quote = quote;
            this.postingResult = postingResult;
        }

        public TaskCoinExchangeQuote getQuote() {
            return quote;
        }

        public BankPostingResult getPostingResult() {
            return postingResult;
        }
    }

    public static final class PreviewResult {

        private final ExchangeMarketQuoteResult formalQuote;
        private final TaskCoinExchangeQuote legacyQuote;

        public PreviewResult(ExchangeMarketQuoteResult formalQuote, TaskCoinExchangeQuote legacyQuote) {
            this.formalQuote = formalQuote;
            this.legacyQuote = legacyQuote;
        }

        public ExchangeMarketQuoteResult getFormalQuote() {
            return formalQuote;
        }

        public TaskCoinExchangeQuote getLegacyQuote() {
            return legacyQuote;
        }
    }

    public static final class ExchangeMarketExecutionCompatibilityResult {

        private final ExchangeMarketExecutionResult formalResult;
        private final TaskCoinExchangeQuote legacyQuote;

        public ExchangeMarketExecutionCompatibilityResult(ExchangeMarketExecutionResult formalResult,
            TaskCoinExchangeQuote legacyQuote) {
            this.formalResult = formalResult;
            this.legacyQuote = legacyQuote;
        }

        public ExchangeMarketExecutionResult getFormalResult() {
            return formalResult;
        }

        public TaskCoinExchangeExecutionResult toLegacyResult() {
            return new TaskCoinExchangeExecutionResult(legacyQuote, formalResult.getPostingResult());
        }
    }

    private static final class HeldCoinSelection {

        private final int slotIndex;
        private final ItemStack stack;
        private final String stackRegistryName;
        private final TaskCoinExchangeQuote legacyQuote;

        private HeldCoinSelection(int slotIndex, ItemStack stack, String stackRegistryName,
            TaskCoinExchangeQuote legacyQuote) {
            this.slotIndex = slotIndex;
            this.stack = stack;
            this.stackRegistryName = stackRegistryName;
            this.legacyQuote = legacyQuote;
        }
    }

    private static final class HeldCoinQuote {

        private final int slotIndex;
        private final ItemStack stack;
        private final String stackRegistryName;
        private final PreviewResult previewResult;

        private HeldCoinQuote(int slotIndex, ItemStack stack, String stackRegistryName, PreviewResult previewResult) {
            this.slotIndex = slotIndex;
            this.stack = stack;
            this.stackRegistryName = stackRegistryName;
            this.previewResult = previewResult;
        }
    }
}
