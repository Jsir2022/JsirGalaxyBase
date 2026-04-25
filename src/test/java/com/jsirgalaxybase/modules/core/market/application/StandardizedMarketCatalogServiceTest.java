package com.jsirgalaxybase.modules.core.market.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Optional;

import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

public class StandardizedMarketCatalogServiceTest {

    @Test
    public void admittedProductCarriesVersionSourceAndReason() {
        StandardizedMarketCatalogService catalog = new StandardizedMarketCatalogService(
            new StandardizedMarketCatalogVersion("catalog-v1", "目录 v1"), new FakeCatalogSource(true));

        StandardizedMarketAdmissionDecision decision = catalog.evaluateProduct("minecraft:iron_ingot:0");

        assertTrue(decision.isAdmitted());
        assertEquals("catalog-v1", decision.getCatalogVersion().getVersionKey());
        assertEquals(StandardizedMarketAdmissionReason.CATALOG_ADMITTED, decision.getReason());
        assertEquals("fake-source", decision.getSourceKey());
        assertEquals("minecraft:iron_ingot:0", decision.requireProduct().getProductKey());
    }

    @Test
    public void rejectedProductUsesCatalogBoundaryReason() {
        StandardizedMarketCatalogService catalog = new StandardizedMarketCatalogService(
            new StandardizedMarketCatalogVersion("catalog-v1", "目录 v1"), new FakeCatalogSource(false));

        StandardizedMarketAdmissionDecision decision = catalog.evaluateProduct("minecraft:stone:0");

        assertFalse(decision.isAdmitted());
        assertEquals(StandardizedMarketAdmissionReason.CATALOG_BOUNDARY_REJECTED, decision.getReason());
        assertTrue(decision.getDetailMessage().contains("标准商品市场目录 catalog-v1 的准入边界"));
        try {
            decision.requireProduct();
            fail("expected rejected decision to throw");
        } catch (MarketOperationException expected) {
            assertTrue(expected.getMessage().contains("Fake Source"));
        }
    }

    @Test
    public void invalidStackReturnsStructuredInvalidStackDecision() {
        StandardizedMarketCatalogService catalog = new StandardizedMarketCatalogService(
            new StandardizedMarketCatalogVersion("catalog-v1", "目录 v1"), new FakeCatalogSource(true));

        StandardizedMarketAdmissionDecision decision = catalog.evaluateStack(null);

        assertFalse(decision.isAdmitted());
        assertEquals(StandardizedMarketAdmissionReason.INVALID_STACK, decision.getReason());
        assertTrue(decision.getDetailMessage().contains("真实物品堆"));
    }

    private static final class FakeCatalogSource implements StandardizedMarketCatalogSource {

        private final boolean admitted;

        private FakeCatalogSource(boolean admitted) {
            this.admitted = admitted;
        }

        @Override
        public String getSourceKey() {
            return "fake-source";
        }

        @Override
        public String getSourceDescription() {
            return "Fake Source";
        }

        @Override
        public Optional<StandardizedMarketCatalogEntry> findEntryByProductKey(String productKey) {
            if (!admitted) {
                return Optional.empty();
            }
            return Optional.of(new StandardizedMarketCatalogEntry(new StandardizedMarketProduct("minecraft:iron_ingot", 0),
                "fake-category", "统一定义、统一计量、统一托管", "fake-entry"));
        }

        @Override
        public Optional<StandardizedMarketCatalogEntry> findEntryByStack(ItemStack stack) {
            if (!admitted || stack == null || stack.getItem() == null) {
                return Optional.empty();
            }
            return findEntryByProductKey("minecraft:iron_ingot:0");
        }
    }
}