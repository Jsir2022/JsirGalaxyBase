package com.jsirgalaxybase.modules.core.market.application;

import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.TaskCoinDescriptor;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinExchangeQuote;

public class TaskCoinExchangePlanner {

    public static final String RULE_VERSION = "market-phase1-source-blind-v1";
    private static final String REGISTRY_PREFIX = "dreamcraft:item.Coin";
    private static final java.util.regex.Pattern SUPPORTED_SUFFIX = java.util.regex.Pattern
        .compile("^([A-Za-z][A-Za-z0-9_]*?)(IV|III|II|I)?$");
    private static final java.util.regex.Pattern UNSUPPORTED_ROMAN_SUFFIX = java.util.regex.Pattern
        .compile(".*(V|VI|VII|VIII|IX|X)$");

    public Optional<TaskCoinDescriptor> resolveRegistryName(String registryName) {
        if (registryName == null) {
            return Optional.empty();
        }

        String normalized = registryName.trim();
        if (!normalized.startsWith(REGISTRY_PREFIX)) {
            return Optional.empty();
        }

        String suffix = normalized.substring(REGISTRY_PREFIX.length());
        if (suffix.isEmpty()) {
            return Optional.empty();
        }

        java.util.regex.Matcher matcher = SUPPORTED_SUFFIX.matcher(suffix);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String family = matcher.group(1);
        String tier = "BASE";
        long faceValue = 1L;
        String tierSuffix = matcher.group(2);
        if ("IV".equals(tierSuffix)) {
            tier = "IV";
            faceValue = 10000L;
        } else if ("III".equals(tierSuffix)) {
            tier = "III";
            faceValue = 1000L;
        } else if ("II".equals(tierSuffix)) {
            tier = "II";
            faceValue = 100L;
        } else if ("I".equals(tierSuffix)) {
            tier = "I";
            faceValue = 10L;
        } else if (UNSUPPORTED_ROMAN_SUFFIX.matcher(suffix).matches()) {
            return Optional.empty();
        }

        if (family.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TaskCoinDescriptor(normalized, family, tier, faceValue));
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