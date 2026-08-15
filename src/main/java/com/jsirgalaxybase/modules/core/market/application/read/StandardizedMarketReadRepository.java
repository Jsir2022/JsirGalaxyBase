package com.jsirgalaxybase.modules.core.market.application.read;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.AccountInventoryResolver;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;

/** Read-only market projection used by terminal charts and product cards. */
public final class StandardizedMarketReadRepository {

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");

    private final MarketOrderBookRepository orders;
    private final MarketTradeRecordRepository trades;
    private final AccountInventoryResolver inventories;

    public StandardizedMarketReadRepository(MarketOrderBookRepository orders, MarketTradeRecordRepository trades,
        AccountInventoryResolver inventories) {
        this.orders = orders;
        this.trades = trades;
        this.inventories = inventories;
    }

    public Map<String, ProductQuote> readPage(List<StandardizedMarketProduct> products, String playerRef, Instant now) {
        if (products == null || products.isEmpty()) { return Collections.emptyMap(); }
        List<String> keys = new ArrayList<String>();
        for (StandardizedMarketProduct product : products) { keys.add(product.getProductKey()); }
        List<MarketTradeRecord> pageTrades = trades.findByProductKeysSince(keys,
            now.minusSeconds(24L * 60L * 60L), Math.max(144, products.size() * 48));
        Map<String, List<MarketTradeRecord>> grouped = new LinkedHashMap<String, List<MarketTradeRecord>>();
        for (String key : keys) { grouped.put(key, new ArrayList<MarketTradeRecord>()); }
        for (MarketTradeRecord trade : pageTrades) {
            List<MarketTradeRecord> bucket = grouped.get(trade.getProduct().getProductKey());
            if (bucket != null) { bucket.add(trade); }
        }
        List<MarketOrder> pageOrders = orders.findOpenOrdersByProductKeys(keys);
        Map<String, List<MarketOrder>> groupedBids = new LinkedHashMap<String, List<MarketOrder>>();
        Map<String, List<MarketOrder>> groupedAsks = new LinkedHashMap<String, List<MarketOrder>>();
        for (String key : keys) {
            groupedBids.put(key, new ArrayList<MarketOrder>());
            groupedAsks.put(key, new ArrayList<MarketOrder>());
        }
        for (MarketOrder order : pageOrders) {
            Map<String, List<MarketOrder>> target = order.getSide().name().equals("BUY") ? groupedBids : groupedAsks;
            List<MarketOrder> bucket = target.get(order.getProduct().getProductKey());
            if (bucket != null) bucket.add(order);
        }
        Comparator<MarketOrder> bidsFirst = (left, right) -> Long.compare(right.getUnitPrice(), left.getUnitPrice());
        Comparator<MarketOrder> asksFirst = (left, right) -> Long.compare(left.getUnitPrice(), right.getUnitPrice());
        for (String key : keys) {
            Collections.sort(groupedBids.get(key), bidsFirst);
            Collections.sort(groupedAsks.get(key), asksFirst);
        }
        Instant dayStart = now.atZone(MARKET_ZONE).toLocalDate().atStartOfDay(MARKET_ZONE).toInstant();
        Map<String, ProductQuote> result = new LinkedHashMap<String, ProductQuote>();
        for (StandardizedMarketProduct product : products) {
            result.put(product.getProductKey(), project(product, playerRef, grouped.get(product.getProductKey()),
                groupedBids.get(product.getProductKey()), groupedAsks.get(product.getProductKey()), dayStart));
        }
        return result;
    }

    public List<Candle> readCandles(String productKey, Instant now, ChartRange range) {
        return readCandles(productKey, now, range, 0L);
    }

    public List<Candle> readCandles(String productKey, Instant now, ChartRange range, long referencePrice) {
        long bucketSeconds = range.bucketSeconds;
        long lastBucket = now.getEpochSecond() - now.getEpochSecond() % bucketSeconds;
        long firstBucket = lastBucket - bucketSeconds * (range.bucketCount - 1L);
        List<MarketTradeRecord> history = trades.findByProductKeySince(productKey,
            Instant.ofEpochSecond(firstBucket), 2048);
        Collections.sort(history, new Comparator<MarketTradeRecord>() {
            @Override public int compare(MarketTradeRecord left, MarketTradeRecord right) {
                return left.getCreatedAt().compareTo(right.getCreatedAt());
            }
        });
        Map<Long, MutableCandle> buckets = new LinkedHashMap<Long, MutableCandle>();
        for (MarketTradeRecord trade : history) {
            long epoch = trade.getCreatedAt().getEpochSecond();
            long start = epoch - epoch % bucketSeconds;
            if (start < firstBucket || start > lastBucket) { continue; }
            MutableCandle candle = buckets.get(Long.valueOf(start));
            if (candle == null) { candle = new MutableCandle(start, trade.getUnitPrice()); buckets.put(Long.valueOf(start), candle); }
            candle.accept(trade);
        }
        MarketTradeRecord prior = trades.findLatestByProductKeyBefore(productKey, Instant.ofEpochSecond(firstBucket));
        long previousClose = prior == null ? 0L : prior.getUnitPrice();
        CandleSource syntheticSource = prior == null ? CandleSource.REFERENCE : CandleSource.CARRY_FORWARD;
        List<Candle> result = new ArrayList<Candle>();
        for (int index = 0; index < range.bucketCount; index++) {
            long start = firstBucket + index * bucketSeconds;
            MutableCandle actual = buckets.get(Long.valueOf(start));
            if (actual != null) {
                Candle candle = actual.freeze();
                result.add(candle);
                previousClose = candle.close;
                syntheticSource = CandleSource.CARRY_FORWARD;
            } else if (previousClose > 0L) {
                result.add(Candle.flat(start, previousClose, syntheticSource));
            } else if (referencePrice > 0L) {
                previousClose = referencePrice;
                syntheticSource = CandleSource.REFERENCE;
                result.add(Candle.flat(start, referencePrice, CandleSource.REFERENCE));
            } else {
                result.add(Candle.empty(start));
            }
        }
        return result;
    }

    private ProductQuote project(StandardizedMarketProduct product, String playerRef, List<MarketTradeRecord> records,
        List<MarketOrder> pageBids, List<MarketOrder> pageAsks, Instant dayStart) {
        List<MarketTradeRecord> history = records == null ? new ArrayList<MarketTradeRecord>()
            : new ArrayList<MarketTradeRecord>(records);
        Collections.sort(history, new Comparator<MarketTradeRecord>() {
            @Override public int compare(MarketTradeRecord left, MarketTradeRecord right) {
                return left.getCreatedAt().compareTo(right.getCreatedAt());
            }
        });
        List<MarketOrder> bids = pageBids == null ? Collections.<MarketOrder>emptyList() : pageBids;
        List<MarketOrder> asks = pageAsks == null ? Collections.<MarketOrder>emptyList() : pageAsks;
        long volume = 0L, turnover = 0L;
        for (MarketTradeRecord trade : history) {
            volume += trade.getQuantity(); turnover += trade.getQuantity() * trade.getUnitPrice();
        }
        long latest = history.isEmpty() ? 0L : history.get(history.size() - 1).getUnitPrice();
        long oldest = history.isEmpty() ? 0L : history.get(0).getUnitPrice();
        long dayOpen = 0L;
        for (MarketTradeRecord trade : history) {
            if (trade.getCreatedAt() != null && !trade.getCreatedAt().isBefore(dayStart)) {
                dayOpen = trade.getUnitPrice();
                break;
            }
        }
        return new ProductQuote(product, latest, oldest, dayOpen, bids.isEmpty() ? 0L : bids.get(0).getUnitPrice(),
            bids.isEmpty() ? 0L : bids.get(0).getOpenQuantity(), asks.isEmpty() ? 0L : asks.get(0).getUnitPrice(),
            asks.isEmpty() ? 0L : asks.get(0).getOpenQuantity(), volume, turnover, history.size(),
            inventories.countSellable(playerRef, product), history);
    }

    public enum ChartRange {
        ONE_HOUR(5L * 60L, 12), DAY(60L * 60L, 24), WEEK(6L * 60L * 60L, 28);
        final long bucketSeconds; final int bucketCount;
        ChartRange(long bucketSeconds, int bucketCount) { this.bucketSeconds = bucketSeconds; this.bucketCount = bucketCount; }
    }

    public static final class ProductQuote {
        public final StandardizedMarketProduct product;
        public final long latestPrice, oldestPrice, dayOpenPrice, bestBidPrice, bestBidQuantity, bestAskPrice,
            bestAskQuantity;
        public final long volume24h, turnover24h, tradeCount24h, sellableQuantity;
        public final List<MarketTradeRecord> trades;
        ProductQuote(StandardizedMarketProduct product, long latestPrice, long oldestPrice, long dayOpenPrice,
            long bestBidPrice,
            long bestBidQuantity, long bestAskPrice, long bestAskQuantity, long volume24h, long turnover24h,
            long tradeCount24h, long sellableQuantity, List<MarketTradeRecord> trades) {
            this.product = product; this.latestPrice = latestPrice; this.oldestPrice = oldestPrice;
            this.dayOpenPrice = dayOpenPrice;
            this.bestBidPrice = bestBidPrice; this.bestBidQuantity = bestBidQuantity;
            this.bestAskPrice = bestAskPrice; this.bestAskQuantity = bestAskQuantity;
            this.volume24h = volume24h; this.turnover24h = turnover24h; this.tradeCount24h = tradeCount24h;
            this.sellableQuantity = sellableQuantity; this.trades = Collections.unmodifiableList(new ArrayList<MarketTradeRecord>(trades));
        }
        public boolean hasTwoSidedBook() { return bestBidPrice > 0L && bestAskPrice > 0L; }
    }

    public static final class Candle {
        public final long startEpochSeconds, open, high, low, close, volume, turnover;
        public final CandleSource source;
        Candle(long startEpochSeconds, long open, long high, long low, long close, long volume, long turnover,
            CandleSource source) {
            this.startEpochSeconds = startEpochSeconds; this.open = open; this.high = high; this.low = low;
            this.close = close; this.volume = volume; this.turnover = turnover; this.source = source;
        }
        static Candle flat(long start, long price, CandleSource source) {
            return new Candle(start, price, price, price, price, 0L, 0L, source);
        }
        static Candle empty(long start) { return new Candle(start, 0L, 0L, 0L, 0L, 0L, 0L, CandleSource.EMPTY); }
    }

    public enum CandleSource { TRADE, CARRY_FORWARD, REFERENCE, EMPTY }

    private static final class MutableCandle {
        final long start, open; long high, low, close, volume, turnover;
        MutableCandle(long start, long price) { this.start = start; this.open = price; this.high = price; this.low = price; this.close = price; }
        void accept(MarketTradeRecord trade) { long price = trade.getUnitPrice(); high = Math.max(high, price); low = Math.min(low, price); close = price; volume += trade.getQuantity(); turnover += price * trade.getQuantity(); }
        Candle freeze() { return new Candle(start, open, high, low, close, volume, turnover, CandleSource.TRADE); }
    }
}
