package com.jsirgalaxybase.modules.core.market.application;

import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.TaskCoinDescriptor;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinExchangeQuote;

public class TaskCoinExchangePlanner {

    public static final String RULE_VERSION = "market-phase1-source-blind-v1";
    public static final String REGISTRY_PREFIX = "dreamcraft:item.Coin";
    private final TaskCoinCatalog catalog;

    public TaskCoinExchangePlanner() {
        this(TaskCoinCatalog.defaultCatalog());
    }

    TaskCoinExchangePlanner(TaskCoinCatalog catalog) {
        this.catalog = catalog == null ? TaskCoinCatalog.defaultCatalog() : catalog;
    }

    public boolean isTaskCoinRegistryName(String registryName) {
        return registryName != null && registryName.trim().startsWith(REGISTRY_PREFIX);
    }

    public boolean isUnsupportedTaskCoinTier(String registryName) {
        if (!isTaskCoinRegistryName(registryName)) {
            return false;
        }

        String normalized = registryName.trim();
        return !catalog.find(normalized).isPresent();
    }

    public Optional<TaskCoinDescriptor> resolveRegistryName(String registryName) {
        if (registryName == null) {
            return Optional.empty();
        }

        return catalog.find(registryName).map(TaskCoinCatalog.Entry::toDescriptor);
    }

    public Optional<TaskCoinExchangeQuote> quote(String registryName, int quantity) {
        if (quantity <= 0) {
            return Optional.empty();
        }

        Optional<TaskCoinDescriptor> descriptor = resolveRegistryName(registryName);
        if (!descriptor.isPresent()) {
            return Optional.empty();
        }

        long effectiveExchangeValue = descriptor.get().getFaceValue() * (long) quantity;
        return Optional.of(new TaskCoinExchangeQuote(descriptor.get(), quantity, effectiveExchangeValue,
            effectiveExchangeValue, RULE_VERSION));
    }
}
