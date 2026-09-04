package com.jsirgalaxybase.modules.itempolicy.domain;

/**
 * A controlled boundary where an ItemStack may be admitted. The policy does
 * not delete items or observe ordinary player inventory operations.
 */
public enum ItemPolicyScope {

    MARKET_CUSTODY,
    CUSTOM_MARKET_ESCROW,
    BASE_VAULT,
    LAND_AUTOMATION;

    public static ItemPolicyScope parseStrict(String value) {
        if (value == null) throw new IllegalArgumentException("item policy scope must not be null");
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown item policy scope: " + value, exception);
        }
    }
}
