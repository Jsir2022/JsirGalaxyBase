package com.jsirgalaxybase.modules.warehouse.application;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.modules.core.market.domain.MarketAccountCenterQuery;
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeHistoryPage;
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.infrastructure.MarketInfrastructure;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivitySnapshot;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivityType;

public class AssetActivityServiceTest {

    @Test public void standardBuyWaitsForVaultDeliveryWhileSellBecomesSettlementActivity() {
        CapturingTradeRepository trades = new CapturingTradeRepository();
        trades.rows.add(trade(2L, "buyer", "self", Instant.ofEpochMilli(2000L)));
        trades.rows.add(trade(1L, "self", "seller", Instant.ofEpochMilli(1000L)));
        AssetActivityService service = new AssetActivityService(
            new MarketInfrastructure(null, null, null, trades, null), null);

        AssetActivitySnapshot snapshot = service.findRecent("self", 20);

        assertEquals("self", trades.playerRef);
        assertEquals(1, snapshot.getRows().size());
        assertEquals(AssetActivityType.STANDARD_SELL_SETTLED, snapshot.getRows().get(0).getType());
        assertEquals("T2", snapshot.getRows().get(0).getReference());
    }

    @Test public void resultIsBoundedToTwentyNewestBusinessRows() {
        CapturingTradeRepository trades = new CapturingTradeRepository();
        for (int index = 0; index < 25; index++)
            trades.rows.add(trade(index, "buyer", "self", Instant.ofEpochMilli(index)));
        AssetActivityService service = new AssetActivityService(
            new MarketInfrastructure(null, null, null, trades, null), null);

        AssetActivitySnapshot snapshot = service.findRecent("self", 20);

        assertEquals(20, snapshot.getRows().size());
        assertEquals("T24", snapshot.getRows().get(0).getReference());
        assertEquals("T5", snapshot.getRows().get(19).getReference());
    }

    private MarketTradeRecord trade(long id, String buyer, String seller, Instant createdAt) {
        return new MarketTradeRecord(id, buyer, seller, null, true, 10L, 2L, 1L,
            id + 100L, id + 200L, id + 300L, createdAt);
    }

    private static final class CapturingTradeRepository implements MarketTradeRecordRepository {
        private final List<MarketTradeRecord> rows = new ArrayList<MarketTradeRecord>();
        private String playerRef;
        @Override public MarketTradeRecord save(MarketTradeRecord tradeRecord) { return tradeRecord; }
        @Override public List<MarketTradeRecord> findByOrderId(long orderId) { return Collections.emptyList(); }
        @Override public MarketTradeHistoryPage findPersonalTradeHistory(String playerRef,
            MarketAccountCenterQuery query) {
            this.playerRef = playerRef;
            return new MarketTradeHistoryPage(rows, rows.size(), 0, query.getPageSize());
        }
    }
}
