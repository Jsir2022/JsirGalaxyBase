package com.jsirgalaxybase.modules.core.market.infrastructure;

import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public class MarketInfrastructure {

    private final MarketOrderBookRepository orderBookRepository;
    private final MarketCustodyInventoryRepository custodyInventoryRepository;
    private final MarketOperationLogRepository operationLogRepository;
    private final MarketTradeRecordRepository tradeRecordRepository;
    private final MarketTransactionRunner transactionRunner;

    public MarketInfrastructure(MarketOrderBookRepository orderBookRepository,
        MarketCustodyInventoryRepository custodyInventoryRepository,
        MarketOperationLogRepository operationLogRepository, MarketTradeRecordRepository tradeRecordRepository,
        MarketTransactionRunner transactionRunner) {
        this.orderBookRepository = orderBookRepository;
        this.custodyInventoryRepository = custodyInventoryRepository;
        this.operationLogRepository = operationLogRepository;
        this.tradeRecordRepository = tradeRecordRepository;
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

    public MarketTransactionRunner getTransactionRunner() {
        return transactionRunner;
    }
}