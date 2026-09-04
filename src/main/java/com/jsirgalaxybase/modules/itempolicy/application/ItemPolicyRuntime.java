package com.jsirgalaxybase.modules.itempolicy.application;

import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyDecision;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyScope;

import net.minecraft.item.ItemStack;

/**
 * Process bridge used only by dedicated-server entrypoints. Until the enabled
 * module installs its service, policy is explicitly permissive.
 */
public final class ItemPolicyRuntime {

    private static volatile ItemPolicyService service;

    private ItemPolicyRuntime() {}

    public static void install(ItemPolicyService installed) { service = installed; }
    public static void clear() { service = null; }
    public static boolean isEnabled() { return service != null; }

    public static ItemPolicyDecision evaluate(ItemPolicyScope scope, ItemStack stack) {
        ItemPolicyService current = service;
        return current == null ? ItemPolicyDecision.allowed() : current.evaluate(scope, stack);
    }

    public static void requireAllowed(ItemPolicyScope scope, String actorRef, String operation, ItemStack stack) {
        ItemPolicyService current = service;
        if (current != null) current.requireAllowed(scope, actorRef, operation, stack);
    }
}
