package com.jsirgalaxybase.modules.core.market.application;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketExecutionRequest;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketExecutionResult;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketQuoteRequest;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketQuoteResult;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinDescriptor;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinExchangeQuote;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;

public class TaskCoinExchangeService {

    private final BankingApplicationService bankingService;
    private final ExchangeMarketService exchangeMarketService;
    private final String sourceServerId;

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
                    return new MarketExchangeException("当前 Base Vault 资产不属于汇率市场支持的任务书硬币资产对");
                }
            });
        if ("TASK_COIN_ASSET_UNSUPPORTED".equals(formalQuote.getLimitPolicy().getReasonCode())) {
            throw new MarketExchangeException("当前 Base Vault 资产不属于汇率市场支持的任务书硬币资产对");
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

}
