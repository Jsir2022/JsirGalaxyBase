package com.jsirgalaxybase.modules.core.market.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

public class GregTechStandardizedMetalCatalogTest {

    private static final Item IRON_INGOT = namedItem("iron_ingot");
    private static final Item STONE = namedItem("stone");

    @Test
    public void allowsConfiguredStandardizedMetalProduct() {
        GregTechStandardizedMetalCatalog catalog = new FakeGregTechCatalog();

        StandardizedMarketCatalogEntry entry = catalog.findEntryByProductKey("minecraft:iron_ingot:0").get();

        assertEquals("minecraft:iron_ingot:0", entry.getProduct().getProductKey());
        assertEquals("gregtech-standardized-metal", entry.getCategoryCode());
    }

    @Test
    public void allowsConfiguredStandardizedMetalStack() {
        GregTechStandardizedMetalCatalog catalog = new FakeGregTechCatalog();

        StandardizedMarketCatalogEntry entry = catalog.findEntryByStack(new ItemStack(IRON_INGOT, 4, 0)).get();

        assertEquals("minecraft:iron_ingot:0", entry.getProduct().getProductKey());
    }

    @Test
    public void rejectsOrdinaryBlockOutsideStandardizedMetalCatalog() {
        GregTechStandardizedMetalCatalog catalog = new FakeGregTechCatalog();

        assertFalse(catalog.findEntryByStack(new ItemStack(STONE, 8, 0)).isPresent());
    }

    @Test
    public void bridgesIntoFormalCatalogServiceAsTemporarySource() {
        StandardizedMarketCatalogService service = new StandardizedMarketCatalogService(
            StandardizedMarketCatalogFactory.DEFAULT_VERSION, new FakeGregTechCatalog());

        StandardizedMarketAdmissionDecision decision = service.evaluateProduct("minecraft:iron_ingot:0");

        assertTrue(decision.isAdmitted());
        assertEquals(GregTechStandardizedMetalCatalog.SOURCE_KEY, decision.getSourceKey());
        assertTrue(decision.getSourceDescription().contains("不等于标准商品市场制度边界本体"));
    }

    private static Item namedItem(String name) {
        return new Item().setUnlocalizedName(name);
    }

    private static final class FakeGregTechCatalog extends GregTechStandardizedMetalCatalog {

        private final Map<String, Item> itemsByKey = new HashMap<String, Item>();
        private final Map<Item, String> keysByItem = new HashMap<Item, String>();

        private FakeGregTechCatalog() {
            super(new StandardizedMarketProductParser());
            register("minecraft:iron_ingot", IRON_INGOT);
            register("minecraft:stone", STONE);
        }

        @Override
        protected Item resolveItem(StandardizedMarketProduct product) {
            Item item = itemsByKey.get(product.getRegistryName());
            if (item == null) {
                throw new MarketOperationException("standardized product item is not registered: " + product.getProductKey());
            }
            return item;
        }

        @Override
        protected String resolveRegistryName(Item item) {
            String key = keysByItem.get(item);
            return key == null ? "" : key;
        }

        private void register(String registryName, Item item) {
            itemsByKey.put(registryName, item);
            keysByItem.put(item, registryName);
        }
    }
}