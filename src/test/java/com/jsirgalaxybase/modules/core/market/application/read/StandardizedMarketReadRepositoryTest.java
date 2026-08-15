package com.jsirgalaxybase.modules.core.market.application.read;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.modules.core.market.application.read.StandardizedMarketReadRepository.Candle;
import com.jsirgalaxybase.modules.core.market.application.read.StandardizedMarketReadRepository.CandleSource;
import com.jsirgalaxybase.modules.core.market.application.read.StandardizedMarketReadRepository.ChartRange;
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;

public class StandardizedMarketReadRepositoryTest {

    private static final StandardizedMarketProduct IRON = new StandardizedMarketProduct("minecraft:iron_ingot", 0);
    private static final Instant NOW = Instant.parse("2026-08-14T08:59:30Z");

    @Test
    public void chartRangesAlwaysReturnAlignedContinuousBuckets() {
        FakeTradeRepository trades = new FakeTradeRepository();
        StandardizedMarketReadRepository repository = repository(trades);

        assertAligned(repository.readCandles(IRON.getProductKey(), NOW, ChartRange.ONE_HOUR, 65L), 12, 300L);
        assertAligned(repository.readCandles(IRON.getProductKey(), NOW, ChartRange.DAY, 65L), 24, 3600L);
        assertAligned(repository.readCandles(IRON.getProductKey(), NOW, ChartRange.WEEK, 65L), 28, 21600L);
    }

    @Test
    public void realTradeKeepsOhlcvAndEmptyBucketsCarryLatestCloseWithoutVolume() {
        long lastBucket = aligned(NOW, 300L);
        long firstBucket = lastBucket - 11L * 300L;
        FakeTradeRepository trades = new FakeTradeRepository();
        trades.prior = trade(1L, 100L, 1L, firstBucket - 1L);
        trades.records.add(trade(2L, 105L, 4L, firstBucket + 2L * 300L + 10L));
        trades.records.add(trade(3L, 103L, 2L, firstBucket + 2L * 300L + 20L));

        List<Candle> candles = repository(trades).readCandles(IRON.getProductKey(), NOW,
            ChartRange.ONE_HOUR, 65L);

        assertEquals(CandleSource.CARRY_FORWARD, candles.get(0).source);
        assertEquals(100L, candles.get(0).close);
        assertEquals(0L, candles.get(0).volume);
        Candle actual = candles.get(2);
        assertEquals(CandleSource.TRADE, actual.source);
        assertEquals(105L, actual.open);
        assertEquals(105L, actual.high);
        assertEquals(103L, actual.low);
        assertEquals(103L, actual.close);
        assertEquals(6L, actual.volume);
        assertEquals(626L, actual.turnover);
        assertEquals(CandleSource.CARRY_FORWARD, candles.get(3).source);
        assertEquals(103L, candles.get(3).close);
        assertEquals(0L, candles.get(3).volume);
    }

    @Test
    public void referenceBaselineAndEmptyMarketRemainDistinguishable() {
        StandardizedMarketReadRepository repository = repository(new FakeTradeRepository());
        List<Candle> reference = repository.readCandles(IRON.getProductKey(), NOW, ChartRange.ONE_HOUR, 88L);
        List<Candle> empty = repository.readCandles(IRON.getProductKey(), NOW, ChartRange.ONE_HOUR, 0L);

        for (Candle candle : reference) {
            assertEquals(CandleSource.REFERENCE, candle.source);
            assertEquals(88L, candle.close);
            assertEquals(0L, candle.volume);
        }
        for (Candle candle : empty) {
            assertEquals(CandleSource.EMPTY, candle.source);
            assertEquals(0L, candle.close);
            assertEquals(0L, candle.volume);
        }
    }

    private static StandardizedMarketReadRepository repository(FakeTradeRepository trades) {
        return new StandardizedMarketReadRepository(null, trades, null);
    }

    private static void assertAligned(List<Candle> candles, int count, long spacing) {
        assertEquals(count, candles.size());
        for (int index = 1; index < candles.size(); index++) {
            assertEquals(spacing, candles.get(index).startEpochSeconds - candles.get(index - 1).startEpochSeconds);
        }
    }

    private static long aligned(Instant value, long bucketSeconds) {
        return value.getEpochSecond() - value.getEpochSecond() % bucketSeconds;
    }

    private static MarketTradeRecord trade(long id, long price, long quantity, long epochSeconds) {
        return new MarketTradeRecord(id, "buyer", "seller", IRON, true, price, quantity, 0L,
            id * 10L, id * 10L + 1L, id * 10L + 2L, Instant.ofEpochSecond(epochSeconds));
    }

    private static final class FakeTradeRepository implements MarketTradeRecordRepository {
        private final List<MarketTradeRecord> records = new ArrayList<MarketTradeRecord>();
        private MarketTradeRecord prior;

        @Override public MarketTradeRecord save(MarketTradeRecord tradeRecord) { return tradeRecord; }
        @Override public List<MarketTradeRecord> findByOrderId(long orderId) {
            return Collections.emptyList();
        }
        @Override public List<MarketTradeRecord> findByProductKeySince(String productKey, Instant since, int limit) {
            return new ArrayList<MarketTradeRecord>(records);
        }
        @Override public MarketTradeRecord findLatestByProductKeyBefore(String productKey, Instant before) {
            return prior;
        }
    }
}
