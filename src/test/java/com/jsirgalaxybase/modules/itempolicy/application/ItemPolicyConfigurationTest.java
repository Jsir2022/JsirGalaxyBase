package com.jsirgalaxybase.modules.itempolicy.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyDecision;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyRule;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyScope;
import com.jsirgalaxybase.modules.itempolicy.port.ItemPolicyAuditSink;


public class ItemPolicyConfigurationTest {

    @Test
    public void parsesScopedExactAndAnyMetaRules() {
        List<ItemPolicyRule> rules = ItemPolicyConfiguration.parseRules(true, new String[] {
            "deny-stone|MARKET_CUSTODY,CUSTOM_MARKET_ESCROW|minecraft:stone:0",
            "deny-dirt|BASE_VAULT|minecraft:dirt"
        });

        assertEquals(2, rules.size());
        assertTrue(rules.get(0).matches(ItemPolicyScope.MARKET_CUSTODY, "minecraft:stone", 0));
        assertFalse(rules.get(0).matches(ItemPolicyScope.BASE_VAULT, "minecraft:stone", 0));
        assertTrue(rules.get(1).matches(ItemPolicyScope.BASE_VAULT, "minecraft:dirt", 3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDuplicateRuleIdentifiers() {
        ItemPolicyConfiguration.parseRules(true, new String[] {
            "same|MARKET_CUSTODY|minecraft:stone",
            "same|BASE_VAULT|minecraft:dirt"
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownScopeWhenEnabled() {
        ItemPolicyConfiguration.parseRules(true, new String[] { "bad|NOT_A_SCOPE|minecraft:stone" });
    }

    @Test
    public void disabledPolicyIgnoresDormantRules() {
        assertTrue(ItemPolicyConfiguration.parseRules(false, new String[] { "not a valid rule" }).isEmpty());
    }

    @Test
    public void deniedItemIsAuditedBeforeViolationIsReturned() {
        final String[] captured = new String[2];
        ItemPolicyAuditSink sink = new ItemPolicyAuditSink() {
            @Override
            public void recordDenied(String server, String actor, ItemPolicyScope scope, String operation,
                String registryName, int meta, String ruleId) {
                captured[0] = registryName;
                captured[1] = ruleId;
            }
        };
        ItemPolicyService service = new ItemPolicyService(ItemPolicyConfiguration.parseRules(true,
            new String[] { "deny-stone|MARKET_CUSTODY|minecraft:stone" }), sink, "test-server");
        ItemPolicyDecision decision = service.evaluate(ItemPolicyScope.MARKET_CUSTODY, "minecraft:stone", 0);

        assertFalse(decision.isAllowed());
        try {
            service.requireAllowed(ItemPolicyScope.MARKET_CUSTODY, "player-a", "test", "minecraft:stone", 0);
        } catch (ItemPolicyViolationException expected) {
            assertEquals("minecraft:stone", captured[0]);
            assertEquals("deny-stone", captured[1]);
            return;
        }
        throw new AssertionError("expected denied item policy violation");
    }
}
