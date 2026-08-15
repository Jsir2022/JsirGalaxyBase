package com.jsirgalaxybase.modules.core.market.port;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord;

public interface MarketTradeRecordRepository {

    MarketTradeRecord save(MarketTradeRecord tradeRecord);

    List<MarketTradeRecord> findByOrderId(long orderId);

    default List<MarketTradeRecord> findByProductKey(String productKey, int limit) {
        return Collections.emptyList();
    }

    default List<MarketTradeRecord> findByProductKeySince(String productKey, Instant since, int limit) {
        return Collections.emptyList();
    }

    /** Latest trade strictly before the requested chart window. */
    default MarketTradeRecord findLatestByProductKeyBefore(String productKey, Instant before) {
        return null;
    }

    /** One page-level history read for terminal browse summaries. */
    default List<MarketTradeRecord> findByProductKeysSince(List<String> productKeys, Instant since, int limit) {
        return Collections.emptyList();
    }

    default List<String> findDistinctProductKeys(int limit) {
        return Collections.emptyList();
    }
}
