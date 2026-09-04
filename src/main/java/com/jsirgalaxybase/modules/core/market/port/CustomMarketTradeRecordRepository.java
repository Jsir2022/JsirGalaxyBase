package com.jsirgalaxybase.modules.core.market.port;

import java.util.Optional;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.modules.core.market.domain.CustomMarketTradeRecord;

public interface CustomMarketTradeRecordRepository {

    CustomMarketTradeRecord save(CustomMarketTradeRecord tradeRecord);

    CustomMarketTradeRecord update(CustomMarketTradeRecord tradeRecord);

    Optional<CustomMarketTradeRecord> findByListingId(long listingId);

    default List<CustomMarketTradeRecord> findRecentByPlayer(String playerRef, int limit) {
        return Collections.emptyList();
    }
}
