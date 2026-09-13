package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Explicit command-root allowlist. An empty set intentionally denies every legacy command reward. */
public final class AllowlistedCommandRewardPolicy implements CommandRewardPolicy {
    private final Set<String> roots;
    public AllowlistedCommandRewardPolicy(Set<String> roots) {
        Set<String> copy = new HashSet<String>();
        if (roots != null) for (String root : roots) if (root != null && !root.trim().isEmpty()) {
            copy.add(root.trim().toLowerCase(Locale.ROOT));
        }
        this.roots = Collections.unmodifiableSet(copy);
    }
    @Override public boolean allows(String command, boolean viaPlayer) {
        if (command == null || command.isEmpty() || command.length() > 2048) return false;
        for (int i = 0; i < command.length(); i++) if (Character.isISOControl(command.charAt(i))) return false;
        int space = command.indexOf(' ');
        String root = (space < 0 ? command : command.substring(0, space)).toLowerCase(Locale.ROOT);
        return roots.contains(root);
    }
}
