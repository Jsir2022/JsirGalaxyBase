package com.jsirgalaxybase.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.module.ModuleManager;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.application.MarketRecoveryService;
import com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketService;
import com.jsirgalaxybase.modules.core.market.application.command.CancelBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CancelSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimMarketAssetCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationType;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderSide;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderStatus;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public class GalaxyBaseCommandTest {

    @Test
    public void sellCreateHandConsumesHeldStackAndDispatchesToSpotService() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        service.sellResult = buildSellCreateResult(41L, 7L, 5L, 12L);
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service,
            Collections.<MarketOperationLog>emptyList(), "test-server");
        FakePlayerContext playerContext = new FakePlayerContext("player-a", new ItemStack(Blocks.stone, 16, 0));
        RecordingReplySink reply = new RecordingReplySink();

        command.executeStandardizedSellCreate(playerContext, reply, institutionCoreModule,
            new GalaxyBaseCommand.HeldStandardizedItem("minecraft:stone:0", true, playerContext.getHeldItem().copy()),
            12L, 5L);

        assertNotNull(service.lastCreateSellCommand);
        assertEquals("player-a", service.lastCreateSellCommand.getPlayerRef());
        assertEquals("minecraft:stone:0", service.lastCreateSellCommand.getProductKey());
        assertEquals(5L, service.lastCreateSellCommand.getQuantity());
        assertEquals(12L, service.lastCreateSellCommand.getUnitPrice());
        assertNotNull(playerContext.getHeldItem());
        assertEquals(11, playerContext.getHeldItem().stackSize);
        assertEquals(1, playerContext.syncCount);
        assertTrue(reply.contains("卖单已创建"));
    }

    @Test
    public void sellCreateHandRestoresHeldStackWhenSpotServiceFails() {
        GalaxyBaseCommand command = new GalaxyBaseCommand(new ModuleManager());
        RecordingSpotMarketService service = new RecordingSpotMarketService();
        service.sellFailure = new MarketOperationException("simulated sell failure");
        RecordingInstitutionCoreModule institutionCoreModule = new RecordingInstitutionCoreModule(service,
            Collections.<MarketOperationLog>emptyList(), "test-server");
        FakePlayerContext playerContext = new FakePlayerContext("player-a", new ItemStack(Blocks.stone, 16, 0));
        RecordingReplySink reply = new RecordingReplySink();

        try {
            command.executeStandardizedSellCreate(playerContext, reply, institutionCoreModule,
                new GalaxyBaseCommand.HeldStandardizedItem("minecraft:stone:0", true,
                    playerContext.getHeldItem().copy()), 12L, 5L);
            fail("expected sell create to propagate failure");
        } catch (MarketOperationException expected) {
            assertTrue(expected.getMessage().contains("simulated sell failure"));
        }

        assertNotNull(playerContext.getHeldItem());
        assertEquals(16, playerContext.getHeldItem().stackSize);
        assertEquals(2, playerContext.syncCount);
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
        private final List<MarketOperationLog> recoveryResults;
        private final String sourceServerId;
        private int lastRecoveryLimit;

        private RecordingInstitutionCoreModule(RecordingSpotMarketService spotMarketService,
            List<MarketOperationLog> recoveryResults, String sourceServerId) {
            this.spotMarketService = spotMarketService;
            this.recoveryResults = recoveryResults;
            this.sourceServerId = sourceServerId;
        }

        @Override
        public StandardizedSpotMarketService getStandardizedSpotMarketService() {
            return spotMarketService;
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

        private CreateSellOrderCommand lastCreateSellCommand;
        private CreateBuyOrderCommand lastCreateBuyCommand;
        private RuntimeException sellFailure;
        private StandardizedSpotMarketService.CreateSellOrderResult sellResult;
        private StandardizedSpotMarketService.CreateBuyOrderResult buyResult;
        private List<MarketCustodyInventory> claimables = Collections.emptyList();

        private RecordingSpotMarketService() {
            super(new NoOpOrderBookRepository(), new NoOpCustodyRepository(), new NoOpOperationLogRepository(),
                new NoOpTradeRecordRepository(), new DirectMarketTransactionRunner(), null);
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
            throw new UnsupportedOperationException();
        }

        @Override
        public StandardizedSpotMarketService.CancelBuyOrderResult cancelBuyOrder(CancelBuyOrderCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StandardizedSpotMarketService.ClaimMarketAssetResult claimMarketAsset(ClaimMarketAssetCommand command) {
            throw new UnsupportedOperationException();
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