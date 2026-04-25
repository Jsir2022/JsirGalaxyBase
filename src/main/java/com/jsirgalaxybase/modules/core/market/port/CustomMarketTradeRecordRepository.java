package com.jsirgalaxybase.modules.core.market.port;

import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.CustomMarketTradeRecord;

public interface CustomMarketTradeRecordRepository {

    CustomMarketTradeRecord save(CustomMarketTradeRecord tradeRecord);

    CustomMarketTradeRecord update(CustomMarketTradeRecord tradeRecord);

    Optional<CustomMarketTradeRecord> findByListingId(long listingId);
}