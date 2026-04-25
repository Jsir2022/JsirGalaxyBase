package com.jsirgalaxybase.modules.core.market.infrastructure;

import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketAuditLogRepository;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketItemSnapshotRepository;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketListingRepository;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public class MarketInfrastructure {

    private final MarketOrderBookRepository orderBookRepository;
    private final MarketCustodyInventoryRepository custodyInventoryRepository;
    private final MarketOperationLogRepository operationLogRepository;
    private final MarketTradeRecordRepository tradeRecordRepository;
    private final CustomMarketListingRepository customMarketListingRepository;
    private final CustomMarketItemSnapshotRepository customMarketItemSnapshotRepository;
    private final CustomMarketTradeRecordRepository customMarketTradeRecordRepository;
    private final CustomMarketAuditLogRepository customMarketAuditLogRepository;
    private final MarketTransactionRunner transactionRunner;

    public MarketInfrastructure(MarketOrderBookRepository orderBookRepository,
        MarketCustodyInventoryRepository custodyInventoryRepository,
        MarketOperationLogRepository operationLogRepository, MarketTradeRecordRepository tradeRecordRepository,
        MarketTransactionRunner transactionRunner) {
        this(orderBookRepository, custodyInventoryRepository, operationLogRepository, tradeRecordRepository, null,
            null, null, null, transactionRunner);
    }

    public MarketInfrastructure(MarketOrderBookRepository orderBookRepository,
        MarketCustodyInventoryRepository custodyInventoryRepository,
        MarketOperationLogRepository operationLogRepository, MarketTradeRecordRepository tradeRecordRepository,
        CustomMarketListingRepository customMarketListingRepository,
        CustomMarketItemSnapshotRepository customMarketItemSnapshotRepository,
        CustomMarketTradeRecordRepository customMarketTradeRecordRepository,
        CustomMarketAuditLogRepository customMarketAuditLogRepository,
        MarketTransactionRunner transactionRunner) {
        this.orderBookRepository = orderBookRepository;
        this.custodyInventoryRepository = custodyInventoryRepository;
        this.operationLogRepository = operationLogRepository;
        this.tradeRecordRepository = tradeRecordRepository;
        this.customMarketListingRepository = customMarketListingRepository;
        this.customMarketItemSnapshotRepository = customMarketItemSnapshotRepository;
        this.customMarketTradeRecordRepository = customMarketTradeRecordRepository;
        this.customMarketAuditLogRepository = customMarketAuditLogRepository;
        this.transactionRunner = transactionRunner;
    }

    public MarketOrderBookRepository getOrderBookRepository() {
        return orderBookRepository;
    }

    public MarketCustodyInventoryRepository getCustodyInventoryRepository() {
        return custodyInventoryRepository;
    }

    public MarketOperationLogRepository getOperationLogRepository() {
        return operationLogRepository;
    }

    public MarketTradeRecordRepository getTradeRecordRepository() {
        return tradeRecordRepository;
    }

    public CustomMarketListingRepository getCustomMarketListingRepository() {
        return customMarketListingRepository;
    }

    public CustomMarketItemSnapshotRepository getCustomMarketItemSnapshotRepository() {
        return customMarketItemSnapshotRepository;
    }

    public CustomMarketTradeRecordRepository getCustomMarketTradeRecordRepository() {
        return customMarketTradeRecordRepository;
    }

    public CustomMarketAuditLogRepository getCustomMarketAuditLogRepository() {
        return customMarketAuditLogRepository;
    }

    public MarketTransactionRunner getTransactionRunner() {
        return transactionRunner;
    }
}