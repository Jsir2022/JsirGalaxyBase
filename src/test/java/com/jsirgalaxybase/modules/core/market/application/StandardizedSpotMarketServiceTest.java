package com.jsirgalaxybase.modules.core.market.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Test;

import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.command.FrozenBalanceCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.InternalTransferCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountStatus;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccountType;
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
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketClaimDeliveryPort;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public class StandardizedSpotMarketServiceTest {

    @Test
    public void createSellOrderMovesInventoryIntoEscrowAndOpensSellOrder() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository);

        depositInventory(service, "req-sell-create-deposit", "player-a", "minecraft:stone:0", 64L, true);

        StandardizedSpotMarketService.CreateSellOrderResult result = service.createSellOrder(new CreateSellOrderCommand(
            "req-sell-create", "player-a", "test-server", "minecraft:stone:0", 64L, true, 12L));

        assertEquals(MarketOrderSide.SELL, result.getOrder().getSide());
        assertEquals(MarketOrderStatus.OPEN, result.getOrder().getStatus());
        assertEquals(64L, result.getOrder().getOpenQuantity());
        assertEquals("minecraft:stone:0", result.getOrder().getProduct().getProductKey());
        assertEquals(MarketCustodyStatus.ESCROW_SELL, result.getCustody().getStatus());
        assertEquals(result.getOrder().getOrderId(), result.getCustody().getRelatedOrderId());
        assertEquals(MarketOperationStatus.COMPLETED, result.getOperationLog().getStatus());
        assertEquals(MarketOperationType.SELL_ORDER_CREATE, result.getOperationLog().getOperationType());
    }

    @Test
    public void cancelSellOrderMovesEscrowIntoAvailableAndCancelsOrder() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository);

        depositInventory(service, "req-sell-create-cancel-deposit", "player-a", "minecraft:stone:0", 32L, true);

        StandardizedSpotMarketService.CreateSellOrderResult created = service.createSellOrder(new CreateSellOrderCommand(
            "req-sell-create-cancel", "player-a", "test-server", "minecraft:stone:0", 32L, true, 15L));

        StandardizedSpotMarketService.CancelSellOrderResult cancelled = service.cancelSellOrder(
            new CancelSellOrderCommand("req-sell-cancel", "player-a", "test-server", created.getOrder().getOrderId()));

        assertEquals(MarketOrderStatus.CANCELLED, cancelled.getOrder().getStatus());
        assertEquals(0L, cancelled.getOrder().getOpenQuantity());
        assertEquals(MarketCustodyStatus.AVAILABLE, cancelled.getCustody().getStatus());
        assertEquals(MarketOperationStatus.COMPLETED, cancelled.getOperationLog().getStatus());
        assertEquals(MarketOperationType.SELL_ORDER_CANCEL, cancelled.getOperationLog().getOperationType());
    }

    @Test
    public void depositInventoryCreatesAvailableCustodyAndListsAvailableAssets() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository);

        StandardizedSpotMarketService.DepositInventoryResult result = depositInventory(service, "req-deposit", "player-a",
            "minecraft:stone:0", 24L, true);

        assertEquals(MarketCustodyStatus.AVAILABLE, result.getCustody().getStatus());
        assertEquals(24L, result.getCustody().getQuantity());
        assertEquals(24L, result.getTotalAvailableQuantity());
        assertEquals(1, service.listAvailableAssets("player-a", "minecraft:stone:0").size());
    }

    @Test
    public void createSellOrderRejectsWhenAvailableInventoryIsMissing() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository);

        try {
            service.createSellOrder(new CreateSellOrderCommand(
                "req-sell-without-available", "player-a", "test-server", "minecraft:stone:0", 8L, true, 12L));
            fail("expected sell create to require AVAILABLE inventory");
        } catch (MarketOperationException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("available"));
        }
    }

    @Test
    public void depositInventoryRejectsProductOutsideCatalogBoundary() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        StandardizedSpotMarketService service = new StandardizedSpotMarketService(orderRepository, custodyRepository,
            operationLogRepository, new FakeMarketTradeRecordRepository(), new DirectMarketTransactionRunner(), null,
            new StandardizedMarketProductParser(), new RejectingProductCatalog(), null);

        try {
            service.depositInventory(new DepositMarketInventoryCommand("req-reject-catalog", "player-a", "test-server",
                "minecraft:stone:0", 8L, true));
            fail("expected catalog boundary rejection");
        } catch (MarketOperationException expected) {
            assertTrue(expected.getMessage().contains("准入边界内"));
        }
    }

    @Test
    public void listOpenSellOrdersSortsByPriceThenTime() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository);

        depositInventory(service, "req-sell-sort-1-deposit", "player-a", "minecraft:stone:0", 16L, true);
        depositInventory(service, "req-sell-sort-2-deposit", "player-b", "minecraft:stone:0", 16L, true);
        depositInventory(service, "req-sell-sort-3-deposit", "player-c", "minecraft:stone:0", 16L, true);

        service.createSellOrder(new CreateSellOrderCommand(
            "req-sell-sort-1", "player-a", "test-server", "minecraft:stone:0", 16L, true, 20L));
        service.createSellOrder(new CreateSellOrderCommand(
            "req-sell-sort-2", "player-b", "test-server", "minecraft:stone:0", 16L, true, 10L));
        service.createSellOrder(new CreateSellOrderCommand(
            "req-sell-sort-3", "player-c", "test-server", "minecraft:stone:0", 16L, true, 10L));

        List<MarketOrder> orders = service.listOpenSellOrders("minecraft:stone:0");

        assertEquals(3, orders.size());
        assertEquals("player-b", orders.get(0).getOwnerPlayerRef());
        assertEquals("player-c", orders.get(1).getOwnerPlayerRef());
        assertEquals("player-a", orders.get(2).getOwnerPlayerRef());
        assertTrue(orders.get(0).getUnitPrice() <= orders.get(1).getUnitPrice());
        assertTrue(orders.get(1).getUnitPrice() <= orders.get(2).getUnitPrice());
    }

    @Test
    public void createSellOrderRejectsRequestIdSemanticsConflict() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository);

        depositInventory(service, "req-conflict-deposit", "player-a", "minecraft:stone:0", 32L, true);

        service.createSellOrder(new CreateSellOrderCommand("req-conflict", "player-a", "test-server",
            "minecraft:stone:0", 16L, true, 10L));

        try {
            service.createSellOrder(new CreateSellOrderCommand("req-conflict", "player-a", "test-server",
                "minecraft:stone:0", 16L, true, 12L));
            fail("expected request semantics conflict");
        } catch (MarketOperationException expected) {
            assertTrue(expected.getMessage().contains("unitPrice"));
        }
    }

    @Test
    public void cancelSellOrderRejectsRequestIdSemanticsConflict() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository);

        depositInventory(service, "req-sell-a-deposit", "player-a", "minecraft:stone:0", 32L, true);

        StandardizedSpotMarketService.CreateSellOrderResult first = service.createSellOrder(new CreateSellOrderCommand(
            "req-sell-a", "player-a", "test-server", "minecraft:stone:0", 16L, true, 10L));
        StandardizedSpotMarketService.CreateSellOrderResult second = service.createSellOrder(new CreateSellOrderCommand(
            "req-sell-b", "player-a", "test-server", "minecraft:stone:0", 16L, true, 10L));

        service.cancelSellOrder(new CancelSellOrderCommand("req-cancel-conflict", "player-a", "test-server",
            first.getOrder().getOrderId()));

        try {
            service.cancelSellOrder(new CancelSellOrderCommand("req-cancel-conflict", "player-a", "test-server",
                second.getOrder().getOrderId()));
            fail("expected cancel request semantics conflict");
        } catch (MarketOperationException expected) {
            assertTrue(expected.getMessage().contains("orderId"));
        }
    }

    @Test
    public void createBuyOrderFreezesFundsAndCancelReleasesRemainingReserve() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        FakeMarketTradeRecordRepository tradeRecordRepository = new FakeMarketTradeRecordRepository();
        FakeMarketSettlementFacade settlementFacade = new FakeMarketSettlementFacade();
        settlementFacade.registerPlayer("buyer");
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository, tradeRecordRepository, new DirectMarketTransactionRunner(), settlementFacade);

        StandardizedSpotMarketService.CreateBuyOrderResult created = service.createBuyOrder(new CreateBuyOrderCommand(
            "req-buy-create", "buyer", "test-server", "minecraft:stone:0", 10L, true, 100L));
        StandardizedSpotMarketService.CancelBuyOrderResult cancelled = service.cancelBuyOrder(
            new CancelBuyOrderCommand("req-buy-cancel", "buyer", "test-server", created.getOrder().getOrderId()));

        assertEquals(MarketOrderSide.BUY, created.getOrder().getSide());
        assertEquals(MarketOrderStatus.OPEN, created.getOrder().getStatus());
        assertEquals(1, settlementFacade.freezeCommands.size());
        assertEquals(1, settlementFacade.releaseCommands.size());
        assertEquals(created.getOrder().getReservedFunds(), settlementFacade.releaseCommands.get(0).getAmount());
        assertEquals(MarketOrderStatus.CANCELLED, cancelled.getOrder().getStatus());
        assertEquals(0L, cancelled.getOrder().getReservedFunds());
        assertEquals(created.getOrder().getReservedFunds(), cancelled.getReleasedFunds());
    }

    @Test
    public void matchingBuyAndSellGeneratesTradeAndClaimableAsset() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        FakeMarketTradeRecordRepository tradeRecordRepository = new FakeMarketTradeRecordRepository();
        FakeMarketSettlementFacade settlementFacade = new FakeMarketSettlementFacade();
        settlementFacade.registerPlayer("buyer");
        settlementFacade.registerPlayer("seller");
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository, tradeRecordRepository, new DirectMarketTransactionRunner(), settlementFacade);

        depositInventory(service, "req-sell-match-deposit", "seller", "minecraft:stone:0", 10L, true);

        StandardizedSpotMarketService.CreateBuyOrderResult buyCreated = service.createBuyOrder(new CreateBuyOrderCommand(
            "req-buy-match", "buyer", "test-server", "minecraft:stone:0", 10L, true, 100L));
        StandardizedSpotMarketService.CreateSellOrderResult sellCreated = service.createSellOrder(new CreateSellOrderCommand(
            "req-sell-match", "seller", "test-server", "minecraft:stone:0", 10L, true, 100L));

        MarketOrder persistedBuy = orderRepository.findById(buyCreated.getOrder().getOrderId()).get();

        assertEquals(MarketOrderStatus.FILLED, persistedBuy.getStatus());
        assertEquals(0L, persistedBuy.getReservedFunds());
        assertEquals(MarketOrderStatus.FILLED, sellCreated.getOrder().getStatus());
        assertEquals(MarketCustodyStatus.SETTLED, sellCreated.getCustody().getStatus());
        assertEquals(1, sellCreated.getTradeRecords().size());
        assertEquals(1, sellCreated.getClaimableAssets().size());
        assertEquals(MarketCustodyStatus.CLAIMABLE, sellCreated.getClaimableAssets().get(0).getStatus());
        assertEquals(1, tradeRecordRepository.records.size());
        assertEquals(1, settlementFacade.freezeCommands.size());
        assertEquals(2, settlementFacade.settleCommands.size());
        assertEquals(1, settlementFacade.taxCommands.size());
        assertEquals(1, settlementFacade.releaseCommands.size());
    }

    @Test
    public void recoveryServiceEscalatesIncompleteOperationsToRecoveryRequired() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();

        MarketOrder order = orderRepository.save(new MarketOrder(0L, MarketOrderSide.SELL, MarketOrderStatus.OPEN,
            "player-a", TestProducts.STONE, true, 10L, 16L, 16L, 0L, 0L, 1L, "test-server", Instant.now(),
            Instant.now()));
        MarketCustodyInventory custody = custodyRepository.save(new MarketCustodyInventory(0L, "player-a",
            TestProducts.STONE, true, 16L, MarketCustodyStatus.ESCROW_SELL, order.getOrderId(), 1L, "test-server",
            Instant.now(), Instant.now()));
        operationLogRepository.save(new MarketOperationLog(0L, "req-recovery", MarketOperationType.SELL_ORDER_CREATE,
            MarketOperationStatus.PROCESSING, "test-server", "player-a", "playerRef=player-a", order.getOrderId(),
            custody.getCustodyId(), 0L, "stuck", Instant.now(), Instant.now()));

        MarketRecoveryService recoveryService = new MarketRecoveryService(orderRepository, custodyRepository,
            operationLogRepository);
        List<MarketOperationLog> escalated = recoveryService.scanAndEscalateIncompleteOperations(10);

        assertEquals(1, escalated.size());
        assertEquals(MarketOperationStatus.RECOVERY_REQUIRED, escalated.get(0).getStatus());
        assertEquals(MarketOrderStatus.EXCEPTION, orderRepository.findById(order.getOrderId()).get().getStatus());
        assertEquals(MarketCustodyStatus.EXCEPTION, custodyRepository.findById(custody.getCustodyId()).get().getStatus());
    }

    @Test
    public void recoveryServiceReleasesFrozenFundsForIncompleteBuyOrder() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        FakeMarketTradeRecordRepository tradeRecordRepository = new FakeMarketTradeRecordRepository();
        FakeMarketSettlementFacade settlementFacade = new FakeMarketSettlementFacade();
        settlementFacade.registerPlayer("buyer");
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository, tradeRecordRepository, new FailAfterCallbackTransactionRunner(),
            settlementFacade);

        try {
            service.createBuyOrder(new CreateBuyOrderCommand("req-buy-recovery", "buyer", "test-server",
                "minecraft:stone:0", 5L, true, 100L));
            fail("expected simulated failure");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("post-commit"));
        }

        MarketOperationLog failedOperation = operationLogRepository.findByRequestId("req-buy-recovery").get();
        assertEquals(MarketOperationStatus.RECOVERY_REQUIRED, failedOperation.getStatus());

        MarketRecoveryService recoveryService = new MarketRecoveryService(orderRepository, custodyRepository,
            operationLogRepository, new DirectMarketTransactionRunner(), settlementFacade);
        List<MarketOperationLog> recovered = recoveryService.scanAndEscalateIncompleteOperations(10);

        assertEquals(1, recovered.size());
        assertEquals(MarketOperationStatus.COMPLETED, recovered.get(0).getStatus());
        assertEquals(1, settlementFacade.releaseCommands.size());
        assertEquals("req-buy-recovery:recovery-release", settlementFacade.releaseCommands.get(0).getRequestId());
        MarketOrder recoveredOrder = orderRepository.findById(recovered.get(0).getRelatedOrderId()).get();
        assertEquals(MarketOrderStatus.CANCELLED, recoveredOrder.getStatus());
        assertEquals(0L, recoveredOrder.getReservedFunds());
    }

    @Test
    public void depositPostCommitFailureLeavesAvailableCustodyRecoverableAndCompletesOnScan() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        StandardizedSpotMarketService service = new StandardizedSpotMarketService(orderRepository, custodyRepository,
            operationLogRepository, new FakeMarketTradeRecordRepository(), new FailAfterCallbackTransactionRunner(), null,
            new StandardizedMarketProductParser(), new PermissiveProductCatalog(), null);

        try {
            depositInventory(service, "req-deposit-recovery", "player-a", "minecraft:stone:0", 9L, true);
            fail("expected simulated failure");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("post-commit"));
        }

        MarketOperationLog failedOperation = operationLogRepository.findByRequestId("req-deposit-recovery").get();
        assertEquals(MarketOperationStatus.RECOVERY_REQUIRED, failedOperation.getStatus());
        assertTrue(failedOperation.getRelatedCustodyId() > 0L);

        MarketCustodyInventory custodyBeforeRecovery = custodyRepository.findById(failedOperation.getRelatedCustodyId()).get();
        assertEquals(MarketCustodyStatus.AVAILABLE, custodyBeforeRecovery.getStatus());

        MarketRecoveryService recoveryService = new MarketRecoveryService(orderRepository, custodyRepository,
            operationLogRepository);
        List<MarketOperationLog> recovered = recoveryService.scanAndEscalateIncompleteOperations(10);

        assertEquals(1, recovered.size());
        assertEquals(MarketOperationStatus.COMPLETED, recovered.get(0).getStatus());
        assertEquals(MarketCustodyStatus.AVAILABLE,
            custodyRepository.findById(failedOperation.getRelatedCustodyId()).get().getStatus());
    }

    @Test
    public void depositRecoveryMarksOperationFailedWhenNoCustodyWasPersisted() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();

        operationLogRepository.save(new MarketOperationLog(0L, "req-deposit-missing-custody",
            MarketOperationType.INVENTORY_DEPOSIT, MarketOperationStatus.RECOVERY_REQUIRED, "test-server", "player-a",
            "playerRef=player-a|sourceServerId=test-server|productKey=minecraft:stone:0|quantity=9|stackable=true|unitPrice=~|orderId=~|custodyId=~",
            MarketRecoveryMetadata.builder().put("mode", "inventory-deposit").putBoolean("safeRollbackIfMissingCustody", true)
                .build().toKey(),
            0L, 0L, 0L, "stuck deposit without custody", Instant.now(), Instant.now()));

        MarketRecoveryService recoveryService = new MarketRecoveryService(orderRepository, custodyRepository,
            operationLogRepository);
        List<MarketOperationLog> recovered = recoveryService.scanAndEscalateIncompleteOperations(10);

        assertEquals(1, recovered.size());
        assertEquals(MarketOperationStatus.FAILED, recovered.get(0).getStatus());
        assertFalse(operationLogRepository.findByRequestId("req-deposit-missing-custody").get().getMessage().isEmpty());
    }

    @Test
    public void claimMarketAssetMarksCustodyClaimedAndSupportsReplay() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        FakeMarketTradeRecordRepository tradeRecordRepository = new FakeMarketTradeRecordRepository();
        FakeMarketSettlementFacade settlementFacade = new FakeMarketSettlementFacade();
        settlementFacade.registerPlayer("buyer");
        settlementFacade.registerPlayer("seller");
        RecordingClaimDeliveryPort claimDeliveryPort = new RecordingClaimDeliveryPort();
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository, tradeRecordRepository, new DirectMarketTransactionRunner(), settlementFacade,
            claimDeliveryPort);

        depositInventory(service, "req-claim-sell-deposit", "seller", "minecraft:stone:0", 8L, true);
        service.createBuyOrder(new CreateBuyOrderCommand(
            "req-claim-buy", "buyer", "test-server", "minecraft:stone:0", 8L, true, 20L));

        StandardizedSpotMarketService.CreateSellOrderResult created = service.createSellOrder(new CreateSellOrderCommand(
            "req-claim-sell", "seller", "test-server", "minecraft:stone:0", 8L, true, 20L));

        StandardizedSpotMarketService.ClaimMarketAssetResult claimed = service.claimMarketAsset(
            new ClaimMarketAssetCommand("req-claim-asset", "buyer", "test-server", created.getClaimableAssets().get(0)
                .getCustodyId()));
        StandardizedSpotMarketService.ClaimMarketAssetResult replayed = service.claimMarketAsset(
            new ClaimMarketAssetCommand("req-claim-asset", "buyer", "test-server", created.getClaimableAssets().get(0)
                .getCustodyId()));

        assertEquals(MarketCustodyStatus.CLAIMED, claimed.getCustody().getStatus());
        assertEquals(MarketOperationStatus.COMPLETED, claimed.getOperationLog().getStatus());
        assertEquals(1, claimDeliveryPort.deliveries.size());
        assertEquals(claimed.getCustody().getCustodyId(), replayed.getCustody().getCustodyId());
    }

    @Test
    public void claimMarketAssetSafeFailureRestoresClaimableCustody() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();
        FakeMarketTradeRecordRepository tradeRecordRepository = new FakeMarketTradeRecordRepository();
        FakeMarketSettlementFacade settlementFacade = new FakeMarketSettlementFacade();
        settlementFacade.registerPlayer("buyer");
        settlementFacade.registerPlayer("seller");
        FailingClaimDeliveryPort claimDeliveryPort = new FailingClaimDeliveryPort(true);
        StandardizedSpotMarketService service = createService(orderRepository, custodyRepository,
            operationLogRepository, tradeRecordRepository, new DirectMarketTransactionRunner(), settlementFacade,
            claimDeliveryPort);

        depositInventory(service, "req-claim-safe-sell-deposit", "seller", "minecraft:stone:0", 4L, true);
        service.createBuyOrder(new CreateBuyOrderCommand(
            "req-claim-safe-buy", "buyer", "test-server", "minecraft:stone:0", 4L, true, 15L));

        StandardizedSpotMarketService.CreateSellOrderResult created = service.createSellOrder(new CreateSellOrderCommand(
            "req-claim-safe-sell", "seller", "test-server", "minecraft:stone:0", 4L, true, 15L));

        try {
            service.claimMarketAsset(new ClaimMarketAssetCommand("req-claim-safe", "buyer", "test-server",
                created.getClaimableAssets().get(0).getCustodyId()));
            fail("expected claim delivery failure");
        } catch (MarketClaimDeliveryException expected) {
            assertTrue(expected.getMessage().contains("claim delivery failed"));
        }

        MarketCustodyInventory restored = custodyRepository.findById(created.getClaimableAssets().get(0).getCustodyId()).get();
        MarketOperationLog failedOperation = operationLogRepository.findByRequestId("req-claim-safe").get();
        assertEquals(MarketCustodyStatus.CLAIMABLE, restored.getStatus());
        assertEquals(MarketOperationStatus.FAILED, failedOperation.getStatus());
    }

    @Test
    public void recoveryServiceCompletesClaimWhenDeliveryAlreadyHappened() {
        FakeMarketOrderBookRepository orderRepository = new FakeMarketOrderBookRepository();
        FakeMarketCustodyInventoryRepository custodyRepository = new FakeMarketCustodyInventoryRepository();
        FakeMarketOperationLogRepository operationLogRepository = new FakeMarketOperationLogRepository();

        MarketCustodyInventory custody = custodyRepository.save(new MarketCustodyInventory(0L, "seller",
            TestProducts.STONE, true, 8L, MarketCustodyStatus.CLAIMING, 11L, 0L, "test-server", Instant.now(),
            Instant.now()));
        MarketOperationLog claimOperation = operationLogRepository.save(new MarketOperationLog(0L,
            "req-claim-recovery", MarketOperationType.CLAIMABLE_ASSET_CLAIM, MarketOperationStatus.RECOVERY_REQUIRED,
            "test-server", "seller", "playerRef=seller|sourceServerId=test-server|productKey=~|quantity=~|stackable=~|unitPrice=~|orderId=~|custodyId="
                + custody.getCustodyId(),
            MarketRecoveryMetadata.builder().put("mode", "claim-asset").putBoolean("deliveryCompleted", true)
                .build().toKey(),
            custody.getRelatedOrderId(), custody.getCustodyId(), 0L, "delivery succeeded but finalize failed",
            Instant.now(), Instant.now()));

        MarketRecoveryService recoveryService = new MarketRecoveryService(orderRepository, custodyRepository,
            operationLogRepository);
        List<MarketOperationLog> recovered = recoveryService.scanAndEscalateIncompleteOperations(10);

        assertEquals(1, recovered.size());
        assertEquals(MarketOperationStatus.COMPLETED, recovered.get(0).getStatus());
        assertEquals(MarketCustodyStatus.CLAIMED,
            custodyRepository.findById(claimOperation.getRelatedCustodyId()).get().getStatus());
    }

    private static final class FakeMarketOrderBookRepository implements MarketOrderBookRepository {

        private final Map<Long, MarketOrder> ordersById = new HashMap<Long, MarketOrder>();
        private long nextId = 1L;

        @Override
        public MarketOrder save(MarketOrder order) {
            MarketOrder persisted = new MarketOrder(nextId++, order.getSide(), order.getStatus(), order.getOwnerPlayerRef(),
                order.getProduct(), order.isStackable(), order.getUnitPrice(), order.getOriginalQuantity(),
                order.getOpenQuantity(), order.getFilledQuantity(), order.getReservedFunds(), order.getCustodyId(),
                order.getSourceServerId(), order.getCreatedAt(), order.getUpdatedAt());
            ordersById.put(Long.valueOf(persisted.getOrderId()), persisted);
            return persisted;
        }

        @Override
        public MarketOrder update(MarketOrder order) {
            ordersById.put(Long.valueOf(order.getOrderId()), order);
            return order;
        }

        @Override
        public Optional<MarketOrder> findById(long orderId) {
            return Optional.ofNullable(ordersById.get(Long.valueOf(orderId)));
        }

        @Override
        public MarketOrder lockById(long orderId) {
            MarketOrder order = ordersById.get(Long.valueOf(orderId));
            if (order == null) {
                throw new MarketOperationException("market order not found: " + orderId);
            }
            return order;
        }

        @Override
        public List<MarketOrder> findOpenSellOrdersByProductKey(String productKey) {
            return filterOrders(productKey, MarketOrderSide.SELL);
        }

        @Override
        public List<MarketOrder> findOpenBuyOrdersByProductKey(String productKey) {
            return filterOrders(productKey, MarketOrderSide.BUY);
        }

        @Override
        public List<MarketOrder> findMatchingSellOrders(String productKey, long maxUnitPrice) {
            List<MarketOrder> matches = new ArrayList<MarketOrder>();
            for (MarketOrder order : ordersById.values()) {
                if (order.getSide() == MarketOrderSide.SELL && isLive(order)
                    && productKey.equals(order.getProduct().getProductKey()) && order.getUnitPrice() <= maxUnitPrice) {
                    matches.add(order);
                }
            }
            Collections.sort(matches, new Comparator<MarketOrder>() {

                @Override
                public int compare(MarketOrder left, MarketOrder right) {
                    int byPrice = Long.compare(left.getUnitPrice(), right.getUnitPrice());
                    if (byPrice != 0) {
                        return byPrice;
                    }
                    int byTime = left.getCreatedAt().compareTo(right.getCreatedAt());
                    if (byTime != 0) {
                        return byTime;
                    }
                    return Long.compare(left.getOrderId(), right.getOrderId());
                }
            });
            return matches;
        }

        @Override
        public List<MarketOrder> findMatchingBuyOrders(String productKey, long minUnitPrice) {
            List<MarketOrder> matches = new ArrayList<MarketOrder>();
            for (MarketOrder order : ordersById.values()) {
                if (order.getSide() == MarketOrderSide.BUY && isLive(order)
                    && productKey.equals(order.getProduct().getProductKey()) && order.getUnitPrice() >= minUnitPrice) {
                    matches.add(order);
                }
            }
            Collections.sort(matches, new Comparator<MarketOrder>() {

                @Override
                public int compare(MarketOrder left, MarketOrder right) {
                    int byPrice = Long.compare(right.getUnitPrice(), left.getUnitPrice());
                    if (byPrice != 0) {
                        return byPrice;
                    }
                    int byTime = left.getCreatedAt().compareTo(right.getCreatedAt());
                    if (byTime != 0) {
                        return byTime;
                    }
                    return Long.compare(left.getOrderId(), right.getOrderId());
                }
            });
            return matches;
        }

        private List<MarketOrder> filterOrders(String productKey, MarketOrderSide side) {
            List<MarketOrder> matches = new ArrayList<MarketOrder>();
            for (MarketOrder order : ordersById.values()) {
                if (order.getSide() == side && productKey.equals(order.getProduct().getProductKey()) && isLive(order)) {
                    matches.add(order);
                }
            }
            return matches;
        }

        private boolean isLive(MarketOrder order) {
            return order.getStatus() == MarketOrderStatus.OPEN || order.getStatus() == MarketOrderStatus.PARTIALLY_FILLED;
        }
    }

    private static final class FakeMarketCustodyInventoryRepository implements MarketCustodyInventoryRepository {

        private final Map<Long, MarketCustodyInventory> custodyById = new HashMap<Long, MarketCustodyInventory>();
        private long nextId = 1L;

        @Override
        public MarketCustodyInventory save(MarketCustodyInventory custodyInventory) {
            MarketCustodyInventory persisted = new MarketCustodyInventory(nextId++, custodyInventory.getOwnerPlayerRef(),
                custodyInventory.getProduct(), custodyInventory.isStackable(), custodyInventory.getQuantity(),
                custodyInventory.getStatus(), custodyInventory.getRelatedOrderId(), custodyInventory.getRelatedOperationId(),
                custodyInventory.getSourceServerId(), custodyInventory.getCreatedAt(), custodyInventory.getUpdatedAt());
            custodyById.put(Long.valueOf(persisted.getCustodyId()), persisted);
            return persisted;
        }

        @Override
        public MarketCustodyInventory update(MarketCustodyInventory custodyInventory) {
            custodyById.put(Long.valueOf(custodyInventory.getCustodyId()), custodyInventory);
            return custodyInventory;
        }

        @Override
        public Optional<MarketCustodyInventory> findById(long custodyId) {
            return Optional.ofNullable(custodyById.get(Long.valueOf(custodyId)));
        }

        @Override
        public MarketCustodyInventory lockById(long custodyId) {
            MarketCustodyInventory custody = custodyById.get(Long.valueOf(custodyId));
            if (custody == null) {
                throw new MarketOperationException("market custody not found: " + custodyId);
            }
            return custody;
        }

        @Override
        public Optional<MarketCustodyInventory> findEscrowSellByOrderId(long orderId) {
            for (MarketCustodyInventory custody : custodyById.values()) {
                if (custody.getRelatedOrderId() == orderId && custody.getStatus() == MarketCustodyStatus.ESCROW_SELL) {
                    return Optional.of(custody);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<MarketCustodyInventory> findByOwnerAndStatus(String ownerPlayerRef, MarketCustodyStatus status) {
            List<MarketCustodyInventory> matches = new ArrayList<MarketCustodyInventory>();
            for (MarketCustodyInventory custody : custodyById.values()) {
                if (ownerPlayerRef.equals(custody.getOwnerPlayerRef()) && custody.getStatus() == status) {
                    matches.add(custody);
                }
            }
            return matches;
        }

        @Override
        public List<MarketCustodyInventory> findByOwnerProductKeyAndStatuses(String ownerPlayerRef, String productKey,
            List<MarketCustodyStatus> statuses) {
            List<MarketCustodyInventory> matches = new ArrayList<MarketCustodyInventory>();
            for (MarketCustodyInventory custody : custodyById.values()) {
                if (ownerPlayerRef.equals(custody.getOwnerPlayerRef())
                    && productKey.equals(custody.getProduct().getProductKey())
                    && statuses.contains(custody.getStatus())) {
                    matches.add(custody);
                }
            }
            return matches;
        }
    }

    private static final class FakeMarketOperationLogRepository implements MarketOperationLogRepository {

        private final Map<Long, MarketOperationLog> operationsById = new HashMap<Long, MarketOperationLog>();
        private final Map<String, MarketOperationLog> operationsByRequestId = new HashMap<String, MarketOperationLog>();
        private long nextId = 1L;

        @Override
        public MarketOperationLog save(MarketOperationLog operationLog) {
            MarketOperationLog persisted = new MarketOperationLog(nextId++, operationLog.getRequestId(),
                operationLog.getOperationType(), operationLog.getStatus(), operationLog.getSourceServerId(),
                operationLog.getPlayerRef(), operationLog.getRequestSemanticsKey(),
                operationLog.getRecoveryMetadataKey(), operationLog.getRelatedOrderId(),
                operationLog.getRelatedCustodyId(), operationLog.getRelatedTradeId(), operationLog.getMessage(),
                operationLog.getCreatedAt(), operationLog.getUpdatedAt());
            operationsById.put(Long.valueOf(persisted.getOperationId()), persisted);
            operationsByRequestId.put(persisted.getRequestId(), persisted);
            return persisted;
        }

        @Override
        public MarketOperationLog update(MarketOperationLog operationLog) {
            operationsById.put(Long.valueOf(operationLog.getOperationId()), operationLog);
            operationsByRequestId.put(operationLog.getRequestId(), operationLog);
            return operationLog;
        }

        @Override
        public Optional<MarketOperationLog> findById(long operationId) {
            return Optional.ofNullable(operationsById.get(Long.valueOf(operationId)));
        }

        @Override
        public Optional<MarketOperationLog> findByRequestId(String requestId) {
            return Optional.ofNullable(operationsByRequestId.get(requestId));
        }

        @Override
        public List<MarketOperationLog> findByStatuses(List<MarketOperationStatus> statuses, int limit) {
            List<MarketOperationLog> matches = new ArrayList<MarketOperationLog>();
            for (MarketOperationLog operation : operationsById.values()) {
                if (statuses.contains(operation.getStatus())) {
                    matches.add(operation);
                }
            }
            if (matches.size() > limit) {
                return new ArrayList<MarketOperationLog>(matches.subList(0, limit));
            }
            return matches;
        }
    }

    private static final class FakeMarketTradeRecordRepository implements MarketTradeRecordRepository {

        private final List<MarketTradeRecord> records = new ArrayList<MarketTradeRecord>();
        private long nextId = 1L;

        @Override
        public MarketTradeRecord save(MarketTradeRecord tradeRecord) {
            MarketTradeRecord persisted = new MarketTradeRecord(nextId++, tradeRecord.getBuyerPlayerRef(),
                tradeRecord.getSellerPlayerRef(), tradeRecord.getProduct(), tradeRecord.isStackable(),
                tradeRecord.getUnitPrice(), tradeRecord.getQuantity(), tradeRecord.getFeeAmount(),
                tradeRecord.getBuyOrderId(), tradeRecord.getSellOrderId(), tradeRecord.getOperationId(),
                tradeRecord.getCreatedAt());
            records.add(persisted);
            return persisted;
        }

        @Override
        public List<MarketTradeRecord> findByOrderId(long orderId) {
            List<MarketTradeRecord> matches = new ArrayList<MarketTradeRecord>();
            for (MarketTradeRecord record : records) {
                if (record.getBuyOrderId() == orderId || record.getSellOrderId() == orderId) {
                    matches.add(record);
                }
            }
            return matches;
        }
    }

    private static final class FakeMarketSettlementFacade extends MarketSettlementFacade {

        private final Map<String, BankAccount> playerAccounts = new HashMap<String, BankAccount>();
        private final BankAccount taxAccount = newAccount(999L, "tax");
        private final List<FrozenBalanceCommand> freezeCommands = new ArrayList<FrozenBalanceCommand>();
        private final List<FrozenBalanceCommand> releaseCommands = new ArrayList<FrozenBalanceCommand>();
        private final List<InternalTransferCommand> settleCommands = new ArrayList<InternalTransferCommand>();
        private final List<InternalTransferCommand> taxCommands = new ArrayList<InternalTransferCommand>();
        private long nextAccountId = 1L;

        private void registerPlayer(String playerRef) {
            playerAccounts.put(playerRef, newAccount(nextAccountId++, playerRef));
        }

        @Override
        public BankAccount requirePlayerAccount(String playerRef) {
            BankAccount account = playerAccounts.get(playerRef);
            if (account == null) {
                throw new MarketOperationException("player bank account not found: " + playerRef);
            }
            return account;
        }

        @Override
        public BankAccount ensureTaxAccount() {
            return taxAccount;
        }

        @Override
        public BankPostingResult freezeBuyerFunds(FrozenBalanceCommand command) {
            freezeCommands.add(command);
            return emptyPostingResult();
        }

        @Override
        public BankPostingResult releaseBuyerFunds(FrozenBalanceCommand command) {
            releaseCommands.add(command);
            return emptyPostingResult();
        }

        @Override
        public BankPostingResult settleFromFrozenFunds(InternalTransferCommand command) {
            settleCommands.add(command);
            return emptyPostingResult();
        }

        @Override
        public BankPostingResult collectTax(InternalTransferCommand command) {
            taxCommands.add(command);
            return emptyPostingResult();
        }

        private BankAccount newAccount(long accountId, String ownerRef) {
            return new BankAccount(accountId, "ACC-" + accountId, BankAccountType.PLAYER, "player", ownerRef,
                "STARCOIN", 1_000_000L, 0L, BankAccountStatus.ACTIVE, 0L, ownerRef, "{}", Instant.now(),
                Instant.now());
        }

        private BankPostingResult emptyPostingResult() {
            return new BankPostingResult(null, Collections.<BankAccount>emptyList(), Collections.emptyList(), null);
        }
    }

    private static final class DirectMarketTransactionRunner implements MarketTransactionRunner {

        @Override
        public <T> T inTransaction(Supplier<T> callback) {
            return callback.get();
        }

        @Override
        public void inTransaction(Runnable callback) {
            callback.run();
        }
    }

    private static final class FailAfterCallbackTransactionRunner implements MarketTransactionRunner {

        @Override
        public <T> T inTransaction(Supplier<T> callback) {
            callback.get();
            throw new RuntimeException("simulated post-commit failure");
        }

        @Override
        public void inTransaction(Runnable callback) {
            callback.run();
            throw new RuntimeException("simulated post-commit failure");
        }
    }

    private static final class RecordingClaimDeliveryPort implements MarketClaimDeliveryPort {

        private final List<String> deliveries = new ArrayList<String>();

        @Override
        public void deliver(String deliveryRequestId, String playerRef, String sourceServerId,
            StandardizedMarketProduct product, boolean stackable, long quantity) {
            deliveries.add(deliveryRequestId + "|" + playerRef + "|" + sourceServerId + "|"
                + product.getProductKey() + "|" + quantity);
        }
    }

    private static final class FailingClaimDeliveryPort implements MarketClaimDeliveryPort {

        private final boolean safeToRestoreClaimable;

        private FailingClaimDeliveryPort(boolean safeToRestoreClaimable) {
            this.safeToRestoreClaimable = safeToRestoreClaimable;
        }

        @Override
        public void deliver(String deliveryRequestId, String playerRef, String sourceServerId,
            StandardizedMarketProduct product, boolean stackable, long quantity) {
            throw new MarketClaimDeliveryException("claim delivery failed for test", safeToRestoreClaimable);
        }
    }

    private static final class TestProducts {

        private static final StandardizedMarketProduct STONE = new StandardizedMarketProduct("minecraft:stone", 0);
    }

    private StandardizedSpotMarketService createService(FakeMarketOrderBookRepository orderRepository,
        FakeMarketCustodyInventoryRepository custodyRepository,
        FakeMarketOperationLogRepository operationLogRepository) {
        return new StandardizedSpotMarketService(orderRepository, custodyRepository, operationLogRepository,
            new FakeMarketTradeRecordRepository(), new DirectMarketTransactionRunner(), null,
            new StandardizedMarketProductParser(), new PermissiveProductCatalog(), null);
    }

    private StandardizedSpotMarketService createService(FakeMarketOrderBookRepository orderRepository,
        FakeMarketCustodyInventoryRepository custodyRepository,
        FakeMarketOperationLogRepository operationLogRepository,
        FakeMarketTradeRecordRepository tradeRecordRepository,
        MarketTransactionRunner transactionRunner,
        MarketSettlementFacade settlementFacade) {
        return new StandardizedSpotMarketService(orderRepository, custodyRepository, operationLogRepository,
            tradeRecordRepository, transactionRunner, settlementFacade, new StandardizedMarketProductParser(),
            new PermissiveProductCatalog(), null);
    }

    private StandardizedSpotMarketService createService(FakeMarketOrderBookRepository orderRepository,
        FakeMarketCustodyInventoryRepository custodyRepository,
        FakeMarketOperationLogRepository operationLogRepository,
        FakeMarketTradeRecordRepository tradeRecordRepository,
        MarketTransactionRunner transactionRunner,
        MarketSettlementFacade settlementFacade,
        MarketClaimDeliveryPort claimDeliveryPort) {
        return new StandardizedSpotMarketService(orderRepository, custodyRepository, operationLogRepository,
            tradeRecordRepository, transactionRunner, settlementFacade, new StandardizedMarketProductParser(),
            new PermissiveProductCatalog(), claimDeliveryPort);
    }

    private StandardizedSpotMarketService.DepositInventoryResult depositInventory(StandardizedSpotMarketService service,
        String requestId, String playerRef, String productKey, long quantity, boolean stackable) {
        return service.depositInventory(new DepositMarketInventoryCommand(requestId, playerRef, "test-server",
            productKey, quantity, stackable));
    }

    private static final class PermissiveProductCatalog implements StandardizedMarketProductCatalog {

        private final StandardizedMarketProductParser parser = new StandardizedMarketProductParser();

        @Override
        public StandardizedMarketCatalogVersion getCatalogVersion() {
            return new StandardizedMarketCatalogVersion("test-catalog-v1", "测试目录 v1");
        }

        @Override
        public String getCatalogSourceKey() {
            return "test-source";
        }

        @Override
        public String getCatalogSourceDescription() {
            return "Test Source";
        }

        @Override
        public StandardizedMarketAdmissionDecision evaluateProduct(String productKey) {
            StandardizedMarketProduct product = parser.parse(productKey);
            return new StandardizedMarketAdmissionDecision(getCatalogVersion(),
                new StandardizedMarketCatalogEntry(product, "test-category", "统一定义、统一计量、统一托管", "test"),
                true, StandardizedMarketAdmissionReason.CATALOG_ADMITTED, "admitted", getCatalogSourceKey(),
                getCatalogSourceDescription());
        }

        @Override
        public StandardizedMarketAdmissionDecision evaluateStack(net.minecraft.item.ItemStack stack) {
            String registryName = cpw.mods.fml.common.registry.GameRegistry.findUniqueIdentifierFor(stack.getItem()).toString();
            return evaluateProduct(registryName + ":" + stack.getItemDamage());
        }

        @Override
        public StandardizedMarketProduct requireTradableProduct(String productKey) {
            return evaluateProduct(productKey).requireProduct();
        }

        @Override
        public StandardizedMarketProduct requireTradableStack(net.minecraft.item.ItemStack stack) {
            return evaluateStack(stack).requireProduct();
        }
    }

    private static final class RejectingProductCatalog implements StandardizedMarketProductCatalog {

        @Override
        public StandardizedMarketCatalogVersion getCatalogVersion() {
            return new StandardizedMarketCatalogVersion("reject-catalog-v1", "拒绝目录 v1");
        }

        @Override
        public String getCatalogSourceKey() {
            return "reject-source";
        }

        @Override
        public String getCatalogSourceDescription() {
            return "Reject Source";
        }

        @Override
        public StandardizedMarketAdmissionDecision evaluateProduct(String productKey) {
            return new StandardizedMarketAdmissionDecision(getCatalogVersion(), null, false,
                StandardizedMarketAdmissionReason.CATALOG_BOUNDARY_REJECTED,
                "当前商品不在标准商品市场目录 reject-catalog-v1 的准入边界内；当前目录来源为 Reject Source。",
                getCatalogSourceKey(), getCatalogSourceDescription());
        }

        @Override
        public StandardizedMarketAdmissionDecision evaluateStack(net.minecraft.item.ItemStack stack) {
            return evaluateProduct("stack");
        }
    }
}