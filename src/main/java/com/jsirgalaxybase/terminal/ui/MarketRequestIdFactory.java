package com.jsirgalaxybase.terminal.ui;

import java.util.UUID;

final class MarketRequestIdFactory {

    static final int DATABASE_LIMIT = 64;
    static final int DERIVED_SUFFIX_BUDGET = 18;
    static final int ROOT_LIMIT = DATABASE_LIMIT - DERIVED_SUFFIX_BUDGET;

    private MarketRequestIdFactory() {}

    static String newRoot(String prefix) {
        return buildRoot(prefix, UUID.randomUUID().toString().replace("-", ""));
    }

    static String buildRoot(String prefix, String nonce) {
        String safeNonce = normalizeNonce(nonce);
        int prefixLimit = Math.max(1, ROOT_LIMIT - safeNonce.length() - 1);
        String safePrefix = normalizePrefix(prefix);
        if (safePrefix.length() > prefixLimit) {
            safePrefix = safePrefix.substring(0, prefixLimit);
        }
        return safePrefix + ":" + safeNonce;
    }

    private static String normalizePrefix(String prefix) {
        String value = prefix == null ? "market" : prefix.trim().toLowerCase();
        value = value.replaceAll("[^a-z0-9_-]", "-");
        return value.isEmpty() ? "market" : value;
    }

    private static String normalizeNonce(String nonce) {
        String value = nonce == null ? "" : nonce.replace("-", "").trim().toLowerCase();
        value = value.replaceAll("[^a-z0-9]", "");
        if (value.isEmpty()) {
            value = UUID.randomUUID().toString().replace("-", "");
        }
        return value.length() <= 32 ? value : value.substring(0, 32);
    }
}
