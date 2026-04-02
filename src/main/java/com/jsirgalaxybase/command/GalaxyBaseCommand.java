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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.application.MarketExchangeException;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketProductParser;
import com.jsirgalaxybase.modules.core.market.application.TaskCoinExchangeService;
import com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketService;
import com.jsirgalaxybase.modules.core.market.application.command.CancelBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CancelSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimMarketAssetCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinExchangeQuote;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;

public class GalaxyBaseCommand extends CommandBase {

    private final ModuleManager moduleManager;
    private final StandardizedMarketProductParser standardizedProductParser = new StandardizedMarketProductParser();

    public GalaxyBaseCommand(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public String getCommandName() {
        return "jsirgalaxybase";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/jsirgalaxybase [modules|architecture|bank|market]";
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
        if (args.length > 0 && "market".equalsIgnoreCase(args[0])) {
            processMarketCommand(sender, args);
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
            return getListOfStringsMatchingLastWord(args, new String[] { "modules", "architecture", "bank", "market" });
        }
        if (args.length == 2 && "bank".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args,
                new String[] { "help", "open", "balance", "ledger", "system", "public", "tx", "init",
                    "grant", "deduct", "transfer" });
        }
        if (args.length == 2 && "market".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args,
                new String[] { "help", "quote", "exchange", "sell", "buy", "book", "claims", "claim",
                    "recover" });
        }
        if (args.length >= 3 && "bank".equalsIgnoreCase(args[0])) {
            String action = args[1].toLowerCase();
            if ("open".equals(action) || "balance".equals(action) || "grant".equals(action)
                || "deduct".equals(action)
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
                return getListOfStringsMatchingLastWord(args, new String[] { "ledger", "all", "ops", "exchange", "tax" });
            }
            if ("public".equals(action) && args.length == 4 && "ledger".equalsIgnoreCase(args[2])) {
                return getListOfStringsMatchingLastWord(args, new String[] { "ops", "exchange", "tax" });
            }
            if ("init".equals(action) && args.length == 3) {
                return getListOfStringsMatchingLastWord(args, new String[] { "system" });
            }
        }
        if (args.length == 3 && "market".equalsIgnoreCase(args[0])) {
            String action = args[1].toLowerCase();
            if ("quote".equals(action) || "exchange".equals(action)) {
                return getListOfStringsMatchingLastWord(args, new String[] { "hand" });
            }
            if ("sell".equals(action) || "buy".equals(action)) {
                return getListOfStringsMatchingLastWord(args, new String[] { "create", "cancel" });
            }
        }
        if (args.length == 4 && "market".equalsIgnoreCase(args[0])) {
            if ("sell".equalsIgnoreCase(args[1]) && "create".equalsIgnoreCase(args[2])) {
                return getListOfStringsMatchingLastWord(args, new String[] { "hand" });
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
            if ("deduct".equals(action)) {
                handleBankDeduct(sender, args, institutionCoreModule, bankingInfrastructure, bankingService);
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

    private void processMarketCommand(ICommandSender sender, String[] args) {
        ReplySink reply = new LiveReplySink(sender);
        if (args.length == 1 || "help".equalsIgnoreCase(args[1])) {
            sendMarketUsage(reply);
            return;
        }

        InstitutionCoreModule institutionCoreModule = moduleManager.findModule(InstitutionCoreModule.class);
        String action = args[1].toLowerCase();

        try {
            if ("recover".equals(action)) {
                processMarketRecoveryCommand(reply, args, institutionCoreModule,
                    sender.canCommandSenderUseCommand(2, getCommandName()));
                return;
            }

            if ("quote".equals(action)) {
                if (!(sender instanceof EntityPlayerMP)) {
                    reply.send("市场一期任务书硬币指令只能由在线玩家执行。");
                    return;
                }
                if (institutionCoreModule == null || institutionCoreModule.getBankingInfrastructure() == null) {
                    reply.send("市场一期任务书硬币兑换未就绪，因为银行基础设施尚未完成启动。");
                    return;
                }
                EntityPlayerMP player = (EntityPlayerMP) sender;
                TaskCoinExchangeService exchangeService = createTaskCoinExchangeService(institutionCoreModule);
                handleMarketQuote(player, args, exchangeService);
                return;
            }
            if ("exchange".equals(action)) {
                if (!(sender instanceof EntityPlayerMP)) {
                    reply.send("市场一期任务书硬币指令只能由在线玩家执行。");
                    return;
                }
                if (institutionCoreModule == null || institutionCoreModule.getBankingInfrastructure() == null) {
                    reply.send("市场一期任务书硬币兑换未就绪，因为银行基础设施尚未完成启动。");
                    return;
                }
                EntityPlayerMP player = (EntityPlayerMP) sender;
                TaskCoinExchangeService exchangeService = createTaskCoinExchangeService(institutionCoreModule);
                handleMarketExchange(player, args, exchangeService);
                return;
            }

            if (!(sender instanceof EntityPlayerMP)) {
                reply.send("第二层标准化现货指令只能由在线玩家执行。管理员恢复可用 /jsirgalaxybase market recover。");
                return;
            }
            if (institutionCoreModule == null || institutionCoreModule.getStandardizedSpotMarketService() == null) {
                reply.send("第二层标准化现货市场未就绪，请检查独立服 PostgreSQL 市场启动日志。");
                return;
            }

            processStandardizedMarketCommand(new LiveStandardizedMarketPlayerContext((EntityPlayerMP) sender), reply,
                args, institutionCoreModule);
        } catch (BankingException e) {
            reply.send("Banking error: " + e.getMessage());
        } catch (MarketExchangeException e) {
            reply.send("Market exchange rejected: " + e.getMessage());
        } catch (MarketOperationException e) {
            reply.send("Market operation rejected: " + e.getMessage());
        }
    }

    protected TaskCoinExchangeService createTaskCoinExchangeService(InstitutionCoreModule institutionCoreModule) {
        return new TaskCoinExchangeService(institutionCoreModule.getBankingInfrastructure(),
            institutionCoreModule.getBankingSourceServerId());
    }

    void processMarketRecoveryCommand(ReplySink reply, String[] args, InstitutionCoreModule institutionCoreModule,
        boolean operator) {
        if (!operator) {
            reply.send("只有管理员才能执行市场恢复扫描。");
            return;
        }
        if (institutionCoreModule == null || institutionCoreModule.getMarketRecoveryService() == null) {
            reply.send("市场恢复服务未就绪，请先确认 institution core 已在独立服完成初始化。");
            return;
        }

        int limit = args.length >= 3 ? parsePositiveInt(args[2], "limit") : 20;
        List<MarketOperationLog> recovered = institutionCoreModule.scanMarketRecovery(Math.min(limit, 100));
        if (recovered.isEmpty()) {
            reply.send("市场恢复扫描完成，没有待处理的异常操作。");
            return;
        }

        reply.send("市场恢复扫描已处理 " + recovered.size() + " 条操作。");
        for (MarketOperationLog operation : recovered) {
            reply.send(
                "op=" + operation.getOperationId() + ", type=" + operation.getOperationType() + ", status="
                    + operation.getStatus() + ", orderId=" + operation.getRelatedOrderId() + ", custodyId="
                    + operation.getRelatedCustodyId() + ", message=" + safeText(operation.getMessage()));
        }
    }

    void processStandardizedMarketCommand(StandardizedMarketPlayerContext playerContext, ReplySink reply,
        String[] args, InstitutionCoreModule institutionCoreModule) {
        String action = args[1].toLowerCase();
        if ("sell".equals(action)) {
            handleStandardizedSellCommand(playerContext, reply, args, institutionCoreModule);
            return;
        }
        if ("buy".equals(action)) {
            handleStandardizedBuyCommand(playerContext, reply, args, institutionCoreModule);
            return;
        }
        if ("book".equals(action)) {
            handleStandardizedBookCommand(playerContext, reply, args, institutionCoreModule);
            return;
        }
        if ("claims".equals(action)) {
            handleStandardizedClaimsCommand(playerContext, reply, institutionCoreModule);
            return;
        }
        if ("claim".equals(action)) {
            handleStandardizedClaimCommand(playerContext, reply, args, institutionCoreModule);
            return;
        }
        sendMarketUsage(reply);
    }

    private void handleStandardizedSellCommand(StandardizedMarketPlayerContext playerContext, ReplySink reply,
        String[] args, InstitutionCoreModule institutionCoreModule) {
        if (args.length < 3) {
            reply.send("Usage: /jsirgalaxybase market sell create hand <unitPrice> [quantity]");
            reply.send("Usage: /jsirgalaxybase market sell cancel <orderId>");
            return;
        }

        String sellAction = args[2].toLowerCase();
        if ("create".equals(sellAction)) {
            if (args.length < 5 || !"hand".equalsIgnoreCase(args[3])) {
                reply.send("Usage: /jsirgalaxybase market sell create hand <unitPrice> [quantity]");
                return;
            }

            HeldStandardizedItem heldItem = resolveHeldStandardizedItem(playerContext);
            long unitPrice = parsePositiveLong(args[4], "unitPrice");
            long quantity = args.length >= 6 ? parsePositiveLong(args[5], "quantity") : heldItem.snapshot.stackSize;
            executeStandardizedSellCreate(playerContext, reply, institutionCoreModule, heldItem, unitPrice, quantity);
            return;
        }

        if ("cancel".equals(sellAction)) {
            if (args.length < 4) {
                reply.send("Usage: /jsirgalaxybase market sell cancel <orderId>");
                return;
            }
            long orderId = parsePositiveLong(args[3], "orderId");
            StandardizedSpotMarketService.CancelSellOrderResult result = institutionCoreModule
                .getStandardizedSpotMarketService().cancelSellOrder(new CancelSellOrderCommand(
                    newRequestId("market-sell-cancel"), playerContext.getPlayerRef(),
                    institutionCoreModule.getBankingSourceServerId(), orderId));
            reply.send("卖单已撤销: orderId=" + result.getOrder().getOrderId() + ", remainingClaimableCustodyId="
                + result.getCustody().getCustodyId() + ", quantity=" + result.getCustody().getQuantity());
            return;
        }

        sendMarketUsage(reply);
    }

    private void handleStandardizedBuyCommand(StandardizedMarketPlayerContext playerContext, ReplySink reply,
        String[] args, InstitutionCoreModule institutionCoreModule) {
        if (args.length < 3) {
            reply.send("Usage: /jsirgalaxybase market buy create <productKey> <quantity> <unitPrice>");
            reply.send("Usage: /jsirgalaxybase market buy cancel <orderId>");
            return;
        }

        String buyAction = args[2].toLowerCase();
        if ("create".equals(buyAction)) {
            if (args.length < 6) {
                reply.send("Usage: /jsirgalaxybase market buy create <productKey> <quantity> <unitPrice>");
                return;
            }
            String productKey = standardizedProductParser.parse(args[3]).getProductKey();
            long quantity = parsePositiveLong(args[4], "quantity");
            long unitPrice = parsePositiveLong(args[5], "unitPrice");
            boolean stackable = resolveStackability(productKey);
            executeStandardizedBuyCreate(playerContext, reply, institutionCoreModule, productKey, quantity, unitPrice,
                stackable);
            return;
        }

        if ("cancel".equals(buyAction)) {
            if (args.length < 4) {
                reply.send("Usage: /jsirgalaxybase market buy cancel <orderId>");
                return;
            }
            long orderId = parsePositiveLong(args[3], "orderId");
            StandardizedSpotMarketService.CancelBuyOrderResult result = institutionCoreModule
                .getStandardizedSpotMarketService().cancelBuyOrder(new CancelBuyOrderCommand(
                    newRequestId("market-buy-cancel"), playerContext.getPlayerRef(),
                    institutionCoreModule.getBankingSourceServerId(), orderId));
            reply.send("买单已撤销: orderId=" + result.getOrder().getOrderId() + ", releasedFunds="
                + result.getOrder().getReservedFunds());
            return;
        }

        sendMarketUsage(reply);
    }

    private void handleStandardizedBookCommand(StandardizedMarketPlayerContext playerContext, ReplySink reply,
        String[] args, InstitutionCoreModule institutionCoreModule) {
        if (args.length < 3) {
            reply.send("Usage: /jsirgalaxybase market book <productKey> [limit]");
            return;
        }
        String productKey = standardizedProductParser.parse(args[2]).getProductKey();
        int limit = args.length >= 4 ? Math.min(parsePositiveInt(args[3], "limit"), 20) : 10;
        List<MarketOrder> orders = institutionCoreModule.getStandardizedSpotMarketService().listOpenSellOrders(productKey);
        if (orders.isEmpty()) {
            reply.send("当前没有该标准化标的的开放卖单: " + productKey);
            return;
        }

        reply.send("开放卖单簿: product=" + productKey + ", showing=" + Math.min(limit, orders.size()) + "/"
            + orders.size());
        for (int i = 0; i < orders.size() && i < limit; i++) {
            MarketOrder order = orders.get(i);
            reply.send("orderId=" + order.getOrderId() + ", owner=" + order.getOwnerPlayerRef() + ", open="
                + order.getOpenQuantity() + ", price=" + order.getUnitPrice() + ", status=" + order.getStatus());
        }
    }

    private void handleStandardizedClaimsCommand(StandardizedMarketPlayerContext playerContext, ReplySink reply,
        InstitutionCoreModule institutionCoreModule) {
        List<MarketCustodyInventory> claimables = institutionCoreModule.getStandardizedSpotMarketService()
            .listClaimableAssets(playerContext.getPlayerRef());
        if (claimables.isEmpty()) {
            reply.send("当前没有可提取的 CLAIMABLE 资产。");
            return;
        }

        reply.send("可提取 CLAIMABLE 资产共 " + claimables.size() + " 条:");
        for (MarketCustodyInventory custody : claimables) {
            reply.send("custodyId=" + custody.getCustodyId() + ", product=" + custody.getProduct().getProductKey()
                + ", quantity=" + custody.getQuantity() + ", orderId=" + custody.getRelatedOrderId());
        }
    }

    private void handleStandardizedClaimCommand(StandardizedMarketPlayerContext playerContext, ReplySink reply,
        String[] args, InstitutionCoreModule institutionCoreModule) {
        if (args.length < 3) {
            reply.send("Usage: /jsirgalaxybase market claim <custodyId>");
            return;
        }
        long custodyId = parsePositiveLong(args[2], "custodyId");
        StandardizedSpotMarketService.ClaimMarketAssetResult result = institutionCoreModule
            .getStandardizedSpotMarketService().claimMarketAsset(new ClaimMarketAssetCommand(
                newRequestId("market-claim"), playerContext.getPlayerRef(),
                institutionCoreModule.getBankingSourceServerId(), custodyId));
        reply.send("CLAIMABLE 资产已提取: custodyId=" + result.getCustody().getCustodyId() + ", product="
            + result.getCustody().getProduct().getProductKey() + ", quantity=" + result.getCustody().getQuantity()
            + ", status=" + result.getCustody().getStatus());
    }

    private HeldStandardizedItem resolveHeldStandardizedItem(StandardizedMarketPlayerContext playerContext) {
        ItemStack heldStack = playerContext.getHeldItem();
        if (heldStack == null || heldStack.getItem() == null || heldStack.stackSize <= 0) {
            throw new MarketOperationException("请先把标准化物品拿在手上，再创建卖单");
        }
        String registryName = resolveRegistryName(heldStack.getItem());
        if (registryName == null || registryName.trim().isEmpty()) {
            throw new MarketOperationException("当前手持物品没有可用的注册名，不能进入标准化现货市场");
        }
        String productKey = registryName + ":" + heldStack.getItemDamage();
        standardizedProductParser.parse(productKey);
        return new HeldStandardizedItem(productKey, heldStack.getMaxStackSize() > 1, heldStack.copy());
    }

    void executeStandardizedSellCreate(StandardizedMarketPlayerContext playerContext, ReplySink reply,
        InstitutionCoreModule institutionCoreModule, HeldStandardizedItem heldItem, long unitPrice, long quantity) {
        if (quantity > heldItem.snapshot.stackSize) {
            throw new MarketOperationException("sell quantity exceeds held stack size");
        }

        applyHeldSellDeduction(playerContext, heldItem.snapshot, quantity);
        try {
            StandardizedSpotMarketService.CreateSellOrderResult result = institutionCoreModule
                .getStandardizedSpotMarketService().createSellOrder(new CreateSellOrderCommand(
                    newRequestId("market-sell-create"), playerContext.getPlayerRef(),
                    institutionCoreModule.getBankingSourceServerId(), heldItem.productKey, quantity,
                    heldItem.stackable, unitPrice));
            reply.send("卖单已创建: orderId=" + result.getOrder().getOrderId() + ", product="
                + result.getOrder().getProduct().getProductKey() + ", open=" + result.getOrder().getOpenQuantity()
                + ", price=" + result.getOrder().getUnitPrice() + ", custodyId="
                + result.getCustody().getCustodyId());
            if (!result.getTradeRecords().isEmpty()) {
                reply.send("本次创建已立即撮合 " + result.getTradeRecords().size() + " 笔成交，新增 claimable="
                    + result.getClaimableAssets().size());
            }
        } catch (RuntimeException exception) {
            restoreHeldItem(playerContext, heldItem.snapshot);
            throw exception;
        }
    }

    void executeStandardizedBuyCreate(StandardizedMarketPlayerContext playerContext, ReplySink reply,
        InstitutionCoreModule institutionCoreModule, String productKey, long quantity, long unitPrice,
        boolean stackable) {
        StandardizedSpotMarketService.CreateBuyOrderResult result = institutionCoreModule
            .getStandardizedSpotMarketService().createBuyOrder(new CreateBuyOrderCommand(
                newRequestId("market-buy-create"), playerContext.getPlayerRef(),
                institutionCoreModule.getBankingSourceServerId(), productKey, quantity, stackable, unitPrice));
        reply.send("买单已创建: orderId=" + result.getOrder().getOrderId() + ", product="
            + result.getOrder().getProduct().getProductKey() + ", open=" + result.getOrder().getOpenQuantity()
            + ", reservedFunds=" + result.getOrder().getReservedFunds());
        if (!result.getClaimableAssets().isEmpty()) {
            reply.send("本次创建已立即获得 claimable=" + result.getClaimableAssets().size());
        }
    }

    private void applyHeldSellDeduction(StandardizedMarketPlayerContext playerContext, ItemStack snapshot,
        long quantity) {
        ItemStack remaining = snapshot.copy();
        remaining.stackSize = snapshot.stackSize - (int) quantity;
        playerContext.setHeldItem(remaining.stackSize > 0 ? remaining : null);
        playerContext.syncInventory();
    }

    private void restoreHeldItem(StandardizedMarketPlayerContext playerContext, ItemStack snapshot) {
        playerContext.setHeldItem(snapshot.copy());
        playerContext.syncInventory();
    }

    private boolean resolveStackability(String productKey) {
        StandardizedMarketProduct product = standardizedProductParser.parse(productKey);
        Item item = resolveItem(product);
        return new ItemStack(item, 1, product.getMeta()).getMaxStackSize() > 1;
    }

    private Item resolveItem(StandardizedMarketProduct product) {
        String registryName = product.getRegistryName();
        int separatorIndex = registryName.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= registryName.length() - 1) {
            throw new MarketOperationException("invalid standardized product registry name");
        }
        Item item = GameRegistry.findItem(registryName.substring(0, separatorIndex),
            registryName.substring(separatorIndex + 1));
        if (item == null) {
            throw new MarketOperationException("standardized product item is not registered: " + product.getProductKey());
        }
        return item;
    }

    private String resolveRegistryName(Item item) {
        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(item);
        if (identifier != null) {
            return identifier.modId + ":" + identifier.name;
        }
        Object fallback = GameData.getItemRegistry().getNameForObject(item);
        return fallback == null ? "" : String.valueOf(fallback);
    }

    private long parsePositiveLong(String rawValue, String fieldName) {
        long value;
        try {
            value = Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            throw new MarketOperationException(fieldName + " must be a valid integer");
        }
        if (value <= 0L) {
            throw new MarketOperationException(fieldName + " must be positive");
        }
        return value;
    }

    private int parsePositiveInt(String rawValue, String fieldName) {
        int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            throw new MarketOperationException(fieldName + " must be a valid integer");
        }
        if (value <= 0) {
            throw new MarketOperationException(fieldName + " must be positive");
        }
        return value;
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
        handleBankAdjustment(sender, args, institutionCoreModule, bankingInfrastructure, bankingService, true);
    }

    private void handleBankDeduct(ICommandSender sender, String[] args, InstitutionCoreModule institutionCoreModule,
        BankingInfrastructure bankingInfrastructure, BankingApplicationService bankingService) {
        handleBankAdjustment(sender, args, institutionCoreModule, bankingInfrastructure, bankingService, false);
    }

    private void handleBankAdjustment(ICommandSender sender, String[] args, InstitutionCoreModule institutionCoreModule,
        BankingInfrastructure bankingInfrastructure, BankingApplicationService bankingService, boolean grant) {
        if (args.length < 4) {
            sender.addChatMessage(new ChatComponentText(
                "Usage: /jsirgalaxybase bank " + (grant ? "grant" : "deduct") + " <player> <amount> [comment]"));
            return;
        }

        EntityPlayerMP targetPlayer = getPlayer(sender, args[2]);
        long amount = Long.parseLong(args[3]);
        String comment = joinTail(args, 4);

        BankAccount systemAccount = ensureManagedAccount(bankingInfrastructure, ManagedBankAccounts.SYSTEM_OPERATIONS_ACCOUNT);
        Optional<BankAccount> existingTargetAccount = bankingService.findAccount(
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            targetPlayer.getUniqueID().toString(),
            BankingConstants.DEFAULT_CURRENCY_CODE);
        if (!grant && !existingTargetAccount.isPresent()) {
            sender.addChatMessage(new ChatComponentText(
                "No bank account exists yet for " + targetPlayer.getCommandSenderName() + "."));
            return;
        }

        BankAccount targetAccount = grant ? ensurePlayerAccount(bankingService, targetPlayer) : existingTargetAccount.get();
        BankPostingResult result = bankingService.postInternalTransfer(new InternalTransferCommand(
            newRequestId(grant ? "grant" : "deduct"),
            grant ? BankTransactionType.SYSTEM_GRANT : BankTransactionType.SYSTEM_DEDUCT,
            BankBusinessType.ADMIN_ADJUSTMENT,
            grant ? systemAccount.getAccountId() : targetAccount.getAccountId(),
            grant ? targetAccount.getAccountId() : systemAccount.getAccountId(),
            institutionCoreModule.getBankingSourceServerId(),
            BankingConstants.OPERATOR_TYPE_ADMIN,
            sender.getCommandSenderName(),
            targetPlayer.getUniqueID().toString(),
            amount,
            comment == null ? (grant ? "ops account grant" : "ops account deduct") : comment,
            buildAdminAdjustmentBusinessRef(grant ? "grant" : "deduct", targetAccount.getAccountId()),
            "{\"command\":\"" + (grant ? "grant" : "deduct") + "\"}"));

        sender.addChatMessage(new ChatComponentText(
            (grant ? "Granted " : "Deducted ") + amount + (grant ? " to " : " from ")
                + targetPlayer.getCommandSenderName() + ". transactionId="
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

        Optional<BankAccount> fromAccount = bankingService.findAccount(
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            fromPlayer.getUniqueID().toString(),
            BankingConstants.DEFAULT_CURRENCY_CODE);
        Optional<BankAccount> toAccount = bankingService.findAccount(
            BankingConstants.OWNER_TYPE_PLAYER_UUID,
            toPlayer.getUniqueID().toString(),
            BankingConstants.DEFAULT_CURRENCY_CODE);
        if (!fromAccount.isPresent() || !toAccount.isPresent()) {
            sender.addChatMessage(new ChatComponentText(
                "Both players must already have bank accounts before transfer. Use /jsirgalaxybase bank open <player>."));
            return;
        }
        BankPostingResult result = bankingService.transferBetweenPlayers(new PlayerTransferCommand(
            newRequestId("transfer"),
            fromAccount.get().getAccountId(),
            toAccount.get().getAccountId(),
            institutionCoreModule.getBankingSourceServerId(),
            sender.getCommandSenderName(),
            fromPlayer.getUniqueID().toString(),
            amount,
            comment == null ? "admin test transfer" : comment,
            buildPlayerTransferBusinessRef(fromAccount.get().getAccountId(), toAccount.get().getAccountId()),
            "{\"command\":\"transfer\"}"));

        sender.addChatMessage(new ChatComponentText(
            "Transferred " + amount + " from " + fromPlayer.getCommandSenderName() + " to "
                + toPlayer.getCommandSenderName() + ". transactionId=" + result.getTransaction().getTransactionId()));
        sender.addChatMessage(new ChatComponentText(
            fromPlayer.getCommandSenderName() + " balance=" + findAccountBalance(result, fromAccount.get().getAccountId())
                + ", " + toPlayer.getCommandSenderName() + " balance="
                + findAccountBalance(result, toAccount.get().getAccountId())));
    }

    private void handleMarketQuote(EntityPlayerMP player, String[] args, TaskCoinExchangeService exchangeService) {
        if (args.length < 3 || !"hand".equalsIgnoreCase(args[2])) {
            player.addChatMessage(new ChatComponentText("Usage: /jsirgalaxybase market quote hand"));
            return;
        }

        TaskCoinExchangeQuote quote = exchangeService.previewHeldCoin(player);
        player.addChatMessage(new ChatComponentText(
            "Held task coin quote: registry=" + quote.getDescriptor().getRegistryName() + ", family="
                + quote.getDescriptor().getFamily() + ", tier=" + quote.getDescriptor().getTier() + ", faceValue="
                + quote.getDescriptor().getFaceValue() + ", quantity=" + quote.getQuantity()));
        player.addChatMessage(new ChatComponentText(
            "effectiveExchangeValue=" + quote.getEffectiveExchangeValue() + ", contributionValue="
                + quote.getContributionValue() + ", ruleVersion=" + quote.getExchangeRuleVersion()));
        player.addChatMessage(new ChatComponentText(
            "Current phase is source-blind: the system recognizes the held Dreamcraft coin item itself, not the original quest source."));
    }

    private void handleMarketExchange(EntityPlayerMP player, String[] args, TaskCoinExchangeService exchangeService) {
        if (args.length < 3 || !"hand".equalsIgnoreCase(args[2])) {
            player.addChatMessage(new ChatComponentText("Usage: /jsirgalaxybase market exchange hand"));
            return;
        }

        TaskCoinExchangeService.TaskCoinExchangeExecutionResult result = exchangeService.exchangeHeldCoin(player);
        player.addChatMessage(new ChatComponentText(
            "Task coin exchange posted: transactionId=" + result.getPostingResult().getTransaction().getTransactionId()
                + ", registry=" + result.getQuote().getDescriptor().getRegistryName() + ", quantity="
                + result.getQuote().getQuantity() + ", effectiveExchangeValue="
                + result.getQuote().getEffectiveExchangeValue()));
        BankAccount playerAccount = findAffectedAccountByOwnerRef(result.getPostingResult(),
            player.getUniqueID().toString());
        if (playerAccount != null) {
            player.addChatMessage(new ChatComponentText("New balance=" + playerAccount.getAvailableBalance()
                + " STARCOIN"));
        }
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

    private BankAccount findAffectedAccountByOwnerRef(BankPostingResult result, String ownerRef) {
        for (BankAccount account : result.getAffectedAccounts()) {
            if (ownerRef.equals(account.getOwnerRef())) {
                return account;
            }
        }
        return null;
    }

    private String newRequestId(String action) {
        return "cmd-" + action + "-" + UUID.randomUUID().toString();
    }

    private String buildPlayerTransferBusinessRef(long fromAccountId, long toAccountId) {
        return "p2p:" + fromAccountId + ":" + toAccountId;
    }

    private String buildAdminAdjustmentBusinessRef(String action, long targetAccountId) {
        return "admin:" + action + ":" + targetAccountId;
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
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank deduct <player> <amount> [comment]"));
        sender.addChatMessage(new ChatComponentText("/jsirgalaxybase bank transfer <fromPlayer> <toPlayer> <amount> [comment]"));
    }

    private void sendMarketUsage(ReplySink reply) {
        reply.send("Market phase-1 commands:");
        reply.send("/jsirgalaxybase market quote hand");
        reply.send("/jsirgalaxybase market exchange hand");
        reply.send("Market phase-2 standardized spot commands:");
        reply.send("/jsirgalaxybase market sell create hand <unitPrice> [quantity]");
        reply.send("/jsirgalaxybase market sell cancel <orderId>");
        reply.send("/jsirgalaxybase market buy create <productKey> <quantity> <unitPrice>");
        reply.send("/jsirgalaxybase market buy cancel <orderId>");
        reply.send("/jsirgalaxybase market book <productKey> [limit]");
        reply.send("/jsirgalaxybase market claims");
        reply.send("/jsirgalaxybase market claim <custodyId>");
        reply.send("/jsirgalaxybase market recover [limit]");
    }

    interface ReplySink {

        void send(String message);
    }

    interface StandardizedMarketPlayerContext {

        String getPlayerRef();

        String getDisplayName();

        ItemStack getHeldItem();

        void setHeldItem(ItemStack stack);

        void syncInventory();
    }

    static final class LiveReplySink implements ReplySink {

        private final ICommandSender sender;

        private LiveReplySink(ICommandSender sender) {
            this.sender = sender;
        }

        @Override
        public void send(String message) {
            sender.addChatMessage(new ChatComponentText(message));
        }
    }

    static final class LiveStandardizedMarketPlayerContext implements StandardizedMarketPlayerContext {

        private final EntityPlayerMP player;

        private LiveStandardizedMarketPlayerContext(EntityPlayerMP player) {
            this.player = player;
        }

        @Override
        public String getPlayerRef() {
            return player.getUniqueID().toString();
        }

        @Override
        public String getDisplayName() {
            return player.getCommandSenderName();
        }

        @Override
        public ItemStack getHeldItem() {
            return player.inventory.getCurrentItem();
        }

        @Override
        public void setHeldItem(ItemStack stack) {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, stack);
        }

        @Override
        public void syncInventory() {
            player.inventory.markDirty();
            if (player.openContainer != null) {
                player.openContainer.detectAndSendChanges();
            }
        }
    }

    static final class HeldStandardizedItem {

        private final String productKey;
        private final boolean stackable;
        private final ItemStack snapshot;

        HeldStandardizedItem(String productKey, boolean stackable, ItemStack snapshot) {
            this.productKey = productKey;
            this.stackable = stackable;
            this.snapshot = snapshot;
        }
    }
}