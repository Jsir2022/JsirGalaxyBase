package com.jsirgalaxybase.terminal.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import org.junit.Test;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketAdmissionDecision;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketAdmissionReason;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogEntry;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogVersion;
import com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketService;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.repository.MarketTransactionRunner;

public class TerminalMarketServiceTest {

    @Test
    public void exchangeViewFallsBackToExplicitEmptyStateWhenRuntimeIsUnavailable() throws Exception {
        TerminalExchangeQuoteView view = invokeBuildExchangeQuoteView(buildExchangeContext(null,
            "汇率市场运行时未完成装配。", false));

        assertEquals("汇率市场不可用", view.serviceState);
        assertEquals("UNAVAILABLE", view.limitStatus);
        assertEquals("0", view.executableFlag);
        assertEquals("汇率市场运行时未完成装配。", view.notes);
        assertTrue(view.executionHint.contains("不能继续执行兑换"));
    }

    @Test
    public void exchangeViewFallsBackToExplicitEmptyStateWhenPlayerIsNotServerContext() throws Exception {
        TerminalExchangeQuoteView view = invokeBuildExchangeQuoteView(buildExchangeContext(null, null, true));

        assertEquals("汇率市场不可用", view.serviceState);
        assertEquals("UNAVAILABLE", view.limitStatus);
        assertEquals("0", view.executableFlag);
        assertEquals("当前客户端上下文不能直接读取正式报价。", view.notes);
        assertFalse(view.hasFormalQuote());
    }

    @Test
    public void inspectRuntimeCatalogProductUsesInjectedRuntimeDecision() {
        StandardizedMarketAdmissionDecision runtimeDecision = new StandardizedMarketAdmissionDecision(
            new StandardizedMarketCatalogVersion("runtime-catalog-v2", "运行时目录 v2"),
            new StandardizedMarketCatalogEntry(new StandardizedMarketProduct("minecraft:stone", 0),
                "runtime-category", "runtime-only", "runtime-entry"),
            false, StandardizedMarketAdmissionReason.CATALOG_BOUNDARY_REJECTED, "runtime rejected",
            "runtime-test-source", "Runtime Test Source");
        RecordingSpotMarketService service = new RecordingSpotMarketService(runtimeDecision);

        StandardizedMarketAdmissionDecision decision = TerminalMarketService.inspectRuntimeCatalogProduct(service,
            "minecraft:stone:0");

        assertFalse(decision.isAdmitted());
        assertEquals("runtime-catalog-v2", decision.getCatalogVersion().getVersionKey());
        assertEquals("runtime-test-source", decision.getSourceKey());
    }

    @Test
    public void inspectRuntimeCatalogStackUsesInjectedRuntimeDecision() {
        StandardizedMarketAdmissionDecision runtimeDecision = new StandardizedMarketAdmissionDecision(
            new StandardizedMarketCatalogVersion("runtime-catalog-v2", "运行时目录 v2"),
            new StandardizedMarketCatalogEntry(new StandardizedMarketProduct("minecraft:stone", 0),
                "runtime-category", "runtime-only", "runtime-entry"),
            false, StandardizedMarketAdmissionReason.CATALOG_BOUNDARY_REJECTED, "runtime rejected",
            "runtime-test-source", "Runtime Test Source");
        RecordingSpotMarketService service = new RecordingSpotMarketService(runtimeDecision);

        StandardizedMarketAdmissionDecision decision = TerminalMarketService.inspectRuntimeCatalogStack(service,
            new ItemStack(Blocks.stone, 1, 0));

        assertFalse(decision.isAdmitted());
        assertEquals("runtime-test-source", decision.getSourceKey());
    }

    @Test
    public void standardizedTerminalNoLongerAutoSelectsFirstProductWithoutExplicitChoice() throws Exception {
        Method method = TerminalMarketService.class.getDeclaredMethod(
            "normalizeSelectedProductKey",
            String.class,
            List.class,
            findHeldMarketItemClass());
        method.setAccessible(true);

        Object normalized = method.invoke(TerminalMarketService.INSTANCE, "", Arrays.asList("product-a", "product-b"), null);

        assertEquals(null, normalized);
    }

    private static final class RecordingSpotMarketService extends StandardizedSpotMarketService {

        private final StandardizedMarketAdmissionDecision runtimeDecision;

        private RecordingSpotMarketService(StandardizedMarketAdmissionDecision runtimeDecision) {
            super(new NoOpOrderBookRepository(), new NoOpCustodyRepository(), new NoOpOperationLogRepository(),
                new NoOpTradeRecordRepository(), new DirectMarketTransactionRunner(), null);
            this.runtimeDecision = runtimeDecision;
        }

        @Override
        public StandardizedMarketAdmissionDecision inspectCatalogProduct(String productKey) {
            return runtimeDecision;
        }

        @Override
        public StandardizedMarketAdmissionDecision inspectCatalogStack(ItemStack stack) {
            return runtimeDecision;
        }
    }

    private static final class NoOpOrderBookRepository implements MarketOrderBookRepository {

        @Override
        public MarketOrder save(MarketOrder order) {
            return order;
        }

        @Override
        public MarketOrder update(MarketOrder order) {
            return order;
        }

        @Override
        public Optional<MarketOrder> findById(long orderId) {
            return Optional.empty();
        }

        @Override
        public MarketOrder lockById(long orderId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MarketOrder> findOpenSellOrdersByProductKey(String productKey) {
            return Collections.emptyList();
        }

        @Override
        public List<MarketOrder> findOpenBuyOrdersByProductKey(String productKey) {
            return Collections.emptyList();
        }

        @Override
        public List<MarketOrder> findMatchingSellOrders(String productKey, long maxUnitPrice) {
            return Collections.emptyList();
        }

        @Override
        public List<MarketOrder> findMatchingBuyOrders(String productKey, long minUnitPrice) {
            return Collections.emptyList();
        }
    }

    private static final class NoOpCustodyRepository implements MarketCustodyInventoryRepository {

        @Override
        public MarketCustodyInventory save(MarketCustodyInventory custodyInventory) {
            return custodyInventory;
        }

        @Override
        public MarketCustodyInventory update(MarketCustodyInventory custodyInventory) {
            return custodyInventory;
        }

        @Override
        public Optional<MarketCustodyInventory> findById(long custodyId) {
            return Optional.empty();
        }

        @Override
        public MarketCustodyInventory lockById(long custodyId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<MarketCustodyInventory> findEscrowSellByOrderId(long orderId) {
            return Optional.empty();
        }

        @Override
        public List<MarketCustodyInventory> findByOwnerAndStatus(String ownerPlayerRef,
            com.jsirgalaxybase.modules.core.market.domain.MarketCustodyStatus status) {
            return Collections.emptyList();
        }
    }

    private static final class NoOpOperationLogRepository implements MarketOperationLogRepository {

        @Override
        public MarketOperationLog save(MarketOperationLog operationLog) {
            return operationLog;
        }

        @Override
        public MarketOperationLog update(MarketOperationLog operationLog) {
            return operationLog;
        }

        @Override
        public Optional<MarketOperationLog> findById(long operationId) {
            return Optional.empty();
        }

        @Override
        public Optional<MarketOperationLog> findByRequestId(String requestId) {
            return Optional.empty();
        }

        @Override
        public List<MarketOperationLog> findByStatuses(List<MarketOperationStatus> statuses, int limit) {
            return Collections.emptyList();
        }
    }

    private static final class NoOpTradeRecordRepository implements MarketTradeRecordRepository {

        @Override
        public com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord save(
            com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord tradeRecord) {
            return tradeRecord;
        }

        @Override
        public List<com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord> findByOrderId(long orderId) {
            return Collections.emptyList();
        }
    }

    private static final class DirectMarketTransactionRunner implements MarketTransactionRunner {

        @Override
        public <T> T inTransaction(java.util.function.Supplier<T> callback) {
            return callback.get();
        }

        @Override
        public void inTransaction(Runnable callback) {
            callback.run();
        }
    }

    private TerminalExchangeQuoteView invokeBuildExchangeQuoteView(Object exchangeContext) throws Exception {
        Method method = TerminalMarketService.class.getDeclaredMethod("buildExchangeQuoteView",
            net.minecraft.entity.player.EntityPlayer.class, exchangeContext.getClass());
        method.setAccessible(true);
        return (TerminalExchangeQuoteView) method.invoke(TerminalMarketService.INSTANCE, null, exchangeContext);
    }

    private Object buildExchangeContext(
        com.jsirgalaxybase.modules.core.market.application.TaskCoinExchangeService exchangeService,
        String unavailableMessage, boolean ready) throws Exception {
        Class<?> exchangeContextClass = findExchangeContextClass();
        Constructor<?> constructor = exchangeContextClass.getDeclaredConstructor(
            com.jsirgalaxybase.modules.core.market.application.TaskCoinExchangeService.class,
            String.class,
            boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(exchangeService, unavailableMessage, Boolean.valueOf(ready));
    }

    private Class<?> findExchangeContextClass() {
        for (Class<?> candidate : TerminalMarketService.class.getDeclaredClasses()) {
            if ("ExchangeContext".equals(candidate.getSimpleName())) {
                return candidate;
            }
        }
        throw new IllegalStateException("ExchangeContext class not found");
    }

    private Class<?> findHeldMarketItemClass() {
        for (Class<?> candidate : TerminalMarketService.class.getDeclaredClasses()) {
            if ("HeldMarketItem".equals(candidate.getSimpleName())) {
                return candidate;
            }
        }
        throw new IllegalStateException("HeldMarketItem class not found");
    }
}