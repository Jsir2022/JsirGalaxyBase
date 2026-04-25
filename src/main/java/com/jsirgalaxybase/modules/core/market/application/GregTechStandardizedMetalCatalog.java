package com.jsirgalaxybase.modules.core.market.application;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;

public class GregTechStandardizedMetalCatalog implements StandardizedMarketCatalogSource {

    public static final String SOURCE_KEY = "gregtech-standardized-metal-adapter";
    public static final String SOURCE_DESCRIPTION = "GregTech 标准金属适配集合；它是当前首版目录来源，不等于标准商品市场制度边界本体。";

    private static final Set<String> ALLOWED_PREFIXES = Collections.unmodifiableSet(new HashSet<String>(
        Arrays.asList(
            "ingot",
            "nugget",
            "dust",
            "dustsmall",
            "dusttiny",
            "plate",
            "platedouble",
            "platedense",
            "foil",
            "stick",
            "sticklong",
            "bolt",
            "screw",
            "ring",
            "springsmall",
            "spring")));

    private final StandardizedMarketProductParser productParser;

    public GregTechStandardizedMetalCatalog() {
        this(new StandardizedMarketProductParser());
    }

    public GregTechStandardizedMetalCatalog(StandardizedMarketProductParser productParser) {
        this.productParser = productParser == null ? new StandardizedMarketProductParser() : productParser;
    }

    @Override
    public String getSourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public String getSourceDescription() {
        return SOURCE_DESCRIPTION;
    }

    @Override
    public Optional<StandardizedMarketCatalogEntry> findEntryByProductKey(String productKey) {
        StandardizedMarketProduct product = productParser.parse(productKey);
        Item item = resolveItem(product);
        ItemStack stack = new ItemStack(item, 1, product.getMeta());
        return inspectTradable(stack, product);
    }

    @Override
    public Optional<StandardizedMarketCatalogEntry> findEntryByStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            return Optional.empty();
        }
        String registryName = resolveRegistryName(stack.getItem());
        if (registryName == null || registryName.trim().isEmpty()) {
            return Optional.empty();
        }
        String productKey = registryName + ":" + stack.getItemDamage();
        StandardizedMarketProduct product = productParser.parse(productKey);
        return inspectTradable(stack, product);
    }

    private Optional<StandardizedMarketCatalogEntry> inspectTradable(ItemStack stack, StandardizedMarketProduct product) {
        Object association = invokeStatic("gregtech.api.util.GTOreDictUnificator", "getAssociation",
            new Class<?>[] { ItemStack.class }, stack);
        if (association == null || !invokeBoolean(association, "hasValidPrefixMaterialData")
            || readField(association, "mPrefix") == null || readField(association, "mMaterial") == null
            || readField(readField(association, "mMaterial"), "mMaterial") == null) {
            return Optional.empty();
        }

        Object prefix = readField(association, "mPrefix");
        Object material = readField(readField(association, "mMaterial"), "mMaterial");
        String prefixName = String.valueOf(prefix).toLowerCase(Locale.ROOT);
        if (!ALLOWED_PREFIXES.contains(prefixName)) {
            return Optional.empty();
        }
        boolean hasMetalItems = invokeBoolean(material, "hasMetalItems");
        Object metalTag = readStaticField("gregtech.api.enums.SubTag", "METAL");
        boolean taggedMetal = metalTag != null && invokeBoolean(material, "contains",
            new Class<?>[] { metalTag.getClass() }, metalTag);
        if (!hasMetalItems && !taggedMetal) {
            return Optional.empty();
        }

        return Optional.of(new StandardizedMarketCatalogEntry(product, "gregtech-standardized-metal",
            "当前商品满足统一定义、统一计量、统一托管并可进入标准商品订单簿。",
            "GregTech 前缀=" + prefixName));
    }

    private Object invokeStatic(String className, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return Class.forName(className).getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (Exception exception) {
            throw new MarketOperationException("failed to inspect GregTech standardized metal metadata");
        }
    }

    private boolean invokeBoolean(Object target, String methodName) {
        return invokeBoolean(target, methodName, new Class<?>[0]);
    }

    private boolean invokeBoolean(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Object value = target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Exception exception) {
            throw new MarketOperationException("failed to inspect GregTech standardized metal metadata");
        }
    }

    private Object readField(Object target, String fieldName) {
        try {
            return target.getClass().getField(fieldName).get(target);
        } catch (Exception exception) {
            throw new MarketOperationException("failed to inspect GregTech standardized metal metadata");
        }
    }

    private Object readStaticField(String className, String fieldName) {
        try {
            return Class.forName(className).getField(fieldName).get(null);
        } catch (Exception exception) {
            throw new MarketOperationException("failed to inspect GregTech standardized metal metadata");
        }
    }

    protected Item resolveItem(StandardizedMarketProduct product) {
        String registryName = product.getRegistryName();
        int separatorIndex = registryName.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= registryName.length() - 1) {
            throw new MarketOperationException("invalid standardized product registry name");
        }
        Item item = GameRegistry.findItem(registryName.substring(0, separatorIndex),
            registryName.substring(separatorIndex + 1));
        if (item == null) {
            throw new MarketOperationException("standardized product item is not registered: " + product.getProductKey());
        }
        return item;
    }

    protected String resolveRegistryName(Item item) {
        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(item);
        if (identifier != null) {
            return identifier.modId + ":" + identifier.name;
        }
        Object fallback = GameData.getItemRegistry().getNameForObject(item);
        return fallback == null ? "" : String.valueOf(fallback);
    }
}