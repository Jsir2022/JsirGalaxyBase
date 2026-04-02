package com.jsirgalaxybase.modules.core.market.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.jsirgalaxybase.modules.core.banking.application.command.FrozenBalanceCommand;
import com.jsirgalaxybase.modules.core.banking.application.command.InternalTransferCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankBusinessType;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransactionType;
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
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketClaimDeliveryPort;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public class StandardizedSpotMarketService {

    private static final int MAKER_FEE_BASIS_POINTS = 20;
    private static final int TAKER_FEE_BASIS_POINTS = 80;

    private final MarketOrderBookRepository orderRepository;
    private final MarketCustodyInventoryRepository custodyRepository;
    private final MarketOperationLogRepository operationLogRepository;
    private final MarketTradeRecordRepository tradeRecordRepository;
    private final MarketTransactionRunner transactionRunner;
    private final MarketSettlementFacade settlementFacade;
    private final StandardizedMarketProductParser productParser;
    private final MarketClaimDeliveryPort claimDeliveryPort;

    public StandardizedSpotMarketService(MarketOrderBookRepository orderRepository,
        MarketCustodyInventoryRepository custodyRepository, MarketOperationLogRepository operationLogRepository) {
        this(orderRepository, custodyRepository, operationLogRepository, new NoOpMarketTradeRecordRepository(),
            new DirectMarketTransactionRunner(), null, new StandardizedMarketProductParser(), null);
    }

    public StandardizedSpotMarketService(MarketOrderBookRepository orderRepository,
        MarketCustodyInventoryRepository custodyRepository, MarketOperationLogRepository operationLogRepository,
        MarketTradeRecordRepository tradeRecordRepository, MarketTransactionRunner transactionRunner,
        MarketSettlementFacade settlementFacade) {
        this(orderRepository, custodyRepository, operationLogRepository, tradeRecordRepository, transactionRunner,
            settlementFacade, new StandardizedMarketProductParser(), null);
    }

    public StandardizedSpotMarketService(MarketOrderBookRepository orderRepository,
        MarketCustodyInventoryRepository custodyRepository, MarketOperationLogRepository operationLogRepository,
        MarketTradeRecordRepository tradeRecordRepository, MarketTransactionRunner transactionRunner,
        MarketSettlementFacade settlementFacade, StandardizedMarketProductParser productParser) {
        this(orderRepository, custodyRepository, operationLogRepository, tradeRecordRepository, transactionRunner,
            settlementFacade, productParser, null);
    }

    public StandardizedSpotMarketService(MarketOrderBookRepository orderRepository,
        MarketCustodyInventoryRepository custodyRepository, MarketOperationLogRepository operationLogRepository,
        MarketTradeRecordRepository tradeRecordRepository, MarketTransactionRunner transactionRunner,
        MarketSettlementFacade settlementFacade, StandardizedMarketProductParser productParser,
        MarketClaimDeliveryPort claimDeliveryPort) {
        this.orderRepository = requireNonNull(orderRepository, "orderRepository");
        this.custodyRepository = requireNonNull(custodyRepository, "custodyRepository");
        this.operationLogRepository = requireNonNull(operationLogRepository, "operationLogRepository");
        this.tradeRecordRepository = requireNonNull(tradeRecordRepository, "tradeRecordRepository");
        this.transactionRunner = requireNonNull(transactionRunner, "transactionRunner");
        this.settlementFacade = settlementFacade;
        this.productParser = requireNonNull(productParser, "productParser");
        this.claimDeliveryPort = claimDeliveryPort;
    }

    public CreateSellOrderResult createSellOrder(CreateSellOrderCommand command) {
        requireNonNull(command, "command");
        String requestId = requireText(command.getRequestId(), "requestId");
        String playerRef = requireText(command.getPlayerRef(), "playerRef");
        String sourceServerId = requireText(command.getSourceServerId(), "sourceServerId");
        requirePositive(command.getQuantity(), "quantity");
        requirePositive(command.getUnitPrice(), "unitPrice");
        ensureQuantityMatchesStackability(command.isStackable(), command.getQuantity());

        StandardizedMarketProduct product = productParser.parse(command.getProductKey());
        MarketRequestSemantics semantics = MarketRequestSemantics.forSellCreate(playerRef, sourceServerId,
            product.getProductKey(), command.getQuantity(), command.isStackable(), command.getUnitPrice());
        Optional<MarketOperationLog> existing = operationLogRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            ensureOperationReplayable(existing.get(), MarketOperationType.SELL_ORDER_CREATE, semantics);
            return replaySellCreate(existing.get());
        }

        Instant now = Instant.now();
        MarketOperationLog created = operationLogRepository.save(new MarketOperationLog(0L, requestId,
            MarketOperationType.SELL_ORDER_CREATE, MarketOperationStatus.CREATED, sourceServerId, playerRef,
            semantics.toKey(), 0L, 0L, 0L, "sell order requested", now, now));
        MarketOperationLog processing = operationLogRepository.update(created.withState(MarketOperationStatus.PROCESSING,
            0L, 0L, 0L, "creating standardized sell order", Instant.now()));

        MutableRefs refs = new MutableRefs();
        try {
            return transactionRunner.inTransaction(new Supplier<CreateSellOrderResult>() {

                @Override
                public CreateSellOrderResult get() {
                    if (settlementFacade != null) {
                        settlementFacade.requirePlayerAccount(playerRef);
                    }

                    MarketCustodyInventory custody = custodyRepository.save(new MarketCustodyInventory(0L, playerRef,
                        product, command.isStackable(), command.getQuantity(), MarketCustodyStatus.ESCROW_SELL, 0L,
                        processing.getOperationId(), sourceServerId, Instant.now(), Instant.now()));
                    refs.custodyId = custody.getCustodyId();
                    MarketOperationLog escrowed = operationLogRepository.update(processing.withState(
                        MarketOperationStatus.PROCESSING, 0L, refs.custodyId, 0L,
                        "sell inventory escrowed", Instant.now()));

                    MarketOrder order = orderRepository.save(new MarketOrder(0L, MarketOrderSide.SELL,
                        MarketOrderStatus.OPEN, playerRef, product, command.isStackable(), command.getUnitPrice(),
                        command.getQuantity(), command.getQuantity(), 0L, 0L, custody.getCustodyId(), sourceServerId,
                        Instant.now(), Instant.now()));
                    refs.orderId = order.getOrderId();
                    MarketCustodyInventory linkedCustody = custodyRepository.update(custody.withStateAndQuantity(
                        MarketCustodyStatus.ESCROW_SELL, custody.getQuantity(), refs.orderId, Instant.now()));
                    MatchExecutionSummary summary = matchSellOrder(order, linkedCustody, playerRef, sourceServerId,
                        escrowed.getOperationId());
                    refs.tradeId = summary.lastTradeId;
                    MarketOperationLog completed = operationLogRepository.update(escrowed.withState(
                        MarketOperationStatus.COMPLETED, summary.order.getOrderId(), summary.primaryCustody.getCustodyId(),
                        refs.tradeId, "sell order completed", Instant.now()));
                    return new CreateSellOrderResult(summary.order, summary.primaryCustody, completed,
                        summary.claimableAssets, summary.tradeRecords);
                }
            });
        } catch (RuntimeException exception) {
            handleOperationFailure(processing, refs, exception);
            throw exception;
        }
    }

    public CancelSellOrderResult cancelSellOrder(CancelSellOrderCommand command) {
        requireNonNull(command, "command");
        String requestId = requireText(command.getRequestId(), "requestId");
        String playerRef = requireText(command.getPlayerRef(), "playerRef");
        String sourceServerId = requireText(command.getSourceServerId(), "sourceServerId");
        if (command.getOrderId() <= 0L) {
            throw new MarketOperationException("orderId must be positive");
        }

        MarketRequestSemantics semantics = MarketRequestSemantics.forSellCancel(playerRef, sourceServerId,
            command.getOrderId());
        Optional<MarketOperationLog> existing = operationLogRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            ensureOperationReplayable(existing.get(), MarketOperationType.SELL_ORDER_CANCEL, semantics);
            return replaySellCancel(existing.get());
        }

        Instant now = Instant.now();
        MarketOperationLog created = operationLogRepository.save(new MarketOperationLog(0L, requestId,
            MarketOperationType.SELL_ORDER_CANCEL, MarketOperationStatus.CREATED, sourceServerId, playerRef,
            semantics.toKey(), command.getOrderId(), 0L, 0L, "sell order cancel requested", now, now));
        MarketOperationLog processing = operationLogRepository.update(created.withState(MarketOperationStatus.PROCESSING,
            command.getOrderId(), 0L, 0L, "cancelling sell order", Instant.now()));

        MutableRefs refs = new MutableRefs();
        refs.orderId = command.getOrderId();
        try {
            return transactionRunner.inTransaction(new Supplier<CancelSellOrderResult>() {

                @Override
                public CancelSellOrderResult get() {
                    MarketOrder order = orderRepository.lockById(command.getOrderId());
                    ensureSellOrderOwner(order, playerRef);
                    ensureCancellableOrder(order);

                    MarketCustodyInventory custody = custodyRepository.findEscrowSellByOrderId(command.getOrderId())
                        .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                            @Override
                            public MarketOperationException get() {
                                return new MarketOperationException(
                                    "escrow custody not found for order: " + command.getOrderId());
                            }
                        });
                    refs.custodyId = custody.getCustodyId();
                    MarketOperationLog identified = operationLogRepository.update(processing.withState(
                        MarketOperationStatus.PROCESSING, order.getOrderId(), refs.custodyId, 0L,
                        "sell order and custody identified", Instant.now()));

                    MarketOrder cancelledOrder = orderRepository.update(order.withLifecycle(MarketOrderStatus.CANCELLED,
                        0L, order.getFilledQuantity(), order.getReservedFunds(), Instant.now()));
                    MarketCustodyInventory claimableCustody = custodyRepository.update(custody.withStateAndQuantity(
                        MarketCustodyStatus.CLAIMABLE, custody.getQuantity(), cancelledOrder.getOrderId(),
                        Instant.now()));
                    MarketOperationLog completed = operationLogRepository.update(identified.withState(
                        MarketOperationStatus.COMPLETED, cancelledOrder.getOrderId(), claimableCustody.getCustodyId(),
                        0L, "sell order cancelled and inventory is claimable", Instant.now()));
                    return new CancelSellOrderResult(cancelledOrder, claimableCustody, completed);
                }
            });
        } catch (RuntimeException exception) {
            handleOperationFailure(processing, refs, exception);
            throw exception;
        }
    }

    public CreateBuyOrderResult createBuyOrder(CreateBuyOrderCommand command) {
        requireNonNull(command, "command");
        String requestId = requireText(command.getRequestId(), "requestId");
        String playerRef = requireText(command.getPlayerRef(), "playerRef");
        String sourceServerId = requireText(command.getSourceServerId(), "sourceServerId");
        requirePositive(command.getQuantity(), "quantity");
        requirePositive(command.getUnitPrice(), "unitPrice");
        ensureQuantityMatchesStackability(command.isStackable(), command.getQuantity());
        if (settlementFacade == null) {
            throw new MarketOperationException("market settlement facade is required for buy orders");
        }

        StandardizedMarketProduct product = productParser.parse(command.getProductKey());
        MarketRequestSemantics semantics = MarketRequestSemantics.forBuyCreate(playerRef, sourceServerId,
            product.getProductKey(), command.getQuantity(), command.isStackable(), command.getUnitPrice());
        Optional<MarketOperationLog> existing = operationLogRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            ensureOperationReplayable(existing.get(), MarketOperationType.BUY_ORDER_CREATE, semantics);
            return replayBuyCreate(existing.get());
        }

        Instant now = Instant.now();
        MarketOperationLog created = operationLogRepository.save(new MarketOperationLog(0L, requestId,
            MarketOperationType.BUY_ORDER_CREATE, MarketOperationStatus.CREATED, sourceServerId, playerRef,
            semantics.toKey(), 0L, 0L, 0L, "buy order requested", now, now));
        MarketOperationLog processing = operationLogRepository.update(created.withState(MarketOperationStatus.PROCESSING,
            0L, 0L, 0L, "freezing buy-side funds", Instant.now()));

        MutableRefs refs = new MutableRefs();
        long reservedFunds = calculateBuyerReservation(command.getUnitPrice(), command.getQuantity());
        try {
            return transactionRunner.inTransaction(new Supplier<CreateBuyOrderResult>() {

                @Override
                public CreateBuyOrderResult get() {
                    BankAccount buyerAccount = settlementFacade.requirePlayerAccount(playerRef);
                    settlementFacade.freezeBuyerFunds(new FrozenBalanceCommand(buildBankRequestId(requestId, "freeze"),
                        BankTransactionType.MARKET_FUNDS_FREEZE, BankBusinessType.MARKET_ORDER_FREEZE,
                        buyerAccount.getAccountId(), sourceServerId, "market", "market", playerRef, reservedFunds,
                        "freeze funds for buy order", buildOrderBusinessRef("buy-freeze", requestId),
                        "{\"marketRequestId\":\"" + requestId + "\"}"));
                    refs.recoveryMetadataKey = MarketRecoveryMetadata.builder()
                        .put("mode", "buy-freeze-recovery")
                        .put("releaseRequestId", buildBankRequestId(requestId, "recovery-release"))
                        .putLong("reservedFunds", reservedFunds)
                        .putBoolean("fundsFrozen", true)
                        .build().toKey();

                    MarketOrder order = orderRepository.save(new MarketOrder(0L, MarketOrderSide.BUY,
                        MarketOrderStatus.OPEN, playerRef, product, command.isStackable(), command.getUnitPrice(),
                        command.getQuantity(), command.getQuantity(), 0L, reservedFunds, 0L, sourceServerId,
                        Instant.now(), Instant.now()));
                    refs.orderId = order.getOrderId();
                    refs.recoveryMetadataKey = MarketRecoveryMetadata.parse(refs.recoveryMetadataKey).toBuilder()
                        .putLong("orderId", refs.orderId)
                        .build().toKey();
                    MarketOperationLog ordered = operationLogRepository.update(processing.withState(
                        MarketOperationStatus.PROCESSING, refs.orderId, 0L, 0L,
                        "buy order created and funds frozen", refs.recoveryMetadataKey, Instant.now()));
                    MatchExecutionSummary summary = matchBuyOrder(order, playerRef, sourceServerId,
                        ordered.getOperationId());
                    refs.tradeId = summary.lastTradeId;
                    MarketOperationLog completed = operationLogRepository.update(ordered.withState(
                        MarketOperationStatus.COMPLETED, summary.order.getOrderId(), 0L, refs.tradeId,
                        "buy order completed", refs.recoveryMetadataKey, Instant.now()));
                    return new CreateBuyOrderResult(summary.order, completed, summary.claimableAssets,
                        summary.tradeRecords);
                }
            });
        } catch (RuntimeException exception) {
            handleOperationFailure(processing, refs, exception);
            throw exception;
        }
    }

    public CancelBuyOrderResult cancelBuyOrder(CancelBuyOrderCommand command) {
        requireNonNull(command, "command");
        String requestId = requireText(command.getRequestId(), "requestId");
        String playerRef = requireText(command.getPlayerRef(), "playerRef");
        String sourceServerId = requireText(command.getSourceServerId(), "sourceServerId");
        if (command.getOrderId() <= 0L) {
            throw new MarketOperationException("orderId must be positive");
        }
        if (settlementFacade == null) {
            throw new MarketOperationException("market settlement facade is required for buy order cancellation");
        }

        MarketRequestSemantics semantics = MarketRequestSemantics.forBuyCancel(playerRef, sourceServerId,
            command.getOrderId());
        Optional<MarketOperationLog> existing = operationLogRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            ensureOperationReplayable(existing.get(), MarketOperationType.BUY_ORDER_CANCEL, semantics);
            return replayBuyCancel(existing.get());
        }

        Instant now = Instant.now();
        String releaseRequestId = buildBankRequestId(requestId, "release");
        MarketOperationLog created = operationLogRepository.save(new MarketOperationLog(0L, requestId,
            MarketOperationType.BUY_ORDER_CANCEL, MarketOperationStatus.CREATED, sourceServerId, playerRef,
            semantics.toKey(), MarketRecoveryMetadata.builder()
                .put("mode", "buy-cancel-recovery")
                .put("releaseRequestId", releaseRequestId)
                .putLong("orderId", command.getOrderId())
                .build().toKey(), command.getOrderId(), 0L, 0L, "buy order cancel requested", now, now));
        MarketOperationLog processing = operationLogRepository.update(created.withState(MarketOperationStatus.PROCESSING,
            command.getOrderId(), 0L, 0L, "releasing frozen funds for buy order", created.getRecoveryMetadataKey(),
            Instant.now()));

        MutableRefs refs = new MutableRefs();
        refs.orderId = command.getOrderId();
        try {
            return transactionRunner.inTransaction(new Supplier<CancelBuyOrderResult>() {

                @Override
                public CancelBuyOrderResult get() {
                    MarketOrder order = orderRepository.lockById(command.getOrderId());
                    ensureBuyOrderOwner(order, playerRef);
                    ensureCancellableOrder(order);
                    BankAccount buyerAccount = settlementFacade.requirePlayerAccount(playerRef);
                    if (order.getReservedFunds() > 0L) {
                        settlementFacade.releaseBuyerFunds(new FrozenBalanceCommand(
                            releaseRequestId, BankTransactionType.MARKET_FUNDS_RELEASE,
                            BankBusinessType.MARKET_ORDER_CANCEL_RELEASE, buyerAccount.getAccountId(), sourceServerId,
                            "market", "market", playerRef, order.getReservedFunds(),
                            "release frozen funds for cancelled buy order",
                            buildOrderBusinessRef("buy-release", String.valueOf(order.getOrderId())),
                            "{\"marketRequestId\":\"" + requestId + "\"}"));
                    }
                    refs.recoveryMetadataKey = MarketRecoveryMetadata.parse(processing.getRecoveryMetadataKey())
                        .toBuilder()
                        .putLong("releasedFunds", order.getReservedFunds())
                        .putBoolean("fundsReleased", true)
                        .build().toKey();
                    MarketOrder cancelledOrder = orderRepository.update(order.withLifecycle(MarketOrderStatus.CANCELLED,
                        0L, order.getFilledQuantity(), 0L, Instant.now()));
                    MarketOperationLog completed = operationLogRepository.update(processing.withState(
                        MarketOperationStatus.COMPLETED, cancelledOrder.getOrderId(), 0L, 0L,
                        "buy order cancelled and remaining frozen funds released", refs.recoveryMetadataKey,
                        Instant.now()));
                    return new CancelBuyOrderResult(cancelledOrder, completed);
                }
            });
        } catch (RuntimeException exception) {
            handleOperationFailure(processing, refs, exception);
            throw exception;
        }
    }

    public ClaimMarketAssetResult claimMarketAsset(ClaimMarketAssetCommand command) {
        requireNonNull(command, "command");
        String requestId = requireText(command.getRequestId(), "requestId");
        String playerRef = requireText(command.getPlayerRef(), "playerRef");
        String sourceServerId = requireText(command.getSourceServerId(), "sourceServerId");
        if (command.getCustodyId() <= 0L) {
            throw new MarketOperationException("custodyId must be positive");
        }
        if (claimDeliveryPort == null) {
            throw new MarketOperationException("market claim delivery port is required for claim operations");
        }

        MarketRequestSemantics semantics = MarketRequestSemantics.forClaim(playerRef, sourceServerId,
            command.getCustodyId());
        Optional<MarketOperationLog> existing = operationLogRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            ensureOperationReplayable(existing.get(), MarketOperationType.CLAIMABLE_ASSET_CLAIM, semantics);
            return replayClaim(existing.get());
        }

        Instant now = Instant.now();
        String initialMetadataKey = MarketRecoveryMetadata.builder()
            .put("mode", "claim-asset")
            .putBoolean("deliveryCompleted", false)
            .putBoolean("safeRestoreClaimable", true)
            .build().toKey();
        MarketOperationLog created = operationLogRepository.save(new MarketOperationLog(0L, requestId,
            MarketOperationType.CLAIMABLE_ASSET_CLAIM, MarketOperationStatus.CREATED, sourceServerId, playerRef,
            semantics.toKey(), initialMetadataKey, 0L, command.getCustodyId(), 0L, "claim asset requested", now,
            now));
        MarketOperationLog processing = operationLogRepository.update(created.withState(MarketOperationStatus.PROCESSING,
            0L, command.getCustodyId(), 0L, "claiming custody asset", initialMetadataKey, Instant.now()));

        MutableRefs refs = new MutableRefs();
        refs.custodyId = command.getCustodyId();
        refs.recoveryMetadataKey = initialMetadataKey;
        try {
            return transactionRunner.inTransaction(new Supplier<ClaimMarketAssetResult>() {

                @Override
                public ClaimMarketAssetResult get() {
                    MarketCustodyInventory custody = custodyRepository.lockById(command.getCustodyId());
                    ensureClaimableCustodyOwner(custody, playerRef);
                    MarketOperationLog identified = operationLogRepository.update(processing.withState(
                        MarketOperationStatus.PROCESSING, custody.getRelatedOrderId(), custody.getCustodyId(), 0L,
                        "claim custody identified", refs.recoveryMetadataKey, Instant.now()));
                    MarketCustodyInventory claimingCustody = custodyRepository.update(custody.withStateAndLinks(
                        MarketCustodyStatus.CLAIMING, custody.getQuantity(), custody.getRelatedOrderId(),
                        identified.getOperationId(), Instant.now()));
                    refs.custodyId = claimingCustody.getCustodyId();

                    try {
                        claimDeliveryPort.deliver(buildClaimDeliveryRequestId(requestId), playerRef, sourceServerId,
                            claimingCustody.getProduct(), claimingCustody.isStackable(), claimingCustody.getQuantity());
                    } catch (MarketClaimDeliveryException exception) {
                        if (exception.isSafeToRestoreClaimable()) {
                            MarketCustodyInventory restored = custodyRepository.update(claimingCustody.withStateAndLinks(
                                MarketCustodyStatus.CLAIMABLE, claimingCustody.getQuantity(),
                                claimingCustody.getRelatedOrderId(), identified.getOperationId(), Instant.now()));
                            operationLogRepository.update(identified.withState(MarketOperationStatus.FAILED,
                                restored.getRelatedOrderId(), restored.getCustodyId(), 0L, exception.getMessage(),
                                refs.recoveryMetadataKey, Instant.now()));
                        }
                        throw exception;
                    }

                    refs.recoveryMetadataKey = MarketRecoveryMetadata.parse(refs.recoveryMetadataKey).toBuilder()
                        .putBoolean("deliveryCompleted", true)
                        .build().toKey();
                    MarketCustodyInventory claimedCustody = custodyRepository.update(claimingCustody.withStateAndLinks(
                        MarketCustodyStatus.CLAIMED, claimingCustody.getQuantity(), claimingCustody.getRelatedOrderId(),
                        identified.getOperationId(), Instant.now()));
                    MarketOperationLog completed = operationLogRepository.update(identified.withState(
                        MarketOperationStatus.COMPLETED, claimedCustody.getRelatedOrderId(),
                        claimedCustody.getCustodyId(), 0L, "claim custody delivered to player", refs.recoveryMetadataKey,
                        Instant.now()));
                    return new ClaimMarketAssetResult(claimedCustody, completed);
                }
            });
        } catch (MarketClaimDeliveryException exception) {
            if (!exception.isSafeToRestoreClaimable()) {
                handleOperationFailure(processing, refs, exception);
            }
            throw exception;
        } catch (RuntimeException exception) {
            handleOperationFailure(processing, refs, exception);
            throw exception;
        }
    }

    public List<MarketOrder> listOpenSellOrders(String productKey) {
        StandardizedMarketProduct product = productParser.parse(productKey);
        List<MarketOrder> openOrders = new ArrayList<MarketOrder>(
            orderRepository.findOpenSellOrdersByProductKey(product.getProductKey()));
        Collections.sort(openOrders, new Comparator<MarketOrder>() {

            @Override
            public int compare(MarketOrder left, MarketOrder right) {
                int priceCompare = Long.compare(left.getUnitPrice(), right.getUnitPrice());
                if (priceCompare != 0) {
                    return priceCompare;
                }
                int timeCompare = left.getCreatedAt().compareTo(right.getCreatedAt());
                if (timeCompare != 0) {
                    return timeCompare;
                }
                return Long.compare(left.getOrderId(), right.getOrderId());
            }
        });
        return openOrders;
    }

    public List<MarketCustodyInventory> listClaimableAssets(String playerRef) {
        return custodyRepository.findByOwnerAndStatus(requireText(playerRef, "playerRef"), MarketCustodyStatus.CLAIMABLE);
    }

    private CreateSellOrderResult replaySellCreate(MarketOperationLog existing) {
        MarketOrder order = orderRepository.findById(existing.getRelatedOrderId())
            .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                @Override
                public MarketOperationException get() {
                    return new MarketOperationException("existing create request lost its market order");
                }
            });
        MarketCustodyInventory custody = custodyRepository.findById(existing.getRelatedCustodyId())
            .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                @Override
                public MarketOperationException get() {
                    return new MarketOperationException("existing create request lost its custody record");
                }
            });
        return new CreateSellOrderResult(order, custody, existing, Collections.<MarketCustodyInventory>emptyList(),
            tradeRecordRepository.findByOrderId(order.getOrderId()));
    }

    private CancelSellOrderResult replaySellCancel(MarketOperationLog existing) {
        MarketOrder order = orderRepository.findById(existing.getRelatedOrderId())
            .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                @Override
                public MarketOperationException get() {
                    return new MarketOperationException("existing cancel request lost its market order");
                }
            });
        MarketCustodyInventory custody = custodyRepository.findById(existing.getRelatedCustodyId())
            .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                @Override
                public MarketOperationException get() {
                    return new MarketOperationException("existing cancel request lost its custody record");
                }
            });
        return new CancelSellOrderResult(order, custody, existing);
    }

    private CreateBuyOrderResult replayBuyCreate(MarketOperationLog existing) {
        MarketOrder order = orderRepository.findById(existing.getRelatedOrderId())
            .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                @Override
                public MarketOperationException get() {
                    return new MarketOperationException("existing buy request lost its market order");
                }
            });
        return new CreateBuyOrderResult(order, existing, listClaimableAssets(existing.getPlayerRef()),
            tradeRecordRepository.findByOrderId(order.getOrderId()));
    }

    private CancelBuyOrderResult replayBuyCancel(MarketOperationLog existing) {
        MarketOrder order = orderRepository.findById(existing.getRelatedOrderId())
            .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                @Override
                public MarketOperationException get() {
                    return new MarketOperationException("existing buy cancel request lost its market order");
                }
            });
        return new CancelBuyOrderResult(order, existing);
    }

    private ClaimMarketAssetResult replayClaim(MarketOperationLog existing) {
        MarketCustodyInventory custody = custodyRepository.findById(existing.getRelatedCustodyId())
            .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                @Override
                public MarketOperationException get() {
                    return new MarketOperationException("existing claim request lost its custody record");
                }
            });
        return new ClaimMarketAssetResult(custody, existing);
    }

    private MatchExecutionSummary matchSellOrder(MarketOrder order, MarketCustodyInventory custody, String sellerPlayerRef,
        String sourceServerId, long parentOperationId) {
        if (settlementFacade == null || order.getOpenQuantity() <= 0L) {
            return new MatchExecutionSummary(order, custody, Collections.<MarketCustodyInventory>emptyList(),
                Collections.<MarketTradeRecord>emptyList(), 0L);
        }

        List<MarketOrder> candidates = orderRepository.findMatchingBuyOrders(order.getProduct().getProductKey(),
            order.getUnitPrice());
        return executeMatching(order, custody, candidates, false, sellerPlayerRef, sourceServerId, parentOperationId);
    }

    private MatchExecutionSummary matchBuyOrder(MarketOrder order, String buyerPlayerRef, String sourceServerId,
        long parentOperationId) {
        if (settlementFacade == null || order.getOpenQuantity() <= 0L) {
            return new MatchExecutionSummary(order, null, Collections.<MarketCustodyInventory>emptyList(),
                Collections.<MarketTradeRecord>emptyList(), 0L);
        }

        List<MarketOrder> candidates = orderRepository.findMatchingSellOrders(order.getProduct().getProductKey(),
            order.getUnitPrice());
        return executeMatching(order, null, candidates, true, buyerPlayerRef, sourceServerId, parentOperationId);
    }

    private MatchExecutionSummary executeMatching(MarketOrder incomingOrder, MarketCustodyInventory incomingCustody,
        List<MarketOrder> restingCandidates, boolean buyIsIncoming, String initiatorPlayerRef, String sourceServerId,
        long parentOperationId) {
        MarketOrder updatedIncomingOrder = incomingOrder;
        MarketCustodyInventory updatedIncomingCustody = incomingCustody;
        List<MarketCustodyInventory> claimableAssets = new ArrayList<MarketCustodyInventory>();
        List<MarketTradeRecord> tradeRecords = new ArrayList<MarketTradeRecord>();
        long lastTradeId = 0L;

        for (MarketOrder candidate : restingCandidates) {
            if (updatedIncomingOrder.getOpenQuantity() <= 0L) {
                break;
            }

            MarketOrder lockedResting = orderRepository.lockById(candidate.getOrderId());
            if (!isOrderMatchable(lockedResting, updatedIncomingOrder, buyIsIncoming)) {
                continue;
            }

            MarketCustodyInventory restingCustody = null;
            if (lockedResting.getSide() == MarketOrderSide.SELL) {
                restingCustody = custodyRepository.findEscrowSellByOrderId(lockedResting.getOrderId())
                    .orElseThrow(new java.util.function.Supplier<MarketOperationException>() {

                        @Override
                        public MarketOperationException get() {
                            return new MarketOperationException("matching sell custody not found for order: "
                                + lockedResting.getOrderId());
                        }
                    });
            }

            long tradeQuantity = Math.min(updatedIncomingOrder.getOpenQuantity(), lockedResting.getOpenQuantity());
            long tradePrice = lockedResting.getUnitPrice();
            long grossAmount = tradePrice * tradeQuantity;
            long buyerFee = calculateFee(grossAmount, buyIsIncoming ? TAKER_FEE_BASIS_POINTS : MAKER_FEE_BASIS_POINTS);
            long sellerFee = calculateFee(grossAmount, buyIsIncoming ? MAKER_FEE_BASIS_POINTS : TAKER_FEE_BASIS_POINTS);

            MarketOrder buyOrder = buyIsIncoming ? updatedIncomingOrder : lockedResting;
            MarketOrder sellOrder = buyIsIncoming ? lockedResting : updatedIncomingOrder;
            MarketCustodyInventory sellCustody = buyIsIncoming ? restingCustody : updatedIncomingCustody;
            if (sellCustody == null) {
                throw new MarketOperationException("sell custody missing for incoming sell order");
            }

            BankAccount buyerAccount = settlementFacade.requirePlayerAccount(buyOrder.getOwnerPlayerRef());
            BankAccount sellerAccount = settlementFacade.requirePlayerAccount(sellOrder.getOwnerPlayerRef());
            BankAccount taxAccount = settlementFacade.ensureTaxAccount();

            long buyerOutflow = grossAmount + buyerFee;
            if (buyOrder.getReservedFunds() < buyerOutflow) {
                throw new MarketOperationException("buy order reserved funds are insufficient for settlement");
            }

            String matchRequestPrefix = buildMatchRequestPrefix(parentOperationId, buyOrder.getOrderId(),
                sellOrder.getOrderId(), tradeRecords.size() + 1);
            settlementFacade.settleFromFrozenFunds(new InternalTransferCommand(matchRequestPrefix + ":gross",
                BankTransactionType.MARKET_SETTLEMENT_FROM_FROZEN, BankBusinessType.MARKET_ORDER_SETTLEMENT,
                buyerAccount.getAccountId(), sellerAccount.getAccountId(), sourceServerId, "market", "market",
                buyOrder.getOwnerPlayerRef(), grossAmount, "gross settlement for matched trade",
                buildOrderBusinessRef("match-gross", buyOrder.getOrderId() + ":" + sellOrder.getOrderId()),
                "{\"operationId\":" + parentOperationId + "}"));

            if (buyerFee > 0L) {
                settlementFacade.settleFromFrozenFunds(new InternalTransferCommand(matchRequestPrefix + ":buyer-tax",
                    BankTransactionType.MARKET_SETTLEMENT_FROM_FROZEN, BankBusinessType.MARKET_TAX_INCOME,
                    buyerAccount.getAccountId(), taxAccount.getAccountId(), sourceServerId, "market", "market",
                    buyOrder.getOwnerPlayerRef(), buyerFee, "buyer fee settlement for matched trade",
                    buildOrderBusinessRef("match-buyer-tax", buyOrder.getOrderId() + ":" + sellOrder.getOrderId()),
                    "{\"operationId\":" + parentOperationId + "}"));
            }

            if (sellerFee > 0L) {
                settlementFacade.collectTax(new InternalTransferCommand(matchRequestPrefix + ":seller-tax",
                    BankTransactionType.MARKET_TAX_COLLECTION, BankBusinessType.MARKET_TAX_INCOME,
                    sellerAccount.getAccountId(), taxAccount.getAccountId(), sourceServerId, "market", "market",
                    sellOrder.getOwnerPlayerRef(), sellerFee, "seller fee settlement for matched trade",
                    buildOrderBusinessRef("match-seller-tax", buyOrder.getOrderId() + ":" + sellOrder.getOrderId()),
                    "{\"operationId\":" + parentOperationId + "}"));
            }

            MarketOperationLog matchOperation = operationLogRepository.save(new MarketOperationLog(0L,
                matchRequestPrefix, MarketOperationType.MATCH_EXECUTION, MarketOperationStatus.COMPLETED,
                sourceServerId, initiatorPlayerRef,
                "match|buyOrderId=" + buyOrder.getOrderId() + "|sellOrderId=" + sellOrder.getOrderId()
                    + "|quantity=" + tradeQuantity + "|unitPrice=" + tradePrice,
                null, buyOrder.getOrderId(), sellOrder.getCustodyId(), 0L, "matched trade executed", Instant.now(),
                Instant.now()));

            MarketTradeRecord tradeRecord = tradeRecordRepository.save(new MarketTradeRecord(0L,
                buyOrder.getOwnerPlayerRef(), sellOrder.getOwnerPlayerRef(), buyOrder.getProduct(),
                buyOrder.isStackable(), tradePrice, tradeQuantity, buyerFee + sellerFee, buyOrder.getOrderId(),
                sellOrder.getOrderId(), matchOperation.getOperationId(), Instant.now()));
            lastTradeId = tradeRecord.getTradeId();

            MarketOrder updatedBuyOrder = orderRepository.update(updateOrderForTrade(buyOrder, tradeQuantity,
                buyerOutflow));
            MarketOrder updatedSellOrder = orderRepository.update(updateOrderForTrade(sellOrder, tradeQuantity, 0L));
            MarketCustodyInventory updatedSellCustody = custodyRepository.update(updateSellCustodyForTrade(sellCustody,
                updatedSellOrder.getOrderId(), tradeQuantity));
            MarketCustodyInventory buyerClaimable = custodyRepository.save(new MarketCustodyInventory(0L,
                updatedBuyOrder.getOwnerPlayerRef(), updatedBuyOrder.getProduct(), updatedBuyOrder.isStackable(),
                tradeQuantity, MarketCustodyStatus.CLAIMABLE, updatedBuyOrder.getOrderId(), matchOperation.getOperationId(),
                sourceServerId, Instant.now(), Instant.now()));

            MarketOperationLog completedMatch = operationLogRepository.update(matchOperation.withState(
                MarketOperationStatus.COMPLETED, buyOrder.getOrderId(), updatedSellCustody.getCustodyId(),
                tradeRecord.getTradeId(), "matched trade persisted", Instant.now()));
            lastTradeId = completedMatch.getRelatedTradeId();
            MarketOrder finalBuyOrder = releaseFilledBuyOrderIfNeeded(updatedBuyOrder, sourceServerId, parentOperationId,
                tradeRecords.size() + 1);

            tradeRecords.add(tradeRecord);
            claimableAssets.add(buyerClaimable);

            if (buyIsIncoming) {
                updatedIncomingOrder = finalBuyOrder;
                updatedIncomingCustody = null;
            } else {
                updatedIncomingOrder = updatedSellOrder;
                updatedIncomingCustody = updatedSellCustody;
            }
        }

        return new MatchExecutionSummary(updatedIncomingOrder, updatedIncomingCustody, claimableAssets, tradeRecords,
            lastTradeId);
    }

    private MarketOrder releaseFilledBuyOrderIfNeeded(MarketOrder buyOrder, String sourceServerId, long parentOperationId,
        int sequence) {
        if (buyOrder.getSide() != MarketOrderSide.BUY || buyOrder.getOpenQuantity() > 0L || buyOrder.getReservedFunds() <= 0L) {
            return buyOrder;
        }

        BankAccount buyerAccount = settlementFacade.requirePlayerAccount(buyOrder.getOwnerPlayerRef());
        settlementFacade.releaseBuyerFunds(new FrozenBalanceCommand(
            buildFilledBuyReleaseRequestId(parentOperationId, buyOrder.getOrderId()),
            BankTransactionType.MARKET_FUNDS_RELEASE, BankBusinessType.MARKET_ORDER_CANCEL_RELEASE,
            buyerAccount.getAccountId(), sourceServerId, "market", "market", buyOrder.getOwnerPlayerRef(),
            buyOrder.getReservedFunds(), "release surplus frozen funds for fully filled buy order",
            buildOrderBusinessRef("buy-surplus-release", String.valueOf(buyOrder.getOrderId())),
            "{\"operationId\":" + parentOperationId + "}"));
        return orderRepository.update(buyOrder.withLifecycle(MarketOrderStatus.FILLED, 0L, buyOrder.getFilledQuantity(),
            0L, Instant.now()));
    }

    private MarketOrder updateOrderForTrade(MarketOrder order, long tradedQuantity, long reservedFundsDecrease) {
        long newOpenQuantity = order.getOpenQuantity() - tradedQuantity;
        long newFilledQuantity = order.getFilledQuantity() + tradedQuantity;
        long newReservedFunds = order.getReservedFunds() - reservedFundsDecrease;
        if (newOpenQuantity < 0L || newReservedFunds < 0L) {
            throw new MarketOperationException("order trade update would underflow order state");
        }
        MarketOrderStatus newStatus = deriveOrderStatus(order.getOriginalQuantity(), newOpenQuantity, newFilledQuantity);
        return order.withLifecycle(newStatus, newOpenQuantity, newFilledQuantity, newReservedFunds, Instant.now());
    }

    private MarketCustodyInventory updateSellCustodyForTrade(MarketCustodyInventory custody, long orderId,
        long tradedQuantity) {
        long remainingQuantity = custody.getQuantity() - tradedQuantity;
        if (remainingQuantity < 0L) {
            throw new MarketOperationException("sell custody trade update would underflow quantity");
        }
        MarketCustodyStatus status = remainingQuantity == 0L ? MarketCustodyStatus.SETTLED : MarketCustodyStatus.ESCROW_SELL;
        return custody.withStateAndQuantity(status, remainingQuantity, orderId, Instant.now());
    }

    private boolean isOrderMatchable(MarketOrder restingOrder, MarketOrder incomingOrder, boolean buyIsIncoming) {
        if (restingOrder.getOpenQuantity() <= 0L) {
            return false;
        }
        if (buyIsIncoming) {
            return restingOrder.getSide() == MarketOrderSide.SELL
                && restingOrder.getUnitPrice() <= incomingOrder.getUnitPrice()
                && restingOrder.getStatus() != MarketOrderStatus.CANCELLED
                && restingOrder.getStatus() != MarketOrderStatus.FILLED
                && restingOrder.getStatus() != MarketOrderStatus.EXCEPTION;
        }
        return restingOrder.getSide() == MarketOrderSide.BUY
            && restingOrder.getUnitPrice() >= incomingOrder.getUnitPrice()
            && restingOrder.getStatus() != MarketOrderStatus.CANCELLED
            && restingOrder.getStatus() != MarketOrderStatus.FILLED
            && restingOrder.getStatus() != MarketOrderStatus.EXCEPTION;
    }

    private void ensureOperationReplayable(MarketOperationLog existing, MarketOperationType expectedType,
        MarketRequestSemantics requestedSemantics) {
        if (existing.getOperationType() != expectedType) {
            throw new MarketOperationException("requestId conflicts with another market operation type");
        }
        MarketRequestSemantics recordedSemantics = MarketRequestSemantics.parse(existing.getRequestSemanticsKey());
        List<String> mismatches = recordedSemantics.diff(requestedSemantics);
        if (!mismatches.isEmpty()) {
            throw new MarketOperationException(
                "requestId conflicts with existing market operation: fields=" + String.join(", ", mismatches));
        }
        if (existing.getStatus() != MarketOperationStatus.COMPLETED) {
            throw new MarketOperationException("requestId already exists but market operation is not safely replayable");
        }
    }

    private void handleOperationFailure(MarketOperationLog operation, MutableRefs refs, RuntimeException exception) {
        if (refs.orderId > 0L) {
            Optional<MarketOrder> existingOrder = orderRepository.findById(refs.orderId);
            if (existingOrder.isPresent() && existingOrder.get().getStatus() != MarketOrderStatus.EXCEPTION) {
                MarketOrder order = existingOrder.get();
                orderRepository.update(order.withLifecycle(MarketOrderStatus.EXCEPTION, order.getOpenQuantity(),
                    order.getFilledQuantity(), order.getReservedFunds(), Instant.now()));
            }
        }
        if (refs.custodyId > 0L) {
            Optional<MarketCustodyInventory> existingCustody = custodyRepository.findById(refs.custodyId);
            if (existingCustody.isPresent() && existingCustody.get().getStatus() != MarketCustodyStatus.EXCEPTION
                && existingCustody.get().getStatus() != MarketCustodyStatus.CLAIMABLE
                && existingCustody.get().getStatus() != MarketCustodyStatus.CLAIMED) {
                MarketCustodyInventory custody = existingCustody.get();
                custodyRepository.update(custody.withStateAndLinks(MarketCustodyStatus.EXCEPTION,
                    custody.getQuantity(), refs.orderId, custody.getRelatedOperationId(), Instant.now()));
            }
        }

        MarketOperationStatus failureStatus = refs.hasRecoveryState() ? MarketOperationStatus.RECOVERY_REQUIRED
            : MarketOperationStatus.FAILED;
        operationLogRepository.update(operation.withState(failureStatus, refs.orderId, refs.custodyId, refs.tradeId,
            exception.getMessage(), refs.recoveryMetadataKey, Instant.now()));
    }

    private void ensureClaimableCustodyOwner(MarketCustodyInventory custody, String playerRef) {
        if (!playerRef.equals(custody.getOwnerPlayerRef())) {
            throw new MarketOperationException("custody is not owned by playerRef");
        }
        if (custody.getStatus() == MarketCustodyStatus.CLAIMED) {
            throw new MarketOperationException("custody has already been claimed");
        }
        if (custody.getStatus() != MarketCustodyStatus.CLAIMABLE) {
            throw new MarketOperationException("custody is not claimable in current status");
        }
    }

    private void ensureQuantityMatchesStackability(boolean stackable, long quantity) {
        if (!stackable && quantity != 1L) {
            throw new MarketOperationException("non-stackable orders must use quantity=1");
        }
    }

    private void ensureSellOrderOwner(MarketOrder order, String playerRef) {
        if (order.getSide() != MarketOrderSide.SELL) {
            throw new MarketOperationException("order is not a sell order");
        }
        if (!playerRef.equals(order.getOwnerPlayerRef())) {
            throw new MarketOperationException("order is not owned by playerRef");
        }
    }

    private void ensureBuyOrderOwner(MarketOrder order, String playerRef) {
        if (order.getSide() != MarketOrderSide.BUY) {
            throw new MarketOperationException("order is not a buy order");
        }
        if (!playerRef.equals(order.getOwnerPlayerRef())) {
            throw new MarketOperationException("order is not owned by playerRef");
        }
    }

    private void ensureCancellableOrder(MarketOrder order) {
        if (order.getStatus() != MarketOrderStatus.OPEN && order.getStatus() != MarketOrderStatus.PARTIALLY_FILLED) {
            throw new MarketOperationException("order is not cancellable in current status");
        }
    }

    private MarketOrderStatus deriveOrderStatus(long originalQuantity, long openQuantity, long filledQuantity) {
        if (openQuantity == 0L) {
            return MarketOrderStatus.FILLED;
        }
        if (filledQuantity > 0L && openQuantity < originalQuantity) {
            return MarketOrderStatus.PARTIALLY_FILLED;
        }
        return MarketOrderStatus.OPEN;
    }

    private long calculateBuyerReservation(long unitPrice, long quantity) {
        long grossAmount = unitPrice * quantity;
        return grossAmount + calculateFee(grossAmount, TAKER_FEE_BASIS_POINTS);
    }

    private long calculateFee(long amount, int basisPoints) {
        if (amount <= 0L || basisPoints <= 0) {
            return 0L;
        }
        return amount * basisPoints / 10000L;
    }

    private String buildBankRequestId(String requestId, String suffix) {
        return requestId + ":" + suffix;
    }

    private String buildMatchRequestPrefix(long operationId, long buyOrderId, long sellOrderId, int sequence) {
        return "market-match:" + operationId + ":" + buyOrderId + ":" + sellOrderId + ":" + sequence;
    }

    private String buildFilledBuyReleaseRequestId(long operationId, long buyOrderId) {
        return "market-buy-release:" + operationId + ":" + buyOrderId;
    }

    private String buildClaimDeliveryRequestId(String requestId) {
        return requestId + ":deliver";
    }

    private String buildOrderBusinessRef(String action, Object ref) {
        return "market:" + action + ":" + String.valueOf(ref);
    }

    private long requirePositive(long value, String fieldName) {
        if (value <= 0L) {
            throw new MarketOperationException(fieldName + " must be positive");
        }
        return value;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new MarketOperationException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new MarketOperationException(fieldName + " must not be null");
        }
        return value;
    }

    public static final class CreateSellOrderResult {

        private final MarketOrder order;
        private final MarketCustodyInventory custody;
        private final MarketOperationLog operationLog;
        private final List<MarketCustodyInventory> claimableAssets;
        private final List<MarketTradeRecord> tradeRecords;

        public CreateSellOrderResult(MarketOrder order, MarketCustodyInventory custody, MarketOperationLog operationLog,
            List<MarketCustodyInventory> claimableAssets, List<MarketTradeRecord> tradeRecords) {
            this.order = order;
            this.custody = custody;
            this.operationLog = operationLog;
            this.claimableAssets = Collections.unmodifiableList(new ArrayList<MarketCustodyInventory>(claimableAssets));
            this.tradeRecords = Collections.unmodifiableList(new ArrayList<MarketTradeRecord>(tradeRecords));
        }

        public MarketOrder getOrder() {
            return order;
        }

        public MarketCustodyInventory getCustody() {
            return custody;
        }

        public MarketOperationLog getOperationLog() {
            return operationLog;
        }

        public List<MarketCustodyInventory> getClaimableAssets() {
            return claimableAssets;
        }

        public List<MarketTradeRecord> getTradeRecords() {
            return tradeRecords;
        }
    }

    public static final class CancelSellOrderResult {

        private final MarketOrder order;
        private final MarketCustodyInventory custody;
        private final MarketOperationLog operationLog;

        public CancelSellOrderResult(MarketOrder order, MarketCustodyInventory custody, MarketOperationLog operationLog) {
            this.order = order;
            this.custody = custody;
            this.operationLog = operationLog;
        }

        public MarketOrder getOrder() {
            return order;
        }

        public MarketCustodyInventory getCustody() {
            return custody;
        }

        public MarketOperationLog getOperationLog() {
            return operationLog;
        }
    }

    public static final class CreateBuyOrderResult {

        private final MarketOrder order;
        private final MarketOperationLog operationLog;
        private final List<MarketCustodyInventory> claimableAssets;
        private final List<MarketTradeRecord> tradeRecords;

        public CreateBuyOrderResult(MarketOrder order, MarketOperationLog operationLog,
            List<MarketCustodyInventory> claimableAssets, List<MarketTradeRecord> tradeRecords) {
            this.order = order;
            this.operationLog = operationLog;
            this.claimableAssets = Collections.unmodifiableList(new ArrayList<MarketCustodyInventory>(claimableAssets));
            this.tradeRecords = Collections.unmodifiableList(new ArrayList<MarketTradeRecord>(tradeRecords));
        }

        public MarketOrder getOrder() {
            return order;
        }

        public MarketOperationLog getOperationLog() {
            return operationLog;
        }

        public List<MarketCustodyInventory> getClaimableAssets() {
            return claimableAssets;
        }

        public List<MarketTradeRecord> getTradeRecords() {
            return tradeRecords;
        }
    }

    public static final class CancelBuyOrderResult {

        private final MarketOrder order;
        private final MarketOperationLog operationLog;

        public CancelBuyOrderResult(MarketOrder order, MarketOperationLog operationLog) {
            this.order = order;
            this.operationLog = operationLog;
        }

        public MarketOrder getOrder() {
            return order;
        }

        public MarketOperationLog getOperationLog() {
            return operationLog;
        }
    }

    public static final class ClaimMarketAssetResult {

        private final MarketCustodyInventory custody;
        private final MarketOperationLog operationLog;

        public ClaimMarketAssetResult(MarketCustodyInventory custody, MarketOperationLog operationLog) {
            this.custody = custody;
            this.operationLog = operationLog;
        }

        public MarketCustodyInventory getCustody() {
            return custody;
        }

        public MarketOperationLog getOperationLog() {
            return operationLog;
        }
    }

    private static final class MatchExecutionSummary {

        private final MarketOrder order;
        private final MarketCustodyInventory primaryCustody;
        private final List<MarketCustodyInventory> claimableAssets;
        private final List<MarketTradeRecord> tradeRecords;
        private final long lastTradeId;

        private MatchExecutionSummary(MarketOrder order, MarketCustodyInventory primaryCustody,
            List<MarketCustodyInventory> claimableAssets, List<MarketTradeRecord> tradeRecords, long lastTradeId) {
            this.order = order;
            this.primaryCustody = primaryCustody;
            this.claimableAssets = claimableAssets;
            this.tradeRecords = tradeRecords;
            this.lastTradeId = lastTradeId;
        }
    }

    private static final class MutableRefs {

        private long orderId;
        private long custodyId;
        private long tradeId;
        private String recoveryMetadataKey;

        private boolean hasRecoveryState() {
            return orderId > 0L || custodyId > 0L || tradeId > 0L
                || (recoveryMetadataKey != null && !recoveryMetadataKey.trim().isEmpty());
        }
    }

    private static final class MarketRequestSemantics {

        private final String playerRef;
        private final String sourceServerId;
        private final String productKey;
        private final Long quantity;
        private final Boolean stackable;
        private final Long unitPrice;
        private final Long orderId;
        private final Long custodyId;

        private MarketRequestSemantics(String playerRef, String sourceServerId, String productKey, Long quantity,
            Boolean stackable, Long unitPrice, Long orderId, Long custodyId) {
            this.playerRef = playerRef;
            this.sourceServerId = sourceServerId;
            this.productKey = productKey;
            this.quantity = quantity;
            this.stackable = stackable;
            this.unitPrice = unitPrice;
            this.orderId = orderId;
            this.custodyId = custodyId;
        }

        private static MarketRequestSemantics forSellCreate(String playerRef, String sourceServerId, String productKey,
            long quantity, boolean stackable, long unitPrice) {
            return new MarketRequestSemantics(playerRef, sourceServerId, productKey, Long.valueOf(quantity),
                Boolean.valueOf(stackable), Long.valueOf(unitPrice), null, null);
        }

        private static MarketRequestSemantics forSellCancel(String playerRef, String sourceServerId, long orderId) {
            return new MarketRequestSemantics(playerRef, sourceServerId, null, null, null, null,
                Long.valueOf(orderId), null);
        }

        private static MarketRequestSemantics forBuyCreate(String playerRef, String sourceServerId, String productKey,
            long quantity, boolean stackable, long unitPrice) {
            return new MarketRequestSemantics(playerRef, sourceServerId, productKey, Long.valueOf(quantity),
                Boolean.valueOf(stackable), Long.valueOf(unitPrice), null, null);
        }

        private static MarketRequestSemantics forBuyCancel(String playerRef, String sourceServerId, long orderId) {
            return new MarketRequestSemantics(playerRef, sourceServerId, null, null, null, null,
                Long.valueOf(orderId), null);
        }

        private static MarketRequestSemantics forClaim(String playerRef, String sourceServerId, long custodyId) {
            return new MarketRequestSemantics(playerRef, sourceServerId, null, null, null, null, null,
                Long.valueOf(custodyId));
        }

        private String toKey() {
            return "playerRef=" + safe(playerRef) + "|sourceServerId=" + safe(sourceServerId) + "|productKey="
                + safe(productKey) + "|quantity=" + nullableNumber(quantity) + "|stackable="
                + nullableBoolean(stackable) + "|unitPrice=" + nullableNumber(unitPrice) + "|orderId="
                + nullableNumber(orderId) + "|custodyId=" + nullableNumber(custodyId);
        }

        private static MarketRequestSemantics parse(String key) {
            if (key == null || key.trim().isEmpty()) {
                return new MarketRequestSemantics(null, null, null, null, null, null, null, null);
            }
            String[] parts = key.split("\\|");
            String playerRef = null;
            String sourceServerId = null;
            String productKey = null;
            Long quantity = null;
            Boolean stackable = null;
            Long unitPrice = null;
            Long orderId = null;
            Long custodyId = null;
            for (String part : parts) {
                int index = part.indexOf('=');
                if (index <= 0) {
                    continue;
                }
                String name = part.substring(0, index);
                String value = part.substring(index + 1);
                if ("playerRef".equals(name)) {
                    playerRef = nullIfToken(value);
                } else if ("sourceServerId".equals(name)) {
                    sourceServerId = nullIfToken(value);
                } else if ("productKey".equals(name)) {
                    productKey = nullIfToken(value);
                } else if ("quantity".equals(name)) {
                    quantity = parseLong(value);
                } else if ("stackable".equals(name)) {
                    stackable = parseBoolean(value);
                } else if ("unitPrice".equals(name)) {
                    unitPrice = parseLong(value);
                } else if ("orderId".equals(name)) {
                    orderId = parseLong(value);
                } else if ("custodyId".equals(name)) {
                    custodyId = parseLong(value);
                }
            }
            return new MarketRequestSemantics(playerRef, sourceServerId, productKey, quantity, stackable, unitPrice,
                orderId, custodyId);
        }

        private List<String> diff(MarketRequestSemantics other) {
            List<String> mismatches = new ArrayList<String>();
            if (!safeEquals(playerRef, other.playerRef)) {
                mismatches.add("playerRef");
            }
            if (!safeEquals(sourceServerId, other.sourceServerId)) {
                mismatches.add("sourceServerId");
            }
            if (!safeEquals(productKey, other.productKey)) {
                mismatches.add("productKey");
            }
            if (!safeEquals(quantity, other.quantity)) {
                mismatches.add("quantity");
            }
            if (!safeEquals(stackable, other.stackable)) {
                mismatches.add("stackable");
            }
            if (!safeEquals(unitPrice, other.unitPrice)) {
                mismatches.add("unitPrice");
            }
            if (!safeEquals(orderId, other.orderId)) {
                mismatches.add("orderId");
            }
            if (!safeEquals(custodyId, other.custodyId)) {
                mismatches.add("custodyId");
            }
            return mismatches;
        }

        private static String safe(String value) {
            return value == null ? "~" : value;
        }

        private static String nullableNumber(Long value) {
            return value == null ? "~" : String.valueOf(value.longValue());
        }

        private static String nullableBoolean(Boolean value) {
            return value == null ? "~" : String.valueOf(value.booleanValue());
        }

        private static String nullIfToken(String value) {
            return "~".equals(value) ? null : value;
        }

        private static Long parseLong(String value) {
            return "~".equals(value) ? null : Long.valueOf(Long.parseLong(value));
        }

        private static Boolean parseBoolean(String value) {
            return "~".equals(value) ? null : Boolean.valueOf(Boolean.parseBoolean(value));
        }

        private static boolean safeEquals(Object left, Object right) {
            return left == null ? right == null : left.equals(right);
        }
    }

    private static final class NoOpMarketTradeRecordRepository implements MarketTradeRecordRepository {

        @Override
        public MarketTradeRecord save(MarketTradeRecord tradeRecord) {
            return tradeRecord;
        }

        @Override
        public List<MarketTradeRecord> findByOrderId(long orderId) {
            return Collections.emptyList();
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
}