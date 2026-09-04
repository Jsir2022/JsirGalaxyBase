package com.jsirgalaxybase.modules.itempolicy.domain;

import java.time.Instant;

/** Read-only denial audit row for the actor's own terminal diagnostics. */
public final class ItemPolicyAuditRecord {
    private final String scope, operation, registryName, ruleId;
    private final int meta;
    private final Instant createdAt;
    public ItemPolicyAuditRecord(String scope, String operation, String registryName, int meta, String ruleId, Instant createdAt) {
        this.scope = text(scope, "UNKNOWN"); this.operation = text(operation, "unknown");
        this.registryName = text(registryName, "unknown"); this.meta = meta; this.ruleId = text(ruleId, "unknown");
        this.createdAt = createdAt == null ? Instant.EPOCH : createdAt;
    }
    public String getScope() { return scope; } public String getOperation() { return operation; }
    public String getRegistryName() { return registryName; } public int getMeta() { return meta; }
    public String getRuleId() { return ruleId; } public Instant getCreatedAt() { return createdAt; }
    private static String text(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }
}
