package com.jsirgalaxybase.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import com.jsirgalaxybase.module.ModuleManager;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketAdmissionDecision;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketAdmissionReason;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogEntry;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogVersion;
import com.jsirgalaxybase.modules.core.market.application.CustomMarketService;
import com.jsirgalaxybase.modules.core.market.application.TaskCoinExchangePlanner;
import com.jsirgalaxybase.modules.core.market.application.TaskCoinExchangeService;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.application.MarketRecoveryService;
import com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketService;
import com.jsirgalaxybase.modules.core.market.application.command.CancelCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.PublishCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.PurchaseCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CancelBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CancelSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimMarketAssetCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.DepositMarketInventoryCommand;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationType;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderSide;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketAuditLog;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketAuditType;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketDeliveryStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketItemSnapshot;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListing;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListingStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketLimitPolicy;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketLimitStatus;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketPairDefinition;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketQuoteResult;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketRuleVersion;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinDescriptor;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinExchangeQuote;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public class GalaxyBaseCommandTest {

    private static final Item CUSTOM_TEST_ITEM = new Item().setUnlocalizedName("custom_claim_test_item");

    @Test
    public void marketUsageDescribesQuoteAndExchangeAsExchangeCompatibilityEntry() throws Exception {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingReplySink reply = new RecordingReplySink();
        Method method = GalaxyBaseCommand.class.getDeclaredMethod("sendMarketUsage", GalaxyBaseCommand.ReplySink.class);
        method.setAccessible(true);

        method.invoke(command, reply);

        assertTrue(reply.contains("MARKET 总入口已拆分为三类子入口"));
        assertTrue(reply.contains("标准商品市场入口（兼容旧 market sell / buy / book / claim 路径）"));
        assertTrue(reply.contains("定制商品市场入口（custom 子入口）"));
        assertTrue(reply.contains("汇率市场入口（quote / exchange 兼容子入口）"));
        assertTrue(reply.contains("/jsirgalaxybase market custom list hand <price>"));
        assertTrue(reply.contains("仅允许当前手持单件"));
        assertTrue(reply.contains("/jsirgalaxybase market custom claim <listingId>"));
        assertTrue(reply.contains("/jsirgalaxybase market custom pending"));
        assertTrue(reply.contains("/jsirgalaxybase market quote hand"));
        assertTrue(reply.contains("/jsirgalaxybase market exchange hand"));
        assertTrue(reply.contains("共享运维入口"));
    }

    @Test
    public void customListHandConsumesHeldStackAndDispatchesToCustomService() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService spotService = new RecordingSpotMarketService();
        RecordingCustomMarketService customService = new RecordingCustomMarketService();
        customService.publishResult = buildCustomPublishResult(301L, "Relic Stone", 1800L);
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(spotService,
            customService, Collections.<MarketOperationLog>emptyList(), "test-server");
        FakePlayerContext playerContext = new FakePlayerContext("seller-a", new ItemStack(Blocks.stone, 1, 0));
        RecordingReplySink reply = new RecordingReplySink();

        command.processCustomMarketCommand(playerContext, reply,
            new String[] { "market", "custom", "list", "hand", "1800" }, institutionCoreModule);

        assertNotNull(customService.lastPublishCommand);
        assertEquals("seller-a", customService.lastPublishCommand.getPlayerRef());
        assertEquals(1800L, customService.lastPublishCommand.getAskingPrice());
        assertEquals(1, playerContext.syncCount);
        assertEquals(null, playerContext.getHeldItem());
        assertTrue(reply.contains("定制商品挂牌已发布"));
        assertTrue(reply.contains("listingId=301"));
    }

    @Test
    public void customListHandRejectsStackedHeldItem() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(
            new RecordingSpotMarketService(), new RecordingCustomMarketService(),
            Collections.<MarketOperationLog>emptyList(), "test-server");

        try {
            command.processCustomMarketCommand(new FakePlayerContext("seller-a", new ItemStack(Blocks.stone, 4, 0)),
                new RecordingReplySink(), new String[] { "market", "custom", "list", "hand", "1800" },
                institutionCoreModule);
            fail("expected stacked custom listing to be rejected");
        } catch (MarketOperationException expected) {
            assertTrue(expected.getMessage().contains("只允许手持单件商品挂牌"));
        }
    }

    @Test
    public void customCancelRestoresSnapshotToHand() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService spotService = new RecordingSpotMarketService();
        RecordingCustomMarketService customService = new RecordingCustomMarketService();
        customService.inspectView = buildCustomListingView(401L, "Used Drill", CustomMarketListingStatus.ACTIVE,
            CustomMarketDeliveryStatus.ESCROW_HELD, null);
        customService.cancelResult = buildCustomCancelResult(401L, "Used Drill");
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(spotService,
            customService, Collections.<MarketOperationLog>emptyList(), "test-server");
        FakePlayerContext playerContext = new FakePlayerContext("seller-a", null);
        RecordingReplySink reply = new RecordingReplySink();

        command.processCustomMarketCommand(playerContext, reply,
            new String[] { "market", "custom", "cancel", "401" }, institutionCoreModule);

        assertNotNull(customService.lastCancelCommand);
        assertEquals(401L, customService.lastCancelCommand.getListingId());
        assertNotNull(playerContext.getHeldItem());
        assertEquals(1, playerContext.getHeldItem().stackSize);
        assertEquals(1, playerContext.syncCount);
        assertTrue(reply.contains("定制商品挂牌已下架并返还到当前手持槽"));
    }

    @Test
    public void customClaimRestoresSnapshotToHandAndDispatchesToCustomService() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService spotService = new RecordingSpotMarketService();
        RecordingCustomMarketService customService = new RecordingCustomMarketService();
        customService.inspectView = buildCustomListingView(402L, "Used Drill", CustomMarketListingStatus.SOLD,
            CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM, "buyer-b");
        customService.claimResult = buildCustomClaimResult(402L, "Used Drill", "buyer-b");
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(spotService,
            customService, Collections.<MarketOperationLog>emptyList(), "test-server");
        FakePlayerContext playerContext = new FakePlayerContext("buyer-b", null);
        RecordingReplySink reply = new RecordingReplySink();

        command.processCustomMarketCommand(playerContext, reply,
            new String[] { "market", "custom", "claim", "402" }, institutionCoreModule);

        assertNotNull(customService.lastClaimCommand);
        assertEquals(402L, customService.lastClaimCommand.getListingId());
        assertNotNull(playerContext.getHeldItem());
        assertEquals(1, playerContext.getHeldItem().stackSize);
        assertEquals(1, playerContext.syncCount);
        assertTrue(reply.contains("定制商品已领取并完结"));
    }

    @Test
    public void customPendingPrintsSellerAndBuyerSections() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService spotService = new RecordingSpotMarketService();
        RecordingCustomMarketService customService = new RecordingCustomMarketService();
        customService.sellerPending = Collections.singletonList(buildCustomListingView(501L, "Ancient Gear",
            CustomMarketListingStatus.SOLD, CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM, "buyer-b"));
        customService.buyerPending = Collections.singletonList(buildCustomListingView(502L, "Ancient Gear",
            CustomMarketListingStatus.SOLD, CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM, "buyer-a"));
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(spotService,
            customService, Collections.<MarketOperationLog>emptyList(), "test-server");
        RecordingReplySink reply = new RecordingReplySink();

        command.processCustomMarketCommand(new FakePlayerContext("seller-a", null), reply,
            new String[] { "market", "custom", "pending" }, institutionCoreModule);

        assertTrue(reply.contains("卖家侧待交付记录: 1"));
        assertTrue(reply.contains("买家侧待领取记录: 1"));
        assertTrue(reply.contains("listingId=501"));
    }

    @Test
    public void buildMarketQuoteMessagesShowsDisallowedFormalFields() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        ExchangeMarketQuoteResult formalQuote = new ExchangeMarketQuoteResult(
            new ExchangeMarketPairDefinition("task-coin-to-starcoin", "TASK_COIN", "STARCOIN", "任务书硬币",
                "星光币"),
            new ExchangeMarketRuleVersion(TaskCoinExchangePlanner.RULE_VERSION, "汇率市场任务书硬币固定规则 v1"),
            new ExchangeMarketLimitPolicy(ExchangeMarketLimitStatus.DISALLOWED, "TASK_COIN_TIER_DISALLOWED",
                "当前汇率市场 v1 只支持 BASE / I / II / III / IV 任务书硬币。"),
            "req-disallowed", "player-a", "s1-test", "dreamcraft:item.CoinMinerV", "UNRESOLVED", "UNRESOLVED",
            0L, 2L, 0L, 0L, 0L, 0L, 1L, 10000,
            "当前汇率市场 v1 只支持 BASE / I / II / III / IV 任务书硬币。");
        TaskCoinExchangeQuote legacyQuote = new TaskCoinExchangeQuote(
            new TaskCoinDescriptor("dreamcraft:item.CoinMinerV", "UNRESOLVED", "UNRESOLVED", 0L), 2, 0L, 0L, 0L,
            TaskCoinExchangePlanner.RULE_VERSION);

        List<String> lines = command.buildMarketQuoteMessages(
            new TaskCoinExchangeService.PreviewResult(formalQuote, legacyQuote));

        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("pair=task-coin-to-starcoin"));
        assertTrue(lines.get(0).contains("family=UNRESOLVED"));
        assertTrue(lines.get(1).contains("limitStatus=DISALLOWED"));
        assertTrue(lines.get(2).contains("reasonCode=TASK_COIN_TIER_DISALLOWED"));
        assertTrue(lines.get(2).contains("notes=当前汇率市场 v1 只支持 BASE / I / II / III / IV 任务书硬币。"));
    }

    @Test
    public void buildStandardizedCatalogAdmissionMessageShowsVersionReasonAndSource() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        StandardizedMarketAdmissionDecision decision = new StandardizedMarketAdmissionDecision(
            new StandardizedMarketCatalogVersion("standardized-spot-catalog-v1", "标准商品市场目录 v1"),
            new StandardizedMarketCatalogEntry(new StandardizedMarketProduct("minecraft:stone", 0),
                "test-category", "统一定义、统一计量、统一托管", "test"),
            true, StandardizedMarketAdmissionReason.CATALOG_ADMITTED, "admitted",
            "gregtech-standardized-metal-adapter", "GregTech 来源");

        String message = command.buildStandardizedCatalogAdmissionMessage(decision);

        assertTrue(message.contains("version=standardized-spot-catalog-v1"));
        assertTrue(message.contains("reason=CATALOG_ADMITTED"));
        assertTrue(message.contains("source=gregtech-standardized-metal-adapter"));
    }

    @Test
    public void sellDepositHandConsumesHeldStackAndDispatchesToSpotService() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        service.depositResult = buildDepositResult(7L, 5L, 5L);
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service,
            Collections.<MarketOperationLog>emptyList(), "test-server");
        FakePlayerContext playerContext = new FakePlayerContext("player-a", new ItemStack(Blocks.stone, 16, 0));
        RecordingReplySink reply = new RecordingReplySink();

        command.executeStandardizedSellDeposit(playerContext, reply, institutionCoreModule,
            new GalaxyBaseCommand.HeldStandardizedItem("minecraft:stone:0", true, playerContext.getHeldItem().copy(),
                admittedCatalogDecision("minecraft:stone:0")),
            5L);

        assertNotNull(service.lastDepositCommand);
        assertEquals("player-a", service.lastDepositCommand.getPlayerRef());
        assertEquals("minecraft:stone:0", service.lastDepositCommand.getProductKey());
        assertEquals(5L, service.lastDepositCommand.getQuantity());
        assertNotNull(playerContext.getHeldItem());
        assertEquals(11, playerContext.getHeldItem().stackSize);
        assertEquals(1, playerContext.syncCount);
        assertTrue(reply.contains("仓储存入完成"));
    }

    @Test
    public void sellDepositHandRestoresHeldStackWhenSpotServiceFails() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        service.depositFailure = new MarketOperationException("simulated deposit failure");
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service,
            Collections.<MarketOperationLog>emptyList(), "test-server");
        FakePlayerContext playerContext = new FakePlayerContext("player-a", new ItemStack(Blocks.stone, 16, 0));
        RecordingReplySink reply = new RecordingReplySink();

        try {
            command.executeStandardizedSellDeposit(playerContext, reply, institutionCoreModule,
                new GalaxyBaseCommand.HeldStandardizedItem("minecraft:stone:0", true,
                    playerContext.getHeldItem().copy(), admittedCatalogDecision("minecraft:stone:0")), 5L);
            fail("expected deposit to propagate failure");
        } catch (MarketOperationException expected) {
            assertTrue(expected.getMessage().contains("simulated deposit failure"));
        }

        assertNotNull(playerContext.getHeldItem());
        assertEquals(16, playerContext.getHeldItem().stackSize);
        assertEquals(2, playerContext.syncCount);
    }

    @Test
    public void sellCreateDispatchesWarehouseProductQuantityAndPrice() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        service.sellResult = buildSellCreateResult(41L, 7L, 5L, 12L);
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service,
            Collections.<MarketOperationLog>emptyList(), "test-server");
        FakePlayerContext playerContext = new FakePlayerContext("player-a", new ItemStack(Blocks.stone, 16, 0));
        RecordingReplySink reply = new RecordingReplySink();

        command.executeStandardizedSellCreate(playerContext, reply, institutionCoreModule, "minecraft:stone:0", 12L,
            5L, true);

        assertNotNull(service.lastCreateSellCommand);
        assertEquals("player-a", service.lastCreateSellCommand.getPlayerRef());
        assertEquals("minecraft:stone:0", service.lastCreateSellCommand.getProductKey());
        assertEquals(5L, service.lastCreateSellCommand.getQuantity());
        assertEquals(12L, service.lastCreateSellCommand.getUnitPrice());
        assertEquals(16, playerContext.getHeldItem().stackSize);
        assertEquals(0, playerContext.syncCount);
        assertTrue(reply.contains("卖单已创建"));
    }

    @Test
    public void buyCreateDispatchesProductKeyQuantityAndPrice() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        service.buyResult = buildBuyCreateResult(73L, 7L, 210L);
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service,
            Collections.<MarketOperationLog>emptyList(), "test-server");
        FakePlayerContext playerContext = new FakePlayerContext("buyer-a", new ItemStack(Blocks.stone, 1, 0));
        RecordingReplySink reply = new RecordingReplySink();

        command.executeStandardizedBuyCreate(playerContext, reply, institutionCoreModule, "minecraft:stone:0", 7L,
            30L, true);

        assertNotNull(service.lastCreateBuyCommand);
        assertEquals("buyer-a", service.lastCreateBuyCommand.getPlayerRef());
        assertEquals("minecraft:stone:0", service.lastCreateBuyCommand.getProductKey());
        assertEquals(7L, service.lastCreateBuyCommand.getQuantity());
        assertEquals(30L, service.lastCreateBuyCommand.getUnitPrice());
        assertTrue(service.lastCreateBuyCommand.isStackable());
        assertTrue(reply.contains("买单已创建"));
    }

    @Test
    public void commandCatalogInspectionUsesRuntimeCatalogDecisionInsteadOfLocalDefaultCatalog() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        service.catalogProductDecision = new StandardizedMarketAdmissionDecision(
            new StandardizedMarketCatalogVersion("runtime-catalog-v2", "运行时目录 v2"),
            new StandardizedMarketCatalogEntry(new StandardizedMarketProduct("minecraft:diamond", 0),
                "runtime-category", "runtime-only", "runtime-entry"),
            true, StandardizedMarketAdmissionReason.CATALOG_ADMITTED, "runtime admitted", "runtime-test-source",
            "Runtime Test Source");
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service,
            Collections.<MarketOperationLog>emptyList(), "test-server");

        StandardizedMarketAdmissionDecision decision = command.inspectRuntimeCatalogProduct(institutionCoreModule,
            "minecraft:diamond:0");

        assertEquals("runtime-catalog-v2", decision.getCatalogVersion().getVersionKey());
        assertEquals("runtime-test-source", decision.getSourceKey());
        assertEquals("minecraft:diamond:0", decision.requireProduct().getProductKey());
    }

    @Test
    public void sellCancelDispatchesOrderIdAndRepliesWithReturnedAvailableCustody() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        service.cancelSellResult = buildSellCancelResult(61L, 91L, 4L);
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service,
            Collections.<MarketOperationLog>emptyList(), "test-server");
        RecordingReplySink reply = new RecordingReplySink();

        command.processStandardizedMarketCommand(new FakePlayerContext("seller-a", null), reply,
            new String[] { "market", "sell", "cancel", "61" }, institutionCoreModule);

        assertNotNull(service.lastCancelSellCommand);
        assertEquals("seller-a", service.lastCancelSellCommand.getPlayerRef());
        assertEquals(61L, service.lastCancelSellCommand.getOrderId());
        assertTrue(reply.contains("卖单已撤销"));
        assertTrue(reply.contains("returnedAvailableCustodyId=91"));
    }

    @Test
    public void buyCancelDispatchesOrderIdAndRepliesWithReleasedFunds() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        service.cancelBuyResult = buildBuyCancelResult(73L, 0L, 210L);
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service,
            Collections.<MarketOperationLog>emptyList(), "test-server");
        RecordingReplySink reply = new RecordingReplySink();

        command.processStandardizedMarketCommand(new FakePlayerContext("buyer-a", null), reply,
            new String[] { "market", "buy", "cancel", "73" }, institutionCoreModule);

        assertNotNull(service.lastCancelBuyCommand);
        assertEquals("buyer-a", service.lastCancelBuyCommand.getPlayerRef());
        assertEquals(73L, service.lastCancelBuyCommand.getOrderId());
        assertTrue(reply.contains("买单已撤销"));
        assertTrue(reply.contains("releasedFunds=210"));
    }

    @Test
    public void claimsCommandPrintsClaimableCustodyLines() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        service.claimables = Collections.singletonList(new MarketCustodyInventory(88L, "player-a",
            new StandardizedMarketProduct("minecraft:stone", 0), true, 9L, MarketCustodyStatus.CLAIMABLE, 21L, 0L,
            "test-server", Instant.now(), Instant.now()));
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service,
            Collections.<MarketOperationLog>emptyList(), "test-server");
        RecordingReplySink reply = new RecordingReplySink();

        command.processStandardizedMarketCommand(new FakePlayerContext("player-a", null), reply,
            new String[] { "market", "claims" }, institutionCoreModule);

        assertTrue(reply.contains("CLAIMABLE 资产共 1 条"));
        assertTrue(reply.contains("custodyId=88"));
    }

    @Test
    public void claimCommandDispatchesCustodyIdAndRepliesWithClaimedAsset() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        service.claimResult = buildClaimResult(88L, 9L);
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service,
            Collections.<MarketOperationLog>emptyList(), "test-server");
        RecordingReplySink reply = new RecordingReplySink();

        command.processStandardizedMarketCommand(new FakePlayerContext("player-a", null), reply,
            new String[] { "market", "claim", "88" }, institutionCoreModule);

        assertNotNull(service.lastClaimCommand);
        assertEquals("player-a", service.lastClaimCommand.getPlayerRef());
        assertEquals(88L, service.lastClaimCommand.getCustodyId());
        assertTrue(reply.contains("CLAIMABLE 资产已提取"));
        assertTrue(reply.contains("custodyId=88"));
    }

    @Test
    public void recoverCommandUsesModuleRecoveryScanAndPrintsSummary() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        List<MarketOperationLog> recovered = Collections.singletonList(new MarketOperationLog(51L, "req-recover",
            MarketOperationType.BUY_ORDER_CREATE, MarketOperationStatus.COMPLETED, "test-server", "player-a",
            "playerRef=player-a", 71L, 0L, 0L, "recovered", Instant.now(), Instant.now()));
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service, recovered,
            "test-server");
        RecordingReplySink reply = new RecordingReplySink();

        command.processMarketRecoveryCommand(reply, new String[] { "market", "recover", "15" },
            institutionCoreModule, true);

        assertEquals(15, institutionCoreModule.lastRecoveryLimit);
        assertTrue(reply.contains("市场恢复扫描已处理 1 条操作"));
        assertTrue(reply.contains("op=51"));
    }

    private StandardizedSpotMarketService.CreateSellOrderResult buildSellCreateResult(long orderId, long custodyId,
        long quantity, long unitPrice) {
        MarketOrder order = new MarketOrder(orderId, MarketOrderSide.SELL, MarketOrderStatus.OPEN, "player-a",
            new StandardizedMarketProduct("minecraft:stone", 0), true, unitPrice, quantity, quantity, 0L, 0L,
            custodyId, "test-server", Instant.now(), Instant.now());
        MarketCustodyInventory custody = new MarketCustodyInventory(custodyId, "player-a",
            new StandardizedMarketProduct("minecraft:stone", 0), true, quantity, MarketCustodyStatus.ESCROW_SELL,
            orderId, 0L, "test-server", Instant.now(), Instant.now());
        MarketOperationLog operationLog = new MarketOperationLog(11L, "req-sell", MarketOperationType.SELL_ORDER_CREATE,
            MarketOperationStatus.COMPLETED, "test-server", "player-a", "playerRef=player-a", orderId, custodyId,
            0L, "created", Instant.now(), Instant.now());
        return new StandardizedSpotMarketService.CreateSellOrderResult(order, custody, operationLog,
            Collections.<MarketCustodyInventory>emptyList(),
            Collections.<com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord>emptyList());
    }

    private StandardizedSpotMarketService.DepositInventoryResult buildDepositResult(long custodyId, long quantity,
        long totalAvailableQuantity) {
        MarketCustodyInventory custody = new MarketCustodyInventory(custodyId, "player-a",
            new StandardizedMarketProduct("minecraft:stone", 0), true, quantity, MarketCustodyStatus.AVAILABLE, 0L,
            0L, "test-server", Instant.now(), Instant.now());
        MarketOperationLog operationLog = new MarketOperationLog(16L, "req-deposit",
            MarketOperationType.INVENTORY_DEPOSIT, MarketOperationStatus.COMPLETED, "test-server", "player-a",
            "playerRef=player-a", 0L, custodyId, 0L, "deposited", Instant.now(), Instant.now());
        return new StandardizedSpotMarketService.DepositInventoryResult(custody, operationLog, totalAvailableQuantity);
    }

    private StandardizedSpotMarketService.CreateBuyOrderResult buildBuyCreateResult(long orderId, long quantity,
        long reservedFunds) {
        MarketOrder order = new MarketOrder(orderId, MarketOrderSide.BUY, MarketOrderStatus.OPEN, "buyer-a",
            new StandardizedMarketProduct("minecraft:stone", 0), true, 30L, quantity, quantity, 0L, reservedFunds,
            0L, "test-server", Instant.now(), Instant.now());
        MarketOperationLog operationLog = new MarketOperationLog(12L, "req-buy", MarketOperationType.BUY_ORDER_CREATE,
            MarketOperationStatus.COMPLETED, "test-server", "buyer-a", "playerRef=buyer-a", orderId, 0L, 0L,
            "created", Instant.now(), Instant.now());
        return new StandardizedSpotMarketService.CreateBuyOrderResult(order, operationLog,
            Collections.<MarketCustodyInventory>emptyList(),
            Collections.<com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord>emptyList());
    }

    private StandardizedSpotMarketService.CancelSellOrderResult buildSellCancelResult(long orderId, long custodyId,
        long quantity) {
        MarketOrder order = new MarketOrder(orderId, MarketOrderSide.SELL, MarketOrderStatus.CANCELLED, "seller-a",
            new StandardizedMarketProduct("minecraft:stone", 0), true, 12L, quantity, 0L, 0L, 0L, custodyId,
            "test-server", Instant.now(), Instant.now());
        MarketCustodyInventory custody = new MarketCustodyInventory(custodyId, "seller-a",
            new StandardizedMarketProduct("minecraft:stone", 0), true, quantity, MarketCustodyStatus.AVAILABLE,
            orderId, 0L, "test-server", Instant.now(), Instant.now());
        MarketOperationLog operationLog = new MarketOperationLog(13L, "req-sell-cancel",
            MarketOperationType.SELL_ORDER_CANCEL, MarketOperationStatus.COMPLETED, "test-server", "seller-a",
            "playerRef=seller-a", orderId, custodyId, 0L, "cancelled", Instant.now(), Instant.now());
        return new StandardizedSpotMarketService.CancelSellOrderResult(order, custody, operationLog);
    }

    private StandardizedSpotMarketService.CancelBuyOrderResult buildBuyCancelResult(long orderId, long reservedFunds,
        long releasedFunds) {
        MarketOrder order = new MarketOrder(orderId, MarketOrderSide.BUY, MarketOrderStatus.CANCELLED, "buyer-a",
            new StandardizedMarketProduct("minecraft:stone", 0), true, 30L, 7L, 0L, 0L, reservedFunds, 0L,
            "test-server", Instant.now(), Instant.now());
        MarketOperationLog operationLog = new MarketOperationLog(14L, "req-buy-cancel",
            MarketOperationType.BUY_ORDER_CANCEL, MarketOperationStatus.COMPLETED, "test-server", "buyer-a",
            "playerRef=buyer-a", orderId, 0L, 0L, "cancelled", Instant.now(), Instant.now());
        return new StandardizedSpotMarketService.CancelBuyOrderResult(order, operationLog, releasedFunds);
    }

    private StandardizedSpotMarketService.ClaimMarketAssetResult buildClaimResult(long custodyId, long quantity) {
        MarketCustodyInventory custody = new MarketCustodyInventory(custodyId, "player-a",
            new StandardizedMarketProduct("minecraft:stone", 0), true, quantity, MarketCustodyStatus.CLAIMED, 21L,
            31L, "test-server", Instant.now(), Instant.now());
        MarketOperationLog operationLog = new MarketOperationLog(15L, "req-claim",
            MarketOperationType.CLAIMABLE_ASSET_CLAIM, MarketOperationStatus.COMPLETED, "test-server", "player-a",
            "playerRef=player-a", 21L, custodyId, 0L, "claimed", Instant.now(), Instant.now());
        return new StandardizedSpotMarketService.ClaimMarketAssetResult(custody, operationLog);
    }

    private StandardizedMarketAdmissionDecision admittedCatalogDecision(String productKey) {
        return new StandardizedMarketAdmissionDecision(
            new StandardizedMarketCatalogVersion("test-catalog-v1", "测试目录 v1"),
            new StandardizedMarketCatalogEntry(new StandardizedMarketProduct("minecraft:stone", 0),
                "test-category", "统一定义、统一计量、统一托管", "test-entry"),
            true, StandardizedMarketAdmissionReason.CATALOG_ADMITTED, "admitted", "test-source", "Test Source");
    }

    private static final class RecordingReplySink implements GalaxyBaseCommand.ReplySink {

        private final List<String> messages = new ArrayList<String>();

        @Override
        public void send(String message) {
            messages.add(message);
        }

        private boolean contains(String text) {
            for (String message : messages) {
                if (message.contains(text)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class FakePlayerContext implements GalaxyBaseCommand.StandardizedMarketPlayerContext {

        private final String playerRef;
        private ItemStack heldItem;
        private int syncCount;

        private FakePlayerContext(String playerRef, ItemStack heldItem) {
            this.playerRef = playerRef;
            this.heldItem = heldItem;
        }

        @Override
        public String getPlayerRef() {
            return playerRef;
        }

        @Override
        public String getDisplayName() {
            return playerRef;
        }

        @Override
        public ItemStack getHeldItem() {
            return heldItem;
        }

        @Override
        public void setHeldItem(ItemStack stack) {
            heldItem = stack;
        }

        @Override
        public void syncInventory() {
            syncCount++;
        }
    }

    private static final class RecordingInstitutionCoreModule extends InstitutionCoreModule {

        private final RecordingSpotMarketService spotMarketService;
        private final RecordingCustomMarketService customMarketService;
        private final List<MarketOperationLog> recoveryResults;
        private final String sourceServerId;
        private int lastRecoveryLimit;

        private RecordingInstitutionCoreModule(RecordingSpotMarketService spotMarketService,
            RecordingCustomMarketService customMarketService, List<MarketOperationLog> recoveryResults,
            String sourceServerId) {
            this.spotMarketService = spotMarketService;
            this.customMarketService = customMarketService;
            this.recoveryResults = recoveryResults;
            this.sourceServerId = sourceServerId;
        }

        private RecordingInstitutionCoreModule(RecordingSpotMarketService spotMarketService,
            List<MarketOperationLog> recoveryResults, String sourceServerId) {
            this(spotMarketService, new RecordingCustomMarketService(), recoveryResults, sourceServerId);
        }

        @Override
        public StandardizedSpotMarketService getStandardizedSpotMarketService() {
            return spotMarketService;
        }

        @Override
        public CustomMarketService getCustomMarketService() {
            return customMarketService;
        }

        @Override
        public MarketRecoveryService getMarketRecoveryService() {
            return new MarketRecoveryService(new NoOpOrderBookRepository(), new NoOpCustodyRepository(),
                new NoOpOperationLogRepository());
        }

        @Override
        public String getBankingSourceServerId() {
            return sourceServerId;
        }

        @Override
        public List<MarketOperationLog> scanMarketRecovery(int limit) {
            lastRecoveryLimit = limit;
            return recoveryResults;
        }
    }

    private static final class RecordingSpotMarketService extends StandardizedSpotMarketService {

        private DepositMarketInventoryCommand lastDepositCommand;
        private CreateSellOrderCommand lastCreateSellCommand;
        private CreateBuyOrderCommand lastCreateBuyCommand;
        private CancelSellOrderCommand lastCancelSellCommand;
        private CancelBuyOrderCommand lastCancelBuyCommand;
        private ClaimMarketAssetCommand lastClaimCommand;
        private RuntimeException depositFailure;
        private RuntimeException sellFailure;
        private StandardizedSpotMarketService.DepositInventoryResult depositResult;
        private StandardizedSpotMarketService.CreateSellOrderResult sellResult;
        private StandardizedSpotMarketService.CreateBuyOrderResult buyResult;
        private StandardizedSpotMarketService.CancelSellOrderResult cancelSellResult;
        private StandardizedSpotMarketService.CancelBuyOrderResult cancelBuyResult;
        private StandardizedSpotMarketService.ClaimMarketAssetResult claimResult;
        private List<MarketCustodyInventory> claimables = Collections.emptyList();
        private StandardizedMarketAdmissionDecision catalogProductDecision = new StandardizedMarketAdmissionDecision(
            new StandardizedMarketCatalogVersion("test-catalog-v1", "测试目录 v1"),
            new StandardizedMarketCatalogEntry(new StandardizedMarketProduct("minecraft:stone", 0),
                "test-category", "test-runtime", "test-entry"),
            true, StandardizedMarketAdmissionReason.CATALOG_ADMITTED, "admitted", "test-source", "Test Source");

        private RecordingSpotMarketService() {
            super(new NoOpOrderBookRepository(), new NoOpCustodyRepository(), new NoOpOperationLogRepository(),
                new NoOpTradeRecordRepository(), new DirectMarketTransactionRunner(), null);
        }

        @Override
        public StandardizedMarketAdmissionDecision inspectCatalogProduct(String productKey) {
            return catalogProductDecision;
        }

        @Override
        public StandardizedMarketAdmissionDecision inspectCatalogStack(ItemStack stack) {
            return catalogProductDecision;
        }

        @Override
        public StandardizedSpotMarketService.DepositInventoryResult depositInventory(DepositMarketInventoryCommand command) {
            lastDepositCommand = command;
            if (depositFailure != null) {
                throw depositFailure;
            }
            return depositResult;
        }

        @Override
        public StandardizedSpotMarketService.CreateSellOrderResult createSellOrder(CreateSellOrderCommand command) {
            lastCreateSellCommand = command;
            if (sellFailure != null) {
                throw sellFailure;
            }
            return sellResult;
        }

        @Override
        public StandardizedSpotMarketService.CreateBuyOrderResult createBuyOrder(CreateBuyOrderCommand command) {
            lastCreateBuyCommand = command;
            return buyResult;
        }

        @Override
        public List<MarketCustodyInventory> listClaimableAssets(String playerRef) {
            return claimables;
        }

        @Override
        public StandardizedSpotMarketService.CancelSellOrderResult cancelSellOrder(CancelSellOrderCommand command) {
            lastCancelSellCommand = command;
            return cancelSellResult;
        }

        @Override
        public StandardizedSpotMarketService.CancelBuyOrderResult cancelBuyOrder(CancelBuyOrderCommand command) {
            lastCancelBuyCommand = command;
            return cancelBuyResult;
        }

        @Override
        public StandardizedSpotMarketService.ClaimMarketAssetResult claimMarketAsset(ClaimMarketAssetCommand command) {
            lastClaimCommand = command;
            return claimResult;
        }
    }

    private static final class RecordingCustomMarketService extends CustomMarketService {

        private PublishCustomMarketListingCommand lastPublishCommand;
        private PurchaseCustomMarketListingCommand lastPurchaseCommand;
        private ClaimCustomMarketListingCommand lastClaimCommand;
        private CancelCustomMarketListingCommand lastCancelCommand;
        private CustomMarketService.PublishListingResult publishResult;
        private CustomMarketService.PurchaseListingResult purchaseResult;
        private CustomMarketService.ClaimListingResult claimResult;
        private CustomMarketService.CancelListingResult cancelResult;
        private CustomMarketService.ListingView inspectView;
        private List<CustomMarketService.ListingView> browse = Collections.emptyList();
        private List<CustomMarketService.ListingView> sellerPending = Collections.emptyList();
        private List<CustomMarketService.ListingView> buyerPending = Collections.emptyList();

        private RecordingCustomMarketService() {
            super(null, null, null, null, new DirectMarketTransactionRunner(), null);
        }

        @Override
        public PublishListingResult publishListing(PublishCustomMarketListingCommand command) {
            lastPublishCommand = command;
            return publishResult;
        }

        @Override
        public ListingView inspectListing(long listingId) {
            return inspectView;
        }

        @Override
        public PurchaseListingResult purchaseListing(PurchaseCustomMarketListingCommand command) {
            lastPurchaseCommand = command;
            return purchaseResult;
        }

        @Override
        public ClaimListingResult claimPurchasedListing(ClaimCustomMarketListingCommand command) {
            lastClaimCommand = command;
            return claimResult;
        }

        @Override
        public CancelListingResult cancelListing(CancelCustomMarketListingCommand command) {
            lastCancelCommand = command;
            return cancelResult;
        }

        @Override
        public List<ListingView> browseListings(int limit) {
            return browse;
        }

        @Override
        public List<ListingView> listSellerPendingDeliveries(String sellerPlayerRef) {
            return sellerPending;
        }

        @Override
        public List<ListingView> listBuyerPendingClaims(String buyerPlayerRef) {
            return buyerPending;
        }
    }

    private CustomMarketService.PublishListingResult buildCustomPublishResult(long listingId, String displayName,
        long price) {
        CustomMarketListing listing = new CustomMarketListing(listingId, "seller-a", null, price, "STARCOIN",
            CustomMarketListingStatus.ACTIVE, CustomMarketDeliveryStatus.ESCROW_HELD, "test-server", Instant.now(),
            Instant.now());
        CustomMarketItemSnapshot snapshot = testSnapshot(701L, listingId, displayName, 1);
        CustomMarketAuditLog auditLog = new CustomMarketAuditLog(801L, "req-custom-publish",
            CustomMarketAuditType.LISTING_PUBLISH, "seller-a", "seller-a|publish", listingId, 0L, "test-server",
            "published", Instant.now(), Instant.now());
        return new CustomMarketService.PublishListingResult(listing, snapshot, auditLog);
    }

    private CustomMarketService.ClaimListingResult buildCustomClaimResult(long listingId, String displayName,
        String buyerRef) {
        CustomMarketListing listing = new CustomMarketListing(listingId, "seller-a", buyerRef, 900L, "STARCOIN",
            CustomMarketListingStatus.SOLD, CustomMarketDeliveryStatus.COMPLETED, "test-server", Instant.now(),
            Instant.now());
        CustomMarketItemSnapshot snapshot = testSnapshot(704L, listingId, displayName, 1);
        CustomMarketTradeRecord tradeRecord = new CustomMarketTradeRecord(902L, listingId, "seller-a", buyerRef,
            900L, "STARCOIN", CustomMarketDeliveryStatus.COMPLETED, Instant.now());
        CustomMarketAuditLog auditLog = new CustomMarketAuditLog(803L, "req-custom-claim",
            CustomMarketAuditType.LISTING_CLAIM, buyerRef, buyerRef + "|claim", listingId, tradeRecord.getTradeId(),
            "test-server", "claimed", Instant.now(), Instant.now());
        return new CustomMarketService.ClaimListingResult(listing, snapshot, tradeRecord, auditLog);
    }

    private CustomMarketService.CancelListingResult buildCustomCancelResult(long listingId, String displayName) {
        CustomMarketListing listing = new CustomMarketListing(listingId, "seller-a", null, 900L, "STARCOIN",
            CustomMarketListingStatus.CANCELLED, CustomMarketDeliveryStatus.CANCELLED, "test-server", Instant.now(),
            Instant.now());
        CustomMarketItemSnapshot snapshot = testSnapshot(702L, listingId, displayName, 1);
        CustomMarketAuditLog auditLog = new CustomMarketAuditLog(802L, "req-custom-cancel",
            CustomMarketAuditType.LISTING_CANCEL, "seller-a", "seller-a|cancel", listingId, 0L, "test-server",
            "cancelled", Instant.now(), Instant.now());
        return new CustomMarketService.CancelListingResult(listing, snapshot, auditLog);
    }

    private CustomMarketService.ListingView buildCustomListingView(long listingId, String displayName,
        CustomMarketListingStatus listingStatus, CustomMarketDeliveryStatus deliveryStatus, String buyerRef) {
        CustomMarketListing listing = new CustomMarketListing(listingId, "seller-a", buyerRef, 1500L, "STARCOIN",
            listingStatus, deliveryStatus, "test-server", Instant.now(), Instant.now());
        CustomMarketItemSnapshot snapshot = testSnapshot(703L, listingId, displayName, 1);
        CustomMarketTradeRecord tradeRecord = buyerRef == null ? null
            : new CustomMarketTradeRecord(901L, listingId, "seller-a", buyerRef, 1500L, "STARCOIN",
                deliveryStatus, Instant.now());
        return new CustomMarketService.ListingView(listing, snapshot, tradeRecord);
    }

    private CustomMarketItemSnapshot testSnapshot(long snapshotId, long listingId, String displayName, int stackSize) {
        return new CustomMarketItemSnapshot(snapshotId, listingId, "test:custom_claim_item", 0, stackSize, stackSize > 1,
            displayName, buildSnapshotNbt(stackSize), Instant.now()) {

            @Override
            public ItemStack toItemStack() {
                return new ItemStack(CUSTOM_TEST_ITEM, stackSize, 0);
            }
        };
    }

    private String buildSnapshotNbt(int stackSize) {
        ItemStack stack = new ItemStack(CUSTOM_TEST_ITEM, stackSize, 0);
        NBTTagCompound root = new NBTTagCompound();
        stack.writeToNBT(root);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(root, outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("failed to build custom snapshot test NBT", exception);
        }
    }

    private static final class NoOpOrderBookRepository implements MarketOrderBookRepository {

        @Override
        public MarketOrder save(MarketOrder order) {
            return order;
        }

        @Override
        public MarketOrder update(MarketOrder order) {
            return order;
        }

        @Override
        public Optional<MarketOrder> findById(long orderId) {
            return Optional.empty();
        }

        @Override
        public MarketOrder lockById(long orderId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MarketOrder> findOpenSellOrdersByProductKey(String productKey) {
            return Collections.emptyList();
        }

        @Override
        public List<MarketOrder> findOpenBuyOrdersByProductKey(String productKey) {
            return Collections.emptyList();
        }

        @Override
        public List<MarketOrder> findMatchingSellOrders(String productKey, long maxUnitPrice) {
            return Collections.emptyList();
        }

        @Override
        public List<MarketOrder> findMatchingBuyOrders(String productKey, long minUnitPrice) {
            return Collections.emptyList();
        }
    }

    private static final class NoOpCustodyRepository implements MarketCustodyInventoryRepository {

        @Override
        public MarketCustodyInventory save(MarketCustodyInventory custodyInventory) {
            return custodyInventory;
        }

        @Override
        public MarketCustodyInventory update(MarketCustodyInventory custodyInventory) {
            return custodyInventory;
        }

        @Override
        public Optional<MarketCustodyInventory> findById(long custodyId) {
            return Optional.empty();
        }

        @Override
        public MarketCustodyInventory lockById(long custodyId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<MarketCustodyInventory> findEscrowSellByOrderId(long orderId) {
            return Optional.empty();
        }

        @Override
        public List<MarketCustodyInventory> findByOwnerAndStatus(String ownerPlayerRef, MarketCustodyStatus status) {
            return Collections.emptyList();
        }
    }

    private static final class NoOpOperationLogRepository implements MarketOperationLogRepository {

        @Override
        public MarketOperationLog save(MarketOperationLog operationLog) {
            return operationLog;
        }

        @Override
        public MarketOperationLog update(MarketOperationLog operationLog) {
            return operationLog;
        }

        @Override
        public Optional<MarketOperationLog> findById(long operationId) {
            return Optional.empty();
        }

        @Override
        public Optional<MarketOperationLog> findByRequestId(String requestId) {
            return Optional.empty();
        }

        @Override
        public List<MarketOperationLog> findByStatuses(List<MarketOperationStatus> statuses, int limit) {
            return Collections.emptyList();
        }
    }

    private static final class NoOpTradeRecordRepository implements MarketTradeRecordRepository {

        @Override
        public com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord save(
            com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord tradeRecord) {
            return tradeRecord;
        }

        @Override
        public List<com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord> findByOrderId(long orderId) {
            return Collections.emptyList();
        }
    }

    private static final class DirectMarketTransactionRunner implements MarketTransactionRunner {

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