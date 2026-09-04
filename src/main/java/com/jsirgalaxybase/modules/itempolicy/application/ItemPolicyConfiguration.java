package com.jsirgalaxybase.modules.itempolicy.application;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyRule;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyScope;

/** Strict parser for `rule-id|SCOPE,SCOPE|modid:item[:meta]` server config entries. */
public final class ItemPolicyConfiguration {

    private ItemPolicyConfiguration() {}

    public static List<ItemPolicyRule> parseRules(boolean enabled, String[] rawRules) {
        if (!enabled) return java.util.Collections.emptyList();
        List<ItemPolicyRule> rules = new ArrayList<ItemPolicyRule>();
        Set<String> ids = new HashSet<String>();
        if (rawRules == null) return rules;
        for (String rawRule : rawRules) {
            if (rawRule == null || rawRule.trim().isEmpty()) {
                throw new IllegalArgumentException("itemPolicyRules cannot contain blank entries when enabled");
            }
            String[] fields = rawRule.trim().split("\\|", -1);
            if (fields.length != 3) {
                throw new IllegalArgumentException("invalid item policy rule '" + rawRule
                    + "'; expected rule-id|SCOPE,SCOPE|modid:item[:meta]");
            }
            String id = fields[0].trim();
            if (!ids.add(id)) throw new IllegalArgumentException("duplicate item policy rule id: " + id);
            EnumSet<ItemPolicyScope> scopes = EnumSet.noneOf(ItemPolicyScope.class);
            for (String scope : fields[1].split(",")) scopes.add(ItemPolicyScope.parseStrict(scope));
            ParsedMatcher matcher = parseMatcher(fields[2].trim(), rawRule);
            rules.add(new ItemPolicyRule(id, scopes, matcher.registryName, matcher.meta));
        }
        return java.util.Collections.unmodifiableList(rules);
    }

    private static ParsedMatcher parseMatcher(String matcher, String rawRule) {
        String[] fragments = matcher.split(":", -1);
        if (fragments.length != 2 && fragments.length != 3 || fragments[0].trim().isEmpty()
            || fragments[1].trim().isEmpty()) {
            throw new IllegalArgumentException("invalid item matcher in rule '" + rawRule + "'");
        }
        Integer meta = null;
        if (fragments.length == 3) {
            try {
                meta = Integer.valueOf(fragments[2].trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("item policy meta must be an integer in rule '" + rawRule + "'", exception);
            }
            if (meta.intValue() < 0) throw new IllegalArgumentException("item policy meta must be non-negative in rule '" + rawRule + "'");
        }
        return new ParsedMatcher(fragments[0].trim() + ":" + fragments[1].trim(), meta);
    }

    private static final class ParsedMatcher {
        private final String registryName;
        private final Integer meta;
        private ParsedMatcher(String registryName, Integer meta) {
            this.registryName = registryName;
            this.meta = meta;
        }
    }
}
