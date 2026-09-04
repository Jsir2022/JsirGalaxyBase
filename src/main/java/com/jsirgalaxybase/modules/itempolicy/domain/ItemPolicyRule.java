package com.jsirgalaxybase.modules.itempolicy.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Immutable deny rule parsed from the server-only configuration. */
public final class ItemPolicyRule {

    private final String ruleId;
    private final Set<ItemPolicyScope> scopes;
    private final String registryName;
    private final Integer meta;

    public ItemPolicyRule(String ruleId, Set<ItemPolicyScope> scopes, String registryName, Integer meta) {
        if (ruleId == null || ruleId.trim().isEmpty()) throw new IllegalArgumentException("ruleId must not be blank");
        if (scopes == null || scopes.isEmpty()) throw new IllegalArgumentException("rule scopes must not be empty");
        if (registryName == null || registryName.trim().isEmpty()) {
            throw new IllegalArgumentException("item policy registryName must not be blank");
        }
        this.ruleId = ruleId.trim();
        this.scopes = Collections.unmodifiableSet(EnumSet.copyOf(scopes));
        this.registryName = registryName.trim().toLowerCase(java.util.Locale.ROOT);
        this.meta = meta;
    }

    public String getRuleId() { return ruleId; }
    public Set<ItemPolicyScope> getScopes() { return scopes; }
    public String getRegistryName() { return registryName; }
    public Integer getMeta() { return meta; }

    public boolean matches(ItemPolicyScope scope, String candidateRegistryName, int candidateMeta) {
        if (!scopes.contains(scope) || candidateRegistryName == null) return false;
        if (!registryName.equals(candidateRegistryName.trim().toLowerCase(java.util.Locale.ROOT))) return false;
        return meta == null || meta.intValue() == candidateMeta;
    }
}
