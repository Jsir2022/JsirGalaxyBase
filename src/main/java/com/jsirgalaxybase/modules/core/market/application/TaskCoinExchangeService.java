package com.jsirgalaxybase.modules.core.market.application;

import java.util.Optional;

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
        this.bankingService = bankingInfrastructure.getBankingApplicationService();
        this.exchangeMarketService = new ExchangeMarketService(bankingInfrastructure, planner, sourceServerId);
        this.sourceServerId = sourceServerId == null || sourceServerId.trim().isEmpty() ? "unknown-server"
            : sourceServerId.trim();
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

        ItemStack snapshot = selection.stack.copy();
        player.inventory.setInventorySlotContents(selection.slotIndex, null);
        player.inventory.markDirty();
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }

        try {
            ExchangeMarketExecutionResult result = exchangeMarketService.executeTaskCoinToStarcoin(
                new ExchangeMarketExecutionRequest(newRequestId(), player.getUniqueID().toString(), sourceServerId,
                    player.getCommandSenderName(), selection.stackRegistryName, selection.stack.stackSize));
            return new ExchangeMarketExecutionCompatibilityResult(result, selection.legacyQuote);
        } catch (RuntimeException exception) {
            player.inventory.setInventorySlotContents(selection.slotIndex, snapshot);
            player.inventory.markDirty();
            if (player.openContainer != null) {
                player.openContainer.detectAndSendChanges();
            }
            throw exception;
        }
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