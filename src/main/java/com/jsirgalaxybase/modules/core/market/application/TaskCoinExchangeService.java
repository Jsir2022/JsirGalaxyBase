package com.jsirgalaxybase.modules.core.market.application;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.application.command.CoinExchangeSettlementCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinExchangeQuote;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;

public class TaskCoinExchangeService {

    private final BankingInfrastructure bankingInfrastructure;
    private final BankingApplicationService bankingService;
    private final TaskCoinExchangePlanner planner;
    private final String sourceServerId;

    public TaskCoinExchangeService(BankingInfrastructure bankingInfrastructure, String sourceServerId) {
        this(bankingInfrastructure, new TaskCoinExchangePlanner(), sourceServerId);
    }

    public TaskCoinExchangeService(BankingInfrastructure bankingInfrastructure, TaskCoinExchangePlanner planner,
        String sourceServerId) {
        this.bankingInfrastructure = bankingInfrastructure;
        this.bankingService = bankingInfrastructure.getBankingApplicationService();
        this.planner = planner;
        this.sourceServerId = sourceServerId == null || sourceServerId.trim().isEmpty() ? "unknown-server"
            : sourceServerId.trim();
    }

    public TaskCoinExchangeQuote previewHeldCoin(EntityPlayerMP player) {
        HeldCoinSelection selection = resolveHeldSelection(player);
        return selection.quote;
    }

    public TaskCoinExchangeExecutionResult exchangeHeldCoin(EntityPlayerMP player) {
        HeldCoinSelection selection = resolveHeldSelection(player);
        Optional<BankAccount> playerAccount = bankingService.findAccount(BankingConstants.OWNER_TYPE_PLAYER_UUID,
            player.getUniqueID().toString(), BankingConstants.DEFAULT_CURRENCY_CODE);
        if (!playerAccount.isPresent()) {
            throw new MarketExchangeException("请先开户后再兑换任务书硬币");
        }

        BankAccount reserveAccount = ManagedBankAccounts.ensureManagedAccount(bankingInfrastructure,
            ManagedBankAccounts.EXCHANGE_RESERVE_ACCOUNT);
        if (reserveAccount.getAvailableBalance() < selection.quote.getEffectiveExchangeValue()) {
            throw new MarketExchangeException("兑换储备余额不足，请稍后再试");
        }

        ItemStack snapshot = selection.stack.copy();
        player.inventory.setInventorySlotContents(selection.slotIndex, null);
        player.inventory.markDirty();
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }

        try {
            BankPostingResult result = bankingService.settleCoinExchange(new CoinExchangeSettlementCommand(
                newRequestId(),
                reserveAccount.getAccountId(),
                playerAccount.get().getAccountId(),
                player.getUniqueID().toString(),
                sourceServerId,
                player.getCommandSenderName(),
                selection.quote.getDescriptor().getFamily(),
                selection.quote.getDescriptor().getTier(),
                selection.quote.getDescriptor().getFaceValue(),
                selection.quote.getQuantity(),
                selection.quote.getEffectiveExchangeValue(),
                selection.quote.getContributionValue(),
                selection.quote.getExchangeRuleVersion(),
                buildBusinessRef(player.getUniqueID().toString(), selection.quote),
                "market phase1 held coin exchange",
                buildExtraJson(selection.quote)));
            return new TaskCoinExchangeExecutionResult(selection.quote, result);
        } catch (RuntimeException exception) {
            player.inventory.setInventorySlotContents(selection.slotIndex, snapshot);
            player.inventory.markDirty();
            if (player.openContainer != null) {
                player.openContainer.detectAndSendChanges();
            }
            throw exception;
        }
    }

    private HeldCoinSelection resolveHeldSelection(EntityPlayerMP player) {
        if (player == null) {
            throw new MarketExchangeException("当前上下文不能直接提交市场兑换请求");
        }

        int currentSlot = player.inventory.currentItem;
        ItemStack heldStack = player.inventory.getCurrentItem();
        if (heldStack == null || heldStack.getItem() == null || heldStack.stackSize <= 0) {
            throw new MarketExchangeException("请先把任务书硬币拿在手上，再执行兑换");
        }

        String registryName = resolveRegistryName(heldStack.getItem());
        TaskCoinExchangeQuote quote = planner.quote(registryName, heldStack.stackSize)
            .orElseThrow(new java.util.function.Supplier<MarketExchangeException>() {

                @Override
                public MarketExchangeException get() {
                    return new MarketExchangeException("当前手持物品不是可兑换的任务书硬币");
                }
            });
        return new HeldCoinSelection(currentSlot, heldStack, quote);
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
        return "market-task-coin-" + UUID.randomUUID().toString();
    }

    private String buildBusinessRef(String playerRef, TaskCoinExchangeQuote quote) {
        return "market:task-coin:" + playerRef + ":" + quote.getDescriptor().getBusinessKey();
    }

    private String buildExtraJson(TaskCoinExchangeQuote quote) {
        return "{\"source\":\"market_command\",\"exchange_mode\":\"held_stack\",\"source_blind\":true,\"registry_name\":\""
            + quote.getDescriptor().getRegistryName() + "\"}";
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

    private static final class HeldCoinSelection {

        private final int slotIndex;
        private final ItemStack stack;
        private final TaskCoinExchangeQuote quote;

        private HeldCoinSelection(int slotIndex, ItemStack stack, TaskCoinExchangeQuote quote) {
            this.slotIndex = slotIndex;
            this.stack = stack;
            this.quote = quote;
        }
    }
}