package com.jsirgalaxybase.modules.core.market.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.jsirgalaxybase.modules.core.banking.application.command.FrozenBalanceCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.domain.BankBusinessType;
import com.jsirgalaxybase.modules.core.banking.domain.BankTransactionType;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationType;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderStatus;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public class MarketRecoveryService {

    private final MarketOrderBookRepository orderRepository;
    private final MarketCustodyInventoryRepository custodyRepository;
    private final MarketOperationLogRepository operationLogRepository;
    private final MarketTransactionRunner transactionRunner;
    private final MarketSettlementFacade settlementFacade;

    public MarketRecoveryService(MarketOrderBookRepository orderRepository,
        MarketCustodyInventoryRepository custodyRepository, MarketOperationLogRepository operationLogRepository) {
        this(orderRepository, custodyRepository, operationLogRepository, new DirectTransactionRunner(), null);
    }

    public MarketRecoveryService(MarketOrderBookRepository orderRepository,
        MarketCustodyInventoryRepository custodyRepository, MarketOperationLogRepository operationLogRepository,
        MarketTransactionRunner transactionRunner, MarketSettlementFacade settlementFacade) {
        this.orderRepository = orderRepository;
        this.custodyRepository = custodyRepository;
        this.operationLogRepository = operationLogRepository;
        this.transactionRunner = transactionRunner;
        this.settlementFacade = settlementFacade;
    }

    public List<MarketOperationLog> scanAndEscalateIncompleteOperations(int limit) {
        List<MarketOperationLog> candidates = operationLogRepository.findByStatuses(Arrays.asList(
            MarketOperationStatus.CREATED,
            MarketOperationStatus.PROCESSING,
            MarketOperationStatus.FAILED,
            MarketOperationStatus.RECOVERY_REQUIRED), limit);
        List<MarketOperationLog> escalated = new ArrayList<MarketOperationLog>();
        for (MarketOperationLog candidate : candidates) {
            escalated.add(recoverCandidate(candidate));
        }
        return escalated;
    }

    private MarketOperationLog recoverCandidate(MarketOperationLog candidate) {
        if (settlementFacade != null && (candidate.getOperationType() == MarketOperationType.BUY_ORDER_CREATE
            || candidate.getOperationType() == MarketOperationType.BUY_ORDER_CANCEL)) {
            return recoverBuySideFunds(candidate);
        }
        if (candidate.getOperationType() == MarketOperationType.CLAIMABLE_ASSET_CLAIM) {
            return recoverClaimOperation(candidate);
        }
        return escalateGeneric(candidate);
    }

    private MarketOperationLog recoverBuySideFunds(final MarketOperationLog candidate) {
        return transactionRunner.inTransaction(new Supplier<MarketOperationLog>() {

            @Override
            public MarketOperationLog get() {
                MarketRecoveryMetadata metadata = MarketRecoveryMetadata.parse(candidate.getRecoveryMetadataKey());
                Optional<MarketOrder> existingOrder = candidate.getRelatedOrderId() > 0L
                    ? orderRepository.findById(candidate.getRelatedOrderId()) : Optional.<MarketOrder>empty();
                long releasableFunds = existingOrder.isPresent() ? existingOrder.get().getReservedFunds()
                    : metadata.getLong("reservedFunds", 0L);
                String releaseRequestId = metadata.get("releaseRequestId");

                if (releaseRequestId != null && releasableFunds > 0L) {
                    BankAccount buyerAccount = settlementFacade.requirePlayerAccount(candidate.getPlayerRef());
                    settlementFacade.releaseBuyerFunds(new FrozenBalanceCommand(releaseRequestId,
                        BankTransactionType.MARKET_FUNDS_RELEASE, BankBusinessType.MARKET_ORDER_CANCEL_RELEASE,
                        buyerAccount.getAccountId(), candidate.getSourceServerId(), "market", "market",
                        candidate.getPlayerRef(), releasableFunds,
                        "recover frozen funds for incomplete buy-side market operation",
                        buildOrderBusinessRef("buy-recovery-release", existingOrder.isPresent()
                            ? String.valueOf(existingOrder.get().getOrderId()) : candidate.getRequestId()),
                        "{\"marketRequestId\":\"" + candidate.getRequestId() + "\"}"));
                    metadata = metadata.toBuilder()
                        .putBoolean("fundsReleased", true)
                        .putLong("releasedFunds", releasableFunds)
                        .build();
                }

                long relatedOrderId = candidate.getRelatedOrderId();
                if (existingOrder.isPresent()) {
                    MarketOrder order = orderRepository.lockById(existingOrder.get().getOrderId());
                    relatedOrderId = order.getOrderId();
                    MarketOrderStatus recoveredStatus = order.getOpenQuantity() == 0L ? MarketOrderStatus.FILLED
                        : MarketOrderStatus.CANCELLED;
                    long recoveredOpenQuantity = recoveredStatus == MarketOrderStatus.CANCELLED ? 0L
                        : order.getOpenQuantity();
                    orderRepository.update(order.withLifecycle(recoveredStatus, recoveredOpenQuantity,
                        order.getFilledQuantity(), 0L, Instant.now()));
                }

                return operationLogRepository.update(candidate.withState(MarketOperationStatus.COMPLETED,
                    relatedOrderId, candidate.getRelatedCustodyId(), candidate.getRelatedTradeId(),
                    "recovered frozen funds for incomplete buy-side market operation", metadata.toKey(),
                    Instant.now()));
            }
        });
    }

    private MarketOperationLog recoverClaimOperation(final MarketOperationLog candidate) {
        return transactionRunner.inTransaction(new Supplier<MarketOperationLog>() {

            @Override
            public MarketOperationLog get() {
                if (candidate.getRelatedCustodyId() <= 0L) {
                    return escalateGeneric(candidate);
                }
                MarketRecoveryMetadata metadata = MarketRecoveryMetadata.parse(candidate.getRecoveryMetadataKey());
                MarketCustodyInventory custody = custodyRepository.lockById(candidate.getRelatedCustodyId());
                if (metadata.getBoolean("deliveryCompleted", false)) {
                    if (custody.getStatus() != MarketCustodyStatus.CLAIMED) {
                        custody = custodyRepository.update(custody.withStateAndLinks(MarketCustodyStatus.CLAIMED,
                            custody.getQuantity(), custody.getRelatedOrderId(), candidate.getOperationId(),
                            Instant.now()));
                    }
                    return operationLogRepository.update(candidate.withState(MarketOperationStatus.COMPLETED,
                        custody.getRelatedOrderId(), custody.getCustodyId(), candidate.getRelatedTradeId(),
                        "recovered claim completion after successful delivery", metadata.toKey(), Instant.now()));
                }

                if (custody.getStatus() != MarketCustodyStatus.CLAIMABLE
                    && custody.getStatus() != MarketCustodyStatus.CLAIMED) {
                    custody = custodyRepository.update(custody.withStateAndLinks(MarketCustodyStatus.CLAIMABLE,
                        custody.getQuantity(), custody.getRelatedOrderId(), candidate.getOperationId(), Instant.now()));
                }
                return operationLogRepository.update(candidate.withState(MarketOperationStatus.FAILED,
                    custody.getRelatedOrderId(), custody.getCustodyId(), candidate.getRelatedTradeId(),
                    "claim recovery restored custody to CLAIMABLE", metadata.toKey(), Instant.now()));
            }
        });
    }

    private MarketOperationLog escalateGeneric(MarketOperationLog candidate) {
        long relatedOrderId = candidate.getRelatedOrderId();
        long relatedCustodyId = candidate.getRelatedCustodyId();

        if (relatedOrderId > 0L) {
            Optional<MarketOrder> order = orderRepository.findById(relatedOrderId);
            if (order.isPresent() && order.get().getStatus() != MarketOrderStatus.EXCEPTION
                && order.get().getStatus() != MarketOrderStatus.CANCELLED
                && order.get().getStatus() != MarketOrderStatus.FILLED) {
                orderRepository.update(order.get().withLifecycle(MarketOrderStatus.EXCEPTION,
                    order.get().getOpenQuantity(), order.get().getFilledQuantity(), order.get().getReservedFunds(),
                    Instant.now()));
            }
        }

        if (relatedCustodyId > 0L) {
            Optional<MarketCustodyInventory> custody = custodyRepository.findById(relatedCustodyId);
            if (custody.isPresent() && custody.get().getStatus() != MarketCustodyStatus.EXCEPTION
                && custody.get().getStatus() != MarketCustodyStatus.CLAIMABLE
                && custody.get().getStatus() != MarketCustodyStatus.CLAIMED) {
                custodyRepository.update(custody.get().withStateAndLinks(MarketCustodyStatus.EXCEPTION,
                    custody.get().getQuantity(), custody.get().getRelatedOrderId(),
                    custody.get().getRelatedOperationId(), Instant.now()));
            }
        }

        return operationLogRepository.update(candidate.withState(MarketOperationStatus.RECOVERY_REQUIRED,
            relatedOrderId, relatedCustodyId, candidate.getRelatedTradeId(),
            "recovery required: " + candidate.getMessage(), candidate.getRecoveryMetadataKey(), Instant.now()));
    }

    private String buildOrderBusinessRef(String action, Object ref) {
        return "market:" + action + ":" + String.valueOf(ref);
    }

    private static final class DirectTransactionRunner implements MarketTransactionRunner {

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