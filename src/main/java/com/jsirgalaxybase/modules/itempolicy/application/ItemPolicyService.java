package com.jsirgalaxybase.modules.itempolicy.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyDecision;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyRule;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyScope;
import com.jsirgalaxybase.modules.itempolicy.port.ItemPolicyAuditSink;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Server-authoritative deny policy. A denied decision is recorded before it is surfaced to a caller. */
public final class ItemPolicyService {

    private final List<ItemPolicyRule> rules;
    private final ItemPolicyAuditSink auditSink;
    private final String sourceServerId;

    public ItemPolicyService(List<ItemPolicyRule> rules, ItemPolicyAuditSink auditSink, String sourceServerId) {
        this.rules = Collections.unmodifiableList(new ArrayList<ItemPolicyRule>(rules == null
            ? Collections.<ItemPolicyRule>emptyList() : rules));
        if (auditSink == null) throw new IllegalArgumentException("item policy audit sink is required");
        if (sourceServerId == null || sourceServerId.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceServerId is required");
        }
        this.auditSink = auditSink;
        this.sourceServerId = sourceServerId.trim();
    }

    public ItemPolicyDecision evaluate(ItemPolicyScope scope, ItemStack stack) {
        if (scope == null || stack == null || stack.getItem() == null) return ItemPolicyDecision.allowed();
        Object registry = Item.itemRegistry.getNameForObject(stack.getItem());
        if (registry == null) return ItemPolicyDecision.allowed();
        return evaluate(scope, String.valueOf(registry), stack.getItemDamage());
    }

    public ItemPolicyDecision evaluate(ItemPolicyScope scope, String registryName, int meta) {
        if (scope == null || registryName == null || registryName.trim().isEmpty()) return ItemPolicyDecision.allowed();
        for (ItemPolicyRule rule : rules) {
            if (rule.matches(scope, registryName, meta)) return ItemPolicyDecision.denied(rule.getRuleId());
        }
        return ItemPolicyDecision.allowed();
    }

    public void requireAllowed(ItemPolicyScope scope, String actorRef, String operation, ItemStack stack) {
        Object registry = stack == null || stack.getItem() == null ? null : Item.itemRegistry.getNameForObject(stack.getItem());
        requireAllowed(scope, actorRef, operation, registry == null ? null : String.valueOf(registry),
            stack == null ? 0 : stack.getItemDamage());
    }

    public void requireAllowed(ItemPolicyScope scope, String actorRef, String operation, String registryName, int meta) {
        ItemPolicyDecision decision = evaluate(scope, registryName, meta);
        if (decision.isAllowed()) return;
        auditSink.recordDenied(sourceServerId, actorRef, scope, operation, registryName, meta, decision.getRuleId());
        throw new ItemPolicyViolationException("物品不允许进入该资产域（规则 " + decision.getRuleId() + "）");
    }
}
