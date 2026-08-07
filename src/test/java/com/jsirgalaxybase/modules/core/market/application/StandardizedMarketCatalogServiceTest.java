package com.jsirgalaxybase.modules.core.market.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Optional;
import java.util.Arrays;

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

    @Test
    public void databaseStyleCatalogSourceExposesStableBrowsePage() {
        StandardizedMarketCatalogService catalog = new StandardizedMarketCatalogService(
            new StandardizedMarketCatalogVersion("catalog-v1", "目录 v1"), new BrowseableFakeCatalogSource());

        StandardizedMarketCatalogPage page = catalog.browse("steel", 1, 2);

        assertEquals("steel", page.getQuery());
        assertEquals(1, page.getPageIndex());
        assertEquals(2, page.getPageSize());
        assertEquals(3, page.getTotalEntries());
        assertTrue(page.hasPreviousPage());
        assertFalse(page.hasNextPage());
        assertEquals("Steel Ingot", page.getEntries().get(0).getDisplayName());
        assertEquals("ingot", page.getEntries().get(0).getUnitLabel());
    }

    @Test
    public void disabledProductIsNeitherBrowsableNorAdmitted() {
        StandardizedMarketCatalogService catalog = new StandardizedMarketCatalogService(
            new StandardizedMarketCatalogVersion("catalog-v1", "目录 v1"), new DisabledCatalogSource());

        StandardizedMarketCatalogPage page = catalog.browse("iron", 0, 8);
        StandardizedMarketAdmissionDecision decision = catalog.evaluateProduct("minecraft:iron_ingot:0");

        assertEquals(0, page.getTotalEntries());
        assertFalse(page.hasNextPage());
        assertFalse(decision.isAdmitted());
        assertEquals(StandardizedMarketAdmissionReason.CATALOG_BOUNDARY_REJECTED, decision.getReason());
    }

    @Test
    public void emptyCatalogueKeepsStablePageContract() {
        StandardizedMarketCatalogService catalog = new StandardizedMarketCatalogService(
            new StandardizedMarketCatalogVersion("catalog-v1", "目录 v1"), new DisabledCatalogSource());

        StandardizedMarketCatalogPage page = catalog.browse("missing", 9, 8);

        assertEquals("missing", page.getQuery());
        assertEquals(0, page.getPageIndex());
        assertEquals(8, page.getPageSize());
        assertEquals(0, page.getEntries().size());
        assertFalse(page.hasPreviousPage());
        assertFalse(page.hasNextPage());
    }

    private static class FakeCatalogSource implements StandardizedMarketCatalogSource {

        private final boolean admitted;

        protected FakeCatalogSource(boolean admitted) {
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

    private static final class BrowseableFakeCatalogSource extends FakeCatalogSource
        implements StandardizedMarketCatalogBrowser {

        private BrowseableFakeCatalogSource() {
            super(true);
        }

        @Override
        public StandardizedMarketCatalogPage browse(String query, int pageIndex, int pageSize) {
            StandardizedMarketCatalogEntry entry = new StandardizedMarketCatalogEntry(
                new StandardizedMarketProduct("gregtech:steel_ingot", 0), "metal", "管理员目录准入", "test",
                "Steel Ingot", "ingot", 20, "catalog-v1");
            return new StandardizedMarketCatalogPage(query, pageIndex, pageSize, 3,
                Arrays.asList(entry));
        }
    }

    private static final class DisabledCatalogSource extends FakeCatalogSource
        implements StandardizedMarketCatalogBrowser {

        private DisabledCatalogSource() {
            super(false);
        }

        @Override
        public StandardizedMarketCatalogPage browse(String query, int pageIndex, int pageSize) {
            return new StandardizedMarketCatalogPage(query, 0, pageSize, 0,
                java.util.Collections.<StandardizedMarketCatalogEntry>emptyList());
        }
    }
}
