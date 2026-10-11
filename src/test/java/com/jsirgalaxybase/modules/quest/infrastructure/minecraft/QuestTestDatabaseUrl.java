package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.net.URI;

final class QuestTestDatabaseUrl {
    private QuestTestDatabaseUrl() { }
    static boolean isExplicitlyIsolated(String url, String acknowledged) {
        if (!"true".equalsIgnoreCase(acknowledged) || url == null) return false;
        try {
            if (!url.startsWith("jdbc:postgresql://")) return false;
            URI uri = URI.create(url.substring("jdbc:".length()));
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || path == null || host.equalsIgnoreCase("galaxy-base")) return false;
            return path.substring(path.lastIndexOf('/') + 1).matches("(?i).*(?:_test|_it)(?:_[a-z0-9]+)?$");
        } catch (RuntimeException invalid) {
            return false;
        }
    }
}
