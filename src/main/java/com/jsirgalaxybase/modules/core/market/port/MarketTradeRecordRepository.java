package com.jsirgalaxybase.modules.core.market.port;

import java.util.List;

import com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord;

public interface MarketTradeRecordRepository {

    MarketTradeRecord save(MarketTradeRecord tradeRecord);

    List<MarketTradeRecord> findByOrderId(long orderId);
}